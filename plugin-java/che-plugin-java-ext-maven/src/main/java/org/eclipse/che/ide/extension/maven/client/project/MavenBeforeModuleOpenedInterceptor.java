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
package org.eclipse.che.ide.extension.maven.client.project;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ext.java.client.dependenciesupdater.DependenciesUpdater;
import org.eclipse.che.ide.ext.java.client.project.interceptor.AbstractBeforeModuleOpenedInterceptor;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import java.util.List;
import java.util.Map;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class MavenBeforeModuleOpenedInterceptor extends AbstractBeforeModuleOpenedInterceptor {

    @Inject
    public MavenBeforeModuleOpenedInterceptor(Provider<DependenciesUpdater> dependenciesUpdater) {
        super(dependenciesUpdater);
    }

    @Override
    public boolean isValid(Node node) {
        HasProjectDescriptor descriptor = (HasProjectDescriptor)node;

        Map<String, List<String>> attr = descriptor.getProjectDescriptor().getAttributes();
        return attr.containsKey(MavenAttributes.PACKAGING) && !"pom".equals(attr.get(MavenAttributes.PACKAGING).get(0));
    }
}
