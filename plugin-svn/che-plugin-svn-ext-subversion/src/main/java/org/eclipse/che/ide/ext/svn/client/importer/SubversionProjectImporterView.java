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
package org.eclipse.che.ide.ext.svn.client.importer;

import org.eclipse.che.ide.api.mvp.View;
import com.google.inject.ImplementedBy;

/**
 * View interface for the Subversion project importer.
 */
@ImplementedBy(SubversionProjectImporterViewImpl.class)
public interface SubversionProjectImporterView extends View<SubversionProjectImporterView.ActionDelegate> {

    public interface ActionDelegate {
        /** Performs any actions appropriate in response to the user having changed the project's name. */
        void projectNameChanged(final String name);

        /** Performs any actions appropriate in response to the user having changed the project's URL. */
        void projectUrlChanged(final String url);

        /** Performs any actions appropriate in response to the user having changed the project's description. */
        void projectDescriptionChanged(final String projectDescriptionValue);

        /** Performs any actions appropriate in response to the user having changed the project's visibility. */
        void projectVisibilityChanged(final boolean aPublic);

        /** Performs any actions appropriate in response to the user having changed the relative path in the project. */
        void projecRelativePathChanged(String value);

        void credentialsChanged(String value, String value2);
    }

    /** Reset the page. */
    void reset();

    /** Show the name error. */
    void showNameError();

    /** Hide the name error. */
    void hideNameError();

    /** Show URL error. */
    void showUrlError( String message);

    /** Hide URL error. */
    void hideUrlError();

    /**
     * @param text the project description
     */
    void setProjectDescription(final String text);

    /**
     * @param url the project's URL
     *
     */
    void setProjectUrl(final String url);

    /**
     * Returns the project URL.
     * 
     * @return the project URL
     */
    String getProjectUrl();

    /**
     * @return the project's name
     */
    String getProjectName();

    /**
     * @param visible the project's visibility
     */
    void setProjectVisibility(final boolean visible);

    /**
     * @param name the project's name
     */
    void setProjectName(final String name);

    /** Give focus to project's URL input. */
    void focusInUrlInput();

    /**
     * Set the enable state of the inputs.
     *
     * @param isEnabled
     *         <code>true</code> if enabled, <code>false</code> if disabled
     */
    void setInputsEnableState(final boolean isEnabled);

    /**
     * Return the project branch to import.
     * 
     * @return the branch
     */
    String getProjectRelativePath();

    /**
     * Sets the relative path in the project repository.
     * 
     * @param projectRelativePath the path
     */
    void setProjectRelativePath(String projectRelativePath);
}
