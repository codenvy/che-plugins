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
package org.eclipse.che.ide.ext.git.client.status;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.api.git.shared.StatusFormat.LONG;

/**
 * Handler to process actions with displaying the status of the Git work tree.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class StatusCommandPresenter {

    private boolean isViewClosed = true;

    private WorkspaceAgent          workspaceAgent;
    private GitServiceClient        service;
    private AppContext              appContext;
    private GitLocalizationConstant constant;
    private GitOutputPartPresenter  console;
    private NotificationManager     notificationManager;

    /**
     * Create presenter.
     *
     * @param service
     * @param appContext
     * @param console
     * @param constant
     * @param notificationManager
     */
    @Inject
    public StatusCommandPresenter(final WorkspaceAgent workspaceAgent,
                                  GitServiceClient service,
                                  EventBus eventBus,
                                  AppContext appContext,
                                  final GitOutputPartPresenter console,
                                  GitLocalizationConstant constant,
                                  NotificationManager notificationManager) {
        this.workspaceAgent = workspaceAgent;
        this.service = service;
        this.appContext = appContext;
        this.console = console;
        this.constant = constant;
        this.notificationManager = notificationManager;

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectReady(ProjectActionEvent event) {
            }

            @Override
            public void onProjectOpened(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                isViewClosed = true;
                console.clear();
                workspaceAgent.hidePart(console);
            }
        });
    }

    /** Show status. */
    public void showStatus() {
        CurrentProject project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        service.statusText(project.getRootProject(), LONG,
                           new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                               @Override
                               protected void onSuccess(String result) {
                                   printGitStatus(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.statusFailed();
                                   Notification notification = new Notification(errorMessage, ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           });
    }

    /**
     * Print colored Git status to Output
     *
     * @param statusText text to be printed
     */
    private void printGitStatus(String statusText) {
        if (isViewClosed) {
            workspaceAgent.openPart(console, PartStackType.INFORMATION);
            isViewClosed = false;
        }

        console.print("");
        String[] lines = statusText.split("\n");
        for (String line : lines) {

            if (line.startsWith("\tmodified:") || line.startsWith("#\tmodified:")) {
                console.printError(line);
                continue;
            }

            if (line.startsWith("\t") || line.startsWith("#\t")) {
                console.printInfo(line);
                continue;
            }

            console.print(line);
        }
    }

}
