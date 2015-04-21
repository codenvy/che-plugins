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
package org.eclipse.che.env.local.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;

import java.util.Map;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class WorkspaceToDirectoryMappingServiceClientImpl implements WorkspaceToDirectoryMappingServiceClient {

    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    public WorkspaceToDirectoryMappingServiceClientImpl(AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
    }

    @Override
    public void setMountPath(String workspaceId, String mountPath, AsyncRequestCallback<Map<String, String>> callback) {
        final String requestUrl = "/api/vfs-directory-mapping/" + workspaceId + "?mountPath=" + mountPath;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void removeMountPath(String workspaceId, AsyncRequestCallback<Map<String, String>> callback) {
        final String requestUrl = "/api/vfs-directory-mapping/" + workspaceId;
        asyncRequestFactory.createDeleteRequest(requestUrl)
                           .header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);

    }

    @Override
    public void getDirectoryMapping(AsyncRequestCallback<Map<String, String>> callback) {
        final String requestUrl = "/api/vfs-directory-mapping";
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);

    }
}
