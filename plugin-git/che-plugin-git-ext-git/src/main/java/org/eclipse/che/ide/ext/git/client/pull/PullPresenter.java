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

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.BranchSearcher;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_REMOTE;

/**
 * Presenter pulling changes from remote repository.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class PullPresenter implements PullView.ActionDelegate {
    private final PullView                view;
    private final GitServiceClient        gitServiceClient;
    private final EventBus                eventBus;
    private final GitLocalizationConstant constant;
    private final EditorAgent             editorAgent;
    private final AppContext              appContext;
    private final NotificationManager     notificationManager;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final DtoFactory              dtoFactory;
    private final BranchSearcher          branchSearcher;
    private       CurrentProject          project;


    @Inject
    public PullPresenter(PullView view,
                         EditorAgent editorAgent,
                         GitServiceClient gitServiceClient,
                         EventBus eventBus,
                         AppContext appContext,
                         GitLocalizationConstant constant,
                         NotificationManager notificationManager,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                         DtoFactory dtoFactory,
                         BranchSearcher branchSearcher) {
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.branchSearcher = branchSearcher;
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
        getRemotes();
    }

    /**
     * Get the list of remote repositories for local one. If remote repositories are found, then get the list of branches (remote and
     * local).
     */
    private void getRemotes() {
        view.setEnablePullButton(true);

        gitServiceClient.remoteList(project.getRootProject(), null, true,
                                    new AsyncRequestCallback<Array<Remote>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Remote.class)) {
                                        @Override
                                        protected void onSuccess(Array<Remote> result) {
                                            getBranches(LIST_REMOTE);
                                            view.setRepositories(result);
                                            view.setEnablePullButton(!result.isEmpty());
                                            view.showDialog();
                                        }

                                        @Override
                                        protected void onFailure(Throwable exception) {
                                            String errorMessage =
                                                    exception.getMessage() != null ? exception.getMessage()
                                                                                   : constant.remoteListFailed();
                                            notificationManager.showError(errorMessage);
                                            view.setEnablePullButton(false);
                                        }
                                    }
                                   );
    }

    /**
     * Get the list of branches.
     *
     * @param remoteMode
     *         is a remote mode
     */
    private void getBranches(@Nonnull final String remoteMode) {
        gitServiceClient.branchList(project.getRootProject(), remoteMode,
                                    new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                                        @Override
                                        protected void onSuccess(Array<Branch> result) {
                                            if (LIST_REMOTE.equals(remoteMode)) {
                                                view.setRemoteBranches(branchSearcher.getRemoteBranchesToDisplay(view.getRepositoryName(),
                                                                                                                 result));
                                                getBranches(LIST_LOCAL);
                                            } else {
                                                view.setLocalBranches(branchSearcher.getLocalBranchesToDisplay(result));
                                                for (Branch branch : result.asIterable()) {
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
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().getValues().asIterable()) {
            openedEditors.add(partPresenter);
        }

        gitServiceClient.pull(project.getRootProject(), getRefs(), remoteName,
                              new AsyncRequestCallback<PullResponse>(dtoUnmarshallerFactory.newUnmarshaller(PullResponse.class)) {
                                  @Override
                                  protected void onSuccess(PullResponse result) {
                                      notificationManager.showInfo(result.getCommandOutput());
                                      if (!result.getCommandOutput().contains("Already up-to-date")) {
                                          refreshProject(openedEditors);
                                      }
                                  }

                                  @Override
                                  protected void onFailure(Throwable throwable) {
                                      if (throwable.getMessage().contains("Merge conflict")) {
                                          refreshProject(openedEditors);
                                      }
                                      handleError(throwable, remoteUrl);
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
        eventBus.fireEvent(new RefreshProjectTreeEvent());
        for (EditorPartPresenter partPresenter : openedEditors) {
            final VirtualFile file = partPresenter.getEditorInput().getFile();
            eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
        }
    }

    /** @return list of refs to fetch */
    @Nonnull
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
    private void handleError(@Nonnull Throwable throwable, @Nonnull String remoteUrl) {
        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            notificationManager.showError(constant.pullFail(remoteUrl));
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
}
