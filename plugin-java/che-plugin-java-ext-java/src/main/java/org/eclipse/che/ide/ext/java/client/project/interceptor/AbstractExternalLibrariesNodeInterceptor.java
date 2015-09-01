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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettings;
import org.eclipse.che.ide.project.node.AbstractProjectBasedNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.eclipse.che.ide.project.node.NodeManager.isProjectOrModuleNode;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public abstract class AbstractExternalLibrariesNodeInterceptor implements NodeInterceptor {

    private JavaNodeManager javaResourceNodeManager;

    @Inject
    public AbstractExternalLibrariesNodeInterceptor(JavaNodeManager javaResourceNodeManager) {
        this.javaResourceNodeManager = javaResourceNodeManager;
    }

    @Override
    public Promise<List<Node>> intercept(Node parent, List<Node> children) {

        if (!(isProjectOrModuleNode(parent)/* || isJavaProject(parent)*/)) {
            return Promises.resolve(children);
        }

        if (getSettings() == null || !getSettings().isShowExternalLibrariesNode() || !show((HasProjectDescriptor)parent)) {
            return Promises.resolve(children);
        }

        insertExternalLibrariesNode(children, createExternalLibrariesNode(parent));

        return Promises.resolve(children);
    }

    public abstract boolean show(HasProjectDescriptor node);

    private void insertExternalLibrariesNode(@Nonnull List<Node> children, @Nullable Node externalLibrariesNode) {
        if (externalLibrariesNode != null) {
            children.add(externalLibrariesNode);
        }
    }

    @Nullable
    private Node createExternalLibrariesNode(Node parent) {
        if (!(parent instanceof AbstractProjectBasedNode<?>)) {
            return null;
        }

        if (getSettings() == null) {
            return null;
        }

        ProjectDescriptor projectDescriptor = ((AbstractProjectBasedNode)parent).getProjectDescriptor();
        return javaResourceNodeManager.getJavaNodeFactory().newExternalLibrariesNode(projectDescriptor, getSettings());
    }

    @Nullable
    private JavaNodeSettings getSettings() {
        NodeSettings settings = javaResourceNodeManager.getJavaSettingsProvider().getSettings();

        return settings instanceof JavaNodeSettings ? (JavaNodeSettings)settings : null;
    }

    @Override
    public Integer weightOrder() {
        return 50;
    }
}
