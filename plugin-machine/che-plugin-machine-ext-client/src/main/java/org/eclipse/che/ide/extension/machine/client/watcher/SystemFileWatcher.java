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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.watcher.WatcherServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.StringUnmarshallerWS;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

import javax.annotation.Nonnull;

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
    private final MessageBus           messageBus;
    private final AppContext           appContext;

    @Inject
    public SystemFileWatcher(WatcherServiceClient watcherService, EventBus eventBus, MessageBus messageBus, AppContext appContext) {
        this.watcherService = watcherService;
        this.eventBus = eventBus;
        this.messageBus = messageBus;
        this.appContext = appContext;
    }

    /**
     * Registers special watcher which allows track changes on file system.
     *
     * @param workspaceId
     *         workspace id need to add watcher for current workspace
     */
    public void registerWatcher(@Nonnull String workspaceId) {
        Promise<Void> watcherPromise = watcherService.registerRecursiveWatcher(workspaceId);

        watcherPromise.then(new Operation<Void>() {
            @Override
            public void apply(Void argument) throws OperationException {
                try {
                    messageBus.subscribe(WATCHER_WS_CHANEL, new SubscriptionHandler<String>(new StringUnmarshallerWS()) {
                        @Override
                        protected void onMessageReceived(String path) {
                            refreshTree(path);
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

    private void refreshTree(@Nonnull String path) {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return;
        }

        String pathToNode = getPathToParent(path);

        boolean isRootNode = !pathToNode.contains("/");

        TreeStructure treeStructure = currentProject.getCurrentTree();

        if (isRootNode) {
            refreshRootNode(treeStructure);
        } else {
            refreshChildNode(treeStructure, pathToNode);
        }
    }

    @Nonnull
    private String getPathToParent(@Nonnull String path) {
        int parentPathEnd = path.lastIndexOf("/");

        return parentPathEnd == -1 ? path : path.substring(0, parentPathEnd);
    }

    private void refreshRootNode(@Nonnull TreeStructure treeStructure) {
        treeStructure.getRootNodes(new AsyncCallback<Array<TreeNode<?>>>() {
            @Override
            public void onSuccess(Array<TreeNode<?>> result) {
                TreeNode<?> rootNode = result.get(0);

                eventBus.fireEvent(new RefreshProjectTreeEvent(rootNode));
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(getClass(), caught);
            }
        });
    }

    private void refreshChildNode(@Nonnull TreeStructure treeStructure, @Nonnull String pathToChild) {
        treeStructure.getNodeByPath(pathToChild, new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                eventBus.fireEvent(new RefreshProjectTreeEvent(result));
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(getClass(), caught);
            }
        });
    }
}
