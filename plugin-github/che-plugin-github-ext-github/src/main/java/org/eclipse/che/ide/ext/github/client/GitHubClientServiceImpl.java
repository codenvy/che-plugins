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

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.ext.github.shared.Collaborators;
import org.eclipse.che.ide.ext.github.shared.GitHubIssueComment;
import org.eclipse.che.ide.ext.github.shared.GitHubIssueCommentInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequest;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestCreationInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestList;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.ext.github.shared.GitHubRepositoryList;
import org.eclipse.che.ide.ext.github.shared.GitHubUser;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation for {@link GitHubClientService}.
 *
 * @author Oksana Vereshchaka
 * @author Stéphane Daviet
 */
@Singleton
public class GitHubClientServiceImpl implements GitHubClientService {
    private static final String LIST           = "/list";
    private static final String LIST_ACCOUNT   = "/list/account";
    private static final String LIST_ORG       = "/list/org";
    private static final String LIST_USER      = "/list/user";
    private static final String LIST_ALL       = "/list/available";
    private static final String COLLABORATORS  = "/collaborators";
    private static final String ORGANIZATIONS  = "/orgs";
    private static final String TOKEN          = "/token";
    private static final String USER           = "/user";
    private static final String SSH_GEN        = "/ssh/generate";
    private static final String FORKS          = "/forks";
    private static final String CREATE_FORK    = "/createfork";
    private static final String PULL_REQUEST   = "/pullrequest";
    private static final String PULL_REQUESTS  = "/pullrequests";
    private static final String ISSUE_COMMENTS = "/issuecomments";
    private static final String REPOSITORIES   = "/repositories";
    /** REST service context. */
    private final String              baseUrl;
    /** Loader to be displayed. */
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    protected GitHubClientServiceImpl(@Named("restContext") String baseUrl,
                                      AsyncRequestLoader loader,
                                      AsyncRequestFactory asyncRequestFactory) {
        this.baseUrl = baseUrl + "/github";
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    @Override
    public void getRepository(@Nonnull String user, @Nonnull String repository, @Nonnull AsyncRequestCallback<GitHubRepository> callback) {
        String url = baseUrl + REPOSITORIES + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesList(@Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String url = baseUrl + LIST;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getForks(@Nonnull String user, @Nonnull String repository,
                         @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String url = baseUrl + FORKS + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void fork(@Nonnull String user, @Nonnull String repository, @Nonnull AsyncRequestCallback<GitHubRepository> callback) {
        String url = baseUrl + CREATE_FORK + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    @Override
    public void commentIssue(@Nonnull String user, @Nonnull String repository, @Nonnull String issue,
                             @Nonnull GitHubIssueCommentInput input, @Nonnull AsyncRequestCallback<GitHubIssueComment> callback) {
        String url = baseUrl + ISSUE_COMMENTS + "/" + user + "/" + repository + "/" + issue;
        asyncRequestFactory.createPostRequest(url, input).loader(loader).send(callback);
    }

    @Override
    public void getPullRequests(@Nonnull String owner, @Nonnull String repository,
                                @Nonnull AsyncRequestCallback<GitHubPullRequestList> callback) {
        String url = baseUrl + PULL_REQUESTS + "/" + owner + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    @Override
    public void getPullRequest(final String owner, final String repository, final String pullRequestId,
                               final AsyncRequestCallback<GitHubPullRequest> callback) {
        String url = baseUrl + PULL_REQUESTS + "/" + owner + "/" + repository + "/" + pullRequestId;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void createPullRequest(@Nonnull String user, @Nonnull String repository, @Nonnull GitHubPullRequestCreationInput input,
                                  @Nonnull AsyncRequestCallback<GitHubPullRequest> callback) {
        String url = baseUrl + PULL_REQUEST + "/" + user + "/" + repository;
        asyncRequestFactory.createPostRequest(url, input).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByUser(String userName, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (userName != null) ? "?username=" + userName : "";
        String url = baseUrl + LIST_USER;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getAllRepositories(@Nonnull AsyncRequestCallback<StringMap<Array<GitHubRepository>>> callback) {
        String url = baseUrl + LIST_ALL;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getCollaborators(@Nonnull String user, @Nonnull String repository, @Nonnull AsyncRequestCallback<Collaborators> callback) {
        String url = baseUrl + COLLABORATORS + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserToken(@Nonnull String user, @Nonnull AsyncRequestCallback<String> callback) {
        String url = baseUrl + TOKEN + "/" + user;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getOrganizations(@Nonnull AsyncRequestCallback<List<String>> callback) {
        String url = baseUrl + ORGANIZATIONS;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserInfo(@Nonnull AsyncRequestCallback<GitHubUser> callback) {
        String url = baseUrl + USER;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByOrganization(String organization, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (organization != null) ? "?organization=" + organization : "";
        String url = baseUrl + LIST_ORG;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByAccount(String account, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (account != null) ? "?account=" + account : "";
        String url = baseUrl + LIST_ACCOUNT;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void updatePublicKey(@Nonnull AsyncRequestCallback<Void> callback) {
        String url = baseUrl + SSH_GEN;
        asyncRequestFactory.createPostRequest(url, null).loader(loader).send(callback);
    }
}
