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
package org.eclipse.che.ide.ext.github.server;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.github.shared.Collaborators;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequest;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestHead;
import org.eclipse.che.ide.ext.github.shared.GitHubPullRequestList;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.ext.github.shared.GitHubRepositoryList;
import org.eclipse.che.ide.ext.github.shared.GitHubUser;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class used for converting kohsuke GitHub instances to DTO objects
 *
 * @author Igor Vinokur
 */
public class GitHubDTOFactory {

    /**
     * Get DTO object of GitHub repositories collection from given repositories list
     * @param ghRepositoriesList collection of repositories from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubRepositoryList getRepositoriesList(PagedIterable<GHRepository> ghRepositoriesList) throws IOException {
        GitHubRepositoryList dtoRepositoriesList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        List<GitHubRepository> dtoRepositories = new ArrayList<>();

        for (GHRepository ghRepository : ghRepositoriesList) {
            dtoRepositories.add(getRepository(ghRepository));
        }

        dtoRepositoriesList.setRepositories(dtoRepositories);

        return dtoRepositoriesList;
    }

    /**
     * Get DTO object of GitHub repositories collection from given repository
     * @param ghRepository repository from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubRepositoryList getRepositoriesList(GHRepository ghRepository) throws IOException {
        GitHubRepositoryList dtoRepositoriesList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        List<GitHubRepository> dtoRepositories = new ArrayList<>();
        dtoRepositories.add(getRepository(ghRepository));

        dtoRepositoriesList.setRepositories(dtoRepositories);

        return dtoRepositoriesList;
    }

    /**
     * Get DTO object of GitHub repositories without repositories
     * @return DTO object
     * @throws IOException
     */
    public GitHubRepositoryList getRepositoriesList() throws IOException {
        GitHubRepositoryList dtoRepositoriesList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        return dtoRepositoriesList;
    }

    /**
     * Get DTO object of GitHub repository from given repository
     * @param ghRepository repository from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubRepository getRepository(GHRepository ghRepository) throws IOException {
        GitHubRepository dtoRepository = DtoFactory.getInstance().createDto(GitHubRepository.class);

        dtoRepository.setName(ghRepository.getName());
        dtoRepository.setUrl(String.valueOf(ghRepository.getUrl()));
        dtoRepository.setHomepage(ghRepository.getHomepage());
        dtoRepository.setForks(ghRepository.getForks());
        dtoRepository.setLanguage(ghRepository.getLanguage());
        dtoRepository.setFork(ghRepository.isFork());
        dtoRepository.setWatchers(ghRepository.getWatchers());
        dtoRepository.setPrivateRepo(ghRepository.isPrivate());
        dtoRepository.setSize(ghRepository.getSize());
        dtoRepository.setDescription(ghRepository.getDescription());
        dtoRepository.setSshUrl(ghRepository.getSshUrl());
        dtoRepository.setHtmlUrl(ghRepository.gitHttpTransportUrl());
        dtoRepository.setUpdatedAt(String.valueOf(ghRepository.getUpdatedAt()));
        dtoRepository.setGitUrl(ghRepository.getGitTransportUrl());
        dtoRepository.setHasWiki(ghRepository.hasWiki());
        dtoRepository.setCloneUrl(String.valueOf(ghRepository.getUrl()));
        dtoRepository.setSvnUrl(ghRepository.getSvnUrl());
        dtoRepository.setOpenedIssues(ghRepository.getOpenIssueCount());
        dtoRepository.setCreatedAt(String.valueOf(ghRepository.getCreatedAt()));
        dtoRepository.setPushedAt(String.valueOf(ghRepository.getPushedAt()));
        dtoRepository.setHasDownloads(ghRepository.hasDownloads());
        dtoRepository.setHasIssues(ghRepository.hasIssues());

        return dtoRepository;
    }

    /**
     * Get DTO object of GitHub pull-requests collection from given pull-requests
     * @param ghPullRequestsList collection of pull-requests from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubPullRequestList getPullRequestsList(PagedIterable<GHPullRequest> ghPullRequestsList) throws IOException {
        GitHubPullRequestList gitHubPullRequestList = DtoFactory.getInstance().createDto(GitHubPullRequestList.class);

        List<GitHubPullRequest> dtoPullRequestsList = new ArrayList<>();

        for (GHPullRequest ghPullRequest : ghPullRequestsList) {
            dtoPullRequestsList.add(getPullRequest(ghPullRequest));
        }

        gitHubPullRequestList.setPullRequests(dtoPullRequestsList);

        return gitHubPullRequestList;
    }

    /**
     * Get DTO object of GitHub pull-requests collection from given pull-request
     * @param ghPullRequest pull-request from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubPullRequestList getPullRequestsList(GHPullRequest ghPullRequest) throws IOException {
        GitHubPullRequestList gitHubPullRequestList = DtoFactory.getInstance().createDto(GitHubPullRequestList.class);

        List<GitHubPullRequest> dtoPullRequestsList = new ArrayList<>();
        dtoPullRequestsList.add(getPullRequest(ghPullRequest));


        gitHubPullRequestList.setPullRequests(dtoPullRequestsList);

        return gitHubPullRequestList;
    }

    /**
     * Get DTO object of GitHub pull-request from given pull-request
     * @param ghPullRequest pull-request from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubPullRequest getPullRequest(GHPullRequest ghPullRequest) throws IOException {
        GitHubPullRequest dtoPullRequest = DtoFactory.getInstance().createDto(GitHubPullRequest.class);

        dtoPullRequest.setId(String.valueOf(ghPullRequest.getId()));
        dtoPullRequest.setUrl(String.valueOf(ghPullRequest.getUrl()));
        dtoPullRequest.setHtmlUrl(String.valueOf(ghPullRequest.getHtmlUrl()));
        dtoPullRequest.setNumber(String.valueOf(ghPullRequest.getNumber()));
        dtoPullRequest.setState(ghPullRequest.getState().toString());
        dtoPullRequest.setHead(getPullRequestHead(ghPullRequest.getHead()));
        dtoPullRequest.setMerged(ghPullRequest.isMerged());
        if (ghPullRequest.getMergedBy() != null) {
            dtoPullRequest.setMergedBy(getUser(ghPullRequest.getMergedBy()));
        }
        dtoPullRequest.setMergeable(ghPullRequest.getMergeable());

        return dtoPullRequest;
    }

    /**
     * Get DTO object of GitHub collaborators collection from given users
     * @param ghCollaborators collection of users from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public Collaborators getCollaborators(GHPersonSet<GHUser> ghCollaborators) throws IOException {
        Collaborators collaborators = DtoFactory.getInstance().createDto(Collaborators.class);

        for (GHUser collaborator : ghCollaborators) {
            collaborators.getCollaborators().add(getUser(collaborator));
        }

        return collaborators;
    }

    /**
     * Get DTO object of GitHub user from given user
     * @param ghUser user from kohsuke GitHub library
     * @return DTO object
     * @throws IOException
     */
    public GitHubUser getUser(GHUser ghUser) throws IOException {
        GitHubUser dtoUser = DtoFactory.getInstance().createDto(GitHubUser.class);

        dtoUser.setId(String.valueOf(ghUser.getId()));
        dtoUser.setHtmlUrl(ghUser.getHtmlUrl());
        dtoUser.setAvatarUrl(ghUser.getAvatarUrl());
        dtoUser.setBio(ghUser.getBlog());
        dtoUser.setCompany(ghUser.getCompany());
        dtoUser.setEmail(ghUser.getEmail());
        dtoUser.setFollowers(ghUser.getFollowersCount());
        dtoUser.setFollowing(ghUser.getFollowingCount());
        dtoUser.setLocation(ghUser.getLocation());
        dtoUser.setLogin(ghUser.getLogin());
        dtoUser.setName(ghUser.getName());
        dtoUser.setPublicGists(ghUser.getPublicGistCount());
        dtoUser.setPublicRepos(ghUser.getPublicRepoCount());
        dtoUser.setUrl(String.valueOf(ghUser.getUrl()));
        dtoUser.setGravatarId(ghUser.getGravatarId());

        return dtoUser;
    }

    /**
     * Get DTO object of GitHub pull-request head from given pull-request head
     * @param ghPullRequestHead pull-request head from kohsuke GitHub library
     * @return DTO object
     */
    public GitHubPullRequestHead getPullRequestHead(GHCommitPointer ghPullRequestHead){
        GitHubPullRequestHead dtoPullRequestHead = DtoFactory.getInstance().createDto(GitHubPullRequestHead.class);

        dtoPullRequestHead.setLabel(ghPullRequestHead.getLabel());
        dtoPullRequestHead.setRef(ghPullRequestHead.getRef());
        dtoPullRequestHead.setSha(ghPullRequestHead.getSha());

        return dtoPullRequestHead;
    }

}
