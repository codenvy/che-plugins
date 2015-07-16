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
package org.eclipse.che.ide.ext.java.client.projecttree;

import org.eclipse.che.ide.api.project.tree.TreeSettings;

/**
 * The settings for the {@link JavaTreeStructure}.
 *
 * @author Artem Zatsarynnyy
 */
public class JavaTreeSettings implements TreeSettings {
    private boolean showHiddenItems;
    private boolean showExtensionForJavaFiles;
    private boolean compactEmptyPackages  = true;
    private boolean showExternalLibraries = true;

    @Override
    public boolean isShowHiddenItems() {
        return showHiddenItems;
    }

    @Override
    public void setShowHiddenItems(boolean showHiddenItems) {
        this.showHiddenItems = showHiddenItems;
    }

    /**
     * Checks if extension for java-files should be shown or not.
     *
     * @return {@code true} - if extension for java-files should be shown, {@code false} - otherwise
     */
    public boolean isShowExtensionForJavaFiles() {
        return showExtensionForJavaFiles;
    }

    /**
     * Sets whether extension for java-files should be shown or not.
     *
     * @param showExtensionForJavaFiles
     *         {@code true} - extension for java-files should be shown, {@code false} - otherwise
     */
    public void setShowExtensionForJavaFiles(boolean showExtensionForJavaFiles) {
        this.showExtensionForJavaFiles = showExtensionForJavaFiles;
    }

    /**
     * Checks if 'empty' packages should be shown as compacted.
     *
     * @return {@code true} - if 'empty' packages should be compacted, {@code false} - otherwise
     */
    public boolean isCompactEmptyPackages() {
        return compactEmptyPackages;
    }

    /**
     * Sets whether 'empty' packages should be shown as compacted or not.
     *
     * @param compactEmptyPackages
     *         {@code true} - if 'empty' packages should be shown as compacted, {@code false} - otherwise
     */
    public void setCompactEmptyPackages(boolean compactEmptyPackages) {
        this.compactEmptyPackages = compactEmptyPackages;
    }

    /**
     * Checks if 'External Libraries' should be shown.
     *
     * @return {@code true} - if 'External libraries' should be shown, {@code false} - otherwise
     */
    public boolean isShowExternalLibraries() {
        return showExternalLibraries;
    }

    /**
     * Sets whether 'External Libraries' should be shown or not.
     *
     * @param showExternalLibraries
     *         {@code true} - if 'External Libraries' should be shown, {@code false} - otherwise
     */
    public void setShowExternalLibraries(boolean showExternalLibraries) {
        this.showExternalLibraries = showExternalLibraries;
    }
}
