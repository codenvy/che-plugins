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

/**
 * Implementation of docker AuthConfig object
 * @see <a href="https://github.com/docker/docker/blob/v1.6.0/registry/auth.go#L29">source</a>
 * @author andrew00x
 */
public class AuthConfig {
    private String serveraddress;
    private String username;
    private String password;
    private String email;
    private String auth;


    public AuthConfig(String serveraddress, String username, String password, String email, String auth) {
        this.serveraddress = serveraddress;
        this.username = username;
        this.password = password;
        this.email = email;
        this.auth = auth;
    }

    public AuthConfig(String serveraddress, String username, String password, String email) {
        this.serveraddress = serveraddress;
        this.username = username;
        this.password = password;
        this.email = email;
        this.auth = "";
    }

    public AuthConfig(AuthConfig other) {
        this.serveraddress = other.serveraddress;
        this.username = other.username;
        this.password = other.password;
        this.email = other.email;
        this.auth = other.auth;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

}
