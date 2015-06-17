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
package org.eclipse.che.ide.ext.git.server.jgit;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.ide.ext.git.server.GitConnection;
import org.eclipse.che.ide.ext.git.server.GitConnectionFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.shared.GitUser;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * JGit implementation for GitConnectionFactory
 * 
 * @author Tareq Sharafy (tareq.sha@gmail.com)
 */
public class JGitConnectionFactory extends GitConnectionFactory {

    private final UserProfileDao userProfileDao;

    @Inject
    public JGitConnectionFactory(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(workDir, getGitUserFromUserProfile(userProfileDao), outputPublisherFactory);
    }

    @Override
    public GitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory)
            throws GitException {
        Repository gitRepo = createRepository(workDir);
        JGitConnection conn = new JGitConnection(gitRepo, user);
        conn.setOutputLineConsumerFactory(outputPublisherFactory);
        return conn;
    }

    private static Repository createRepository(File workDir) throws GitException {
        try {
            return new FileRepository(new File(workDir, Constants.DOT_GIT));
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }
}
