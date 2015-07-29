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
