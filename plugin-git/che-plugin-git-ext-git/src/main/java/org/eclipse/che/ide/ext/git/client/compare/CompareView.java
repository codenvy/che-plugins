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
package org.eclipse.che.ide.ext.git.client.compare;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

/**
 * The view of {@link ComparePresenter}.
 *
 * @author Igor Vinokur
 */
@ImplementedBy(CompareViewImpl.class)
interface CompareView extends View<CompareView.ActionDelegate> {
    /**
     * Set a title of the window.
     *
     * @param title the name of the compare window
     */
    void setTitle(String title);

    /** Hide compare panel. */
    void hide();

    /**
     * Show compare panel with specified contents.
     * 
     * @param oldContent content from specified working tree
     * 
     * @param newContent content of current file
     * 
     * @param revision revision or branch which is getting part in comparing
     * 
     * @param fileName the name of the changed file
     */

    void show(String oldContent, String newContent, String revision, String fileName);

    interface ActionDelegate {
        /** Performs some actions in response to user's clicking on the 'Close' button. */
        void onCloseButtonClicked();
    }
}
