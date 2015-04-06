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
package org.eclipse.che.ide.ext.runner.client.tabs.history;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.history.runner.RunnerWidget;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class HistoryPresenterTest {
    @Mock
    private HistoryView      view;
    @Mock
    private WidgetFactory    widgetFactory;
    @Mock
    private SelectionManager selectionManager;
    @Mock
    private ConsoleContainer consolePresenter;

    @Mock
    private RunnerWidget runnerWidget;
    @Mock
    private RunnerWidget runnerWidget2;
    @Mock
    private Runner       runner;
    @Mock
    private Runner       runner2;

    @InjectMocks
    private HistoryPresenter historyPresenter;

    @Before
    public void setUp() {
        when(widgetFactory.createRunner()).thenReturn(runnerWidget).thenReturn(runnerWidget2);
    }

    @Test
    public void shouldAddRunner() {
        historyPresenter.addRunner(runner);

        verify(selectionManager).setRunner(runner);
        verify(widgetFactory).createRunner();
        verify(runnerWidget).setDelegate(historyPresenter);
        verify(runnerWidget).update(runner);
        verify(view).addRunner(runnerWidget);

        verify(runnerWidget).unSelect();
        verify(runnerWidget).select();
    }

    @Test
    public void runnerShouldNotBeAddedIfWidgetForThisRunningIsAlreadyCreated() {
        historyPresenter.addRunner(runner);
        reset(selectionManager, widgetFactory, view, runnerWidget);

        historyPresenter.addRunner(runner);

        verifyNoMoreInteractions(selectionManager, widgetFactory, view, runnerWidget);
    }

    @Test
    public void ifAddedNewRunnerShouldUpdateRunnerWidget() {
        historyPresenter.addRunner(runner);
        reset(runnerWidget);
        historyPresenter.update(runner);

        verify(runnerWidget).update(runner);
    }

    @Test
    public void ifAddedNoneRunnerShouldUpdateRunnerWidget() {
        historyPresenter.update(runner);

        verify(runnerWidget, never()).update(runner);
    }

    @Test
    public void oneRunnerShouldBeOnSelect() {
        historyPresenter.addRunner(runner);
        reset(runnerWidget);

        historyPresenter.selectRunner(runner);

        verify(runnerWidget).unSelect();
        verify(runnerWidget).select();
    }

    @Test
    public void OneRunnerFromTwoRunnerShouldAreOnSelect() {
        historyPresenter.addRunner(runner);
        historyPresenter.addRunner(runner2);
        reset(runnerWidget);
        reset(runnerWidget2);

        historyPresenter.selectRunner(runner);

        verify(runnerWidget).unSelect();
        verify(runnerWidget2).unSelect();

        verify(runnerWidget).select();
    }

    @Test
    public void viewShouldBeReturned() {
        assertThat(historyPresenter.getView(), CoreMatchers.<IsWidget>is(view));
    }

    @Test
    public void shouldSetVisibleTrue() {
        historyPresenter.setVisible(true);
        verify(view).setVisible(true);
    }

    @Test
    public void shouldSetVisibleFalse() {
        historyPresenter.setVisible(false);
        verify(view).setVisible(false);
    }

    @Test
    public void shouldGoContainer() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        historyPresenter.go(container);

        verify(container).setWidget(view);
    }

    @Test
    public void viewShouldBeCleared() {
        historyPresenter.clear();

        verify(view).clear();
    }

    @Test
    public void runnerShouldBeRemoved() throws Exception {
        historyPresenter.addRunner(runner);

        historyPresenter.onRunnerCleanBtnClicked(runner);

        verify(view).removeRunner(runnerWidget);
        verify(consolePresenter).deleteSelectedConsole();
        verify(selectionManager).getRunner();
    }

    @Test
    public void firstRunnerShouldBeSelectedWhenWeRemoveRunner() throws Exception {
        when(selectionManager.getRunner()).thenReturn(runner2);
        historyPresenter.addRunner(runner);
        historyPresenter.addRunner(runner2);

        historyPresenter.onRunnerCleanBtnClicked(runner2);

        verify(view).removeRunner(runnerWidget2);
        verify(consolePresenter).deleteSelectedConsole();
        verify(selectionManager).getRunner();

        verify(selectionManager, times(2)).setRunner(runner);
        verify(runnerWidget, times(3)).unSelect();
        verify(runnerWidget, times(2)).select();
    }

    @Test
    public void firstRunnerShouldNotBeSelectedWhenWeRemoveRunner() throws Exception {
        historyPresenter.addRunner(runner);
        historyPresenter.addRunner(runner2);
        reset(selectionManager, runnerWidget);
        when(selectionManager.getRunner()).thenReturn(runner);

        historyPresenter.onRunnerCleanBtnClicked(runner2);

        verify(view).removeRunner(runnerWidget2);
        verify(consolePresenter).deleteSelectedConsole();
        verify(selectionManager).getRunner();

        verify(selectionManager, never()).setRunner(runner);

        verify(runnerWidget, never()).select();
    }
}