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
package org.eclipse.che.ide.ext.git.client.url;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringUnmarshaller;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Presenter for showing git url.
 *
 * @author Ann Zhuleva
 * @author Oleksii Orel
 */
@Singleton
public class ShowProjectGitReadOnlyUrlPresenter implements ShowProjectGitReadOnlyUrlView.ActionDelegate {
    private final DtoUnmarshallerFactory        dtoUnmarshallerFactory;
    private       ShowProjectGitReadOnlyUrlView view;
    private       GitServiceClient              service;
    private       AppContext                    appContext;
    private       GitLocalizationConstant       constant;
    private       NotificationManager           notificationManager;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param appContext
     * @param constant
     * @param notificationManager
     */
    @Inject
    public ShowProjectGitReadOnlyUrlPresenter(ShowProjectGitReadOnlyUrlView view, GitServiceClient service,
                                              AppContext appContext, GitLocalizationConstant constant,
                                              NotificationManager notificationManager, DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        final CurrentProject project = appContext.getCurrentProject();
        view.showDialog();
        service.remoteList(project.getRootProject(), null, true,
                           new AsyncRequestCallback<Array<Remote>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Remote.class)) {
                               @Override
                               protected void onSuccess(Array<Remote> result) {
                                   view.setRemotes(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   view.setRemotes(null);
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage()
                                                                          : constant.remoteListFailed();
                                   Notification notification = new Notification(errorMessage, ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           }
                          );
        service.getGitReadOnlyUrl(project.getRootProject(),
                                  new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                                      @Override
                                      protected void onSuccess(String result) {
                                          view.setLocaleUrl(result);
                                      }

                                      @Override
                                      protected void onFailure(Throwable exception) {
                                          String errorMessage = exception.getMessage() != null && !exception.getMessage().isEmpty()
                                                                ? exception.getMessage() : constant.initFailed();
                                          Notification notification = new Notification(errorMessage, ERROR);
                                          notificationManager.showNotification(notification);
                                      }
                                  });
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseClicked() {
        view.close();
    }
}