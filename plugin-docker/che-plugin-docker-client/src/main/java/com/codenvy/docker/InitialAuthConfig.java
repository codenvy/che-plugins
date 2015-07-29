/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.docker;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.inject.ConfigurationProperties;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static org.eclipse.che.commons.lang.Strings.isNullOrEmpty;

/**
 * Collects auth configurations for private docker registries. Credential might be configured in .properties files, see details {@link
 * org.eclipse.che.inject.CodenvyBootstrap}. Credentials configured as (key=value) pairs. Key is string that starts with prefix
 * {@code docker.registry.auth.} followed by url and credentials of docker registry server.
 * <pre>{@code
 * docker.registry.auth.url=localhost:5000
 * docker.registry.auth.username=user1
 * docker.registry.auth.password=pass
 * docker.registry.auth.email=user1@email.com
 * }</pre>
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class InitialAuthConfig {
    private static final String CONFIGURATION_PREFIX         = "docker.registry.auth.";
    private static final String CONFIGURATION_PREFIX_PATTERN = "docker\\.registry\\.auth\\..+";

    AuthConfig predefinedConfig;

    @Inject
    InitialAuthConfig(ConfigurationProperties configurationProperties) {
        String serverAddress = "https://index.docker.io/v1/";
        String username = null, password = null, email = null;
        for (Map.Entry<String, String> e : configurationProperties.getProperties(CONFIGURATION_PREFIX_PATTERN).entrySet()) {
            final String classifier = e.getKey().replaceFirst(CONFIGURATION_PREFIX, "");
            switch (classifier) {
                case "url": {
                    serverAddress = e.getValue();
                    break;
                }
                case "email": {
                    email = e.getValue();
                    break;
                }
                case "username": {
                    username = e.getValue();
                    break;
                }
                case "password": {
                    password = e.getValue();
                    break;
                }
            }
        }
        if (!isNullOrEmpty(serverAddress) && !isNullOrEmpty(username) && !isNullOrEmpty(password) && !isNullOrEmpty(email)) {
            predefinedConfig = new AuthConfig(serverAddress, username, password, email);
        }
    }


    public String getAuthConfigHeader() {
        if (predefinedConfig != null) {
            return Base64.encodeBase64String(JsonHelper.toJson(predefinedConfig).getBytes());
        } else {
            return "";
        }
    }

    public AuthConfig getInitialAuthConfig() {
        if (predefinedConfig != null) {
            return predefinedConfig;
        } else {
            return null;
        }
    }

    public AuthConfigs getAuthConfigs() {
        AuthConfigs authConfigs = new AuthConfigs();
        if (predefinedConfig != null) {
            authConfigs.addConfig(predefinedConfig);
        }
        return authConfigs;
    }
}
