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
package org.eclipse.che.ide.ext.git.client.commit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.git.client.DateTimeFormatter;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for commit changes on git.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class CommitPresenter implements CommitView.ActionDelegate {
    private final GitOutputPartPresenter  console;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final AppContext              appContext;
    private final CommitView              view;
    private final GitServiceClient        service;
    private final GitLocalizationConstant constant;
    private final NotificationManager     notificationManager;
    private final DateTimeFormatter       dateTimeFormatter;
    private final SelectionAgent          selectionAgent;

    @Inject
    public CommitPresenter(CommitView view,
                           GitServiceClient service,
                           GitLocalizationConstant constant,
                           GitOutputPartPresenter console,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           AppContext appContext,
                           DateTimeFormatter dateTimeFormatter,
                           final SelectionAgent selectionAgent) {
        this.view = view;
        this.console = console;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.dateTimeFormatter = dateTimeFormatter;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.notificationManager = notificationManager;
        this.selectionAgent = selectionAgent;
    }

    /** Show dialog. */
    public void showDialog() {
        view.setAmend(false);
        view.setAllFilesInclude(false);
        view.setIncludeSelection(false);
        view.setOnlySelection(false);
        view.setEnableCommitButton(!view.getMessage().isEmpty());
        view.showDialog();
        view.focusInMessageField();
    }

    /** {@inheritDoc} */
    @Override
    public void onCommitClicked() {
        final String message = view.getMessage();
        final boolean all = view.isAllFilesInclued();
        final boolean amend = view.isAmend();
        final boolean selectionFlag = this.view.isIncludeSelection();
        final boolean onlySelectionFlag = this.view.isOnlySelection();

        if (selectionFlag) {
            commitAddSelection(message, amend);
        } else if (onlySelectionFlag) {
            commitOnlySelection(message, amend);
        } else {
            doCommit(message, all, amend);
        }

    }

    private void commitAddSelection(final String message, final boolean amend) {
        // first git-add the selection
        @SuppressWarnings("unchecked")
        final Selection<HasStorablePath> selection = (Selection<HasStorablePath>)this.selectionAgent.getSelection();
        if (selection.isEmpty() || !(selection.getHeadElement() instanceof HasStorablePath)) {
            doCommit(message, false, amend);
        } else {
            final List<String> filePattern = buildFileList(selection);

            try {
                service.add(appContext.getCurrentProject().getRootProject(), false, filePattern, new RequestCallback<Void>() {
                    @Override
                    protected void onSuccess(final Void result) {
                        // then commit
                        doCommit(message, false, amend);
                    }

                    @Override
                    protected void onFailure(final Throwable exception) {
                        handleError(exception);
                    }
                });
            } catch (final WebSocketException e) {
                handleError(new Exception("Communication error with the server", e));
            }
        }
    }

    private List<String> buildFileList(final Selection<HasStorablePath> selection) {
        final List<String> filePattern = new ArrayList<>();
        final List<HasStorablePath> selected = selection.getAllElements();
        final String base = appContext.getCurrentProject().getRootProject().getPath();
        for (final HasStorablePath node : selected) {
            filePattern.add(getPath(node, base));
        }
        return filePattern;
    }

    private void commitOnlySelection(final String message, final boolean amend) {
        // first git-add the selection
        @SuppressWarnings("unchecked")
        final Selection<HasStorablePath> selection = (Selection<HasStorablePath>)this.selectionAgent.getSelection();
        if (selection != null && !selection.isEmpty() && (selection.getHeadElement() != null)) {
            final List<String> files = buildFileList(selection);
            service.commit(appContext.getCurrentProject().getRootProject(), message, files, amend,
                           new AsyncRequestCallback<Revision>(dtoUnmarshallerFactory.newUnmarshaller(Revision.class)) {
                               @Override
                               protected void onSuccess(final Revision result) {
                                   if (!result.isFake()) {
                                       onCommitSuccess(result);
                                   } else {
                                       console.printError(result.getMessage());
                                       final Notification notification = new Notification(result.getMessage(), ERROR);
                                       notificationManager.showNotification(notification);
                                   }
                               }

                               @Override
                               protected void onFailure(final Throwable exception) {
                                   handleError(exception);
                               }
                           }
                          );
        } // else don't commit as it goes against user intent
        this.view.close();
    }

    private void doCommit(final String message, final boolean all, final boolean amend) {
        service.commit(appContext.getCurrentProject().getRootProject(), message, all, amend,
                       new AsyncRequestCallback<Revision>(dtoUnmarshallerFactory.newUnmarshaller(Revision.class)) {
                           @Override
                           protected void onSuccess(final Revision result) {
                               if (!result.isFake()) {
                                   onCommitSuccess(result);
                               } else {
                                   console.printError(result.getMessage());
                                   final Notification notification = new Notification(result.getMessage(), ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           }

                           @Override
                           protected void onFailure(final Throwable exception) {
                               handleError(exception);
                           }
                       }
                      );
        this.view.close();
    }

    private String getPath(final HasStorablePath node, final String base) {
        String path = node.getStorablePath();
        if (path.startsWith(base)) {
            path = path.replaceFirst(base, "");
        }
        if (path.startsWith("/")) {
            path = path.replaceFirst("/", "");
        }
        if (path.isEmpty() || "/".equals(path)) {
            path = ".";
        }
        return path;
    }

    /**
     * Performs action when commit is successfully completed.
     *
     * @param revision
     *         a {@link Revision}
     */
    private void onCommitSuccess(@NotNull final Revision revision) {
        String date = dateTimeFormatter.getFormattedDate(revision.getCommitTime());
        String message = constant.commitMessage(revision.getId(), date);

        if ((revision.getCommitter() != null && revision.getCommitter().getName() != null &&
             !revision.getCommitter().getName().isEmpty())) {
            message += " " + constant.commitUser(revision.getCommitter().getName());
        }

        console.printInfo(message);
        Notification notification = new Notification(message, INFO);
        notificationManager.showNotification(notification);
        view.setMessage("");
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@NotNull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.commitFailed();
        console.printError(errorMessage);
        Notification notification = new Notification(errorMessage, ERROR);
        notificationManager.showNotification(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onValueChanged() {
        String message = view.getMessage();
        view.setEnableCommitButton(!message.isEmpty());
    }

    @Override
    public void setAmendCommitMessage() {
        final Unmarshallable<LogResponse> unmarshall = dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class);
        this.service.log(appContext.getCurrentProject().getRootProject(), false, new AsyncRequestCallback<LogResponse>(unmarshall) {
            @Override
            protected void onSuccess(final LogResponse result) {
                final List<Revision> commits = result.getCommits();
                String message = "";
                if (commits != null && (!commits.isEmpty())) {
                    final Revision tip = commits.get(0);
                    if (tip != null) {
                        message = tip.getMessage();
                    }
                }
                CommitPresenter.this.view.setMessage(message);
                CommitPresenter.this.view.setEnableCommitButton(!message.isEmpty());
            }

            @Override
            protected void onFailure(final Throwable exception) {
                Log.warn(CommitPresenter.class, "Git log failed", exception);
                CommitPresenter.this.view.setMessage("");
            }
        });
    }
}
