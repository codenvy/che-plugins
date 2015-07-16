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
package org.eclipse.che.ide.ext.java.client.projecttree.nodes;

import org.eclipse.che.api.project.shared.dto.ItemReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
public class JavaFolderNodeTest extends BaseNodeTest {
    @Mock
    private ItemReference  folderItemReference;
    private JavaFolderNode javaFolderNode;

    @Before
    public void setUp() {
        super.setUp();
        javaFolderNode = new JavaFolderNode(projectNode,
                                            folderItemReference,
                                            treeStructure,
                                            eventBus,
                                            projectServiceClient,
                                            dtoUnmarshallerFactory);
    }

    @Test
    public void shouldCreateChildSourceFolderNode() {
        ItemReference folderItem = mock(ItemReference.class);
        when(folderItem.getType()).thenReturn("folder");
        when(folderItem.getPath()).thenReturn(PROJECT_PATH + "/src/main/java");

        javaFolderNode.createChildNode(folderItem);

        verify(treeStructure).newSourceFolderNode(eq(javaFolderNode), eq(folderItem));
    }

    @Test
    public void shouldCreateChildJavaFolderNode() {
        ItemReference folderItem = mock(ItemReference.class);
        when(folderItem.getType()).thenReturn("folder");

        javaFolderNode.createChildNode(folderItem);

        verify(treeStructure).newJavaFolderNode(eq(javaFolderNode), eq(folderItem));
    }
}
