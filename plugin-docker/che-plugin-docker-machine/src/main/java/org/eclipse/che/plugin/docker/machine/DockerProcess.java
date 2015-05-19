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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;

import java.io.IOException;
import java.util.Arrays;

/**
 * Docker implementation of {@link InstanceProcess}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerProcess implements InstanceProcess {
    private final DockerConnector docker;
    private final String          container;
    private final String          pidFilePath;
    private final int             pid;

    private boolean started;
    private String  command;

    DockerProcess(DockerConnector docker, String container, String pidFilePath, int pid) {
        this.docker = docker;
        this.container = container;
        this.pidFilePath = pidFilePath;
        this.started = true;
        this.pid = pid;
    }

    DockerProcess(DockerConnector docker, String container, String command, String pidFilePath, int pid) {
        this.docker = docker;
        this.container = container;
        this.command = command;
        this.pidFilePath = pidFilePath;
        this.started = false;
        this.pid = pid;
    }

    @Override
    public int getPid() {
        return pid;
    }

    @Override
    public String getCommandLine() {
        if (command == null) {
            command = readCommandLine();
        }
        return command;
    }

    @Override
    public void start() throws ConflictException, MachineException {
        start(null);
    }

    @Override
    public void start(LineConsumer output) throws ConflictException, MachineException {
        if (started) {
            throw new ConflictException("Process already started.");
        }
        // Trap is invoked when bash session ends. Here we kill all subprocesses of shell and remove pid-file.
        final String trap = String.format("trap '[ -z \"$(jobs -p)\" ] || kill $(jobs -p); [ -e %1$s ] && rm %1$s' EXIT", pidFilePath);
        // 'echo' saves shell pid in file, then run command
        final String bashCommand = trap + "; echo $$>" + pidFilePath + "; " + command;
        final String[] command = {"/bin/bash", "-c", bashCommand};
        Exec exec;
        try {
            exec = docker.createExec(container, output == null, command);
        } catch (IOException e) {
            throw new MachineException(String.format("Error occurs while initializing command %s in docker container %s: %s",
                                                     Arrays.toString(command), container, e.getMessage()), e);
        }
        started = true;
        try {
            docker.startExec(exec.getId(), output == null ? null : new LogMessagePrinter(output));
        } catch (IOException e) {
            throw new MachineException(String.format("Error occurs while executing command %s in docker container %s: %s",
                                                     Arrays.toString(exec.getCommand()), container, e.getMessage()), e);
        }
    }

    @Override
    public boolean isAlive() {
        if (!started) {
            return false;
        }
        // Read pid from file and run 'kill -0 [pid]' command.
        final String isAliveCmd = String.format("[ -r %1$s ] && kill -0 $(<%1$s) || echo 'Unable read PID file'", pidFilePath);
        final ListLineConsumer output = new ListLineConsumer();
        final String[] command = {"/bin/bash", "-c", isAliveCmd};
        Exec exec;
        try {
            exec = docker.createExec(container, false, command);
        } catch (IOException e) {
            throw new DockerRuntimeException(String.format("Error occurs while initializing command %s in docker container %s: %s",
                                                           Arrays.toString(command), container, e.getMessage()), e);
        }
        try {
            docker.startExec(exec.getId(), new LogMessagePrinter(output));
        } catch (IOException e) {
            throw new DockerRuntimeException(String.format("Error occurs while executing command %s in docker container %s: %s",
                                                           Arrays.toString(exec.getCommand()), container, e.getMessage()), e);
        }
        // 'kill -0 [pid]' is silent if process is running or print "No such process" message otherwise
        return output.getText().isEmpty();
    }

    @Override
    public void kill() throws MachineException {
        if (started) {
            // Read pid from file and run 'kill [pid]' command.
            final String killCmd = String.format("[ -r %1$s ] && kill $(<%1$s)", pidFilePath);
            final String[] command = {"/bin/bash", "-c", killCmd};
            Exec exec;
            try {
                exec = docker.createExec(container, true, command);
            } catch (IOException e) {
                throw new MachineException(String.format("Error occurs while initializing command %s in docker container %s: %s",
                                                         Arrays.toString(command), container, e.getMessage()), e);
            }
            try {
                docker.startExec(exec.getId(), LogMessageProcessor.DEV_NULL);
            } catch (IOException e) {
                throw new MachineException(String.format("Error occurs while executing command %s in docker container %s: %s",
                                                         Arrays.toString(exec.getCommand()), container, e.getMessage()), e);
            }
        }
    }

    private String readCommandLine() {
        final String readCommandLineCmd = String.format(
                "[ ! -r %1$s ] && echo 'Unable read PID file' || echo $(<%1$s) | xargs ps -o command= -p | sed 's/^.*; //g'", pidFilePath);
        final ValueHolder<String> cmdLineHolder = new ValueHolder<>();
        final String[] command = {"/bin/bash", "-c", readCommandLineCmd};
        Exec exec;
        try {
            exec = docker.createExec(container, false, command);
        } catch (IOException e) {
            throw new DockerRuntimeException(String.format("Error occurs while initializing command %s in docker container %s: %s",
                                                           Arrays.toString(command), container, e.getMessage()), e);
        }
        try {
            docker.startExec(exec.getId(), new LogMessageProcessor() {
                @Override
                public void process(LogMessage logMessage) {
                    cmdLineHolder.set(logMessage.getContent());
                }
            });
        } catch (IOException e) {
            throw new DockerRuntimeException(String.format("Error occurs while executing command %s in docker container %s: %s",
                                                           Arrays.toString(exec.getCommand()), container, e.getMessage()), e);
        }
        return cmdLineHolder.get();
    }
}
