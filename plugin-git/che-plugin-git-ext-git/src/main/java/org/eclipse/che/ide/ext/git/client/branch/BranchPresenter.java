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
package org.eclipse.che.ide.ext.git.client.branch;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_ALL;

/**
 * Presenter for displaying and work with branches.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class BranchPresenter implements BranchView.ActionDelegate {
    private DtoFactory              dtoFactory;
    private DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private BranchView              view;
    private GitOutputPartPresenter  gitConsole;
    private WorkspaceAgent          workspaceAgent;
    private DialogFactory           dialogFactory;
    private EventBus                eventBus;
    private CurrentProject          project;
    private GitServiceClient        service;
    private GitLocalizationConstant constant;
    private EditorAgent             editorAgent;
    private Branch                  selectedBranch;
    private AppContext              appContext;
    private NotificationManager     notificationManager;

    /** Create presenter. */
    @Inject
    public BranchPresenter(BranchView view,
                           EventBus eventBus,
                           DtoFactory dtoFactory,
                           EditorAgent editorAgent,
                           GitServiceClient service,
                           GitLocalizationConstant constant,
                           AppContext appContext,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           GitOutputPartPresenter gitConsole,
                           WorkspaceAgent workspaceAgent,
                           DialogFactory dialogFactory) {
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.gitConsole = gitConsole;
        this.workspaceAgent = workspaceAgent;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
        this.eventBus = eventBus;
        this.editorAgent = editorAgent;
        this.service = service;
        this.constant = constant;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        view.setEnableCheckoutButton(false);
        view.setEnableDeleteButton(false);
        view.setEnableRenameButton(false);
        getBranches();
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onRenameClicked() {
        if (selectedBranch.isRemote()) {
            dialogFactory.createConfirmDialog(constant.branchConfirmRenameTitle(), constant.branchConfirmRenameMessage(),
                                              getConfirmRenameBranchCallback(), null).show();
        } else {
            renameBranch();
        }
    }

    private ConfirmCallback getConfirmRenameBranchCallback() {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                renameBranch();
            }
        };
    }

    private void renameBranch() {
        final String selectedBranchName = getSelectedBranchName();
        dialogFactory.createInputDialog(constant.branchTitleRename(), constant.branchTypeRename(), selectedBranchName,
                                        0, selectedBranchName.length(), getNewBranchNameCallback(), null).show();
    }

    private InputCallback getNewBranchNameCallback() {
        return new InputCallback() {
            @Override
            public void accepted(String newBranchName) {
                renameBranch(newBranchName);
            }
        };
    }

    private void renameBranch(String newName) {
        service.branchRename(project.getRootProject(), selectedBranch.getDisplayName(), newName, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMessage =
                        (exception.getMessage() != null) ? exception.getMessage() : constant.branchRenameFailed();
                notificationManager.showError(errorMessage);
                getBranches();//rename of remote branch occurs in three stages, so needs update list of branches on view
            }
        });
    }

    /** @return name of branch, e.g. 'origin/master' -> 'master' */
    private String getSelectedBranchName() {
        String selectedBranchName = selectedBranch.getDisplayName();
        String[] tokens = selectedBranchName.split("/");
        return tokens.length > 0 ? tokens[tokens.length - 1] : selectedBranchName;
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteClicked() {
        final String name = selectedBranch.getName();

        service.branchDelete(project.getRootProject(), name, true, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
            }

            @Override
            protected void onFailure(Throwable exception) {
                handleError(exception);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCheckoutClicked() {
        final List<EditorPartPresenter> openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().getValues().asIterable()) {
            openedEditors.add(partPresenter);
        }

        String name = selectedBranch.getDisplayName();
        String startingPoint = null;
        boolean remote = selectedBranch.isRemote();
        if (remote) {
            startingPoint = selectedBranch.getDisplayName();
        }
        if (name == null) {
            return;
        }

        service.branchCheckout(project.getRootProject(), name, startingPoint, remote, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
                String projectPath = project.getRootProject().getPath();
                //In this case we can have unconfigured state of the project,
                //so we must repeat the logic which is performed when we open a project
                eventBus.fireEvent(new OpenProjectEvent(projectPath));
            }

            @Override
            protected void onFailure(Throwable exception) {
                printGitMessage(exception.getMessage());
            }
        });
    }

    private void printGitMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return;
        }
        JSONObject jsonObject = JSONParser.parseStrict(messageText).isObject();
        if (jsonObject == null) {
            return;
        }
        String message = "";
        if (jsonObject.containsKey("message")) {
            message = jsonObject.get("message").isString().stringValue();
        }

        workspaceAgent.openPart(gitConsole, PartStackType.INFORMATION);

        gitConsole.print("");
        String[] lines = message.split("\n");
        for (String line : lines) {
            gitConsole.printError(line);
        }
    }

    /** Get the list of branches. */
    private void getBranches() {
        service.branchList(project.getRootProject(), LIST_ALL,
                           new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(Array<Branch> result) {
                                   view.setBranches(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   final String errorMessage =
                                           (exception.getMessage() != null) ? exception.getMessage() : constant.branchesListFailed();
                                   notificationManager.showError(errorMessage);
                               }
                           }
                          );
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateClicked() {
        dialogFactory.createInputDialog(constant.branchCreateNew(), constant.branchTypeNew(), new InputCallback() {
            @Override
            public void accepted(String value) {
                if (!value.isEmpty()) {
                    service.branchCreate(project.getRootProject(), value, null,
                                         new AsyncRequestCallback<Branch>(dtoUnmarshallerFactory.newUnmarshaller(Branch.class)) {
                                             @Override
                                             protected void onSuccess(Branch result) {
                                                 getBranches();
                                             }

                                             @Override
                                             protected void onFailure(Throwable exception) {
                                                 final String errorMessage = (exception.getMessage() != null)
                                                                             ? exception.getMessage()
                                                                             : constant.branchCreateFailed();
                                                 notificationManager.showError(errorMessage);
                                             }
                                         }
                                        );
                }

            }
        }, null).show();
    }

    /** {@inheritDoc} */
    @Override
    public void onBranchSelected(@Nonnull Branch branch) {
        selectedBranch = branch;
        boolean isActive = selectedBranch.isActive();

        view.setEnableCheckoutButton(!isActive);
        view.setEnableDeleteButton(!isActive);
        view.setEnableRenameButton(true);
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param throwable
     *         exception what happened
     */
    void handleError(@Nonnull Throwable throwable) {
        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            notificationManager.showError(constant.branchDeleteFailed());
            return;
        }

        try {
            errorMessage = dtoFactory.createDtoFromJson(errorMessage, ServiceError.class).getMessage();
            if (errorMessage.equals("Unable get private ssh key")) {
                notificationManager.showError(constant.messagesUnableGetSshKey());
                return;
            }
            notificationManager.showError(errorMessage);
        } catch (Exception e) {
            notificationManager.showError(errorMessage);
        }
    }
}