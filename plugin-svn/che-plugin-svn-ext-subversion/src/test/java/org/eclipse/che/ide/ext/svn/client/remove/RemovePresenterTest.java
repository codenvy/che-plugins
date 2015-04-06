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
package org.eclipse.che.ide.ext.svn.client.remove;

import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.utils.TestUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.add.AddPresenter}.
 */
public class RemovePresenterTest extends BaseSubversionPresenterTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<CLIOutputResponse>> asyncRequestCallbackStatusCaptor;

    private RemovePresenter presenter;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter = new RemovePresenter(appContext, dtoUnmarshallerFactory, eventBus, notificationManager,
                                        rawOutputPresenter, constants, service, workspaceAgent, projectExplorerPart);
    }

    @Test
    public void testAddNothingSelected() throws Exception {
        // We cannot test this since the SelectionAgent has a bug where something always appears selected
    }

    @Test
    public void testAddWithPathsSelected() throws Exception {
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

        presenter.showRemove();

        verify(service).remove(eq(PROJECT_PATH), argThat(TestUtils.sameAsList(expectedPaths)),
                               asyncRequestCallbackStatusCaptor.capture());
    }

}