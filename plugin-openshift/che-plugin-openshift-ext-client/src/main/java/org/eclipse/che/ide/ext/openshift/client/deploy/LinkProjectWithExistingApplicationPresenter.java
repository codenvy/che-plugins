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
package org.eclipse.che.ide.ext.openshift.client.deploy;

import com.google.inject.Inject;

import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
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
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presenter, which handles logic for linking current project with OpenShift application.
 *
 * @author Anna Shumilova
 */
public class LinkProjectWithExistingApplicationPresenter extends ValidateAuthenticationPresenter
        implements LinkProjectWithExistingApplicationView.ActionDelegate {

    private final LinkProjectWithExistingApplicationView view;
    private final AppContext                             appContext;
    private final OpenshiftLocalizationConstant          locale;
    private final NotificationManager                    notificationManager;
    private final DialogFactory                          dialogFactory;
    private final OpenshiftServiceClient                 openShiftClient;
    private final ProjectServiceClient                   projectServiceClient;
    private final GitServiceClient                       gitService;
    private final DtoUnmarshallerFactory                 dtoUnmarshaller;
    private final Map<String, List<BuildConfig>>         buildConfigMap;
    private       BuildConfig                            selectedBuildConfig;


    @Inject
    public LinkProjectWithExistingApplicationPresenter(OpenshiftLocalizationConstant locale,
                                                       LinkProjectWithExistingApplicationView view,
                                                       OpenshiftServiceClient openShiftClient,
                                                       ProjectServiceClient projectServiceClient,
                                                       DtoUnmarshallerFactory dtoUnmarshaller,
                                                       GitServiceClient gitService,
                                                       NotificationManager notificationManager,
                                                       DialogFactory dialogFactory,
                                                       AppContext appContext,
                                                       DtoFactory dtoFactory,
                                                       OpenshiftAuthenticator openshiftAuthenticator,
                                                       OpenshiftAuthorizationHandler openshiftAuthorizationHandler) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.view.setDelegate(this);

        this.appContext = appContext;
        this.locale = locale;
        this.notificationManager = notificationManager;
        this.dialogFactory = dialogFactory;
        this.openShiftClient = openShiftClient;
        this.projectServiceClient = projectServiceClient;
        this.gitService = gitService;
        this.dtoUnmarshaller = dtoUnmarshaller;

        buildConfigMap = new HashMap<>();
    }

    /**
     * Show dialog box for managing linking project to OpenShift application.
     */
    @Override
    protected void onSuccessAuthentication() {
        if (appContext.getCurrentProject() != null) {
            //Check is Git repository:
            ProjectDescriptor projectDescription = appContext.getCurrentProject().getRootProject();
            List<String> listVcsProvider = projectDescription.getAttributes().get("vcs.provider.name");
            if (listVcsProvider != null && !listVcsProvider.isEmpty() && listVcsProvider.contains("git")) {
                getGitRemoteRepositories(projectDescription);
            } else {
                dialogFactory.createMessageDialog(locale.notGitRepositoryWarningTitle(),
                                                  locale.notGitRepositoryWarning(projectDescription.getName()),
                                                  null).show();
            }
        }
    }

    /**
     * Retrieve Git remote repositories of the current project.
     */
    private void getGitRemoteRepositories(final ProjectDescriptor project) {
        gitService.remoteList(project, null, true,
                              new AsyncRequestCallback<List<Remote>>(dtoUnmarshaller.newListUnmarshaller(Remote.class)) {
                                  @Override
                                  protected void onSuccess(List<Remote> result) {
                                      if (!result.isEmpty()) {
                                          view.setGitRemotes(result);
                                          prepareView();
                                          loadOpenShiftData();
                                      } else {
                                          dialogFactory.createMessageDialog(locale.noGitRemoteRepositoryWarningTitle(),
                                                                            locale.noGitRemoteRepositoryWarning(project.getName()),
                                                                            null).show();
                                      }
                                  }

                                  @Override
                                  protected void onFailure(Throwable exception) {
                                      notificationManager.showError(locale.getGitRemoteRepositoryError(project.getName()));
                                  }
                              });
    }

    /**
     * Prepare the view state to be shown.
     */
    private void prepareView() {
        selectedBuildConfig = null;
        view.enableLinkButton(false);
        view.setBuildConfigGitUrl("");
        view.showView();
    }

    @Override
    public void onLinkApplicationClicked() {
        String remoteUrl = view.getGitRemoteUrl();
        //Change location in existing
        selectedBuildConfig.getSpec().getSource().getGit().setUri(remoteUrl);
        selectedBuildConfig.getSpec().getSource().getGit().setRef(null);
        selectedBuildConfig.getSpec().getSource().setContextDir(appContext.getCurrentProject().getRootProject().getContentRoot());

        updateBuildConfig(selectedBuildConfig);
    }

    /**
     * Update OpenShift Build Config object.
     *
     * @param buildConfig
     *         buildConfig data to be updated
     */
    private void updateBuildConfig(BuildConfig buildConfig) {
        openShiftClient.updateBuildConfig(buildConfig).then(new Operation<BuildConfig>() {
            @Override
            public void apply(BuildConfig result) throws OperationException {
                view.closeView();
                markAsOpenshiftProject(result);
                notificationManager.showInfo(locale.linkProjectWithExistingUpdateBuildConfigSuccess(
                        result.getMetadata().getName()));
            }
        });
    }

    /**
     * Mark current project as OpenShift one.
     *
     * @param buildConfig
     *         OpenShift application info
     */
    private void markAsOpenshiftProject(final BuildConfig buildConfig) {
        final ProjectDescriptor projectDescription = appContext.getCurrentProject().getRootProject();
        List<String> mixins = projectDescription.getMixins();
        if (!mixins.contains(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID)) {
            mixins.add(OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID);
        }

        Map<String, List<String>> attributes = projectDescription.getAttributes();
        attributes.put(OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME, Arrays.asList(
                buildConfig.getMetadata().getName()));

        attributes.put(OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME, Arrays.asList(
                buildConfig.getMetadata().getNamespace()));

        projectDescription.withMixins(mixins)
                          .withType(projectDescription.getType())
                          .withAttributes(attributes);

        projectServiceClient.updateProject(projectDescription.getPath(), projectDescription,
                                           new AsyncRequestCallback<ProjectDescriptor>(
                                                   dtoUnmarshaller.newUnmarshaller(ProjectDescriptor.class)) {
                                               @Override
                                               protected void onSuccess(ProjectDescriptor result) {
                                                   appContext.getCurrentProject().setRootProject(result);
                                                   notificationManager.showInfo(locale.linkProjectWithExistingSuccess(result.getName(),
                                                                                                                      buildConfig
                                                                                                                              .getMetadata()
                                                                                                                              .getName()));
                                               }

                                               @Override
                                               protected void onFailure(Throwable exception) {
                                                   notificationManager.showError(exception.getMessage());
                                               }
                                           });
    }

    @Override
    public void onCancelClicked() {
        view.closeView();
    }

    @Override
    public void onBuildConfigSelected(BuildConfig buildConfig) {
        selectedBuildConfig = buildConfig;

        if (buildConfig != null) {
            view.setBuildConfigGitUrl(buildConfig.getSpec().getSource().getGit().getUri());
            String project = appContext.getCurrentProject().getRootProject().getName();
            view.setReplaceWarningMessage(locale.linkProjectWithExistingReplaceWarning(buildConfig.getMetadata().getName(), project));
        }

        view.enableLinkButton((buildConfig != null));
    }

    /**
     * Load OpenShift Project and Application data.
     */
    private void loadOpenShiftData() {
        openShiftClient.getProjects().then(new Operation<List<Project>>() {
            @Override
            public void apply(List<Project> result) throws OperationException {
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
     */
    private void getBuildConfigs(final String namespace) {
        openShiftClient.getBuildConfigs(namespace).then(new Operation<List<BuildConfig>>() {
            @Override
            public void apply(List<BuildConfig> result) throws OperationException {
                buildConfigMap.put(namespace, result);
                //TODO update by portions?
                view.setBuildConfigs(buildConfigMap);
            }
        });//TODO add catching of error
    }
}
