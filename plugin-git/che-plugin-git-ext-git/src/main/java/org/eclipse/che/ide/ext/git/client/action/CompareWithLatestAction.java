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
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.compare.ComparePresenter;
import org.eclipse.che.ide.ext.git.client.compare.changedList.ChangedListPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.api.git.shared.DiffRequest.DiffType.NAME_STATUS;

/**
 * Action for comparing with latest repository version
 *
 * @author Igor Vinokur
 */
@Singleton
public class CompareWithLatestAction extends GitAction {
    private final ComparePresenter        comparePresenter;
    private final ChangedListPresenter    changedListPresenter;
    private final DialogFactory           dialogFactory;
    private final NotificationManager     notificationManager;
    private final GitServiceClient        gitService;
    private final GitLocalizationConstant locale;

    private final static String REVISION = "HEAD";

    @Inject
    public CompareWithLatestAction(ComparePresenter presenter,
                                   ChangedListPresenter changedListPresenter,
                                   AppContext appContext,
                                   DialogFactory dialogFactory,
                                   NotificationManager notificationManager,
                                   GitServiceClient gitService,
                                   GitLocalizationConstant constant,
                                   ProjectExplorerPresenter projectExplorer) {
        super(constant.compareTitle(), constant.compareTitle(), null, appContext, projectExplorer);
        this.comparePresenter = presenter;
        this.changedListPresenter = changedListPresenter;
        this.dialogFactory = dialogFactory;
        this.notificationManager = notificationManager;
        this.gitService = gitService;
        this.locale = constant;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectConfigDto project = appContext.getCurrentProject().getRootProject();
        String pattern;
        String path;

        Selection<ResourceBasedNode<?>> selection = getExplorerSelection();

        if (selection == null || selection.getHeadElement() == null) {
            path = project.getPath();
        } else {
            path = ((HasStorablePath)selection.getHeadElement()).getStorablePath();
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
                                        Map<String, String> items = new HashMap<>();
                                        for (String item : changedFiles) {
                                            items.put(item.substring(2, item.length()), item.substring(0, 1));
                                        }
                                        changedListPresenter.show(items, REVISION);
                                    }
                                }
                            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.showError(exception.getMessage());
            }
        });
    }

    private Selection<ResourceBasedNode<?>> getExplorerSelection() {
        final Selection<ResourceBasedNode<?>> selection = (Selection<ResourceBasedNode<?>>)projectExplorer.getSelection();
        if (selection == null || selection.isEmpty() || selection.getHeadElement() instanceof HasStorablePath) {
            return selection;
        } else {
            return null;
        }
    }
}
