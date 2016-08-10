/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit.ssh;

import com.google.inject.Inject;

import org.eclipse.che.api.git.exception.GitException;;
import org.eclipse.che.api.git.GitUrlUtils;
import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;

/**
 * Implementation SshKeyProvider that provides private key
 *
 * @author Igor Vinokur
 */
public class SshKeyProviderImpl implements SshKeyProvider {

    private final SshKeyStore         sshKeyStore;

    @Inject
    public SshKeyProviderImpl(SshKeyStore sshKeyStore) {
        this.sshKeyStore = sshKeyStore;
    }

    /**
     * Get private ssh key.
     *
     * @param url
     *         url to git repository
     * @return private ssh key
     * @throws GitException
     *         if an error occurs while generating keys
     */
    @Override
    public byte[] getPrivateKey(String url) throws GitException {
        String host = GitUrlUtils.getHost(url);
        SshKey privateKey;

        // check keys existence
        try {
            if ((privateKey = sshKeyStore.getPrivateKey(host)) == null) {
                throw new GitException("Unable get private ssh key");
            }
        } catch (SshKeyStoreException e) {
            throw new GitException(e.getMessage(), e);
        }
        return privateKey.getBytes();
    }
}
