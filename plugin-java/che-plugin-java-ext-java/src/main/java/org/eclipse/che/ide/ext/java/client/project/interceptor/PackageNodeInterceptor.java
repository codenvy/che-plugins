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
package org.eclipse.che.ide.ext.java.client.project.interceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettingsProvider;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ItemReferenceChainFilter;

import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class PackageNodeInterceptor implements NodeInterceptor {

    private       JavaNodeManager nodeManager;

    @Inject
    public PackageNodeInterceptor(JavaNodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }

    @Override
    public Promise<List<Node>> intercept(final Node parent, final List<Node> children) {

        if (parent instanceof FolderReferenceNode && ((FolderReferenceNode)parent).getAttributes().containsKey("javaContentRoot")) {
            //we catch source folder node
            final FolderReferenceNode sourceFolder = (FolderReferenceNode)parent;

            final JavaNodeSettingsProvider settingsProvider = nodeManager.getJavaSettingsProvider();

            return nodeManager.getChildren(sourceFolder.getData(),
                                           sourceFolder.getProjectDescriptor(),
                                           settingsProvider.getSettings(),
                                           emptyMiddlePackageFilter()).thenPromise(new Function<List<Node>, Promise<List<Node>>>() {
                @Override
                public Promise<List<Node>> apply(List<Node> arg) throws FunctionException {

                    for (Node pkg : arg) {
                        pkg.setParent(parent);
                        if (pkg instanceof FolderReferenceNode) {
                            String parentPath = sourceFolder.getStorablePath();
                            String pkgPath = ((FolderReferenceNode)pkg).getStorablePath();

                            String fqnPath = pkgPath.replaceFirst(parentPath, "");
                            if (fqnPath.startsWith("/")) {
                                fqnPath = fqnPath.substring(1);
                            }

                            fqnPath = fqnPath.replaceAll("/", ".");

                            ((FolderReferenceNode)pkg).getPresentation(false).setPresentableText(fqnPath);
                        }
                    }

                    return Promises.resolve(arg);
                }
            });
        }

        return Promises.resolve(children);
    }

    private ItemReferenceChainFilter emptyMiddlePackageFilter() {
        return new ItemReferenceChainFilter() {
            @Override
            public Promise<List<ItemReference>> process(List<ItemReference> referenceList) {

                if (referenceList.isEmpty() || referenceList.size() > 1) {
                    //if children in directory more than one
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
                    return Promises.resolve(Collections.singletonList(parent));
                }

                if ("file".equals(children.get(0).getType())) {
                    return Promises.resolve(Collections.singletonList(parent));
                } else {
                    return foundFirstNonEmpty(children.get(0));
                }

            }
        };
    }

    @Override
    public Integer weightOrder() {
        return 51;
    }
}
