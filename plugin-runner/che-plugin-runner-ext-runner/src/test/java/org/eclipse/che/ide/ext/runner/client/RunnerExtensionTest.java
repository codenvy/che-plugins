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
package org.eclipse.che.ide.ext.runner.client;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.actions.CreateCustomRunnerAction;
import org.eclipse.che.ide.ext.runner.client.actions.RunAction;
import org.eclipse.che.ide.ext.runner.client.actions.RunWithAction;
import org.eclipse.che.ide.ext.runner.client.constants.ActionId;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_BUILD_TOOLBAR;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_CONTEXT_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RIGHT_TOOLBAR;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN_CONTEXT_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN_TOOLBAR;
import static org.eclipse.che.ide.ext.runner.client.RunnerExtension.BUILDER_PART_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RunnerExtensionTest {

    @Captor
    private ArgumentCaptor<Constraints>        constraintsCaptor;
    @Captor
    private ArgumentCaptor<DefaultActionGroup> actionGroupCaptor;
    @Mock
    private RunnerResources.RunnerCss          runnerCss;
    @Mock
    private RunnerResources                    resources;
    private RunnerExtension                    extension;

    @Before
    public void setUp() throws Exception {
        when(resources.runnerCss()).thenReturn(runnerCss);

        extension = new RunnerExtension(resources);
    }

    @Test
    public void cssResourcesShouldBeInjected() throws Exception {
        verify(runnerCss).ensureInjected();
    }

    @Test
    public void runnerPanelShouldBeOpened() throws Exception {
        WorkspaceAgent workspaceAgent = mock(WorkspaceAgent.class);
        RunnerManagerPresenter runnerManagerPresenter = mock(RunnerManagerPresenter.class);

        extension.setUpRunnerConsole(workspaceAgent, runnerManagerPresenter);

        verify(workspaceAgent).openPart(eq(runnerManagerPresenter), eq(PartStackType.INFORMATION), constraintsCaptor.capture());
        verifyConstants(Anchor.AFTER, BUILDER_PART_ID);
    }


    @Test
    public void runnerMenuActionsShouldBeAdded() throws Exception {
        // prepare step
        ActionManager actionManager = mock(ActionManager.class);

        RunAction runAction = mock(RunAction.class);
        RunWithAction runWithAction = mock(RunWithAction.class);
        ChooseRunnerAction chooseRunnerAction = mock(ChooseRunnerAction.class);
        CreateCustomRunnerAction createCustomRunnerAction = mock(CreateCustomRunnerAction.class);

        DefaultActionGroup rightToolbarGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup runContextGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup contextMenuGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup mainMenuGroup = mock(DefaultActionGroup.class);

        when(actionManager.getAction(GROUP_RIGHT_TOOLBAR)).thenReturn(rightToolbarGroup);
        when(actionManager.getAction(GROUP_RUN_CONTEXT_MENU)).thenReturn(runContextGroup);
        when(actionManager.getAction(GROUP_MAIN_CONTEXT_MENU)).thenReturn(contextMenuGroup);
        when(actionManager.getAction(GROUP_RUN)).thenReturn(mainMenuGroup);

        // test step
        extension.setUpRunActions(actionManager, runAction, runWithAction, chooseRunnerAction, createCustomRunnerAction);

        // check step
        verify(rightToolbarGroup).add(actionGroupCaptor.capture(), constraintsCaptor.capture());
        verifyConstants(Anchor.AFTER, GROUP_BUILD_TOOLBAR);

        verify(runContextGroup).addSeparator();
        verify(runContextGroup).add(runAction);
        verify(contextMenuGroup).add(runContextGroup);

        DefaultActionGroup runToolbarGroup = actionGroupCaptor.getValue();
        assertThat(runToolbarGroup.getChildrenCount(), is(2));

        verify(actionManager).registerAction(GROUP_RUN_TOOLBAR, runToolbarGroup);

        verifyConstants(Anchor.AFTER, GROUP_BUILD_TOOLBAR);
    }


    @Test
    public void runnerToolbarActionsShouldBeAddedToRegisteredEarlyGroup() throws Exception {
        // prepare step
        ActionManager actionManager = mock(ActionManager.class);

        RunAction runAction = mock(RunAction.class);
        RunWithAction runWithAction = mock(RunWithAction.class);
        ChooseRunnerAction chooseRunnerAction = mock(ChooseRunnerAction.class);

        DefaultActionGroup rightToolbarGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup runToolbarGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup runContextGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup contextMenuGroup = mock(DefaultActionGroup.class);
        DefaultActionGroup mainMenuGroup = mock(DefaultActionGroup.class);
        CreateCustomRunnerAction createCustomRunnerAction = mock(CreateCustomRunnerAction.class);

        when(actionManager.getAction(GROUP_RUN_TOOLBAR)).thenReturn(runToolbarGroup);
        when(actionManager.getAction(GROUP_RUN_CONTEXT_MENU)).thenReturn(runContextGroup);
        when(actionManager.getAction(GROUP_MAIN_CONTEXT_MENU)).thenReturn(contextMenuGroup);
        when(actionManager.getAction(GROUP_RUN)).thenReturn(mainMenuGroup);

        // test step
        extension.setUpRunActions(actionManager, runAction, runWithAction, chooseRunnerAction, createCustomRunnerAction);

        // check step
        verify(actionManager, never()).registerAction(GROUP_RUN_TOOLBAR, runToolbarGroup);
        verify(rightToolbarGroup, never()).add(eq(runToolbarGroup), constraintsCaptor.capture());
        verify(runToolbarGroup).add(chooseRunnerAction);
        verify(runToolbarGroup).add(eq(runAction), constraintsCaptor.capture());
        verifyConstants(Anchor.AFTER, ActionId.CHOOSE_RUNNER_ID.getId());

        verify(runContextGroup).addSeparator();
        verify(runContextGroup).add(runAction);
        verify(contextMenuGroup).add(runContextGroup);
    }

    private void verifyConstants(Anchor anchor, String actionId) {
        Constraints constraints = constraintsCaptor.getValue();
        assertThat(constraints.myAnchor, is(anchor));
        assertThat(constraints.myRelativeToActionId, is(actionId));
    }

}