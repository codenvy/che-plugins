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
import org.eclipse.che.api.machine.shared.Recipe;

import javax.annotation.Nullable;

/**
 * Provides Docker machine implementation instances.
 *
 * @author Alexander Garagatyi
 */
public interface DockerMachineFactory {
    InstanceProcess createProcess(@Assisted("container") String container,
                                  @Assisted("command") @Nullable String command,
                                  @Assisted("pid_file_path") String pidFilePath,
                                  @Assisted int pid,
                                  @Assisted boolean isStarted) throws MachineException;

    Instance createInstance(@Assisted("machineId") String machineId,
                            @Assisted("workspaceId") String workspaceId,
                            @Assisted boolean workspaceIsBound,
                            @Assisted("creator") String creator,
                            @Assisted("displayName") String displayName,
                            @Assisted("container") String container,
                            @Assisted DockerNode node,
                            @Assisted LineConsumer outputConsumer,
                            @Assisted("recipe") Recipe recipe,
                            @Assisted int memorySizeMB) throws MachineException;

    DockerNode createNode(@Assisted String containerId) throws MachineException;
}
