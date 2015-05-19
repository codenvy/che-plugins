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

package org.eclipse.che.jdt.rest;

import org.eclipse.che.api.core.util.Cancellable;
import org.eclipse.che.api.core.util.CancellableProcessWrapper;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.core.util.StreamPump;
import org.eclipse.che.api.core.util.Watchdog;
import org.eclipse.che.core.internal.resources.ResourcesPlugin;
import org.eclipse.che.ide.maven.tools.MavenUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.che.jdt.maven.MavenClasspathUtil;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Evgen Vidolob
 */
@Path("classpath")
public class JavaClasspathService {
    private static final IClasspathEntry[] EMPTY = new IClasspathEntry[0];
    private static final JavaModel         model = JavaModelManager.getJavaModelManager().getJavaModel();
    private static final Logger            LOG   = LoggerFactory.getLogger(JavaClasspathService.class);

    @GET
    @Path("update")
    public boolean update(@QueryParam("projectpath") final String projectPath) {
        IJavaProject javaProject = model.getJavaProject(projectPath);
        File dir = new File(ResourcesPlugin.getPathToWorkspace() + projectPath);
        boolean succes = generateClaspath(projectPath, dir);
        if (succes) {
            try {
                IClasspathContainer container = MavenClasspathUtil.readMavenClasspath(javaProject);
                JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[]{javaProject},
                                                                  new IClasspathContainer[]{container}, null);
            } catch (JavaModelException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return succes;

    }



    private boolean generateClaspath(final String projectPath, File dir) {
        StreamPump output = null;
        Watchdog watcher = null;
        int timeout = 10; //10 minutes
        int result = -1;
        String command = MavenUtils.getMavenExecCommand();
        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder().command(command, "dependency:build-classpath", "-Dmdep.outputFile=.codenvy/classpath.maven").directory(dir).redirectErrorStream(true);
            Process process = processBuilder.start();

            if (timeout > 0) {
                watcher = new Watchdog("Maven classpath" + "-WATCHDOG", timeout, TimeUnit.MINUTES);
                watcher.start(new CancellableProcessWrapper(process, new Cancellable.Callback() {
                    @Override
                    public void cancelled(Cancellable cancellable) {
                            LOG.warn("Your build has been shutdown due to timeout. Project: " + projectPath);
                    }
                }));
            }
            output = new StreamPump();
            output.start(process, LineConsumer.DEV_NULL);
            try {
                result = process.waitFor();
            } catch (InterruptedException e) {
                Thread.interrupted(); // we interrupt thread when cancel task
                ProcessUtil.kill(process);
            }
            try {
                output.await(); // wait for logger
            } catch (InterruptedException e) {
                Thread.interrupted(); // we interrupt thread when cancel task, NOTE: logs may be incomplete
            }
        } catch (IOException e) {
            LOG.error("", e);
        } finally {
            if (watcher != null) {
                watcher.stop();
            }
            if (output != null) {
                output.stop();
            }
        }
        return result == 0;
    }
}
