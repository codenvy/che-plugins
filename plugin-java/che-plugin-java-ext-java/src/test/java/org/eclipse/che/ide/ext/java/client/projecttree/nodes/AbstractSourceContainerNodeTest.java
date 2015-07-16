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

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
public class AbstractSourceContainerNodeTest extends BaseNodeTest {
    @Mock
    private ItemReference               folderItemReference;
    private AbstractSourceContainerNode sourceContainerNode;

    @Before
    public void setUp() {
        super.setUp();
        sourceContainerNode = new DummySourceContainerNode(null,
                                                           folderItemReference,
                                                           treeStructure,
                                                           eventBus,
                                                           projectServiceClient,
                                                           dtoUnmarshallerFactory);
    }

    @Test
    public void testCanContainsFolder() throws Exception {
        assertFalse(sourceContainerNode.canContainsFolder());
    }

    @Test
    public void shouldCreateChildSourceFileNode() {
        ItemReference javaFileItem = mock(ItemReference.class);
        when(javaFileItem.getName()).thenReturn("Test.java");
        when(javaFileItem.getType()).thenReturn("file");

        sourceContainerNode.createChildNode(javaFileItem);

        verify(treeStructure).newSourceFileNode(eq(sourceContainerNode), eq(javaFileItem));
    }

    @Test
    public void shouldCreateChildPackageNode() {
        ItemReference packageItem = mock(ItemReference.class);
        when(packageItem.getType()).thenReturn("folder");

        sourceContainerNode.createChildNode(packageItem);

        verify(treeStructure).newPackageNode(eq(sourceContainerNode), eq(packageItem));
    }

    private class DummySourceContainerNode extends AbstractSourceContainerNode {
        public DummySourceContainerNode(TreeNode<?> parent,
                                        ItemReference data,
                                        JavaTreeStructure treeStructure,
                                        EventBus eventBus,
                                        ProjectServiceClient projectServiceClient,
                                        DtoUnmarshallerFactory dtoUnmarshallerFactory) {
            super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        }
    }
}
