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
package org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common;

import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.eclipse.che.ide.ext.runner.client.constants.TimeInterval;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.rest.Pair;
import com.google.gwt.user.client.Timer;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class LogMessagesHandlerTest {
    private static final String MESSAGE1 = "message1";
    private static final String MESSAGE2 = "message2";
    private static final String MESSAGE3 = "message3";

    //mocks for constructor
    @Mock
    private LogMessageUnmarshaller          unmarshaller;
    @Mock
    private ConsoleContainer                consoleContainer;
    @Mock
    private TimerFactory                    timerFactory;
    @Mock
    private Runner                          runner;
    @Mock
    private LogMessagesHandler.ErrorHandler errorHandler;

    @Mock
    private JsoArray<Pair> headers;
    @Mock
    private Pair           pair1;
    @Mock
    private Timer          flushTimer;
    @Mock
    private LogMessage     logMessage1;
    @Mock
    private LogMessage     logMessage2;
    @Mock
    private LogMessage     logMessage3;
    @Mock
    private Message        message1;
    @Mock
    private Message        message2;
    @Mock
    private Message        message3;

    private LogMessagesHandler logMessagesHandler;

    @Before
    public void setUp() {
        when(timerFactory.newInstance(any(TimerFactory.TimerCallBack.class))).thenReturn(flushTimer);

        logMessagesHandler = new LogMessagesHandler(unmarshaller, consoleContainer, timerFactory, runner, errorHandler);

        when(headers.size()).thenReturn(1);
        when(headers.get(0)).thenReturn(pair1);
        when(pair1.getName()).thenReturn("x-everrest-websocket-message-type");
        when(pair1.getValue()).thenReturn("none");
        when(message1.getHeaders()).thenReturn(headers);
        when(message2.getHeaders()).thenReturn(headers);
        when(message3.getHeaders()).thenReturn(headers);
        List<Pair> list = new ArrayList<>();
        list.add(pair1);
        when(headers.toList()).thenReturn(list);

        when(logMessage1.getText()).thenReturn(MESSAGE1);
        when(logMessage2.getText()).thenReturn(MESSAGE2);
        when(logMessage3.getText()).thenReturn(MESSAGE3);

        when(unmarshaller.getPayload()).thenReturn(logMessage1).thenReturn(logMessage2).thenReturn(logMessage3);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        when(logMessage1.getNumber()).thenReturn(2);
        logMessagesHandler.onMessage(message1);

        ArgumentCaptor<TimerFactory.TimerCallBack> timerCaptor = ArgumentCaptor.forClass(TimerFactory.TimerCallBack.class);
        verify(timerFactory).newInstance(timerCaptor.capture());
        timerCaptor.getValue().onRun();
        verify(logMessage1).getText();
        verify(consoleContainer).print(runner, MESSAGE1);
        verify(logMessage1, times(2)).getNumber();
    }

    @Test
    public void shouldStopWhenListOfMessageAreEmpty() throws Exception {
        logMessagesHandler.stop();
        verify(consoleContainer, never()).print(eq(runner), isNull(String.class));
        verify(flushTimer).cancel();
    }

    @Test
    public void shouldStop() throws Exception {
        when(logMessage1.getNumber()).thenReturn(3);
        when(logMessage2.getNumber()).thenReturn(2);
        when(logMessage3.getNumber()).thenReturn(4);

        logMessagesHandler.onMessage(message1);
        logMessagesHandler.onMessage(message2);
        logMessagesHandler.onMessage(message3);

        logMessagesHandler.stop();

        verify(logMessage1).getText();
        verify(consoleContainer).print(runner, MESSAGE1);
        verify(logMessage1, times(2)).getNumber();

        verify(logMessage2).getText();
        verify(consoleContainer).print(runner, MESSAGE2);
        verify(logMessage2, times(2)).getNumber();

        verify(logMessage3).getText();
        verify(consoleContainer).print(runner, MESSAGE3);
        verify(logMessage3, times(2)).getNumber();

        verify(flushTimer).cancel();
    }

    /* We should print message with right ordinal index */
    @Test
    public void shouldOnMessageWithRightOrdinalIndex() {
        when(logMessage1.getNumber()).thenReturn(1);

        logMessagesHandler.onMessage(message1);

        verify(flushTimer).cancel();
        verify(logMessage1).getText();
        verify(consoleContainer).print(runner, MESSAGE1);
        verify(logMessage1, times(2)).getNumber();

        verify(flushTimer).schedule(TimeInterval.FIVE_SEC.getValue());
    }

    /* If we get message with wrong ordinal index we can't print this message. This message we should put in queue.*/
    @Test
    public void shouldNotPrintMessageIfIndexMessageNotNext() {
        logMessagesHandler.onMessage(message1);

        verify(flushTimer, never()).cancel();
        verify(logMessage1, never()).getText();
        verify(consoleContainer, never()).print(runner, MESSAGE1);
        verify(logMessage1).getNumber();

        verify(flushTimer).schedule(TimeInterval.FIVE_SEC.getValue());
    }

    /*
     * First step: we put message with wrong ordinal index(2, 3). These messages we can't print, it should be save in the map.
     * Second: we put message with next index for terminal(with index 1), this message terminal will be print and after
     * that terminal print next messages(2, 3) in right order.
     */
    @Test
    public void shouldOnMessageWhenNextPostMessageIsNotNull() {
        when(logMessage1.getNumber()).thenReturn(2);
        when(logMessage2.getNumber()).thenReturn(3);
        when(logMessage3.getNumber()).thenReturn(1);

        logMessagesHandler.onMessage(message1);
        logMessagesHandler.onMessage(message2);
        logMessagesHandler.onMessage(message3);

        verify(flushTimer).cancel();

        verify(logMessage1).getText();
        verify(consoleContainer).print(runner, MESSAGE1);
        verify(logMessage1, times(3)).getNumber();

        verify(logMessage2).getText();
        verify(consoleContainer).print(runner, MESSAGE2);
        verify(logMessage2, times(3)).getNumber();

        verify(logMessage3).getText();
        verify(consoleContainer).print(runner, MESSAGE3);
        verify(logMessage3, times(2)).getNumber();

        verify(flushTimer, times(3)).schedule(TimeInterval.FIVE_SEC.getValue());
    }

    /*
     * First step: we put 2 message with wrong ordinal index (with index 2, 4). These messages we can't print, they should be save in map.
     * Second: we put message with next index for terminal (1). Terminal will be print this message and after that terminal print next
     * messages in right order (will be print message 2). Message 4 won't be print because there isn't message 3.
     */
    @Test
    public void shouldPrintMessageInOrderAndSaveMessageWhichNeedToPrintLater() {
        when(logMessage1.getNumber()).thenReturn(2);
        when(logMessage2.getNumber()).thenReturn(1);
        when(logMessage3.getNumber()).thenReturn(4);

        logMessagesHandler.onMessage(message1);
        logMessagesHandler.onMessage(message2);
        logMessagesHandler.onMessage(message3);

        verify(flushTimer).cancel();
        verify(logMessage1).getText();
        verify(consoleContainer).print(runner, MESSAGE1);
        verify(logMessage1, times(3)).getNumber();

        verify(logMessage2).getText();
        verify(consoleContainer).print(runner, MESSAGE2);
        verify(logMessage2, times(2)).getNumber();

        verify(logMessage3, never()).getText();
        verify(consoleContainer, never()).print(runner, MESSAGE3);
        verify(logMessage3).getNumber();

        verify(flushTimer, times(3)).schedule(TimeInterval.FIVE_SEC.getValue());
    }

    @Test
    public void shouldOnMessageWithErrorBecauseWrongHeaders() {
        when(headers.size()).thenReturn(1);
        when(headers.get(0)).thenReturn(pair1);
        when(pair1.getName()).thenReturn("wrong name");
        when(pair1.getValue()).thenReturn("wrong value");
        when(message1.getHeaders()).thenReturn(headers);

        logMessagesHandler.onMessage(message1);

        verify(errorHandler).onErrorHappened();
    }

    @Test
    public void shouldOnMessageWithErrorBecauseUnmarshallerException() throws UnmarshallerException {
        when(headers.size()).thenReturn(1);
        when(headers.get(0)).thenReturn(pair1);
        when(pair1.getName()).thenReturn("wrong name");
        when(pair1.getValue()).thenReturn("wrong value");
        when(message1.getHeaders()).thenReturn(headers);

        doThrow(UnmarshallerException.class).when(unmarshaller).unmarshal(message1);

        logMessagesHandler.onMessage(message1);

        verify(errorHandler).onErrorHappened();
    }
}
