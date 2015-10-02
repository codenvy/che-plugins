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

import com.google.web.bindery.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.java.client.event.DependencyUpdatedEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.project.node.jar.ExternalLibrariesNode;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.loging.Log;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Updates dependencies for Maven project.
 *
 * @author Artem Zatsarynnyy
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class DependenciesUpdater {
    private final NotificationManager        notificationManager;
    private final AppContext                 appContext;
    private final JavaClasspathServiceClient classpathServiceClient;
    private final JavaLocalizationConstant   javaLocalizationConstant;

    private       boolean                  updating;
    private       Notification             notification;
    private       EventBus                 eventBus;
    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public DependenciesUpdater(JavaLocalizationConstant javaLocalizationConstant,
                               NotificationManager notificationManager,
                               AppContext appContext,
                               JavaClasspathServiceClient classpathServiceClient,
                               EventBus eventBus,
                               ProjectExplorerPresenter projectExplorer) {
        this.javaLocalizationConstant = javaLocalizationConstant;
        this.notificationManager = notificationManager;
        this.appContext = appContext;
        this.classpathServiceClient = classpathServiceClient;
        this.eventBus = eventBus;
        this.projectExplorer = projectExplorer;
    }

    public void updateDependencies(final ProjectDescriptor project) {
        if (updating) {
            return;
        }

        if (appContext.getCurrentProject() == null) {
            return;
        }

        notification = new Notification(javaLocalizationConstant.updatingDependencies(), PROGRESS, true);
        notificationManager.showNotification(notification);
        updating = true;

        classpathServiceClient.updateDependencies(
                project.getPath(), new AsyncRequestCallback<Boolean>() {
                    @Override
                    protected void onSuccess(Boolean descriptor) {
                        onUpdated();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        Log.warn(DependenciesUpdater.class, "Failed to launch build process and get build task descriptor for " + project);
                        updating = false;
                        updateFinishedWithError(exception, notification);
                    }
                });
    }

    private void onUpdated() {
        updating = false;
        notification.setMessage(javaLocalizationConstant.dependenciesSuccessfullyUpdated());
        notification.setStatus(FINISHED);
        projectExplorer.reloadChildrenByType(ExternalLibrariesNode.class);
        eventBus.fireEvent(new DependencyUpdatedEvent());
    }

    private void updateFinishedWithError(Throwable exception, Notification notification) {
        notification.setMessage(exception.getMessage());
        notification.setType(ERROR);
        notification.setStatus(FINISHED);
    }
}
