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
package org.eclipse.che.plugin.docker.machine.ext;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.Exec;
import org.eclipse.che.plugin.docker.client.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;

import static java.nio.file.Files.notExists;

/**
 * Starts extensions server in the machine after start
 *
 * @author Alexander Garagatyi
 */
@Singleton // must be eager
public class DockerMachineExtServerLauncher {
    public static final String START_EXT_SERVER_COMMAND = "machine.server.ext.run_command";

    private static final Logger LOG = LoggerFactory.getLogger(DockerMachineExtServerLauncher.class);

    private final EventService    eventService;
    private final DockerConnector docker;
    private final MachineManager  machineManager;
    private final String          extServerStartCommand;

    @Inject
    public DockerMachineExtServerLauncher(EventService eventService,
                                          DockerConnector docker,
                                          MachineManager machineManager,
                                          @Named(START_EXT_SERVER_COMMAND) String extServerStartCommand) {
        this.eventService = eventService;
        this.docker = docker;
        this.machineManager = machineManager;
        this.extServerStartCommand = extServerStartCommand;
    }

    @PostConstruct
    public void start() {
        eventService.subscribe(new EventSubscriber<MachineStatusEvent>() {
            @Override
            public void onEvent(MachineStatusEvent event) {
                // TODO launch it on dev machines only
                if (event.getEventType() == MachineStatusEvent.EventType.RUNNING) {
                    try {
                        final Instance machine = machineManager.getMachine(event.getMachineId());

                        // ext server doesn't exist in non-dev machines
                        if (machine.isDev()) {
                            final String containerId = machine.getMetadata().getProperties().get("id");

                            final Exec exec = docker.createExec(containerId, true, "/bin/sh", "-c", extServerStartCommand);
                            // TODO check that ext server starts successfully
                            docker.startExec(exec.getId(), MessageProcessor.DEV_NULL);
                        }
                    } catch (IOException | MachineException | NotFoundException e) {
                        LOG.error(e.getLocalizedMessage(), e);
                        // TODO send event that ext server is unavailable
                    }
                }
            }
        });
    }
}
