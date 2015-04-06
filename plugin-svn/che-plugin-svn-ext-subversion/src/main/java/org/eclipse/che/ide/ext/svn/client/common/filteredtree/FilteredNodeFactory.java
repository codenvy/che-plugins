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
package org.eclipse.che.ide.ext.svn.client.common.filteredtree;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.NodeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory that helps to create nodes for {@link FilteredTreeStructure}.
 *
 * @author Vladyslav Zhukovskyi
 */
public interface FilteredNodeFactory extends NodeFactory {

    /**
     * Creates a new {@link FilteredProjectNode} owned by the specified
     * {@code treeStructure}
     * with the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link ProjectDescriptor}
     * @param treeStructure
     *         the {@link FilteredTreeStructure} to create the node for
     * @return a new {@link FilteredProjectNode}
     */
    FilteredProjectNode newFilteredProjectNode(@Nullable TreeNode<?> parent,
                                               @Nonnull ProjectDescriptor data,
                                               @Nonnull FilteredTreeStructure treeStructure);
}
