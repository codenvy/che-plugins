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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.internal.ResourceAllocators;
import org.eclipse.che.docker.AuthConfigs;
import org.eclipse.che.docker.DockerConnector;
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
