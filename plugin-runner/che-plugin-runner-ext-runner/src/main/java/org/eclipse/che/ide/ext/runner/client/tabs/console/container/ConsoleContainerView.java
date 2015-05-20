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
package org.eclipse.che.ide.ext.runner.client.tabs.console.container;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

import javax.annotation.Nonnull;

/**
 * The abstract representation of console container widget UI part.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
@ImplementedBy(ConsoleContainerViewImpl.class)
public interface ConsoleContainerView extends View<ConsoleContainerView.ActionDelegate> {

    /**
     * Show a given widget in the special place in the container.
     *
     * @param console
     *         console that needs to be shown
     */
    void showWidget(@Nonnull IsWidget console);

    /**
     * Remove a given widget from the container.
     *
     * @param console
     *         widget that needs to be remove
     */
    void removeWidget(@Nonnull IsWidget console);

    /**
     * Changes visibility of the widget.
     *
     * @param visible
     *         visible state that needs to be applied
     */
    void setVisible(boolean visible);

    /**
     * Changes visibility of the no runner label.
     *
     * @param visible
     *         visible state that needs to be applied
     */
    void setVisibleNoRunnerLabel(boolean visible);

    /**
     * Select 'Wrap Text' button.
     *
     * @param isChecked
     *         selection state of button
     */
    void selectWrapTextButton(boolean isChecked);

    interface ActionDelegate {
        /** Performs some actions in response to user's clicking on 'Wrap Text' button. */
        void onWrapTextClicked();

        /** Performs some actions in response to user's clicking on 'Scroll bottom' button. */
        void onScrollBottomClicked();

        /** Performs some actions in response to user's clicking on 'Clean' button. */
        void onCleanClicked();
    }

}