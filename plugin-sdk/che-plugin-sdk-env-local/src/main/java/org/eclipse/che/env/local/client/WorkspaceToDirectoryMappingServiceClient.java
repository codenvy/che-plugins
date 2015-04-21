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

import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.Map;

/**
 * @author Vitaly Parfonov
 */
public interface WorkspaceToDirectoryMappingServiceClient {


    public void setMountPath(String workspaceId, String mountPath, AsyncRequestCallback<Map<String, String>> callback);

    public void removeMountPath(String workspaceId, AsyncRequestCallback<Map<String, String>> callback);

    public void getDirectoryMapping(AsyncRequestCallback<Map<String, String>> callback);

}
