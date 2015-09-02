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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;

import java.util.LinkedList;
import java.util.Queue;

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
    private final AppContext                 context;
    private final JavaClasspathServiceClient classpathServiceClient;
    private final JavaLocalizationConstant   javaLocalizationConstant;

    private Queue<Pair<ProjectDescriptor, Boolean>> projects = new LinkedList<>();
    private boolean                                 updating = false;
    private Notification notification;

    @Inject
    public DependenciesUpdater(JavaLocalizationConstant javaLocalizationConstant,
                               NotificationManager notificationManager,
                               AppContext context,
                               JavaClasspathServiceClient classpathServiceClient) {
        this.javaLocalizationConstant = javaLocalizationConstant;
        this.notificationManager = notificationManager;
        this.context = context;
        this.classpathServiceClient = classpathServiceClient;
    }

    public void updateDependencies(final ProjectDescriptor project, final boolean force) {
        if (updating) {
            projects.add(new Pair<>(project, force));
            return;
        }

        final CurrentProject currentProject = context.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        notification = new Notification(javaLocalizationConstant.updatingDependencies(), PROGRESS);
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
    }

    private void updateFinishedWithError(Throwable exception, Notification notification) {
        notification.setMessage(exception.getMessage());
        notification.setType(ERROR);
        notification.setStatus(FINISHED);
    }
}
