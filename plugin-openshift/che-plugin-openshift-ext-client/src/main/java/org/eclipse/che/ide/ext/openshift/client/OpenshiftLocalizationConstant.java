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
package org.eclipse.che.ide.ext.openshift.client;

import com.google.gwt.i18n.client.Messages;

/**
 * @author Sergii Leschenko
 */
public interface OpenshiftLocalizationConstant extends Messages {
    @Key("authorization.request.title")
    String authorizationRequestTitle();

    @Key("authorization.request.message")
    String authorizationRequestMessage();

    @Key("openshift.connect.account.title")
    String connectAccountTitle();

    @Key("openshift.disconnect.account.title")
    String disconnectAccountTitle();

    @Key("openshift.login.successful")
    String loginSuccessful();

    @Key("openshift.login.failed")
    String loginFailed();

    @Key("openshift.logout.successful")
    String logoutSuccessful();

    @Key("openshift.logout.failed")
    String logoutFailed();


    @Key("create.from.template.view.title")
    String createFromTemplateViewTitle();

    @Key("create.from.template.success")
    String createFromTemplateSuccess();

    @Key("create.from.template.failed")
    String createFromTemplateFailed();

    @Key("link.with.existing.action")
    String linkProjectWithExistingApplicationAction();

    @Key("link.with.existing.view.title")
    String linkProjectWithExistingApplicationViewTitle();

    @Key("link.with.existing.link.button")
    String linkProjectWithExistingApplicationLinkButton();

    @Key("link.with.existing.buildconfig.url")
    String linkProjectWithExistingBuildConfigUrl();

    @Key("link.with.existing.remote.url")
    String linkProjectWithExistingRemoteUrl();

    @Key("link.with.existing.replace.warning")
    String linkProjectWithExistingReplaceWarning(String application, String project);

    @Key("link.with.existing.update.buildconfig.success")
    String linkProjectWithExistingUpdateBuildConfigSuccess(String application);

    @Key("link.with.existing.success")
    String linkProjectWithExistingSuccess(String project, String application);
    
    @Key("new.application.action")
    String newApplicationAction();

    @Key("import.application.action")
    String importApplicationAction();

    @Key("import.application.view.title")
    String importApplicationViewTitle();

    @Key("import.application.import.button")
    String importApplicationImportButton();

    @Key("import.application.info")
    String importApplicationInfo();

    @Key("import.application.source.url")
    String importApplicationSourceUrl();

    @Key("import.application.branch.name")
    String importApplicationBranchName();

    @Key("import.application.context.dir")
    String importApplicationContextDir();

    @Key("not.git.repository.warning")
    String notGitRepositoryWarning(String project);

    @Key("not.git.repository.warning.title")
    String notGitRepositoryWarningTitle();

    @Key("no.git.remote.repositories.warning")
    String noGitRemoteRepositoryWarning(String project);

    @Key("no.git.remote.repositories.warning.title")
    String noGitRemoteRepositoryWarningTitle();

    @Key("get.git.remote.repositories.error")
    String getGitRemoteRepositoryError(String project);

    @Key("show.application.url.tooltip")
    String showApplicationUrlTooltip();

    @Key("application.url.title")
    String applicationURLWindowTitle();

    @Key("application.urls.title")
    String applicationURLsWindowTitle();

    @Key("no.application.url")
    String noApplicationUrlLabel();

    @Key("button.close")
    String buttonClose();

    @Key("get.routes.error")
    String getRoutesError();

    @Key("show.webhooks.tooltip")
    String showWebhooksTooltip();

    @Key("webhook.url.title")
    String webhookWindowTitle();

    @Key("webhooks.urls.title")
    String webhooksWindowTitle();

    @Key("no.webhook.url")
    String noWebhookLabel();

    @Key("webhook.url.label.title")
    String webhookURLLabelTitle();

    @Key("webhook.secret.label.title")
    String webhookSecretLabelTitle();

    @Key("start.build.title")
    String startBuildTitle();

    @Key("no.buildconfigs.error")
    String noBuildConfigError();

    @Key("start.build.error")
    String startBuildError();

    @Key("build.status.running")
    String buildStatusRunning(String buildName);

    @Key("build.status.completed")
    String buildStatusCompleted(String buildName);

    @Key("build.status.failed")
    String buildStatusFailed(String buildName);

    @Key("failed.to.retrieve.token.message")
    String failedToRetrieveTokenMessage(String buildName);

    @Key("failed.to.watch.build.by.websocket")
    String failedToWatchBuildByWebSocket(String buildName);

    @Key("unlink.project.action.title")
    String unlinkProjectActionTitle();

    @Key("unlink.project.successful")
    String unlinkProjectSuccessful(String project);

    @Key("unlink.project.failed")
    String unlinkProjectFailed();

    @Key("deploy.project.window.title")
    String deployProjectWindowTitle();

    @Key("deploy.project.window.deploy")
    String deployProjectWindowDeploy();

    @Key("deploy.project.window.application.section")
    String deployProjectWindowApplicationSection();

    @Key("deploy.project.window.application.name")
    String deployProjectWindowApplicationName();

    @Key("deploy.project.window.create.openshift.project")
    String deployProjectWindowCreateOpenShiftProject();

    @Key("deploy.project.window.project.name")
    String deployProjectWindowProjectName();

    @Key("deploy.project.window.display.name")
    String deployProjectWindowDisplayName();

    @Key("deploy.project.window.description")
    String deployProjectWindowDescription();

    @Key("deploy.project.window.choose.existing.project")
    String deployProjectWindowChooseExistingProject();

    @Key("deploy.project.window.deploy.section")
    String deployProjectWindowDeploySection();

    @Key("deploy.project.window.build.image")
    String deployProjectWindowBuildImage();

    @Key("deploy.project.window.labels.section")
    String deployProjectWindowLabelsSection();

    @Key("deploy.project.window.variables.section")
    String deployProjectWindowVariablesSection();

    @Key("deploy.project.window.variables.add.tooltip")
    String deployProjectWindowVariablesAddTooltip();

    @Key("deploy.project.window.labels.add.tooltip")
    String deployProjectWindowLabelsAddTooltip();

    @Key("deploy.project.success")
    String deployProjectSuccess(String project);

    @Key("invalid.openshift.project.name.error")
    String invalidOpenShiftProjectNameError();

    @Key("existing.project.name.error")
    String existingProjectNameError();

    @Key("invalid.che.project.name.error")
    String invalidCheProjectNameError();

    @Key("retrieving.projects.data")
    String retrievingProjectsData();

    @Key("delete.project.action.description")
    String deleteProjectActionDescription();

    @Key("delete.project.action")
    String deleteProjectAction();

    @Key("delete.project.dialog.title")
    String deleteProjectDialogTitle();

    @Key("delete.project.without.app.label")
    String deleteProjectWithoutAppLabel(String projectName);

    @Key("delete.single.app.project.label")
    String deleteSingleAppProjectLabel(String projectName);

    @Key("delete.multiple.app.project.label")
    String deleteMultipleAppProjectLabel(String projectName, String applications);

    @Key("delete.project.failed")
    String deleteProjectFailed(String projectName);

    @Key("delete.project.success")
    String deleteProjectSuccess(String projectName);

    @Key("project.successfully.reset")
    String projectSuccessfullyReset(String cheProjectName);

    @Key("project.is.not.linked.to.openshift.error")
    String projectIsNotLinkedToOpenShiftError(String projectName);
}
