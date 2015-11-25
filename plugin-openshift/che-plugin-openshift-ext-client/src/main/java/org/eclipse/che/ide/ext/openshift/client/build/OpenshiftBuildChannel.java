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
package org.eclipse.che.ide.ext.openshift.client.build;

import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.WebSocket;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.ConnectionErrorHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.MessageReceivedHandler;

/**
 * WebSocket channel for watching of builds statuses of openshift application
 *
 * @author Sergii Leschenko
 */
public class OpenshiftBuildChannel {
    private       WebSocket      ws;

    private OpenshiftBuildChannel(String openshiftWebSocketPath, String token, String namespace) {
        String wsConnectionUrl = openshiftWebSocketPath + "/watch/namespaces/" + namespace + "/builds?access_token=" + token;

        if (WebSocket.isSupported()) {
            ws = WebSocket.create(wsConnectionUrl);
        } else {
            Log.error(getClass(), "WebSocket is not supported.");
        }
    }

    public void close() {
        ws.close();
    }

    public static class Builder {
        private       ConnectionOpenedHandler openedHandler;
        private       ConnectionClosedHandler closedHandler;
        private       ConnectionErrorHandler  errorHandler;
        private       MessageReceivedHandler  messageHandler;
        private final String                  webSocketPath;
        private final String                  namespace;
        private final String                  token;

        Builder(String webSocketPath, String namespace, String token) {
            this.webSocketPath = webSocketPath;
            this.namespace = namespace;
            this.token = token;
        }

        public Builder withOpenedHandler(ConnectionOpenedHandler openedHandler) {
            this.openedHandler = openedHandler;
            return this;
        }

        public Builder withErrorHandler(ConnectionErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        public Builder withClosedHandler(ConnectionClosedHandler closedHandler) {
            this.closedHandler = closedHandler;
            return this;
        }

        public Builder withMessageHandler(MessageReceivedHandler messageHandler) {
            this.messageHandler = messageHandler;
            return this;
        }

        public OpenshiftBuildChannel build() {
            OpenshiftBuildChannel openshiftBuildChannel = new OpenshiftBuildChannel(webSocketPath, token, namespace);
            if (messageHandler != null) {
                openshiftBuildChannel.ws.setOnMessageHandler(messageHandler);
            }

            if (errorHandler != null) {
                openshiftBuildChannel.ws.setOnErrorHandler(errorHandler);
            }

            if (closedHandler != null) {
                openshiftBuildChannel.ws.setOnCloseHandler(closedHandler);
            }

            if (openedHandler != null) {
                openshiftBuildChannel.ws.setOnOpenHandler(openedHandler);
            }
            return openshiftBuildChannel;
        }
    }
}
