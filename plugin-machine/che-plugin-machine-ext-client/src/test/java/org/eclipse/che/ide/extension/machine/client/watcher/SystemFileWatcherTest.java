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
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.watcher.WatcherServiceClient;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

import static org.eclipse.che.ide.extension.machine.client.watcher.SystemFileWatcher.WATCHER_WS_CHANEL;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class SystemFileWatcherTest {

    private static final String SOME_TEXT = "someText";

    private static final String PATH_TO_FILE   = "parent/child/changed_file";
    private static final String PATH_TO_PARENT = "parent/child";

    //constructor mocks
    @Mock
    private WatcherServiceClient watcherService;
    @Mock
    private EventBus             eventBus;
    @Mock
    private MessageBus           messageBus;
    @Mock
    private AppContext           appContext;

    //additional mocks
    @Mock
    private Promise<Void>  registerPromise;
    @Mock
    private CurrentProject currentProject;
    @Mock
    private TreeStructure  treeStructure;
    @Mock
    private TreeNode<?>    treeNode;

    @Captor
    private ArgumentCaptor<Operation<Void>>                   operationCaptor;
    @Captor
    private ArgumentCaptor<SubscriptionHandler<String>>       subscriptionCaptor;
    @Captor
    private ArgumentCaptor<AsyncCallback<TreeNode<?>>>        nodeCaptor;
    @Captor
    private ArgumentCaptor<AsyncCallback<Array<TreeNode<?>>>> rootNodeCaptor;

    @InjectMocks
    private SystemFileWatcher systemFileWatcher;

    @Test
    public void watcherShouldBeRegistered() throws Exception {
        callRefreshTreeMethod(PATH_TO_FILE);

        verify(appContext).getCurrentProject();
        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(PATH_TO_PARENT), nodeCaptor.capture());
        nodeCaptor.getValue().onSuccess(treeNode);

        verify(eventBus).fireEvent(Matchers.<RefreshProjectTreeEvent>anyObject());
    }

    private void callRefreshTreeMethod(@Nonnull String pathToNode) throws Exception {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getCurrentTree()).thenReturn(treeStructure);

        when(watcherService.registerRecursiveWatcher(SOME_TEXT)).thenReturn(registerPromise);

        systemFileWatcher.registerWatcher(SOME_TEXT);

        verify(watcherService).registerRecursiveWatcher(SOME_TEXT);

        verify(registerPromise).then(operationCaptor.capture());
        operationCaptor.getValue().apply(null);

        verify(messageBus).subscribe(eq(WATCHER_WS_CHANEL), subscriptionCaptor.capture());
        SubscriptionHandler<String> handler = subscriptionCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method method = handler.getClass().getDeclaredMethod("onMessageReceived", Object.class);

        method.setAccessible(true);

        method.invoke(handler, pathToNode);
    }

    @Test
    public void parentNodeShouldBeRefreshed() throws Exception {
        Array<TreeNode<?>> nodes = Collections.<TreeNode<?>>createArray(treeNode);

        callRefreshTreeMethod(PATH_TO_PARENT);

        verify(treeStructure).getRootNodes(rootNodeCaptor.capture());
        rootNodeCaptor.getValue().onSuccess(nodes);

        verify(eventBus).fireEvent(Matchers.<RefreshProjectTreeEvent>anyObject());
    }
}