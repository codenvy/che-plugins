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
package org.eclipse.che.ide.ext.runner.client.tabs.console.container;

import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Console;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.ENVIRONMENT;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.RUNNER;

/**
 * @author Andrienko Alexander
 * @author Andrey Plotnikov
 */
@RunWith(GwtMockitoTestRunner.class)
public class ConsoleContainerPresenterTest {
    private static final String MESSAGE  = "message";
    private static final String MESSAGE2 = "message";

    @Mock
    private ConsoleContainerView view;
    @Mock
    private SelectionManager     selectionManager;
    @Mock
    private WidgetFactory        widgetFactory;

    @Mock
    private Runner  runner;
    @Mock
    private Console console;

    @InjectMocks
    private ConsoleContainerPresenter presenter;

    @Before
    public void setUp() {
        when(widgetFactory.createConsole(runner)).thenReturn(console);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        verify(view).setDelegate(presenter);
        verify(selectionManager).addListener(presenter);
    }

    @Test
    public void shouldPrintWhenConsoleIsNull() {
        presenter.print(runner, MESSAGE);

        verify(widgetFactory).createConsole(runner);
        verify(console).print(MESSAGE);
    }

    @Test
    public void messageShouldBePrintedWhenConsoleIsNotNull() {
        presenter.print(runner, MESSAGE);

        reset(widgetFactory, console);

        presenter.print(runner, MESSAGE2);

        verify(widgetFactory, never()).createConsole(runner);
        verify(console).print(MESSAGE);
    }

    @Test
    public void infoMessageShouldBePrintedWhenConsoleIsNull() {
        presenter.printInfo(runner, MESSAGE);

        verify(widgetFactory).createConsole(runner);
        verify(console).printInfo(MESSAGE);
    }

    @Test
    public void infoMessageShouldBePrintedWhenConsoleIsNotNull() {
        presenter.printInfo(runner, MESSAGE);

        reset(widgetFactory, console);

        presenter.printInfo(runner, MESSAGE2);

        verify(widgetFactory, never()).createConsole(runner);
        verify(console).printInfo(MESSAGE);
    }

    @Test
    public void errorMessageShouldBePrintedWhenConsoleIsNull() {
        presenter.printError(runner, MESSAGE);

        verify(widgetFactory).createConsole(runner);
        verify(console).printError(MESSAGE);
    }

    @Test
    public void errorMessageShouldBePrintedWhenConsoleIsNotNull() {
        presenter.printError(runner, MESSAGE);

        reset(widgetFactory, console);

        presenter.printError(runner, MESSAGE2);

        verify(widgetFactory, never()).createConsole(runner);
        verify(console).printError(MESSAGE);
    }

    @Test
    public void warningMessageShouldBePrintedWhenConsoleIsNull() {
        presenter.printWarn(runner, MESSAGE);

        verify(widgetFactory).createConsole(runner);
        verify(console).printWarn(MESSAGE);
    }

    @Test
    public void warningMessageShouldBePrintedWhenConsoleIsNotNull() {
        presenter.printWarn(runner, MESSAGE);

        reset(widgetFactory, console);

        presenter.printWarn(runner, MESSAGE2);

        verify(widgetFactory, never()).createConsole(runner);
        verify(console).printWarn(MESSAGE);
    }

    @Test
    public void shouldResetView() {
        presenter.printWarn(runner, MESSAGE);

        presenter.reset();

        verify(view).removeWidget(console);
    }

    @Test
    public void selectionShouldBeOnChangedWhenSelectionIsEnvironment() {
        presenter.onSelectionChanged(ENVIRONMENT);

        verify(selectionManager, never()).getRunner();
        verify(view, never()).showWidget(any(IsWidget.class));
    }

    @Test
    public void selectionShouldBeOnChangedWhenSelectionIsRunnerIsNull() {
        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();
        verify(view, never()).showWidget(any(IsWidget.class));
    }

    @Test
    public void selectionShouldBeOnChangedWhenSelectionIsRunner() {
        when(selectionManager.getRunner()).thenReturn(runner);

        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();
        verify(view).showWidget(console);
    }

    @Test
    public void viewShouldBeReturned() {
        assertThat(presenter.getView(), CoreMatchers.<IsWidget>is(view));
    }

    @Test
    public void presenterShouldBeVisible() {
        presenter.setVisible(true);
        verify(view).setVisible(true);
    }

    @Test
    public void presenterShouldNotBeVisible() {
        presenter.setVisible(false);
        verify(view).setVisible(false);
    }

    @Test
    public void shouldGo() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(view);
    }

    @Test
    public void scrollToBottomShouldBeNotPerformed() throws Exception {
        presenter.onScrollBottomClicked();

        verify(console, never()).scrollBottom();
    }

    @Test
    public void scrollToBottomShouldBePerformed() throws Exception {
        when(selectionManager.getRunner()).thenReturn(runner);
        presenter.onSelectionChanged(RUNNER);

        presenter.onScrollBottomClicked();

        verify(console).scrollBottom();
    }

    @Test
    public void shouldOnWrapTextClickedWhenSelectedConsoleIsNull() {
        reset(console, view);
        presenter.onWrapTextClicked();
        verifyZeroInteractions(console, view);
    }

    @Test
    public void shouldOnWrapTextClickedWhenIsWrapTextTrue() {
        when(console.isWrapText()).thenReturn(true);
        when(selectionManager.getRunner()).thenReturn(runner);
        presenter.onSelectionChanged(RUNNER);

        presenter.onWrapTextClicked();

        verify(console).changeWrapTextParam();
        verify(console).isWrapText();
        verify(view).selectWrapTextButton(true);
    }

    @Test
    public void shouldOnWrapTextClickedWhenIsWrapTextFalse() {
        when(console.isWrapText()).thenReturn(false);
        when(selectionManager.getRunner()).thenReturn(runner);
        presenter.onSelectionChanged(RUNNER);

        presenter.onWrapTextClicked();

        verify(console).changeWrapTextParam();
        verify(console).isWrapText();
        verify(view).selectWrapTextButton(false);
    }

    @Test
    public void cleanSelectedConsoleShouldBeNotPerformed() throws Exception {
        presenter.onCleanClicked();

        verify(console, never()).clear();
    }

    @Test
    public void cleanSelectedConsoleShouldBePerformed() throws Exception {
        when(selectionManager.getRunner()).thenReturn(runner);
        presenter.onSelectionChanged(RUNNER);

        presenter.onCleanClicked();

        verify(console).clear();
    }

    @Test
    public void selectedConsoleShouldBeDeleted() {
        presenter.deleteSelectedConsole();

        verify(selectionManager).getRunner();

        when(selectionManager.getRunner()).thenReturn(runner);
        presenter.onSelectionChanged(RUNNER);

        verify(widgetFactory).createConsole(runner);
        verify(view).showWidget(console);
    }
}