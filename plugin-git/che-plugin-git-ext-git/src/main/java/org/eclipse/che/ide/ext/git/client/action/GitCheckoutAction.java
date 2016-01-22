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
package org.eclipse.che.ide.ext.git.client.action;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;

import javax.inject.Inject;
import java.util.List;

/**
 * Action for git checkout perform.
 *
 * @author Anton Korneta
 */
public class GitCheckoutAction extends Action {
    private final AppContext              appContext;
    private final GitLocalizationConstant localization;
    private final NotificationManager     notificationManager;
    private final GitServiceClient        gitServiceClient;
    private final DtoFactory              dtoFactory;

    @Inject
    public GitCheckoutAction(AppContext appContext,
                             GitLocalizationConstant localization,
                             NotificationManager notificationManager,
                             GitServiceClient gitServiceClient,
                             DtoFactory dtoFactory) {
        this.appContext = appContext;
        this.localization = localization;
        this.notificationManager = notificationManager;
        this.gitServiceClient = gitServiceClient;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getParameters() == null) {
            notificationManager.notify(localization.gitCheckoutImpossibleWithoutParams(),
                                       StatusNotification.Status.FAIL,
                                       false);
            return;
        }
        final String wsId = appContext.getWorkspaceId();
        final String project = event.getParameters().get("project");
        if (project == null) {
            notificationManager.notify(localization.gitCheckoutParameterRequired("Project path "),
                                       StatusNotification.Status.FAIL,
                                       false);
            return;
        }
        final String branchName = event.getParameters().get("branch");
        if (branchName == null) {
            notificationManager.notify(localization.gitCheckoutParameterRequired("Branch name "),
                                       StatusNotification.Status.FAIL,
                                       false);
            return;
        }
        final String startPoint = event.getParameters().get("startPoint");
        final CheckoutRequest request = dtoFactory.createDto(CheckoutRequest.class).withName(branchName);
        gitServiceClient.branchList(wsId, project, BranchListRequest.LIST_ALL)
                        .then(new Operation<List<Branch>>() {
                            @Override
                            public void apply(List<Branch> branches) throws OperationException {
                                for (Branch branch : branches) {
                                    if (branchName.equals(branch.getName())) {
                                        checkout(wsId, project, request, localization.gitCheckoutWithoutCreation(branchName));
                                        return;
                                    }
                                }
                                request.withCreateNew(true);
                                if (startPoint != null) {
                                    request.setStartPoint(startPoint);
                                    checkout(wsId, project, request, localization.gitCheckoutWithCreation(branchName, startPoint));
                                    return;
                                }
                                checkout(wsId, project, request, localization.gitCheckoutWithCreationFromDefault(branchName));
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                notificationManager.notify(arg.getMessage(), StatusNotification.Status.FAIL, false);
                            }
                        });
    }

    private void checkout(String wsId, String projectPath, final CheckoutRequest request, final String msg) {
        gitServiceClient.checkout(wsId, projectPath, request)
                        .then(new Operation<Void>() {
                            @Override
                            public void apply(Void arg) throws OperationException {
                                notificationManager.notify(msg, StatusNotification.Status.SUCCESS, true);
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError error) throws OperationException {
                                notificationManager.notify(localization.gitCheckoutError(request.getName(), request.getStartPoint()),
                                                           StatusNotification.Status.FAIL,
                                                           true);
                            }
                        });
    }
}
