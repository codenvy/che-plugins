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
package org.eclipse.che.ide.ext.svn.client.askcredentials;

import javax.inject.Inject;

import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.Notification.Status;
import org.eclipse.che.ide.api.notification.Notification.Type;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.askcredentials.AskCredentialsView.AskCredentialsDelegate;
import org.eclipse.che.ide.rest.AsyncRequestCallback;


public class AskCredentialsPresenter implements AskCredentialsDelegate {

    private final AskCredentialsView view;
    private final NotificationManager notificationManager;
    private final SubversionExtensionLocalizationConstants constants;
    private final SubversionClientService clientService;
    private String repositoryUrl;

    @Inject
    public AskCredentialsPresenter(final AskCredentialsView view,
                                   final NotificationManager notificationManager,
                                   final SubversionExtensionLocalizationConstants constants,
                                   final SubversionClientService clientService) {
        this.notificationManager = notificationManager;
        this.constants = constants;
        this.view = view;
        this.view.setDelegate(this);
        this.clientService = clientService;
    }

    @Override
    public void onSaveClicked() {
        saveCredentials(this.view.getUsername(), this.view.getPassword());
        this.view.clearUsername();
        this.view.clearPassword();
        this.view.close();
    }

    @Override
    public void onCancelClicked() {
        this.view.clearUsername();
        this.view.clearPassword();
        this.view.close();
    }

    public void askCredentials(final String repositoryUrl) {
        this.view.clearUsername();
        this.view.clearPassword();
        this.view.setRepositoryUrl(repositoryUrl);
        this.repositoryUrl = repositoryUrl;
        this.view.showDialog();
    }

    private void saveCredentials(final String username, final String password) {
        final Notification notification = new Notification(constants.notificationSavingCredentials(repositoryUrl), Status.PROGRESS);
        this.notificationManager.showNotification(notification);
        this.clientService.saveCredentials(this.repositoryUrl, username, password, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(final Void notUsed) {
                notification.setMessage(constants.notificationCredentialsSaved(repositoryUrl));
                notification.setStatus(Status.FINISHED);
            }

            @Override
            protected void onFailure(final Throwable exception) {
                notification.setMessage(constants.notificationCredentialsFailed(repositoryUrl));
                notification.setType(Type.ERROR);
            }
        });
    }
}
