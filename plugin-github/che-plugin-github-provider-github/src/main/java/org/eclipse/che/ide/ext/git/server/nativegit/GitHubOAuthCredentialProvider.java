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

import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.git.impl.nativegit.CredentialsProvider;
import org.eclipse.che.git.impl.nativegit.UserCredential;
import org.eclipse.che.security.oauth.GitHubOAuthAuthenticator;
import org.eclipse.che.security.oauth.OAuthAuthenticationException;
import org.eclipse.che.security.oauth.shared.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * @author Sergii Kabashniuk
 */
@Singleton
public class GitHubOAuthCredentialProvider implements CredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubOAuthCredentialProvider.class);

    private static String OAUTH_PROVIDER_NAME = "github";
    private final GitHubOAuthAuthenticator authAuthenticator;


    @Inject
    public GitHubOAuthCredentialProvider(GitHubOAuthAuthenticator authAuthenticator) {
        this.authAuthenticator = authAuthenticator;

    }

    @Override
    public UserCredential getUserCredential() throws GitException {
        try {
            OAuthToken token = authAuthenticator.getToken(EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {
                return new UserCredential(token.getToken(), token.getToken(), OAUTH_PROVIDER_NAME);
            }
        } catch (IOException e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        try {
            OAuthToken token = authAuthenticator.getToken(EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {
                User user = authAuthenticator.getUser(token);
                if (user != null) {
                    return DtoFactory.getInstance().createDto(GitUser.class)
                                     .withEmail(user.getEmail())
                                     .withName(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Anonymous");
                }

            }
        } catch (IOException | OAuthAuthenticationException e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }


    @Override
    public String getId() {
        return OAUTH_PROVIDER_NAME;
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return url.contains("github.com");
    }


}
