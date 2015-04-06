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
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.GenericTreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.NodeFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

/**
 * Builds a currently opened project's tree structure that reflects the project's physical structure which shows filtered content.
 *
 * @author Vladyslav Zhukovskyi
 */
public class FilteredTreeStructure extends GenericTreeStructure {

    public FilteredTreeStructure(NodeFactory nodeFactory, EventBus eventBus, AppContext appContext,
                                 ProjectServiceClient projectServiceClient, DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(nodeFactory, eventBus, appContext, projectServiceClient, dtoUnmarshallerFactory);
    }

    /** {@inheritDoc} */
    @Override
    public FilteredNodeFactory getNodeFactory() {
        return (FilteredNodeFactory)super.getNodeFactory();
    }

    /** {@inheritDoc} */
    @Override
    public FileNode newFileNode(@Nonnull TreeNode parent, @Nonnull ItemReference data) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public FilteredProjectNode newProjectNode(@Nonnull ProjectDescriptor data) {
        return getNodeFactory().newFilteredProjectNode(null, data, this);
    }

    /** {@inheritDoc} */
    @Override
    public FolderNode newFolderNode(@Nonnull TreeNode parent, @Nonnull ItemReference data) {
        return getNodeFactory().newFolderNode(parent, data, this);
    }
}
