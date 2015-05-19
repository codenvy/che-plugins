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
package org.eclipse.che.plugin.docker.client.connection;

import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.plugin.docker.client.DockerCertificates;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class TcpConnection extends DockerConnection {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    private static final int DEFAULT_READ_TIMEOUT       = 60000;

    private final URI                baseUri;
    private final DockerCertificates certificates;
    private final int                connectionTimeout;
    private final int                readTimeout;

    private HttpURLConnection connection;

    public TcpConnection(URI baseUri) {
        this(baseUri, null, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public TcpConnection(URI baseUri, int connectionTimeout, int readTimeout) {
        this(baseUri, null, connectionTimeout, readTimeout);
    }

    public TcpConnection(URI baseUri, DockerCertificates certificates) {
        this(baseUri, certificates, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    @Inject
    public TcpConnection(@Named("docker.connection.tcp.base_uri") URI baseUri,
                         DockerCertificates certificates,
                         @Named("docker.connection.tcp.connection_timeout_millis") int connectionTimeoutMillisecond,
                         @Named("docker.connection.tcp.read_timeout_millis") int readTimeoutMillisecond) {
        if ("https".equals(baseUri.getScheme())) {
            if (certificates == null) {
                throw new IllegalArgumentException("Certificates are required for https connection.");
            }
        } else if (!("http".equals(baseUri.getScheme()))) {
            throw new IllegalArgumentException(String.format("Invalid URL '%s', only http and https protocols are supported.", baseUri));
        }
        this.baseUri = baseUri;
        this.certificates = certificates;
        this.connectionTimeout = connectionTimeoutMillisecond;
        this.readTimeout = readTimeoutMillisecond;
    }

    @Override
    protected DockerResponse request(String method, String path, List<Pair<String, ?>> headers, Entity entity) throws IOException {
        final URL url = baseUri.resolve(path).toURL();
        final String protocol = url.getProtocol();
        connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        if ("https".equals(protocol)) {
            ((HttpsURLConnection)connection).setSSLSocketFactory(certificates.getSslContext().getSocketFactory());
        }
        connection.setRequestMethod(method);
        for (Pair<String, ?> header : headers) {
            connection.setRequestProperty(header.first, String.valueOf(header.second));
        }
        if (entity != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                entity.writeTo(output);
            }
        }
        return new TcpDockerResponse(connection);
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
