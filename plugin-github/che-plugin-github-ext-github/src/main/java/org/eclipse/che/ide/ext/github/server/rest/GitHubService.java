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
package org.eclipse.che.ide.ext.github.server.rest;

import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.github.server.DTOFactory;
import org.eclipse.che.ide.ext.github.server.GitHubFactory;
import org.eclipse.che.ide.ext.github.server.GitHubKeyUploader;
import org.eclipse.che.ide.ext.github.shared.Collaborators;
import org.eclipse.che.ide.ext.github.shared.GitHubIssueCommentInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequest;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestCreationInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestList;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.ext.github.shared.GitHubRepositoryList;
import org.eclipse.che.ide.ext.github.shared.GitHubUser;
import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST service to get the list of repositories from GitHub (where sample projects are located).
 *
 * @author Oksana Vereshchaka
 * @author Stéphane Daviet
 * @author Kevin Pollet
 */
@Path("github")
public class GitHubService {
    @Inject
    private GitHubFactory gitHubFactory;

    @Inject
    private DTOFactory dtoFactory;

    @Inject
    private GitHubKeyUploader githubKeyUploader;

    @Inject
    private SshKeyStore sshKeyStore;

    @Path("repositories/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository getUserRepository(@PathParam("user") String user, @PathParam("repository") String repository)
            throws IOException {
        return dtoFactory.getRepository(gitHubFactory.connect().getUser(user).getRepository(repository));
    }

    @Path("list/user")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByUser(@QueryParam("username") String userName) throws IOException {
        return dtoFactory.getRepositoriesList(gitHubFactory.connect().getUser(userName).listRepositories());
    }

    @Path("list/org")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByOrganization(@QueryParam("organization") String organization) throws IOException {
        return dtoFactory.getRepositoriesList(gitHubFactory.connect().getOrganization(organization).listRepositories());
    }

    @Path("list/account")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByAccount(@QueryParam("account") String account) throws IOException {
        GitHub gitHub = gitHubFactory.connect();
        try {
            //First, try to retrieve organization repositories:
            return dtoFactory.getRepositoriesList(gitHub.getOrganization(account).listRepositories());
        } catch (IOException ioException) {
            //If account is not organization, then try by user name:
            try {
                return dtoFactory.getRepositoriesList(gitHub.getUser(account).listRepositories());
            } catch (IOException exception) {
                throw ioException;
            }
        }
    }

    @Path("list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositories() throws IOException {
        return dtoFactory.getRepositoriesList(gitHubFactory.connect().getMyself().listRepositories());
    }

    @Path("forks/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList forks(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        GitHubRepositoryList gitHubRepositoryList = dtoFactory.createDTORepositoriesList();

        for (GHRepository ghRepository : gitHubFactory.connect().getMyself().listRepositories()) {
            if (ghRepository.isFork() && ghRepository.getName().equals(repository)) {
                gitHubRepositoryList.getRepositories().add(dtoFactory.getRepository(ghRepository));
            }
        }

        return gitHubRepositoryList;
    }

    @Path("createfork/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository fork(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        return dtoFactory.getRepository(gitHubFactory.connect().getUser(user).getRepository(repository).fork());
    }

    @Path("issuecomments/{user}/{repository}/{issue}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public void commentIssue(@PathParam("user") String user,
                             @PathParam("repository") String repository,
                             @PathParam("issue") String issue,
                             GitHubIssueCommentInput input) throws IOException {
        gitHubFactory.connect().getUser(user).getRepository(repository).getIssue(Integer.getInteger(issue)).comment(input.getBody());
    }

    @Path("pullrequests/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequestList listPullRequestsByRepository(@PathParam("user") String user, @PathParam("repository") String repository)
            throws IOException {
        return dtoFactory
                .getPullRequestsList(gitHubFactory.connect().getUser(user).getRepository(repository).listPullRequests(GHIssueState.OPEN));
    }

    @Path("pullrequests/{user}/{repository}/{pullRequestId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequestList getPullRequestsById(@PathParam("user") String user, @PathParam("repository") String repository, @PathParam("pullRequestId") String pullRequestId)
            throws IOException {
        GitHubPullRequestList gitHubPullRequestList = dtoFactory.createDTOPullRequestsList();

        gitHubPullRequestList.getPullRequests().add(dtoFactory.getPullRequest(
                gitHubFactory.connect().getUser(user).getRepository(repository).getPullRequest(Integer.valueOf(pullRequestId))));

        return gitHubPullRequestList;
    }

    @Path("pullrequest/{user}/{repository}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequest createPullRequest(@PathParam("user") String user, @PathParam("repository") String repository, GitHubPullRequestCreationInput input)
            throws IOException {
        GitHubPullRequest pullRequest =
                dtoFactory.getPullRequest(gitHubFactory.connect().getUser(user).getRepository(repository).createPullRequest(
                        input.getTitle(), input.getHead(), input.getBase(), input.getBody()));

        return pullRequest;
    }

    @Path("list/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<GitHubRepository>> availableRepositories() throws IOException {
        GitHub gitHub = gitHubFactory.connect();

        //Get users' repositories
        GitHubRepositoryList gitHubRepositoryList = dtoFactory.getRepositoriesList(gitHub.getMyself().listRepositories());

        Map<String, List<GitHubRepository>> repoList = new HashMap<>();
        repoList.put(getUserInfo().getLogin(), gitHubRepositoryList.getRepositories());

        //Get other repositories from all organizations that user's belong to
        for (GHOrganization ghOrganization : gitHub.getMyself().getAllOrganizations()) {
            String organizationName = ghOrganization.getLogin();
            repoList.put(organizationName,
                         dtoFactory.getRepositoriesList(gitHub.getOrganization(organizationName).listRepositories()).getRepositories());
        }

        return repoList;
    }

    @Path("page")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList getPage(@QueryParam("url") String url) throws IOException {
        return dtoFactory.createDTORepositoriesList();
    }

    @Path("orgs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listOrganizations() throws IOException {
        List<String> organizations = new ArrayList<>();

        for (GHOrganization ghOrganization : gitHubFactory.connect().getMyself().getAllOrganizations()) {
            organizations.add(ghOrganization.getLogin());
        }

        return organizations;
    }

    @Path("user")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubUser getUserInfo() throws IOException {
        return dtoFactory.getUser(gitHubFactory.connect().getMyself());
    }

    @GET
    @Path("collaborators/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collaborators collaborators(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        Collaborators collaborators = dtoFactory.createDTOCollaborators();

        for (GHUser colaborator : gitHubFactory.connect().getUser(user).getRepository(repository).getCollaborators()) {
            collaborators.getCollaborators().add(dtoFactory.getUser(colaborator));
        }

        return collaborators;
    }

    @GET
    @Path("token/{userid}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getToken(@PathParam("userid") String userId) throws IOException {
        return gitHubFactory.getToken(userId);
    }

    @POST
    @Path("ssh/generate")
    public void updateSSHKey() throws Exception {
        final String host = "github.com";
        SshKey publicKey;

        try {
            if (sshKeyStore.getPrivateKey(host) != null) {
                publicKey = sshKeyStore.getPublicKey(host);
                if (publicKey == null) {
                    sshKeyStore.removeKeys(host);
                    publicKey = sshKeyStore.genKeyPair(host, null, null).getPublicKey();
                }
            } else {
                publicKey = sshKeyStore.genKeyPair(host, null, null).getPublicKey();
            }
        } catch (SshKeyStoreException e) {
            throw new GitException(e.getMessage(), e);
        }

        // update public key
        try {
            githubKeyUploader.uploadKey(publicKey);
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }
}
