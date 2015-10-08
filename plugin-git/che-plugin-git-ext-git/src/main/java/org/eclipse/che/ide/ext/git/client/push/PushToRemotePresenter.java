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
package org.eclipse.che.ide.ext.git.client.push;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.commons.exception.UnauthorizedException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.BranchFilterByRemote;
import org.eclipse.che.ide.ext.git.client.BranchSearcher;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_REMOTE;
import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Presenter for pushing changes to remote repository.
 *
 * @author Ann Zhuleva
 * @author Sergii Leschenko
 */
@Singleton
public class PushToRemotePresenter implements PushToRemoteView.ActionDelegate {
    private final DtoFactory              dtoFactory;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final GitOutputPartPresenter  console;
    private final BranchSearcher          branchSearcher;
    private       PushToRemoteView        view;
    private       GitServiceClient        service;
    private       AppContext              appContext;
    private       GitLocalizationConstant constant;
    private       NotificationManager     notificationManager;
    private       CurrentProject          project;

    @Inject

    public PushToRemotePresenter(DtoFactory dtoFactory,
                                 PushToRemoteView view,
                                 GitServiceClient service,
                                 GitOutputPartPresenter console,
                                 AppContext appContext,
                                 GitLocalizationConstant constant,
                                 NotificationManager notificationManager,
                                 DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                 BranchSearcher branchSearcher) {
        this.dtoFactory = dtoFactory;
        this.view = view;
        this.console = console;
        this.branchSearcher = branchSearcher;

        this.view = view;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */

    public void showDialog() {
        project = appContext.getCurrentProject();
        updateRemotes();
    }

    /**
     * Get the list of remote repositories for local one.
     * If remote repositories are found, then get the list of branches (remote and local).
     */
    void updateRemotes() {
        service.remoteList(project.getRootProject(), null, true,
                           new AsyncRequestCallback<List<Remote>>(dtoUnmarshallerFactory.newListUnmarshaller(Remote.class)) {
                               @Override
                               protected void onSuccess(List<Remote> result) {
                                   updateLocalBranches();
                                   view.setRepositories(result);
                                   view.setEnablePushButton(!result.isEmpty());
                                   view.showDialog();
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage() : constant.remoteListFailed();
                                   console.printError(errorMessage);
                                   notificationManager.showError(errorMessage);
                                   view.setEnablePushButton(false);
                               }
                           }
                          );
    }

    /**
     * Update list of local and remote branches on view.
     */
    void updateLocalBranches() {
        //getting local branches
        getBranchesForCurrentProject(LIST_LOCAL, new AsyncCallback<List<Branch>>() {
            @Override
            public void onSuccess(List<Branch> result) {
                List<String> localBranches = branchSearcher.getLocalBranchesToDisplay(result);
                view.setLocalBranches(localBranches);

                for (Branch branch : result) {
                    if (branch.isActive()) {
                        view.selectLocalBranch(branch.getDisplayName());
                        break;
                    }
                }

                //getting remote branch only after selecting current local branch
                updateRemoteBranches();
            }

            @Override
            public void onFailure(Throwable exception) {
                String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.localBranchesListFailed();
                console.printError(errorMessage);
                notificationManager.showError(errorMessage);
                view.setEnablePushButton(false);
            }
        });
    }

    /**
     * Update list of remote branches on view.
     */
    void updateRemoteBranches() {
        getBranchesForCurrentProject(LIST_REMOTE, new AsyncCallback<List<Branch>>() {
            @Override
            public void onSuccess(final List<Branch> result) {
                // Need to add the upstream of local branch in the list of remote branches
                // to be able to push changes to the remote upstream branch
                getUpstreamBranch(new AsyncCallback<Branch>() {
                    @Override
                    public void onSuccess(Branch upstream) {
                        BranchFilterByRemote remoteRefsHandler = new BranchFilterByRemote(view.getRepository());

                        final List<String> remoteBranches = branchSearcher.getRemoteBranchesToDisplay(remoteRefsHandler, result);

                        String selectedRemoteBranch = null;
                        if (upstream != null && upstream.isRemote() && remoteRefsHandler.isLinkedTo(upstream)) {
                            String simpleUpstreamName = remoteRefsHandler.getBranchNameWithoutRefs(upstream);
                            if (!remoteBranches.contains(simpleUpstreamName)) {
                                remoteBranches.add(simpleUpstreamName);
                            }
                            selectedRemoteBranch = simpleUpstreamName;
                        }

                        // Need to add the current local branch in the list of remote branches
                        // to be able to push changes to the remote branch  with same name
                        final String currentBranch = view.getLocalBranch();
                        if (!remoteBranches.contains(currentBranch)) {
                            remoteBranches.add(currentBranch);
                        }
                        if (selectedRemoteBranch == null) {
                            selectedRemoteBranch = currentBranch;
                        }

                        view.setRemoteBranches(remoteBranches);
                        view.selectRemoteBranch(selectedRemoteBranch);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        console.printError(constant.failedGettingConfig());
                        notificationManager.showError(constant.failedGettingConfig());
                    }
                });
            }

            @Override
            public void onFailure(Throwable exception) {
                String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.remoteBranchesListFailed();
                console.printError(errorMessage);
                notificationManager.showError(errorMessage);
                view.setEnablePushButton(false);
            }

        });
    }


    /**
     * Get upstream branch for selected local branch. Can invoke {@code onSuccess(null)} if upstream branch isn't set
     */
    private void getUpstreamBranch(final AsyncCallback<Branch> result) {

        final String configBranchRemote = "branch." + view.getLocalBranch() + ".remote";
        final String configUpstreamBranch = "branch." + view.getLocalBranch() + ".merge";
        service.config(project.getRootProject(), Arrays.asList(configUpstreamBranch, configBranchRemote), false,
                       new AsyncRequestCallback<Map<String, String>>(new StringMapUnmarshaller()) {
                           @Override
                           protected void onSuccess(Map<String, String> configs) {
                               if (configs.containsKey(configBranchRemote) && configs.containsKey(configUpstreamBranch)) {
                                   String displayName = configs.get(configBranchRemote) + "/" + configs.get(configUpstreamBranch);
                                   Branch upstream = dtoFactory.createDto(Branch.class)
                                                               .withActive(false)
                                                               .withRemote(true)
                                                               .withDisplayName(displayName)
                                                               .withName("refs/remotes/" + displayName);

                                   result.onSuccess(upstream);
                               } else {
                                   result.onSuccess(null);
                               }
                           }

                           @Override
                           protected void onFailure(Throwable exception) {
                               result.onFailure(exception);
                           }
                       });
    }

    /**
     * Get the list of branches.
     *
     * @param remoteMode
     *         is a remote mode
     */
    void getBranchesForCurrentProject(@NotNull final String remoteMode,
                                      final AsyncCallback<List<Branch>> asyncResult) {
        service.branchList(project.getRootProject(),
                           remoteMode,
                           new AsyncRequestCallback<List<Branch>>(dtoUnmarshallerFactory.newListUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(List<Branch> result) {
                                   asyncResult.onSuccess(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   asyncResult.onFailure(exception);
                               }
                           }
                          );
    }

    /** {@inheritDoc} */
    @Override
    public void onPushClicked() {
        final Notification notification = new Notification(constant.pushProcess(), PROGRESS, true);
        notificationManager.showNotification(notification);

        final String repository = view.getRepository();
        service.push(project.getRootProject(), getRefs(), repository, false,
                     new AsyncRequestCallback<PushResponse>(dtoUnmarshallerFactory.newUnmarshaller(PushResponse.class)) {
                         @Override
                         protected void onSuccess(PushResponse result) {
                             console.printInfo(result.getCommandOutput());
                             notification.setStatus(FINISHED);
                             if (result.getCommandOutput().contains("Everything up-to-date")) {
                                 notification.setMessage(constant.pushUpToDate());
                             } else {
                                 notification.setMessage(constant.pushSuccess(repository));
                             }
                         }

                         @Override
                         protected void onFailure(Throwable exception) {
                             handleError(exception, notification);
                         }
                     });
        view.close();
    }

    /** @return list of refs to push */
    @NotNull
    private List<String> getRefs() {
        String localBranch = view.getLocalBranch();
        String remoteBranch = view.getRemoteBranch();
        return new ArrayList<>(Arrays.asList(localBranch + ":" + remoteBranch));
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onLocalBranchChanged() {
        view.addRemoteBranch(view.getLocalBranch());
        view.selectRemoteBranch(view.getLocalBranch());
    }

    @Override
    public void onRepositoryChanged() {
        updateRemoteBranches();
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param throwable
     *         exception what happened
     */
    void handleError(@NotNull Throwable throwable, Notification notification) {
        notification.setType(ERROR);
        if (throwable instanceof UnauthorizedException) {
            console.printError(constant.messagesNotAuthorized());
            notification.setMessage(constant.messagesNotAuthorized());
            return;
        }

        String errorMessage = throwable.getMessage();
        if (errorMessage == null) {
            console.printError(constant.pushFail());
            notification.setMessage(constant.pushFail());
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
}
