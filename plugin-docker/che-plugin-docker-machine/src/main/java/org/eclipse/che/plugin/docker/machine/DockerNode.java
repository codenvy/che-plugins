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
package org.eclipse.che.plugin.docker.machine;

import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

/**
 * Provides access to operation machines need but not supported by the Docker
 *
 * @author Alexander Garagatyi
 */
public interface DockerNode {
    /**
     * Bind project to docker container
     *
     * @param workspaceId
     *         workspace that owns project to bind
     * @param project
     *         project to bind
     * @throws MachineException
     *         if error occur
     */
    void bindProject(String workspaceId, ProjectBinding project) throws MachineException;

    /**
     * Unbind project from docker container
     *
     * @param workspaceId
     *         workspace that owns project to unbind
     * @param project
     *         project to unbind
     * @throws MachineException
     *         if error occur
     */
    void unbindProject(String workspaceId, ProjectBinding project) throws MachineException;

    /**
     * Get path of folder on docker node that will contain bound projects
     */
    String getProjectsFolder();
}
