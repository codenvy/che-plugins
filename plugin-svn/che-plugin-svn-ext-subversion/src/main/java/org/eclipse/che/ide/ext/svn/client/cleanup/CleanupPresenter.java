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
package org.eclipse.che.ide.ext.svn.client.cleanup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.PathTypeFilter;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Handler for the {@link org.eclipse.che.ide.ext.svn.client.action.CleanupAction} action.
 */
@Singleton
public class CleanupPresenter extends SubversionActionPresenter {

    private final DtoUnmarshallerFactory                   dtoUnmarshallerFactory;
    private final NotificationManager                      notificationManager;
    private final SubversionClientService                  service;
    private final SubversionExtensionLocalizationConstants constants;

    /**
     * Constructor.
     */
    @Inject
    protected CleanupPresenter(final AppContext appContext,
                               final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               final EventBus eventBus,
                               final NotificationManager notificationManager,
                               final RawOutputPresenter console,
                               final SubversionExtensionLocalizationConstants constants,
                               final SubversionClientService service,
                               final WorkspaceAgent workspaceAgent,
                               final NewProjectExplorerPresenter projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);

        this.service = service;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.constants = constants;
    }

    public void cleanup() {

        final String projectPath = getCurrentProjectPath();
        if (projectPath == null) {
            return;
        }

        final List<PathTypeFilter> filters = new ArrayList<>();
        filters.add(PathTypeFilter.FOLDER);
        filters.add(PathTypeFilter.PROJECT);
        final List<String> selectedPaths = getSelectedPaths(filters);

        this.service.cleanup(projectPath, selectedPaths,
                new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {
            @Override
            protected void onSuccess(final CLIOutputResponse result) {

                printResponse(result.getCommand(), result.getOutput(), result.getErrOutput());

                final Notification notification = new Notification(constants.cleanupSuccessful(), INFO);
                notificationManager.showNotification(notification);
            }

            @Override
            protected void onFailure(final Throwable exception) {
                final String errorMessage = exception.getMessage();
                final Notification notification = new Notification(constants.cleanupFailed() + ": " + errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

}
