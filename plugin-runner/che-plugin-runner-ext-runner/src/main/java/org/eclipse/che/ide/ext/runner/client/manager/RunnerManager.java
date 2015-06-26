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
package org.eclipse.che.ide.ext.runner.client.manager;

import com.google.inject.ImplementedBy;

import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;

import javax.annotation.Nonnull;

/**
 * It is the main class of structure. It provides an ability to launch a new runner. It can launch default runner or custom runner. Default
 * runner will be launched when one uses method without runner options in other case will be launched custom runner that described into
 * options.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
@ImplementedBy(RunnerManagerPresenter.class)
public interface RunnerManager {

    /**
     * Launch a new default runner.
     *
     * @return new instance of the runner
     */
    Runner launchRunner();

    /**
     * Launch a new runner with given configuration.
     *
     * @param runOptions
     *         configuration of the runner
     * @return new instance of the runner
     */
    @Nonnull
    Runner launchRunner(@Nonnull RunOptions runOptions);

    /**
     * Launch a new runner with given configurations.
     *
     * @param environmentName
     *         name of custom configuration
     * @param scope
     *         scope value of current environment
     * @param runOptions
     *         configuration of the runner
     * @return new instance of the runner
     */
    @Nonnull
    Runner launchRunner(@Nonnull RunOptions runOptions, @Nonnull Scope scope, @Nonnull String environmentName);

    /**
     * Stops launch and run actions.
     *
     * @param runner
     *         runner which performs actions
     */
    void stopRunner(@Nonnull Runner runner);

}