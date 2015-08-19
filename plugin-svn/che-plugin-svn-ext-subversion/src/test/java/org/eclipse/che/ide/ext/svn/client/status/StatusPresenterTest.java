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
package org.eclipse.che.ide.ext.svn.client.status;

import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.utils.TestUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.status.StatusPresenter}.
 */
public class StatusPresenterTest extends BaseSubversionPresenterTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<CLIOutputResponse>> asyncRequestCallbackStatusCaptor;

    private StatusPresenter presenter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter = new StatusPresenter(appContext, dtoUnmarshallerFactory, eventBus, notificationManager,
                                        rawOutputPresenter, service, constants, workspaceAgent, projectExplorerPart);
    }

    @Test
    public void testStatusNothingSelected() throws Exception {
        presenter.showStatus();

        verify(service).status(eq(PROJECT_PATH), anyList(), anyString(), eq(false), eq(false), eq(false), eq(true),
                               eq(false), anyList(), asyncRequestCallbackStatusCaptor.capture());
    }

    @Test
    public void testStatusProjectSelected() throws Exception {
        final Selection selection = mock(Selection.class);
        final ProjectNode project = mock(ProjectNode.class);
        final List<StorableNode> allItems = new ArrayList<>();

        allItems.add(project);

        when(projectExplorerPart.getSelection()).thenReturn(selection);
        when(selection.isEmpty()).thenReturn(false);
        when(selection.getAllElements()).thenReturn(allItems);
        when(project.getProject()).thenReturn(project);
        when(project.getPath()).thenReturn(PROJECT_PATH);

        presenter.showStatus();

        verify(service).status(eq(PROJECT_PATH), argThat(TestUtils.sameAsList(Collections.<String>emptyList())),
                               anyString(), eq(false), eq(false), eq(false), eq(true),  eq(false), anyList(),
                               asyncRequestCallbackStatusCaptor.capture());

    }

    @Test
    public void testStatusWithPathsSelected() throws Exception {
        final FileNode fileNode = mock(FileNode.class);
        final FolderNode folderNode = mock(FolderNode.class);
        final Selection selection = mock(Selection.class);
        final ProjectNode project = mock(ProjectNode.class);
        final List<StorableNode> allItems = new ArrayList<>();
        final List<String> expectedPaths = new ArrayList<>();

        allItems.add(fileNode);
        allItems.add(folderNode);

        expectedPaths.add("file");
        expectedPaths.add("folder");

        when(projectExplorerPart.getSelection()).thenReturn(selection);
        when(selection.isEmpty()).thenReturn(false);
        when(selection.getAllElements()).thenReturn(allItems);
        when(fileNode.getProject()).thenReturn(project);
        when(fileNode.getPath()).thenReturn(PROJECT_PATH + "/" + "file");
        when(folderNode.getProject()).thenReturn(project);
        when(folderNode.getPath()).thenReturn(PROJECT_PATH + "/" + "folder");
        when(project.getPath()).thenReturn(PROJECT_PATH);

        presenter.showStatus();

        verify(service).status(eq(PROJECT_PATH), argThat(TestUtils.sameAsList(expectedPaths)), anyString(), eq(false),
                               eq(false), eq(false), eq(true), eq(false), anyList(),
                               asyncRequestCallbackStatusCaptor.capture());
    }

}
