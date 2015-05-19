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

import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.Exec;
import org.eclipse.che.plugin.docker.client.LogMessage;
import org.eclipse.che.plugin.docker.client.LogMessageProcessor;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.HostConfig;

import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.spi.Image;
import org.eclipse.che.api.machine.server.spi.ImageMetadata;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Docker implementation of {@link Image}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerImage implements Image {

    private static final Logger LOG = LoggerFactory.getLogger(DockerImage.class);

    private final DockerConnector   docker;
    private final DockerImageKey    key;
    private final LineConsumer      outputConsumer;
    private final String            registry;
    private final DockerNodeFactory dockerNodeFactory;

    DockerImage(DockerConnector docker,
                String registry,
                DockerNodeFactory dockerNodeFactory,
                DockerImageKey key,
                LineConsumer outputConsumer) {
        this.docker = docker;
        this.key = key;
        this.outputConsumer = outputConsumer;
        this.registry = registry;
        this.dockerNodeFactory = dockerNodeFactory;
    }

    @Override
    public ImageMetadata getMetadata() throws MachineException {
        try {
            return new DockerImageMetadata(key, docker.inspectImage(key.getImageId()));
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    @Override
    public Instance createInstance() throws MachineException {
        try {
            final ContainerConfig config = new ContainerConfig().withImage(key.getImageId())
                                                                .withMemorySwap(-1)
                                                                .withExposedPorts(Collections.singletonMap("4300", Collections
                                                                        .<String, String>emptyMap()));

            LOG.debug("Creating container from image {}", key.getImageId());

            final String containerId = docker.createContainer(config, null).getId();

            LOG.debug("Container {} has been created successfully", containerId);

            final DockerNode node = dockerNodeFactory.createNode(containerId);
            String hostProjectsFolder = node.getProjectsFolder();

            LOG.debug("Starting container {}", containerId);
            docker.startContainer(containerId,
                                  new HostConfig().withPublishAllPorts(true)
                                                  .withBinds(String.format("%s:%s", hostProjectsFolder, "/projects"),
                                                             "/usr/local/codenvy/terminal:/usr/local/codenvy/terminal"),//TODO add :ro
                                  new LogMessagePrinter(outputConsumer));
            LOG.debug("Container {} has been started successfully", containerId);

            startTerminal(containerId);

            return new DockerInstance(docker, registry, containerId, outputConsumer, node);
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    private void startTerminal(final String container) {
        try {
            final Exec exec = docker.createExec(container, true, "/bin/bash", "-c",
                                                "/usr/local/codenvy/terminal/terminal -addr :4300 -cmd /bin/sh -static /usr/local/codenvy/terminal/");
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    LOG.error(String.format("Terminal error in container %s. %s", container, logMessage.getContent()));
                }
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }
}
