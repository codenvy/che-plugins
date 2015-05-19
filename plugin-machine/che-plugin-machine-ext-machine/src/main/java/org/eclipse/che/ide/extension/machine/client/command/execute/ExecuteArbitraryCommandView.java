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
package org.eclipse.che.ide.extension.machine.client.command.execute;

import org.eclipse.che.ide.api.mvp.View;

import javax.annotation.Nonnull;

/**
 * The view of {@link ExecuteArbitraryCommandPresenter}.
 *
 * @author Artem Zatsarynnyy
 */
public interface ExecuteArbitraryCommandView extends View<ExecuteArbitraryCommandView.ActionDelegate> {

    /** @return entered command */
    @Nonnull
    String getCommand();

    /**
     * Set content into buildCommand field.
     *
     * @param message
     *         text what need to insert
     */
    void setCommand(@Nonnull String message);

    /** Close view. */
    void close();

    /** Show view. */
    void show();

    /** Needs for delegate some function into Commit view. */
    interface ActionDelegate {
        /** Performs any actions appropriate in response to the user having pressed the Commit button. */
        void onExecuteClicked();

        /** Performs any actions appropriate in response to the user having pressed the Cancel button. */
        void onCancelClicked();
    }
}
