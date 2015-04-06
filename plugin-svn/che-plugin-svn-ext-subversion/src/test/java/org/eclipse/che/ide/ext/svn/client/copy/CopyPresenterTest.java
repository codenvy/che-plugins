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
package org.eclipse.che.ide.ext.svn.client.copy;

import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.ext.svn.client.common.filteredtree.FilteredTreeStructure;
import org.eclipse.che.ide.ext.svn.client.common.filteredtree.FilteredTreeStructureProvider;
import org.eclipse.che.test.GwtReflectionUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.copy.CopyPresenter}.
 *
 * @author Vladyslav Zhukovskyi
 */
public class CopyPresenterTest extends BaseSubversionPresenterTest {
    @Captor
    private ArgumentCaptor<AsyncCallback<Array<TreeNode<?>>>> asyncRequestCallbackStatusCaptor;

    private CopyPresenter presenter;

    @Mock
    CopyView copyView;

    @Mock
    FilteredTreeStructureProvider treeStructureProvider;

    @Mock
    FilteredTreeStructure filteredTreeStructure;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter =
                new CopyPresenter(appContext, eventBus, rawOutputPresenter, workspaceAgent, copyView, notificationManager,
                                  service, dtoUnmarshallerFactory, treeStructureProvider, constants, projectExplorerPart);
    }

    @Test
    public void testCopyViewShouldBeShowed() throws Exception {
        when(treeStructureProvider.get()).thenReturn(filteredTreeStructure);

        presenter.showCopy(mock(FileNode.class));

        verify(copyView).onShow();
    }

    @Test
    public void testCopyViewShouldSetProjectNode() throws Exception {
        when(treeStructureProvider.get()).thenReturn(filteredTreeStructure);

        presenter.showCopy(mock(FileNode.class));

        Array<TreeNode<?>> children = Collections.createArray();
        children.add(mock(ProjectNode.class));

        verify(filteredTreeStructure).getRootNodes(asyncRequestCallbackStatusCaptor.capture());
        AsyncCallback<Array<TreeNode<?>>> requestCallback = asyncRequestCallbackStatusCaptor.getValue();
        GwtReflectionUtils.callPrivateMethod(requestCallback, "onSuccess", children);

        verify(copyView).setProjectNodes(eq(children));
    }

    @Test
    public void testEmptyTargetMessageAlertShouldBeAppear() throws Exception {
        when(constants.copyEmptyTarget()).thenReturn("message");

        presenter.onNewNameChanged("/foo");

        verify(copyView).showErrorMarker("message");
    }

    @Test
    public void testEmptyItemEqualMessageAlertShouldBeAppear() throws Exception {
        FileNode node = mock(FileNode.class);
        FileNode checkNode = mock(FileNode.class);

        when(treeStructureProvider.get()).thenReturn(filteredTreeStructure);
        when(constants.copyItemEqual()).thenReturn("message");
        when(node.getPath()).thenReturn("/foo");
        when(checkNode.getPath()).thenReturn("/");

        presenter.showCopy(node);
        presenter.onNewNameChanged("foo");
        presenter.onNodeSelected(checkNode);


        verify(copyView, atMost(2)).showErrorMarker("message");
    }
}
