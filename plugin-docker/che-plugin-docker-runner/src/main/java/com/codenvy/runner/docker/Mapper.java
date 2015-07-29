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
package com.codenvy.runner.docker;

/**
 * Describes docker based environment for deploy an application. The following snippet is an example.
 * <pre>
 *     {
 *         "webPort":8080,
 *         "debugPort":8000,
 *         "bindApplicationDir":"/home/user/application",
 *         "description":"Tomcat7"
 *     }
 * </pre>
 * Valid keys and values for the <i>Mapper.json</i> file include the following:
 * <ul>
 * <li><b>webPort</b> - runner uses webPort value to connect the Docker container and route requests from the Internet to the user
 * application. This value may be omitted for other than web application.</li>
 * <li><b>debugPort</b> - runner uses debugPort value when application is running under debugger. This value may be omitted if debug is not
 * supported.</li>
 * <li><b>bindApplicationDir</b> - path for binding (mounting) application from hosted file system to the container file system.</li>
 * </ul>
 *
 * @author andrew00x
 */
public class Mapper {
    private int    webPort    = -1;
    private int    debugPort  = -1;
    private int    shellPort  = -1;
    private String bindApplicationDir;

    public int getWebPort() {
        return webPort;
    }

    public void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public int getShellPort() {
        return shellPort;
    }

    public void setShellPort(int shellPort) {
        this.shellPort = shellPort;
    }

    public String getBindApplicationDir() {
        return bindApplicationDir;
    }

    public void setBindApplicationDir(String bindApplicationDir) {
        this.bindApplicationDir = bindApplicationDir;
    }

    @Override
    public String toString() {
        return "DockerEnvironment{" +
               "webPort=" + webPort +
               ", debugPort=" + debugPort +
               ", shellPort=" + shellPort +
               ", bindApplicationDir='" + bindApplicationDir + '\'' +
               '}';
    }
}
