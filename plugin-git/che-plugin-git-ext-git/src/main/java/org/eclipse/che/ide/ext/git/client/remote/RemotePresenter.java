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
package org.eclipse.che.ide.ext.git.client.remote;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.git.client.remote.add.AddRemoteRepositoryPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;

import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Presenter for working with remote repository list (view, add and delete).
 *
 * @author Ann Zhuleva
 */
@Singleton
public class RemotePresenter implements RemoteView.ActionDelegate {
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;

    private RemoteView                   view;
    private GitServiceClient             service;
    private AppContext                   appContext;
    private GitLocalizationConstant      constant;
    private AddRemoteRepositoryPresenter addRemoteRepositoryPresenter;
    private NotificationManager          notificationManager;
    private Remote                       selectedRemote;
    private ProjectDescriptor            project;

    @Inject
    public RemotePresenter(RemoteView view, GitServiceClient service, AppContext appContext, GitLocalizationConstant constant,
                           AddRemoteRepositoryPresenter addRemoteRepositoryPresenter, NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.view = view;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.addRemoteRepositoryPresenter = addRemoteRepositoryPresenter;
        this.notificationManager = notificationManager;
    }

    /**
     * Show dialog.
     */
    public void showDialog() {
        project = appContext.getCurrentProject().getRootProject();
        getRemotes();
    }

    /**
     * Get the list of remote repositories for local one. If remote repositories are found,
     * then get the list of branches (remote and local).
     */
    private void getRemotes() {
        service.remoteList(project, null, true,
                           new AsyncRequestCallback<List<Remote>>(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class)) {
                               @Override
                               protected void onSuccess(List<Remote> result) {
                                   view.setEnableDeleteButton(selectedRemote != null);
                                   view.setRemotes(result);
                                   if (!view.isShown()) {
                                       view.showDialog();
                                   }
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage() : constant.remoteListFailed();
                                   handleError(errorMessage);
                               }
                           });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCloseClicked() {
        view.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAddClicked() {
        addRemoteRepositoryPresenter.showDialog(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                getRemotes();
            }

            @Override
            public void onFailure(Throwable caught) {
                String errorMessage = caught.getMessage() != null ? caught.getMessage() : constant.remoteAddFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDeleteClicked() {
        if (selectedRemote == null) {
            handleError(constant.selectRemoteRepositoryFail());
            return;
        }

        final String name = selectedRemote.getName();
        service.remoteDelete(project, name, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getRemotes();
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.remoteDeleteFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoteSelected(@Nonnull Remote remote) {
        selectedRemote = remote;
        view.setEnableDeleteButton(selectedRemote != null);
    }

    private void handleError(@Nonnull String errorMessage) {
        Notification notification = new Notification(errorMessage, ERROR);
        notificationManager.showNotification(notification);
    }
}
