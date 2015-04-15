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

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ui.dropdown.DropDownListFactory;
import org.eclipse.che.ide.ui.dropdown.DropDownHeaderWidget;
import org.eclipse.che.ide.ui.dropdown.SimpleListElementAction;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.runner.client.RunnerExtension.RUNNER_LIST;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ChooseRunnerActionTest {

    private static final String TEXT = "some test/test/runner";
    private List<Environment> projectEnvList;
    private List<Environment> systemEnvList;

    //variables for constructor
    @Mock
    private RunnerResources            resources;
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private AppContext                 appContext;
    @Mock
    private ActionManager              actionManager;
    @Mock
    private DropDownListFactory        listFactory;

    @Mock
    private RunnerResources.RunnerCss css;
    @Mock
    private Environment               projectEnv1;
    @Mock
    private Environment               systemEnv1;
    @Mock
    private SimpleListElementAction   systemAction;
    @Mock
    private SimpleListElementAction   projectAction;
    @Mock
    private DefaultActionGroup        runnersList;
    @Mock
    private DropDownHeaderWidget      dropDownHeaderWidget;
    @Mock
    private SVGResource               svgResource;

    private ChooseRunnerAction action;

    @Before
    public void setUp() {
        when(locale.actionChooseRunner()).thenReturn(TEXT);
        when(listFactory.createList(RUNNER_LIST)).thenReturn(dropDownHeaderWidget);

        action = new ChooseRunnerAction(resources, locale, appContext, actionManager, listFactory);

        projectEnvList = Collections.singletonList(projectEnv1);
        systemEnvList = Collections.singletonList(systemEnv1);

        when(resources.scopeSystem()).thenReturn(svgResource);
        when(resources.scopeProject()).thenReturn(svgResource);
        when(systemEnv1.getName()).thenReturn(TEXT);
        when(systemEnv1.getId()).thenReturn(TEXT);
        when(projectEnv1.getName()).thenReturn(TEXT);
        when(projectEnv1.getId()).thenReturn(TEXT);
    }

    @Test
    public void actionShouldBePerformed() throws Exception {
        verify(locale, times(2)).actionChooseRunner();
        verify(listFactory).createList(RUNNER_LIST);
        verify(actionManager, times(2)).registerAction(anyString(), Matchers.<Action>any());
    }

    @Test
    public void systemRunnersShouldBeAdded() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(actionManager.getAction(RUNNER_LIST)).thenReturn(runnersList);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(listFactory.createElement(TEXT, svgResource, dropDownHeaderWidget)).thenReturn(systemAction);

        action.addSystemRunners(systemEnvList);

        verify(systemEnv1, times(4)).getName();
        verify(resources, times(2)).scopeSystem();
        verify(listFactory).createElement(TEXT, svgResource, dropDownHeaderWidget);
        verify(runnersList, times(2)).addSeparator();
        verify(runnersList, times(2)).addAll(Matchers.<ActionGroup>any());
        verify(dropDownHeaderWidget).selectElement(resources.scopeProject(), TEXT);

        action.addProjectRunners(projectEnvList);

        verify(listFactory, times(3)).createElement(TEXT, svgResource, dropDownHeaderWidget);
    }

    @Test
    public void projectRunnersShouldBeAdded() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(actionManager.getAction(RUNNER_LIST)).thenReturn(runnersList);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(listFactory.createElement(TEXT, svgResource, dropDownHeaderWidget)).thenReturn(projectAction);

        action.addProjectRunners(projectEnvList);

        verify(projectEnv1, times(4)).getName();
        verify(resources, times(2)).scopeProject();
        verify(listFactory).createElement(TEXT, svgResource, dropDownHeaderWidget);
        verify(runnersList, times(2)).addSeparator();
        verify(runnersList, times(2)).addAll(Matchers.<ActionGroup>any());
        verify(dropDownHeaderWidget).selectElement(resources.scopeProject(), TEXT);

        action.addSystemRunners(systemEnvList);

        verify(listFactory, times(3)).createElement(TEXT, svgResource, dropDownHeaderWidget);
    }

    @Test
    public void defaultRunnerShouldNotBeSelectedIfCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.selectDefaultRunner();

        verify(dropDownHeaderWidget, never()).selectElement((SVGResource)any(), anyString());
    }

    @Test
    public void defaultRunnerShouldNotBeSelectedIfProjectDoesNotHaveAnyEnvironments() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        action.selectDefaultRunner();

        verify(dropDownHeaderWidget, never()).selectElement((SVGResource)any(), anyString());
    }

    @Test
    public void EnvironmentShouldNotBeSelectedIfProjectDoesNotHaveAnyEnvironment() throws Exception {
        action.selectEnvironment();

        verify(dropDownHeaderWidget, never()).getSelectedElementName();
    }

    @Test
    public void selectedEnvironmentShouldBeProjectEnvironment() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(actionManager.getAction(RUNNER_LIST)).thenReturn(runnersList);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(listFactory.createElement(TEXT, svgResource, dropDownHeaderWidget)).thenReturn(projectAction);

        action.addProjectRunners(projectEnvList);

        verify(projectEnv1, times(4)).getName();
        verify(resources, times(2)).scopeProject();
        verify(listFactory).createElement(TEXT, svgResource, dropDownHeaderWidget);
        verify(runnersList, times(2)).addSeparator();
        verify(runnersList, times(2)).addAll(Matchers.<ActionGroup>any());
        verify(dropDownHeaderWidget).selectElement(resources.scopeProject(), TEXT);

        when(dropDownHeaderWidget.getSelectedElementName()).thenReturn(TEXT);

        assertThat(action.selectEnvironment(), is(projectEnv1));

        verify(dropDownHeaderWidget).getSelectedElementName();
    }

    @Test
    public void selectedEnvironmentShouldBeSystemEnvironment() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(actionManager.getAction(RUNNER_LIST)).thenReturn(runnersList);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(listFactory.createElement(TEXT, svgResource, dropDownHeaderWidget)).thenReturn(projectAction);

        action.addSystemRunners(systemEnvList);

        verify(systemEnv1, times(4)).getName();
        verify(resources, times(2)).scopeSystem();
        verify(listFactory).createElement(TEXT, svgResource, dropDownHeaderWidget);
        verify(runnersList, times(2)).addSeparator();
        verify(runnersList, times(2)).addAll(Matchers.<ActionGroup>any());
        verify(dropDownHeaderWidget).selectElement(resources.scopeProject(), TEXT);

        when(dropDownHeaderWidget.getSelectedElementName()).thenReturn(TEXT);

        assertThat(action.selectEnvironment(), is(systemEnv1));

        verify(dropDownHeaderWidget).getSelectedElementName();
    }

    @Test
    public void selectedEnvironmentIsNullIfSelectedNameDoesNotExist() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(actionManager.getAction(RUNNER_LIST)).thenReturn(runnersList);
        when(currentProject.getRunner()).thenReturn(TEXT);
        when(listFactory.createElement(TEXT, svgResource, dropDownHeaderWidget)).thenReturn(projectAction);

        action.addSystemRunners(systemEnvList);
        action.addProjectRunners(projectEnvList);

        assertThat(action.selectEnvironment(), nullValue());
    }
}