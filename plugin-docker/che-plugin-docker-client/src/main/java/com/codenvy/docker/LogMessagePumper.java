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

import com.google.common.io.ByteStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author andrew00x
 */
class LogMessagePumper {
    private static final Logger LOG = LoggerFactory.getLogger(LogMessagePumper.class);

    private static final int STREAM_HEADER_LENGTH = 8;
    private static final int MAX_LINE_LENGTH      = 1024;

    private final InputStream         source;
    private final LogMessageProcessor target;

    LogMessagePumper(InputStream source, LogMessageProcessor target) {
        this.source = source;
        this.target = target;
    }

    void start() throws IOException {
        final byte[] buf = new byte[MAX_LINE_LENGTH];
        StringBuilder lineBuf = null;
        boolean endOfLine = false;
        LogMessage.Type logMessageType = LogMessage.Type.DOCKER;
        for (; ; ) {
            int r = ByteStreams.read(source, buf, 0, STREAM_HEADER_LENGTH);
            if (r != 8) {
                if (r != -1) {
                    LOG.debug("Invalid stream, can't read header. Header of each frame must contain 8 bytes but got {}", r);
                }
                if (lineBuf != null && lineBuf.length() > 0) {
                    target.process(new LogMessage(logMessageType, lineBuf.toString()));
                    lineBuf.setLength(0);
                }
                break;
            }
            logMessageType = getLogMessageType(buf);
            int remaining = getPayloadLength(buf);
            while (remaining > 0) {
                r = source.read(buf, 0, Math.min(remaining, buf.length));
                int offset = 0;
                int lineLength = lineBuf != null ? lineBuf.length() : 0;
                for (int i = 0; i < r; i++, lineLength++) {
                    endOfLine = false;
                    if (buf[i] == '\n' || buf[i] == '\r' || lineLength > MAX_LINE_LENGTH) {
                        if (lineBuf != null && lineBuf.length() > 0) {
                            lineBuf.append(new String(buf, offset, i - offset));
                            target.process(new LogMessage(logMessageType, lineBuf.toString()));
                            lineBuf.setLength(0);
                        } else {
                            target.process(new LogMessage(logMessageType, new String(buf, offset, i - offset)));
                        }
                        offset = i + 1;
                        lineLength = 0;
                        endOfLine = true;
                    }
                }
                if (!endOfLine) {
                    if (lineBuf == null) {
                        lineBuf = new StringBuilder(MAX_LINE_LENGTH);
                    }
                    lineBuf.append(new String(buf, offset, r - offset));
                }
                remaining -= r;
            }
        }
    }

    private int getPayloadLength(byte[] header) {
        return (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
    }

    private LogMessage.Type getLogMessageType(byte[] header) {
        switch (header[0]) {
            case 0:
                return LogMessage.Type.STDIN;
            case 1:
                return LogMessage.Type.STDOUT;
            case 2:
                return LogMessage.Type.STDERR;
            default:
                throw new IllegalArgumentException(String.format("Invalid docker stream type %d", header[0]));
        }
    }
}
