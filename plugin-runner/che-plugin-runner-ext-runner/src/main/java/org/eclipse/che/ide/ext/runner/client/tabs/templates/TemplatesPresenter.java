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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetSystemEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.state.PanelState;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.ALL;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;

/**
 * The class contains business logic to change displaying of environments depending on scope or type.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class TemplatesPresenter implements TemplatesContainer, FilterWidget.ActionDelegate {

    private final TemplatesView                 view;
    private final FilterWidget                  filter;
    private final SelectionManager              selectionManager;
    private final GetProjectEnvironmentsAction  projectEnvironmentsAction;
    private final GetSystemEnvironmentsAction   systemEnvironmentsAction;
    private final GetEnvironmentsUtil           environmentUtil;
    private final List<Environment>             systemEnvironments;
    private final List<Environment>             projectEnvironments;
    private final Map<Scope, List<Environment>> environmentMap;
    private final PropertiesContainer           propertiesContainer;
    private final AppContext                    appContext;
    private final RunnerManagerView             runnerManagerView;
    private final RunnerUtil                    runnerUtil;
    private final PanelState panelState;


    private RunnerEnvironmentTree tree;
    private String                currentType;
    private Scope                 currentScope;
    private boolean               matchProjectType;
    private Environment           selectedEnvironment;

    @Inject
    public TemplatesPresenter(TemplatesView view,
                              FilterWidget filter,
                              RunnerLocalizationConstant locale,
                              AppContext appContext,
                              GetProjectEnvironmentsAction projectEnvironmentsAction,
                              GetSystemEnvironmentsAction systemEnvironmentsAction,
                              GetEnvironmentsUtil environmentUtil,
                              PropertiesContainer propertiesContainer,
                              SelectionManager selectionManager,
                              RunnerManagerView runnerManagerView,
                              RunnerUtil runnerUtil,
                              PanelState panelState) {
        this.filter = filter;
        this.selectionManager = selectionManager;
        this.filter.setDelegate(this);

        this.view = view;
        this.view.setFilterWidget(filter);

        this.projectEnvironmentsAction = projectEnvironmentsAction;
        this.systemEnvironmentsAction = systemEnvironmentsAction;
        this.environmentUtil = environmentUtil;
        this.propertiesContainer = propertiesContainer;
        this.appContext = appContext;
        this.runnerManagerView = runnerManagerView;
        this.runnerUtil = runnerUtil;
        this.panelState = panelState;

        this.projectEnvironments = new ArrayList<>();
        this.systemEnvironments = new ArrayList<>();

        this.environmentMap = new EnumMap<>(Scope.class);
        this.environmentMap.put(PROJECT, projectEnvironments);
        this.environmentMap.put(SYSTEM, systemEnvironments);

        this.currentScope = ALL;
    }

    /** {@inheritDoc} */
    @Override
    public void select(@Nullable Environment environment) {
        this.selectedEnvironment = environment;
        propertiesContainer.show(environment);
        view.selectEnvironment(environment);
    }

    /** {@inheritDoc} */
    @Override
    public void addEnvironments(@Nonnull RunnerEnvironmentTree tree, @Nonnull Scope scope) {
        ProjectDescriptor descriptor = getProjectDescriptor();

        List<Environment> list;

        if (SYSTEM.equals(scope)) {
            list = systemEnvironments;
            this.tree = tree;
        } else {
            list = projectEnvironments;
        }

        if (scope.equals(currentScope) || ALL.equals(currentScope)) {
            List<Environment> environments = environmentUtil.getEnvironmentsByProjectType(tree, descriptor.getType(), scope);
            addEnvironments(list, environments, scope);
        }
    }

    @Nonnull
    private ProjectDescriptor getProjectDescriptor() {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            throw new IllegalStateException("Current project is null");
        }

        return currentProject.getProjectDescription();
    }

    private void addEnvironments(@Nonnull List<Environment> sourceList,
                                 @Nonnull List<Environment> targetList,
                                 @Nonnull Scope scope) {
        sourceList.clear();
        sourceList.addAll(targetList);

        environmentMap.put(scope, sourceList);
        view.addEnvironment(environmentMap);

        selectPreviousOrFirstEnvironment();

        if (!(RUNNERS).equals(panelState.getState())) {
            changeEnableStateRunButton();
        }
    }

    private void selectPreviousOrFirstEnvironment() {
        propertiesContainer.setVisible(true);
        Environment environment = null;

        // try to select the previous element if it still exists
        if (this.selectedEnvironment != null) {
            for (Map.Entry<Scope, List<Environment>> entry : environmentMap.entrySet()) {
                List<Environment> value = entry.getValue();
                for (Environment env : value) {
                    if (env.getName().equals(selectedEnvironment.getName()) && env.getScope().equals(selectedEnvironment.getScope())) {
                        environment = env;
                        break;
                    }
                }
            }
        }

        // else we take the first element
        if (environment == null) {
            for (Map.Entry<Scope, List<Environment>> entry : environmentMap.entrySet()) {
                List<Environment> value = entry.getValue();
                if (!value.isEmpty()) {
                    environment = value.get(0);
                    break;
                }
            }
        }

        select(environment);
        selectionManager.setEnvironment(environment);
    }

    /** {@inheritDoc} */
    @Override
    public void setTypeItem(@Nonnull String item) {
        currentType = item;
    }

    /** {@inheritDoc} */
    @Override
    public void showEnvironments() {
        view.clearEnvironmentsPanel();
        systemEnvironments.clear();

        projectEnvironmentsAction.perform();

        filter.setMatchesProjectType(true);

        selectEnvironment();
    }

    /** {@inheritDoc} */
    @Override
    public void selectEnvironment() {
        Environment selectedEnvironment = selectionManager.getEnvironment();
        if (selectedEnvironment == null) {
            selectPreviousOrFirstEnvironment();
        } else {
            selectionManager.setEnvironment(selectedEnvironment);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onValueChanged() {
        view.clearEnvironmentsPanel();

        matchProjectType = filter.getMatchesProjectType();

        switch (currentScope) {
            case SYSTEM:
                selectSystemScope();
                break;

            case PROJECT:
                performProjectEnvironments();
                break;

            default:
                selectAllScope();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void changeEnableStateRunButton() {
        if (!runnerUtil.hasRunPermission()) {
            return;
        }

        List<Environment> environmentList = environmentMap.get(currentScope);

        boolean runButtonIsEnable = currentScope == ALL || !environmentList.isEmpty();

        runnerManagerView.setEnableRunButton(runButtonIsEnable);
    }

    private void selectSystemScope() {
        projectEnvironments.clear();

        if (matchProjectType) {
            systemEnvironmentsAction.perform();
        } else {
            performSystemEnvironments();
        }
    }

    private void performProjectEnvironments() {
        projectEnvironments.clear();
        systemEnvironments.clear();

        projectEnvironmentsAction.perform();
    }

    private void selectAllScope() {
        if (matchProjectType) {
            projectEnvironmentsAction.perform();
            systemEnvironmentsAction.perform();
        } else {
            performProjectEnvironments();
            performSystemEnvironments();
        }
    }

    private void performSystemEnvironments() {
        systemEnvironments.clear();
        List<RunnerEnvironmentLeaf> leaves = environmentUtil.getAllEnvironments(tree);
        List<Environment> environments = environmentUtil.getEnvironmentsFromNodes(leaves, SYSTEM);

        addEnvironments(systemEnvironments, environments, SYSTEM);
    }

    /** {@inheritDoc} */
    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }
}