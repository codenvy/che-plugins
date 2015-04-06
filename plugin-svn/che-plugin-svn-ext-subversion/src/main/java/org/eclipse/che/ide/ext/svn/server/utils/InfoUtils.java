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
package org.eclipse.che.ide.ext.svn.server.utils;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class InfoUtils {

    private static final String KEY_PATH = "Path";
    private static final String KEY_NAME = "Name";
    private static final String KEY_URL = "URL";
    private static final String KEY_REPOSITORY_ROOT = "Repository Root";
    private static final String KEY_REPOSITORY_UUID = "Repository UUID";
    private static final String KEY_REVISION = "Revision";
    private static final String KEY_NODE_KIND = "Node Kind";
    private static final String KEY_LAST_CHANGE_AUTHOR = "Last Changed Author";
    private static final String KEY_LAST_CHANGE_REVISION = "Last Changed Rev";
    private static final String KEY_LAST_CHANGE_DATE = "Last Changed Date";

    private static final String STARTSWITH_PATTERN = "^{0}: (.*)$";

    private InfoUtils() {
    }

    public static String getPath(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_PATH));
        return searchPattern(infoOutput, pattern);
    }

    private static String searchPattern(final List<String> infoOutput, final Pattern pattern) {
        for (String line: infoOutput) {
            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                final String value = matcher.group(1);
                return value;
            } else {
                continue;
            }
        }
        return null;
    }

    public static String getName(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_NAME));
        return searchPattern(infoOutput, pattern);
    }

    public static String getRepositoryUrl(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_URL));
        return searchPattern(infoOutput, pattern);
    }

    public static String getRepositoryRootUrl(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_REPOSITORY_ROOT));
        return searchPattern(infoOutput, pattern);
    }

    public static String getRepositoryUUID(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_REPOSITORY_UUID));
        return searchPattern(infoOutput, pattern);
    }

    public static String getRevision(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_REVISION));
        return searchPattern(infoOutput, pattern);
    }

    public static String getNodeKind(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_NODE_KIND));
        return searchPattern(infoOutput, pattern);
    }

    public static String getLastChangeAuthor(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_LAST_CHANGE_AUTHOR));
        return searchPattern(infoOutput, pattern);
    }

    public static String getLastChangeRevision(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_LAST_CHANGE_REVISION));
        return searchPattern(infoOutput, pattern);
    }

    public static String getLastChangeDate(final List<String> infoOutput) {
        final Pattern pattern = Pattern.compile(MessageFormat.format(STARTSWITH_PATTERN, KEY_LAST_CHANGE_DATE));
        return searchPattern(infoOutput, pattern);
    }

}
