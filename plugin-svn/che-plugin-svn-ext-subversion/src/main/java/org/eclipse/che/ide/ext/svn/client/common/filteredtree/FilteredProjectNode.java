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

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Node that represents a filtered project.
 *
 * @author Vladyslav Zhukovskyi
 */
public class FilteredProjectNode extends ProjectNode {

    @AssistedInject
    public FilteredProjectNode(@Assisted TreeNode<?> parent,
                               @Assisted ProjectDescriptor data,
                               @Assisted FilteredTreeStructure treeStructure,
                               EventBus eventBus,
                               ProjectServiceClient projectServiceClient,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public FilteredTreeStructure getTreeStructure() {
        return (FilteredTreeStructure)super.getTreeStructure();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    protected AbstractTreeNode<?> createChildNode(ItemReference item) {
        if ("file".equals(item.getType())) {
            return getTreeStructure().newFileNode(FilteredProjectNode.this, item);
        } else if ("folder".equals(item.getType()) || "project".equals(item.getType())) {
            return getTreeStructure().newFolderNode(FilteredProjectNode.this, item);
        }
        return null;
    }
}
