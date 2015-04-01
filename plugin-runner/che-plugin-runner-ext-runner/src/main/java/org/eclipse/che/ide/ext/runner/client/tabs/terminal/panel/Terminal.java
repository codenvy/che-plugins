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
package org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel;

import com.google.gwt.user.client.ui.IsWidget;

import org.eclipse.che.ide.ext.runner.client.models.Runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The widget that provides an ability to work like terminal. It contains methods for updating visual components.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
public interface Terminal extends IsWidget {
    /**
     * Updates widget components from a given runner.
     *
     * @param runner
     *         runner where all parameters are located
     */
    void update(@Nullable Runner runner);

    /**
     * Changes visibility of the terminal.
     *
     * @param isVisible
     *         <code>true</code> terminal is visible,<code>false</code> terminal isn't visible
     */
    void setVisible(boolean isVisible);

    /**
     * Changes visibility of the unavailable label.
     *
     * @param isVisible
     *         <code>true</code> label is visible,<code>false</code> label isn't visible
     */
    void setUnavailableLabelVisible(boolean isVisible);

    /**
     * Sets terminal url.
     *
     * @param runner
     *         which contains current terminal
     */
    void setUrl(@Nonnull Runner runner);

    /**
     * Removes url from terminal.
     */
    void removeUrl();
}