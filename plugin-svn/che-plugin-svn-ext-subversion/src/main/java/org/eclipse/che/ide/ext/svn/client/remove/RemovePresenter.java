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
package org.eclipse.che.ide.ext.svn.client.remove;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Handler for the {@link org.eclipse.che.ide.ext.svn.client.action.RemoveAction} action.
 */
@Singleton
public class RemovePresenter extends SubversionActionPresenter {

    private final DtoUnmarshallerFactory                   dtoUnmarshallerFactory;
    private final NotificationManager                      notificationManager;
    private final SubversionClientService                  service;
    private final SubversionExtensionLocalizationConstants constants;

    private Notification notification;

    /**
     * Constructor.
     */
    @Inject
    protected RemovePresenter(final AppContext appContext,
                              final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              final EventBus eventBus,
                              final NotificationManager notificationManager,
                              final RawOutputPresenter console,
                              final SubversionExtensionLocalizationConstants constants,
                              final SubversionClientService service,
                              final WorkspaceAgent workspaceAgent,
                              final ProjectExplorerPresenter projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);

        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.constants = constants;
    }

    public void showRemove() {
        final String projectPath = getCurrentProjectPath();
        if (projectPath == null) {
            return;
        }

        final List<String> selectedPaths = getSelectedPaths();
        notification = new Notification(constants.removeStarted(selectedPaths.size()), PROGRESS);
        notificationManager.showNotification(notification);

        service.remove(projectPath, getSelectedPaths(), new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {
            @Override
            protected void onSuccess(final CLIOutputResponse response) {
                printResponse(response.getCommand(), response.getOutput(), response.getErrOutput());

                notification.setMessage(constants.removeSuccessful());
                notification.setStatus(FINISHED);
                notification.setType(INFO);

                updateProjectExplorer();
            }

            @Override
            protected void onFailure(final Throwable exception) {
                String errorMessage = exception.getMessage();

                notification.setMessage(constants.removeFailed() + ": " + errorMessage);
                notification.setStatus(FINISHED);
                notification.setType(ERROR);
            }
        });
    }

}
