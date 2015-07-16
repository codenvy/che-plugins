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
package org.eclipse.che.ide.ext.java.client.projecttree;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.ExternalLibrariesNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.JavaProjectNode;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class JavaTreeStructureTest {
    @Mock
    private JavaNodeFactory        nodeFactory;
    @Mock
    private EventBus               eventBus;
    @Mock
    private AppContext             appContext;
    @Mock
    private ProjectServiceClient   projectServiceClient;
    @Mock
    private IconRegistry           iconRegistry;
    @Mock
    private JavaNavigationService  javaNavigationService;
    @Mock
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;
    @InjectMocks
    private JavaTreeStructure      treeStructure;

    @Test
    public void testGetNodeFactory() throws Exception {
        assertEquals(nodeFactory, treeStructure.getNodeFactory());
    }

    @Test
    public void testNewJavaFolderNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        ItemReference data = mock(ItemReference.class);
        when(data.getType()).thenReturn("folder");

        treeStructure.newJavaFolderNode(parent, data);

        verify(nodeFactory).newJavaFolderNode(eq(parent), eq(data), eq(treeStructure));
    }

    @Test
    public void testNewSourceFolderNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        ItemReference data = mock(ItemReference.class);
        when(data.getType()).thenReturn("folder");

        treeStructure.newSourceFolderNode(parent, data);

        verify(nodeFactory).newSourceFolderNode(eq(parent), eq(data), eq(treeStructure));
    }

    @Test
    public void testNewPackageNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        ItemReference data = mock(ItemReference.class);
        when(data.getType()).thenReturn("folder");

        treeStructure.newPackageNode(parent, data);

        verify(nodeFactory).newPackageNode(eq(parent), eq(data), eq(treeStructure));
    }

    @Test
    public void testNewSourceFileNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        ItemReference data = mock(ItemReference.class);
        when(data.getType()).thenReturn("file");

        treeStructure.newSourceFileNode(parent, data);

        verify(nodeFactory).newSourceFileNode(eq(parent), eq(data), eq(treeStructure));
    }

    @Test
    public void testNewExternalLibrariesNode() throws Exception {
        JavaProjectNode parent = mock(JavaProjectNode.class);
        ProjectDescriptor parentData = mock(ProjectDescriptor.class);
        when(parentData.getPath()).thenReturn("/project");
        when(parent.getData()).thenReturn(parentData);

        treeStructure.newExternalLibrariesNode(parent);

        verify(nodeFactory).newExternalLibrariesNode(eq(parent), anyObject(), eq(treeStructure));
    }

    @Test
    public void testNewJarNode() throws Exception {
        ExternalLibrariesNode parent = mock(ExternalLibrariesNode.class);
        Jar data = mock(Jar.class);

        treeStructure.newJarNode(parent, data);

        verify(nodeFactory).newJarNode(eq(parent), eq(data), eq(treeStructure));
    }

    @Test
    public void testNewJarContainerNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        JarEntry data = mock(JarEntry.class);

        treeStructure.newJarContainerNode(parent, data, 1);

        verify(nodeFactory).newJarContainerNode(eq(parent), eq(data), eq(treeStructure), anyInt());
    }

    @Test
    public void testNewJarFileNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        JarEntry data = mock(JarEntry.class);

        treeStructure.newJarFileNode(parent, data, 1);

        verify(nodeFactory).newJarFileNode(eq(parent), eq(data), eq(treeStructure), anyInt());
    }

    @Test
    public void testNewJarClassNode() throws Exception {
        AbstractTreeNode parent = mock(AbstractTreeNode.class);
        JarEntry data = mock(JarEntry.class);

        treeStructure.newJarClassNode(parent, data, 1);

        verify(nodeFactory).newJarClassNode(eq(parent), eq(data), eq(treeStructure), anyInt());
    }
}
