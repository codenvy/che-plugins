/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.docker.json;

/**
 * @author Eugene Voevodin
 */
public class ExecInfo {

    private String        id;
    private ContainerInfo container;
    private ProcessConfig processConfig;
    private boolean       openStdout;
    private boolean       openStderr;
    private boolean       openStdin;
    private boolean       running;
    private int           exitCode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ContainerInfo getContainer() {
        return container;
    }

    public void setContainer(ContainerInfo container) {
        this.container = container;
    }

    public ProcessConfig getProcessConfig() {
        return processConfig;
    }

    public void setProcessConfig(ProcessConfig processConfig) {
        this.processConfig = processConfig;
    }

    public boolean isOpenStdout() {
        return openStdout;
    }

    public void setOpenStdout(boolean openStdout) {
        this.openStdout = openStdout;
    }

    public boolean isOpenStderr() {
        return openStderr;
    }

    public void setOpenStderr(boolean openStderr) {
        this.openStderr = openStderr;
    }

    public boolean isOpenStdin() {
        return openStdin;
    }

    public void setOpenStdin(boolean openStdin) {
        this.openStdin = openStdin;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public String toString() {
        return "ExecInfo{" +
               "id='" + id + '\'' +
               ", container=" + container +
               ", processConfig=" + processConfig +
               ", openStdout=" + openStdout +
               ", openStderr=" + openStderr +
               ", openStdin=" + openStdin +
               ", running=" + running +
               ", exitCode=" + exitCode +
               '}';
    }
}
