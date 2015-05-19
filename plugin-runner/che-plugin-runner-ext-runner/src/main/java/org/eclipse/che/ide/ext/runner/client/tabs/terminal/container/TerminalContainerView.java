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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.container;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

import javax.annotation.Nonnull;

/**
 * The abstract representation of console container widget UI part.
 *
 * @author Valeriy Svydenko
 */
@ImplementedBy(TerminalContainerViewImpl.class)
public interface TerminalContainerView extends View<TerminalContainerView.ActionDelegate> {

    /**
     * Add a given widget in the special place in the container.
     *
     * @param terminal
     *         terminal that needs to be shown
     */
    void addWidget(@Nonnull IsWidget terminal);

    /**
     * Remove a given widget from the container.
     *
     * @param terminal
     *         terminal that needs to be remove
     */
    void removeWidget(@Nonnull IsWidget terminal);

    /**
     * Changes visibility of the widget.
     *
     * @param visible
     *         visible state that needs to be applied
     */
    void setVisible(boolean visible);


    /**
     * Changes visibility of the dummy label.
     *
     * @param visible
     *         visible state that needs to be applied
     */
    void setVisibleDummyContent(boolean visible);

    interface ActionDelegate {
    }

}