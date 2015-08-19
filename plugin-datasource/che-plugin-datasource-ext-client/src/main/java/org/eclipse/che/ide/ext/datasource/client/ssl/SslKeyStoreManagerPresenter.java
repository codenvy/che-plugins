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
package org.eclipse.che.ide.ext.datasource.client.ssl;

import javax.validation.constraints.NotNull;

import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.Notification.Type;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.AbstractPreferencePagePresenter;
import org.eclipse.che.ide.commons.exception.ExceptionThrownEvent;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslKeyDialogPresenter;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslTrustCertDialogPresenter;
import org.eclipse.che.ide.ext.datasource.shared.ssl.SslKeyStoreEntry;

import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import java.util.List;

@Singleton
public class SslKeyStoreManagerPresenter extends AbstractPreferencePagePresenter implements SslKeyStoreManagerView.ActionDelegate {
    private final DtoUnmarshallerFactory            dtoUnmarshallerFactory;
    private       SslKeyStoreManagerView            view;
    private       SslKeyStoreClientService          service;
    private       SslMessages                       constant;
    private       EventBus                          eventBus;
    private       UploadSslKeyDialogPresenter       uploadSshKeyPresenter;
    private       UploadSslTrustCertDialogPresenter uploadSshServerCertPresenter;
    private       NotificationManager               notificationManager;

    @Inject
    public SslKeyStoreManagerPresenter(SslKeyStoreManagerView view,
                                       SslKeyStoreClientService service,
                                       SslResources resources,
                                       SslMessages constant,
                                       EventBus eventBus,
                                       UploadSslKeyDialogPresenter uploadSshKeyPresenter,
                                       UploadSslTrustCertDialogPresenter uploadSshServerCertPresenter,
                                       NotificationManager notificationManager,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(constant.sslManagerTitle(), constant.sslManagerCategory(), resources.sshKeyManager());

        this.view = view;
        this.uploadSshServerCertPresenter = uploadSshServerCertPresenter;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.eventBus = eventBus;
        this.uploadSshKeyPresenter = uploadSshKeyPresenter;
        this.notificationManager = notificationManager;
    }

    /** {@inheritDoc} */
    @Override
    public void onClientKeyDeleteClicked(@NotNull SslKeyStoreEntry key) {
        boolean needToDelete = Window.confirm(constant.deleteSslKeyQuestion(key.getAlias()));
        if (needToDelete) {
            service.deleteClientKey(key, new AsyncRequestCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    refreshClientKeys();
                }

                @Override
                public void onFailure(Throwable exception) {
                    Notification notification = new Notification(exception.getMessage(), Type.ERROR);
                    notificationManager.showNotification(notification);
                    eventBus.fireEvent(new ExceptionThrownEvent(exception));
                }
            });
        }
    }

    protected void refreshClientKeys() {
        service.getAllClientKeys(
                new AsyncRequestCallback<List<SslKeyStoreEntry>>(dtoUnmarshallerFactory.newListUnmarshaller(SslKeyStoreEntry.class)) {
                    @Override
                    public void onSuccess(List<SslKeyStoreEntry> result) {
                        view.setClientKeys(result);
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        Notification notification = new Notification(exception.getMessage(), Notification.Type.ERROR);
                        notificationManager.showNotification(notification);
                        eventBus.fireEvent(new ExceptionThrownEvent(exception));
                    }
                });
    }

    /** {@inheritDoc} */
    @Override
    public void onClientKeyUploadClicked() {
        uploadSshKeyPresenter.showDialog(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                refreshClientKeys();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(SslKeyStoreManagerPresenter.class, "Failed showing dialog", caught);
            }
        });
    }

    @Override
    public void onServerCertDeleteClicked(SslKeyStoreEntry key) {
        boolean needToDelete = Window.confirm(constant.deleteSslKeyQuestion(key.getAlias()));
        if (needToDelete) {
            service.deleteServerCert(key, new AsyncRequestCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    refreshServerCerts();
                }

                @Override
                public void onFailure(Throwable exception) {
                    Notification notification = new Notification(exception.getMessage(), Type.ERROR);
                    notificationManager.showNotification(notification);
                    eventBus.fireEvent(new ExceptionThrownEvent(exception));
                }
            });
        }
    }

    protected void refreshServerCerts() {
        service.getAllServerCerts(
                new AsyncRequestCallback<List<SslKeyStoreEntry>>(dtoUnmarshallerFactory.newListUnmarshaller(SslKeyStoreEntry.class)) {
                    @Override
                    public void onSuccess(List<SslKeyStoreEntry> result) {
                        view.setServerCerts(result);
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        Notification notification = new Notification(exception.getMessage(), Notification.Type.ERROR);
                        notificationManager.showNotification(notification);
                        eventBus.fireEvent(new ExceptionThrownEvent(exception));
                    }
                });
    }

    @Override
    public void onServerCertUploadClicked() {
        uploadSshServerCertPresenter.showDialog(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                refreshServerCerts();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(SslKeyStoreManagerPresenter.class, "Failed showing dialog", caught);
            }
        });
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void go(AcceptsOneWidget container) {
        refreshClientKeys();
        refreshServerCerts();
        container.setWidget(view);
    }

    @Override
    public void storeChanges() {

    }

    @Override
    public void revertChanges() {

    }
}
