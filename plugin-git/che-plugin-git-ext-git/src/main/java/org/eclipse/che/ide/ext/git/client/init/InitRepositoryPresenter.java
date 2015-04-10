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
package org.eclipse.che.ide.ext.git.client.init;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitRepositoryInitializer;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

/**
 * Presenter for Git command Init Repository.
 *
 * @author Ann Zhuleva
 * @author Roman Nikitenko
 */
@Singleton
public class InitRepositoryPresenter {
    private final GitRepositoryInitializer gitRepositoryInitializer;
    private final EventBus                 eventBus;
    private final AppContext               appContext;
    private final GitLocalizationConstant  constant;
    private final NotificationManager      notificationManager;

    @Inject
    public InitRepositoryPresenter(AppContext appContext,
                                   EventBus eventBus,
                                   GitLocalizationConstant constant,
                                   NotificationManager notificationManager,
                                   GitRepositoryInitializer gitRepositoryInitializer) {
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.constant = constant;
        this.notificationManager = notificationManager;
        this.gitRepositoryInitializer = gitRepositoryInitializer;
    }

    public void initRepository() {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null || currentProject.getRootProject() == null) {
            Log.error(getClass(), "Open the project before initialize repository");
            return;
        }

        gitRepositoryInitializer.initGitRepository(currentProject.getRootProject(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                handleError(caught);
            }

            @Override
            public void onSuccess(Void result) {
                notificationManager.showInfo(constant.initSuccess());
                //it's need for show .git in project tree
                eventBus.fireEvent(new RefreshProjectTreeEvent());
            }
        });
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@Nonnull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.initFailed();
        notificationManager.showError(errorMessage);
    }
}
