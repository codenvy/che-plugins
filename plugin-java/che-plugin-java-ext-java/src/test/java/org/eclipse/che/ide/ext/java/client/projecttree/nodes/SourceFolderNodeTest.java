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

import static org.junit.Assert.assertFalse;

/** @author Artem Zatsarynnyy */
public class SourceFolderNodeTest extends BaseNodeTest {
    @Mock
    private ItemReference    folderItemReference;
    private SourceFolderNode sourceFolderNode;

    @Before
    public void setUp() {
        super.setUp();
        sourceFolderNode = new SourceFolderNode(null,
                                                folderItemReference,
                                                treeStructure,
                                                eventBus,
                                                projectServiceClient,
                                                dtoUnmarshallerFactory,
                                                iconRegistry);
    }

    @Test
    public void testIsRenamable() throws Exception {
        assertFalse(sourceFolderNode.isRenamable());
    }
}
