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
package org.eclipse.che.ide.extension.machine.client.watcher;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.watcher.WatcherServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.StringUnmarshallerWS;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspaceHandler;

import javax.validation.constraints.NotNull;

/**
 * The class contains business logic which allows describe on spacial socket and do some actions when
 * files are changed on file system.
 *
 * @author Dmitry Shnurenko
 */
public class SystemFileWatcher {

    static final String WATCHER_WS_CHANEL = "watcher:chanel:1";

    private final WatcherServiceClient watcherService;
    private final EventBus             eventBus;
    private final AppContext           appContext;
    private final ProjectExplorerPresenter projectExplorer;

    private MessageBus messageBus;

    @Inject
    public SystemFileWatcher(WatcherServiceClient watcherService,
                             EventBus eventBus,
                             AppContext appContext,
                             final MessageBusProvider messageBusProvider,
                             ProjectExplorerPresenter projectExplorer) {
        this.watcherService = watcherService;
        this.eventBus = eventBus;
        this.messageBus = messageBusProvider.getMessageBus();
        this.appContext = appContext;
        this.projectExplorer = projectExplorer;

        eventBus.addHandler(StartWorkspaceEvent.TYPE, new StartWorkspaceHandler() {
            @Override
            public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
                messageBus = messageBusProvider.getMessageBus();
            }
        });
    }

    /**
     * Registers special watcher which allows track changes on file system.
     *
     * @param workspaceId
     *         workspace id need to add watcher for current workspace
     */
    public void registerWatcher(@NotNull String workspaceId) {
        Promise<Void> watcherPromise = watcherService.registerRecursiveWatcher(workspaceId);

        watcherPromise.then(new Operation<Void>() {
            @Override
            public void apply(Void argument) throws OperationException {
                try {
                    messageBus.subscribe(WATCHER_WS_CHANEL, new SubscriptionHandler<String>(new StringUnmarshallerWS()) {
                        @Override
                        protected void onMessageReceived(String path) {
                            projectExplorer.reloadChildren();
                        }

                        @Override
                        protected void onErrorReceived(Throwable exception) {
                            Log.error(getClass(), exception);
                        }
                    });
                } catch (WebSocketException e) {
                    Log.error(getClass(), e);
                }
            }
        });
    }
}
