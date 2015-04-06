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
package org.eclipse.che.ide.ext.runner.client.actions;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Action which allows user select runner from all environments.
 *
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
@Singleton
public class ChooseRunnerAction extends AbstractRunnerActions implements CustomComponentAction {
    private final ListBox    environments;
    private final AppContext appContext;

    private List<Environment> systemRunners;
    private List<Environment> projectRunners;

    @Inject
    public ChooseRunnerAction(RunnerResources resources,
                              RunnerLocalizationConstant locale,
                              AppContext appContext) {
        super(appContext, locale.actionChooseRunner(), locale.actionChooseRunner(), null);

        this.appContext = appContext;

        systemRunners = new LinkedList<>();
        projectRunners = new LinkedList<>();

        environments = new ListBox();
        environments.addStyleName(resources.runnerCss().runnersAction());
        environments.addStyleName(resources.runnerCss().runnerFontStyle());
        environments.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                selectEnvironment();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        //do nothing
    }

    /** {@inheritDoc} */
    @Override
    public Widget createCustomComponent(Presentation presentation) {
        return environments;
    }

    /**
     * Adds system environments to the list.
     *
     * @param systemEnvironments
     *         list of system environments
     */
    public void addSystemRunners(@Nonnull List<Environment> systemEnvironments) {
        systemRunners.clear();
        environments.clear();

        for (Environment environment : projectRunners) {
            environments.addItem(environment.getName());
        }

        for (Environment environment : systemEnvironments) {
            String name = environment.getName();
            environments.addItem(name);
        }

        systemRunners.addAll(systemEnvironments);

        selectDefaultRunner();
    }

    /**
     * Adds project environments to the list.
     *
     * @param projectEnvironments
     *         list of system environments
     */
    public void addProjectRunners(@Nonnull List<Environment> projectEnvironments) {
        projectRunners.clear();
        environments.clear();

        for (Environment environment : projectEnvironments) {
            String name = environment.getName();
            environments.addItem(name);
        }

        for (Environment environment : systemRunners) {
            environments.addItem(environment.getName());
        }

        projectRunners.addAll(projectEnvironments);

        selectDefaultRunner();
    }

    /** @return selected environment. */
    @Nullable
    public Environment selectEnvironment() {
        //Method returns null if list of environments is empty. And app will be run with default runner.
        if (environments.getItemCount() < 1) {
            return null;
        }
        String selectedEnvironmentName = environments.getValue(environments.getSelectedIndex());

        for (Environment environment : projectRunners) {
            if (environment.getName().equals(selectedEnvironmentName)) {
                return environment;
            }
        }

        for (Environment environment : systemRunners) {
            if (environment.getName().equals(selectedEnvironmentName)) {
                return environment;
            }
        }

        return null;
    }

    private void selectDefaultRunner() {
        String runnerName = getDefaultRunnerName();
        if (runnerName == null) {
            return;
        }

        for (int index = 0; index < environments.getItemCount(); index++) {
            if (runnerName.equals(environments.getValue(index))) {
                environments.setItemSelected(index, true);
                return;
            }
        }
    }

    @Nullable
    private String getDefaultRunnerName() {
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return null;
        }

        for (Environment e : systemRunners) {
            if (e.getId().equals(currentProject.getRunner())) {
                return e.getName();
            }
        }

        for (Environment e : projectRunners) {
            if (e.getId().equals(currentProject.getRunner())) {
                return e.getName();
            }
        }

        return null;
    }
}
