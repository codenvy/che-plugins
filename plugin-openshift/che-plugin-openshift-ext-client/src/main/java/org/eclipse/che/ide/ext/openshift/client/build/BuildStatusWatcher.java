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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftWebSocketPathProvider;
import org.eclipse.che.ide.ext.openshift.client.dto.BuildChangeEvent;
import org.eclipse.che.ide.ext.openshift.client.oauth.OAuthTokenChangedEvent;
import org.eclipse.che.ide.ext.openshift.client.oauth.OAuthTokenChangedEventHandler;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectMeta;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.MessageReceivedEvent;
import org.eclipse.che.ide.websocket.events.MessageReceivedHandler;
import org.eclipse.che.ide.websocket.events.WebSocketClosedEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Complete;
import static org.eclipse.che.ide.ext.openshift.shared.dto.BuildStatus.Phase.Failed;

/**
 * @author Sergii Leschenko
 */
@Singleton
public class BuildStatusWatcher {
    private final NotificationManager            notificationManager;
    private final OpenshiftAuthorizationHandler  authorizationHandler;
    private final DtoFactory                     dtoFactory;
    private final OpenshiftWebSocketPathProvider wsPathProvider;

    private final Map<BuildId, Notification>         buildsToNotifications = new HashMap<>();
    private final Map<String, OpenshiftBuildChannel> namespacesToChannels  = new HashMap<>();
    private final Map<BuildId, String>               buildsToNamespaces    = new HashMap<>();
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public BuildStatusWatcher(NotificationManager notificationManager,
                              OpenshiftAuthorizationHandler authorizationHandler,
                              EventBus eventBus,
                              DtoFactory dtoFactory,
                              final OpenshiftLocalizationConstant locale,
                              OpenshiftWebSocketPathProvider wsPathProvider) {
        this.notificationManager = notificationManager;
        this.authorizationHandler = authorizationHandler;
        this.dtoFactory = dtoFactory;
        this.locale = locale;
        this.wsPathProvider = wsPathProvider;

        eventBus.addHandler(OAuthTokenChangedEvent.TYPE, new OAuthTokenChangedEventHandler() {
            @Override
            public void onTokenChange(String token) {
                if (token == null) {
                    //stop watching all builds and close all WebSocket connection
                    for (Map.Entry<BuildId, Notification> buildToNotification : buildsToNotifications.entrySet()) {
                        final Notification notification = buildToNotification.getValue();
                        notification.setType(Type.ERROR);
                        notification.setMessage(locale.failedToRetrieveTokenMessage(buildToNotification.getKey().toString()));
                    }
                    buildsToNotifications.clear();

                    for (OpenshiftBuildChannel openshiftBuildChannel : namespacesToChannels.values()) {
                        openshiftBuildChannel.close();
                    }
                    namespacesToChannels.clear();
                    buildsToNamespaces.clear();
                }
            }
        });
    }

    public void watch(final Build build) {
        final BuildId buildId = BuildId.of(build);
        final Notification notification = new Notification(locale.buildStatusRunning(buildId.toString()), PROGRESS);
        notificationManager.showNotification(notification);

        buildsToNotifications.put(buildId, notification);
        buildsToNamespaces.put(buildId, buildId.getNamespace());

        final String namespace = build.getMetadata().getNamespace();
        if (!namespacesToChannels.containsKey(namespace)) {
            final WebSocketWatcher webSocketWatcher = new WebSocketWatcher(namespace);
            wsPathProvider.get(new AsyncCallback<String>() {
                @Override
                public void onSuccess(String openshiftWebSocketEndpoint) {
                    OpenshiftBuildChannel buildChannel =
                            new OpenshiftBuildChannel.Builder(openshiftWebSocketEndpoint, namespace, authorizationHandler.getToken())
                                    .withMessageHandler(webSocketWatcher)
                                    .withClosedHandler(webSocketWatcher)
                                    .build();
                    namespacesToChannels.put(namespace, buildChannel);
                }

                @Override
                public void onFailure(Throwable caught) {
                    stopWatching(buildId);
                    notification.setType(Type.ERROR);
                    notification.setMessage(locale.failedToWatchBuildByWebSocket(buildId.toString()) + " " + caught.getLocalizedMessage());
                }
            });
        }
    }

    private void stopWatching(BuildId build) {
        buildsToNotifications.remove(build);
        buildsToNamespaces.remove(build);

        final OpenshiftBuildChannel openshiftBuildChannel = namespacesToChannels.get(build.getNamespace());

        if (openshiftBuildChannel != null && !buildsToNamespaces.containsValue(build.getNamespace())) {
            openshiftBuildChannel.close();
            namespacesToChannels.remove(build.getNamespace());
        }
    }

    private class WebSocketWatcher implements MessageReceivedHandler, ConnectionClosedHandler {
        private final String namespace;

        private WebSocketWatcher(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            final BuildChangeEvent buildChangeEvent = dtoFactory.createDtoFromJson(event.getMessage(), BuildChangeEvent.class);
            final Build build = buildChangeEvent.getObject();
            final BuildId buildId = BuildId.of(build);
            final Notification notification = buildsToNotifications.get(buildId);
            if (notification != null) {
                final BuildStatus.Phase phase = build.getStatus().getPhase();
                if (Failed.equals(phase)) {
                    notification.setMessage(locale.buildStatusFailed(buildId.toString()));
                    notification.setType(Type.ERROR);
                    stopWatching(buildId);
                } else if (Complete.equals(phase)) {
                    notification.setMessage(locale.buildStatusCompleted(buildId.toString()));
                    notification.setStatus(FINISHED);
                    stopWatching(buildId);
                }
            }
        }

        @Override
        public void onClose(WebSocketClosedEvent event) {
            // 1000 is code for normal closure. See http://tools.ietf.org/html/rfc6455#section-7.4.1
            if (event.getCode() != 1000) {
                namespacesToChannels.remove(namespace);

                Iterator<Map.Entry<BuildId, Notification>> iterator = buildsToNotifications.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry<BuildId, Notification> buildIdNotificationEntry = iterator.next();
                    final BuildId buildId = buildIdNotificationEntry.getKey();
                    if (buildId.getNamespace().equals(namespace)) {
                        Notification notification = buildIdNotificationEntry.getValue();
                        notification.setType(Type.ERROR);
                        notification.setMessage(locale.failedToWatchBuildByWebSocket(buildId.toString()));
                    }

                    buildsToNamespaces.remove(buildId);
                    iterator.remove();
                }
            }
        }
    }

    private static class BuildId {
        private final String namespace;
        private final String name;

        public BuildId(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BuildId)) {
                return false;
            }
            BuildId other = (BuildId)o;
            return Objects.equals(namespace, other.namespace) &&
                   Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + Objects.hashCode(namespace);
            hash = hash * 31 + Objects.hashCode(name);
            return hash;
        }

        @Override
        public String toString() {
            return namespace + "/" + name;
        }

        public static BuildId of(Build build) {
            final ObjectMeta metadata = build.getMetadata();
            return new BuildId(metadata.getNamespace(), metadata.getName());
        }
    }
}
