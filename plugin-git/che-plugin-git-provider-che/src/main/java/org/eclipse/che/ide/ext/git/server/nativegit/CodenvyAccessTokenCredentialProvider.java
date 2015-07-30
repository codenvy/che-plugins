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
package org.eclipse.che.ide.ext.git.server.nativegit;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.git.impl.nativegit.CredentialsProvider;
import org.eclipse.che.git.impl.nativegit.UserCredential;
import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Credentials provider for Codenvy
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class CodenvyAccessTokenCredentialProvider implements CredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CodenvyAccessTokenCredentialProvider.class);

    private final String codenvyHost;
    private final String apiEndpoint;

    @Inject
    public CodenvyAccessTokenCredentialProvider(@Named("api.endpoint") String apiEndPoint) throws URISyntaxException {
        this.apiEndpoint = apiEndPoint;
        this.codenvyHost = new URI(apiEndPoint).getHost();
    }

    @Override
    public UserCredential getUserCredential() throws GitException {
        String token = EnvironmentContext.getCurrent().getUser().getToken();
        if (token != null) {
            return new UserCredential(token, "x-codenvy", "codenvy");
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        try {

            User user = EnvironmentContext.getCurrent().getUser();
            GitUser gitUser = DtoFactory.getInstance().createDto(GitUser.class);
            if (user.isTemporary()) {
                return gitUser.withEmail("anonymous@noemail.com")
                              .withName("Anonymous");
            } else {

                Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("GET")
                                      .withHref(UriBuilder.fromUri(apiEndpoint).path("profile").build().toString());
                final ProfileDescriptor profile = HttpJsonHelper.request(ProfileDescriptor.class, link);


                String firstName = profile.getAttributes().get("firstName");
                String lastName = profile.getAttributes().get("lastName");
                String email = profile.getAttributes().get("email");

                String name;
                if (firstName != null || lastName != null) {
                    // add this temporary for fixing problem with "<none>" in last name of user from profile
                    name = Joiner.on(" ").skipNulls().join(firstName, lastName.contains("<none>") ? "" : lastName);
                } else {
                    name = user.getName();
                }
                gitUser.setName(name != null && !name.isEmpty() ? name : "Anonymous");
                gitUser.setEmail(email != null ? email : "anonymous@noemail.com");
                return gitUser;

            }

        } catch (IOException | ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            LOG.warn(e.getLocalizedMessage());
            // throw new GitException(e);
        }
        return null;
    }

    @Override
    public String getId() {
        return "codenvy";
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return url.contains(codenvyHost);
    }

}

