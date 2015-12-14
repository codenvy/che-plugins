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
package org.eclipse.che.ide.ext.git.client.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.compare.ComparePresenter;
import org.eclipse.che.ide.ext.git.client.compare.changedList.ChangedListPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.Collections;

import static org.eclipse.che.api.git.shared.DiffRequest.DiffType.NAME_STATUS;

/** @author Igor Vinokur */
@Singleton
public class CompareWithLatestRepositoryAction extends GitAction {
    private final ComparePresenter        comparePresenter;
    private final ChangedListPresenter    changedListPresenter;
    private final DialogFactory           dialogFactory;
    private final NotificationManager     notificationManager;
    private final GitServiceClient        gitService;
    private final GitLocalizationConstant locale;
    private final SelectionAgent          selectionAgent;

    private final String REVISION = "HEAD";

    @Inject
    public CompareWithLatestRepositoryAction(ComparePresenter presenter,
                         ChangedListPresenter changedListPresenter,
                         AppContext appContext,
                         DialogFactory dialogFactory,
                         NotificationManager notificationManager,
                         GitServiceClient gitService,
                         GitLocalizationConstant constant,
                         SelectionAgent selectionAgent,
                         ProjectExplorerPresenter projectExplorer) {
        super(constant.compareTitle(), constant.compareTitle(), null, appContext, projectExplorer);
        this.comparePresenter = presenter;
        this.changedListPresenter = changedListPresenter;
        this.dialogFactory = dialogFactory;
        this.notificationManager = notificationManager;
        this.gitService = gitService;
        this.locale = constant;
        this.selectionAgent = selectionAgent;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectConfigDto project = appContext.getCurrentProject().getRootProject();
        String pattern;
        String path;

        Selection<HasStorablePath> selection = (Selection<HasStorablePath>)selectionAgent.getSelection();

        if (selection == null || selection.getHeadElement() == null) {
            path = project.getPath();
        } else {
            path = selection.getHeadElement().getStorablePath();
        }

        pattern = path.replaceFirst(project.getPath(), "");
        pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;

        gitService.diff(project, Collections.singletonList(pattern), NAME_STATUS, false, 0, REVISION, false,
                        new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                            @Override
                            protected void onSuccess(String result) {
                                if (result.isEmpty()) {
                                    dialogFactory.createMessageDialog(locale.compareMessageIdenticalContemtTitle(),
                                                                      locale.compareMessageIdenticalContemtText(), new ConfirmCallback() {
                                                @Override
                                                public void accepted() {
                                                    //Do nothing
                                                }
                                            }).show();
                                } else {
                                    String[] changedFiles = result.split("\n");
                                    if (changedFiles.length == 1) {
                                        comparePresenter.show(changedFiles[0].substring(2), changedFiles[0].substring(0, 1), REVISION);
                                    } else {
                                        changedListPresenter.show(changedFiles, REVISION);
                                    }
                                }
                            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.showError(exception.getMessage());
            }
        });
    }
}
