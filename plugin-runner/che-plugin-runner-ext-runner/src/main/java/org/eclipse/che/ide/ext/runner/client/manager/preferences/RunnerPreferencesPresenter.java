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
package org.eclipse.che.ide.ext.runner.client.manager.preferences;

import org.eclipse.che.api.account.gwt.client.AccountServiceClient;
import org.eclipse.che.api.account.shared.dto.UpdateResourcesDescriptor;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.AbstractPreferencePagePresenter;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.*;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;

/**
 * The presenter for managing user's runners settings,.
 *
 * @author Ann Shumilova
 */
@Singleton
public class RunnerPreferencesPresenter extends AbstractPreferencePagePresenter implements RunnerPreferencesView.ActionDelegate {
    private AppContext              appContext;
    private final DtoFactory        dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private NotificationManager     notificationManager;
    private final RunnerPreferencesView view;
    private final AccountServiceClient accountService;
    private final WorkspaceServiceClient workspaceService;
    private Shutdown shutdown;

    /** Create presenter. */
    @Inject
    public RunnerPreferencesPresenter(RunnerPreferencesView view,
                                      RunnerLocalizationConstant locale,
                                  AppContext appContext,
                                  NotificationManager notificationManager,
                                  WorkspaceServiceClient workspaceService,
                                  DtoFactory dtoFactory,
                                  DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                  AccountServiceClient accountService) {
        super(locale.userPreferencesRunnersShutdownPolicy(), locale.userPreferencesRunnersTitle(), null);
        this.view = view;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.accountService = accountService;
        this.workspaceService = workspaceService;
        this.view.setDelegate(this);
        this.notificationManager = notificationManager;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        view.enableSetButton(false);
        if (appContext.getWorkspace().getAttributes().containsKey(Constants.RUNNER_LIFETIME)) {
           String value = appContext.getWorkspace().getAttributes().get(Constants.RUNNER_LIFETIME);
           shutdown = Shutdown.detect(Integer.parseInt(value));
           if (shutdown != null) {
               view.selectShutdown(shutdown);
           } else {
               shutdown = Shutdown.BY_TIMEOUT_4;
               view.selectShutdown(shutdown);
           }
       } else {
            shutdown = Shutdown.BY_TIMEOUT_4;
            view.selectShutdown(shutdown);
        }
    }

    @Override
    public void storeChanges() {
    }

    @Override
    public void revertChanges() {
        if (appContext.getWorkspace().getAttributes().containsKey(Constants.RUNNER_LIFETIME)) {
            String value = appContext.getWorkspace().getAttributes().get(Constants.RUNNER_LIFETIME);
            shutdown = Shutdown.detect(Integer.parseInt(value));
            if (shutdown != null) {
                view.selectShutdown(shutdown);
            } else {
                shutdown = Shutdown.BY_TIMEOUT_4;
                view.selectShutdown(shutdown);
            }
        }
       view.enableSetButton(false);
    }

    @Override
    public void onValueChanged() {
        view.enableSetButton(!view.getShutdown().equals(shutdown));
    }

    @Override
    public void onSetShutdownClicked() {
        /*TODO when https://jira.codenvycorp.com/browse/CLDIDE-2354 is fixed */
        UpdateResourcesDescriptor updateResourcesDescriptor = dtoFactory.createDto(UpdateResourcesDescriptor.class)
                .withWorkspaceId(appContext.getWorkspace().getId())
                .withRunnerTimeout(view.getShutdown().getTimeout())
                .withBuilderTimeout(view.getShutdown().getTimeout())
                .withRunnerRam(RAM.MB_4096.getValue())
                .withResourcesUsageLimit(2.0);

        accountService.redistributeResources(appContext.getWorkspace().getAccountId(), Collections.createArray(updateResourcesDescriptor), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                shutdown = view.getShutdown();
                view.enableSetButton(false);
                loadWorkspace();
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), Notification.Type.ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    private void loadWorkspace() {
        workspaceService.getWorkspace(appContext.getWorkspace().getId(),
                new AsyncRequestCallback<WorkspaceDescriptor>(
                        dtoUnmarshallerFactory.newUnmarshaller(WorkspaceDescriptor.class)) {
                    @Override
                    protected void onSuccess(WorkspaceDescriptor result) {
                        appContext.getWorkspace().setAttributes(result.getAttributes());
                    }

                    @Override
                    protected void onFailure(Throwable throwable) {
                    }
                }
        );
    }
}