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
package org.eclipse.che.ide.ext.svn.client.log;

import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputPresenter;
import org.eclipse.che.ide.ext.svn.client.common.SubversionActionPresenter;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

public class ShowLogPresenter extends SubversionActionPresenter {

    private final AppContext              appContext;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final SubversionClientService subversionClientService;
    private final NotificationManager     notificationManager;

    @Inject
    protected ShowLogPresenter(final AppContext appContext,
                               final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               final EventBus eventBus,
                               final WorkspaceAgent workspaceAgent,
                               final RawOutputPresenter console,
                               final SubversionClientService subversionClientService,
                               final NotificationManager notificationManager,
                               final ProjectExplorerPart projectExplorerPart) {
        super(appContext, eventBus, console, workspaceAgent, projectExplorerPart);

        this.appContext = appContext;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.subversionClientService = subversionClientService;
        this.notificationManager = notificationManager;
    }

    public void showLog() {
        CurrentProject project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        subversionClientService.showLog(project.getRootProject().getPath(), getSelectedPaths(), "1:HEAD",
                                        new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {
                                            @Override
                                            protected void onSuccess(CLIOutputResponse result) {
                                                printCommand(result.getCommand());
                                                printAndSpace(result.getOutput());
                                            }

                                            @Override
                                            protected void onFailure(Throwable exception) {
                                                notificationManager.showError(exception.getMessage());
                                            }
                                        });
    }

}
