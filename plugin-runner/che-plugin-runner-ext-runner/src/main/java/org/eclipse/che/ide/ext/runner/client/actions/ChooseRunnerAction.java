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

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ui.dropdown.DropDownHeaderWidget;
import org.eclipse.che.ide.ui.dropdown.DropDownListFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

import static org.eclipse.che.ide.ext.runner.client.RunnerExtension.RUNNER_LIST;

/**
 * Action which allows user select runner from all environments.
 *
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
@Singleton
public class ChooseRunnerAction extends AbstractRunnerActions implements CustomComponentAction {
    private static final String PROJECT_ENVIRONMENT = "projectEnvironment";
    private static final String SYSTEM_ENVIRONMENT  = "systemEnvironment";

    private final AppContext           appContext;
    private final DropDownHeaderWidget dropDownHeaderWidget;
    private final DropDownListFactory  configRunnerFactory;
    private final RunnerResources      resources;
    private final ActionManager        actionManager;

    private List<Environment>  systemRunners;
    private List<Environment>  projectRunners;
    private DefaultActionGroup projectActions;
    private DefaultActionGroup systemActions;

    @Inject
    public ChooseRunnerAction(RunnerResources resources,
                              RunnerLocalizationConstant locale,
                              AppContext appContext,
                              ActionManager actionManager,
                              DropDownListFactory dropDownListFactory) {
        super(appContext, locale.actionChooseRunner(), locale.actionChooseRunner(), null);

        this.appContext = appContext;
        this.dropDownHeaderWidget = dropDownListFactory.createList(RUNNER_LIST);
        this.resources = resources;
        this.actionManager = actionManager;
        this.configRunnerFactory = dropDownListFactory;

        systemRunners = new LinkedList<>();
        projectRunners = new LinkedList<>();

        projectActions = new DefaultActionGroup(PROJECT_ENVIRONMENT, false, actionManager);
        systemActions = new DefaultActionGroup(SYSTEM_ENVIRONMENT, false, actionManager);

        actionManager.registerAction(PROJECT_ENVIRONMENT, projectActions);
        actionManager.registerAction(SYSTEM_ENVIRONMENT, systemActions);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        //do nothing
    }

    /** {@inheritDoc} */
    @Override
    public Widget createCustomComponent(Presentation presentation) {
        return (Widget)dropDownHeaderWidget;
    }

    /**
     * Adds system environments to the list.
     *
     * @param systemEnvironments
     *         list of system environments
     */
    public void addSystemRunners(@Nonnull List<Environment> systemEnvironments) {
        DefaultActionGroup runnersList = (DefaultActionGroup)actionManager.getAction(RUNNER_LIST);

        systemRunners.clear();

        clearRunnerActions(runnersList);
        projectActions.removeAll();
        systemActions.removeAll();

        for (Environment environment : projectRunners) {
            projectActions.add(configRunnerFactory.createElement(environment.getName(), resources.scopeProject(), dropDownHeaderWidget));
        }

        runnersList.addSeparator();

        for (Environment environment : systemEnvironments) {
            systemActions.add(configRunnerFactory.createElement(environment.getName(), resources.scopeSystem(), dropDownHeaderWidget));
        }

        runnersList.addAll(projectActions);
        runnersList.addSeparator();
        runnersList.addAll(systemActions);

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
        DefaultActionGroup runnersList = (DefaultActionGroup)actionManager.getAction(RUNNER_LIST);

        projectRunners.clear();

        clearRunnerActions(runnersList);
        projectActions.removeAll();
        systemActions.removeAll();

        for (Environment environment : projectEnvironments) {
            projectActions.add(configRunnerFactory.createElement(environment.getName(), resources.scopeProject(), dropDownHeaderWidget));
        }

        runnersList.addSeparator();

        for (Environment environment : systemRunners) {
            systemActions.add(configRunnerFactory.createElement(environment.getName(), resources.scopeSystem(), dropDownHeaderWidget));
        }

        runnersList.addAll(projectActions);
        runnersList.addSeparator();
        runnersList.addAll(systemActions);

        projectRunners.addAll(projectEnvironments);

        selectDefaultRunner();
    }

    private void clearRunnerActions(@Nonnull DefaultActionGroup runnersList) {
        for (Action a : projectActions.getChildActionsOrStubs()) {
            runnersList.remove(a);
        }
        for (Action a : systemActions.getChildActionsOrStubs()) {
            runnersList.remove(a);
        }
    }

    /** @return selected environment. */
    @Nullable
    public Environment selectEnvironment() {
        //Method returns null if list of environments is empty. And app will be run with default runner.
        if (systemRunners.isEmpty() && projectRunners.isEmpty()) {
            return null;
        }
        String selectedEnvironmentName = dropDownHeaderWidget.getSelectedElementName();

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

    /** Selects default environment from system environments if it is defined for current project. */
    public void selectDefaultRunner() {
        String runnerName = getDefaultRunnerName();
        if (runnerName == null) {
            return;
        }

        for (Environment e : systemRunners) {
            if (runnerName.equals(e.getName())) {
                dropDownHeaderWidget.selectElement(resources.scopeSystem(), e.getName());
                return;
            }
        }

        for (Environment e : projectRunners) {
            if (runnerName.equals(e.getName())) {
                dropDownHeaderWidget.selectElement(resources.scopeProject(), e.getName());

                return;
            }
        }
    }

    /** Clears selected element of the 'Choose runner' menu. */
    public void setEmptyDefaultRunner() {
        dropDownHeaderWidget.selectElement(null, "");
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
