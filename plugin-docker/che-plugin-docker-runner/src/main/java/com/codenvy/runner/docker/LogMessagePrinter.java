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
package com.codenvy.runner.docker;

import com.codenvy.docker.LogMessage;
import com.codenvy.docker.LogMessageFormatter;
import com.codenvy.docker.LogMessageProcessor;

import org.eclipse.che.api.core.util.LineConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author andrew00x
 */
public class LogMessagePrinter implements LogMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(LogMessagePrinter.class);

    private final LineConsumer        output;
    private final LogMessageFormatter formatter;

    public LogMessagePrinter(LineConsumer output, LogMessageFormatter formatter) {
        this.output = output;
        this.formatter = formatter;
    }

    public LogMessagePrinter(LineConsumer output) {
        this(output, LogMessageFormatter.DEFAULT);
    }

    @Override
    public void process(LogMessage logMessage) {
        try {
            output.writeLine(formatter.format(logMessage));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
