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

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link MavenFolderNode} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class MavenFolderNodeTest extends BaseNodeTest {
    private static final String FOLDER_PATH = "/project/folder";
    @Mock
    private ItemReference     itemReference;
    @Mock
    private ProjectDescriptor projectDescriptor;
    @InjectMocks
    private MavenFolderNode   mavenFolderNode;

    @Before
    public void setUp() {
        super.setUp();
        when(mavenFolderNode.getPath()).thenReturn(FOLDER_PATH);
    }

    @Test
    public void shouldCreateChildModuleNode() {
        ItemReference projectItem = mock(ItemReference.class);
        when(projectItem.getType()).thenReturn("project");
        when(projectItem.getName()).thenReturn("my_module");

        ProjectDescriptor moduleDescriptor = mock(ProjectDescriptor.class);
        when(moduleDescriptor.getName()).thenReturn("my_module");
        List<ProjectDescriptor> modules = Arrays.asList(moduleDescriptor);

        mavenFolderNode.createChildNode(projectItem, modules);

        verify(treeStructure).newModuleNode(eq(mavenFolderNode), eq(moduleDescriptor));
    }

    @Test
    public void shouldCreateChildSourceFolderNode() {
        ItemReference sourceFolderItem = mock(ItemReference.class);
        when(sourceFolderItem.getType()).thenReturn("folder");
        when(sourceFolderItem.getPath()).thenReturn("/project/src/main/java");
        List<ProjectDescriptor> modules = new ArrayList<>();

        mavenFolderNode.createChildNode(sourceFolderItem, modules);

        verify(treeStructure).newSourceFolderNode(eq(mavenFolderNode), eq(sourceFolderItem));
    }

    @Test
    public void shouldCreateChildJavaFolderNode() {
        ItemReference folderItem = mock(ItemReference.class);
        when(folderItem.getType()).thenReturn("folder");
        List<ProjectDescriptor> modules = new ArrayList<>();

        mavenFolderNode.createChildNode(folderItem, modules);

        verify(treeStructure).newJavaFolderNode(eq(mavenFolderNode), eq(folderItem));
    }
}
