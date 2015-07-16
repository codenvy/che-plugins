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
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * {@link FolderNode} that may contains {@link SourceFolderNode}s.
 *
 * @author Artem Zatsarynnyy
 * @author Vladyslav Zhukovskii
 */
public class JavaFolderNode extends FolderNode {

    @Inject
    public JavaFolderNode(@Assisted TreeNode<?> parent,
                          @Assisted ItemReference data,
                          @Assisted JavaTreeStructure treeStructure,
                          EventBus eventBus,
                          ProjectServiceClient projectServiceClient,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
    }

    @Nonnull
    @Override
    public JavaTreeStructure getTreeStructure() {
        return (JavaTreeStructure)super.getTreeStructure();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    protected AbstractTreeNode<?> createChildNode(ItemReference item) {
        if (JavaSourceFolderUtil.isSourceFolder(item, getProject())) {
            return getTreeStructure().newSourceFolderNode(this, item);
        } else if ("folder".equals(item.getType())) {
            return getTreeStructure().newJavaFolderNode(this, item);
        } else {
            return super.createChildNode(item);
        }
    }
}
