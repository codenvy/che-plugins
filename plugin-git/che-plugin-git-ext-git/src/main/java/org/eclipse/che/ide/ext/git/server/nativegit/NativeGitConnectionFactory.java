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

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.ide.ext.git.server.GitConnection;
import org.eclipse.che.ide.ext.git.server.GitConnectionFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.shared.GitUser;

/**
 * Native implementation for GitConnectionFactory
 *
 * @author Eugene Voevodin
 */
@Singleton
public class NativeGitConnectionFactory extends GitConnectionFactory {

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
        return getConnection(workDir, getGitUserFromUserProfile(userProfileDao), outputPublisherFactory);
    }
}
