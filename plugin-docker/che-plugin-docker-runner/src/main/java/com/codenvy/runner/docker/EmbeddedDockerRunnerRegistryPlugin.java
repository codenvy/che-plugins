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
package com.codenvy.runner.docker;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.ResourceAllocators;
import org.eclipse.che.api.runner.internal.RunnerRegistry;
import com.codenvy.docker.DockerConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The following directory structure is required:
 * <pre>
 *     ${runner.docker.dockerfiles_repo}/
 *        JavaWeb/
 *            Tomcat7/
 *                Dockerfile
 *                Mapper.json
 *            default/
 *                Dockerfile
 *                Mapper.json
 * </pre>
 * <ul>
 * <li><b>${runner.docker.dockerfiles_repo}</b> - configuration parameter that points to the root directory where docker files for all
 * supported runners and environments are located</li>
 * <li><b>JavaWeb</b> - directory that contains description of environments for running java web application</li>
 * <li><b>Tomcat7</b> - directory that contains description of environment that uses tomcat 7. This directory must contains file
 * <i>Dockerfile</i> and might contain file <i>Mapper.json</i>. File <i>Mapper.json</i> contains additional information that helps us
 * understand how should we tread exposed ports and bonded volumes if any.
 *
 * @author andrew00x
 */
@Singleton
public class EmbeddedDockerRunnerRegistryPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedDockerRunnerRegistryPlugin.class);

    public static final String DOCKERFILES_REPO = "runner.docker.dockerfiles_repo";

    private final List<EmbeddedDockerRunner> myRunners;
    private final RunnerRegistry             registry;

    @Inject
    public EmbeddedDockerRunnerRegistryPlugin(RunnerRegistry registry,
                                              @Named(Constants.DEPLOY_DIRECTORY) File deployDirectoryRoot,
                                              @Named(Constants.APP_CLEANUP_TIME) int cleanupTime,
                                              @Named(BaseDockerRunner.HOST_NAME) String hostName,
                                              @Nullable @Named(
                                                      BaseDockerRunner.WATCH_UPDATE_OF_PROJECT_TYPES) String[] watchUpdateProjectTypes,
                                              ResourceAllocators allocators,
                                              CustomPortService portService,
                                              DockerConnector dockerConnector,
                                              EventService eventService,
                                              ApplicationLinksGenerator applicationLinksGenerator,
                                              @Nullable @Named(DOCKERFILES_REPO) String dockerfilesRepository) {
        this.registry = registry;
        this.myRunners = new LinkedList<>();
        File dockerFilesDir = null;
        if (!(dockerfilesRepository == null || dockerfilesRepository.isEmpty())) {
            dockerFilesDir = new File(dockerfilesRepository);
        }
        if (dockerFilesDir == null) {
            final URL dockerFilesUrl = Thread.currentThread().getContextClassLoader().getResource("codenvy/runner/docker");
            if (dockerFilesUrl != null) {
                try {
                    dockerFilesDir = new File(dockerFilesUrl.toURI());
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        if (dockerFilesDir != null && dockerFilesDir.isDirectory()) {
            final Map<String, EmbeddedDockerRunner> runnersMap = new HashMap<>();
            final Path dockerFilesDirPath = dockerFilesDir.toPath();
            for (File environmentDir : findEnvironmentDirectories(dockerFilesDir)) {
                final Path relEnvPath = dockerFilesDirPath.relativize(environmentDir.toPath());
                try {
                    final int nameCount = relEnvPath.getNameCount();
                    final String runner = relEnvPath.subpath(0, nameCount - 1).toString();
                    final String environment = relEnvPath.subpath(nameCount - 1, nameCount).toString();
                    EmbeddedDockerRunner dockerRunner = runnersMap.get(runner);
                    if (dockerRunner == null) {
                        runnersMap.put(runner, dockerRunner = new EmbeddedDockerRunner(deployDirectoryRoot,
                                                                                       cleanupTime,
                                                                                       hostName,
                                                                                       watchUpdateProjectTypes,
                                                                                       allocators,
                                                                                       portService,
                                                                                       dockerConnector,
                                                                                       eventService,
                                                                                       applicationLinksGenerator,
                                                                                       runner));
                    }
                    dockerRunner.registerEnvironment(new EmbeddedDockerEnvironment(environment, environmentDir));
                } catch (RuntimeException e) {
                    LOG.error(String.format("Unable read docker environment from '%s'. Subdirectory '%s' has incorrect structure. ",
                                            environmentDir, relEnvPath));
                }
            }
            this.myRunners.addAll(runnersMap.values());
        }
    }

    @PostConstruct
    private void start() {
        for (EmbeddedDockerRunner runner : myRunners) {
            runner.start();
            registry.add(runner);
        }
    }

    @PreDestroy
    private void stop() {
        for (EmbeddedDockerRunner runner : myRunners) {
            registry.remove(runner.getName());
            runner.stop();
        }
    }

    private final FileFilter dirFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private List<File> findEnvironmentDirectories(File dockerFilesDir) {
        final List<File> runnerEnvironments = new LinkedList<>();
        final LinkedList<File> q = new LinkedList<>();
        if (dockerFilesDir.isDirectory()) {
            q.add(dockerFilesDir);
        }
        while (!q.isEmpty()) {
            File dir = q.poll();
            if (new File(dir, "Dockerfile").isFile()) {
                runnerEnvironments.add(dir);
            } else {
                final File[] dirs = dir.listFiles(dirFilter);
                if (dirs != null) {
                    Collections.addAll(q, dirs);
                }
            }
        }
        return runnerEnvironments;
    }
}
