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

/**
 * @author andrew00x
 */
public interface LogMessageFormatter {
    String format(LogMessage logMessage);

    LogMessageFormatter DEFAULT = new LogMessageFormatter() {
        @Override
        public String format(LogMessage logMessage) {
            final StringBuilder sb = new StringBuilder();
            final LogMessage.Type type = logMessage.getType();
            switch (type) {
                case STDOUT:
                    sb.append("[STDOUT]");
                    break;
                case STDERR:
                    sb.append("[STDERR]");
                    break;
                case DOCKER:
                    sb.append("[DOCKER]");
                    break;
            }
            final String content = logMessage.getContent();
            if (content != null) {
                sb.append(' ');
                sb.append(content);
            }
            return sb.toString();

        }
    };

}
