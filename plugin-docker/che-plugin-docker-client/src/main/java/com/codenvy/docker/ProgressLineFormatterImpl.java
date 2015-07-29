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

import com.codenvy.docker.json.ProgressStatus;

/**
 * Default implementation of {@link ProgressLineFormatter}
 *
 * @author Alexander Garagatyi
 */
public class ProgressLineFormatterImpl implements ProgressLineFormatter {
    @Override
    public String format(ProgressStatus progressStatus) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[DOCKER] ");
        final String stream = progressStatus.getStream();
        final String status = progressStatus.getStatus();
        final String error = progressStatus.getError();
        if (error != null) {
            sb.append("[ERROR] ");
            sb.append(error);
        } else if (stream != null) {
            sb.append(stream.trim());
        } else if (status != null) {
            final String id = progressStatus.getId();
            if (id != null) {
                sb.append(id);
                sb.append(':');
                sb.append(' ');
            }
            sb.append(status);
            sb.append(' ');
            sb.append(parseProgressText(progressStatus));
        }
        return sb.toString();
    }

    /**
     * Parses text data from progress string. Typical progress string from docker API:
     * {@code [==================================&gt;                ]  9.13 MB/13.38 MB 19s}
     * This method gets text part from progress string, e.g. {@code 9.13 MB/13.38 MB 19s}
     *
     * @return text data from progress string or empty string if progress string is {@code null} or doesn't contains text date
     */
    protected String parseProgressText(ProgressStatus progressStatus) {
        // "
        final String rawProgress = progressStatus.getProgress();
        if (rawProgress == null) {
            return "";
        }
        // skip progress indicator: ==================================>                ]
        final int l = rawProgress.length();
        int n = 0;
        while (n < l && rawProgress.charAt(n) != '[') {
            n++;
        }
        int p = n;
        while (p < l && rawProgress.charAt(p) != ']') {
            p++;
        }
        if (p == n) {
            // unexpected string
            return "";
        }
        ++p;
        while (p < l && Character.isWhitespace(rawProgress.charAt(p))) {
            p++;
        }
        if (p == l) {
            // unexpected string
            return "";
        }
        return rawProgress.substring(p);
    }
}
