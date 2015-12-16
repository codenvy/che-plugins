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

import org.eclipse.che.api.core.model.machine.MachineState;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.machine.node.DockerNode;

/**
 * Provides Docker machine implementation instances.
 *
 * @author Alexander Garagatyi
 */
public interface DockerMachineFactory {
    /**
     * Creates docker implementation of {@link InstanceProcess} for process that is running or can be started in container
     *
     * @param container container identifier
     * @param command command line of process
     * @param pidFilePath path where file that identifies running process should be placed
     * @param pid id of the {@code InstanceProcess}. It's external PID that may differ from PID inside container
     * @param isStarted if process was started already
     * @throws MachineException if error occurs on creation of {@code InstanceProcess}
     */
    InstanceProcess createProcess(@Assisted("container") String container,
                                  @Assisted("command") @Nullable String command,
                                  @Assisted("pid_file_path") String pidFilePath,
                                  @Assisted int pid,
                                  @Assisted boolean isStarted) throws MachineException;

    /**
     * Creates docker implementation of {@link Instance}
     *
     * @param machineState description of machine
     * @param container container that represents {@code Instance}
     * @param node description of server where container is running
     * @param outputConsumer consumer of output from container main process
     * @throws MachineException if error occurs on creation of {@code Instance}
     */
    Instance createInstance(@Assisted MachineState machineState,
                            @Assisted("container") String container,
                            @Assisted DockerNode node,
                            @Assisted LineConsumer outputConsumer) throws MachineException;

    /**
     * Creates {@link DockerNode} that describes server where container is running
     *
     * @param workspaceId id of workspace that owns docker container
     * @param containerId identifier of container
     * @throws MachineException if error occurs on creation of {@code DockerNode}
     */
    DockerNode createNode(@Assisted("workspace") String workspaceId,
                          @Assisted("container") String containerId) throws MachineException;

    /**
     * Creates {@link DockerInstanceMetadata} instance using assisted injection
     *
     * @param containerInfo description of docker container
     * @param containerHost host where docker container is placed
     */
    DockerInstanceMetadata createMetadata(@Assisted ContainerInfo containerInfo,
                                          @Assisted String containerHost);
}
