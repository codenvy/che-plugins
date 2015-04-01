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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEventHandler;
import org.eclipse.che.ide.ext.runner.client.selection.Selection;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel.Terminal;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.RUNNING;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.STOPPED;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class TerminalContainerPresenterTest {
    @Captor
    private ArgumentCaptor<RunnerApplicationStatusEventHandler> statusEventHandlerArgumentCaptor;

    //variables for constructor
    @Mock
    private TerminalContainerView view;
    @Mock
    private WidgetFactory         widgetFactory;
    @Mock
    private SelectionManager      selectionManager;
    @Mock
    private EventBus              eventBus;

    @Mock
    private Runner   runner;
    @Mock
    private Terminal terminal;

    @InjectMocks
    private TerminalContainerPresenter presenter;

    @Before
    public void setUp() {
        when(selectionManager.getRunner()).thenReturn(runner);
        when(widgetFactory.createTerminal()).thenReturn(terminal);
    }

    @Test
    public void terminalIsNotRefreshedIfItIsNull() throws Exception {
        verify(eventBus).addHandler((Event.Type<RunnerApplicationStatusEventHandler>)any(), statusEventHandlerArgumentCaptor.capture());

        RunnerApplicationStatusEventHandler handlerValue = statusEventHandlerArgumentCaptor.getValue();
        handlerValue.onRunnerStatusChanged(runner);

        verify(terminal, never()).setVisible(anyBoolean());
        verify(terminal, never()).setUnavailableLabelVisible(anyBoolean());
        verify(terminal, never()).setUrl(runner);
    }

    @Test
    public void terminalIsNotRefreshedIfSelectedRunnerIsNull() throws Exception {
        presenter.onSelectionChanged(Selection.RUNNER);

        when(selectionManager.getRunner()).thenReturn(null);

        verify(eventBus).addHandler((Event.Type<RunnerApplicationStatusEventHandler>)any(), statusEventHandlerArgumentCaptor.capture());

        RunnerApplicationStatusEventHandler handlerValue = statusEventHandlerArgumentCaptor.getValue();
        handlerValue.onRunnerStatusChanged(runner);

        verify(terminal, never()).setVisible(anyBoolean());
        verify(terminal, never()).setUnavailableLabelVisible(anyBoolean());
        verify(terminal, never()).setUrl(runner);
    }

    @Test
    public void terminalIsNotRefreshedIfCurrentRunnerIsNotSelected() throws Exception {
        presenter.onSelectionChanged(Selection.RUNNER);

        Runner selectedRunner = mock(Runner.class);

        when(selectionManager.getRunner()).thenReturn(selectedRunner);

        verify(eventBus).addHandler((Event.Type<RunnerApplicationStatusEventHandler>)any(), statusEventHandlerArgumentCaptor.capture());

        RunnerApplicationStatusEventHandler handlerValue = statusEventHandlerArgumentCaptor.getValue();
        handlerValue.onRunnerStatusChanged(runner);

        verify(terminal, never()).setVisible(anyBoolean());
        verify(terminal, never()).setUnavailableLabelVisible(anyBoolean());
        verify(terminal, never()).setUrl(runner);
    }

    @Test
    public void terminalShouldBeVisibleIfStatusOfRunnerIsRunning() throws Exception {
        presenter.onSelectionChanged(Selection.RUNNER);

        when(selectionManager.getRunner()).thenReturn(runner);
        when(runner.getStatus()).thenReturn(RUNNING);

        verify(eventBus).addHandler((Event.Type<RunnerApplicationStatusEventHandler>)any(), statusEventHandlerArgumentCaptor.capture());

        RunnerApplicationStatusEventHandler handlerValue = statusEventHandlerArgumentCaptor.getValue();
        handlerValue.onRunnerStatusChanged(runner);

        verify(terminal).setVisible(true);
        verify(terminal).setUnavailableLabelVisible(false);
        verify(terminal).setUrl(runner);
    }

    @Test
    public void terminalShouldBeVisibleIfStatusOfRunnerIsNotRunning() throws Exception {
        presenter.onSelectionChanged(Selection.RUNNER);

        when(selectionManager.getRunner()).thenReturn(runner);
        when(runner.getStatus()).thenReturn(STOPPED);

        verify(eventBus).addHandler((Event.Type<RunnerApplicationStatusEventHandler>)any(), statusEventHandlerArgumentCaptor.capture());

        RunnerApplicationStatusEventHandler handlerValue = statusEventHandlerArgumentCaptor.getValue();
        handlerValue.onRunnerStatusChanged(runner);

        verify(terminal).setVisible(false);
        verify(terminal).setUnavailableLabelVisible(true);
        verify(terminal, never()).setUrl(runner);
    }

    @Test
    public void terminalUrlShouldBeRemoved() throws Exception {
        presenter.onSelectionChanged(Selection.RUNNER);

        presenter.removeTerminalUrl(runner);

        verify(terminal).removeUrl();
        verify(terminal).setVisible(false);
        verify(terminal).setUnavailableLabelVisible(true);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        verify(view).setDelegate(presenter);
        verify(selectionManager).addListener(presenter);
    }

    @Test
    public void shouldOnSelectionChangedToEnvironment() {
        reset(view, selectionManager);

        presenter.onSelectionChanged(Selection.ENVIRONMENT);

        verifyNoMoreInteractions(selectionManager, widgetFactory, view);
    }

    @Test
    public void shouldOnSelectionChangedToRunnerWhenRunnerIsNull() {
        reset(view, selectionManager);
        when(selectionManager.getRunner()).thenReturn(null);

        presenter.onSelectionChanged(Selection.RUNNER);

        verify(selectionManager).getRunner();
        verifyNoMoreInteractions(selectionManager, widgetFactory, view);
    }

    @Test
    public void shouldOnSelectionChangedToRunner() {
        presenter.onSelectionChanged(Selection.RUNNER);

        verify(selectionManager).getRunner();

        verify(widgetFactory).createTerminal();
        verify(view).addWidget(terminal);
    }

    @Test
    public void shouldOnSelectionChangedToRunnerWhenTerminalIsNotNullAndRunnerIsAlive() {
        when(runner.isAlive()).thenReturn(true);
        //generate terminal
        presenter.onSelectionChanged(Selection.RUNNER);

        presenter.onSelectionChanged(Selection.RUNNER);

        verify(selectionManager, times(2)).getRunner();

        verify(terminal).setVisible(false);
        verify(terminal, times(2)).setUnavailableLabelVisible(false);
        verify(runner).isAlive();
        verify(terminal).setVisible(true);
    }

    @Test
    public void shouldOnSelectionChangedToRunnerWhenTerminalIsNotNullAndRunnerIsNotAlive() {
        when(runner.isAlive()).thenReturn(false);
        //generate terminal
        presenter.onSelectionChanged(Selection.RUNNER);

        presenter.onSelectionChanged(Selection.RUNNER);

        verify(selectionManager, times(2)).getRunner();

        verify(terminal, times(2)).setVisible(false);
        verify(terminal).setUnavailableLabelVisible(false);
        verify(runner).isAlive();

        verify(terminal).setUnavailableLabelVisible(true);
    }

    @Test
    public void viewShouldBeReturned() {
        assertThat(presenter.getView(), CoreMatchers.<IsWidget>is(view));
    }

    @Test
    public void viewShouldBeVisible() {
        presenter.setVisible(true);

        view.setVisible(true);
    }

    @Test
    public void viewShouldNotBeVisible() {
        presenter.setVisible(false);

        view.setVisible(false);
    }

    @Test
    public void shouldGo() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        container.setWidget(view);
    }

    @Test
    public void terminalShouldBeUpdated() {
        presenter.onSelectionChanged(Selection.RUNNER);
        reset(terminal);

        presenter.update(runner);

        verify(terminal).update(runner);
    }

    @Test
    public void viewShouldBeReset() {
        presenter.onSelectionChanged(Selection.RUNNER);

        presenter.reset();

        verify(view).removeWidget(terminal);
    }
}