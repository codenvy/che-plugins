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
package org.eclipse.che.ide.ext.git.server;

import java.io.File;
import java.util.Map;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.shared.GitUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/** @author andrew00x */
public abstract class GitConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GitConnectionFactory.class);
    
    /**
     * Get connection to Git repository located in <code>workDir</code>.
     *
     * @param workDir
     *         repository directory
     * @param user
     *         user
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public final GitConnection getConnection(String workDir, GitUser user) throws GitException {
        return getConnection(new File(workDir), user, LineConsumerFactory.NULL);
    }

    /**
     * Get connection to Git repository located in <code>workDir</code>
     *
     * @param workDir
     *         repository directory
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public final GitConnection getConnection(String workDir) throws GitException {
        return getConnection(new File(workDir));
    }

    /**
     * Get connection to Git repository located in <code>workDir</code>.
     *
     * @param workDir
     *         repository directory
     * @param user
     *         user
     * @param outputPublisherFactory
     *         a consumer factory for git output
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public final GitConnection getConnection(String workDir, GitUser user, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(new File(workDir), user, outputPublisherFactory);
    }

    /**
     * Get connection to Git repository located in <code>workDir</code>
     *
     * @param workDir
     *         repository directory
     * @param outputPublisherFactory
     *         a factory consumer for git output
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public final GitConnection getConnection(String workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(new File(workDir), outputPublisherFactory);
    }

    /**
     * Get connection to Git repository located in <code>workDir</code>
     *
     * @param workDir
     *         repository directory
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public final GitConnection getConnection(File workDir) throws GitException {
        return getConnection(workDir, LineConsumerFactory.NULL);
    }

    /**
     * Get connection to Git repository located in <code>workDir</code>.
     *
     * @param workDir
     *         repository directory
     * @param user
     *         user
     * @param outputPublisherFactory
     *         to create a consumer for git output
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public abstract GitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory) throws GitException;

    /**
     * Get connection to Git repository locate in <code>workDir</code>
     *
     * @param workDir
     *         repository directory
     * @param outputPublisherFactory
     *         to create a consumer for git output
     * @return connection to Git repository
     * @throws GitException
     *         if can't initialize connection
     */
    public abstract GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException;

    /**
     * Get a Git user by inferring his details from the given profile DAO. 
     * @param userProfileDao
     * @return
     */
    protected static GitUser getGitUserFromUserProfile(UserProfileDao userProfileDao) {
        final User user = EnvironmentContext.getCurrent().getUser();
        Map<String, String> profileAttributes = null;
        try {
            profileAttributes = userProfileDao.getById(user.getId()).getAttributes();
        } catch (NotFoundException | ServerException e) {
            LOG.warn("Failed to obtain user information.", e);
        }
        final GitUser gitUser = DtoFactory.getInstance().createDto(GitUser.class);
        if (profileAttributes == null) {
            return gitUser.withName(user.getName());
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