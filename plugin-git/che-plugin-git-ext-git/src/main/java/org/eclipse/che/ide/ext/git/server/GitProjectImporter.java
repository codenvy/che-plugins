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
package org.eclipse.che.ide.ext.git.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.dto.server.DtoFactory;

import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.ext.git.shared.BranchListRequest;
import org.eclipse.che.ide.ext.git.shared.CloneRequest;
import org.eclipse.che.ide.ext.git.shared.FetchRequest;
import org.eclipse.che.ide.ext.git.shared.InitRequest;
import org.eclipse.che.ide.ext.git.shared.RemoteAddRequest;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class GitProjectImporter implements ProjectImporter {

    private final GitConnectionFactory gitConnectionFactory;
    private final LocalPathResolver    localPathResolver;
    private static final Logger LOG = LoggerFactory.getLogger(GitProjectImporter.class);

    @Inject
    public GitProjectImporter(GitConnectionFactory gitConnectionFactory, LocalPathResolver localPathResolver) {
        this.gitConnectionFactory = gitConnectionFactory;
        this.localPathResolver = localPathResolver;
    }

    @Override
    public String getId() {
        return "git";
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Import project from hosted GIT repository URL.";
    }

    /** {@inheritDoc} */
    @Override
    public ImporterCategory getCategory() {
        return ImporterCategory.SOURCE_CONTROL;
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                              LineConsumerFactory consumerFactory)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        GitConnection git = null;
        try {
            // For factory: checkout particular commit after clone
            String commitId = null;
            // For factory: github pull request feature
            String remoteOriginFetch = null;
            String branch = null;
            // For factory or probably for our projects templates:
            // If git repository contains more than one project need clone all repository but after cloning keep just
            // sub-project that is specified in parameter "keepDirectory".
            String keepDirectory = null;
            // For factory and for our projects templates:
            // Keep all info related to the vcs. In case of Git: ".git" directory and ".gitignore" file.
            // Delete vcs info if false.
            String branchMerge = null;
            boolean keepVcs = true;
            if (parameters != null) {
                commitId = parameters.get("commitId");
                branch = parameters.get("branch");
                remoteOriginFetch = parameters.get("remoteOriginFetch");
                keepDirectory = parameters.get("keepDirectory");
                if (parameters.containsKey("keepVcs")) {
                    keepVcs = Boolean.parseBoolean(parameters.get("keepVcs"));
                }
                branchMerge = parameters.get("branchMerge");
            }
            // Get path to local file. Git works with local filesystem only.
            final String localPath = localPathResolver.resolve((VirtualFileImpl)baseFolder.getVirtualFile());
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            if (keepDirectory != null) {
                final File temp = Files.createTempDirectory(null).toFile();
                try {
                    git = gitConnectionFactory.getConnection(temp, consumerFactory);
                    sparsecheckout(git, location, branch == null ? "master" : branch, keepDirectory, dtoFactory);
                    // Copy content of directory to the project folder.
                    final File projectDir = new File(localPath);
                    IoUtil.copy(git.getWorkingDir(), projectDir, IoUtil.ANY_FILTER);
                } finally {
                    FileCleaner.addFile(temp);
                }
            } else {
                git = gitConnectionFactory.getConnection(localPath, consumerFactory);
                if (baseFolder.getChildren().size() == 0) {
                    cloneRepository(git, "origin", location, dtoFactory);
                    if (commitId != null) {
                        checkoutCommit(git, commitId, dtoFactory);
                    } else if (remoteOriginFetch != null) {
                        git.getConfig().add("remote.origin.fetch", remoteOriginFetch);
                        fetch(git, "origin", dtoFactory);
                        if (branch != null) {
                            checkoutBranch(git, branch, dtoFactory);
                        }
                    } else if (branch != null) {
                        checkoutBranch(git, branch, dtoFactory);
                    }
                } else {
                    initRepository(git, dtoFactory);
                    addRemote(git, "origin", location, dtoFactory);
                    if (commitId != null) {
                        fetchBranch(git, "origin", branch == null ? "*" : branch, dtoFactory);
                        checkoutCommit(git, commitId, dtoFactory);
                    } else if (remoteOriginFetch != null) {
                        git.getConfig().add("remote.origin.fetch", remoteOriginFetch);
                        fetch(git, "origin", dtoFactory);
                        if (branch != null) {
                            checkoutBranch(git, branch, dtoFactory);
                        }
                    } else {
                        fetchBranch(git, "origin", branch == null ? "*" : branch, dtoFactory);

                        List<Branch> branchList = git.branchList(dtoFactory.createDto(BranchListRequest.class).withListMode("r"));
                        if (!branchList.isEmpty()) {
                            checkoutBranch(git, branch == null ? "master" : branch, dtoFactory);
                        }
                    }
                }
                if (branchMerge != null) {
                    git.getConfig().set("branch." + (branch == null ? "master" : branch) + ".merge", branchMerge);
                }
                if (!keepVcs) {
                    cleanGit(git.getWorkingDir());
                }
            }
        } catch (UnauthorizedException e) {
            throw new UnauthorizedException(
                    "You are not authorized to perform the remote import. Codenvy may need accurate keys to the " +
                    "external system. You can create a new SSH key pair in Window->Preferences->Keys And " +
                    "Certificates->SSH Keystore.");
        } catch (URISyntaxException e) {
            throw new ServerException(
                    "Your project cannot be imported. The issue is either from git configuration, a malformed URL, " +
                    "or file system corruption. Please contact support for assistance.",
                    e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    private void cloneRepository(GitConnection git, String remoteName, String url, DtoFactory dtoFactory)
            throws ServerException, UnauthorizedException, URISyntaxException {
        final CloneRequest request = dtoFactory.createDto(CloneRequest.class).withRemoteName(remoteName).withRemoteUri(url);
        git.clone(request);
    }

    private void initRepository(GitConnection git, DtoFactory dtoFactory) throws GitException {
        final InitRequest request = dtoFactory.createDto(InitRequest.class).withInitCommit(false).withBare(false);
        git.init(request);
    }

    private void addRemote(GitConnection git, String name, String url, DtoFactory dtoFactory) throws GitException {
        final RemoteAddRequest request = dtoFactory.createDto(RemoteAddRequest.class).withName(name).withUrl(url);
        git.remoteAdd(request);
    }

    private void fetch(GitConnection git, String remote, DtoFactory dtoFactory) throws UnauthorizedException, GitException {
        final FetchRequest request = dtoFactory.createDto(FetchRequest.class).withRemote(remote);
        git.fetch(request);
    }

    private void fetchBranch(GitConnection gitConnection, String remote, String branch, DtoFactory dtoFactory)
            throws UnauthorizedException, GitException {

        final List<String> refSpecs = Collections.singletonList(String.format("refs/heads/%1$s:refs/remotes/origin/%1$s", branch));
        try {
            fetchRefSpecs(gitConnection, remote, refSpecs, dtoFactory);
        } catch (GitException e) {
            LOG.warn("Git exception on branch fetch", e);
            throw new GitException(
                    String.format("Unable to fetch remote branch %s. Make sure it exists and can be accessed.", branch),
                    e);
        }
    }

    private void fetchRefSpecs(GitConnection git, String remote, List<String> refSpecs, DtoFactory dtoFactory)
            throws UnauthorizedException, GitException {
        final FetchRequest request = dtoFactory.createDto(FetchRequest.class).withRemote(remote).withRefSpec(refSpecs);
        git.fetch(request);
    }

    private void checkoutCommit(GitConnection git, String commit, DtoFactory dtoFactory) throws GitException {
        final BranchCheckoutRequest request = dtoFactory.createDto(BranchCheckoutRequest.class).withName("temp").withCreateNew(true)
                                                        .withStartPoint(commit);
        try {
            git.branchCheckout(request);
        } catch (GitException e) {
            LOG.warn("Git exception on commit checkout", e);
            throw new GitException(
                    String.format("Unable to checkout commit %s. Make sure it exists and can be accessed.", commit), e);
        }
    }

    private void checkoutBranch(GitConnection git, String branch, DtoFactory dtoFactory) throws GitException {
        final BranchCheckoutRequest request = dtoFactory.createDto(BranchCheckoutRequest.class).withName(branch);
        try {
            git.branchCheckout(request);
        } catch (GitException e) {
            LOG.warn("Git exception on branch checkout", e);
            throw new GitException(
                    String.format("Unable to checkout remote branch %s. Make sure it exists and can be accessed.",
                                  branch), e);
        }
    }

    private void sparsecheckout(GitConnection git, String url, String branch, String directory, DtoFactory dtoFactory)
            throws GitException, UnauthorizedException {
        /*
        Does following sequence of Git commands:
        $ git init
        $ git remote add origin <URL>
        $ git config core.sparsecheckout true
        $ echo keepDirectory >> .git/info/sparse-checkout
        $ git pull origin master
        */
        initRepository(git, dtoFactory);
        addRemote(git, "origin", url, dtoFactory);
        git.getConfig().add("core.sparsecheckout", "true");
        final File workingDir = git.getWorkingDir();
        final File sparseCheckout = new File(workingDir, ".git" + File.separator + "info" + File.separator + "sparse-checkout");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(sparseCheckout.toPath(), Charset.forName("UTF-8"))) {
                writer.write(directory);
            }
        } catch (IOException e) {
            throw new GitException(e);
        }
        fetchBranch(git, "origin", branch, dtoFactory);
        checkoutBranch(git, branch, dtoFactory);
    }

    private void cleanGit(File project) {
        IoUtil.deleteRecursive(new File(project, ".git"));
        new File(project, ".gitignore").delete();
    }
}
