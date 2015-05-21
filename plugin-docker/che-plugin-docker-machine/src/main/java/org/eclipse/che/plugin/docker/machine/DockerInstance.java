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
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.commons.lang.NameGenerator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker implementation of {@link Instance}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstance implements Instance {
    private static final AtomicInteger pidSequence           = new AtomicInteger(1);
    private static final String        PID_FILE_TEMPLATE     = "/tmp/docker-exec-%s.pid";
    private static final Pattern       PID_FILE_PATH_PATTERN = Pattern.compile(String.format(PID_FILE_TEMPLATE, "([0-9]+)"));

    private final String          container;
    private final DockerConnector docker;
    private final LineConsumer    outputConsumer;
    private final String          registry;
    private final DockerNode      node;

    DockerInstance(DockerConnector docker,
                   String registry,
                   String container,
                   LineConsumer outputConsumer,
                   DockerNode node) {
        this.container = container;
        this.docker = docker;
        this.outputConsumer = outputConsumer;
        this.registry = registry;
        this.node = node;
    }

    @Override
    public InstanceMetadata getMetadata() throws MachineException {
        try {
            ContainerInfo info = docker.inspectContainer(container);
            return new DockerInstanceMetadata(info);
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    @Override
    public InstanceProcess getProcess(final int pid) throws NotFoundException, MachineException {
        final String findPidFilesCmd = String.format("[ -r %1$s ] && echo '%1$s'", String.format(PID_FILE_TEMPLATE, pid));
        try {
            final Exec exec = docker.createExec(container, false, "/bin/bash", "-c", findPidFilesCmd);
            final ValueHolder<DockerProcess> dockerProcess = new ValueHolder<>();
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    final String pidFilePath = logMessage.getContent();
                    dockerProcess.set(new DockerProcess(docker, container, pidFilePath, pid));
                }
            });

            if (dockerProcess.get() == null) {
                throw new NotFoundException(String.format("Process with pid %s not found", pid));
            }
            return dockerProcess.get();
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    @Override
    public List<InstanceProcess> getProcesses() throws MachineException {
        final String findPidFilesCmd = String.format("find %s -print", String.format(PID_FILE_TEMPLATE, "*"));
        try {
            final Exec exec = docker.createExec(container, false, "/bin/bash", "-c", findPidFilesCmd);
            final List<InstanceProcess> processes = new LinkedList<>();
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    final String pidFilePath = logMessage.getContent().trim();
                    final Matcher matcher = PID_FILE_PATH_PATTERN.matcher(pidFilePath);
                    if (matcher.matches()) {
                        try {
                            processes.add(new DockerProcess(docker, container, pidFilePath, Integer.parseInt(matcher.group(1))));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            });
            return processes;
        } catch (IOException e) {
            throw new MachineException(e);
        }
    }

    @Override
    public InstanceProcess createProcess(String commandLine) throws MachineException {
        final Integer pid = pidSequence.getAndIncrement();
        return new DockerProcess(docker, container, commandLine, String.format(PID_FILE_TEMPLATE, pid), pid);
    }

    @Override
    public InstanceKey saveToSnapshot(String owner, String label) throws MachineException {
        try {
            final String repository = generateRepository();
            String comment = String.format("Suspended at %1$ta %1$tb %1$td %1$tT %1$tZ %1$tY", System.currentTimeMillis());
            if (owner != null) {
                comment = comment + " by " + owner;
            }
            // !! We SHOULD NOT pause container before commit because all execs will fail
            final String imageId = docker.commit(container, repository, null, comment, owner);
            // to push image to private registry it should be tagged with registry in repo name
            // https://docs.docker.com/reference/api/docker_remote_api_v1.16/#push-an-image-on-the-registry
            docker.tag(imageId, registry + "/" + repository, null);
            final ProgressLineFormatterImpl progressLineFormatter = new ProgressLineFormatterImpl();
            docker.push(repository, null, registry, new ProgressMonitor() {
                @Override
                public void updateProgress(ProgressStatus currentProgressStatus) {
                    try {
                        outputConsumer.writeLine(progressLineFormatter.format(currentProgressStatus));
                    } catch (IOException ignored) {
                    }
                }
            });
            return new DockerInstanceKey(repository, null, imageId, registry);
        } catch (IOException e) {
            throw new MachineException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    private String generateRepository() {
        return NameGenerator.generate(null, 16);
    }

    @Override
    public void destroy() throws MachineException {
        try {
            docker.killContainer(container);
            docker.removeContainer(container, true, true);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage());
        }
    }

    @Override
    public void bindProject(String workspaceId, ProjectBinding project) throws MachineException {
        node.bindProject(workspaceId, project);
    }

    @Override
    public void unbindProject(String workspaceId, ProjectBinding project) throws MachineException {
        node.unbindProject(workspaceId, project);
    }
}
