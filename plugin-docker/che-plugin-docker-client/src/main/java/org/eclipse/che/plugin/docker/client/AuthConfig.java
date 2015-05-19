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

/**
 * @author andrew00x
 */
public class AuthConfig {
    private String serveraddress;
    private String username;
    private String password;

    public AuthConfig(String serveraddress, String username, String password) {
        this.serveraddress = serveraddress;
        this.username = username;
        this.password = password;
    }

    public AuthConfig(AuthConfig other) {
        this.serveraddress = other.serveraddress;
        this.username = other.username;
        this.password = other.password;
    }

    public AuthConfig() {
    }

    public String getServeraddress() {
        return serveraddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setServeraddress(String serveraddress) {
        this.serveraddress = serveraddress;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
