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

package org.eclipse.che.core.internal.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IWorkspace;

import javax.annotation.PostConstruct;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class ResourcesPlugin {
    /**
     * The workspace managed by the single instance of this
     * plug-in runtime class, or <code>null</code> is there is none.
     */
    private static Workspace workspace = null;
    private static String indexPath;
    private static String workspacePath;

    @Inject
    public ResourcesPlugin(@Named("che.jdt.workspace.index.dir") String indexPath, @Named("che.workspace.root.path") String workspacePath) {
        ResourcesPlugin.indexPath = indexPath;
        ResourcesPlugin.workspacePath = workspacePath;
    }

    public static String getPathToWorkspace() {
        return workspacePath;
    }

    public static String getIndexPath() {
        return indexPath;
    }

    @PostConstruct
    public void start() {
        workspace = new Workspace(workspacePath);
    }

    /**
     * Returns the workspace. The workspace is not accessible after the resources
     * plug-in has shutdown.
     *
     * @return the workspace that was created by the single instance of this
     *   plug-in class.
     */
    public static IWorkspace getWorkspace() {
        if (workspace == null)
            throw new IllegalStateException(Messages.resources_workspaceClosed);
        return workspace;
    }
}
