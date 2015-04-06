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

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabPresenter;

import javax.annotation.Nonnull;

/**
 * The common representation of terminal container widget. This widget provides an ability to manager many terminal widgets for every
 * runner.
 *
 * @author Valeriy Svydenko
 */
@ImplementedBy(TerminalContainerPresenter.class)
public interface TerminalContainer extends TabPresenter {
    /**
     * The method update terminal of current runner.
     *
     * @param runner
     *         runner which need update
     */
    void update(@Nonnull Runner runner);

    /** Cleans the data of console widgets. */
    void reset();

    /**
     * Removes url from terminal.
     *
     * @param runner
     *         instance of Runner which contains iframe with terminal
     */
    void removeTerminalUrl(@Nonnull Runner runner);
}