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
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.AbstractInstance;
import org.eclipse.che.api.machine.server.impl.ServerImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.Server;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.Exec;
import org.eclipse.che.plugin.docker.client.LogMessage;
import org.eclipse.che.plugin.docker.client.LogMessageProcessor;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.client.json.PortBinding;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker implementation of {@link Instance}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstance extends AbstractInstance {
    private static final   AtomicInteger pidSequence           = new AtomicInteger(1);
    private static final   String        PID_FILE_TEMPLATE     = "/tmp/docker-exec-%s.pid";
    private static final   Pattern       PID_FILE_PATH_PATTERN = Pattern.compile(String.format(PID_FILE_TEMPLATE, "([0-9]+)"));
    protected static final Pattern       SERVICE_LABEL_PATTERN =
            Pattern.compile("che:server:(?<port>[0-9]+(/tcp|/udp)?):(?<servprop>ref|protocol)");

    private final DockerMachineFactory dockerMachineFactory;
    private final String               container;
    private final DockerConnector      docker;
    private final LineConsumer         outputConsumer;
    private final String               registry;
    private final DockerNode           node;
    private final String               workspaceId;
    private final boolean              workspaceIsBound;
    private final int memorySizeMB;

    private ContainerInfo containerInfo;

    @Inject
    public DockerInstance(DockerConnector docker,
                          @Named("machine.docker.registry") String registry,
                          DockerMachineFactory dockerMachineFactory,
                          @Assisted("machineId") String machineId,
                          @Assisted("workspaceId") String workspaceId,
                          @Assisted boolean workspaceIsBound,
                          @Assisted("creator") String creator,
                          @Assisted("displayName") String displayName,
                          @Assisted("container") String container,
                          @Assisted DockerNode node,
                          @Assisted LineConsumer outputConsumer,
                          @Assisted Recipe recipe,
                          @Assisted int memorySizeMB) {
        super(machineId, "docker", workspaceId, creator, recipe, workspaceIsBound, displayName);
        this.dockerMachineFactory = dockerMachineFactory;
        this.container = container;
        this.docker = docker;
        this.outputConsumer = outputConsumer;
        this.registry = registry;
        this.node = node;
        this.workspaceId = workspaceId;
        this.workspaceIsBound = workspaceIsBound;
        this.memorySizeMB = memorySizeMB;
    }

    @Override
    public LineConsumer getLogger() {
        return outputConsumer;
    }

    @Override
    public InstanceMetadata getMetadata() throws MachineException {
        try {
            if (containerInfo == null) {
                containerInfo = docker.inspectContainer(container);
            }

            return new DockerInstanceMetadata(containerInfo);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Map<String, Server> getServers() throws MachineException {
        try {
            if (containerInfo == null) {
                containerInfo = docker.inspectContainer(container);
            }

            final Map<String, Server> servers = getServersWithFilledPorts(node.getHost(), containerInfo.getNetworkSettings().getPorts());

            addRefAndUrlToServerFromImageLabels(servers, containerInfo.getConfig().getLabels());

            addDefaultReferenceForServersWithoutReference(servers);

            return servers;
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    private void addDefaultReferenceForServersWithoutReference(Map<String, Server> servers) {
        for (Map.Entry<String, Server> server : servers.entrySet()) {
            if (server.getValue().getRef() == null) {
                // replace / if server port contains it. E.g. 5411/udp
                ((ServerImpl)server.getValue()).setRef("Server-" + server.getKey().replace("/", "-"));
            }
        }
    }

    protected HashMap<String, Server> getServersWithFilledPorts(final String host, final Map<String, List<PortBinding>> exposedPorts) {
        final HashMap<String, Server> servers = new LinkedHashMap<>();

        for (Map.Entry<String, List<PortBinding>> portEntry : exposedPorts.entrySet()) {
            // in form 1234/tcp or 1234
            String portOrPortUdp = portEntry.getKey();
            // we are assigning ports automatically, so have 1 to 1 binding (at least per protocol)
            if (!portOrPortUdp.endsWith("/udp")) {
                // cut off /tcp if it presents
                portOrPortUdp = portOrPortUdp.split("/", 2)[0];
            }
            final PortBinding portBinding = portEntry.getValue().get(0);
            final ServerImpl server = new ServerImpl();
            servers.put(portOrPortUdp, server.setAddress(host + ":" + portBinding.getHostPort()));
        }

        return servers;
    }

    protected void addRefAndUrlToServerFromImageLabels(final Map<String, Server> servers, final Map<String, String> labels) {
        for (Map.Entry<String, String> label : labels.entrySet()) {
            final Matcher matcher = SERVICE_LABEL_PATTERN.matcher(label.getKey());
            if (matcher.matches()) {
                final String port = matcher.group("port");
                if (servers.containsKey(port)) {
                    final ServerImpl server = (ServerImpl)servers.get(port);
                    if ("ref".equals(matcher.group("servprop"))) {
                        server.setRef(label.getValue());
                    } else {
                        // value is protocol
                        server.setUrl(label.getValue() + "://" + server.getAddress());
                    }
                }
            }
        }
    }

    @Override
    public InstanceProcess getProcess(final int pid) throws NotFoundException, MachineException {
        final String findPidFilesCmd = String.format("[ -r %1$s ] && echo '%1$s'", String.format(PID_FILE_TEMPLATE, pid));
        try {
            final Exec exec = docker.createExec(container, false, "/bin/bash", "-c", findPidFilesCmd);
            final ValueHolder<InstanceProcess> dockerProcess = new ValueHolder<>();
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    final String pidFilePath = logMessage.getContent();
                    try {
                        dockerProcess.set(dockerMachineFactory.createProcess(container, null, pidFilePath, pid, true));
                    } catch (MachineException ignore) {
                    }
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
                            processes.add(dockerMachineFactory
                                                  .createProcess(container, null, pidFilePath, Integer.parseInt(matcher.group(1)), true));
                        } catch (NumberFormatException | MachineException ignore) {
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
            if (workspaceIsBound) {
                node.unbindWorkspace(workspaceId, node.getProjectsFolder());
            }

            docker.killContainer(container);

            docker.removeContainer(container, true, true);
        } catch (IOException e) {
            throw new MachineException(e.getLocalizedMessage());
        }
    }

    @Override
    protected void doBindProject(ProjectBinding project) throws MachineException {
        node.bindProject(workspaceId, project);
    }

    @Override
    public void doUnbindProject(ProjectBinding project) throws MachineException {
        node.unbindProject(workspaceId, project);
    }

    @Override
    public DockerNode getNode() {
        return node;
    }

    @Override
    public int getMemorySize() {
        return memorySizeMB;
    }
}
