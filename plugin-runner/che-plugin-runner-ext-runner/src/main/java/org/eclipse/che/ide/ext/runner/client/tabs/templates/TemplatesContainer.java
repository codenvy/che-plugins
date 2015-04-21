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
package org.eclipse.che.ide.ext.runner.client.tabs.templates;

import com.google.inject.ImplementedBy;

import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides methods which allow work with templates panel.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(TemplatesPresenter.class)
public interface TemplatesContainer extends TabPresenter {
    /**
     * Calls special method on view which set current environment selected.
     *
     * @param environment
     *         environment which was selected
     */
    void select(@Nullable Environment environment);

    /**
     * Calls method on view which adds environment widget on templates panel.
     *
     * @param tree
     *         list of environments which need add
     * @param scope
     *         scope of environments which are saved in list
     * @return list environments generated from tree by scope
     */
    List<Environment> addEnvironments(@Nonnull RunnerEnvironmentTree tree, @Nonnull Scope scope);

    /**
     * Returns the project environments
     *
     * @return the list of project environments
     */
    @Nonnull
    List<Environment> getProjectEnvironments();

    /** Shows environments when user click on templates tab the first time. */
    void showEnvironments();

    /** Selects environment when we click on configs tab. */
    void selectEnvironment();

    /** Changes enable state run button */
    void changeEnableStateRunButton();

    /**
     * Sets current environment as default for project. If we want delete default environment we need hand on null.
     *
     * @param environment
     *         environment which need set
     */
    void setDefaultEnvironment(@Nullable Environment environment);

}