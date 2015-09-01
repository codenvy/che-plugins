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
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettings;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ItemReferenceChainFilter;
import org.eclipse.che.ide.project.node.resource.ItemReferenceProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
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
                       @Nonnull EventBus eventBus,
                       @Nonnull JavaNodeManager nodeManager,
                       @Nonnull ItemReferenceProcessor resourceProcessor) {
        super(itemReference, projectDescriptor, nodeSettings, eventBus, nodeManager, resourceProcessor);
        this.nodeManager = nodeManager;
    }

    @Nonnull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return nodeManager.getChildren(getData(),
                                       getProjectDescriptor(),
                                       getSettings(),
                                       emptyMiddlePackageFilter());
    }

    private ItemReferenceChainFilter emptyMiddlePackageFilter() {
        return new ItemReferenceChainFilter() {
            @Override
            public Promise<List<ItemReference>> process(List<ItemReference> referenceList) {

                if (referenceList.isEmpty() || referenceList.size() > 1) {
                    //if children in directory more than one

                    final List<ItemReference> files = new ArrayList<>();
                    List<ItemReference> otherNodes = new ArrayList<>();
                    //filter folders to proceed deep child
                    for (ItemReference itemReference : referenceList) {
                        if ("file".equals(itemReference.getType())) {
                            files.add(itemReference);
                        } else {
                            otherNodes.add(itemReference);
                        }
                    }

                    if (!otherNodes.isEmpty()) {
                        if (otherNodes.size() == 1) {
                            return foundFirstNonEmpty(otherNodes.get(0)).thenPromise(new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
                                @Override
                                public Promise<List<ItemReference>> apply(List<ItemReference> arg) throws FunctionException {
                                    arg.addAll(files);
                                    return Promises.resolve(arg);
                                }
                            });
                        }
                    }

                    return Promises.resolve(referenceList);
                }

                //else we have one child. check if it file

                if ("file".equals(referenceList.get(0).getType())) {
                    return Promises.resolve(referenceList);
                }

                //so start check if we have single folder, just seek all children to find non empty directory

                return foundFirstNonEmpty(referenceList.get(0));
            }
        };
    }

    private Promise<List<ItemReference>> foundFirstNonEmpty(ItemReference parent) {
        return AsyncPromiseHelper.createFromAsyncRequest(nodeManager.getItemReferenceRC(parent.getPath()))
                                 .thenPromise(checkForEmptiness(parent));
    }

    private Function<List<ItemReference>, Promise<List<ItemReference>>> checkForEmptiness(final ItemReference parent) {
        return new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
            @Override
            public Promise<List<ItemReference>> apply(List<ItemReference> children) throws FunctionException {
                if (children.isEmpty() || children.size() > 1) {
                    List<ItemReference> list = new ArrayList<>();
                    list.add(parent);
                    return Promises.resolve(list);
                }

                if ("file".equals(children.get(0).getType())) {
                    List<ItemReference> list = new ArrayList<>();
                    list.add(parent);
                    return Promises.resolve(list);
                } else {
                    return foundFirstNonEmpty(children.get(0));
                }

            }
        };
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableText(getDisplayFqn());
        presentation.setPresentableIcon(nodeManager.getJavaNodesResources().packageFolder());
    }

    private String getDisplayFqn() {
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
            if (parent instanceof FolderReferenceNode && ((FolderReferenceNode)parent).getAttributes().containsKey("javaContentRoot")) {
                String parentStorablePath = ((FolderReferenceNode)parent).getStorablePath();
                String currentStorablePath = getStorablePath();

                fqn = currentStorablePath.substring(parentStorablePath.length() + 1).replace('/', '.');
                break;
            }

            parent = parent.getParent();
        }

        return fqn;
    }
}
