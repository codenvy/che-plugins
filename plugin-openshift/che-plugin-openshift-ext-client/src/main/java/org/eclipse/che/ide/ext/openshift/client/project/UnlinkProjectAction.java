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
package org.eclipse.che.ide.ext.openshift.client.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for unlink Che project from OpenShift.
 *
 * @author Sergii Leschenko
 */
@Singleton
public class UnlinkProjectAction extends AbstractPerspectiveAction {

    private final AnalyticsEventLogger          eventLogger;
    private final AppContext                    appContext;
    private final ProjectServiceClient          projectServiceClient;
    private final DtoUnmarshallerFactory        unmarshallerFactory;
    private final NotificationManager           notificationManager;
    private final OpenshiftLocalizationConstant locale;

    @Inject
    public UnlinkProjectAction(AnalyticsEventLogger eventLogger,
                               AppContext appContext,
                               ProjectServiceClient projectServiceClient,
                               DtoUnmarshallerFactory unmarshallerFactory,
                               NotificationManager notificationManager,
                               OpenshiftLocalizationConstant locale) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.unlinkProjectActionTitle(), null, null, null);
        this.eventLogger = eventLogger;
        this.appContext = appContext;
        this.projectServiceClient = projectServiceClient;
        this.unmarshallerFactory = unmarshallerFactory;
        this.notificationManager = notificationManager;
        this.locale = locale;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        event.getPresentation().setVisible(currentProject != null);
        event.getPresentation().setEnabled(currentProject != null
                                           && currentProject.getRootProject().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        final ProjectDescriptor projectDescription = appContext.getCurrentProject().getRootProject();
        List<String> mixins = projectDescription.getMixins();
        if (mixins.contains(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID)) {
            mixins.remove(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID);

            Map<String, List<String>> attributes = projectDescription.getAttributes();
            attributes.remove(OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME);
            attributes.remove(OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME);

            projectServiceClient.updateProject(projectDescription.getPath(), projectDescription,
                                               new AsyncRequestCallback<ProjectDescriptor>(
                                                       unmarshallerFactory.newUnmarshaller(ProjectDescriptor.class)) {
                                                   @Override
                                                   protected void onSuccess(ProjectDescriptor result) {
                                                       appContext.getCurrentProject().setRootProject(result);
                                                       notificationManager.showInfo(locale.unlinkProjectSuccessful(result.getName()));
                                                   }

                                                   @Override
                                                   protected void onFailure(Throwable exception) {
                                                       notificationManager
                                                               .showError(locale.unlinkProjectFailed() + " " + exception.getMessage());
                                                   }
                                               });
        }
    }
}
