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
package org.eclipse.che.ide.extension.maven.client.projecttree;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gwt.test.Mock;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.UpdateTreeNodeChildrenEvent;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.vectomatic.dom.svg.ui.SVGImage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ModuleNode} functionality.
 *
 * @author Alexander Andrienko
 */
public class ModuleNodeTest extends BaseNodeTest {
    private static final String PARENT_PATH    = "/project/folder";
    private static final String NAME           = "name";
    private static final String PATH           = PARENT_PATH + "/" + NAME;
    private static final String NEW_NAME       = "newName";
    private static final String NEW_PATH       = PARENT_PATH + "/" + NEW_NAME;
    private static final String PARENT_RENAMED = "/project/folder1";

    @Mock
    private AppContext          appContext;
    @Mock
    private Throwable           throwable;
    @Mock
    private AsyncCallback<Void> asyncCallback;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>              asyncRequestCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectDescriptor>> asyncRequestCallbackArgumentCaptor;
    @Captor
    private ArgumentCaptor<UpdateTreeNodeChildrenEvent>             argumentCaptor;

    private ModuleNode moduleNode;

    @Before
    public void setUp() {
        Icon icon = mock(Icon.class);
        SVGImage svgImage = mock(SVGImage.class);
        when(icon.getSVGImage()).thenReturn(svgImage);
        when(iconRegistry.getIcon(anyString())).thenReturn(icon);

        moduleNode = new ModuleNode(projectNode,
                                    projectDescriptor,
                                    treeStructure,
                                    eventBus,
                                    projectServiceClient,
                                    dtoUnmarshallerFactory,
                                    iconRegistry,
                                    appContext);

        moduleNode.setData(projectDescriptor);
        when(projectDescriptor.getName()).thenReturn(NAME);
        when(projectDescriptor.getPath()).thenReturn(PATH);
        when(projectNode.getPath()).thenReturn(PARENT_PATH);
    }

    @Test
    public void moduleShouldBeRenamed() {
        TreeNode.RenameCallback renameCallback = mock(TreeNode.RenameCallback.class);
        ProjectDescriptor result = mock(ProjectDescriptor.class);

        moduleNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), asyncRequestCallbackArgCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgCaptor.getValue(), (Void)null);

        verify(projectServiceClient).getProject(eq(NEW_PATH), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), result);

        assertThat(moduleNode.getData(), is(result));

        verify(eventBus).fireEvent(any(UpdateTreeNodeChildrenEvent.class));
    }

    @Test
    public void moduleShouldNotBeRenamed() {
        TreeNode.RenameCallback renameCallback = mock(TreeNode.RenameCallback.class);

        moduleNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), asyncRequestCallbackArgCaptor.capture());
        GwtReflectionUtils.callOnFailure(asyncRequestCallbackArgCaptor.getValue(), throwable);

        renameCallback.onFailure(throwable);
    }

    @Test
    public void moduleShouldNotBeRenamedBecauseFailedGetProjectDescriptor() {
        TreeNode.RenameCallback renameCallback = mock(TreeNode.RenameCallback.class);

        moduleNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), asyncRequestCallbackArgCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgCaptor.getValue(), (Void)null);

        verify(projectServiceClient).getProject(eq(NEW_PATH), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(asyncRequestCallbackArgumentCaptor.getValue(), throwable);

        renameCallback.onFailure(throwable);
    }

    @Test
    public void projectDescriptorShouldBeUpdated() {
        when(projectNode.getPath()).thenReturn(PARENT_RENAMED);

        moduleNode.updateData(asyncCallback, NEW_PATH);

        ProjectDescriptor result = mock(ProjectDescriptor.class);
        verify(projectServiceClient).getProject(eq(PARENT_RENAMED + "/" + NAME), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), result);

        assertThat(moduleNode.getData(), is(result));
        asyncCallback.onSuccess(null);
    }

    @Test
    public void projectDescriptorShouldNotBeUpdated() {
        when(projectNode.getPath()).thenReturn(PARENT_RENAMED);

        moduleNode.updateData(asyncCallback, NEW_PATH);

        verify(projectServiceClient).getProject(eq(PARENT_RENAMED + "/" + NAME), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(asyncRequestCallbackArgumentCaptor.getValue(), throwable);

        verify(asyncCallback).onFailure(throwable);
    }
}
