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
package org.eclipse.che.ide.ext.openshift.client.webhooks;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHook;

import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;

/**
 * Presenter for showing webhooks of openshift application.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class ShowWebhooksPresenter implements ShowWebhooksView.ActionDelegate {
    private final ShowWebhooksView              view;
    private final OpenshiftServiceClient        service;
    private final AppContext                    appContext;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;
    private final DtoFactory                    dtoFactory;

    @Inject
    public ShowWebhooksPresenter(ShowWebhooksView view,
                                 OpenshiftServiceClient service,
                                 AppContext appContext,
                                 NotificationManager notificationManager,
                                 OpenshiftLocalizationConstant locale,
                                 DtoFactory dtoFactory) {
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.locale = locale;
        this.service = service;
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.view.setDelegate(this);
    }

    @Override
    public void onCloseClicked() {
        view.close();
    }

    public void showDialog() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }
        final ProjectDescriptor projectDescription = currentProject.getRootProject();

        service.getBuildConfigs(getAttributeValue(projectDescription, OPENSHIFT_NAMESPACE_VARIABLE_NAME),
                                getAttributeValue(projectDescription, OPENSHIFT_APPLICATION_VARIABLE_NAME))
               .thenPromise(getWebHooks())
               .then(showWebhooks())
               .catchError(onGetBuildConfigsFailed());
    }

    private Function<List<BuildConfig>, Promise<List<WebHook>>> getWebHooks() {
        return new Function<List<BuildConfig>, Promise<List<WebHook>>>() {
            @Override
            public Promise<List<WebHook>> apply(List<BuildConfig> result) throws FunctionException {
                if (result.isEmpty()) {
                    return Promises.resolve(Collections.<WebHook>emptyList());
                }
                return service.getWebhooks(result.get(0).getMetadata().getNamespace(), result.get(0).getMetadata().getName());
            }
        };
    }

    private Operation<List<WebHook>> showWebhooks() {
        return new Operation<List<WebHook>>() {
            @Override
            public void apply(List<WebHook> webHooks) throws OperationException {
                view.setWebhooks(webHooks);
                view.showDialog();
            }
        };
    }

    private Operation<PromiseError> onGetBuildConfigsFailed() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.showError(locale.getRoutesError() + ". " + serviceError.getMessage());
            }
        };
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
