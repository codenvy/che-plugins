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
package org.eclipse.che.ide.ext.git.server.jgit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.Config;
import org.eclipse.che.ide.ext.git.server.DiffPage;
import org.eclipse.che.ide.ext.git.server.GitConnection;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.server.LogPage;
import org.eclipse.che.ide.ext.git.shared.AddRequest;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.ext.git.shared.BranchCreateRequest;
import org.eclipse.che.ide.ext.git.shared.BranchDeleteRequest;
import org.eclipse.che.ide.ext.git.shared.BranchListRequest;
import org.eclipse.che.ide.ext.git.shared.CloneRequest;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import org.eclipse.che.ide.ext.git.shared.DiffRequest;
import org.eclipse.che.ide.ext.git.shared.FetchRequest;
import org.eclipse.che.ide.ext.git.shared.GitUser;
import org.eclipse.che.ide.ext.git.shared.InitRequest;
import org.eclipse.che.ide.ext.git.shared.LogRequest;
import org.eclipse.che.ide.ext.git.shared.LsRemoteRequest;
import org.eclipse.che.ide.ext.git.shared.MergeRequest;
import org.eclipse.che.ide.ext.git.shared.MergeResult;
import org.eclipse.che.ide.ext.git.shared.MoveRequest;
import org.eclipse.che.ide.ext.git.shared.PullRequest;
import org.eclipse.che.ide.ext.git.shared.PushRequest;
import org.eclipse.che.ide.ext.git.shared.Remote;
import org.eclipse.che.ide.ext.git.shared.RemoteAddRequest;
import org.eclipse.che.ide.ext.git.shared.RemoteListRequest;
import org.eclipse.che.ide.ext.git.shared.RemoteReference;
import org.eclipse.che.ide.ext.git.shared.RemoteUpdateRequest;
import org.eclipse.che.ide.ext.git.shared.ResetRequest;
import org.eclipse.che.ide.ext.git.shared.Revision;
import org.eclipse.che.ide.ext.git.shared.RmRequest;
import org.eclipse.che.ide.ext.git.shared.Status;
import org.eclipse.che.ide.ext.git.shared.StatusFormat;
import org.eclipse.che.ide.ext.git.shared.Tag;
import org.eclipse.che.ide.ext.git.shared.TagCreateRequest;
import org.eclipse.che.ide.ext.git.shared.TagDeleteRequest;
import org.eclipse.che.ide.ext.git.shared.TagListRequest;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: JGitConnection.java 22817 2011-03-22 09:17:52Z andrew00x $
 */
public class JGitConnection implements GitConnection {
    // The JGit repository
    private final Repository repository;
    // The git user
    private final GitUser user;
    // Configuration object
    JGitConfigImpl _config;
    // Git stuff
    Git _git;

    /**
     * @param repository
     *            the JGit repository
     * @param user
     *            the user
     */
    JGitConnection(Repository repository, GitUser user) {
        this.repository = repository;
        this.user = user;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#add(org.exoplatform.ide.git.shared.AddRequest) */
    @Override
    public void add(AddRequest request) throws GitException {
        AddCommand addCommand = getGit().add().setUpdate(request.isUpdate());

        List<String> filepattern = request.getFilepattern();
        if (filepattern == null) {
            filepattern = AddRequest.DEFAULT_PATTERN;
        }
        for (String filepatternItem : filepattern) {
            addCommand.addFilepattern(filepatternItem);
        }

        try {
            addCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchCheckout(org.exoplatform.ide.git.shared.BranchCheckoutRequest) */
    @Override
    public void branchCheckout(BranchCheckoutRequest request) throws GitException {
        CheckoutCommand checkoutCommand = getGit().checkout();
        String startPoint = request.getStartPoint();
        String cleanName = cleanRemoteName(request.getName());
        if (startPoint != null) {
            checkoutCommand.setStartPoint(startPoint);
        }
        checkoutCommand.setCreateBranch(request.isCreateNew());
        checkoutCommand.setName(cleanName);
        checkoutCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
        try {
            checkoutCommand.call();
        } catch (GitAPIException e) {
            if (e.getMessage().endsWith("already exists")) {
                throw new IllegalArgumentException("fatal: A branch named '" + cleanName + "' already exists.");
            }
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchCreate(org.exoplatform.ide.git.shared.BranchCreateRequest) */
    @Override
    public Branch branchCreate(BranchCreateRequest request) throws GitException {
        CreateBranchCommand createBranchCommand = getGit().branchCreate().setName(request.getName());
        String start = request.getStartPoint();
        if (start != null) {
            createBranchCommand.setStartPoint(start);
        }
        try {
            Ref brRef = createBranchCommand.call();
            String refName = brRef.getName();
            String displayName = Repository.shortenRefName(refName);
            Branch branch = createDto(Branch.class);
            return branch.withName(refName).withActive(false).withDisplayName(displayName).withRemote(false);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchDelete(org.exoplatform.ide.git.shared.BranchDeleteRequest) */
    @Override
    public void branchDelete(BranchDeleteRequest request) throws GitException {
        try {
            getGit().branchDelete().setBranchNames(request.getName()).setForce(request.isForce()).call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchRename(String oldName, String newName) */
    @Override
    public void branchRename(String oldName, String newName) throws GitException {
        try {
            getGit().branchRename().setOldName(oldName).setNewName(newName).call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchList(org.exoplatform.ide.git.shared.BranchListRequest) */
    @Override
    public List<Branch> branchList(BranchListRequest request) throws GitException {
        String listMode = request.getListMode();
        if (listMode != null
                && !(listMode.equals(BranchListRequest.LIST_ALL) || listMode.equals(BranchListRequest.LIST_REMOTE))) {
            throw new IllegalArgumentException("Unsupported list mode '" + listMode + "'. Must be either 'a' or 'r'. ");
        }

        ListBranchCommand listBranchCommand = getGit().branchList();
        if (listMode != null) {
            if (listMode.equals(BranchListRequest.LIST_ALL)) {
                listBranchCommand.setListMode(ListMode.ALL);
            } else if (listMode.equals(BranchListRequest.LIST_REMOTE)) {
                listBranchCommand.setListMode(ListMode.REMOTE);
            }
        }
        List<Ref> refs;
        try {
            refs = listBranchCommand.call();
        } catch (GitAPIException err) {
            throw new GitException(err);
        }
        String current = null;
        try {
            Ref headRef = repository.getRef(Constants.HEAD);
            if (!(headRef == null || Constants.HEAD.equals(headRef.getLeaf().getName()))) {
                current = headRef.getLeaf().getName();
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }

        List<Branch> branches = new ArrayList<Branch>();
        if (current == null) {
            branches.add(createDto(Branch.class).withName("(no branch)").withActive(true).withDisplayName("(no name)")
                    .withRemote(false));
        }

        for (Ref brRef : refs) {
            String refName = brRef.getName();
            Branch branch = createDto(Branch.class).withName(refName).withActive(refName.equals(current))
                    .withDisplayName(Repository.shortenRefName(refName))
                    .withRemote(brRef.getName().startsWith("refs/remotes"));
            branches.add(branch);
        }
        return branches;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#clone(org.exoplatform.ide.git.shared.CloneRequest) */
    public void clone(CloneRequest request) throws GitException {
        try {
            if (request.getRemoteName() == null) {
                request.setRemoteName(Constants.DEFAULT_REMOTE_NAME);
            }
            if (request.getWorkingDir() == null) {
                request.setWorkingDir(repository.getWorkTree().getCanonicalPath());
            }
            CloneCommand cloneCom = Git.cloneRepository();
            cloneCom.setRemote(request.getRemoteName());
            cloneCom.setURI(request.getRemoteUri());
            cloneCom.setDirectory(new File(request.getWorkingDir()));
            if (request.getBranchesToFetch() != null) {
                cloneCom.setBranchesToClone(new ArrayList<String>(request.getBranchesToFetch()));
            } else {
                cloneCom.setCloneAllBranches(true);
            }
            Repository repo = cloneCom.call().getRepository();
            StoredConfig config = repo.getConfig();
            GitUser gitUser = getUser();
            if (gitUser != null) {
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME,
                        gitUser.getName());
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL,
                        gitUser.getEmail());
            }

            config.save();

        } catch (InvalidRemoteException e) {
            throw new GitException(e);
        } catch (TransportException e) {
            throw new GitException(e.getMessage());
        } catch (GitAPIException e) {
            throw new GitException(e);
        } catch (IOException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#commit(org.exoplatform.ide.git.shared.CommitRequest) */
    @Override
    public Revision commit(CommitRequest request) throws GitException {
        try {
            if (!repository.getRepositoryState().canCommit()) {
                Revision rev = createDto(Revision.class);
                rev.setMessage("Commit is not possible because repository state is '"
                        + repository.getRepositoryState().getDescription() + "'");
                return rev;
            }

            if (request.isAmend() && !repository.getRepositoryState().canAmend()) {
                Revision rev = createDto(Revision.class);
                rev.setMessage("Amend is not possible because repository state is '"
                        + repository.getRepositoryState().getDescription() + "'");
                return rev;
            }

            // TODO had to set message line NativeGitConnect because JGit's previous implementation uses
            // stat.createString
            Status stat = status(StatusFormat.LONG);
            if (stat.getAdded().isEmpty() && stat.getChanged().isEmpty() && stat.getRemoved().isEmpty()) {
                if (request.isAll()) {
                    if (stat.getMissing().isEmpty() && stat.getModified().isEmpty()) {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    }
                } else {
                    if (stat.getMissing().isEmpty() && stat.getModified().isEmpty()) {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    } else {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    }
                }
            }

            CommitCommand commitCommand = getGit().commit();

            String configName = repository.getConfig().getString(ConfigConstants.CONFIG_USER_SECTION, null,
                    ConfigConstants.CONFIG_KEY_NAME);
            String configEmail = repository.getConfig().getString(ConfigConstants.CONFIG_USER_SECTION, null,
                    ConfigConstants.CONFIG_KEY_EMAIL);

            String gitName = getUser().getName();
            String gitEmail = getUser().getEmail();

            String comitterName = configName != null ? configName : gitName;
            String comitterEmail = configEmail != null ? configEmail : gitEmail;

            commitCommand.setCommitter(comitterName, comitterEmail);
            commitCommand.setAuthor(comitterName, comitterEmail);
            commitCommand.setMessage(request.getMessage());
            commitCommand.setAll(request.isAll());
            commitCommand.setAmend(request.isAmend());

            RevCommit result = commitCommand.call();

            GitUser gitUser = createDto(GitUser.class).withName(comitterName).withEmail(comitterEmail);
            Revision revision = createDto(Revision.class).withBranch(getCurrentBranch())
                    .withId(result.getId().getName()).withMessage(result.getFullMessage())
                    .withCommitTime((long) result.getCommitTime() * 1000).withCommitter(gitUser);
            return revision;
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#diff(org.exoplatform.ide.git.shared.DiffRequest) */
    @Override
    public DiffPage diff(DiffRequest request) throws GitException {
        return new JGitDiffPage(request, repository);
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#fetch(org.exoplatform.ide.git.shared.FetchRequest) */
    @Override
    public void fetch(FetchRequest request) throws GitException {
        try {
            List<RefSpec> fetchRefSpecs = null;
            List<String> refSpec = request.getRefSpec();
            if (refSpec != null && refSpec.size() > 0) {
                fetchRefSpecs = new ArrayList<RefSpec>(refSpec.size());
                for (String refSpecItem : refSpec) {
                    RefSpec fetchRefSpec = (refSpecItem.indexOf(':') < 0) //
                    ? new RefSpec(Constants.R_HEADS + refSpecItem + ":") //
                            : new RefSpec(refSpecItem);
                    fetchRefSpecs.add(fetchRefSpec);
                }
            } else {
                fetchRefSpecs = Arrays.asList(new RefSpec(Constants.HEAD));
            }

            FetchCommand fetchCommand = getGit().fetch();

            String remote = request.getRemote();
            if (remote != null) {
                fetchCommand.setRemote(remote);
            }
            if (fetchRefSpecs != null) {
                fetchCommand.setRefSpecs(fetchRefSpecs);
            }
            int timeout = request.getTimeout();
            if (timeout > 0) {
                fetchCommand.setTimeout(timeout);
            }
            fetchCommand.setRemoveDeletedRefs(request.isRemoveDeletedRefs());

            // If this an unknown remote with no refspecs given, put HEAD (otherwise JGit fails)
            if (remote != null && (refSpec == null || refSpec.isEmpty())) {
                boolean found = false;
                List<Remote> configRemotes = remoteList(createDto(RemoteListRequest.class));
                for (Remote configRemote : configRemotes) {
                    if (remote.equals(configRemote.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fetchRefSpecs = Arrays.asList(new RefSpec(Constants.HEAD + ":" + Constants.FETCH_HEAD));
                }
            }

            fetchCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#init(org.exoplatform.ide.git.shared.InitRequest) */
    @Override
    public void init(InitRequest request) throws GitException {
        File workDir = repository.getWorkTree();
        if (!workDir.exists()) {
            throw new GitException("Working folder " + workDir + " not exists . ");
        }

        boolean bare = request.isBare();

        try {
            repository.create(bare);

            if (!bare) {
                try {
                    Git git = getGit();
                    git.add().addFilepattern(".").call();
                    git.commit().setMessage("init").call();
                } catch (GitAPIException e) {
                    throw new GitException(e);
                }
            }
            GitUser gitUser = getUser();
            if (gitUser != null) {
                StoredConfig config = repository.getConfig();
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME,
                        gitUser.getName());
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL,
                        gitUser.getEmail());
                config.save();
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#log(org.exoplatform.ide.git.shared.LogRequest) */
    @Override
    public LogPage log(LogRequest request) throws GitException {
        LogCommand logCommand = getGit().log();
        try {
            Iterator<RevCommit> revIterator = logCommand.call().iterator();
            List<Revision> commits = new ArrayList<Revision>();

            while (revIterator.hasNext()) {
                RevCommit commit = revIterator.next();
                PersonIdent committerIdentity = commit.getCommitterIdent();
                GitUser gitUser = createDto(GitUser.class).withName(committerIdentity.getName()).withEmail(
                        committerIdentity.getEmailAddress());
                Revision revision = createDto(Revision.class).withId(commit.getId().getName())
                        .withMessage(commit.getFullMessage()).withCommitTime((long) commit.getCommitTime() * 1000)
                        .withCommitter(gitUser);
                commits.add(revision);
            }
            return new LogPage(commits);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#log(org.exoplatform.ide.git.shared.LogRequest) */
    @Override
    public List<GitUser> getCommiters() throws GitException {
        List<GitUser> gitUsers = new ArrayList<GitUser>();
        try {
            LogCommand logCommand = getGit().log();
            Iterator<RevCommit> revIterator = logCommand.call().iterator();

            while (revIterator.hasNext()) {
                RevCommit commit = revIterator.next();
                PersonIdent committerIdentity = commit.getCommitterIdent();
                GitUser gitUser = createDto(GitUser.class).withName(committerIdentity.getName()).withEmail(
                        committerIdentity.getEmailAddress());
                if (!gitUsers.contains(gitUser)) {
                    gitUsers.add(gitUser);
                }
            }
        } catch (GitAPIException e) {
            throw new GitException(e);
        }

        return gitUsers;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#merge(org.exoplatform.ide.git.shared.MergeRequest) */
    @Override
    public MergeResult merge(MergeRequest request) throws GitException {
        try {
            Ref ref = repository.getRef(request.getCommit());
            if (ref == null) {
                throw new IllegalArgumentException("Invalid reference to commit for merge " + request.getCommit());
            }
            // Shorten local branch names by removing '/refs/heads/' from the beginning
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS)) {
                name = name.substring(Constants.R_HEADS.length());
            }
            org.eclipse.jgit.api.MergeResult jgitMergeResult = getGit().merge().include(name, ref.getObjectId()).call();
            return new JGitMergeResult(jgitMergeResult);
        } catch (CheckoutConflictException e) {
            org.eclipse.jgit.api.MergeResult jgitMergeResult = new org.eclipse.jgit.api.MergeResult(
                    e.getConflictingPaths());
            return new JGitMergeResult(jgitMergeResult);
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#mv(org.exoplatform.ide.git.shared.MoveRequest) */
    @Override
    public void mv(MoveRequest request) throws GitException {
        throw new RuntimeException("Not implemented yet. ");
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#pull(org.exoplatform.ide.git.shared.PullRequest) */
    @Override
    public void pull(PullRequest request) throws GitException {
        try {
            if (repository.getRepositoryState().equals(RepositoryState.MERGING)) {
                throw new GitException("Pull request cannot be performed because repository state is 'MERGING'");
            }
            String fullBranch = repository.getFullBranch();
            if (!fullBranch.startsWith(Constants.R_HEADS)) {
                throw new DetachedHeadException("HEAD is detached. Cannot pull. ");
            }

            String branch = fullBranch.substring(Constants.R_HEADS.length());

            StoredConfig config = repository.getConfig();
            String remote = request.getRemote();
            if (remote == null) {
                remote = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                        ConfigConstants.CONFIG_KEY_REMOTE);
                if (remote == null) {
                    remote = Constants.DEFAULT_REMOTE_NAME;
                }
            }

            String remoteBranch = null;
            RefSpec fetchRefSpecs = null;
            String refSpec = request.getRefSpec();
            if (refSpec != null) {
                fetchRefSpecs = (refSpec.indexOf(':') < 0) //
                ? new RefSpec(Constants.R_HEADS + refSpec + ":" + fullBranch) //
                        : new RefSpec(refSpec);
                remoteBranch = fetchRefSpecs.getSource();
            } else {
                remoteBranch = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                        ConfigConstants.CONFIG_KEY_MERGE);
            }

            if (remoteBranch == null) {
                String key = ConfigConstants.CONFIG_BRANCH_SECTION + "." + branch + "."
                        + ConfigConstants.CONFIG_KEY_MERGE;
                throw new GitException("Remote branch is not specified in request and " + key
                        + " in configuration is not set. ");
            }

            FetchCommand fetchCommand = getGit().fetch();
            fetchCommand.setRemote(remote);
            if (fetchRefSpecs != null) {
                fetchCommand.setRefSpecs(fetchRefSpecs);
            }
            int timeout = request.getTimeout();
            if (timeout > 0) {
                fetchCommand.setTimeout(timeout);
            }

            FetchResult fetchResult = fetchCommand.call();

            Ref remoteBranchRef = fetchResult.getAdvertisedRef(remoteBranch);
            if (remoteBranchRef == null) {
                remoteBranchRef = fetchResult.getAdvertisedRef(Constants.R_HEADS + remoteBranch);
            }
            if (remoteBranchRef == null) {
                throw new GitException("Cannot get ref for remote branch " + remoteBranch + ". ");
            }
            org.eclipse.jgit.api.MergeResult res = getGit().merge().include(remoteBranchRef).call();
            if (res.getMergeStatus().equals(org.eclipse.jgit.api.MergeResult.MergeStatus.ALREADY_UP_TO_DATE)) {
                throw new GitException(res.getMergeStatus().toString());
            }

            if (res.getConflicts() != null) {
                StringBuilder message = new StringBuilder("Merge conflict appeared in files:</br>");
                Map<String, int[][]> allConflicts = res.getConflicts();
                for (String path : allConflicts.keySet()) {
                    message.append(path + "</br>");
                }
                message.append("Automatic merge failed; fix conflicts and then commit the result.");
                throw new GitException(message.toString());
            }
        } catch (CheckoutConflictException e) {
            StringBuilder message = new StringBuilder(
                    "error: Your local changes to the following files would be overwritten by merge:</br>");
            for (String path : e.getConflictingPaths()) {
                message.append(path + "</br>");
            }
            message.append("Please, commit your changes before you can merge. Aborting.");
            throw new GitException(message.toString());
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#push(org.exoplatform.ide.git.shared.PushRequest) */
    @Override
    public void push(PushRequest request) throws GitException {
        StringBuilder message = new StringBuilder();
        try {
            PushCommand pushCommand = getGit().push();
            String remote = request.getRemote();
            if (request.getRemote() != null) {
                pushCommand.setRemote(remote);
            }

            List<String> refSpec = request.getRefSpec();
            if (refSpec != null && refSpec.size() > 0) {
                List<RefSpec> refSpecInst = new ArrayList<RefSpec>(refSpec.size());
                for (String refSpecItem : refSpec) {
                    refSpecInst.add(new RefSpec(refSpecItem));
                }
                pushCommand.setRefSpecs(refSpecInst);
            }

            pushCommand.setForce(request.isForce());

            int timeout = request.getTimeout();
            if (timeout > 0) {
                pushCommand.setTimeout(timeout);
            }

            Iterable<PushResult> list = pushCommand.call();
            for (PushResult pushResult : list) {
                Collection<RemoteRefUpdate> refUpdates = pushResult.getRemoteUpdates();
                for (RemoteRefUpdate remoteRefUpdate : refUpdates) {
                    if (!remoteRefUpdate.getStatus().equals(org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK)) {

                        if (remoteRefUpdate.getStatus().equals(
                                org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE)) {
                            message.append("Already up-to-date.");
                        } else {
                            message.append("! [rejected] " + getCurrentBranch() + " -> "
                                    + request.getRefSpec().get(0).split(":")[1] + " (non-fast-forward)\n");
                            message.append("error: failed to push some refs to " + request.getRemote() + "\n");
                            message.append("To prevent you from losing history, non-fast-forward updates were rejected\n");
                            message.append("Merge the remote changes (e.g. git pull) before pushing again.\n");
                        }
                        throw new GitException(message.toString());
                    }
                }
            }
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteAdd(org.exoplatform.ide.git.shared.RemoteAddRequest) */
    @Override
    public void remoteAdd(RemoteAddRequest request) throws GitException {
        String remoteName = request.getName();
        if (remoteName == null || remoteName.length() == 0) {
            throw new IllegalArgumentException("Remote name required. ");
        }

        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections("remote");
        if (remoteNames.contains(remoteName)) {
            throw new IllegalArgumentException("Remote " + remoteName + " already exists. ");
        }

        String url = request.getUrl();
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("Remote url required. ");
        }

        RemoteConfig remoteConfig;
        try {
            remoteConfig = new RemoteConfig(config, remoteName);
        } catch (URISyntaxException e) {
            // Not happen since it is newly created remote.
            throw new GitException(e.getMessage(), e);
        }

        try {
            remoteConfig.addURI(new URIish(url));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Remote url " + url + " is invalid. ");
        }

        List<String> branches = request.getBranches();
        if (branches != null) {
            for (String branch : branches) {
                remoteConfig.addFetchRefSpec( //
                        new RefSpec(Constants.R_HEADS + branch + ":" + Constants.R_REMOTES + remoteName + "/" + branch)
                                .setForceUpdate(true));
            }
        } else {
            remoteConfig.addFetchRefSpec(new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES + remoteName
                    + "/*").setForceUpdate(true));
        }

        remoteConfig.update(config);

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteDelete(java.lang.String) */
    @Override
    public void remoteDelete(String name) throws GitException {
        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE);
        if (!remoteNames.contains(name)) {
            throw new GitException("Remote " + name + " not found. ");
        }

        config.unsetSection(ConfigConstants.CONFIG_REMOTE_SECTION, name);
        Set<String> branches = config.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION);

        for (String branch : branches) {
            String r = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                    ConfigConstants.CONFIG_KEY_REMOTE);
            if (name.equals(r)) {
                config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch, ConfigConstants.CONFIG_KEY_REMOTE);
                config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch, ConfigConstants.CONFIG_KEY_MERGE);
                List<Branch> remoteBranches = branchList(createDto(BranchListRequest.class).withListMode("r"));
                for (Branch remoteBranch : remoteBranches) {
                    if (remoteBranch.getDisplayName().startsWith(name)) {
                        branchDelete(createDto(BranchDeleteRequest.class).withName(remoteBranch.getName()).withForce(
                                true));
                    }
                }
            }
        }

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteList(org.exoplatform.ide.git.shared.RemoteListRequest) */
    @Override
    public List<Remote> remoteList(RemoteListRequest request) throws GitException {
        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = new HashSet<String>(config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE));
        String remote = request.getRemote();

        if (remote != null && remoteNames.contains(remote)) {
            remoteNames.clear();
            remoteNames.add(remote);
        }

        List<Remote> result = new ArrayList<Remote>(remoteNames.size());
        for (String rn : remoteNames) {
            try {
                List<URIish> uris = new RemoteConfig(config, rn).getURIs();
                result.add(createDto(Remote.class).withName(rn).withUrl(uris.isEmpty() ? null : uris.get(0).toString()));
            } catch (URISyntaxException e) {
                throw new GitException(e.getMessage(), e);
            }
        }
        return result;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteUpdate(org.exoplatform.ide.git.shared.RemoteUpdateRequest) */
    @Override
    public void remoteUpdate(RemoteUpdateRequest request) throws GitException {
        String remoteName = request.getName();
        if (remoteName == null || remoteName.length() == 0) {
            throw new IllegalArgumentException("Remote name required. ");
        }

        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE);
        if (!remoteNames.contains(remoteName)) {
            throw new IllegalArgumentException("Remote " + remoteName + " not found. ");
        }

        RemoteConfig remoteConfig;
        try {
            remoteConfig = new RemoteConfig(config, remoteName);
        } catch (URISyntaxException e) {
            throw new GitException(e.getMessage(), e);
        }

        List<String> tmp;

        tmp = request.getBranches();
        if (tmp != null && tmp.size() > 0) {
            if (!request.isAddBranches()) {
                remoteConfig.setFetchRefSpecs(new ArrayList<RefSpec>());
                remoteConfig.setPushRefSpecs(new ArrayList<RefSpec>());
            } else {
                // Replace wildcard refspec if any.
                remoteConfig.removeFetchRefSpec(new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES
                        + remoteName + "/*").setForceUpdate(true));
                remoteConfig.removeFetchRefSpec(new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES
                        + remoteName + "/*"));
            }

            // Add new refspec.
            for (String branch : tmp) {
                remoteConfig.addFetchRefSpec(new RefSpec(Constants.R_HEADS + branch + ":" + Constants.R_REMOTES
                        + remoteName + "/" + branch).setForceUpdate(true));
            }
        }

        // Remove URLs first.
        tmp = request.getRemoveUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.removeURI(new URIish(url));
                } catch (URISyntaxException e) {
                    // Ignore this error. Cannot remove invalid URL.
                }
            }
        }

        // Add new URLs.
        tmp = request.getAddUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.addURI(new URIish(url));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Remote url " + url + " is invalid. ");
                }
            }
        }

        // Remove URLs for pushing.
        tmp = request.getRemovePushUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.removePushURI(new URIish(url));
                } catch (URISyntaxException e) {
                    // Ignore this error. Cannot remove invalid URL.
                }
            }
        }

        // Add URLs for pushing.
        tmp = request.getAddPushUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.addPushURI(new URIish(url));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Remote push url " + url + " is invalid. ");
                }
            }
        }

        remoteConfig.update(config);

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#reset(org.exoplatform.ide.git.shared.ResetRequest) */
    @Override
    public void reset(ResetRequest request) throws GitException {
        try {
            if (!repository.getRepositoryState().canResetHead()) {
                throw new GitException("Reset is not possible because repository state is"
                        + repository.getRepositoryState().getDescription() + ".");
            }

            ResetCommand req = getGit().reset();
            req.setRef(request.getCommit());

            if (request.getType().equals(ResetRequest.ResetType.HARD)) {
                req.setMode(ResetCommand.ResetType.HARD);
            } else if (request.getType().equals(ResetRequest.ResetType.KEEP)) {
                req.setMode(ResetCommand.ResetType.KEEP);
            } else if (request.getType().equals(ResetRequest.ResetType.MERGE)) {
                req.setMode(ResetCommand.ResetType.MERGE);
            } else if (request.getType().equals(ResetRequest.ResetType.MIXED)) {
                req.setMode(ResetCommand.ResetType.MIXED);
            } else if (request.getType().equals(ResetRequest.ResetType.SOFT)) {
                req.setMode(ResetCommand.ResetType.SOFT);
            }

            req.call();
        } catch (CheckoutConflictException e) {
            throw new GitException(e);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#rm(org.exoplatform.ide.git.shared.RmRequest) */
    @Override
    public void rm(RmRequest request) throws GitException {
        List<String> files = request.getItems();
        RmCommand rmCommand = getGit().rm();

        rmCommand.setCached(request.isCached());

        if (files != null) {
            for (String file : files) {
                rmCommand.addFilepattern(file);
            }
        }
        try {
            rmCommand.call();
        } catch (NoFilepatternException e) {
            throw new IllegalArgumentException("File pattern may not be null or empty. ");
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#status(org.exoplatform.ide.git.shared.StatusRequest) */
    @Override
    public Status status(StatusFormat format) throws GitException {
        org.eclipse.jgit.api.Status jgitStatus;
        try {
            jgitStatus = getGit().status().call();
        } catch (NoWorkTreeException | GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
        // Remove files that are inside untracked folders to match the behavior of native Git
        List<String> untrackedFolders = new ArrayList<String>(jgitStatus.getUntrackedFolders());
        List<String> untrackedFiles = new ArrayList<String>();
        for (String untrackedFile : jgitStatus.getUntracked()) {
            boolean addUntrackedFile = true;
            for (String untrackedFolder : untrackedFolders) {
                if (untrackedFile.startsWith(untrackedFolder + '/')) {
                    addUntrackedFile = false;
                    break;
                }
            }
            if (addUntrackedFile) {
                untrackedFiles.add(untrackedFile);
            }
        }
        // The Che result
        String currentBranch = getCurrentBranch();
        Status cheStatus = createDto(Status.class);
        cheStatus.setAdded(new ArrayList<String>(jgitStatus.getAdded()));
        cheStatus.setBranchName(currentBranch);
        cheStatus.setChanged(new ArrayList<String>(jgitStatus.getChanged()));
        cheStatus.setClean(jgitStatus.isClean());
        cheStatus.setConflicting(new ArrayList<String>(jgitStatus.getConflicting()));
        cheStatus.setFormat(format);
        cheStatus.setMissing(new ArrayList<String>(jgitStatus.getMissing()));
        cheStatus.setModified(new ArrayList<String>(jgitStatus.getModified()));
        cheStatus.setRemoved(new ArrayList<String>(jgitStatus.getRemoved()));
        cheStatus.setUntracked(untrackedFiles);
        cheStatus.setUntrackedFolders(untrackedFolders);
        return cheStatus;
    }

    static <T extends Comparable<T>> List<T> asSortedList(Set<T> set) {
        List<T> list = new ArrayList<T>(set);
        Collections.sort(list);
        return list;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagCreate(org.exoplatform.ide.git.shared.TagCreateRequest) */
    @Override
    public Tag tagCreate(TagCreateRequest request) throws GitException {
        String commit = request.getCommit();
        if (commit == null) {
            commit = Constants.HEAD;
        }

        try {
            RevWalk revWalk = new RevWalk(repository);
            RevObject revObject;
            try {
                revObject = revWalk.parseAny(repository.resolve(commit));
            } finally {
                revWalk.release();
            }

            TagCommand tagCommand = getGit().tag().setName(request.getName()).setObjectId(revObject)
                    .setMessage(request.getMessage()).setForceUpdate(request.isForce());

            GitUser tagger = getUser();
            if (tagger != null) {
                tagCommand.setTagger(new PersonIdent(tagger.getName(), tagger.getEmail()));
            }

            Ref revTagRef = tagCommand.call();
            RevTag revTag = revWalk.parseTag(revTagRef.getLeaf().getObjectId());
            return createDto(Tag.class).withName(revTag.getTagName());
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagDelete(org.exoplatform.ide.git.shared.TagDeleteRequest) */
    @Override
    public void tagDelete(TagDeleteRequest request) throws GitException {
        try {
            String tagName = request.getName();
            Ref tagRef = repository.getRef(tagName);
            if (tagRef == null) {
                throw new IllegalArgumentException("Tag " + tagName + " not found. ");
            }

            RefUpdate updateRef = repository.updateRef(tagRef.getName());
            updateRef.setRefLogMessage("tag deleted", false);
            updateRef.setForceUpdate(true);
            Result deleteResult;
            deleteResult = updateRef.delete();
            if (deleteResult != Result.FORCED && deleteResult != Result.FAST_FORWARD) {
                throw new GitException("Can't delete tag " + tagName + ". Result " + deleteResult);
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagList(org.exoplatform.ide.git.shared.TagListRequest) */
    @Override
    public List<Tag> tagList(TagListRequest request) throws GitException {
        String patternStr = request.getPattern();
        Pattern pattern = null;
        if (patternStr != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < patternStr.length(); i++) {
                char c = patternStr.charAt(i);
                if (c == '*' || c == '?') {
                    sb.append('.');
                } else if (c == '.' || c == '(' || c == ')' || c == '[' || c == ']' || c == '^' || c == '$' || c == '|') {
                    sb.append('\\');
                }
                sb.append(c);
            }
            pattern = Pattern.compile(sb.toString());
        }

        Set<String> tagNames = repository.getTags().keySet();
        List<Tag> tags = new ArrayList<Tag>(tagNames.size());

        for (String tagName : tagNames) {
            if (pattern == null || pattern.matcher(tagName).matches()) {
                tags.add(createDto(Tag.class).withName(tagName));
            }
        }
        return tags;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#getUser() */
    public GitUser getUser() {
        return user;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#close() */
    @Override
    public void close() {
        repository.close();
    }

    public Repository getRepository() {
        return repository;
    }

    public String getCurrentBranch() throws GitException {
        try {
            Ref headRef;
            headRef = repository.getRef(Constants.HEAD);
            return Repository.shortenRefName(headRef.getLeaf().getName());
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /**
     * Method for cleaning name of remote branch to be checked out. I.e. it takes something like "origin/testBranch" and
     * returns "testBranch". This is needed for view-compatibility with console Git client.
     * 
     * @param branchName
     *            is a name of branch to be cleaned
     * @return branchName without remote repository name
     * @throws GitException
     */
    private String cleanRemoteName(String branchName) throws GitException {
        String returnName = branchName;
        List<Remote> remotes = this.remoteList(createDto(RemoteListRequest.class));
        for (Remote remote : remotes) {
            if (branchName.startsWith(remote.getName())) {
                returnName = branchName.replaceFirst(remote.getName() + "/", "");
            }
        }
        return returnName;
    }

    @Override
    public File getWorkingDir() {
        return repository.getWorkTree();
    }

    @Override
    public List<RemoteReference> lsRemote(LsRemoteRequest request) throws UnauthorizedException, GitException {
        LsRemoteCommand jgitCommand = getGit().lsRemote();
        jgitCommand.setRemote(request.getRemoteUrl());
        // TODO handle cases of isUseAuthorization()
        Collection<Ref> refs;
        try {
            refs = jgitCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
        // Translate the JGit result
        List<RemoteReference> remoteRefs = new ArrayList<RemoteReference>(refs.size());
        for (Ref ref : refs) {
            String commitId = ref.getObjectId().name();
            String name = ref.getName();
            RemoteReference remoteRef = createDto(RemoteReference.class).withCommitId(commitId).withReferenceName(name);
            remoteRefs.add(remoteRef);
        }
        return remoteRefs;
    }

    @Override
    public Config getConfig() throws GitException {
        if (_config != null) {
            return _config;
        }
        return _config = new JGitConfigImpl(repository);
    }

    @Override
    public void setOutputLineConsumerFactory(LineConsumerFactory outputPublisherFactory) {
        // XXX nothing to do, not outputs are produced by JGit
    }

    private Git getGit() {
        if (_git != null) {
            return _git;
        }
        return _git = new Git(repository);
    }

    private static <T> T createDto(Class<T> clazz) {
        return DtoFactory.getInstance().createDto(clazz);
    }
}
