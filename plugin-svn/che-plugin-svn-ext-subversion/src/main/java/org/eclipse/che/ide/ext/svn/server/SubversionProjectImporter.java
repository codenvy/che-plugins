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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Map;

import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsException;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.shared.CheckoutRequest;
import org.eclipse.che.ide.ext.svn.shared.ImportParameterKeys;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ProjectImporter} for Subversion.
 */
@Singleton
public class SubversionProjectImporter implements ProjectImporter {

    public static final String ID = "subversion";

    private final LocalPathResolver localPathResolver;

    private final SubversionApi subversionApi;

    private final CredentialsProvider credentialsProvider;

    /**
     * Constructor.
     */
    @Inject
    public SubversionProjectImporter(final CredentialsProvider credentialsProvider,
                                     final LocalPathResolver localPathResolver,
                                     final SubversionApi subversionApi) {
        this.localPathResolver = localPathResolver;
        this.subversionApi = subversionApi;
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Import project from Subversion repository URL.";
    }
    
    @Override
    public ImporterCategory getCategory() {
        return ImporterCategory.SOURCE_CONTROL;
    }

    @Override
    public void importSources(final FolderEntry baseFolder, final String location, final Map<String, String> parameters)
            throws IOException, ForbiddenException, ServerException, UnauthorizedException, ConflictException {
        importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
    }

    @Override
    public void importSources(final FolderEntry baseFolder, final String location,
                              final Map<String, String> parameters, final LineConsumerFactory factory)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        if (!baseFolder.isFolder()) {
            throw new IOException("Project cannot be imported into \"" + baseFolder.getName() + "\".  "
                                          + "It is not a folder.");
        }

        // can't store the credentials yet, no workspaceId/projectId
        String[] credentials = null;
        String username = "";
        String password = "";
        if (parameters != null) {
            String paramUsername = parameters.get(ImportParameterKeys.PARAMETER_USERNAME);
            if (paramUsername != null) {
                username = paramUsername;
            }
            String paramPassword = parameters.get(ImportParameterKeys.PARAMETER_PASSWORD);
            if (paramPassword != null) {
                password = paramPassword;
            }
            credentials = new String[]{username, password};
            try {
                this.credentialsProvider.storeCredential(location, new CredentialsProvider.Credentials(username, password));
            } catch (final CredentialsException e) {
                LoggerFactory.getLogger(SubversionProjectImporter.class.getName())
                             .warn("Could not store credentials - try to continue anyway." + e.getMessage());
            }
        }
        // Perform checkout
        this.subversionApi.checkout(DtoFactory.getInstance()
                                         .createDto(CheckoutRequest.class)
                                         .withProjectPath(localPathResolver
                                                                  .resolve((VirtualFileImpl)baseFolder
                                                                          .getVirtualFile()))
                                         .withUrl(location),
                                    credentials);
    }

}
