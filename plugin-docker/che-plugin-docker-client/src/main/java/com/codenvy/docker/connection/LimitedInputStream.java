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
public class LimitedInputStream extends InputStream {
    private final InputStream input;
    private final int         limit;

    private int pos;

    LimitedInputStream(InputStream input, int limit) {
        this.input = input;
        this.limit = limit;
    }

    @Override
    public synchronized int read() throws IOException {
        final byte[] b = new byte[1];
        if (doRead(b, 0, 1) == -1) {
            return -1;
        }
        return b[0];
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        return doRead(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        return doRead(b, 0, len);
    }

    private int doRead(byte[] b, int off, int len) throws IOException {
        if (pos >= limit) {
            return -1;
        }
        int n = input.read(b, 0, Math.min(len - off, limit - pos));
        pos += n;
        return n;
    }
}
