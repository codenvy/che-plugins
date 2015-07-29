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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.ResourceAllocators;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.Pair;

import com.codenvy.docker.AuthConfig;
import com.codenvy.docker.AuthConfigs;
import com.codenvy.docker.DockerConnector;
import com.codenvy.docker.InitialAuthConfig;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@Singleton
public class DockerRunner extends BaseDockerRunner {

    private final String            apiEndPoint;
    private final InitialAuthConfig initialAuthConfig;
    private static final String AUTH_PREFERENCE_NAME = "codenvy:dockerCredentials";

    @Inject
    public DockerRunner(@Named(Constants.DEPLOY_DIRECTORY) java.io.File deployDirectoryRoot,
                        @Named(Constants.APP_CLEANUP_TIME) int cleanupTime,
                        @Named(HOST_NAME) String hostName,
                        @Named("api.endpoint") String apiEndpoint,
                        @Nullable @Named(WATCH_UPDATE_OF_PROJECT_TYPES) String[] watchUpdateProjectTypes,
                        ResourceAllocators allocators,
                        CustomPortService portService,
                        InitialAuthConfig initialAuthConfig,
                        DockerConnector dockerConnector,
                        EventService eventService,
                        ApplicationLinksGenerator applicationLinksGenerator) {
        super(deployDirectoryRoot,
              cleanupTime,
              hostName,
              watchUpdateProjectTypes == null ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(watchUpdateProjectTypes)),
              allocators,
              portService,
              dockerConnector,
              eventService,
              applicationLinksGenerator);
        this.apiEndPoint = apiEndpoint;
        this.initialAuthConfig = initialAuthConfig;
    }

    public List<RunnerEnvironment> getEnvironments() {
        // Must no appears as 'system' runners.
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "docker";
    }

    @Override
    public String getDescription() {
        return "The linux container runtime";
    }

    @Override
    protected DockerEnvironment getDockerEnvironment(RunRequest request) throws IOException, RunnerException {
        return new CustomDockerEnvironment(request);
    }

    @Override
    protected AuthConfigs getAuthConfigs(RunRequest request) throws IOException, RunnerException {

        AuthConfigs initial = initialAuthConfig.getAuthConfigs();
        AuthConfigs userConfig = null;
        try {
            String response =
                    HttpJsonHelper.requestString(apiEndPoint + "/profile/prefs", "GET", null, Pair.of("token", request.getUserToken()));
            Map<String, String> userPrefs = JsonHelper.fromJson(response, Map.class, new TypeToken<Map<String, String>>() {
            }.getType());
            if (userPrefs.containsKey(AUTH_PREFERENCE_NAME)) {
                userConfig = JsonHelper.fromJson(userPrefs.get(AUTH_PREFERENCE_NAME), AuthConfigs.class, null);
            }
        } catch (ForbiddenException | UnauthorizedException | ServerException un) {
            return null;
        } catch (ConflictException | NotFoundException | JsonParseException e) {
            LOG.warn(e.getLocalizedMessage());
        }

        if (userConfig != null) {
            for (AuthConfig one : userConfig.getConfigs().values()) {
                initial.addConfig(one);
            }
        }
        return initial;
    }
}
