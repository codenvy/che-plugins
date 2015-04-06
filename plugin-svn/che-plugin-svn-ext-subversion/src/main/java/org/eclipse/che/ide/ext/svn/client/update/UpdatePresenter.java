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
package org.eclipse.che.ide.ext.svn.client.update;

import java.util.ArrayList;

import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputWithRevisionResponse;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Handler for the {@link import org.eclipse.che.ide.ext.svn.client.action.UpdateAction} action.
 */
@Singleton
public class UpdatePresenter extends SubversionActionPresenter {

    private final DtoUnmarshallerFactory                   dtoUnmarshallerFactory;
    private final NotificationManager                      notificationManager;
    private final SubversionClientService                  service;
    private final SubversionExtensionLocalizationConstants constants;

    private Notification notification;

    /**
     * Constructor.
     */
    @Inject
    public UpdatePresenter(final AppContext appContext,
                           final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           final EventBus eventBus,
                           final RawOutputPresenter console,
                           final SubversionClientService service,
                           final WorkspaceAgent workspaceAgent,
                           final SubversionExtensionLocalizationConstants constants,
                           final NotificationManager notificationManager,
                           final ProjectExplorerPart projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);

        this.constants = constants;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.service = service;
    }

    public void showUpdate() {
        doUpdate("HEAD", "infinity", false, null);
    }

    protected void doUpdate(final String revision, final String depth, final boolean ignoreExternals,
                            final UpdateToRevisionView view) {
        final String projectPath = getCurrentProjectPath();
        if (projectPath == null) {
            return;
        }

        notification = new Notification(constants.updateToRevisionStarted(revision), PROGRESS);

        notificationManager.showNotification(notification);

        // TODO: Add UI widget for "Accept" part of update

        service.update(projectPath, getSelectedPaths(), revision, depth, ignoreExternals, "postpone",
                       new AsyncRequestCallback<CLIOutputWithRevisionResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputWithRevisionResponse.class)) {
                           @Override
                           protected void onSuccess(final CLIOutputWithRevisionResponse response) {
                               printResponse(response.getCommand(), response.getOutput(), response.getErrOutput());

                               notification.setMessage(constants.updateSuccessful(Long.toString(response.getRevision())));
                               notification.setStatus(FINISHED);
                               notification.setType(INFO);

                               updateProjectExplorer();

                               if (view != null) {
                                   view.close();
                               }
                           }

                           @Override
                           protected void onFailure(final Throwable exception) {
                               String errorMessage = exception.getMessage();

                               notification.setMessage(constants.updateFailed() + ": " + errorMessage);
                               notification.setStatus(FINISHED);
                               notification.setType(ERROR);
                           }
                       });
    }

}
