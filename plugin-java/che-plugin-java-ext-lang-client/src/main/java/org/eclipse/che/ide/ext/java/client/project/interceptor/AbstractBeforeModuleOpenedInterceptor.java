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

import com.google.inject.Provider;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ext.java.client.dependenciesupdater.DependenciesUpdater;
import org.eclipse.che.ide.ext.java.client.project.node.JavaNodeManager;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;

import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractBeforeModuleOpenedInterceptor implements NodeInterceptor {

    private Provider<DependenciesUpdater> dependenciesUpdater;

    public AbstractBeforeModuleOpenedInterceptor(Provider<DependenciesUpdater> dependenciesUpdater) {
        this.dependenciesUpdater = dependenciesUpdater;
    }

    @Override
    public Promise<List<Node>> intercept(Node parent, List<Node> children) {

        if (parent instanceof ModuleDescriptorNode && JavaNodeManager.isJavaProject(parent) && isValid(parent)) {
            dependenciesUpdater.get().updateDependencies(((HasProjectDescriptor)parent).getProjectDescriptor());
        }

        return Promises.resolve(children);
    }

    @Override
    public Integer weightOrder() {
        return 53;
    }

    public abstract boolean isValid(Node node);


}
