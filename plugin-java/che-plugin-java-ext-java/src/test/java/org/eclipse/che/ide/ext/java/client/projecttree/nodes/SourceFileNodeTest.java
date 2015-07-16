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

import org.eclipse.che.ide.api.editor.EditorAgent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
public class SourceFileNodeTest extends BaseNodeTest {
    private static final String FILE_ITEM_NAME = "Test.java";
    @Mock
    private ItemReference  fileItemReference;
    @Mock
    private EditorAgent    editorAgent;
    private SourceFileNode sourceFileNode;

    @Before
    public void setUp() {
        super.setUp();
        when(fileItemReference.getName()).thenReturn(FILE_ITEM_NAME);
        sourceFileNode = new SourceFileNode(null,
                                            fileItemReference,
                                            treeStructure,
                                            eventBus,
                                            projectServiceClient,
                                            dtoUnmarshallerFactory,
                                            editorAgent);
    }

    @Test
    public void testGetDisplayName() throws Exception {
        assertEquals("Test", sourceFileNode.getDisplayName());
    }

    @Test
    public void testIsRenamable() throws Exception {
        assertFalse(sourceFileNode.isRenamable());
    }
}
