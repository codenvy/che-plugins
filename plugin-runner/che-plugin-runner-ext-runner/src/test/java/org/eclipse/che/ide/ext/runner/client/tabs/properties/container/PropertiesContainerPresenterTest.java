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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.container;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.runner.client.selection.Selection.ENVIRONMENT;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.RUNNER;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesContainerPresenterTest {

    @Mock
    private PropertiesContainerView view;
    @Mock
    private WidgetFactory           widgetFactory;
    @Mock
    private SelectionManager        selectionManager;
    @Mock
    private AppContext              appContext;

    @Mock
    private Runner          runner;
    @Mock
    private PropertiesPanel currentPanel;
    @Mock
    private PropertiesPanel stabPanel;
    @Mock
    private Environment     environment;
    @Mock
    private CurrentProject  currentProject;

    private PropertiesContainerPresenter presenter;

    @Before
    public void setUp() {
        when(widgetFactory.createPropertiesPanel()).thenReturn(stabPanel);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        presenter = new PropertiesContainerPresenter(view, widgetFactory, selectionManager, appContext);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        verify(view).setDelegate(presenter);
        verify(widgetFactory).createPropertiesPanel();
        verify(selectionManager).addListener(presenter);
    }

    @Test
    public void panelShouldBeShownWhenRunnerIsNull() {
        reset(widgetFactory, currentPanel, view);

        presenter.show((Runner)null);

        verify(view).clear();

        verifyNoMoreInteractions(widgetFactory, currentPanel, view);
    }

    @Test
    public void panelForRunnerShouldBeCreatedAndShownWhenCurrentPanelIsNull() {
        when(widgetFactory.createPropertiesPanel(runner)).thenReturn(currentPanel);

        presenter.show(runner);

        verify(widgetFactory).createPropertiesPanel(runner);

        verify(currentPanel).update(runner);
        verify(view).showWidget(currentPanel);
        verify(currentPanel).hideButtonsPanel();
    }

    @Test
    public void ifPanelForRunnerIsAlreadyCreatedWeShouldNotCreateNewPanel() {
        when(widgetFactory.createPropertiesPanel(runner)).thenReturn(currentPanel);

        presenter.show(runner);
        presenter.show(runner);

        verify(widgetFactory).createPropertiesPanel(runner);
        verify(currentPanel, times(2)).update(runner);
        verify(view, times(2)).showWidget(currentPanel);
    }

    @Test
    public void panelForEnvironmentShouldBeCreatedAndShownWhenCurrentPanelIsNull() {
        when(widgetFactory.createPropertiesPanel(environment)).thenReturn(currentPanel);

        presenter.show(environment);

        verify(widgetFactory).createPropertiesPanel(environment);
        verify(currentPanel).addListener(presenter);

        verify(currentPanel).update(environment);
        verify(view).showWidget(currentPanel);
    }

    @Test
    public void panelForEnvironmentShouldBeCreatedAndShownWhenCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.show(environment);

        verify(appContext).getCurrentProject();

        verify(view).showWidget(stabPanel);
    }

    @Test
    public void stubShouldBeShownWhen() {
        reset(widgetFactory, currentPanel, view);

        presenter.show((Environment)null);

        verify(view).showWidget(stabPanel);

        verifyNoMoreInteractions(widgetFactory, currentPanel, view);
    }

    @Test
    public void widgetCreatedByEnvironmentShouldBeShownWhenCurrentPanelIsNotNull() {
        when(widgetFactory.createPropertiesPanel(environment)).thenReturn(currentPanel);

        presenter.show(environment);
        presenter.show(environment);

        verify(widgetFactory).createPropertiesPanel(environment);

        verify(currentPanel, times(2)).update(environment);
        verify(view, times(2)).showWidget(currentPanel);
    }

    @Test
    public void viewShouldBeReturned() {
        assertThat(presenter.getView(), Is.<IsWidget>is(view));
    }

    @Test
    public void viewShouldBeVisible() {
        presenter.setVisible(true);

        verify(view).setVisible(true);
    }

    @Test
    public void viewShouldNotBeVisible() {
        presenter.setVisible(false);

        verify(view).setVisible(false);
    }

    @Test
    public void containerShouldGoneWidget() {
        when(widgetFactory.createPropertiesPanel(runner)).thenReturn(currentPanel);
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.show(runner);
        reset(view);
        presenter.go(container);

        verify(container).setWidget(view);
        verify(view).showWidget(currentPanel);
    }

    @Test
    public void presenterShouldApplyContainerButShouldNotShowPanelIfPanelIsNull() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(view);
        verify(view, never()).showWidget(currentPanel);
    }

    @Test
    public void runnerShouldBeNotChangedIfSelectionIsEnvironment() {
        reset(selectionManager, widgetFactory, currentPanel, view);
        presenter.onSelectionChanged(ENVIRONMENT);

        verifyNoMoreInteractions(selectionManager, widgetFactory, currentPanel, view);
    }

    @Test
    public void runnerSelectionShouldBeNotChangedIfRunnerIsNull() {
        reset(selectionManager, widgetFactory, currentPanel, view);
        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();
        verifyNoMoreInteractions(selectionManager, widgetFactory, currentPanel, view);
    }

    @Test
    public void runnerSelectionShouldBeChangedAndShowPanel() {
        when(widgetFactory.createPropertiesPanel(runner)).thenReturn(currentPanel);
        when(selectionManager.getRunner()).thenReturn(runner);

        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();
        verify(widgetFactory).createPropertiesPanel(runner);
        verify(currentPanel).update(runner);
        verify(view).showWidget(currentPanel);
    }

    @Test
    public void widgetFactoryShouldCreateNewPanelForRunnerAfterResetPresenter() {
        when(widgetFactory.createPropertiesPanel(runner)).thenReturn(currentPanel);

        presenter.show(runner);
        presenter.reset();
        presenter.show(runner);

        verify(widgetFactory, times(2)).createPropertiesPanel(runner);
    }

    @Test
    public void widgetFactoryShouldCreateNewPanelForEnvironmentAfterRemovingThisPanel() {
        PropertiesPanel newPanel = mock(PropertiesPanel.class);
        when(widgetFactory.createPropertiesPanel(environment)).thenReturn(currentPanel).thenReturn(newPanel);

        presenter.show(environment);
        presenter.onPanelRemoved(environment);
        presenter.show(environment);

        verify(widgetFactory, times(2)).createPropertiesPanel(environment);
    }

    @Test
    public void dummyContentShouldBeShowed() throws Exception {
        presenter.setVisibleNoRunnerLabel(true);

        verify(view).setVisibleNoRunnerLabel(true);
    }

}