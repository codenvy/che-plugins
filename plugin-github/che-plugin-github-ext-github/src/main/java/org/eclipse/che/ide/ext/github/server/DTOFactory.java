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
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DTOFactory {

    public GitHubRepositoryList createDTORepositoriesList () {
        return DtoFactory.getInstance().createDto(GitHubRepositoryList.class);
    }

    public GitHubPullRequestList createDTOPullRequestsList () {
        return DtoFactory.getInstance().createDto(GitHubPullRequestList.class);
    }

    public Collaborators createDTOCollaborators () {
        return DtoFactory.getInstance().createDto(Collaborators.class);
    }

    public GitHubRepositoryList getRepositoriesList(PagedIterable<GHRepository> ghRepositoriesList) throws IOException {
        GitHubRepositoryList dtoRepositoriesList = DtoFactory.getInstance().createDto(GitHubRepositoryList.class);

        List<GitHubRepository> dtoRepositories = new ArrayList<>();

        for (GHRepository ghRepository : ghRepositoriesList) {
            dtoRepositories.add(getRepository(ghRepository));
        }

        dtoRepositoriesList.setRepositories(dtoRepositories);

        return dtoRepositoriesList;
    }

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

    public GitHubPullRequestList getPullRequestsList(PagedIterable<GHPullRequest> ghPullRequestsList) throws IOException {
        GitHubPullRequestList gitHubPullRequestList = DtoFactory.getInstance().createDto(GitHubPullRequestList.class);

        List<GitHubPullRequest> dtoPullRequestsList = new ArrayList<>();

        for (GHPullRequest ghPullRequest : ghPullRequestsList) {
            dtoPullRequestsList.add(getPullRequest(ghPullRequest));
        }

        gitHubPullRequestList.setPullRequests(dtoPullRequestsList);

        return gitHubPullRequestList;
    }

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

    private GitHubPullRequestHead getPullRequestHead(GHCommitPointer ghPullRequestHead){
        GitHubPullRequestHead dtoPullRequestHead = DtoFactory.getInstance().createDto(GitHubPullRequestHead.class);

        dtoPullRequestHead.setLabel(ghPullRequestHead.getLabel());
        dtoPullRequestHead.setRef(ghPullRequestHead.getRef());
        dtoPullRequestHead.setSha(ghPullRequestHead.getSha());

        return dtoPullRequestHead;
    }

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

}
