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
package org.eclipse.che.ide.ext.svn.server;


import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks Subversion configuration to ensure properly work SVN-extension in Codenvy.
 * <p/>
 * Here we check that we have correct setting for svn ignore.
 * Must be ignored files:
 * <pre>.codenvy
 *  .vfs</pre>
 * like system files for Codenvy.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class SubversionConfigurationChecker {

    private static final Set<String> SUBVERSION_IGNORE_PATTERNS = new LinkedHashSet<>();

    static {
        SUBVERSION_IGNORE_PATTERNS.add(".codenvy");
        SUBVERSION_IGNORE_PATTERNS.add(".vfs");
    }

    private static final Logger LOG = LoggerFactory.getLogger(SubversionConfigurationChecker.class);

    private final Path GLOBAL_SUBVERSION_CONFIG_FILE_PATH;

    protected SubversionConfigurationChecker() {
        GLOBAL_SUBVERSION_CONFIG_FILE_PATH = Paths.get(System.getProperty("user.home") + "/.subversion/config");
    }

    // Constructor need for unit-tests
    protected SubversionConfigurationChecker(Path subversionConfigFile) {
        GLOBAL_SUBVERSION_CONFIG_FILE_PATH = subversionConfigFile;
    }

    @PostConstruct
    public void start() {
        try {
            ensureExistingSvnConfigFile();
            checkAndUpdateConfigFile();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    protected Path getLoadedConfigFile() {
        return GLOBAL_SUBVERSION_CONFIG_FILE_PATH;
    }

    protected void ensureExistingSvnConfigFile() throws IOException {
        if (Files.notExists(GLOBAL_SUBVERSION_CONFIG_FILE_PATH)) {
            Files.createFile(GLOBAL_SUBVERSION_CONFIG_FILE_PATH);
        }
    }

    protected void checkAndUpdateConfigFile() throws IOException {
        List<String> subversionConfigContent = Files.readAllLines(GLOBAL_SUBVERSION_CONFIG_FILE_PATH, Charset.forName("UTF-8"));

        int miscellanySectionIndex = -1;
        int globalIgnoresIndex = -1;
        boolean ignoreSectionCommented = true;

        for (int i = 0; i < subversionConfigContent.size(); i++) {
            String line = subversionConfigContent.get(i);

            if (line.startsWith("[miscellany]")) {
                miscellanySectionIndex = i;
            } else if (line.trim().startsWith("# global-ignores")) {
                globalIgnoresIndex = i;
                ignoreSectionCommented = true;
                break;
            } else if (line.trim().startsWith("global-ignores")) {
                globalIgnoresIndex = i;
                ignoreSectionCommented = false;
                break;
            }
        }

        if (miscellanySectionIndex == -1) { //if this section doesn't exist
            subversionConfigContent.add("[miscellany]");
            subversionConfigContent.add("global-ignores = " + getDefaultExcludes());
        } else if (globalIgnoresIndex == -1) { //in case if misc section exists but config parameter isn't
            subversionConfigContent.add(miscellanySectionIndex, "global-ignores = " + getDefaultExcludes());
        } else {
            String configParameter = subversionConfigContent.get(globalIgnoresIndex);
            if (ignoreSectionCommented) {
                configParameter = configParameter.substring(2).concat(" ").concat(getDefaultExcludes());
            } else {
                Iterable<String> filteredExcludes = filterExistingExcludes(configParameter, SUBVERSION_IGNORE_PATTERNS);
                configParameter = configParameter.concat(" ").concat(Joiner.on(" ").join(filteredExcludes));
            }

            subversionConfigContent.set(globalIgnoresIndex, configParameter);
        }

        updateConfigFile(Joiner.on('\n').join(subversionConfigContent), GLOBAL_SUBVERSION_CONFIG_FILE_PATH);
    }

    protected void updateConfigFile(String content, Path configFile) throws IOException {
        Files.write(configFile, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    protected String getDefaultExcludes() {
        return Joiner.on(' ').join(SUBVERSION_IGNORE_PATTERNS);
    }

    private Iterable<String> filterExistingExcludes(String configLine, final Set<String> defaultExcludes) {
        if (!configLine.contains("=")) {
            throw new IllegalStateException("Wrong configuration parameter");
        }

        final String values = configLine.substring(configLine.indexOf('=') + 1).trim();
        if (values.isEmpty()) {
            return defaultExcludes;
        }

        return Iterables.filter(defaultExcludes, new Predicate<String>() {
            @Override
            public boolean apply(String defValue) {
                List<String> existValues = Splitter.on(' ').splitToList(values);
                return !existValues.contains(defValue);
            }
        });
    }
}
