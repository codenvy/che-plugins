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
package com.codenvy.docker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of docker model ConfigFile object
 * @see <a href="https://github.com/docker/docker/blob/v1.6.0/registry/auth.go#L37">source</a>
 * @author Max Shaposhnik
 *
 */
public class AuthConfigs {

    private Map<String, AuthConfig> configs;
    private String                  rootPath; // not yet used


    public AuthConfigs() {
        this.configs = new HashMap<>();
    }

    public void addConfig(AuthConfig authConfig) {
        this.configs.put(authConfig.getServeraddress(), authConfig);
    }

    public Map<String, AuthConfig> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    //for json conversion
    public void setConfigs(Map<String, AuthConfig> configs) {
        this.configs = configs;
    }
}
