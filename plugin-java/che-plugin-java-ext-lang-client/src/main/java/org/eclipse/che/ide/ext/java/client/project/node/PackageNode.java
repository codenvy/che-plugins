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
package org.eclipse.che.ide.ext.java.client.project.node;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettings;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.resource.ItemReferenceProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class PackageNode extends FolderReferenceNode {

    private final JavaNodeManager nodeManager;

    @Inject
    public PackageNode(@Assisted ItemReference itemReference,
                       @Assisted ProjectDescriptor projectDescriptor,
                       @Assisted JavaNodeSettings nodeSettings,
                       @NotNull EventBus eventBus,
                       @NotNull JavaNodeManager nodeManager,
                       @NotNull ItemReferenceProcessor resourceProcessor) {
        super(itemReference, projectDescriptor, nodeSettings, eventBus, nodeManager, resourceProcessor);
        this.nodeManager = nodeManager;
    }

    @NotNull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return nodeManager.getChildren(getData(),
                                       getProjectDescriptor(),
                                       getSettings());
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        presentation.setPresentableText(getDisplayFqn());
        presentation.setPresentableIcon(nodeManager.getJavaNodesResources().packageFolder());
    }

    @Override
    public String getName() {
        return getDisplayFqn();
    }

    public String getDisplayFqn() {
        Node parent = getParent();

        if (parent != null && parent instanceof HasStorablePath) {
            String parentPath = ((HasStorablePath)parent).getStorablePath();
            String pkgPath = getStorablePath();

            String fqnPath = pkgPath.replaceFirst(parentPath, "");
            if (fqnPath.startsWith("/")) {
                fqnPath = fqnPath.substring(1);
            }

            fqnPath = fqnPath.replaceAll("/", ".");
            return fqnPath;
        }

        return getData().getPath();
    }

    public String getQualifiedName() {
        String fqn = "";

        Node parent = getParent();

        while (parent != null) {
            if (parent instanceof SourceFolderNode) {
                String parentStorablePath = ((FolderReferenceNode)parent).getStorablePath();
                String currentStorablePath = getStorablePath();

                fqn = currentStorablePath.substring(parentStorablePath.length() + 1).replace('/', '.');
                break;
            }

            parent = parent.getParent();
        }

        return fqn;
    }

    @Override
    public RenameProcessor<ItemReference> getRenameProcessor() {
        return null;
    }
}
