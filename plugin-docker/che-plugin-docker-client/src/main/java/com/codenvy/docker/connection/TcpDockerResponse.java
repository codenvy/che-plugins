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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
public class TcpDockerResponse implements DockerResponse {
    private final HttpURLConnection connection;

    TcpDockerResponse(HttpURLConnection connection) {
        this.connection = connection;
    }

    @Override
    public int getStatus() throws IOException {
        return connection.getResponseCode();
    }

    @Override
    public int getContentLength() throws IOException {
        return connection.getContentLength();
    }

    @Override
    public String getContentType() throws IOException {
        return connection.getContentType();
    }

    @Override
    public String getHeader(String name) throws IOException {
        return connection.getHeaderField(name);
    }

    @Override
    public String[] getHeaders(String name) throws IOException {
        final Map<String, List<String>> allHeaders = connection.getHeaderFields();
        final List<String> headers = allHeaders.get(name);
        return headers != null ? headers.toArray(new String[headers.size()]) : new String[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream entityStream = connection.getErrorStream();
        if (entityStream == null) {
            entityStream = connection.getInputStream();
        }
        return entityStream;
    }
}
