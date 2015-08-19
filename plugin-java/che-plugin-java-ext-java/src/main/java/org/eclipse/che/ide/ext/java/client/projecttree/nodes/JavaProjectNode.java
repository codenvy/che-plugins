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
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaTreeStructure;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Node that represents Java project.
 *
 * @author Artem Zatsarynnyy
 * @author Vladyslav Zhukovskii
 */
public class JavaProjectNode extends ProjectNode {

    protected boolean shouldAddExternalLibrariesNode = true;

    @Inject
    public JavaProjectNode(@Assisted TreeNode<?> parent,
                           @Assisted ProjectDescriptor data,
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

    @Override
    public void setChildren(List<TreeNode<?>> children) {
        if (getTreeStructure().getSettings().isShowExternalLibraries() && shouldAddExternalLibrariesNode) {
            ExternalLibrariesNode librariesNode = getTreeStructure().newExternalLibrariesNode(this);
            children.add(librariesNode);
        }
        super.setChildren(children);
    }
}
