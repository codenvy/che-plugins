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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.MachineMetadata;
import org.eclipse.che.api.core.model.machine.MachineState;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.AbstractInstance;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.Exec;
import org.eclipse.che.plugin.docker.client.LogMessage;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.machine.node.DockerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Arrays;
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
 * @author Anton Korneta
 */
public class DockerInstance extends AbstractInstance {
    private static final Logger LOG = LoggerFactory.getLogger(DockerInstance.class);

    private static final   AtomicInteger pidSequence           = new AtomicInteger(1);
    private static final   String        PID_FILE_TEMPLATE     = "/tmp/docker-exec-%s.pid";
    private static final   Pattern       PID_FILE_PATH_PATTERN = Pattern.compile(String.format(PID_FILE_TEMPLATE, "([0-9]+)"));

    private final DockerMachineFactory       dockerMachineFactory;
    private final String                     container;
    private final DockerConnector            docker;
    private final LineConsumer               outputConsumer;
    private final String                     registry;
    private final DockerNode                 node;
    private final DockerInstanceStopDetector dockerInstanceStopDetector;

    private ContainerInfo containerInfo;

    @Inject
    public DockerInstance(DockerConnector docker,
                          @Named("machine.docker.registry") String registry,
                          DockerMachineFactory dockerMachineFactory,
                          @Assisted MachineState machineState,
                          @Assisted("container") String container,
                          @Assisted DockerNode node,
                          @Assisted LineConsumer outputConsumer,
                          DockerInstanceStopDetector dockerInstanceStopDetector) {
        super(machineState);
        this.dockerMachineFactory = dockerMachineFactory;
        this.container = container;
        this.docker = docker;
        this.outputConsumer = outputConsumer;
        this.registry = registry;
        this.node = node;
        this.dockerInstanceStopDetector = dockerInstanceStopDetector;
    }

    @Override
    public LineConsumer getLogger() {
        return outputConsumer;
    }

    @Override
    public MachineMetadata getMetadata() {
        try {
            if (containerInfo == null) {
                containerInfo = docker.inspectContainer(container);
            }

            return dockerMachineFactory.createMetadata(containerInfo, node.getHost());
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    @Override
    public InstanceProcess getProcess(final int pid) throws NotFoundException, MachineException {
        final String findPidFilesCmd = String.format("[ -r %1$s ] && echo '%1$s'", String.format(PID_FILE_TEMPLATE, pid));
        try {
            final Exec exec = docker.createExec(container, false, "/bin/bash", "-c", findPidFilesCmd);
            final ValueHolder<InstanceProcess> dockerProcess = new ValueHolder<>();
            docker.startExec(exec.getId(), logMessage -> {
                final String pidFilePath = logMessage.getContent();
                try {
                    dockerProcess.set(dockerMachineFactory.createProcess(container, null, pidFilePath, pid, true));
                } catch (MachineException ignore) {
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
            docker.startExec(exec.getId(), logMessage -> {
                final String pidFilePath = logMessage.getContent().trim();
                final Matcher matcher = PID_FILE_PATH_PATTERN.matcher(pidFilePath);
                if (matcher.matches()) {
                    try {
                        processes.add(dockerMachineFactory.createProcess(container,
                                                                         null,
                                                                         pidFilePath,
                                                                         Integer.parseInt(matcher.group(1)),
                                                                         true));
                    } catch (NumberFormatException | MachineException ignore) {
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
        return dockerMachineFactory.createProcess(container, commandLine, String.format(PID_FILE_TEMPLATE, pid), pid, false);
    }

    @Override
    public InstanceKey saveToSnapshot(String owner) throws MachineException {
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
            docker.push(repository, null, registry, currentProgressStatus -> {
                try {
                    outputConsumer.writeLine(progressLineFormatter.format(currentProgressStatus));
                } catch (IOException ignored) {
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
        dockerInstanceStopDetector.stopDetection(container);
        try {
            if (isDev()) {
                node.unbindWorkspace();
            }

            docker.killContainer(container);

            docker.removeContainer(container, true, true);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage());
        }
    }

    @Override
    public DockerNode getNode() {
        return node;
    }

    /**
     * Reads file content by specified file path.
     *
     * TODO:
     * add file size checking,
     * note that size checking and file content reading
     * should be done in an atomic way,
     * which means that two separate instance processes is not the case.
     *
     * @param filePath
     *         path to file on machine instance
     * @param startFrom
     *         line number to start reading from
     * @param limit
     *         limitation on line
     * @return if {@code limit} and {@code startFrom} grater than 0
     * content from {@code startFrom} to {@code startFrom + limit} will be returned,
     * if file contains less lines than {@code startFrom} empty content will be returned
     * @throws MachineException
     *         if any error occurs with file reading
     */
    @Override
    public String readFileContent(String filePath, int startFrom, int limit) throws MachineException {
        if (limit <= 0 || startFrom <= 0) {
            throw new MachineException("Impossible to read file " + limit + " lines from " + startFrom + " line");
        }

        // command sed getting file content from startFrom line to (startFrom + limit)
        String bashCommand = String.format("sed -n \'%1$2s, %2$2sp\' %3$2s", startFrom, startFrom + limit, filePath);

        final String[] command = {"/bin/bash", "-c", bashCommand};

        ListLineConsumer lines = new ListLineConsumer();
        try {
            Exec exec = docker.createExec(container, false, command);
            docker.startExec(exec.getId(), new LogMessagePrinter(lines, LogMessage::getContent));
        } catch (IOException e) {
            throw new MachineException(String.format("Error occurs while initializing command %s in docker container %s: %s",
                                                     Arrays.toString(command), container, e.getLocalizedMessage()), e);
        }

        String content = lines.getText();
        if (content.contains("sed: can't read " + filePath + ": No such file or directory") ||
            content.contains("cat: " + filePath + ": No such file or directory")) {
            throw new MachineException("File with path " + filePath + " not found");
        }
        return content;
    }

    /**
     * Copies files from specified container.
     *
     * @param sourceMachine
     *         source machine
     * @param sourcePath
     *         path to file or directory inside specified container
     * @param targetPath
     *         path to destination file or directory inside container
     * @param overwrite
     *         If "false" then it will be an error if unpacking the given content would cause
     *         an existing directory to be replaced with a non-directory and vice versa.
     * @throws MachineException
     *         if any error occurs when files are being copied
     */
    @Override
    public void copy(Instance sourceMachine, String sourcePath, String targetPath, boolean overwrite) throws MachineException {
        if (!(sourceMachine instanceof DockerInstance)) {
            throw new MachineException("Unsupported copying between not docker machines");
        }
        try {
            docker.putResource(container,
                               targetPath,
                               docker.getResource(((DockerInstance)sourceMachine).container, sourcePath),
                               overwrite);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage());
        }
    }
}
