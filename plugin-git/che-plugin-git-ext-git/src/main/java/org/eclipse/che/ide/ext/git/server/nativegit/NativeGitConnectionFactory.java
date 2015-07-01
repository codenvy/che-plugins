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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.GitConnection;
import org.eclipse.che.ide.ext.git.server.GitConnectionFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.shared.GitUser;
import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Map;

/**
 * Native implementation for GitConnectionFactory
 *
 * @author Eugene Voevodin
 */
@Singleton
public class NativeGitConnectionFactory extends GitConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NativeGitConnectionFactory.class);

    private final SshKeysManager    keysManager;
    private final CredentialsLoader credentialsLoader;
    private final UserProfileDao    userProfileDao;

    @Inject
    public NativeGitConnectionFactory(SshKeysManager keysManager, CredentialsLoader credentialsLoader, UserProfileDao userProfileDao) {
        this.keysManager = keysManager;
        this.credentialsLoader = credentialsLoader;
        this.userProfileDao = userProfileDao;
    }

    @Override
    public GitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory) throws GitException {
        final GitConnection gitConnection = new NativeGitConnection(workDir, user, keysManager, credentialsLoader);
        gitConnection.setOutputLineConsumerFactory(outputPublisherFactory);
        return gitConnection;
    }

    @Override
    public GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(workDir, getGitUser(), outputPublisherFactory);
    }

    private GitUser getGitUser() {
        final User user = EnvironmentContext.getCurrent().getUser();
        Map<String, String> profileAttributes = null;
        try {
            profileAttributes = userProfileDao.getById(user.getId()).getAttributes();
        } catch (NotFoundException | ServerException e) {
            LOG.warn("Failed to obtain user information.", e);
        }
        final GitUser gitUser = DtoFactory.getInstance().createDto(GitUser.class);
        if (profileAttributes == null) {
            return gitUser.withName(user.getName()).withEmail("anonymous@noemail.com");
        }
        final String firstName = profileAttributes.get("firstName");
        final String lastName = profileAttributes.get("lastName");
        final String email = profileAttributes.get("email");
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
}
