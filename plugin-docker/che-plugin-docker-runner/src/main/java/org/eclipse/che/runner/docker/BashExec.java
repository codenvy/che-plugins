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
package org.eclipse.che.runner.docker;

import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.docker.DockerConnector;
import org.eclipse.che.docker.Exec;

import java.io.IOException;

/**
 * Execs command with /bin/bash interpreter.
 *
 * @author andrew00x
 */
public class BashExec {
    private final DockerConnector docker;
    private final String          container;
    private final String          command;

    private String pidFilePath;

    public BashExec(DockerConnector docker, String container, String command) {
        this.docker = docker;
        this.container = container;
        this.command = command;
    }

    public void exec() throws IOException {
        exec(null);
    }

    /**
     * Execs command.
     *
     * @param output
     *         consumer of command's output. If this parameter is {@code null} command started in the background. If this parameter is
     *         specified then this method is blocked until process produced by this command is running.
     * @throws IOException
     *         if an i/o error occurs while try exec docker API
     */
    public void exec(LineConsumer output) throws IOException {
        // Generate name of pid-file that will store pid of shell process.
        final String pidFilePath = NameGenerator.generate("/tmp/codenvy-docker-", ".pid", 8);
        // Trap is invoked when bash session ends. Here we kill all subprocesses of shell and remove pid-file.
        final String trap = String.format("trap '[ -z \"$(jobs -p)\" ] || kill $(jobs -p); [ -e %1$s ] && rm %1$s' EXIT", pidFilePath);
        // 'echo' saves shell pid in file, then run command
        final String bashCommand = trap + "; echo $$>" + pidFilePath + "; " + command;
        final Exec exec = docker.createExec(container, output == null, "/bin/bash", "-c", bashCommand);
        this.pidFilePath = pidFilePath;
        docker.startExec(exec.getId(), new LogMessagePrinter(output));
    }

    /**
     * Checks is process produces by this command is running or not.
     *
     * @return {@code true} if process running and {@code false} otherwise
     * @throws IOException
     *         if an i/o error occurs while try exec docker API
     */
    public boolean isAlive() throws IOException {
        if (pidFilePath == null) {
            return false;
        }
        // Read pid from file and run 'kill -0 [pid]' command.
        final String isAliveCmd = String.format("[ -r %1$s ] && kill -0 $(<%1$s) || echo 'Unable read PID file'", pidFilePath);
        final Exec exec = docker.createExec(container, false, "/bin/bash", "-c", isAliveCmd);
        final ListLineConsumer output = new ListLineConsumer();
        docker.startExec(exec.getId(), new LogMessagePrinter(output));
        // 'kill -0 [pid]' is silent if process is running or print "No such process" message otherwise
        return output.getText().isEmpty();
    }

    /**
     * Kills process produces by this command.
     *
     * @throws IOException
     *         if an i/o error occurs while try exec docker API
     */
    public void kill() throws IOException {
        if (pidFilePath != null) {
            // Read pid from file and run 'kill [pid]' command.
            final String killCmd = String.format("[ -r %1$s ] && kill $(<%1$s)", pidFilePath);
            final Exec exec = docker.createExec(container, true, "/bin/bash", "-c", killCmd);
            docker.startExec(exec.getId(), null);
        }
    }
}
