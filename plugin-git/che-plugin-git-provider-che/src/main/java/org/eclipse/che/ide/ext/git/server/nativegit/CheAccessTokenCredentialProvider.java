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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.git.CredentialsProvider;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.UserCredential;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Credentials provider for Che
 *
 * @author Alexander Garagatyi
 * @author Valeriy Svydenko
 */
@Singleton
public class CheAccessTokenCredentialProvider implements CredentialsProvider {
    private final String        cheHostName;
    private       PreferenceDao preferenceDao;

    @Inject
    public CheAccessTokenCredentialProvider(@Named("api.endpoint") String apiEndPoint,
                                            PreferenceDao preferenceDao) throws URISyntaxException {
        this.preferenceDao = preferenceDao;
        this.cheHostName = new URI(apiEndPoint).getHost();
    }

    @Override
    public UserCredential getUserCredential() throws GitException {
        String token = EnvironmentContext.getCurrent()
                                         .getUser()
                                         .getToken();
        if (token != null) {
            return new UserCredential(token, "x-che", "che_password");
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        User user = EnvironmentContext.getCurrent()
                                      .getUser();
        GitUser gitUser = newDto(GitUser.class);
        if (user.isTemporary()) {
            gitUser.setEmail("anonymous@noemail.com");
            gitUser.setName("Anonymous");
        } else {
            String name = null;
            String email = null;
            try {
                Map<String, String> preferences = preferenceDao.getPreferences(EnvironmentContext.getCurrent()
                                                                                                 .getUser()
                                                                                                 .getId(),
                                                                               "git.committer.\\w+");
                name = preferences.get("git.committer.name");
                email = preferences.get("git.committer.email");
            } catch (ServerException e) {
                //ignored
            }

            gitUser.setName(isNullOrEmpty(name) ? "Anonymous" : name);
            gitUser.setEmail(isNullOrEmpty(email) ? "anonymous@noemail.com" : email);
        }

        return gitUser;
    }

    @Override
    public String getId() {
        return "codenvy";
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return url.contains(cheHostName);
    }

}