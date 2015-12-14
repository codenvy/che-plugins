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
package org.eclipse.che.ide.ext.git.client.compare;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;

/**
 * Presenter for compearing current files with files from specified working tree
 * 
 * @author Igor Vinokur
 */
@Singleton
public class ComparePresenter implements CompareView.ActionDelegate {

    private final AppContext           appContext;
    private final CompareView          view;
    private final ProjectServiceClient projectService;
    private final GitServiceClient     gitService;
    private final NotificationManager  notificationManager;

    @Inject
    public ComparePresenter(AppContext appContext,
                            CompareView view,
                            ProjectServiceClient projectService,
                            GitServiceClient gitServiceClient,
                            NotificationManager notificationManager) {
        this.appContext = appContext;
        this.view = view;
        this.projectService = projectService;
        this.gitService = gitServiceClient;
        this.notificationManager = notificationManager;
        this.view.setDelegate(this);
    }

    /**
     * Show dialog.
     */
    public void show(final String file, final String state, final String revision) {        
        if (state.contains("A")) {
            showCompare(file, "", revision);
        } else if (state.contains("D")) {
            gitService.show(appContext.getCurrentProject().getRootProject(), file, revision,
                        new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                            @Override
                            protected void onSuccess(final String oldContent) {
                                view.setTitle(file);
                                view.show(oldContent, "", revision, file.substring(file.lastIndexOf("/")));
                            }

                            @Override
                            protected void onFailure(Throwable exception) {
                                notificationManager.showError(exception.getMessage());
                            }
                        });
        } else {
            gitService.show(appContext.getCurrentProject().getRootProject(), file, revision,
                        new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                            @Override
                            protected void onSuccess(final String oldContent) {
                                showCompare(file, oldContent, revision);
                            }

                            @Override
                            protected void onFailure(Throwable exception) {
                                notificationManager.showError(exception.getMessage());
                            }
                        });
        }    
        
    }

    private void showCompare(final String item, final String oldContent, final String revision) {
        String fullItemPath = appContext.getCurrentProject().getRootProject().getName() + "/" + item;

        projectService.getFileContent(fullItemPath,
                                      new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                                          @Override
                                          protected void onSuccess(final String newContent) {
                                              view.setTitle(item);
                                              view.show(oldContent, newContent, revision, item.substring(item.lastIndexOf("/")));
                                          }

                                          @Override
                                          protected void onFailure(Throwable exception) {
                                              notificationManager.showError(exception.getMessage());
                                          }
                                      });
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseButtonClicked() {
        view.hide();
    }
}
