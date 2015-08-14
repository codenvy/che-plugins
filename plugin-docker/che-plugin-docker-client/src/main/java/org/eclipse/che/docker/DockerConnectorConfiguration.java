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
package org.eclipse.che.docker;

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
