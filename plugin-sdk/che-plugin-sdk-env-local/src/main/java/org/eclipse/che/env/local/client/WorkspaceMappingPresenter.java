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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.util.loging.Log;

import java.util.Map;

/**
 * The presenter for managing user's runners settings,.
 *
 * @author Vitalii Parfonov
 */
@Singleton
public class WorkspaceMappingPresenter {
    private final AppContext                               appContext;
    private final NotificationManager                      notificationManager;
    private final EventBus                                 eventBus;
    private final DialogFactory                            dialogFactory;
    private final WorkspaceToDirectoryMappingServiceClient service;

    private String rootFolder;


    /** Create presenter. */
    @Inject
    public WorkspaceMappingPresenter(DialogFactory dialogFactory,
                                     AppContext appContext,
                                     NotificationManager notificationManager,
                                     WorkspaceToDirectoryMappingServiceClient service,
                                     EventBus eventBus) {
        this.dialogFactory = dialogFactory;
        this.service = service;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.eventBus = eventBus;

        init();
    }


    public void init() {
        service.getDirectoryMapping(new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
            @Override
            public void onSuccess(Map<String, String> result) {
                if (result == null || result.isEmpty()) {
                    final String wsId = appContext.getWorkspace().getId();
                    getSetupDialog(wsId, "").show();
                }
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.error(WorkspaceMappingPresenter.class, exception.getMessage());
                notificationManager.showError(exception.getMessage());
            }
        });
    }

    public void showDialog() {
        service.getDirectoryMapping(new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
            @Override
            public void onSuccess(Map<String, String> result) {
                if (result == null && result.isEmpty()) {
                    return;
                }
                Map.Entry<String, String> entry = result.entrySet().iterator().next();
                final String wsId = entry.getKey();
                final String path = entry.getValue();
                getSetupDialog(wsId, path).show();
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.error(WorkspaceMappingPresenter.class, exception.getMessage());
                notificationManager.showError(exception.getMessage());
            }
        });
    }

    public String getRootFolder() {
        return rootFolder;
    }


    private InputDialog getSetupDialog(final String workspaceId, final String path) {
        InputDialog inputDialog = dialogFactory.createInputDialog("Workspace", "Workspace", path, 0, 0, new InputCallback() {
            @Override
            public void accepted(final String value) {
                service.setMountPath(workspaceId, value, new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
                    @Override
                    protected void onSuccess(Map<String, String> result) {
                        eventBus.fireEvent(new RefreshProjectTreeEvent());
                        rootFolder = value;
                    }

                    @Override
                    protected void onFailure(Throwable exception) {/* do nothing for now  */}
                });

            }
        }, new CancelCallback() {
            @Override
            public void cancelled() {

            }
        });

        return inputDialog;
    }


}
