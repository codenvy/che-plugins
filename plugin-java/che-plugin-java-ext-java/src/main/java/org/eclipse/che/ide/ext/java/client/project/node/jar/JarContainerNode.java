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
package org.eclipse.che.ide.ext.java.client.project.node.jar;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class JarContainerNode extends AbstractJavaSyntheticNode<Jar> {
    @Inject
    public JarContainerNode(@Assisted Jar jar,
                            @Assisted ProjectDescriptor projectDescriptor,
                            @Assisted NodeSettings nodeSettings,
                            @Nonnull JavaNodeManager javaResourceNodeManager) {
        super(jar, projectDescriptor, nodeSettings, javaResourceNodeManager);
    }

    @Nonnull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return nodeManager.getJarLibraryChildren(getProjectDescriptor(), getData().getId(), getSettings());
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableIcon(nodeManager.getJavaNodesResources().jarIcon());
        presentation.setPresentableText(getData().getName());
    }

    @Nonnull
    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
