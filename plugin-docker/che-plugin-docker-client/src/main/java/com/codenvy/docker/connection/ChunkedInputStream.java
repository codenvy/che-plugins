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
public class ChunkedInputStream extends InputStream {
    private final InputStream input;
    private StringBuilder chunkSizeBuf;
    private int           chunkSize;
    private int           chunkPos;
    private boolean       eof;

    ChunkedInputStream(InputStream input) {
        this.input = input;
        chunkSizeBuf = new StringBuilder();
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

    @Override
    public synchronized int available() {
        return (chunkSize - chunkPos);
    }

    private int doRead(byte[] b, int off, int len) throws IOException {
        if (eof) {
            return -1;
        }
        if (chunkSize == 0) {
            chunkPos = 0;
            for (; ; ) {
                int i = input.read();
                if (i < 0) {
                    throw new IOException("Can't read size of chunk");
                }
                if (i == '\n') {
                    break;
                }
                chunkSizeBuf.append((char)i);
            }

            int l = chunkSizeBuf.length();
            int endSize = 0;
            while (endSize < l && Character.digit(chunkSizeBuf.charAt(endSize), 16) != -1) {
                endSize++;
            }
            try {
                chunkSize = Integer.parseInt(chunkSizeBuf.substring(0, endSize), 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size");
            }
            chunkSizeBuf.setLength(0);
            if (chunkSize == 0) {
                eof = true;
            }
        }
        final int n = input.read(b, 0, Math.min(len - off, chunkSize - chunkPos));
        chunkPos += n;
        if (chunkPos == chunkSize) {
            if ('\r' != input.read()) { // skip '\r'
                throw new IOException("CR character is missing");
            }
            if ('\n' != input.read()) { // skip '\n'
                throw new IOException("LF character is missing");
            }
            chunkSize = 0;
            chunkPos = 0;
        }
        if (eof) {
            return -1;
        }
        return n;
    }
}
