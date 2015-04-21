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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.github.server.GitHubDTOFactory;
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
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

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
 * @author Igor vinokur
 */
@Path("github")
public class GitHubService {
    @Inject
    private GitHubFactory gitHubFactory;

    @Inject
    private GitHubDTOFactory gitHubDTOFactory;

    @Inject
    private GitHubKeyUploader githubKeyUploader;

    @Inject
    private SshKeyStore sshKeyStore;

    @GET
    @Path("repositories/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository getUserRepository(@PathParam("user") String user, @PathParam("repository") String repository)
            throws ApiException {
        try {
            return gitHubDTOFactory.createRepository(gitHubFactory.connect().getUser(user).getRepository(repository));
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("list/user")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByUser(@QueryParam("username") String userName) throws ApiException {
        try {
            return gitHubDTOFactory.createRepositoriesList(gitHubFactory.connect().getUser(userName).listRepositories());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("list/org")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByOrganization(@QueryParam("organization") String organization) throws ApiException {
        try {
            return gitHubDTOFactory.createRepositoriesList(gitHubFactory.connect().getOrganization(organization).listRepositories());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("list/account")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByAccount(@QueryParam("account") String account) throws ApiException {
        GitHub gitHub = gitHubFactory.connect();
        try {
            //First, try to retrieve organization repositories:
            return gitHubDTOFactory.createRepositoriesList(gitHub.getOrganization(account).listRepositories());
        } catch (IOException ioException) {
            //If account is not organization, then try by user name:
            try {
                return gitHubDTOFactory.createRepositoriesList(gitHub.getUser(account).listRepositories());
            } catch (IOException exception) {
                throw new ServerException(exception.getMessage());
            }
        }
    }

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositories() throws ApiException {
        try {
            return gitHubDTOFactory.createRepositoriesList(gitHubFactory.connect().getMyself().listRepositories());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("forks/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList forks(@PathParam("user") String user, @PathParam("repository") String repository) throws ApiException {
        GitHubRepositoryList gitHubRepositoryList;
        try {
            gitHubRepositoryList = gitHubDTOFactory.createRepositoriesList();
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

        PagedIterable<GHRepository> repositories;
        try {
            repositories = gitHubFactory.connect().getMyself().listRepositories();
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

        for (GHRepository ghRepository : repositories) {
            if (ghRepository.isFork() && ghRepository.getName().equals(repository)) {
                try {
                    gitHubRepositoryList = gitHubDTOFactory.createRepositoriesList(ghRepository);
                } catch (IOException e) {
                    throw new ServerException(e.getMessage());
                }
                break;
            }
        }

        return gitHubRepositoryList;
    }

    @GET
    @Path("createfork/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository fork(@PathParam("user") String user, @PathParam("repository") String repository) throws ApiException {
        try {
            return gitHubDTOFactory.createRepository(gitHubFactory.connect().getUser(user).getRepository(repository).fork());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @POST
    @Path("issuecomments/{user}/{repository}/{issue}")
    @Produces(MediaType.APPLICATION_JSON)
    public void commentIssue(@PathParam("user") String user,
                             @PathParam("repository") String repository,
                             @PathParam("issue") String issue,
                             GitHubIssueCommentInput input) throws ApiException {
        try {
            gitHubFactory.connect().getUser(user).getRepository(repository).getIssue(Integer.getInteger(issue)).comment(input.getBody());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("pullrequests/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequestList listPullRequestsByRepository(@PathParam("user") String user, @PathParam("repository") String repository)
            throws ApiException {
        try {
            return gitHubDTOFactory.createPullRequestsList(gitHubFactory.connect().getUser(user).getRepository(repository)
                                                                        .listPullRequests(GHIssueState.OPEN));
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("pullrequests/{user}/{repository}/{pullRequestId}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequestList getPullRequestsById(@PathParam("user") String user, @PathParam("repository") String repository, @PathParam("pullRequestId") String pullRequestId)
            throws ApiException {
        try {
            return gitHubDTOFactory.createPullRequestsList(gitHubFactory.connect().getUser(user).getRepository(repository)
                                                                        .getPullRequest(Integer.valueOf(pullRequestId)));
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

    }

    @POST
    @Path("pullrequest/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequest createPullRequest(@PathParam("user") String user, @PathParam("repository") String repository, GitHubPullRequestCreationInput input)
            throws ApiException {
        try {
            return gitHubDTOFactory.createPullRequest(gitHubFactory.connect().getUser(user).getRepository(repository).createPullRequest(
                    input.getTitle(), input.getHead(), input.getBase(), input.getBody()));
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("list/available")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<GitHubRepository>> availableRepositories() throws ApiException {
        GitHub gitHub = gitHubFactory.connect();

        //Get users' repositories
        GitHubRepositoryList gitHubRepositoryList;
        try {
            gitHubRepositoryList = gitHubDTOFactory.createRepositoriesList(gitHub.getMyself().listRepositories());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

        Map<String, List<GitHubRepository>> repoList = new HashMap<>();
        repoList.put(getUserInfo().getLogin(), gitHubRepositoryList.getRepositories());

        //Get other repositories from all organizations that user's belong to
        GHPersonSet<GHOrganization> allOrganizations;
        try {
            allOrganizations = gitHub.getMyself().getAllOrganizations();
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

        for (GHOrganization ghOrganization : allOrganizations) {
            String organizationName = ghOrganization.getLogin();
            try {
                repoList.put(organizationName, gitHubDTOFactory.createRepositoriesList(
                        gitHub.getOrganization(organizationName).listRepositories()).getRepositories());
            } catch (IOException e) {
                throw new ServerException(e.getMessage());
            }
        }

        return repoList;
    }

    @GET
    @Path("orgs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listOrganizations() throws ApiException {
        List<String> organizations = new ArrayList<>();

        GHPersonSet<GHOrganization> myOrganizations;
        try {
            myOrganizations = gitHubFactory.connect().getMyself().getAllOrganizations();
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }

        for (GHOrganization ghOrganization : myOrganizations) {
            organizations.add(ghOrganization.getLogin());
        }

        return organizations;
    }

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubUser getUserInfo() throws ApiException {
        try {
            return gitHubDTOFactory.createUser(gitHubFactory.connect().getMyself());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @GET
    @Path("collaborators/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collaborators collaborators(@PathParam("user") String user, @PathParam("repository") String repository) throws ApiException {
        try {
            return gitHubDTOFactory.createCollaborators(gitHubFactory.connect().getUser(user).getRepository(repository).getCollaborators());
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * @deprecated use necessary method from rest service
     */
    @GET
    @Deprecated
    @Path("token/{userid}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getToken(@PathParam("userid") String userId) throws ApiException {
        return gitHubFactory.getToken(userId);
    }

    @POST
    @Path("ssh/generate")
    public void updateSSHKey() throws ApiException {
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
