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
package org.eclipse.che.ide.ext.github.client;

import com.google.gwt.i18n.client.Messages;

/**
 * @author Evgen Vidolob
 */
public interface GitHubLocalizationConstant extends Messages {
    // MESSAGES
    @Key("github.ssh.key.update.failed")
    String gitHubSshKeyUpdateFailed();

    @Key("importProject.message.startWithWhiteSpace")
    String importProjectMessageStartWithWhiteSpace();

    @Key("importProject.message.nameRepoIncorrect")
    String importProjectMessageNameRepoIncorrect();

    @Key("importProject.message.protocolIncorrect")
    String importProjectMessageProtocolIncorrect();

    @Key("importProject.message.hostIncorrect")
    String importProjectMessageHostIncorrect();

    /*
     * ImportFromGitHub
     */
    @Key("import.github.account")
    String importFromGithubAccount();

    //Authorization
    @Key("authorization.title")
    String authTitle();

    @Key("authorization.message.authRequest")
    String authMessageAuthRequest();

    @Key("authorization.message.unableCreateSshKey")
    String authMessageUnableCreateSshKey();

    @Key("authorization.generateKeyLabel")
    String authGenerateKeyLabel();

    @Key("authorization.message.keyUploadSuccess")
    String authMessageKeyUploadSuccess();

    /*
     * SamplesListGrid
     */
    @Key("samplesListGrid.column.name")
    String samplesListRepositoryColumn();

    @Key("samplesListGrid.column.description")
    String samplesListDescriptionColumn();

    @Key("github.sshkey.title")
    String githubSshKeyTitle();

    @Key("github.sshkey.label")
    String githubSshKeyLabel();

    //GithubImporterPage
    @Key("view.import.githubImporterPage.projectUrl")
    String githubImporterPageProjectUrl();

    @Key("view.import.githubImporterPage.recursive")
    String githubImporterPageRecursive();

    @Key("view.import.githubImporterPage.projectInfo")
    String githubImporterPageProjectInfo();

    @Key("view.import.githubImporterPage.projectName")
    String githubImporterPageProjectName();

    @Key("view.import.githubImporterPageProjectNamePrompt")
    String githubImporterPageProjectNamePrompt();

    @Key("view.import.githubImporterPage.projectDescription")
    String githubImporterPageProjectDescription();

    @Key("view.import.githubImporterPage.projectDescriptionPrompt")
    String githubImporterPageProjectDescriptionPrompt();

    @Key("view.import.githubImporterPage.projectPrivacy")
    String githubImporterPageProjectPrivacy();

    @Key("view.import.githubImporterPage.projectVisibilityPublic")
    String githubImporterPageprojectVisibilityPublic();

    @Key("view.import.githubImporterPage.projectVisibilityPrivate")
    String githubImporterPageprojectVisibilityPrivate();

    @Key("view.import.githubImporterPage.keepDirectory")
    String githubImporterPageKeepDirectory();

    @Key("view.import.githubImporterPage.keepDirectoryField")
    String githubImporterPageKeepDirectoryField();

}
