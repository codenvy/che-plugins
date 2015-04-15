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
package org.eclipse.che.ide.ext.runner.client.tabs.history.runner;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.common.item.ItemWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.history.runner.RunnerWidget.ActionDelegate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.DONE;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.FAILED;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_PROGRESS;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.STOPPED;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RunnerWidgetTest {
    private static final String TEXT = "text";

    @Captor
    private ArgumentCaptor<MouseOutHandler>  mouseOutCaptor;
    @Captor
    private ArgumentCaptor<MouseOverHandler> mouseOverCaptor;
    @Captor
    private ArgumentCaptor<ClickHandler>     clickCaptor;

    @Mock
    private ItemWidget       itemWidget;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private RunnerResources  resources;
    @Mock
    private SelectionManager selectionManager;

    @Mock
    private Runner                    runner;
    @Mock
    private RunnerResources.RunnerCss css;
    @Mock
    private SimpleLayoutPanel         imagePanel;
    @Mock
    private MouseOutEvent             mouseOutEvent;
    @Mock
    private MouseOverEvent            mouseOverEvent;
    @Mock
    private ClickEvent                clickEvent;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private SVGResource               svgResource;
    @Mock
    private ActionDelegate            delegate;


    private RunnerWidget runnerWidget;

    @Before
    public void setUp() {
        when(resources.runnerInProgress()).thenReturn(svgResource);
        when(resources.runnerInQueue()).thenReturn(svgResource);
        when(resources.runnerFailed()).thenReturn(svgResource);
        when(resources.runnerTimeout()).thenReturn(svgResource);
        when(resources.runnerDone()).thenReturn(svgResource);
        when(resources.runnerDone()).thenReturn(svgResource);
        when(resources.erase()).thenReturn(svgResource);

        when(resources.runnerCss().whiteColor()).thenReturn(TEXT);

        when(itemWidget.getImagePanel()).thenReturn(imagePanel);

        runnerWidget = new RunnerWidget(itemWidget, resources, selectionManager);

        when(resources.runnerCss()).thenReturn(css);
        when(runner.getTitle()).thenReturn(TEXT);
        when(runner.getRAM()).thenReturn(512);
        when(runner.getCreationTime()).thenReturn(TEXT);

        runnerWidget.setDelegate(delegate);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        ArgumentCaptor<ItemWidget.ActionDelegate> actionDelegateCaptor =
                ArgumentCaptor.forClass(ItemWidget.ActionDelegate.class);

        verify(resources).runnerInProgress();
        verify(resources).runnerInQueue();
        verify(resources).runnerFailed();
        verify(resources).runnerTimeout();
        verify(resources, times(2)).runnerDone();
        verify(itemWidget).getImagePanel();
        verify(resources).erase();

        verify(itemWidget).setDelegate(actionDelegateCaptor.capture());
        ItemWidget.ActionDelegate actionDelegate = actionDelegateCaptor.getValue();
        actionDelegate.onWidgetClicked();

        verify(selectionManager).setRunner(any(Runner.class));
    }

    @Test
    public void onMouseOutEventShouldBePerformed() throws Exception {
        when(runner.getStatus()).thenReturn(IN_PROGRESS);
        runnerWidget.update(runner);
        reset(itemWidget);

        verify(imagePanel).addDomHandler(mouseOutCaptor.capture(), eq(MouseOutEvent.getType()));

        mouseOutCaptor.getValue().onMouseOut(mouseOutEvent);

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void onMouseOverEventShouldBePerformedWhenRunnerStatusIsFailed() throws Exception {
        when(runner.getStatus()).thenReturn(FAILED);

        verifyMouseOverEvent();
    }

    private void verifyMouseOverEvent() {
        runnerWidget.update(runner);
        reset(itemWidget);

        verify(imagePanel).addDomHandler(mouseOverCaptor.capture(), eq(MouseOverEvent.getType()));

        mouseOverCaptor.getValue().onMouseOver(mouseOverEvent);

        verify(resources).erase();
        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void onMouseOverEventShouldBePerformedWhenRunnerStatusIsStopped() throws Exception {
        when(runner.getStatus()).thenReturn(STOPPED);

        verifyMouseOverEvent();
    }

    @Test
    public void imageShouldNotBeSetWhenStatusIsNotFailedOrStopped() throws Exception {
        when(runner.getStatus()).thenReturn(DONE);

        runnerWidget.update(runner);
        reset(itemWidget);

        verify(imagePanel).addDomHandler(mouseOverCaptor.capture(), eq(MouseOverEvent.getType()));

        mouseOverCaptor.getValue().onMouseOver(mouseOverEvent);

        verify(itemWidget, never()).setImage(any(SVGImage.class));
    }

    @Test
    public void clickEventShouldBePerformedWhenRunnerStatusIsFailed() throws Exception {
        when(runner.getStatus()).thenReturn(FAILED);

        verifyClickEvent();
    }

    private void verifyClickEvent() {
        runnerWidget.update(runner);
        reset(itemWidget);

        verify(imagePanel).addDomHandler(clickCaptor.capture(), eq(ClickEvent.getType()));

        clickCaptor.getValue().onClick(clickEvent);

        verify(delegate).onRunnerCleanBtnClicked(runner);
    }

    @Test
    public void clickEventShouldBePerformedWhenRunnerStatusIsStopped() throws Exception {
        when(runner.getStatus()).thenReturn(STOPPED);

        verifyClickEvent();
    }

    @Test
    public void clickEventShouldBePerformedWhenRunnerStatusIsNotFailedOrStopped() throws Exception {
        when(runner.getStatus()).thenReturn(DONE);
        runnerWidget.update(runner);
        reset(itemWidget);

        verify(imagePanel).addDomHandler(clickCaptor.capture(), eq(ClickEvent.getType()));

        clickCaptor.getValue().onClick(clickEvent);

        verify(delegate, never()).onRunnerCleanBtnClicked(runner);
    }

    @Test
    public void shouldSelect() {
        runnerWidget.select();

        verify(itemWidget).select();
    }

    @Test
    public void shouldUnSelect() {
        runnerWidget.unSelect();

        verify(itemWidget).unSelect();
    }

    @Test
    public void shouldUpdateRunnerWithStatusInProgress() {
        when(runner.getStatus()).thenReturn(IN_PROGRESS);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).blueColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldUpdateRunnerWithStatusQueue() {
        when(runner.getStatus()).thenReturn(Runner.Status.IN_QUEUE);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).yellowColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldUpdateRunnerWithStatusFailed() {
        when(runner.getStatus()).thenReturn(Runner.Status.FAILED);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).redColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldUpdateRunnerWithStatusTimeOut() {
        when(runner.getStatus()).thenReturn(Runner.Status.TIMEOUT);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).whiteColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldUpdateRunnerWithStatusStopped() {
        when(runner.getStatus()).thenReturn(Runner.Status.STOPPED);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).redColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldUpdateRunnerWithStatusDone() {
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);
        when(css.blueColor()).thenReturn(TEXT);

        runnerWidget.update(runner);

        verify(css).greenColor();

        shouldUpdateItemWidgetParameter();
    }

    @Test
    public void shouldAsWidget() {
        runnerWidget.asWidget();

        verify(itemWidget).asWidget();
    }

    private void shouldUpdateItemWidgetParameter() {
        verify(itemWidget).setImage(any(SVGImage.class));
        verify(runner).getTitle();
        verify(itemWidget).setName(TEXT);
        verify(runner).getRAM();
        verify(itemWidget).setDescription("512MB");
        verify(runner).getCreationTime();
        verify(itemWidget).setStartTime(TEXT);
    }
}