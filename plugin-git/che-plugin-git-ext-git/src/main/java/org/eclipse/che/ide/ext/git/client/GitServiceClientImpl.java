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
package org.eclipse.che.ide.ext.git.client;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.add.AddRequestHandler;
import org.eclipse.che.ide.ext.git.client.clone.CloneRequestStatusHandler;
import org.eclipse.che.ide.ext.git.client.fetch.FetchRequestHandler;
import org.eclipse.che.ide.ext.git.client.init.InitRequestStatusHandler;
import org.eclipse.che.ide.ext.git.shared.AddRequest;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.ext.git.shared.BranchCreateRequest;
import org.eclipse.che.ide.ext.git.shared.BranchDeleteRequest;
import org.eclipse.che.ide.ext.git.shared.BranchListRequest;
import org.eclipse.che.ide.ext.git.shared.CloneRequest;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import org.eclipse.che.ide.ext.git.shared.Commiters;
import org.eclipse.che.ide.ext.git.shared.ConfigRequest;
import org.eclipse.che.ide.ext.git.shared.DiffRequest;
import org.eclipse.che.ide.ext.git.shared.FetchRequest;
import org.eclipse.che.ide.ext.git.shared.GitUrlVendorInfo;
import org.eclipse.che.ide.ext.git.shared.InitRequest;
import org.eclipse.che.ide.ext.git.shared.LogRequest;
import org.eclipse.che.ide.ext.git.shared.LogResponse;
import org.eclipse.che.ide.ext.git.shared.MergeRequest;
import org.eclipse.che.ide.ext.git.shared.MergeResult;
import org.eclipse.che.ide.ext.git.shared.PullRequest;
import org.eclipse.che.ide.ext.git.shared.PullResult;
import org.eclipse.che.ide.ext.git.shared.PushRequest;
import org.eclipse.che.ide.ext.git.shared.PushResponse;
import org.eclipse.che.ide.ext.git.shared.Remote;
import org.eclipse.che.ide.ext.git.shared.RemoteAddRequest;
import org.eclipse.che.ide.ext.git.shared.RemoteListRequest;
import org.eclipse.che.ide.ext.git.shared.RepoInfo;
import org.eclipse.che.ide.ext.git.shared.ResetRequest;
import org.eclipse.che.ide.ext.git.shared.Revision;
import org.eclipse.che.ide.ext.git.shared.RmRequest;
import org.eclipse.che.ide.ext.git.shared.Status;
import org.eclipse.che.ide.ext.git.shared.StatusFormat;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBuilder;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.MimeType.TEXT_PLAIN;
import static org.eclipse.che.ide.ext.git.shared.StatusFormat.PORCELAIN;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;
import static com.google.gwt.http.client.RequestBuilder.POST;

/**
 * Implementation of the {@link GitServiceClient}.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class GitServiceClientImpl implements GitServiceClient {
    public static final String ADD               = "/add";
    public static final String BRANCH_LIST       = "/branch-list";
    public static final String BRANCH_CHECKOUT   = "/branch-checkout";
    public static final String BRANCH_CREATE     = "/branch-create";
    public static final String BRANCH_DELETE     = "/branch-delete";
    public static final String BRANCH_RENAME     = "/branch-rename";
    public static final String CLONE             = "/clone";
    public static final String COMMIT            = "/commit";
    public static final String CONFIG            = "/config";
    public static final String DIFF              = "/diff";
    public static final String FETCH             = "/fetch";
    public static final String INIT              = "/init";
    public static final String LOG               = "/log";
    public static final String MERGE             = "/merge";
    public static final String STATUS            = "/status";
    public static final String RO_URL            = "/read-only-url";
    public static final String PUSH              = "/push";
    public static final String PULL              = "/pull";
    public static final String REMOTE_LIST       = "/remote-list";
    public static final String REMOTE_ADD        = "/remote-add";
    public static final String REMOTE_DELETE     = "/remote-delete";
    public static final String REMOVE            = "/rm";
    public static final String RESET             = "/reset";
    public static final String COMMITERS         = "/commiters";
    public static final String DELETE_REPOSITORY = "/delete-repository";
    /** REST service context. */
    private final String                  baseHttpUrl;
    private final String                  gitServicePath;
    /** Loader to be displayed. */
    private final AsyncRequestLoader      loader;
    private final MessageBus              wsMessageBus;
    private final EventBus                eventBus;
    private final GitLocalizationConstant constant;
    private final DtoFactory              dtoFactory;
    private final AsyncRequestFactory     asyncRequestFactory;

    @Inject
    protected GitServiceClientImpl(@RestContext String restContext,
                                   @Named("workspaceId") String workspaceId,
                                   AsyncRequestLoader loader,
                                   MessageBus wsMessageBus,
                                   EventBus eventBus,
                                   GitLocalizationConstant constant,
                                   DtoFactory dtoFactory,
                                   AsyncRequestFactory asyncRequestFactory) {
        this.loader = loader;
        this.gitServicePath = "/git/" + workspaceId;
        this.baseHttpUrl = restContext + gitServicePath;
        this.wsMessageBus = wsMessageBus;
        this.eventBus = eventBus;
        this.constant = constant;
        this.dtoFactory = dtoFactory;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void init(@Nonnull ProjectDescriptor project, boolean bare, @Nonnull RequestCallback<Void> callback) throws WebSocketException {
        InitRequest initRequest = dtoFactory.createDto(InitRequest.class);
        initRequest.setBare(bare);
        initRequest.setWorkingDir(project.getName());
        initRequest.setInitCommit(true);

        callback.setStatusHandler(new InitRequestStatusHandler(project.getName(), eventBus, constant));
        String url = gitServicePath + INIT + "?projectPath=" + project.getPath();

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(initRequest)).header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        wsMessageBus.send(message, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void cloneRepository(@Nonnull ProjectDescriptor project, @Nonnull String remoteUri, @Nonnull String remoteName,
                                @Nonnull RequestCallback<RepoInfo> callback) throws WebSocketException {
        CloneRequest cloneRequest = dtoFactory.createDto(CloneRequest.class)
                                              .withRemoteName(remoteName)
                                              .withRemoteUri(remoteUri)
                                              .withWorkingDir(project.getPath());

        String params = "?projectPath=" + project.getPath();
        callback.setStatusHandler(new CloneRequestStatusHandler(project.getPath(), remoteUri, eventBus, constant));

        String url = gitServicePath + CLONE + params;

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(cloneRequest))
               .header(CONTENTTYPE, APPLICATION_JSON)
               .header(ACCEPT, APPLICATION_JSON);
        Message message = builder.build();

        wsMessageBus.send(message, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void statusText(@Nonnull ProjectDescriptor project, StatusFormat format, @Nonnull AsyncRequestCallback<String> callback) {
        String url = baseHttpUrl + STATUS;
        String params = "?projectPath=" + project.getPath() + "&format=" + format;

        asyncRequestFactory.createPostRequest(url + params, null)
                           .loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON)
                           .header(ACCEPT, TEXT_PLAIN)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void add(@Nonnull ProjectDescriptor project, boolean update, @Nullable List<String> filePattern,
                    @Nonnull RequestCallback<Void> callback) throws WebSocketException {
        AddRequest addRequest = dtoFactory.createDto(AddRequest.class).withUpdate(update);
        if (filePattern == null) {
            addRequest.setFilepattern(AddRequest.DEFAULT_PATTERN);
        } else {
            addRequest.setFilepattern(filePattern);
        }
        callback.setStatusHandler(new AddRequestHandler(project.getName(), eventBus, constant));
        String url = gitServicePath + ADD + "?projectPath=" + project.getPath();

        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(addRequest))
               .header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        wsMessageBus.send(message, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void commit(@Nonnull ProjectDescriptor project, @Nonnull String message, boolean all, boolean amend,
                       @Nonnull AsyncRequestCallback<Revision> callback) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(all);
        String url = baseHttpUrl + COMMIT + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, commitRequest).loader(loader).send(callback);
    }

    @Override
    public void commit(@Nonnull final ProjectDescriptor project, @Nonnull final String message,
                       @Nonnull final List<String> files, final boolean amend,
                       @Nonnull final AsyncRequestCallback<Revision> callback) {
        CommitRequest commitRequest = dtoFactory.createDto(CommitRequest.class)
                                                .withMessage(message)
                                                .withAmend(amend)
                                                .withAll(false)
                                                .withFiles(files);
        String url = baseHttpUrl + COMMIT + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, commitRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void config(@Nonnull ProjectDescriptor project, @Nullable List<String> entries, boolean all,
                       @Nonnull AsyncRequestCallback<Map<String, String>> callback) {
        ConfigRequest configRequest = dtoFactory.createDto(ConfigRequest.class)
                                                .withGetAll(all)
                                                .withConfigEntry(entries);
        String url = baseHttpUrl + CONFIG + "?projectPath=" + project.getPath();

        asyncRequestFactory.createPostRequest(url, configRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void push(@Nonnull ProjectDescriptor project, @Nonnull List<String> refSpec, @Nonnull String remote,
                     boolean force, @Nonnull AsyncRequestCallback<PushResponse> callback) {
        PushRequest pushRequest =
                dtoFactory.createDto(PushRequest.class).withRemote(remote).withRefSpec(refSpec).withForce(force);
        String url = baseHttpUrl + PUSH + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, pushRequest).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void remoteList(@Nonnull ProjectDescriptor project, @Nullable String remoteName, boolean verbose,
                           @Nonnull AsyncRequestCallback<Array<Remote>> callback) {
        RemoteListRequest remoteListRequest = dtoFactory.createDto(RemoteListRequest.class).withVerbose(verbose);
        if (remoteName != null) {
            remoteListRequest.setRemote(remoteName);
        }
        String url = baseHttpUrl + REMOTE_LIST + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, remoteListRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchList(@Nonnull ProjectDescriptor project, @Nullable String remoteMode,
                           @Nonnull AsyncRequestCallback<Array<Branch>> callback) {
        BranchListRequest branchListRequest = dtoFactory.createDto(BranchListRequest.class).withListMode(remoteMode);
        String url = baseHttpUrl + BRANCH_LIST + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchListRequest).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void status(@Nonnull ProjectDescriptor project, @Nonnull AsyncRequestCallback<Status> callback) {
        String params = "?projectPath=" + project.getPath() + "&format=" + PORCELAIN;
        String url = baseHttpUrl + STATUS + params;
        asyncRequestFactory.createPostRequest(url, null).loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchDelete(@Nonnull ProjectDescriptor project, @Nonnull String name, boolean force,
                             @Nonnull AsyncRequestCallback<String> callback) {
        BranchDeleteRequest branchDeleteRequest =
                dtoFactory.createDto(BranchDeleteRequest.class).withName(name).withForce(force);
        String url = baseHttpUrl + BRANCH_DELETE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchDeleteRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchRename(@Nonnull ProjectDescriptor project, @Nonnull String oldName, @Nonnull String newName,
                             @Nonnull AsyncRequestCallback<String> callback) {
        String params = "?projectPath=" + project.getPath() + "&oldName=" + oldName + "&newName=" + newName;
        String url = baseHttpUrl + BRANCH_RENAME + params;
        asyncRequestFactory.createPostRequest(url, null).loader(loader)
                           .header(CONTENTTYPE, MimeType.APPLICATION_FORM_URLENCODED)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchCreate(@Nonnull ProjectDescriptor project, @Nonnull String name, @Nonnull String startPoint,
                             @Nonnull AsyncRequestCallback<Branch> callback) {

        BranchCreateRequest branchCreateRequest =
                dtoFactory.createDto(BranchCreateRequest.class).withName(name).withStartPoint(startPoint);
        String url = baseHttpUrl + BRANCH_CREATE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchCreateRequest).loader(loader).header(ACCEPT, APPLICATION_JSON).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void branchCheckout(@Nonnull ProjectDescriptor project, @Nonnull String name, @Nullable String startPoint,
                               boolean createNew, @Nonnull AsyncRequestCallback<String> callback) {
        BranchCheckoutRequest branchCheckoutRequest = dtoFactory.createDto(BranchCheckoutRequest.class)
                                                                .withName(name)
                                                                .withStartPoint(startPoint)
                                                                .withCreateNew(createNew);
        String url = baseHttpUrl + BRANCH_CHECKOUT + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, branchCheckoutRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(@Nonnull ProjectDescriptor project, List<String> items, boolean cached,
                       @Nonnull AsyncRequestCallback<String> callback) {
        RmRequest rmRequest = dtoFactory.createDto(RmRequest.class).withItems(items).withCached(cached).withRecursively(true);
        String url = baseHttpUrl + REMOVE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, rmRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void reset(@Nonnull ProjectDescriptor project, @Nonnull String commit, @Nullable ResetRequest.ResetType resetType,
                      @Nullable List<String> filePattern, @Nonnull AsyncRequestCallback<Void> callback) {

        ResetRequest resetRequest = dtoFactory.createDto(ResetRequest.class).withCommit(commit);
        if (resetType != null) {
            resetRequest.setType(resetType);
        }
        if (filePattern != null) {
            resetRequest.setFilePattern(filePattern);
        }
        String url = baseHttpUrl + RESET + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, resetRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void log(@Nonnull ProjectDescriptor project, boolean isTextFormat, @Nonnull AsyncRequestCallback<LogResponse> callback) {
        LogRequest logRequest = dtoFactory.createDto(LogRequest.class);
        String url = baseHttpUrl + LOG + "?projectPath=" + project.getPath();
        if (isTextFormat) {
            asyncRequestFactory.createPostRequest(url, logRequest).send(callback);
        } else {
            asyncRequestFactory.createPostRequest(url, logRequest).loader(loader).header(ACCEPT, APPLICATION_JSON).send(callback);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void remoteAdd(@Nonnull ProjectDescriptor project, @Nonnull String name, @Nonnull String repositoryURL,
                          @Nonnull AsyncRequestCallback<String> callback) {
        RemoteAddRequest remoteAddRequest = dtoFactory.createDto(RemoteAddRequest.class).withName(name).withUrl(repositoryURL);
        String url = baseHttpUrl + REMOTE_ADD + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, remoteAddRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void remoteDelete(@Nonnull ProjectDescriptor project, @Nonnull String name,
                             @Nonnull AsyncRequestCallback<String> callback) {
        String url = baseHttpUrl + REMOTE_DELETE + '/' + name + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, null).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void fetch(@Nonnull ProjectDescriptor project, @Nonnull String remote, List<String> refspec,
                      boolean removeDeletedRefs, @Nonnull RequestCallback<String> callback) throws WebSocketException {
        FetchRequest fetchRequest = dtoFactory.createDto(FetchRequest.class).withRefSpec(refspec).withRemote(remote)
                                              .withRemoveDeletedRefs(removeDeletedRefs);

        callback.setStatusHandler(new FetchRequestHandler(project.getName(), refspec, eventBus, constant));
        String url = gitServicePath + FETCH + "?projectPath=" + project.getPath();
        MessageBuilder builder = new MessageBuilder(POST, url);
        builder.data(dtoFactory.toJson(fetchRequest))
               .header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();
        wsMessageBus.send(message, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void pull(@Nonnull ProjectDescriptor project, @Nonnull String refSpec, @Nonnull String remote,
                     @Nonnull AsyncRequestCallback<PullResult> callback) {
        PullRequest pullRequest = dtoFactory.createDto(PullRequest.class).withRemote(remote).withRefSpec(refSpec);
        String url = baseHttpUrl + PULL + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, pullRequest).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void diff(@Nonnull ProjectDescriptor project, @Nonnull List<String> fileFilter,
                     @Nonnull DiffRequest.DiffType type, boolean noRenames, int renameLimit, @Nonnull String commitA,
                     @Nonnull String commitB, @Nonnull AsyncRequestCallback<String> callback) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(fileFilter)
                                            .withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withCommitB(commitB)
                                            .withRenameLimit(renameLimit);

        diff(diffRequest, project.getPath(), callback);
    }

    /** {@inheritDoc} */
    @Override
    public void diff(@Nonnull ProjectDescriptor project, @Nonnull List<String> fileFilter,
                     @Nonnull DiffRequest.DiffType type, boolean noRenames, int renameLimit, @Nonnull String commitA, boolean cached,
                     @Nonnull AsyncRequestCallback<String> callback) {
        DiffRequest diffRequest = dtoFactory.createDto(DiffRequest.class)
                                            .withFileFilter(fileFilter).withType(type)
                                            .withNoRenames(noRenames)
                                            .withCommitA(commitA)
                                            .withRenameLimit(renameLimit)
                                            .withCached(cached);

        diff(diffRequest, project.getPath(), callback);
    }

    /**
     * Make diff request.
     *
     * @param diffRequest
     *         request for diff
     * @param projectPath
     *         project path
     * @param callback
     *         callback
     */
    private void diff(DiffRequest diffRequest, @Nonnull String projectPath, AsyncRequestCallback<String> callback) {
        String url = baseHttpUrl + DIFF + "?projectPath=" + projectPath;
        asyncRequestFactory.createPostRequest(url, diffRequest).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void merge(@Nonnull ProjectDescriptor project, @Nonnull String commit,
                      @Nonnull AsyncRequestCallback<MergeResult> callback) {
        MergeRequest mergeRequest = dtoFactory.createDto(MergeRequest.class).withCommit(commit);
        String url = baseHttpUrl + MERGE + "?projectPath=" + project.getPath();
        asyncRequestFactory.createPostRequest(url, mergeRequest).loader(loader)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getGitReadOnlyUrl(@Nonnull ProjectDescriptor project, @Nonnull AsyncRequestCallback<String> callback) {
        String url = baseHttpUrl + RO_URL + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getCommitters(@Nonnull ProjectDescriptor project, @Nonnull AsyncRequestCallback<Commiters> callback) {
        String url = baseHttpUrl + COMMITERS + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).header(ACCEPT, APPLICATION_JSON).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteRepository(@Nonnull ProjectDescriptor project, @Nonnull AsyncRequestCallback<Void> callback) {
        String url = baseHttpUrl + DELETE_REPOSITORY + "?projectPath=" + project.getPath();
        asyncRequestFactory.createGetRequest(url).loader(loader)
                           .header(CONTENTTYPE, APPLICATION_JSON).header(ACCEPT, TEXT_PLAIN)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUrlVendorInfo(@Nonnull String vcsUrl, @Nonnull AsyncRequestCallback<GitUrlVendorInfo> callback) {
        asyncRequestFactory.createGetRequest(baseHttpUrl + "/git-service/info?vcsurl=" + vcsUrl)
                           .header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON).send(
                callback);
    }
}