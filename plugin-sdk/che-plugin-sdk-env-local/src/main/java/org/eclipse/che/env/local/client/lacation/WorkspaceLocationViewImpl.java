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
package org.eclipse.che.env.local.client.lacation;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.env.local.client.LocalizationConstant;
import org.eclipse.che.env.local.client.WorkspaceToDirectoryMappingServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Valeriy Svydenko
 */
public class WorkspaceLocationViewImpl extends Window implements WorkspaceLocationView {
    private final WorkspaceToDirectoryMappingServiceClient service;
    private final AppContext                               appContext;
    private final EventBus                                 eventBus;
    private final DialogFactory                            dialogFactory;
    private final NotificationManager                      notificationManager;

    @UiField
    TextBox workspaceLocation;

    interface WorkspaceLocationViewImplUiBinder extends UiBinder<DockLayoutPanel, WorkspaceLocationViewImpl> {
    }

    @Inject
    public WorkspaceLocationViewImpl(WorkspaceLocationViewImplUiBinder uiBinder,
                                     LocalizationConstant localizationConstant,
                                     WorkspaceToDirectoryMappingServiceClient service,
                                     DialogFactory dialogFactory,
                                     AppContext appContext,
                                     EventBus eventBus,
                                     NotificationManager notificationManager) {
        this.service = service;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.eventBus = eventBus;
        this.dialogFactory = dialogFactory;
        this.setTitle(localizationConstant.rootFolderDialogTitleChange());

        setWidget(uiBinder.createAndBindUi(this));
        bind();
    }

    /** Bind handlers. */
    private void bind() {
        Button okButton = createButton("Ok", "change-workspace-ok", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                String wsId = appContext.getWorkspace().getId();
                service.setMountPath(wsId,
                                     URL.encode(workspaceLocation.getText()),
                                     new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
                                         @Override
                                         protected void onSuccess(Map<String, String> result) {
                                             eventBus.fireEvent(new RefreshProjectTreeEvent());
                                             closeDialog();
                                         }

                                         @Override
                                         protected void onFailure(Throwable exception) {
                                             Log.error(WorkspaceLocationViewImpl.class, exception.getMessage());
                                             notificationManager.showError(exception.getMessage());
                                             dialogFactory.createMessageDialog("Workspace not set",
                                                                               JsonHelper.parseJsonMessage(exception.getMessage()),
                                                                               new ConfirmCallback() {
                                                                                   @Override
                                                                                   public void accepted() {
                                                                                   }
                                                                               }).show();
                                         }
                                     });
            }
        });
        getFooter().add(okButton);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        //do nothing
    }

    /** {@inheritDoc} */
    @Override
    protected void onClose() {
        this.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void showDialog() {
        this.show();
    }

    /** {@inheritDoc} */
    @Override
    public void setWorkspaceLocation(@Nonnull String workspaceLocation) {
        this.workspaceLocation.setText(workspaceLocation);
    }

    /** {@inheritDoc} */
    @Override
    public void closeDialog() {
        this.hide();
        this.onClose();
    }
}