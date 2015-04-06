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
package org.eclipse.che.ide.ext.svn.server;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.vfs.server.ContentStream;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.Strings;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsException;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider.Credentials;
import org.eclipse.che.ide.ext.svn.server.repository.RepositoryUrlProvider;
import org.eclipse.che.ide.ext.svn.server.rest.SubversionService;
import org.eclipse.che.ide.ext.svn.server.upstream.CommandLineResult;
import org.eclipse.che.ide.ext.svn.server.upstream.UpstreamUtils;
import org.eclipse.che.ide.ext.svn.server.utils.InfoUtils;
import org.eclipse.che.ide.ext.svn.server.utils.SubversionUtils;
import org.eclipse.che.ide.ext.svn.shared.AddRequest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputWithRevisionResponse;
import org.eclipse.che.ide.ext.svn.shared.CheckoutRequest;
import org.eclipse.che.ide.ext.svn.shared.CleanupRequest;
import org.eclipse.che.ide.ext.svn.shared.CommitRequest;
import org.eclipse.che.ide.ext.svn.shared.Constants;
import org.eclipse.che.ide.ext.svn.shared.CopyRequest;
import org.eclipse.che.ide.ext.svn.shared.InfoRequest;
import org.eclipse.che.ide.ext.svn.shared.InfoResponse;
import org.eclipse.che.ide.ext.svn.shared.LockRequest;
import org.eclipse.che.ide.ext.svn.shared.MoveRequest;
import org.eclipse.che.ide.ext.svn.shared.RemoveRequest;
import org.eclipse.che.ide.ext.svn.shared.ResolveRequest;
import org.eclipse.che.ide.ext.svn.shared.RevertRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowDiffRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowLogRequest;
import org.eclipse.che.ide.ext.svn.shared.StatusRequest;
import org.eclipse.che.ide.ext.svn.shared.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides Subversion APIs.
 */
@Singleton
public class SubversionApi {

    private static Logger LOG = LoggerFactory.getLogger(SubversionApi.class);

    private final CredentialsProvider   credentialsProvider;
    private final RepositoryUrlProvider repositoryUrlProvider;

    private final ConcurrentHashMap<String, ExportedItem> exportedItems = new ConcurrentHashMap<>();

    private ScheduledExecutorService cleanScheduler;
    private static final int EXPORTED_ITEM_CLEAN_PERIOD = 30; // 30 minutes

    private class ExportedItem {
        String exportedPath;
        File   zip;
        long   generatedAt;

        public ExportedItem(String exportedPath, File zip, long generatedAt) {
            this.exportedPath = exportedPath;
            this.zip = zip;
            this.generatedAt = generatedAt;
        }
    }

    @PostConstruct
    public void start() {
        cleanScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("ExportedItemsScheduler-")
                                                                                              .setDaemon(true).build());

        cleanScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<String, ExportedItem>> iterator = exportedItems.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, ExportedItem> exportedItemEntry = iterator.next();
                    if (System.currentTimeMillis() - exportedItemEntry.getValue().generatedAt > 1000 * 60 * 30) {
                        if (!IoUtil.deleteRecursive(exportedItemEntry.getValue().zip.getParentFile())) {
                            LOG.warn("Item {} deletion was failed.", exportedItemEntry.getValue().exportedPath);
                        }
                        iterator.remove();
                    }
                }
            }
        }, EXPORTED_ITEM_CLEAN_PERIOD, EXPORTED_ITEM_CLEAN_PERIOD, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() {
        cleanScheduler.shutdownNow();
    }

    @Inject
    public SubversionApi(final CredentialsProvider credentialsProvider,
                         final RepositoryUrlProvider repositoryUrlProvider) {
        this.credentialsProvider = credentialsProvider;
        this.repositoryUrlProvider = repositoryUrlProvider;
    }

    /**
     * Perform an "svn add" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse add(final AddRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        // Flags
        addFlag(cliArgs, "--no-ignore", request.isAddIgnored());
        addFlag(cliArgs, "--parents", request.isAddParents());

        if (request.isAutoProps()) {
            cliArgs.add("--auto-props");
        }

        if (request.isNotAutoProps()) {
            cliArgs.add("--no-auto-props");
        }

        // Options
        addOption(cliArgs, "--depth", request.getDepth());

        // Command Name
        cliArgs.add("add");

        // Command Arguments

        final CommandLineResult result = runCommand(cliArgs, projectPath, request.getPaths());

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout())
                         .withErrOutput(result.getStderr());
    }


    /**
     * Perform an "svn revert" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse revert(RevertRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);
        addOption(cliArgs, "--depth", request.getDepth());

        cliArgs.add("revert");

        final CommandLineResult result = runCommand(cliArgs, projectPath, addWorkingCopyPathIfNecessary(request.getPaths()));

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout())
                         .withErrOutput(result.getStderr());
    }

    /**
     * Perform an "svn copy" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse copy(final CopyRequest request) throws IOException, SubversionException {

        //for security reason we should forbid file protocol
        if (request.getSource().startsWith("file://") || request.getDestination().startsWith("file://")) {
            throw new SubversionException("Url is not acceptable");
        }

        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        if (!Strings.isNullOrEmpty(request.getComment())) {
            addOption(cliArgs, "--message", "\"" + request.getComment() + "\"");
        }

        // Command Name
        cliArgs.add("copy");

        final CommandLineResult result = runCommand(cliArgs, projectPath, Arrays.asList(request.getSource(), request.getDestination()));

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout())
                         .withErrOutput(result.getStderr());
    }

    /**
     * Perform an "svn checkout" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputWithRevisionResponse checkout(final CheckoutRequest request)
            throws IOException, SubversionException {
        return checkout(request, null);
    }

    public CLIOutputWithRevisionResponse checkout(final CheckoutRequest request, final String[] credentials)
            throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        // Flags
        addFlag(cliArgs, "--ignore-externals", request.isIgnoreExternals());

        // Options
        addOption(cliArgs, "--depth", request.getDepth());
        addOption(cliArgs, "--revision", request.getRevision());

        // Command Name
        cliArgs.add("checkout");

        // Command Arguments
        cliArgs.add(request.getUrl());
        cliArgs.add(projectPath.getAbsolutePath());

        CommandLineResult result;
        if (credentials == null) {
            result = runCommand(cliArgs, projectPath, request.getPaths());
        } else {
            result = runCommand(cliArgs, projectPath, request.getPaths(), credentials);
        }

        return DtoFactory.getInstance()
                         .createDto(CLIOutputWithRevisionResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withRevision(SubversionUtils.getCheckoutRevision(result.getStdout()));
    }

    /**
     * Perform an "svn commit" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputWithRevisionResponse commit(final CommitRequest request)
            throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        // Flags
        addFlag(cliArgs, "--keep-changelists", request.isKeepChangeLists());
        addFlag(cliArgs, "--no-unlock", request.isKeepLocks());

        // Command Name
        cliArgs.add("commit");

        // Command Arguments
        cliArgs.add("-m");
        cliArgs.add(request.getMessage());

        final CommandLineResult result = runCommand(cliArgs, projectPath,
                                                    addWorkingCopyPathIfNecessary(request.getPaths()));

        return DtoFactory.getInstance()
                         .createDto(CLIOutputWithRevisionResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withRevision(SubversionUtils.getCommitRevision(result.getStdout()))
                         .withOutput(result.getStdout());
    }

    /**
     * Perform an "svn remove" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse remove(final RemoveRequest request)
            throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        // Command Name
        cliArgs.add("remove");

        final CommandLineResult result = runCommand(cliArgs, projectPath, request.getPaths());

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout());
    }

    /**
     * Perform an "svn status" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse status(final StatusRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        // Flags
        addFlag(cliArgs, "--ignore-externals", request.isIgnoreExternals());
        addFlag(cliArgs, "--no-ignore", !request.isShowIgnored());
        addFlag(cliArgs, "--quiet", !request.isShowUnversioned());
        addFlag(cliArgs, "--show-updates", request.isShowUpdates());
        addFlag(cliArgs, "--verbose", request.isVerbose());

        // Options
        addOptionList(cliArgs, "--changelist", request.getChangeLists());
        addOption(cliArgs, "--depth", request.getDepth());

        // Command Name
        cliArgs.add("status");

        final CommandLineResult result = runCommand(cliArgs, projectPath,
                                                    addWorkingCopyPathIfNecessary(request.getPaths()));

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout());
    }

    /**
     * Perform an "svn checkout" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputWithRevisionResponse update(final UpdateRequest request)
            throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> uArgs = new LinkedList<>();

        addStandardArgs(uArgs);

        // Flags
        addFlag(uArgs, "--ignore-externals", request.isIgnoreExternals());

        // Options
        addOption(uArgs, "--depth", request.getDepth());
        addOption(uArgs, "--revision", request.getRevision());

        // Command Name
        uArgs.add("update");

        final CommandLineResult result = runCommand(uArgs, projectPath,
                                                    addWorkingCopyPathIfNecessary(request.getPaths()));

        return DtoFactory.getInstance()
                         .createDto(CLIOutputWithRevisionResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withRevision(SubversionUtils.getUpdateRevision(result.getStdout()))
                         .withOutput(result.getStdout());
    }

    /**
     * Perform an "svn log" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse showLog(final ShowLogRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> uArgs = new LinkedList<>();

        addStandardArgs(uArgs);
        addOption(uArgs, "--revision", request.getRevision());
        uArgs.add("log");

        final CommandLineResult result = runCommand(uArgs, projectPath, request.getPaths());

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout());
    }

    public CLIOutputResponse lockUnlock(final LockRequest request, final boolean lock) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());

        final List<String> args = new ArrayList<>();
        addStandardArgs(args);
        addFlag(args, "--force", request.isForce());

        // command
        if (lock) {
            args.add("lock");
        } else {
            args.add("unlock");
        }

        final CommandLineResult result = runCommand(args, projectPath, request.getTargets());

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout())
                         .withErrOutput(result.getStderr());
    }

    /**
     * Perform an "svn diff" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse showDiff(final ShowDiffRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> uArgs = new LinkedList<>();

        addStandardArgs(uArgs);
        addOption(uArgs, "--revision", request.getRevision());
        uArgs.add("diff");

        final CommandLineResult result = runCommand(uArgs, projectPath, request.getPaths());

        return DtoFactory.getInstance()
                .createDto(CLIOutputResponse.class)
                .withCommand(result.getCommandLine().toString())
                .withOutput(result.getStdout());
    }

    /**
     * Perform an "svn resolve" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse resolve(final ResolveRequest request) throws IOException, SubversionException {
        final File projectPath = new File(request.getProjectPath());

        Map<String, String> resolutions = request.getConflictResolutions();
        List<String> results = new ArrayList<String>();

        for (String path : resolutions.keySet()) {
            final List<String> uArgs = new LinkedList<>();

            addStandardArgs(uArgs);
            addDepth(uArgs, request.getDepth());
            addOption(uArgs, "--accept", resolutions.get(path));
            uArgs.add("resolve");

            final CommandLineResult result = runCommand(uArgs, projectPath, Arrays.asList(path));

            results.addAll(result.getStdout());
        }

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withOutput(results);
    }

    /**
     * Perform an "svn export" based on the request.
     *
     * @param projectPath
     *         project path
     * @param path
     *         exported path
     * @param revision
     *         specified revision to export
     * @param serviceContext
     *         REST service context
     * @param workspace
     *         current user workspace
     * @return Response which contains hyperlink with download url
     * @throws IOException
     *         if there is a problem executing the command
     * @throws ServerException
     *         if there is an exporting issue
     */
    public Response exportPath(String projectPath, String path, String revision, ServiceContext serviceContext, String workspace)
            throws IOException, ServerException {

        final File project = new File(projectPath);
        final List<String> uArgs = new LinkedList<>();

        addStandardArgs(uArgs);

        if (!Strings.isNullOrEmpty(revision)) {
            addOption(uArgs, "--revision", revision);
        }

        uArgs.add("--force");
        uArgs.add("export");

        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        final CommandLineResult result = runCommand(uArgs, project, Arrays.asList(path, tempDir.getAbsolutePath()));

        if (result.getExitCode() != 0) {
            throw new ServerException("Exporting was failed.");
        }

        final String fileName = Paths.get(".".equals(path) ? projectPath : path).getFileName().toString() + ".zip";

        File zip = new File(tempDir, fileName);
        ZipUtils.zipDir(tempDir.getPath(), zip.getParentFile(), zip, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.equals(fileName);
            }
        });

        exportedItems.put(fileName, new ExportedItem(path, zip, System.currentTimeMillis()));

        Link link = DtoFactory.getInstance().createDto(Link.class)
                              .withRel(Constants.REL_DOWNLOAD_EXPORT_PATH)
                              .withHref(serviceContext.getServiceUriBuilder().clone().path(SubversionService.class, "downloadExportedPath")
                                                      .build(workspace, fileName)
                                                      .toString())
                              .withMethod("GET").withProduces(MediaType.ZIP.toString());


        Hyperlinks links = DtoFactory.getInstance().createDto(Hyperlinks.class)
                                     .withLinks(Collections.singletonList(link));

        return Response.ok(links, MediaType.JSON_UTF_8.toString()).build();
    }

    /**
     * Download stored zip created by method {@link SubversionApi#exportPath}
     *
     * @param path
     *         exported path
     * @return ContentStream of zip archive
     * @throws NotFoundException
     *         in case if exported zip wasn't found
     * @throws IOException
     *         if there is a problem executing the command
     */
    public ContentStream downloadExportedPath(String path) throws NotFoundException, IOException {
        if (!exportedItems.containsKey(path)) {
            throw new NotFoundException("Exported zip for path '" + path + "' doesn't exists or deleted due to expiration.");
        }

        ExportedItem item = exportedItems.get(path);

        return new ContentStream(item.zip.getName(),
                                 new FileInputStream(item.zip),
                                 MediaType.ZIP.toString(),
                                 item.zip.length(),
                                 new Date(item.generatedAt));
    }

    /**
     * Perform an "svn move" based on the request.
     *
     * @param request
     *         the request
     * @return the response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    public CLIOutputResponse move(final MoveRequest request) throws IOException, SubversionException {

        Predicate<String> sourcePredicate = new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.startsWith("file://");
            }
        };

        //for security reason we should forbid file protocol
        if (Iterables.any(request.getSource(), sourcePredicate) || request.getDestination().startsWith("file://")) {
            throw new SubversionException("Url is not acceptable");
        }

        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new LinkedList<>();

        addStandardArgs(cliArgs);

        if (!Strings.isNullOrEmpty(request.getComment())) {
            addOption(cliArgs, "--message", "\"" + request.getComment() + "\"");
        }

        // Command Name
        cliArgs.add("move");

        final List<String> paths = new ArrayList<>();
        paths.addAll(request.getSource());
        paths.add(request.getDestination());

        final CommandLineResult result = runCommand(cliArgs, projectPath, paths);

        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout())
                         .withErrOutput(result.getStderr());
    }

    private static void addDepth(final List<String> args, final String depth) {
        if (depth != null && !depth.isEmpty()) {
            args.add("--depth");
            args.add(depth);
        }
    }

    /** Adds flag to arguments when value is true. */
    private void addFlag(final List<String> args, final String argName, final boolean value) {
        if (value) {
            args.add(argName);
        }
    }

    /** Adds an option to arguments. */
    private void addOption(final List<String> args, final String optName, final String value) {
        if (value != null && !value.isEmpty()) {
            args.add(optName);
            args.add(value);
        }
    }

    /** Adds multivalued option to arguments. */
    private void addOptionList(final List<String> args, final String optName, final List<String> values) {
        for (final String value : values) {
            if (value != null && !value.isEmpty()) {
                args.add(optName);
                args.add(value);
            }
        }
    }

    private void addStandardArgs(final List<String> args) {
        args.add("--no-auth-cache");
        args.add("--non-interactive");
        args.add("--trust-server-cert");
    }

    private List<String> addWorkingCopyPathIfNecessary(List<String> paths) {
        if (paths == null) {
            paths = new ArrayList<>();
        }

        // If there are no paths, add the working copy root to the list of paths
        if (paths.size() == 0) {
            paths.add(".");
        }

        return paths;
    }

    private CommandLineResult runCommand(final List<String> args, final File projectPath,
                                         final List<String> paths) throws IOException, SubversionException {
        return runCommand(args, projectPath, paths, getCredentialArgs(projectPath.getAbsolutePath()));
    }

    private CommandLineResult runCommand(final List<String> args, final File projectPath,
                                         final List<String> paths, final String[] credentials) throws IOException, SubversionException {
        final List<String> lines = new ArrayList<>();
        final CommandLineResult result;
        final StringBuffer buffer;
        boolean isWarning = false;

        // Add paths to the end of the list of arguments
        for (final String path : paths) {
            args.add(path);
        }

        String[] credentialsArgs;
        if (credentials != null && credentials.length == 2) {
            credentialsArgs = new String[]{"--username", credentials[0], "--password", credentials[1]};
        } else {
            credentialsArgs = null;
        }

        final Map<String, String> env = new HashMap<>();
        env.put("LANG", "C");
        result = UpstreamUtils.executeCommandLine(env, "svn", args.toArray(new String[args.size()]),
                                                  credentialsArgs, -1, projectPath);

        if (result.getExitCode() != 0) {
            buffer = new StringBuffer();

            lines.addAll(result.getStdout());
            lines.addAll(result.getStderr());

            for (final String line : lines) {
                // Subversion returns an error code of 1 even when the "error" is just a warning
                if (line.startsWith("svn: warning: ")) {
                    isWarning = true;
                }

                buffer.append(line);
                buffer.append("\n");
            }

            if (!isWarning) {
                throw new SubversionException(buffer.toString());
            }
        }

        return result;
    }

    private String[] getCredentialArgs(final String projectPath) throws SubversionException, IOException {
        Credentials credentials;
        try {
            credentials = this.credentialsProvider.getCredentials(getRepositoryUrl(projectPath));
        } catch (final CredentialsException e) {
            credentials = null;
        }
        if (credentials != null) {
            return new String[]{credentials.getUsername(), credentials.getPassword()};
        } else {
            return null;
        }
    }

    public String getRepositoryUrl(final String projectPath) throws SubversionException, IOException {
        return this.repositoryUrlProvider.getRepositoryUrl(projectPath);
    }

    public InfoResponse info(final InfoRequest request) throws SubversionException, IOException {
        final File projectPath = new File(request.getProjectPath());
        List<String> pathList;
        if (request.getPath() == null) {
            pathList = Collections.emptyList();
        } else {
            pathList = Collections.singletonList(request.getPath());
        }
        final CommandLineResult clResult = runCommand(Collections.singletonList("info"),
                                                      projectPath,
                                                      pathList,
                                                      null);
        final InfoResponse response = DtoFactory.getInstance().createDto(InfoResponse.class)
                                                .withCommandLine(clResult.getCommandLine().toString())
                                                .withExitCode(clResult.getExitCode());
        if (clResult.getExitCode() == 0) {
            response.withRepositoryUrl(InfoUtils.getRepositoryUrl(clResult.getStdout()))
                    .withRepositoryRoot(InfoUtils.getRepositoryRootUrl(clResult.getStdout()))
                    .withRevision(InfoUtils.getRevision(clResult.getStdout()));
        } else {
            response.withErrorOutput(clResult.getStderr());
        }

        return response;
    }

    public CLIOutputResponse cleanup(final CleanupRequest request) throws SubversionException, IOException {
        final File projectPath = new File(request.getProjectPath());
        final List<String> cliArgs = new ArrayList<>();

        addStandardArgs(cliArgs);

        // Command Name
        cliArgs.add("cleanup");

        final CommandLineResult result = runCommand(cliArgs, projectPath, addWorkingCopyPathIfNecessary(request.getPaths()));
        return DtoFactory.getInstance()
                         .createDto(CLIOutputResponse.class)
                         .withCommand(result.getCommandLine().toString())
                         .withOutput(result.getStdout());

    }
}
