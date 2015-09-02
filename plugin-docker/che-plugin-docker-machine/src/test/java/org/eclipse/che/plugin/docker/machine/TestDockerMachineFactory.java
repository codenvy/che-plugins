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

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.plugin.docker.client.DockerConnector;

import javax.annotation.Nullable;

/**
 * @author Alexander Garagatyi
 */
public class TestDockerMachineFactory implements DockerMachineFactory {
    private final DockerConnector docker;

    public TestDockerMachineFactory(DockerConnector docker) {
        this.docker = docker;
    }

    @Override
    public InstanceProcess createProcess(@Assisted("container") String container,
                                         @Assisted("command") @Nullable String command,
                                         @Assisted("pid_file_path") String pidFilePath,
                                         @Assisted int pid,
                                         @Assisted boolean isStarted)
            throws MachineException {
        return new DockerProcess(docker, container, command, pidFilePath, pid, isStarted);
    }

    @Override
    public Instance createInstance(@Assisted("machineId") String machineId,
                                   @Assisted("workspaceId") String workspaceId,
                                   @Assisted boolean isDev,
                                   @Assisted("creator") String creator,
                                   @Assisted("displayName") String displayName,
                                   @Assisted("container") String container,
                                   @Assisted DockerNode node,
                                   @Assisted LineConsumer outputConsumer,
                                   @Assisted Recipe recipe,
                                   @Assisted int memorySizeMB) throws MachineException {
        return new DockerInstance(docker,
                                  "localhost:5000",
                                  this,
                                  machineId,
                                  workspaceId,
                                  isDev,
                                  creator,
                                  displayName,
                                  container,
                                  node,
                                  outputConsumer,
                                  recipe,
                                  memorySizeMB);
    }

    @Override
    public DockerNode createNode(@Assisted String containerId) throws MachineException {
        return new DockerNode() {
            @Override
            public void bindProject(String workspaceId, ProjectBinding project) throws MachineException {
            }

            @Override
            public void unbindProject(String workspaceId, ProjectBinding project) throws MachineException {
            }

            @Override
            public void bindWorkspace(String workspaceId, String hostProjectsFolder) throws MachineException {
            }

            @Override
            public void unbindWorkspace(String workspaceId, String hostProjectsFolder) throws MachineException {
            }

            @Override
            public String getProjectsFolder() {
                return "/tmp";
            }

            @Override
            public String getHost() {
                return "localhost";
            }
        };
    }
}
