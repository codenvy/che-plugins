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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.console.button.ConsoleButton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class ConsoleContainerViewImplTest {
    private static final String SOME_MESSAGE = "some message";

    // constructor params
    @Mock
    private RunnerResources            resources;
    @Mock
    private WidgetFactory              widgetFactory;
    @Mock
    private RunnerLocalizationConstant locale;

    // local params
    @Mock
    private ConsoleButton                       button1;
    @Mock
    private ConsoleButton                       button2;
    @Mock
    private ConsoleButton                       button3;
    @Mock
    private ConsoleContainerView.ActionDelegate delegate;

    @Captor
    private ArgumentCaptor<ConsoleButton.ActionDelegate> delegateCaptor;

    private ConsoleContainerViewImpl view;

    @Before
    public void setUp() throws Exception {
        when(widgetFactory.createConsoleButton(anyString(), any(SVGResource.class))).thenReturn(button1)
                                                                                    .thenReturn(button2)
                                                                                    .thenReturn(button3);

        when(locale.consoleTooltipScroll()).thenReturn(SOME_MESSAGE);
        when(locale.consoleTooltipClear()).thenReturn(SOME_MESSAGE);

        view = new ConsoleContainerViewImpl(resources, widgetFactory, locale);
        view.setDelegate(delegate);
    }

    @Test
    public void constructorActionShouldBePerformed() throws Exception {
        verify(resources).arrowBottom();
        verify(resources).erase();

        verify(locale).consoleTooltipScroll();
        verify(locale).consoleTooltipClear();
    }

    @Test
    public void shouldShowWidget() {
        IsWidget console = mock(IsWidget.class);

        view.showWidget(console);

        verify(view.mainPanel).setWidget(console);
    }

    @Test
    public void wrapTextButtonActionShouldBePerformed() throws Exception {
        verify(button1).setDelegate(delegateCaptor.capture());

        ConsoleButton.ActionDelegate buttonDelegate = delegateCaptor.getValue();
        buttonDelegate.onButtonClicked();

        verify(delegate).onWrapTextClicked();
    }

    @Test
    public void cleanButtonActionShouldBePerformed() throws Exception {
        verify(button3).setDelegate(delegateCaptor.capture());

        ConsoleButton.ActionDelegate buttonDelegate = delegateCaptor.getValue();
        buttonDelegate.onButtonClicked();

        verify(delegate).onCleanClicked();
    }

    @Test
    public void scrollBottomButtonActionShouldBePerformed() throws Exception {
        verify(button2).setDelegate(delegateCaptor.capture());

        ConsoleButton.ActionDelegate buttonDelegate = delegateCaptor.getValue();
        buttonDelegate.onButtonClicked();

        verify(delegate).onScrollBottomClicked();
    }

    @Test
    public void shouldSelectWrapTextButtonIsTrue() {
        view.selectWrapTextButton(true);

        verify(button1).setCheckedStatus(true);
    }

    @Test
    public void shouldRemoveWidget() {
        IsWidget console  = mock(IsWidget.class);

        view.removeWidget(console);

        verify(view.mainPanel).remove(console);
    }

    @Test
    public void shouldSelectWrapTextButtonIsFalse() {
        view.selectWrapTextButton(false);

        verify(button1).setCheckedStatus(false);
    }

    @Test
    public void dummyContentShouldBeShowed() throws Exception {
        view.setVisibleDummyContent(true);

        verify(view.noRunnerLabel).setVisible(true);
    }
}