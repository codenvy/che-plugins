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
package org.eclipse.che.ide.ext.svn.client.commit;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.commit.CommitView.CommitViewDelegate;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputWithRevisionResponse;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.web.bindery.event.shared.EventBus;

public class CommitPresenter extends SubversionActionPresenter implements CommitViewDelegate {

    private final SubversionClientService subversionService;
    private final CommitView view;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final NotificationManager notificationManager;
    private final SubversionExtensionLocalizationConstants constants;

    @Inject
    public CommitPresenter(final AppContext appContext,
                           final CommitView view,
                           final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           final EventBus eventBus,
                           final NotificationManager notificationManager,
                           final RawOutputPresenter console,
                           final SubversionExtensionLocalizationConstants constants,
                           final SubversionClientService subversionService,
                           final WorkspaceAgent workspaceAgent,
                           final ProjectExplorerPart projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);
        this.subversionService = subversionService;
        this.view = view;
        this.view.setDelegate(this);
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.constants = constants;
    }

    public void show() {
        final String projectPath = getCurrentProjectPath();
        if (projectPath != null) {
            this.view.setMessage("");
            this.view.setEnableCommitButton(false);
            this.view.setCommitSelection(false);
            this.view.setKeepLocksState(false);

            this.view.showDialog();
        }
    }

    @Override
    public void onCancelClicked() {
        this.view.close();
    }

    @Override
    public void onCommitClicked() {
        final String message = view.getMessage();
        final boolean keepLocks = view.getKeepLocksState();
        final boolean selectionFlag = this.view.isCommitSelection();

        if (selectionFlag) {
            commitSelection(message, keepLocks);
        } else {
            commitAll(message, keepLocks);
        }
    }

    @Override
    public void onValueChanged() {
        final String message = view.getMessage();
        this.view.setEnableCommitButton(!message.isEmpty());
    }

    private void commitSelection(final String message, final boolean keepLocks) {
        final List<String> paths = getSelectedPaths();
        doCommit(message, paths, keepLocks);
    }

    private void commitAll(final String message, final boolean keepLocks) {
        doCommit(message, Collections.singletonList("."), keepLocks);
    }

    private void doCommit(final String message, final List<String> paths, final boolean keepLocks) {
        this.subversionService.commit(getCurrentProjectPath(), paths, message, false, keepLocks,
                                      new AsyncRequestCallback<CLIOutputWithRevisionResponse>(
                                              dtoUnmarshallerFactory.newUnmarshaller(CLIOutputWithRevisionResponse.class)) {
                           @Override
                           protected void onSuccess(final CLIOutputWithRevisionResponse result) {
                               printResponse(result.getCommand(), result.getOutput(), result.getErrOutput());
                           }

                           @Override
                           protected void onFailure(final Throwable exception) {
                               handleError(exception);
                           }
                       }
               );
        this.view.close();
    }

    private void handleError(@Nonnull final Throwable e) {
        String errorMessage;
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorMessage = e.getMessage();
        } else {
            errorMessage = constants.commitFailed();
        }
        final Notification notification = new Notification(errorMessage, ERROR);
        this.notificationManager.showNotification(notification);
    }

}
