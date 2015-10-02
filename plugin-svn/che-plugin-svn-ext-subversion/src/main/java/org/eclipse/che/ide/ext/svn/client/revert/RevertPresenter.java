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
package org.eclipse.che.ide.ext.svn.client.revert;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;

import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;
import static org.eclipse.che.ide.api.notification.Notification.Type.WARNING;

public class RevertPresenter extends SubversionActionPresenter {

    private final AppContext                               appContext;
    private final DtoUnmarshallerFactory                   dtoUnmarshallerFactory;
    private final SubversionClientService                  subversionClientService;
    private final NotificationManager                      notificationManager;
    private final SubversionExtensionLocalizationConstants constants;
    private final DialogFactory                            dialogFactory;

    private Notification                                   notification;

    @Inject
    protected RevertPresenter(final AppContext appContext,
                              final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              final EventBus eventBus,
                              final WorkspaceAgent workspaceAgent,
                              final RawOutputPresenter console,
                              final SubversionClientService subversionClientService,
                              final SubversionExtensionLocalizationConstants constants,
                              final NotificationManager notificationManager,
                              final DialogFactory dialogFactory,
                              final ProjectExplorerPresenter projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);
        this.appContext = appContext;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.subversionClientService = subversionClientService;
        this.constants = constants;
        this.notificationManager = notificationManager;
        this.dialogFactory = dialogFactory;
    }

    public void show() {
        CurrentProject project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        List<String> paths = getSelectedPaths();
        ConfirmDialog confirmDialog = createConfirmDialog(project, paths);
        confirmDialog.show();
    }

    private ConfirmDialog createConfirmDialog(final CurrentProject project, final List<String> paths) {
        final ConfirmCallback okCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                notification = new Notification(constants.revertStarted(), PROGRESS);
                notificationManager.showNotification(notification);

                subversionClientService.revert(project.getRootProject().getPath(),
                                               paths,
                                               "infinity",
                                               new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {

                                                   @Override
                                                   protected void onSuccess(CLIOutputResponse result) {

                                                       printCommand(result.getCommand());

                                                       print(result.getOutput());

                                                       List<String> errOutput = result.getErrOutput();
                                                       printAndSpace(errOutput);

                                                       if (errOutput == null || errOutput.size() == 0) {
                                                           notification.setMessage(constants.revertSuccessful());
                                                           notification.setStatus(FINISHED);
                                                           notification.setType(INFO);
                                                       } else {
                                                           notification.setMessage(constants.revertWarning());
                                                           notification.setStatus(FINISHED);
                                                           notification.setType(WARNING);
                                                       }

                                                       updateProjectExplorer();
                                                   }

                                                   @Override
                                                   protected void onFailure(Throwable exception) {
                                                       String errorMessage = exception.getMessage();
                                                       notification.setMessage(constants.revertFailed() + ": " + errorMessage);
                                                       notification.setStatus(FINISHED);
                                                       notification.setType(ERROR);
                                                   }
                                               });
            }
        };

        final CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void cancelled() {

            }
        };

        String pathsString = null;
        for (String path : paths) {
            if (pathsString == null) {
                pathsString = path;
            }
            else {
                pathsString += ", " + path;
            }
        }

        String confirmText = paths.size() > 0 ? constants.revertConfirmText(" to " + pathsString) : constants.revertConfirmText("");
        return dialogFactory.createConfirmDialog(constants.revertTitle(), confirmText, okCallback, cancelCallback);
    }
}
