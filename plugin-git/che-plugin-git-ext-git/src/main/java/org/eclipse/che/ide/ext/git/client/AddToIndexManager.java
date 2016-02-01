/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.git.client;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.event.CreateFileNodeEvent;
import org.eclipse.che.ide.event.CreateFileNodeEventHandler;
import org.eclipse.che.ide.event.DeleteNodesEvent;
import org.eclipse.che.ide.event.DeleteNodesEventHandler;
import org.eclipse.che.ide.event.RenameNodeEvent;
import org.eclipse.che.ide.event.RenameNodeEventHandler;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.ext.java.client.event.RefactoringProvidedEvent;
import org.eclipse.che.ide.ext.java.client.event.RefactoringProvidedEventHandler;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeInfo;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ModuleNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.SUCCESS;

/**
 * @author Igor Vinokur
 */
public class AddToIndexManager implements CreateFileNodeEventHandler, RefactoringProvidedEventHandler, DeleteNodesEventHandler,
                                          RenameNodeEventHandler {
    private final AppContext              appContext;
    private final DialogFactory           dialogFactory;
    private final DtoUnmarshallerFactory  unmarshallerFactory;
    private final GitLocalizationConstant locale;
    private final GitServiceClient        gitService;
    private final NotificationManager     notificationManager;

    private final static String YES_BUTTON = "Yes";
    private final static String NO_BUTTON = "No";

    public AddToIndexManager(AppContext appContext,
                             DialogFactory dialogFactory,
                             DtoUnmarshallerFactory unmarshallerFactory,
                             EventBus eventBus,
                             GitLocalizationConstant locale,
                             GitServiceClient gitService,
                             NotificationManager notificationManager) {
        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.unmarshallerFactory = unmarshallerFactory;
        this.locale = locale;
        this.gitService = gitService;
        this.notificationManager = notificationManager;

        eventBus.addHandler(CreateFileNodeEvent.TYPE, this);
        eventBus.addHandler(RefactoringProvidedEvent.TYPE, this);
        eventBus.addHandler(DeleteNodesEvent.TYPE, this);
        eventBus.addHandler(RenameNodeEvent.TYPE, this);
    }

    @Override
    public void onFileNodeCreated(CreateFileNodeEvent event) {
        if (!isUnderGit()) {
            return;
        }
        final String pathName = normalizePath(event.getItem().getPath());
        final String fileName = pathName.substring(pathName.lastIndexOf("/") + 1);
        ConfirmCallback confirmCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                Notification successNotification = new StatusNotification(locale.addSuccess(),
                                                                          locale.newFileAddToIndexNotification(fileName),
                                                                          SUCCESS,
                                                                          false,
                                                                          appContext.getCurrentProject().getRootProject(),
                                                                          null);
                addToIndex(Collections.singletonList(pathName), successNotification);
            }
        };
        CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void cancelled() {
                //Do nothing
            }
        };
        dialogFactory.createConfirmDialog(locale.newFileAddToIndexDialogTitle(),
                                          locale.newFileAddToIndexDialogText(fileName),
                                          YES_BUTTON,
                                          NO_BUTTON,
                                          confirmCallback,
                                          cancelCallback).show();
    }

    @Override
    public void onNodesDeleted(final DeleteNodesEvent event) {
        if (!isUnderGit()) {
            return;
        }
        AsyncRequestCallback<Status> callback = new AsyncRequestCallback<Status>(unmarshallerFactory.newUnmarshaller(Status.class)) {
            @Override
            protected void onSuccess(Status result) {
                final List<String> nodePathNamesToAdd =new ArrayList<>();
                for (ResourceBasedNode deletedNode : event.getDeletedNodes()) {
                    String deletedNodePathName = normalizePath(getNodePathName(deletedNode));
                    if ((result.getMissing().contains(deletedNodePathName)) ||  wasRenamed(result.getMissing(), deletedNodePathName)) {
                        nodePathNamesToAdd.add(deletedNodePathName);
                    }
                }
                if (nodePathNamesToAdd.isEmpty()) {
                    return;
                }
                ConfirmCallback confirmCallback = new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        Notification successNotification = new StatusNotification(locale.addSuccess(),
                                                                                  locale.deleteAddToIndexDialogNotification(),
                                                                                  SUCCESS,
                                                                                  false,
                                                                                  appContext.getCurrentProject().getRootProject(),
                                                                                  null);
                        addToIndex(nodePathNamesToAdd, successNotification);
                    }
                };
                CancelCallback cancelCallback = new CancelCallback() {
                    @Override
                    public void cancelled() {
                        //Do nothing
                    }
                };
                dialogFactory.createConfirmDialog(locale.deleteAddToIndexDialogTitle(),
                                                  locale.deleteAddToIndexDialogText(),
                                                  YES_BUTTON,
                                                  NO_BUTTON,
                                                  confirmCallback,
                                                  cancelCallback).show();
            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.notify(locale.statusFailed(), FAIL, true, appContext.getCurrentProject().getRootProject());
            }
        };

        gitStatus(callback);
    }

    @Override
    public void onRefactoringProvided(final RefactoringProvidedEvent event) {
        if (!isUnderGit()) {
            return;
        }
        AsyncRequestCallback<Status> callback = new AsyncRequestCallback<Status>(unmarshallerFactory.newUnmarshaller(Status.class)) {
            @Override
            protected void onSuccess(Status result) {
                List<String> nodePathNamesToAdd = new ArrayList<>();
                for (ChangeInfo changeInfo : event.getRefactoringResult()) {
                    if (changeInfo.getOldPath() == null) {
                        continue;
                    }
                    String oldPathName = normalizePath(changeInfo.getOldPath());
                    String newPathName = normalizePath(changeInfo.getPath());
                    if ((result.getMissing().contains(oldPathName) && result.getUntracked().contains(newPathName)) ||
                        wasRenamed(result.getMissing(), oldPathName)) {
                        nodePathNamesToAdd.add(oldPathName);
                        nodePathNamesToAdd.add(newPathName);
                    }
                }
                if (!nodePathNamesToAdd.isEmpty()) {
                    Notification successNotification = new StatusNotification(locale.addSuccess(),
                                                                              locale.refactoringAddToIndexDialogNotification(),
                                                                              SUCCESS,
                                                                              false,
                                                                              appContext.getCurrentProject().getRootProject(),
                                                                              null);
                    addToIndex(nodePathNamesToAdd, successNotification);
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.notify(locale.statusFailed(), FAIL, true, appContext.getCurrentProject().getRootProject());
            }
        };

        gitStatus(callback);
    }

    @Override
        public void onNodeRenamed(final RenameNodeEvent event) {
        if (!isUnderGit()) {
            return;
        }
        AsyncRequestCallback<Status> callback = new AsyncRequestCallback<Status>(unmarshallerFactory.newUnmarshaller(Status.class)) {
            @Override
            protected void onSuccess(Status result) {
                List<String> nodePathNamesToAdd = new ArrayList<>();
                String oldPathName = normalizePath(event.getNode().getStorablePath());
                String newPathName = normalizePath(event.getNewFilePathName());
                if ((result.getMissing().contains(oldPathName) && result.getUntracked().contains(newPathName)) ||
                    wasRenamed(result.getMissing(), oldPathName)) {
                    nodePathNamesToAdd.add(oldPathName);
                    nodePathNamesToAdd.add(newPathName);
                }
                if (nodePathNamesToAdd.isEmpty()) {
                    return;
                }
                Notification successNotification =
                        new StatusNotification(locale.addSuccess(),
                                               locale.newFileAddToIndexNotification                           (
                                                       newPathName.substring(newPathName.lastIndexOf("/") + 1)),
                                               SUCCESS,
                                               false,
                                               appContext.getCurrentProject().getRootProject(),
                                               null);
                addToIndex(nodePathNamesToAdd, successNotification);

            }

            @Override
            protected void onFailure(Throwable exception) {
                notificationManager.notify(locale.statusFailed(), FAIL, true, appContext.getCurrentProject().getRootProject());
            }
        };
        gitStatus(callback);
    }

    private void gitStatus(AsyncRequestCallback<Status> callback) {
        gitService.status(appContext.getWorkspaceId(),
                          appContext.getCurrentProject().getRootProject(),
                          callback);
    }

    private void addToIndex(List<String> filesToAdd, final Notification successNotification) {
        final ProjectConfigDto project = appContext.getCurrentProject().getRootProject();
        try {
            gitService.add(appContext.getWorkspaceId(), project, false, filesToAdd,
                           new RequestCallback<Void>() {
                               @Override
                               protected void onSuccess(Void result) {
                                   notificationManager.notify(successNotification);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   notificationManager.notify(locale.addFailed(), FAIL, true, project);
                               }
                           });
        } catch (WebSocketException exception) {
            notificationManager.notify(locale.addFailed(), FAIL, true, project);
        }
    }

    private String normalizePath(String path) {
        String projectPath = appContext.getCurrentProject().getRootProject().getPath();

        String pattern = path;
        if (path.startsWith(projectPath)) {
            pattern = pattern.replaceFirst(projectPath, "");
        }
        if (pattern.startsWith("/")) {
            pattern = pattern.replaceFirst("/", "");
        }
        return pattern;
    }

    private boolean wasRenamed(List<String> missing, String oldName) {
        for (String renaming : missing) {
            if (renaming.contains(oldName)) {
                return true;
            }
        }
        return false;
    }

    private String getNodePathName(ResourceBasedNode node) {
        if (node instanceof ModuleNode) {
            return  ((ModuleNode)node).getStorablePath();
        } else if (node instanceof FolderReferenceNode) {
            return  ((FolderReferenceNode)node).getStorablePath();
        } else if (node instanceof FileReferenceNode) {
            return  ((FileReferenceNode)node).getStorablePath();
        } else {
            return "";
        }
    }

    private boolean isUnderGit() {
        //TODO rework checking for git mixin when IDEX-3725 will be fixed
        final ProjectConfigDto project = appContext.getCurrentProject().getRootProject();
        Map<String, List<String>> attributes = project.getAttributes();
        return attributes.containsKey("vcs.provider.name") && attributes.get("vcs.provider.name").contains("git");
    }
}
