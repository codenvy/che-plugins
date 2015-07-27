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
package org.eclipse.che.env.local.client.location;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.env.local.client.WorkspaceToDirectoryMappingServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;
import org.eclipse.che.ide.util.loging.Log;

import java.util.Map;

/**
 * The presenter for managing user's runners settings.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class WorkspaceLocationPresenter implements WorkspaceLocationView.ActionDelegate {
    private static final String DEFAULT_NAME = "__default";

    private final WorkspaceToDirectoryMappingServiceClient service;
    private final AppContext                               appContext;
    private final WorkspaceLocationView                    view;

    private String workspaceLocation;

    @Inject
    public WorkspaceLocationPresenter(WorkspaceLocationView view,
                                      WorkspaceToDirectoryMappingServiceClient service,
                                      AppContext appContext) {
        this.view = view;
        this.service = service;
        this.appContext = appContext;
        this.view.setDelegate(this);
    }

    /** Shows workspace localization window */
    public void show() {
        service.getDirectoryMapping(new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
            @Override
            public void onSuccess(Map<String, String> result) {
                String wsId = appContext.getWorkspace().getId();
                if (result != null && !result.isEmpty()) {
                    workspaceLocation = result.get(DEFAULT_NAME);
                    if (workspaceLocation == null || workspaceLocation.isEmpty()) {
                        workspaceLocation = result.get(wsId);
                    }
                    view.setWorkspaceLocation(workspaceLocation);
                }
                view.showDialog();
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.error(WorkspaceLocationPresenter.class, exception.getMessage());
            }
        });
    }

    /** Shows initialization window with workspace location */
    public void init() {
        service.getDirectoryMapping(new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
            @Override
            public void onSuccess(Map<String, String> result) {
                if (result != null && !result.isEmpty()) {
                    workspaceLocation = result.get(DEFAULT_NAME);
                    if (workspaceLocation == null || workspaceLocation.isEmpty()) {
                        return;
                    }
                    view.setWorkspaceLocation(workspaceLocation);
                }
                view.showDialog();
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.error(WorkspaceLocationPresenter.class, exception.getMessage());
            }
        });
    }

}