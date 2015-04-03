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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.ALL;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.ext.runner.client.state.State.TEMPLATE;

/**
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplatesPresenterTest {

    private static final String SOME_TEXT = "someText";
    private static final String TYPE_ALL  = "All";

    //constructor mocks
    @Mock
    private TemplatesView                view;
    @Mock
    private FilterWidget                 filter;
    @Mock
    private RunnerLocalizationConstant   locale;
    @Mock
    private GetProjectEnvironmentsAction projectEnvironmentsAction;
    @Mock
    private GetSystemEnvironmentsAction  systemEnvironmentsAction;
    @Mock
    private GetEnvironmentsUtil          environmentUtil;
    @Mock
    private PropertiesContainer          propertiesContainer;
    @Mock
    private AppContext                   appContext;
    @Mock
    private SelectionManager             selectionManager;
    @Mock
    private RunnerManagerView            runnerManagerView;
    @Mock
    private  RunnerUtil runnerUtil;
    @Mock
    private PanelState panelState;

    //additional mocks
    @Mock
    private Environment                 environment;
    @Mock
    private RunnerEnvironmentTree       tree;
    @Mock
    private CurrentProject              currentProject;
    @Mock
    private ProjectDescriptor           descriptor;
    @Mock
    private Environment                 systemEnvironment1;
    @Mock
    private Environment                 systemEnvironment2;
    @Mock
    private Environment                 projectEnvironment1;
    @Mock
    private Environment                 projectEnvironment2;
    @Mock
    private List<RunnerEnvironmentLeaf> leaves;
    @Mock
    private AcceptsOneWidget            container;

    private TemplatesPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(locale.configsTypeAll()).thenReturn(TYPE_ALL);

        presenter = new TemplatesPresenter(view,
                                           filter,
                                           locale,
                                           appContext,
                                           projectEnvironmentsAction,
                                           systemEnvironmentsAction,
                                           environmentUtil,
                                           propertiesContainer,
                                           selectionManager,
                                           runnerManagerView,
                                           runnerUtil,
                                           panelState);

        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(descriptor.getType()).thenReturn(SOME_TEXT);

        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, SYSTEM)).thenReturn(Arrays.asList(systemEnvironment1,
                                                                                                             systemEnvironment2));
        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT)).thenReturn(Arrays.asList(projectEnvironment1,
                                                                                                              projectEnvironment2));
        when(environmentUtil.getAllEnvironments(tree)).thenReturn(leaves);
        when(environmentUtil.getEnvironmentsFromNodes(leaves, SYSTEM)).thenReturn(Arrays.asList(systemEnvironment1, systemEnvironment2));

        when(panelState.getState()).thenReturn(TEMPLATE);
        when(runnerUtil.hasRunPermission()).thenReturn(true);

        when(systemEnvironment1.getName()).thenReturn("systemEnvironment1");
        when(systemEnvironment1.getScope()).thenReturn(SYSTEM);
        when(systemEnvironment2.getName()).thenReturn("systemEnvironment2");
        when(systemEnvironment2.getScope()).thenReturn(SYSTEM);
        when(projectEnvironment1.getName()).thenReturn("projectEnvironment1");
        when(projectEnvironment1.getScope()).thenReturn(PROJECT);
        when(projectEnvironment2.getName()).thenReturn("projectEnvironment2");
        when(projectEnvironment2.getScope()).thenReturn(PROJECT);

    }

    @Test
    public void constructorShouldBeVerified() throws Exception {
        verify(view).setFilterWidget(filter);
        verify(locale).configsTypeAll();
    }

    @Test
    public void environmentShouldBeSelected() throws Exception {
        presenter.select(environment);

        verify(propertiesContainer).show(environment);
        verify(view).selectEnvironment(environment);
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateExceptionShouldBeThrownWhenCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.addEnvironments(tree, SYSTEM);

        verify(currentProject, never()).getProjectDescription();
    }

    @Test
    public void environmentWithScopeSystemShouldBeAdded() throws Exception {
        when(filter.getScope()).thenReturn(SYSTEM);
        when(filter.getType()).thenReturn(TYPE_ALL);
        presenter.onValueChanged();
        reset(view, propertiesContainer);

        presenter.addEnvironments(tree, SYSTEM);

        getProjectDescriptorShouldBeVerified();

        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, SYSTEM);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());

        verify(propertiesContainer).setVisible(true);
        verify(propertiesContainer).show(systemEnvironment1);
        verify(view).selectEnvironment(systemEnvironment1);
        verify(selectionManager).setEnvironment(systemEnvironment1);
    }

    private void getProjectDescriptorShouldBeVerified() {
        verify(appContext).getCurrentProject();
        verify(currentProject).getProjectDescription();
    }

    @Test
    public void environmentShouldNotBeSelectedWhenEnvironmentListIsEmpty() throws Exception {
        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, SYSTEM)).thenReturn(Collections.<Environment>emptyList());
        when(filter.getScope()).thenReturn(SYSTEM);
        when(filter.getType()).thenReturn(TYPE_ALL);
        presenter.onValueChanged();
        reset(view, propertiesContainer, selectionManager);

        presenter.addEnvironments(tree, SYSTEM);

        verify(propertiesContainer).setVisible(true);
        verify(propertiesContainer).show(isNull(Environment.class));
        verify(selectionManager).setEnvironment(isNull(Environment.class));
    }

    @Test
    public void environmentWithScopeProjectShouldBeAdded() throws Exception {
        presenter.addEnvironments(tree, PROJECT);

        getProjectDescriptorShouldBeVerified();

        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());

        verify(propertiesContainer).setVisible(true);
        verify(propertiesContainer).show(projectEnvironment1);
        verify(view).selectEnvironment(projectEnvironment1);
        verify(selectionManager).setEnvironment(projectEnvironment1);

        verify(panelState).getState();
        verify(runnerUtil).hasRunPermission();
        verify(runnerManagerView).setEnableRunButton(true);
    }

    @Test
    public void typeItemShouldBeSet() throws Exception {
        presenter.setTypeItem(SOME_TEXT);

        verify(filter).addType(SOME_TEXT);
    }

    @Test
    public void environmentsShouldBeShown() throws Exception {
        presenter.setTypeItem(SOME_TEXT);

        presenter.showEnvironments();
        presenter.addEnvironments(tree, PROJECT);

        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).selectScope(PROJECT);
        verify(filter).selectType(SOME_TEXT);

        verify(propertiesContainer).show(projectEnvironment1);
        verify(view).selectEnvironment(projectEnvironment1);
        verify(selectionManager).setEnvironment(projectEnvironment1);
    }

    @Test
    public void firstEnvironmentShouldBeSelectedIfSelectedEnvIsNull() throws Exception {
        when(selectionManager.getEnvironment()).thenReturn(null);

        presenter.setTypeItem(SOME_TEXT);

        presenter.showEnvironments();
        presenter.addEnvironments(tree, PROJECT);

        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).selectScope(PROJECT);
        verify(filter).selectType(SOME_TEXT);

        verify(propertiesContainer).show(projectEnvironment1);
        verify(view).selectEnvironment(projectEnvironment1);
        verify(selectionManager).setEnvironment(projectEnvironment1);
    }

    @Test
    public void previousEnvironmentShouldBeSelected() throws Exception {
        when(selectionManager.getEnvironment()).thenReturn(environment);

        presenter.setTypeItem(SOME_TEXT);

        presenter.showEnvironments();
        presenter.addEnvironments(tree, PROJECT);

        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).selectScope(PROJECT);
        verify(filter).selectType(SOME_TEXT);

        verify(selectionManager).setEnvironment(environment);
    }

    @Test
    public void systemEnvironmentsShouldBePerformedWhenTypeAll() throws Exception {
        when(filter.getScope()).thenReturn(SYSTEM);
        when(filter.getType()).thenReturn(TYPE_ALL);
        presenter.onValueChanged();

        presenter.addEnvironments(tree, SYSTEM);
        reset(view);

        presenter.onValueChanged();

        verify(view).clearEnvironmentsPanel();
        systemEnvironmentsPerformShouldBeVerified();
    }

    private void systemEnvironmentsPerformShouldBeVerified() {
        verify(environmentUtil).getAllEnvironments(tree);
        verify(environmentUtil).getEnvironmentsFromNodes(leaves, SYSTEM);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());
    }

    @Test
    public void systemEnvironmentsShouldBePerformedWhenTypeIsNotAll() throws Exception {
        when(filter.getScope()).thenReturn(SYSTEM);
        when(filter.getType()).thenReturn(SOME_TEXT);

        presenter.onValueChanged();

        verify(systemEnvironmentsAction).perform();
    }

    @Test
    public void projectEnvironmentsShouldBePerformed() throws Exception {
        when(filter.getScope()).thenReturn(PROJECT);

        presenter.onValueChanged();

        verify(projectEnvironmentsAction).perform();
    }

    @Test
    public void allEnvironmentShouldBeSelectedWhenScopeIsAllAndTypeIsNotAll() throws Exception {
        when(filter.getScope()).thenReturn(ALL);
        when(filter.getType()).thenReturn(SOME_TEXT);

        presenter.onValueChanged();

        verify(projectEnvironmentsAction).perform();
        verify(systemEnvironmentsAction).perform();
    }

    @Test
    public void allEnvironmentShouldBeSelectedWhenScopeIsAllAndTypeIsAll() throws Exception {
        when(filter.getScope()).thenReturn(ALL);
        when(filter.getType()).thenReturn(TYPE_ALL);
        presenter.onValueChanged();
        reset(projectEnvironmentsAction);

        presenter.addEnvironments(tree, SYSTEM);
        reset(view);

        presenter.onValueChanged();

        systemEnvironmentsPerformShouldBeVerified();
        verify(projectEnvironmentsAction).perform();
    }

    @Test
    public void firstEnvironmentShouldBeSelectedWhenSelectedEnvironmentIsNull() {
        prepareMocks();
        when(selectionManager.getEnvironment()).thenReturn(null);

        presenter.selectEnvironment();

        verify(propertiesContainer).setVisible(true);
        verify(selectionManager).setEnvironment(systemEnvironment1);
    }

    private void prepareMocks() {
        when(filter.getScope()).thenReturn(SYSTEM);
        when(filter.getType()).thenReturn(TYPE_ALL);

        presenter.onValueChanged();
        presenter.addEnvironments(tree, SYSTEM);

        reset(propertiesContainer, selectionManager);
    }

    @Test
    public void environmentShouldBeSelectedWhenSelectedEnvironmentIsNotNull() throws Exception {
        prepareMocks();
        when(selectionManager.getEnvironment()).thenReturn(environment);

        presenter.selectEnvironment();

        verify(selectionManager).setEnvironment(environment);
        verify(selectionManager, never()).setEnvironment(systemEnvironment1);
    }

    @Test
    public void systemEnvironmentsShouldNotBeAddedWhenSelectedScopeIsProject() throws Exception {
        when(filter.getScope()).thenReturn(PROJECT);
        presenter.onValueChanged();

        presenter.addEnvironments(tree, SYSTEM);

        verify(view, never()).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());
    }

    @Test
    public void widgetShouldBeSetToContainer() throws Exception {
        presenter.go(container);

        verify(container).setWidget(view);
    }

    @Test
    public void viewShouldBeReturned() throws Exception {
        TemplatesView widget = (TemplatesView)presenter.getView();

        assertThat(widget, equalTo(view));
    }

    @Test
    public void visibleParameterShouldBeSet() throws Exception {
        presenter.setVisible(true);

        verify(view).setVisible(true);
    }

    @Test
    public void runButtonShouldBeEnable() {
        presenter.addEnvironments(tree, SYSTEM);
        reset(runnerUtil, runnerManagerView);

        presenter.changeEnableStateRunButton();

        verify(runnerUtil).hasRunPermission();
        runnerManagerView.setEnableRunButton(true);
    }

    @Test
    public void runButtonShouldBeDisableBecauseEnvironmentListIsEmpty() {
        presenter.changeEnableStateRunButton();

        verify(runnerUtil).hasRunPermission();

        verify(runnerManagerView).setEnableRunButton(false);
    }

    @Test
    public void runButtonShouldBeDisableBecauseUserHasNotPermission() {
        when(runnerUtil.hasRunPermission()).thenReturn(false);

        presenter.changeEnableStateRunButton();

        verify(runnerUtil).hasRunPermission();

        verifyNoMoreInteractions(runnerManagerView);
    }

    @Test
    public void runnerStateShouldNotBeChangedIfOpenRunnerPropertiesPanel1() {
        when(panelState.getState()).thenReturn(RUNNERS);

        presenter.addEnvironments(tree, PROJECT);

        getProjectDescriptorShouldBeVerified();

        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());

        verify(propertiesContainer).setVisible(true);
        verify(propertiesContainer).show(projectEnvironment1);
        verify(view).selectEnvironment(projectEnvironment1);
        verify(selectionManager).setEnvironment(projectEnvironment1);

        verify(panelState).getState();

        verifyNoMoreInteractions(panelState, runnerUtil, runnerManagerView);
    }

    @Test
    public void runnerStateShouldNotBeChangedIfOpenRunnerPropertiesPanel2() throws Exception {
        when(panelState.getState()).thenReturn(RUNNERS);

        presenter.addEnvironments(tree, SYSTEM);

        getProjectDescriptorShouldBeVerified();

        verifyNoMoreInteractions(environmentUtil, panelState, runnerUtil, runnerManagerView);
    }

    @Test
    public void runEnableStatusShouldBeChangedAfterChangeScopeFromProjectToAll() {
        presenter.addEnvironments(tree, SYSTEM);
        presenter.addEnvironments(tree, PROJECT);

        when(filter.getScope()).thenReturn(ALL);
        when(filter.getType()).thenReturn(TYPE_ALL);
        presenter.onValueChanged();
        reset(runnerManagerView, view, panelState);

        presenter.addEnvironments(tree, ALL);

        verify(appContext, times(3)).getCurrentProject();
        verify(currentProject, times(3)).getProjectDescription();
        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, ALL);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());
        verify(descriptor, times(2)).getType();

        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());
        verify(propertiesContainer, times(3)).setVisible(true);

        verify(propertiesContainer, times(2)).show(systemEnvironment1);
        verify(view).selectEnvironment(systemEnvironment1);
        verify(selectionManager, times(2)).setEnvironment(systemEnvironment1);

        verify(panelState).getState();

        verify(runnerUtil, times(3)).hasRunPermission();

        //run button should be visible
        verify(runnerManagerView).setEnableRunButton(true);
    }
}