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
package org.eclipse.che.ide.ext.git.client.compare.changedList;

import org.eclipse.che.ide.api.mvp.View;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The view of {@link ChangedListPresenter}.
 *
 * @author Igor Vinokur
 */
public interface ChangedListView extends View<ChangedListView.ActionDelegate> {
    /** Needs for delegate some function into Changed list view. */
    interface ActionDelegate {
        /** Performs any actions appropriate in response to the user having pressed the Close button. */
        void onCloseClicked();

        /** Performs any actions appropriate in response to the user having pressed the Compare button. */
        void onCompareClicked();

        /**
         * Performs any action in response to the user having select file.
         *
         * @param file selected file
         */
        void onFileSelected(@NotNull String file);

        /** Performs any action in response to the user do not have any selected branch. */
        void onFileUnselected();
    }

    /**
     * Set changed files.
     *
     * @param files list of changed files
     */
    void setChanges(@NotNull List<String> files);

    /** Close dialog. */
    void close();

    /** Show dialog. */
    void showDialog();

    /**
     * Change the enable state of the compare button.
     *
     * @param enabled <code>true</code> to enable the button, <code>false</code> to disable it
     */
    void setEnableCompareButton(boolean enabled);
}