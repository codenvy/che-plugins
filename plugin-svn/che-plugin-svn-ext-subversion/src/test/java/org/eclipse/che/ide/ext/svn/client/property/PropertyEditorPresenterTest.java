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
package org.eclipse.che.ide.ext.svn.client.property;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.ext.svn.client.common.BaseSubversionPresenterTest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.shared.Depth;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.eclipse.che.ide.ext.svn.client.property.PropertyEditorPresenter}.
 *
 * @author Vladyslav Zhukovskyi
 */
public class PropertyEditorPresenterTest extends BaseSubversionPresenterTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<CLIOutputResponse>> asyncRequestCallbackStatusCaptor;

    private PropertyEditorPresenter presenter;

    @Mock
    PropertyEditorView view;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        presenter =
                new PropertyEditorPresenter(appContext, eventBus, rawOutputPresenter, workspaceAgent, projectExplorerPart, view, service,
                                            dtoUnmarshallerFactory, notificationManager, constants);
    }

    @Test
    public void testViewShouldBeShowed() throws Exception {
        presenter.showEditor();

        verify(view).onShow();
    }

    @Test
    public void testEditRequestShouldBeFired() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        ProjectDescriptor descriptor = mock(ProjectDescriptor.class);
        Selection selection = mock(Selection.class);

        FolderNode folderNode = mock(FolderNode.class);
        List<FolderNode> nodes = Collections.singletonList(folderNode);

        ProjectNode projectNode = mock(ProjectNode.class);

        when(view.isEditPropertySelected()).thenReturn(true);
        when(view.getDepth()).thenReturn(Depth.FULLY_RECURSIVE);
        when(view.getSelectedProperty()).thenReturn("propName");
        when(view.getPropertyValue()).thenReturn("propValue");
        when(view.isForceSelected()).thenReturn(true);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(descriptor.getPath()).thenReturn("/foo");
        when(projectExplorerPart.getSelection()).thenReturn(selection);
        when(selection.getAllElements()).thenReturn(nodes);
        when(folderNode.getPath()).thenReturn("/foo");
        when(projectNode.getPath()).thenReturn("/");
        when(folderNode.getProject()).thenReturn(projectNode);

        presenter.onOkClicked();

        verify(view).onClose();

        verify(service).propertySet(eq("/foo"), eq("propName"), eq("propValue"), eq(Depth.FULLY_RECURSIVE), eq(true), eq("foo"),
                                    asyncRequestCallbackStatusCaptor.capture());
    }

    @Test
    public void testDeleteRequestShouldBeFired() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        ProjectDescriptor descriptor = mock(ProjectDescriptor.class);
        Selection selection = mock(Selection.class);

        FolderNode folderNode = mock(FolderNode.class);
        List<FolderNode> nodes = Collections.singletonList(folderNode);

        ProjectNode projectNode = mock(ProjectNode.class);

        when(view.isDeletePropertySelected()).thenReturn(true);
        when(view.getDepth()).thenReturn(Depth.FULLY_RECURSIVE);
        when(view.getSelectedProperty()).thenReturn("propName");
        when(view.getPropertyValue()).thenReturn("propValue");
        when(view.isForceSelected()).thenReturn(true);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(descriptor.getPath()).thenReturn("/foo");
        when(projectExplorerPart.getSelection()).thenReturn(selection);
        when(selection.getAllElements()).thenReturn(nodes);
        when(folderNode.getPath()).thenReturn("/foo");
        when(projectNode.getPath()).thenReturn("/");
        when(folderNode.getProject()).thenReturn(projectNode);

        presenter.onOkClicked();

        verify(view).onClose();

        verify(service).propertyDelete(eq("/foo"), eq("propName"), eq(Depth.FULLY_RECURSIVE), eq(true), eq("foo"),
                                    asyncRequestCallbackStatusCaptor.capture());
    }
}
