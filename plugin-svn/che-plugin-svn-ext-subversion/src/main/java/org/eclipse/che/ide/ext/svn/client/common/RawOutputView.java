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
package org.eclipse.che.ide.ext.svn.client.common;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;


/**
 * View for {@link RawOutputPresenter}.
 */
public interface RawOutputView extends View<RawOutputView.ActionDelegate> {

    /**
     * Action handler for the view actions/controls.
     */
    public interface ActionDelegate extends BaseActionDelegate {
        /**
         * Handle the clear button.
         */
        void onClearClicked();
    }

    /**
     * Print text in view.
     *
     * @param text The text to display
     */
    void print(final String text);

    /**
     * Set title of view.
     *
     * @param title The title to display
     */
    void setTitle(final String title);

    /**
     * Clear console. Remove all messages.
     */
    void clear();

    /**
     * Scroll to bottom of the view.
     */
    void scrollBottom();

    void printInfo(String text);

    void print(String text, String color);

//    void printBold(String text);
}
