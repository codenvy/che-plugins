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
import java.io.OutputStream;

import static com.codenvy.docker.CLibraryFactory.getCLibrary;

/**
* @author andrew00x
*/
public class UnixSocketOutputStream extends OutputStream {
    private final int fd;
    private final CLibrary cLib = getCLibrary();

    UnixSocketOutputStream(int fd) {
        this.fd = fd;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b}, 0, 1);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int n;
        try {
            n = cLib.send(fd, b, len, 0);
        } catch (LastErrorException e) {
            throw new IOException("error: " + cLib.strerror(e.getErrorCode()));
        }
        if (n != len) {
            throw new IOException(String.format("Failed writing %d bytes", len));
        }
    }
}
