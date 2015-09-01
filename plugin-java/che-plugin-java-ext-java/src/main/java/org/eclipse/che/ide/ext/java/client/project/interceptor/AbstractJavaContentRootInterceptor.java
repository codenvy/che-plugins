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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.shared.ContentRoot;
import org.eclipse.che.ide.project.node.FolderReferenceNode;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractJavaContentRootInterceptor implements NodeInterceptor {

    private JavaNodeManager javaResourceNodeManager;

    public AbstractJavaContentRootInterceptor(JavaNodeManager javaResourceNodeManager) {
        this.javaResourceNodeManager = javaResourceNodeManager;
    }

    @Override
    public Promise<List<Node>> intercept(Node parent, List<Node> children) {
        for (Node child : children) {
            ContentRoot contentRoot = getSourceType(child);

            if (contentRoot == null) {
                continue;
            }

            setupIcon((FolderReferenceNode)child, contentRoot);
            setupAttribute((FolderReferenceNode)child, "javaContentRoot");
        }

        return Promises.resolve(children);
    }

    @Nullable
    private ContentRoot getSourceType(Node node) {
        if (!JavaNodeManager.isJavaProject(node)) {
            return null;
        }

        if (!(node instanceof FolderReferenceNode)) {
            return null;
        }

        final FolderReferenceNode folderNode = (FolderReferenceNode)node;
        final ProjectDescriptor descriptor = folderNode.getProjectDescriptor();

        String srcFolder = _getSourceFolder(descriptor, getSrcFolderAttribute());
        if (folderNode.getStorablePath().equals(srcFolder)) {
            return ContentRoot.SOURCE;
        }

        String testSrcFolder = _getSourceFolder(descriptor, getTestSrcFolderAttribute());
        if (folderNode.getStorablePath().equals(testSrcFolder)) {
            return ContentRoot.TEST_SOURCE;
        }

//        String resourceFolder = _getSourceFolder(descriptor, getResourceFolderAttribute());
//        if (folderNode.getStorablePath().equals(resourceFolder)) {
//            return ContentRoot.RESOURCE;
//        }

        return null;
    }

    private String _getSourceFolder(ProjectDescriptor descriptor, String srcAttribute) {
        if (descriptor.getAttributes().containsKey(srcAttribute)) { //TODO avoid hard code those values
            final String srcFolder = descriptor.getAttributes().get(srcAttribute).get(0);
            return descriptor.getPath() + (srcFolder.startsWith("/") ? srcFolder : "/" + srcFolder);
        }

        return null;
    }

    public abstract String getSrcFolderAttribute();

    public abstract String getTestSrcFolderAttribute();

    public abstract String getResourceFolderAttribute();

    private void setupIcon(FolderReferenceNode srcFolder, ContentRoot srcType) {
        switch (srcType) {
            case SOURCE:
                srcFolder.getPresentation(true).setPresentableIcon(javaResourceNodeManager.getJavaNodesResources().srcFolder());
                break;
            case TEST_SOURCE:
                srcFolder.getPresentation(true).setPresentableIcon(javaResourceNodeManager.getJavaNodesResources().testSrcFolder());
                break;
//            case RESOURCE:
//                srcFolder.getPresentation(true).setPresentableIcon(javaResourceNodeManager.getJavaNodesResources().resourceFolder());
            default:
                throw new IllegalArgumentException("Wrong source type");
        }
    }

    private void setupAttribute(FolderReferenceNode node, String attributeName) {
        node.getAttributes().put(attributeName, Collections.singletonList("true"));
    }

    @Override
    public Integer weightOrder() {
        return 0;
    }
}
