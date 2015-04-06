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

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesView.ActionDelegate;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.defaultrunnerinfo.DefaultRunnerInfo;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class TemplatesViewImplTest {

    private static final String SOME_TEXT = "someText";

    private Map<Scope, List<Environment>> environmentMap;

    //constructor mocks
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private RunnerResources            resources;
    @Mock
    private WidgetFactory              widgetFactory;
    @Mock
    private DefaultRunnerInfo          defaultRunnerInfo;
    @Mock
    private PopupPanel                 popupPanel;

    //additional mocks
    @Mock
    private EnvironmentWidget widget;
    @Mock
    private Environment       systemEnvironment1;
    @Mock
    private Environment       systemEnvironment2;
    @Mock
    private Environment       projectEnvironment1;
    @Mock
    private Environment       projectEnvironment2;
    @Mock
    private FilterWidget      filterWidget;
    @Mock
    private EnvironmentWidget environmentWidget;
    @Mock
    private MouseOverEvent    overEvent;
    @Mock
    private MouseOutEvent     outEvent;
    @Mock
    private ActionDelegate    delegate;

    @Captor
    private ArgumentCaptor<MouseOverHandler> mouseOverCaptor;
    @Captor
    private ArgumentCaptor<MouseOutHandler>  mouseOutCaptor;

    @InjectMocks
    private TemplatesViewImpl view;

    @Before
    public void setUp() throws Exception {
        view.setDelegate(delegate);

        when(locale.templatesDefaultRunnerStub()).thenReturn(SOME_TEXT);
        when(resources.runnerCss().fullSize()).thenReturn(SOME_TEXT);
        when(resources.runnerCss().defaultRunnerStub()).thenReturn(SOME_TEXT);
        when(resources.runnerCss().fontSizeTen()).thenReturn(SOME_TEXT);

        environmentMap = new EnumMap<>(Scope.class);
        environmentMap.put(PROJECT, Arrays.asList(projectEnvironment1, projectEnvironment2));
        environmentMap.put(SYSTEM, Arrays.asList(systemEnvironment1, systemEnvironment2));

        when(widgetFactory.createEnvironment()).thenReturn(widget);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(locale).templatesDefaultRunnerStub();

        verify(resources.runnerCss()).fullSize();
        verify(resources.runnerCss()).defaultRunnerStub();
        verify(resources.runnerCss()).fontSizeTen();
    }

    @Test
    public void onMouseOverHandlerShouldBeFired() {
        verify(view.defaultRunner).addDomHandler(mouseOverCaptor.capture(), eq(MouseOverEvent.getType()));

        mouseOverCaptor.getValue().onMouseOver(overEvent);

        verify(delegate).onDefaultRunnerMouseOver();
    }

    @Test
    public void onMouseOutHandlerShouldBeFired() {
        verify(view.defaultRunner).addDomHandler(mouseOutCaptor.capture(), eq(MouseOutEvent.getType()));

        mouseOutCaptor.getValue().onMouseOut(outEvent);

        verify(popupPanel).hide();
    }

    @Test
    public void environmentsShouldBeAdded() throws Exception {
        view.addEnvironment(environmentMap);

        verify(view.environmentsPanel).clear();
        verify(widget, times(2)).setScope(PROJECT);
        verify(widget, times(2)).setScope(SYSTEM);
        verify(widget).update(projectEnvironment1);
        verify(widget).update(projectEnvironment2);
        verify(widget).update(systemEnvironment1);
        verify(widget).update(systemEnvironment2);
        verify(widgetFactory, times(4)).createEnvironment();
        verify(view.environmentsPanel, times(4)).add(widget);
    }

    @Test
    public void widgetsShouldBeGotFromCache() throws Exception {
        view.addEnvironment(environmentMap);

        view.addEnvironment(environmentMap);

        verify(widget, times(4)).unSelect();
        verify(widgetFactory, times(4)).createEnvironment();
        verify(view.environmentsPanel, times(8)).add(widget);
    }

    @Test
    public void environmentShouldBeSelected() throws Exception {
        view.addEnvironment(environmentMap);

        view.selectEnvironment(projectEnvironment1);

        verify(widget, times(4)).unSelect();
        verify(widget).select();
    }

    @Test
    public void filterWidgetShouldBwSet() throws Exception {
        view.setFilterWidget(filterWidget);

        verify(view.filterPanel).setWidget(filterWidget);
    }

    @Test
    public void environmentsPanelShouldBeCleared() throws Exception {
        view.clearEnvironmentsPanel();

        verify(view.environmentsPanel).clear();
    }

    @Test
    public void defaultRunnerShouldBeSet() throws Exception {
        view.setDefaultProjectWidget(environmentWidget);

        verify(view.defaultRunner).setWidget(environmentWidget);
    }

    @Test
    public void defaultEnvironmentInfoShouldBeSet() throws Exception {
        when(view.defaultRunner.getAbsoluteLeft()).thenReturn(0);
        when(view.defaultRunner.getAbsoluteTop()).thenReturn(0);

        view.showDefaultEnvironmentInfo(systemEnvironment1);

        verify(popupPanel).setPopupPosition(175, -90);
        verify(popupPanel).show();
    }
}