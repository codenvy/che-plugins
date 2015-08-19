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

import org.eclipse.che.ide.api.project.tree.TreeNode;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
public class JavaProjectNodeTest extends BaseNodeTest {
    private JavaProjectNode javaProjectNode;

    @Before
    public void setUp() {
        super.setUp();
        javaProjectNode = new JavaProjectNode(null,
                                              projectDescriptor,
                                              treeStructure,
                                              eventBus,
                                              projectServiceClient,
                                              dtoUnmarshallerFactory);
    }

    @Test
    public void shouldAddExternalLibrariesNode() {
        when(javaTreeSettings.isShowExternalLibraries()).thenReturn(Boolean.TRUE);
        ExternalLibrariesNode externalLibrariesNode = mock(ExternalLibrariesNode.class);
        when(treeStructure.newExternalLibrariesNode(any(JavaProjectNode.class))).thenReturn(externalLibrariesNode);

        List<TreeNode<?>> children = new ArrayList<>();
        javaProjectNode.setChildren(children);

        verify(treeStructure).newExternalLibrariesNode(any(JavaProjectNode.class));
        assertTrue(children.contains(externalLibrariesNode));
    }

    @Test
    public void shouldNotAddExternalLibrariesNode() {
        when(javaTreeSettings.isShowExternalLibraries()).thenReturn(Boolean.FALSE);

        javaProjectNode.setChildren(Collections.<TreeNode<?>>emptyList());

        verify(treeStructure, never()).newExternalLibrariesNode(any(JavaProjectNode.class));
    }
}
