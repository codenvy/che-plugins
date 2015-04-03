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
import org.eclipse.che.commons.env.EnvironmentContext;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public class GitHubFactory {

    private final OAuthTokenProvider oauthTokenProvider;

    @Inject
    private GitHubFactory(OAuthTokenProvider oauthTokenProvider) {
        this.oauthTokenProvider = oauthTokenProvider;
    }

    public GitHub connect() throws IOException {
        return GitHub.connectUsingOAuth(getToken(getUserId()));
    }

    public String getToken(String user) throws IOException {
        OAuthToken token = oauthTokenProvider.getToken("github", user);
        String oauthToken = token != null ? token.getToken() : null;
        if (oauthToken == null || oauthToken.isEmpty()) {
            return "";
        }
        return oauthToken;
    }

    private static String getUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}