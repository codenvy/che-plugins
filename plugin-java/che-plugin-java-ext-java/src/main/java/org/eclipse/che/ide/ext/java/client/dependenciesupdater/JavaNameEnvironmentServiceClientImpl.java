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
package org.eclipse.che.ide.ext.java.client.dependenciesupdater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.ide.ext.java.client.JavaExtension;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * Implementation of {@link JavaNameEnvironmentServiceClient}.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class JavaNameEnvironmentServiceClientImpl implements JavaNameEnvironmentServiceClient {
    private final AsyncRequestFactory asyncRequestFactory;

    private final String UPDATE_DEPENDENCIES          = "/update-dependencies-launch-task";
    private final String UPDATE_DEPENDENCIES_AND_WAIT = "/update-dependencies-wait-build-end";
    private final String SERVICE_PATH;

    @Inject
    protected JavaNameEnvironmentServiceClientImpl(@Named("workspaceId") String workspaceId, AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        SERVICE_PATH = JavaExtension.getJavaCAPath() + "/java-name-environment/" + workspaceId;
    }

    /** {@inheritDoc} */
    @Override
    public void updateDependencies(String projectPath, boolean force, AsyncRequestCallback<BuildTaskDescriptor> callback) {
        final String requestUrl = SERVICE_PATH + UPDATE_DEPENDENCIES + "?projectpath=" + projectPath + "&force=" + force;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void updateDependenciesAndWait(String projectPath,
                                          BuildTaskDescriptor buildTaskDescriptor,
                                          AsyncRequestCallback<Void> callback) {
        final String requestUrl = SERVICE_PATH + UPDATE_DEPENDENCIES_AND_WAIT + "?projectpath=" + projectPath;
        asyncRequestFactory.createPostRequest(requestUrl, buildTaskDescriptor, true)
                           .send(callback);
    }
}
