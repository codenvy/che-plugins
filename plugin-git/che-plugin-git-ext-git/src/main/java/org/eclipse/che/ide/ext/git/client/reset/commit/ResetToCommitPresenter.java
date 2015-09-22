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
package org.eclipse.che.ide.ext.git.client.reset.commit;

import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.LogResponse;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for resetting head to commit.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class ResetToCommitPresenter implements ResetToCommitView.ActionDelegate {
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private       ResetToCommitView           view;
    private final NewProjectExplorerPresenter projectExplorer;
    private       GitServiceClient            service;
    private       EventBus                    eventBus;
    private       Revision                    selectedRevision;
    private       AppContext                  appContext;
    private       GitLocalizationConstant     constant;
    private       NotificationManager         notificationManager;
    private       EditorAgent                 editorAgent;
    private       List<EditorPartPresenter>   openedEditors;

    /**
     * Create presenter.
     */
    @Inject
    public ResetToCommitPresenter(ResetToCommitView view,
                                  EventBus eventBus,
                                  GitServiceClient service,
                                  GitLocalizationConstant constant,
                                  EditorAgent editorAgent,
                                  AppContext appContext,
                                  NotificationManager notificationManager,
                                  DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                  NewProjectExplorerPresenter projectExplorer) {
        this.view = view;
        this.projectExplorer = projectExplorer;
        this.view.setDelegate(this);
        this.service = service;
        this.eventBus = eventBus;
        this.constant = constant;
        this.editorAgent = editorAgent;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /**
     * Show dialog.
     */
    public void showDialog() {
        service.log(appContext.getCurrentProject().getRootProject(), false,
                    new AsyncRequestCallback<LogResponse>(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class)) {
                        @Override
                        protected void onSuccess(LogResponse result) {
                            view.setRevisions(result.getCommits());
                            view.setMixMode(true);
                            view.setEnableResetButton(selectedRevision != null);
                            view.showDialog();
                        }

                        @Override
                        protected void onFailure(Throwable exception) {
                            String errorMessage = (exception.getMessage() != null) ? exception.getMessage() : constant.logFailed();
                            Notification notification = new Notification(errorMessage, ERROR);
                            notificationManager.showNotification(notification);
                        }
                    }
                   );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResetClicked() {
        view.close();

        openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().values()) {
            openedEditors.add(partPresenter);
        }
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRevisionSelected(@NotNull Revision revision) {
        selectedRevision = revision;
        view.setEnableResetButton(selectedRevision != null);
    }

    /**
     * Reset current HEAD to the specified state and refresh project in the success case.
     */
    private void reset() {
        ResetRequest.ResetType type = view.isMixMode() ? ResetRequest.ResetType.MIXED : null;
        type = (type == null && view.isSoftMode()) ? ResetRequest.ResetType.SOFT : type;
        type = (type == null && view.isHardMode()) ? ResetRequest.ResetType.HARD : type;
//        type = (type == null && view.isKeepMode()) ? ResetRequest.ResetType.KEEP : type;
//        type = (type == null && view.isMergeMode()) ? ResetRequest.ResetType.MERGE : type;

        final ResetRequest.ResetType finalType = type;
        final ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        service.reset(project, selectedRevision.getId(), finalType, null,
                      new AsyncRequestCallback<Void>() {
                          @Override
                          protected void onSuccess(Void result) {
                              if (ResetRequest.ResetType.HARD.equals(finalType) || ResetRequest.ResetType.MERGE.equals(finalType)) {
                                  // Only in the cases of <code>ResetRequest.ResetType.HARD</code>  or <code>ResetRequest.ResetType.MERGE</code>
                                  // must change the workdir
                                  //In this case we can have unconfigured state of the project,
                                  //so we must repeat the logic which is performed when we open a project
                                  refreshProject(openedEditors);
                              }
                              Notification notification = new Notification(constant.resetSuccessfully(), INFO);
                              notificationManager.showNotification(notification);

                          }

                          @Override
                          protected void onFailure(Throwable exception) {
                              String errorMessage = (exception.getMessage() != null) ? exception.getMessage() : constant.resetFail();
                              Notification notification = new Notification(errorMessage, ERROR);
                              notificationManager.showNotification(notification);
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
}

