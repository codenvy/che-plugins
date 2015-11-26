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
package org.eclipse.che.ide.ext.openshift.client.project.wizard;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.api.mvp.View;

/**
 * View for {@link CreateProjectPresenter}.
 *
 * @author Vlad Zhukovskiy
 */
@ImplementedBy(CreateProjectViewImpl.class)
public interface CreateProjectView extends View<CreateProjectView.ActionDelegate> {

    /**
     * Shows specified wizard page.
     */
    void showPage(Presenter presenter);

    /**
     * Shows wizard.
     */
    void showWizard();

    /**
     * Hides wizard.
     */
    void closeWizard();

    /**
     * Enables next wizard page button.
     */
    void setNextButtonEnabled(boolean enabled);

    /**
     * Enables previous wizard page button.
     */
    void setPreviousButtonEnabled(boolean enabled);

    /**
     * Enables create wizard page button.
     */
    void setCreateButtonEnabled(boolean enabled);

    /**
     * Animates create button.
     *
     * @param animate
     *         is button animated
     */
    void animateCreateButton(boolean animate);

    /**
     * Blocks the view and does not allow it to be closed.
     *
     * @param blocked
     *         blocked or not
     */
    void setBlocked(boolean blocked);

    /**
     * Handles operations from the view.
     */
    interface ActionDelegate {
        /**
         * Perform operations when next button clicked.
         */
        void onNextClicked();

        /**
         * Perform operations when previous button clicked.
         */
        void onPreviousClicked();

        /**
         * Perform operations when create button clicked.
         */
        void onCreateClicked();
    }
}
