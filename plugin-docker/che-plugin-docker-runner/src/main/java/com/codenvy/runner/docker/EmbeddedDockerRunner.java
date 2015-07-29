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
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.internal.ResourceAllocators;

import com.codenvy.docker.AuthConfigs;
import com.codenvy.docker.DockerConnector;
import org.eclipse.che.dto.server.DtoFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Runner based on BaseDockerRunner that uses prepared set of dockerfiles.
 *
 * @author andrew00x
 */
public class EmbeddedDockerRunner extends BaseDockerRunner {
    private final String                                 name;
    private final Map<String, EmbeddedDockerEnvironment> dockerEnvironments;

    EmbeddedDockerRunner(java.io.File deployDirectoryRoot,
                         int cleanupTime,
                         String hostName,
                         String[] watchUpdateProjectTypes,
                         ResourceAllocators allocators,
                         CustomPortService portService,
                         DockerConnector dockerConnector,
                         EventService eventService,
                         ApplicationLinksGenerator applicationLinksGenerator,
                         String name) {
        super(deployDirectoryRoot,
              cleanupTime,
              hostName,
              watchUpdateProjectTypes == null ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(watchUpdateProjectTypes)),
              allocators,
              portService,
              dockerConnector,
              eventService,
              applicationLinksGenerator);
        this.name = name;
        this.dockerEnvironments = new HashMap<>();
    }

    void registerEnvironment(EmbeddedDockerEnvironment env) {
        dockerEnvironments.put(env.getId(), env);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "The linux container runtime";
    }

    @Override
    public List<RunnerEnvironment> getEnvironments() {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final List<RunnerEnvironment> environments = new LinkedList<>();
        for (EmbeddedDockerEnvironment dockerEnvironment : dockerEnvironments.values()) {
            final RunnerEnvironment runnerEnvironment = dtoFactory.createDto(RunnerEnvironment.class)
                                                                  .withId(dockerEnvironment.getId())
                                                                  .withDisplayName(dockerEnvironment.getDisplayName())
                                                                  .withDescription(dockerEnvironment.getDescription());
            environments.add(runnerEnvironment);
        }
        return environments;
    }

    @Override
    protected EmbeddedDockerEnvironment getDockerEnvironment(RunRequest request) throws IOException, RunnerException {
        final EmbeddedDockerEnvironment environment = dockerEnvironments.get(request.getEnvironmentId());
        if (environment == null) {
            throw new RunnerException(String.format("Invalid environment id %s", request.getEnvironmentId()));
        }
        return environment;
    }

    @Override
    protected AuthConfigs getAuthConfigs(RunRequest request) throws IOException, RunnerException {
         return new AuthConfigs();
    }
}
