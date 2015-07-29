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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.util.SystemInfo;

import java.io.File;
import java.net.URI;

/**
 * @author Alexander Garagatyi
 */
public class DockerConnectorConfiguration {
    @Inject(optional = true)
    @Named("docker.client.daemon_url")
    private URI dockerDaemonUri = dockerDaemonUri();

    @Inject(optional = true)
    @Named("docker.client.certificates_folder")
    private String dockerCertificatesDirectoryPath = boot2dockerCertsDirectoryPath();

    @Inject
    private InitialAuthConfig authConfigs;

    @Inject
    public DockerConnectorConfiguration(InitialAuthConfig initialAuthConfig) {
        this.authConfigs = initialAuthConfig;
    }

    public DockerConnectorConfiguration(URI dockerDaemonUri,
                                        String dockerCertificatesDirectoryPath,
                                        InitialAuthConfig authConfigs) {
        this.authConfigs = authConfigs;
        this.dockerDaemonUri = dockerDaemonUri;
        this.dockerCertificatesDirectoryPath = dockerCertificatesDirectoryPath;
    }

    private static URI dockerDaemonUri() {
        return SystemInfo.isLinux() ? DockerConnector.UNIX_SOCKET_URI : DockerConnector.BOOT2DOCKER_URI;
    }

    private static String boot2dockerCertsDirectoryPath() {
        return SystemInfo.isLinux() ? null : DockerConnector.BOOT2DOCKER_CERTS_DIR;
    }

    public URI getDockerDaemonUri() {
        return dockerDaemonUri;
    }

    public InitialAuthConfig getAuthConfigs() {
        return authConfigs;
    }

    public DockerCertificates getDockerCertificates() {
        if (dockerCertificatesDirectoryPath == null || !getDockerDaemonUri().getScheme().equals("https")) {
            return null;
        }
        final File dockerCertificatesDirectory = new File(dockerCertificatesDirectoryPath);
        return dockerCertificatesDirectory.isDirectory() ? DockerCertificates.loadFromDirectory(dockerCertificatesDirectory) : null;
    }
}
