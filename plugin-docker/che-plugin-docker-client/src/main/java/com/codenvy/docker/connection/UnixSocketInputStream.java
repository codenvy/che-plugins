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

import com.codenvy.docker.CLibrary;
import com.sun.jna.LastErrorException;

import java.io.IOException;
import java.io.InputStream;

import static com.codenvy.docker.CLibraryFactory.getCLibrary;

/**
* @author andrew00x
*/
public class UnixSocketInputStream extends InputStream {
    private final int fd;
    private final CLibrary cLib = getCLibrary();

    UnixSocketInputStream(int fd) {
        this.fd = fd;
    }

    @Override
    public int read() throws IOException {
        final byte[] bytes = new byte[1];
        if (read(bytes) == 0) {
            return -1;
        }
        return bytes[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int n;
        try {
            n = cLib.recv(fd, b, len, 0);
        } catch (LastErrorException e) {
            throw new IOException("error: " + cLib.strerror(e.getErrorCode()));
        }
        if (n == 0) {
            return -1;
        }
        return n;
    }
}
