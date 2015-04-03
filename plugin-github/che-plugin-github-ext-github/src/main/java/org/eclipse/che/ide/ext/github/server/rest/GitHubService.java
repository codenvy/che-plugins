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

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.github.server.GitHubFactory;
import org.eclipse.che.ide.ext.github.server.GitHubKeyUploader;
import org.eclipse.che.ide.ext.github.shared.Collaborators;
import org.eclipse.che.ide.ext.github.shared.GitHubIssueCommentInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequest;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestCreationInput;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestHead;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestList;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.ext.github.shared.GitHubRepositoryList;
import org.eclipse.che.ide.ext.github.shared.GitHubUser;
import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;

import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
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
 */
@Path("github")
public class GitHubService {
    @Inject
    private GitHubFactory gitHubFactory;

    @Inject
    private GitHubKeyUploader githubKeyUploader;

    @Inject
    private SshKeyStore sshKeyStore;

    @Path("repositories/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository getUserRepository(@PathParam("user") String user, @PathParam("repository") String repository)
            throws IOException {
        return getDTORepository(gitHubFactory.connect().getUser(user).getRepository(repository));
    }

    @Path("list/user")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByUser(@QueryParam("username") String userName) throws IOException {
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
        gitHubRepositoryList.getRepositories().addAll(getDTORepositorieslist(gitHubFactory.connect().getUser(userName).listRepositories()));

        return gitHubRepositoryList;
    }

    @Path("list/org")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByOrganization(@QueryParam("organization") String organization) throws IOException {
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
        gitHubRepositoryList.getRepositories()
                            .addAll(getDTORepositorieslist(gitHubFactory.connect().getOrganization(organization).listRepositories()));

        return gitHubRepositoryList;
    }

    @Path("list/account")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositoriesByAccount(@QueryParam("account") String account) throws IOException {
        GitHub gitHub = gitHubFactory.connect();

        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
        try {
            //First, try to retrieve organization repositories:
            gitHubRepositoryList.getRepositories().addAll(getDTORepositorieslist(gitHub.getOrganization(account).listRepositories()));
        } catch (IOException ioException) {
            //If account is not organization, then try by user name:
            try {
                gitHubRepositoryList.getRepositories().addAll(getDTORepositorieslist(gitHub.getUser(account).listRepositories()));
            } catch (IOException exception) {}
        }
        return gitHubRepositoryList;
    }

    @Path("list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList listRepositories() throws IOException {
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
        gitHubRepositoryList.getRepositories().addAll(getDTORepositorieslist(gitHubFactory.connect().getMyself().listRepositories()));

        return gitHubRepositoryList;
    }

    @Path("forks/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList forks(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        for (GHRepository ghRepository : gitHubFactory.connect().getMyself().listRepositories()) {
            if (ghRepository.isFork() && ghRepository.getName().equals(repository)) {
                gitHubRepositoryList.getRepositories().add(getDTORepository(ghRepository));
            }
        }

        return gitHubRepositoryList;
    }

    @Path("createfork/{user}/{repository}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepository fork(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        return getDTORepository(gitHubFactory.connect().getUser(user).getRepository(repository).fork());
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
        GitHub gitHub = gitHubFactory.connect();

        GitHubPullRequestList gitHubPullRequestList = DtoFactory.getInstance().createDto(GitHubPullRequestList.class);

        gitHubPullRequestList.getPullRequests().addAll(getDTOPullRequestList(
                gitHub.getUser(user).getRepository(repository).listPullRequests(GHIssueState.OPEN)));

        return gitHubPullRequestList;
    }

    @Path("pullrequests/{user}/{repository}/{pullRequestId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequestList getPullRequestsById(@PathParam("user") String user, @PathParam("repository") String repository, @PathParam("pullRequestId") String pullRequestId)
            throws IOException {
        GitHubPullRequestList gitHubPullRequestList = DtoFactory.getInstance().createDto(GitHubPullRequestList.class);

        gitHubPullRequestList.getPullRequests().add(getDTOPullRequest(
                gitHubFactory.connect().getUser(user).getRepository(repository).getPullRequest(Integer.valueOf(pullRequestId))));

        return gitHubPullRequestList;
    }

    @Path("pullrequest/{user}/{repository}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubPullRequest createPullRequest(@PathParam("user") String user, @PathParam("repository") String repository, GitHubPullRequestCreationInput input)
            throws IOException {
        GitHubPullRequest pullRequest = getDTOPullRequest(gitHubFactory.connect().getUser(user).getRepository(repository)
                                                                       .createPullRequest(input.getTitle(), input.getHead(),
                                                                                          input.getBase(), input.getBody()));

        return pullRequest;
    }

    @Path("list/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<GitHubRepository>> availableRepositories() throws IOException {
        GitHub gitHub = gitHubFactory.connect();

        //Get users' repositories
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
        gitHubRepositoryList.getRepositories().addAll(getDTORepositorieslist(gitHub.getMyself().listRepositories()));

        Map<String, List<GitHubRepository>> repoList = new HashMap<>();
        repoList.put(getUserInfo().getLogin(), gitHubRepositoryList.getRepositories());

        //Get other repositories from all organizations that user's belong to
        for (GHOrganization ghOrganization : gitHub.getMyself().getAllOrganizations()) {
            String organizationName = ghOrganization.getLogin();
            repoList.put(organizationName, getDTORepositorieslist(gitHub.getOrganization(organizationName).listRepositories()));
        }

        return repoList;
    }

    @Path("page")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubRepositoryList getPage(@QueryParam("url") String url) throws IOException {
        GitHubRepositoryList gitHubRepositoryList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        return gitHubRepositoryList;
    }

    @Path("orgs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listOrganizations() throws IOException {
        ArrayList<String> organizations = new ArrayList<>();

        for (GHOrganization organization : gitHubFactory.connect().getMyself().getAllOrganizations()) {
            organizations.add(organization.getLogin());
        }

        return organizations;
    }

    @Path("user")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GitHubUser getUserInfo() throws IOException {
        return getDTOUser(gitHubFactory.connect().getMyself());
    }

    @GET
    @Path("collaborators/{user}/{repository}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collaborators collaborators(@PathParam("user") String user, @PathParam("repository") String repository) throws IOException {
        Collaborators collaborators = DtoFactory.getInstance().createDto(Collaborators.class);

        for (GHUser colaborator : gitHubFactory.connect().getUser(user).getRepository(repository).getCollaborators()) {
            collaborators.getCollaborators().add(getDTOUser(colaborator));
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

    private ArrayList<GitHubRepository> getDTORepositorieslist(PagedIterable<GHRepository> repositories) throws IOException {
        ArrayList<GitHubRepository> dtoRepositories = new ArrayList<>();

        for (GHRepository repository : repositories) {
            dtoRepositories.add(getDTORepository(repository));
        }

        return dtoRepositories;
    }

    private GitHubRepository getDTORepository(GHRepository repository) throws IOException {
        GitHubRepository dtoRepository = DtoFactory.getInstance().createDto(GitHubRepository.class);

        dtoRepository.setName(repository.getName());
        dtoRepository.setUrl(String.valueOf(repository.getUrl()));
        dtoRepository.setHomepage(repository.getHomepage());
        dtoRepository.setForks(repository.getForks());
        dtoRepository.setLanguage(repository.getLanguage());
        dtoRepository.setFork(repository.isFork());
        dtoRepository.setWatchers(repository.getWatchers());
        dtoRepository.setPrivateRepo(repository.isPrivate());
        dtoRepository.setSize(repository.getSize());
        dtoRepository.setDescription(repository.getDescription());
        dtoRepository.setSshUrl(repository.getSshUrl());
        dtoRepository.setHtmlUrl(repository.gitHttpTransportUrl());
        dtoRepository.setUpdatedAt(String.valueOf(repository.getUpdatedAt()));
        dtoRepository.setGitUrl(repository.getGitTransportUrl());
        dtoRepository.setHasWiki(repository.hasWiki());
        dtoRepository.setCloneUrl(String.valueOf(repository.getUrl()));
        dtoRepository.setSvnUrl(repository.getSvnUrl());
        dtoRepository.setOpenedIssues(repository.getOpenIssueCount());
        dtoRepository.setCreatedAt(String.valueOf(repository.getCreatedAt()));
        dtoRepository.setPushedAt(String.valueOf(repository.getPushedAt()));
        dtoRepository.setHasDownloads(repository.hasDownloads());
        dtoRepository.setHasIssues(repository.hasIssues());

        return dtoRepository;
    }

    private ArrayList<GitHubPullRequest> getDTOPullRequestList(PagedIterable<GHPullRequest> pullRequests) throws IOException {
        ArrayList<GitHubPullRequest> dtoPullRequestList = new ArrayList<>();

        for (GHPullRequest pullRequest : pullRequests) {
            dtoPullRequestList.add(getDTOPullRequest(pullRequest));
        }

        return dtoPullRequestList;
    }

    private GitHubPullRequest getDTOPullRequest(GHPullRequest pullRequest) throws IOException {
        GitHubPullRequest dtoPullRequest = DtoFactory.getInstance().createDto(GitHubPullRequest.class);

        dtoPullRequest.setId(String.valueOf(pullRequest.getId()));
        dtoPullRequest.setUrl(String.valueOf(pullRequest.getUrl()));
        dtoPullRequest.setHtmlUrl(String.valueOf(pullRequest.getHtmlUrl()));
        dtoPullRequest.setNumber(String.valueOf(pullRequest.getNumber()));
        dtoPullRequest.setState(pullRequest.getState().toString());
        dtoPullRequest.setHead(getDTOPullRequestHead(pullRequest.getHead()));
        dtoPullRequest.setMerged(pullRequest.isMerged());
        if (pullRequest.getMergedBy() != null) {
            dtoPullRequest.setMergedBy(getDTOUser(pullRequest.getMergedBy()));
        }
        dtoPullRequest.setMergeable(pullRequest.getMergeable());

        return dtoPullRequest;
    }

    private GitHubPullRequestHead getDTOPullRequestHead(GHCommitPointer pullRequestHead){
        GitHubPullRequestHead dtoPullRequestHead = DtoFactory.getInstance().createDto(GitHubPullRequestHead.class);

        dtoPullRequestHead.setLabel(pullRequestHead.getLabel());
        dtoPullRequestHead.setRef(pullRequestHead.getRef());
        dtoPullRequestHead.setSha(pullRequestHead.getSha());

        return dtoPullRequestHead;
    }

    private GitHubUser getDTOUser(GHUser user) throws IOException {
        GitHubUser dtoUser = DtoFactory.getInstance().createDto(GitHubUser.class);

        dtoUser.setId(String.valueOf(user.getId()));
        dtoUser.setHtmlUrl(user.getHtmlUrl());
        dtoUser.setAvatarUrl(user.getAvatarUrl());
        dtoUser.setBio(user.getBlog());
        dtoUser.setCompany(user.getCompany());
        dtoUser.setEmail(user.getEmail());
        dtoUser.setFollowers(user.getFollowersCount());
        dtoUser.setFollowing(user.getFollowingCount());
        dtoUser.setLocation(user.getLocation());
        dtoUser.setLogin(user.getLogin());
        dtoUser.setName(user.getName());
        dtoUser.setPublicGists(user.getPublicGistCount());
        dtoUser.setPublicRepos(user.getPublicRepoCount());
        dtoUser.setUrl(String.valueOf(user.getUrl()));
        dtoUser.setGravatarId(user.getGravatarId());

        return dtoUser;
    }
}
