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
package com.codenvy.docker.connection;

import com.codenvy.docker.DockerCertificates;

import org.eclipse.che.commons.lang.Pair;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * @author andrew00x
 */
public class TcpConnection extends DockerConnection {
    private final URI             baseUri;
    private final DockerCertificates certificates;

    private HttpURLConnection connection;

    public TcpConnection(URI baseUri) {
        this(baseUri, null);
    }

    public TcpConnection(URI baseUri, DockerCertificates certificates) {
        if ("https".equals(baseUri.getScheme())) {
            if (certificates == null) {
                throw new IllegalArgumentException("Certificates are required for https connection.");
            }
        } else if (!("http".equals(baseUri.getScheme()))) {
            throw new IllegalArgumentException(String.format("Invalid URL '%s', only http and https protocols are supported.", baseUri));
        }
        this.baseUri = baseUri;
        this.certificates = certificates;
    }

    @Override
    protected DockerResponse request(String method, String path, List<Pair<String, ?>> headers, Entity entity) throws IOException {
        final URL url = baseUri.resolve(path).toURL();
        final String protocol = url.getProtocol();
        connection = (HttpURLConnection)url.openConnection();
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
