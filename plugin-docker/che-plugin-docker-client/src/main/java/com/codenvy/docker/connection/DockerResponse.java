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

/**
 * @author andrew00x
 */
public interface DockerResponse {
    int getStatus() throws IOException;

    int getContentLength() throws IOException;

    String getContentType() throws IOException;

    String getHeader(String name) throws IOException;

    String[] getHeaders(String name) throws IOException;

    InputStream getInputStream() throws IOException;
}
