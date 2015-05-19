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
package org.eclipse.che.plugin.docker.machine.local;

import org.eclipse.che.plugin.docker.machine.DockerNode;
import org.eclipse.che.plugin.docker.machine.DockerNodeFactory;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

/**
 *
 * Local directory as a workspace (projects tree) to be bound to Machines
 *
 * @author gazarenkov
 */
public class LocalDockerNode implements DockerNode {

    private final File folder;

    public LocalDockerNode(String projectsFolder) throws IOException {

        folder = new File(projectsFolder);
        if(!folder.exists() || folder.isFile())
            throw new IOException("Folder "+folder.getAbsolutePath()+ " does not exist");

    }

    @Override
    public void bindProject(String workspaceId, ProjectBinding project) throws MachineException {

    }

    @Override
    public void unbindProject(String workspaceId, ProjectBinding project) throws MachineException {

    }

    @Override
    public String getProjectsFolder() {
        return folder.getAbsolutePath();
    }
}
