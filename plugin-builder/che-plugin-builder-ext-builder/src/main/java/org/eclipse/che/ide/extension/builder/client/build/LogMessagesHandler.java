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
package org.eclipse.che.ide.extension.builder.client.build;

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.extension.builder.client.BuilderExtension;
import org.eclipse.che.ide.extension.builder.client.console.BuilderConsolePresenter;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Timer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class listens for log messages from the server
 * and process it. Logic of this class is slightly complicated
 * since we can't guaranty correct order of messages and
 * delivery it from the server over WebSocket connection.
 * So messages may be received in shuffled order and some
 * messages may be never received.
 *
 * @author Artem Zatsarynnyy
 */
class LogMessagesHandler extends SubscriptionHandler<LogMessage> {
    private final BuildTaskDescriptor     buildTaskDescriptor;
    private final BuilderConsolePresenter console;
    private final MessageBus              messageBus;
    private       int                     lastPrintedMessageNum;
    private Map<Integer, LogMessage> postponedMessages = new HashMap<>();
    private Timer flushTimer;

    LogMessagesHandler(BuildTaskDescriptor buildTaskDescriptor, BuilderConsolePresenter console, MessageBus messageBus) {
        super(new LogMessageUnmarshaller());
        this.buildTaskDescriptor = buildTaskDescriptor;
        this.console = console;
        this.messageBus = messageBus;
        flushTimer = new Timer() {
            @Override
            public void run() {
                printAllPostponedMessages();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected void onMessageReceived(LogMessage result) {
        if (result.num == lastPrintedMessageNum + 1) {
            flushTimer.cancel();
            printLine(result);
        } else if (result.num > lastPrintedMessageNum) {
            postponedMessages.put(result.num, result);
        }

        printNextPostponedMessages();
        flushTimer.schedule(5000);
    }

    /** Print all messages from buffer for the moment and stop handling. */
    void stop() {
        printAllPostponedMessages();
        flushTimer.cancel();
    }

    /** Print next postponed messages with contiguous line numbers. */
    private void printNextPostponedMessages() {
        LogMessage nextLogMessage = postponedMessages.get(lastPrintedMessageNum + 1);
        while (nextLogMessage != null) {
            printLine(nextLogMessage);

            postponedMessages.remove(nextLogMessage.num);
            nextLogMessage = postponedMessages.get(nextLogMessage.num + 1);
        }
    }

    /** Print all postponed messages in correct order. */
    private void printAllPostponedMessages() {
        for (int i = lastPrintedMessageNum + 1; !postponedMessages.isEmpty(); i++) {
            LogMessage nextLogMessage = postponedMessages.get(i);
            if (nextLogMessage != null) {
                printLine(nextLogMessage);
                postponedMessages.remove(i);
            }
        }
    }

    private void printLine(LogMessage logMessage) {
        console.print(logMessage.text);
        lastPrintedMessageNum = logMessage.num;
    }

    /** {@inheritDoc} */
    @Override
    protected void onErrorReceived(Throwable throwable) {
        try {
            messageBus.unsubscribe(BuilderExtension.BUILD_OUTPUT_CHANNEL + buildTaskDescriptor.getTaskId(), this);
            Log.error(LogMessagesHandler.class, throwable);
        } catch (WebSocketException e) {
            Log.error(LogMessagesHandler.class, e);
        }
    }
}

class LogMessageUnmarshaller implements Unmarshallable<LogMessage> {
    LogMessage logMessage;

    /** {@inheritDoc} */
    @Override
    public void unmarshal(Message response) throws UnmarshallerException {
        JSONObject jsonObject = JSONParser.parseStrict(response.getBody()).isObject();
        if (jsonObject != null && jsonObject.containsKey("line")) {
            final int lineNumber = (int)jsonObject.get("num").isNumber().doubleValue();
            final String text = jsonObject.get("line").isString().stringValue();
            logMessage = new LogMessage(lineNumber, text);
        }
    }

    /** {@inheritDoc} */
    @Override
    public LogMessage getPayload() {
        return logMessage;
    }
}

class LogMessage {
    int    num;
    String text;

    LogMessage(int lineNumber, String text) {
        this.num = lineNumber;
        this.text = text;
    }
}
