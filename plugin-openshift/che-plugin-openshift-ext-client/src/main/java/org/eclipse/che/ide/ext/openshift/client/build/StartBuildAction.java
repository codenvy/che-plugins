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
package org.eclipse.che.ide.ext.openshift.client.build;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.Build;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * @author Sergii Leschenko
 */
public class StartBuildAction extends AbstractPerspectiveAction {
    private final OpenshiftAuthorizationHandler authorizationHandler;
    private final AppContext                    appContext;
    private final OpenshiftServiceClient        openshiftService;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final BuildStatusWatcher            buildStatusWatcher;

    @Inject
    public StartBuildAction(OpenshiftAuthorizationHandler authorizationHandler,
                            AppContext appContext,
                            OpenshiftServiceClient openshiftService,
                            NotificationManager notificationManager,
                            BuildStatusWatcher buildStatusWatcher,
                            OpenshiftLocalizationConstant locale) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.startBuildTitle(), null, null, null);
        this.authorizationHandler = authorizationHandler;
        this.appContext = appContext;
        this.openshiftService = openshiftService;
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.buildStatusWatcher = buildStatusWatcher;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final ProjectDescriptor projectDescriptor = appContext.getCurrentProject().getRootProject();
        final String namespace = getAttributeValue(projectDescriptor, OPENSHIFT_NAMESPACE_VARIABLE_NAME);
        final String application = getAttributeValue(projectDescriptor, OPENSHIFT_APPLICATION_VARIABLE_NAME);

        openshiftService.getBuildConfigs(namespace, application)
                        .thenPromise(new Function<List<BuildConfig>, Promise<BuildConfig>>() {
                            @Override
                            public Promise<BuildConfig> apply(List<BuildConfig> arg) throws FunctionException {
                                if (arg.isEmpty() || arg.size() > 1) {
                                    throw new FunctionException(locale.noBuildConfigError());
                                }

                                return Promises.resolve(arg.get(0));
                            }
                        })
                        .thenPromise(new Function<BuildConfig, Promise<Build>>() {
                            @Override
                            public Promise<Build> apply(BuildConfig buildConfig) throws FunctionException {
                                return openshiftService.startBuild(namespace, buildConfig.getMetadata().getName());
                            }
                        })
                        .then(new Operation<Build>() {
                            @Override
                            public void apply(Build startedBuild) throws OperationException {
                                buildStatusWatcher.watch(startedBuild);
                            }
                        })
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError arg) throws OperationException {
                                notificationManager.showError(locale.startBuildError() + " " + arg.getMessage());
                            }
                        });
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        event.getPresentation().setVisible(currentProject != null);
        event.getPresentation().setEnabled(authorizationHandler.isLoggedIn()
                                           && currentProject != null
                                           && currentProject.getRootProject().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
    }

    /** Returns first value of attribute of null if it is absent in project descriptor */
    private String getAttributeValue(ProjectDescriptor projectDescriptor, String attribute) {
        final List<String> values = projectDescriptor.getAttributes().get(attribute);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
