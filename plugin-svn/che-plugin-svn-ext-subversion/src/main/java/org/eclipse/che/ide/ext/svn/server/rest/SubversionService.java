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
package org.eclipse.che.ide.ext.svn.server.rest;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.shared.PropertyFilter;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.ide.ext.svn.server.SubversionApi;
import org.eclipse.che.ide.ext.svn.server.SubversionException;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsException;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider.Credentials;
import org.eclipse.che.ide.ext.svn.shared.AddRequest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponseList;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputWithRevisionResponse;
import org.eclipse.che.ide.ext.svn.shared.CleanupRequest;
import org.eclipse.che.ide.ext.svn.shared.CommitRequest;
import org.eclipse.che.ide.ext.svn.shared.CopyRequest;
import org.eclipse.che.ide.ext.svn.shared.InfoRequest;
import org.eclipse.che.ide.ext.svn.shared.InfoResponse;
import org.eclipse.che.ide.ext.svn.shared.LockRequest;
import org.eclipse.che.ide.ext.svn.shared.MoveRequest;
import org.eclipse.che.ide.ext.svn.shared.RemoveRequest;
import org.eclipse.che.ide.ext.svn.shared.ResolveRequest;
import org.eclipse.che.ide.ext.svn.shared.RevertRequest;
import org.eclipse.che.ide.ext.svn.shared.SaveCredentialsRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowDiffRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowLogRequest;
import org.eclipse.che.ide.ext.svn.shared.StatusRequest;
import org.eclipse.che.ide.ext.svn.shared.UpdateRequest;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * REST API endpoints for this extension.
 */
@Path("svn/{ws-id}")
public class SubversionService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(SubversionService.class);

    private final LocalPathResolver         localPathResolver;
    private final VirtualFileSystemRegistry vfsRegistry;
    private final SubversionApi             subversionApi;

    private final CredentialsProvider credentialsProvider;

    @PathParam("ws-id")
    private String workspaceId;
    @QueryParam("projectId")
    private String projectId;

    /**
     * Constructor.
     */
    @Inject
    public SubversionService(final LocalPathResolver localPathResolver,
                             final VirtualFileSystemRegistry vfsRegistry,
                             final SubversionApi subversionApi,
                             final CredentialsProvider credentialsProvider) {
        this.localPathResolver = localPathResolver;
        this.vfsRegistry = vfsRegistry;
        this.subversionApi = subversionApi;
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Add the selected paths to version control.
     *
     * @param request
     *         the add request
     * @return the add response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse add(final AddRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.add(request);
    }

    /**
     * Remove the selected paths to version control.
     *
     * @param request
     *         the remove request
     * @return the remove response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("remove")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse remove(final RemoveRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.remove(request);
    }

    /**
     * Revert the selected paths.
     *
     * @param request
     *         the revert request
     * @return the revert response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("revert")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse revert(final RevertRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.revert(request);
    }

    /**
     * Copy provided path.
     *
     * @param request
     *         the copy request
     * @return the copy response
     * @throws ServerException
     *         if there is a Subversion issue
     * @throws IOException
     *         if there is a problem executing the command
     */
    @Path("copy")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse copy(final CopyRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.copy(request);
    }

    /**
     * Retrieve the status of the paths in the request or the working copy as a whole.
     *
     * @param request
     *         the status request
     * @return the status response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("status")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse update(final StatusRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.status(request);
    }

    @Path("info")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public InfoResponse info(final InfoRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.info(request);
    }

    /**
     * Update the working copy.
     *
     * @param request
     *         the update request
     * @return the update response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("update")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputWithRevisionResponse update(final UpdateRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.update(request);
    }

    /**
     * Show log.
     *
     * @param request
     *         the show log request
     * @return the show log response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("showlog")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse showLog(final ShowLogRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.showLog(request);
    }

    /**
     * Show diff.
     *
     * @param request
     *         the show diff request
     * @return the show diff response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("showdiff")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse showDiff(final ShowDiffRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.showDiff(request);
    }

    /**
     * Resolve conflicts.
     *
     * @param request
     *         the resolve conflicts request
     * @return the resolve conflicts response
     * @throws IOException
     *         if there is a problem executing the command
     * @throws SubversionException
     *         if there is a Subversion issue
     */
    @Path("resolve")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponseList resolve(final ResolveRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return subversionApi.resolve(request);
    }

    /**
     * Commits the specified changes.
     *
     * @param request
     *         the commit request
     * @return the commit response
     * @throws ServerException
     * @throws IOException
     */
    @Path("commit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputWithRevisionResponse commit(final CommitRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.commit(request);
    }

    /**
     * Cleans up the working copy.
     *
     * @param request
     *         the cleanup request
     * @return the response
     * @throws ServerException
     * @throws IOException
     */
    @Path("cleanup")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse cleanup(final CleanupRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.cleanup(request);
    }

    @Path("lock")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse lock(final LockRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.lockUnlock(request, true);
    }

    @Path("unlock")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse unlock(final LockRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.lockUnlock(request, false);
    }

    @Path("saveCredentials")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public void saveCredentials(final SaveCredentialsRequest request) throws ServerException, IOException {
        try {
            this.credentialsProvider.storeCredential(request.getRepositoryUrl(),
                                                     new Credentials(request.getUsername(), request.getPassword()));
        } catch (final CredentialsException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @Path("export/{projectPath:.*}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportPath(final @PathParam("projectPath") String projectPath,
                               final @DefaultValue(".") @QueryParam("path") String path,
                               final @QueryParam("revision") String revision) throws ServerException, IOException {
        return this.subversionApi.exportPath(getRealPath(projectPath), path, revision);
    }

    /**
     * Move provided path.
     *
     * @param request
     *         the copy request
     * @return the copy response
     * @throws ServerException
     *         if there is a Subversion issue
     * @throws IOException
     *         if there is a problem executing the command
     */
    @Path("move")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public CLIOutputResponse move(final MoveRequest request) throws ServerException, IOException {
        request.setProjectPath(getRealPath(request.getProjectPath()));
        return this.subversionApi.move(request);
    }

    /**
     * Convert the project path to a local filesystem path.
     *
     * @param path
     *         the project path
     * @return the local filesystem path
     * @throws ServerException
     *         if something goes wrong
     */
    private String getRealPath(final String path) throws ServerException {
        try {
            final VirtualFileSystem vfs = vfsRegistry.getProvider(workspaceId).newInstance(null);
            final Item item = vfs.getItemByPath(path, null, false, PropertyFilter.ALL_FILTER);
            final MountPoint mountPoint = vfs.getMountPoint();
            final VirtualFile virtualFile = mountPoint.getVirtualFile(item.getPath());

            return localPathResolver.resolve((VirtualFileImpl)virtualFile);
        } catch (ForbiddenException | NotFoundException | ServerException e) {
            LOG.error("Some unknown exception", e);
            throw new ServerException(e);
        }
    }

}
