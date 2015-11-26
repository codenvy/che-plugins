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
package org.eclipse.che.ide.ext.openshift.client.importapp;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriber;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.NameUtils;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presenter, which handles logic for importing OpenShift application to Codenvy.
 *
 * @author Anna Shumilova
 * @author Vitaliy Guliy
 */
public class ImportApplicationPresenter extends ValidateAuthenticationPresenter implements ImportApplicationView.ActionDelegate {

    private final ImportApplicationView               view;
    private final OpenshiftLocalizationConstant       locale;
    private final OpenshiftServiceClient              openShiftClient;
    private final ProjectServiceClient                projectServiceClient;
    private final DtoFactory                          dtoFactory;
    private final ImportProjectNotificationSubscriber importProjectNotificationSubscriber;
    private final DtoUnmarshallerFactory              dtoUnmarshallerFactory;
    private final EventBus                            eventBus;

    private final List<String>                   cheProjects;
    private final List<Project>                  projectList;
    private final Map<String, List<BuildConfig>> buildConfigMap;
    private       BuildConfig                    selectedBuildConfig;

    @Inject
    public ImportApplicationPresenter(OpenshiftLocalizationConstant locale, ImportApplicationView view,
                                      OpenshiftServiceClient openShiftClient,
                                      ProjectServiceClient projectServiceClient,
                                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                      NotificationManager notificationManager,
                                      OpenshiftAuthenticator openshiftAuthenticator,
                                      OpenshiftAuthorizationHandler openshiftAuthorizationHandler,
                                      ImportProjectNotificationSubscriber importProjectNotificationSubscriber,
                                      EventBus eventBus,
                                      DtoFactory dtoFactory) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.view.setDelegate(this);
        this.openShiftClient = openShiftClient;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.importProjectNotificationSubscriber = importProjectNotificationSubscriber;
        this.eventBus = eventBus;
        this.locale = locale;
        projectList = new ArrayList<>();
        buildConfigMap = new HashMap<>();
        cheProjects = new ArrayList<>();
    }

    @Override
    protected void onSuccessAuthentication() {
        projectList.clear();
        buildConfigMap.clear();
        selectedBuildConfig = null;

        view.setErrorMessage("");
        view.setProjectName("");
        view.setProjectDescription("");
        view.setApplicationInfo(null);

        view.enableImportButton(false);
        view.animateImportButton(false);
        view.enableCancelButton(true);

        view.setBuildConfigs(buildConfigMap);
        view.enableBuildConfigs(true);

        view.enableNameField(false);
        view.enableDescriptionField(false);

        view.showView();

        view.showLoadingBuildConfigs("Loading projects...");

        loadCheProjects();
    }

    /**
     * Load che projects for following verifications.
     */
    private void loadCheProjects() {
        projectServiceClient.getProjects(false).then(new Operation<List<ProjectDescriptor>>() {
            @Override
            public void apply(List<ProjectDescriptor> result) throws OperationException {
                cheProjects.clear();
                for (ProjectDescriptor project : result) {
                    cheProjects.add(project.getName());
                }

                loadOpenshiftProjects();
            }
        });
    }

    /**
     * Load OpenShift Project and Application data.
     */
    private void loadOpenshiftProjects() {
        openShiftClient.getProjects().then(new Operation<List<Project>>() {
            @Override
            public void apply(List<Project> result) throws OperationException {
                if (result.isEmpty()) {
                    view.setBuildConfigs(buildConfigMap);
                    return;
                }

                projectList.addAll(result);
                for (Project project : result) {
                    getBuildConfigs(project.getMetadata().getName());
                }
            }
        });
    }

    /**
     * Get OpenShift Build Configs by namespace.
     *
     * @param namespace
     *         namespace
     */
    private void getBuildConfigs(final String namespace) {
        openShiftClient.getBuildConfigs(namespace).then(new Operation<List<BuildConfig>>() {
            @Override
            public void apply(List<BuildConfig> result) throws OperationException {
                buildConfigMap.put(namespace, result);

                if (buildConfigMap.size() == projectList.size()) {
                    view.setBuildConfigs(buildConfigMap);
                }
            }
        });
    }

    @Override
    public void onImportApplicationClicked() {
        view.enableBuildConfigs(false);
        view.enableNameField(false);
        view.enableDescriptionField(false);

        view.enableImportButton(false);
        view.animateImportButton(true);

        view.enableCancelButton(false);
        view.setBlocked(true);

        importProject();
    }

    /**
     * Fills project data and imports project.
     */
    private void importProject() {
        final ProjectConfigDto projectConfig = dtoFactory.createDto(ProjectConfigDto.class)
                                                         .withSource(dtoFactory.createDto(SourceStorageDto.class));

        projectConfig.setName(view.getProjectName());

        Map<String, String> importOptions = new HashMap<String, String>();
        String branch = selectedBuildConfig.getSpec().getSource().getGit().getRef();
        if (branch != null && !branch.isEmpty()) {
            importOptions.put("branch", selectedBuildConfig.getSpec().getSource().getGit().getRef());
        }

        String contextDir = selectedBuildConfig.getSpec().getSource().getContextDir();
        if (contextDir != null && !contextDir.isEmpty()) {
            importOptions.put("keepDirectory", contextDir);
        }

        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        attributes.put(OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME, Arrays.asList(
                selectedBuildConfig.getMetadata().getName()));

        attributes.put(OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME, Arrays.asList(
                selectedBuildConfig.getMetadata().getNamespace()));

        projectConfig.withMixins(Arrays.asList(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID))
                     .withAttributes(attributes);

        projectConfig.getSource().withType("git").withParameters(importOptions)
                     .withLocation(selectedBuildConfig.getSpec().getSource().getGit().getUri());

        projectConfig.withType("blank").withDescription(view.getProjectDescription());

        importProjectNotificationSubscriber.subscribe(view.getProjectName());

        try {
            projectServiceClient.importProject(view.getProjectName(), false, projectConfig.getSource(), new RequestCallback<Void>(
                    dtoUnmarshallerFactory.newWSUnmarshaller(Void.class)) {
                @Override
                protected void onSuccess(final Void result) {
                    createProject(projectConfig);
                }

                @Override
                protected void onFailure(Throwable exception) {
                    createProjectFailure(exception);
                }
            });
        } catch (Exception e) {
            createProjectFailure(e);
        }
    }

    /**
     * Creates Codenvy project from imported sources.
     *
     * @param projectConfig
     *         project config
     */
    private void createProject(ProjectConfigDto projectConfig) {
        try {
            projectServiceClient.updateProject(projectConfig.getName(), projectConfig, new AsyncRequestCallback<ProjectDescriptor>(
                    dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class)) {
                @Override
                protected void onSuccess(ProjectDescriptor result) {
                    view.animateImportButton(false);
                    view.setBlocked(false);
                    view.closeView();

                    eventBus.fireEvent(new CreateProjectEvent(result));

                    if (!result.getProblems().isEmpty()) {
                        eventBus.fireEvent(new ConfigureProjectEvent(result));
                    }

                    importProjectNotificationSubscriber.onSuccess();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    createProjectFailure(exception);
                }
            });
        } catch (Exception e) {
            createProjectFailure(e);
        }
    }

    /**
     * Handles errors when creating a project.
     *
     * @param exception
     *         cause
     */
    private void createProjectFailure(Throwable exception) {
        view.animateImportButton(false);
        view.enableImportButton(true);
        view.enableCancelButton(true);
        view.setBlocked(false);

        view.enableNameField(true);
        view.enableDescriptionField(true);

        view.setErrorMessage(exception.getMessage());
        importProjectNotificationSubscriber.onFailure(exception.getMessage());
    }

    @Override
    public void onCancelClicked() {
        view.closeView();
    }

    @Override
    public void onBuildConfigSelected(BuildConfig buildConfig) {
        selectedBuildConfig = buildConfig;

        if (buildConfig != null) {
            view.enableNameField(true);
            view.enableDescriptionField(true);

            view.setProjectName(buildConfig.getMetadata().getName());
            view.setApplicationInfo(buildConfig);
        }

        view.enableImportButton((buildConfig != null && view.getProjectName() != null && !view.getProjectName().isEmpty()));
    }

    @Override
    public void onProjectNameChanged(String name) {
        view.enableImportButton(selectedBuildConfig != null & isCheProjectNameValid(view.getProjectName()));
    }

    private boolean isCheProjectNameValid(String projectName) {
        if (cheProjects.contains(projectName)) {
            view.showCheProjectNameError(locale.existingProjectNameError());
            return false;
        }
        if (!NameUtils.checkProjectName(projectName)) {
            view.showCheProjectNameError(locale.invalidCheProjectNameError());
            return false;
        }
        view.hideCheProjectNameError();
        return true;
    }

}
