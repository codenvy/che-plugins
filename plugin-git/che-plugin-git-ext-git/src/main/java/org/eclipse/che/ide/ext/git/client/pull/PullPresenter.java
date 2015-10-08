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
package org.eclipse.che.ide.ext.git.client.pull;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.BranchSearcher;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_REMOTE;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Presenter pulling changes from remote repository.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class PullPresenter implements PullView.ActionDelegate {
    private final PullView                 view;
    private final GitServiceClient         gitServiceClient;
    private final EventBus                 eventBus;
    private final GitLocalizationConstant  constant;
    private final EditorAgent              editorAgent;
    private final AppContext               appContext;
    private final NotificationManager      notificationManager;
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private final GitOutputPartPresenter   console;
    private final DtoFactory               dtoFactory;
    private final BranchSearcher           branchSearcher;
    private final ProjectExplorerPresenter projectExplorer;
    private       CurrentProject           project;


    @Inject
    public PullPresenter(PullView view,
                         EditorAgent editorAgent,
                         GitServiceClient gitServiceClient,
                         GitOutputPartPresenter console,
                         EventBus eventBus,
                         AppContext appContext,
                         GitLocalizationConstant constant,
                         NotificationManager notificationManager,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                         DtoFactory dtoFactory,
                         BranchSearcher branchSearcher,
                         ProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.console = console;
        this.dtoFactory = dtoFactory;
        this.branchSearcher = branchSearcher;
        this.projectExplorer = projectExplorer;
        this.view.setDelegate(this);
        this.gitServiceClient = gitServiceClient;
        this.eventBus = eventBus;
        this.constant = constant;
        this.editorAgent = editorAgent;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        updateRemotes();
    }

    /**
     * Update the list of remote repositories for local one. If remote repositories are found, then update the list of branches (remote and
     * local).
     */
    private void updateRemotes() {
        view.setEnablePullButton(true);

        gitServiceClient.remoteList(project.getRootProject(), null, true,
                                    new AsyncRequestCallback<List<Remote>>(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class)) {
                                        @Override
                                        protected void onSuccess(List<Remote> result) {
                                            updateBranches(LIST_REMOTE);
                                            view.setRepositories(result);
                                            view.setEnablePullButton(!result.isEmpty());
                                            view.showDialog();
                                        }

                                        @Override
                                        protected void onFailure(Throwable exception) {
                                            String errorMessage =
                                                    exception.getMessage() != null ? exception.getMessage()
                                                                                   : constant.remoteListFailed();
                                            console.printError(errorMessage);
                                            notificationManager.showError(errorMessage);
                                            view.setEnablePullButton(false);
                                        }
                                    }
                                   );
    }

    /**
     * Update the list of branches.
     *
     * @param remoteMode is a remote mode
     */
    private void updateBranches(@NotNull final String remoteMode) {
        gitServiceClient.branchList(project.getRootProject(), remoteMode,
                                    new AsyncRequestCallback<List<Branch>>(dtoUnmarshallerFactory.newListUnmarshaller(Branch.class)) {
                                        @Override
                                        protected void onSuccess(List<Branch> result) {
                                            if (LIST_REMOTE.equals(remoteMode)) {
                                                view.setRemoteBranches(branchSearcher.getRemoteBranchesToDisplay(view.getRepositoryName(),
                                                                                                                 result));
                                                updateBranches(LIST_LOCAL);
                                            } else {
                                                view.setLocalBranches(branchSearcher.getLocalBranchesToDisplay(result));
                                                for (Branch branch : result) {
                                                    if (branch.isActive()) {
                                                        view.selectRemoteBranch(branch.getDisplayName());
                                                        break;
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        protected void onFailure(Throwable exception) {
                                            String errorMessage =
                                                    exception.getMessage() != null ? exception.getMessage()
                                                                                   : constant.branchesListFailed();
                                            console.printError(errorMessage);
                                            notificationManager.showError(errorMessage);
                                            view.setEnablePullButton(false);
                                        }
                                    }
                                   );
    }

    /** {@inheritDoc} */
    @Override
    public void onPullClicked() {
        String remoteName = view.getRepositoryName();
        final String remoteUrl = view.getRepositoryUrl();
        view.close();

        final List<EditorPartPresenter> openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().values()) {
            openedEditors.add(partPresenter);
        }

        final Notification notification = new Notification(constant.pullProcess(), PROGRESS, true);
        notificationManager.showNotification(notification);
        gitServiceClient.pull(project.getRootProject(), getRefs(), remoteName,
                              new AsyncRequestCallback<PullResponse>(dtoUnmarshallerFactory.newUnmarshaller(PullResponse.class)) {
                                  @Override
                                  protected void onSuccess(PullResponse result) {
                                      console.printInfo(result.getCommandOutput());
                                      notification.setStatus(FINISHED);
                                      if (result.getCommandOutput().contains("Already up-to-date")) {
                                          notification.setMessage(constant.pullUpToDate());
                                      } else {
                                          refreshProject(openedEditors);
                                          notification.setMessage(constant.pullSuccess(remoteUrl));
                                      }
                                  }

                                  @Override
                                  protected void onFailure(Throwable throwable) {
                                      if (throwable.getMessage().contains("Merge conflict")) {
                                          refreshProject(openedEditors);
                                      }
                                      handleError(throwable, remoteUrl, notification);
                                  }
                              });
    }

    /**
     * Refresh project.
     *
     * @param openedEditors
     *         editors that corresponds to open files
     */
    private void refreshProject(final List<EditorPartPresenter> openedEditors) {
        projectExplorer.reloadChildren();
        for (EditorPartPresenter partPresenter : openedEditors) {
            final VirtualFile file = partPresenter.getEditorInput().getFile();
            eventBus.fireEvent(new FileContentUpdateEvent(file.getPath()));
        }
    }

    /** @return list of refs to fetch */
    @NotNull
    private String getRefs() {
        String remoteName = view.getRepositoryName();
        String localBranch = view.getLocalBranch();
        String remoteBranch = view.getRemoteBranch();

        return localBranch.isEmpty() ? remoteBranch
                                     : "refs/heads/" + localBranch + ":" + "refs/remotes/" + remoteName + "/" + remoteBranch;
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param throwable
     *         exception what happened
     */
    private void handleError(@NotNull Throwable throwable, @NotNull String remoteUrl, Notification notification) {
        String errorMessage = throwable.getMessage();
        notification.setType(ERROR);
        if (errorMessage == null) {
            console.printError(constant.pullFail(remoteUrl));
            notification.setMessage(constant.pullFail(remoteUrl));
            return;
        }

        try {
            errorMessage = dtoFactory.createDtoFromJson(errorMessage, ServiceError.class).getMessage();
            if (errorMessage.equals("Unable get private ssh key")) {
                console.printError(constant.messagesUnableGetSshKey());
                notification.setMessage(constant.messagesUnableGetSshKey());
                return;
            }
            console.printError(errorMessage);
            notification.setMessage(errorMessage);
        } catch (Exception e) {
            console.printError(errorMessage);
            notification.setMessage(errorMessage);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoteBranchChanged() {
        view.selectLocalBranch(view.getRemoteBranch());
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoteRepositoryChanged() {
        updateBranches(LIST_REMOTE);
    }
}
