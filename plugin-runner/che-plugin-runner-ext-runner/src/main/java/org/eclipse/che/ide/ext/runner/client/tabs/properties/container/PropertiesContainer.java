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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.container;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabPresenter;

import javax.annotation.Nullable;

/**
 * The container for properties panels.
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
@ImplementedBy(PropertiesContainerPresenter.class)
public interface PropertiesContainer extends TabPresenter {
    /**
     * Show properties panel for runner.
     *
     * @param runner
     *         runner that is bound with properties panel
     */
    void show(@Nullable Runner runner);

    /**
     * Show properties panel for environment.
     *
     * @param environment
     *         environment that is bound with properties panel
     */
    void show(@Nullable Environment environment);

    /** Cleans the data of the history panel. */
    void reset();

    /**
     * Changes visibility of the no runner label.
     *
     * @param visible
     *         visible state that needs to be applied
     */
    void setVisibleNoRunnerLabel(boolean visible);
}