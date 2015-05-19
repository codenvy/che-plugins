/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.docker.client;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.inject.ConfigurationProperties;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects auth configurations for private docker registries. Credential might be configured in .properties files, see details {@link
 * org.eclipse.che.inject.CodenvyBootstrap}. Credentials configured as (key=value) pairs. Key is string that starts with prefix
 * {@code docker.registry.auth.} followed by host name and port (optional) of docker registry server, e.g. {@code
 * docker.registry.auth.localhost:5000}. Value is comma separated pair of username and password, e.g.:
 * <pre>{@code
 * docker.registry.auth.localhost:5000=user:secret
 * }</pre>
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class AuthConfigs {
    private static final String CONFIGURATION_PREFIX         = "docker.registry.auth.";
    private static final String CONFIGURATION_PREFIX_PATTERN = "docker\\.registry\\.auth\\..+";
    private static final String INDEX_SERVER_NAME            = "docker.io";

    private Map<String, String> encodedAuthConfigs;

    @Inject
    AuthConfigs(ConfigurationProperties configurationProperties) {
        final Set<AuthConfig> configs = new HashSet<>();
        for (Map.Entry<String, String> e : configurationProperties.getProperties(CONFIGURATION_PREFIX_PATTERN).entrySet()) {
            final String key = e.getKey();
            final String serverAddress = key.replaceFirst(CONFIGURATION_PREFIX, "");
            final String value = e.getValue();
            String username;
            String password = "";
            final int i = value.indexOf(':');
            final int length = value.length();
            if (i < 0) {
                username = value;
            } else if (i == length) {
                username = value.substring(0, i);
            } else {
                username = value.substring(0, i);
                password = value.substring(i + 1, length);
            }
            configs.add(new AuthConfig(serverAddress, username, password));
        }

        encodedAuthConfigs = new HashMap<>(configs.size());

        for (AuthConfig authConfig : configs) {
            final AuthConfig copyAuthConfig = new AuthConfig(authConfig);
            copyAuthConfig.setServeraddress(null);
            encodedAuthConfigs.put(authConfig.getServeraddress(), Base64.encodeBase64String(JsonHelper.toJson(copyAuthConfig).getBytes()));
        }
    }

    public AuthConfigs(Set<AuthConfig> authConfigs) {
        encodedAuthConfigs = new HashMap<>(authConfigs.size());

        for (AuthConfig authConfig : authConfigs) {
            final AuthConfig copyAuthConfig = new AuthConfig(authConfig);
            copyAuthConfig.setServeraddress(null);
            encodedAuthConfigs.put(authConfig.getServeraddress(), Base64.encodeBase64String(JsonHelper.toJson(copyAuthConfig).getBytes()));
        }
    }

    public String getAuthHeader(String repositoryName) {
        final String authHeader = encodedAuthConfigs.get(parseIndexName(repositoryName));
        return authHeader == null ? "null" : authHeader;
    }

    private String parseIndexName(String repositoryName) {
        if (repositoryName == null) {
            return INDEX_SERVER_NAME;
        }
        if (repositoryName.contains("://")) {
            throw new IllegalArgumentException(String.format("Invalid repository name '%s'", repositoryName));
        }
        final int i = repositoryName.indexOf('/');
        if (i <= 0) {
            return INDEX_SERVER_NAME;
        }
        final String prefix = repositoryName.substring(0, i);
        if (prefix.indexOf('.') < 0 && prefix.indexOf(':') < 0 && !"localhost".equals(prefix)) {
            return INDEX_SERVER_NAME;
        }
        return prefix;
    }
}
