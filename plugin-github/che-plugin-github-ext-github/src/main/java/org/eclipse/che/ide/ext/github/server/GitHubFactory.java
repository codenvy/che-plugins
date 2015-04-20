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

import com.google.inject.Inject;

import org.eclipse.che.api.auth.oauth.OAuthTokenProvider;
import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.api.core.UnauthorizedException;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * Factory class used to generate connection to GitHub
 *
 * @author Igor Vinokur
 */
public class GitHubFactory {

    private final OAuthTokenProvider oauthTokenProvider;

    @Inject
    private GitHubFactory(OAuthTokenProvider oauthTokenProvider) {
        this.oauthTokenProvider = oauthTokenProvider;
    }

    /**
     * Connect to GitHub API
     * @return connected GitHub API class
     * @throws IOException
     */
    public GitHub connect() throws Exception {
        try {
            return GitHub.connectUsingOAuth(getToken(getUserId()));
        } catch (IOException e ) {
            throw new UnauthorizedException(e.getMessage());
        }
    }

    /**
     * Get user's token
     * @param user user's ID
     * @return authentication token
     * @throws IOException
     * @deprecated Use getToken method from rest service
     */
    public String getToken(String user) throws Exception {
        OAuthToken token = null;
        try {
            token = oauthTokenProvider.getToken("github", user);
        } catch (IOException e) {
            throw new ServerException(e.getMessage());
        }
        String oauthToken = token != null ? token.getToken() : null;
        if (oauthToken == null || oauthToken.isEmpty()) {
            throw new NotFoundException("Token is empty");
        }
        return oauthToken;
    }

    private static String getUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}