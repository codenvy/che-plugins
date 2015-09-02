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
package org.eclipse.che.git.impl.nativegit.ssh;

import com.google.inject.Inject;

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.git.impl.nativegit.GitUrl;
import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyPair;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;
import org.eclipse.che.ide.ext.ssh.server.SshKeyUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation SshKeyProvider that provides private key and upload public
 *
 * @author Anton Korneta
 */
public class SshKeyProviderImpl implements SshKeyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SshKeyProviderImpl.class);
    private final SshKeyStore         sshKeyStore;
    private final Set<SshKeyUploader> sshKeyUploaders;

    @Inject
    public SshKeyProviderImpl(SshKeyStore sshKeyStore, Set<SshKeyUploader> sshKeyUploaders) {
        this.sshKeyStore = sshKeyStore;
        this.sshKeyUploaders = sshKeyUploaders;
    }

    /**
     * Get private ssh key and upload public ssh key to repository hosting service.
     *
     * @param url
     *         url to git repository
     * @return private ssh key
     * @throws GitException
     *         if an error occurs while generating or uploading keys
     */
    @Override
    public byte[] getPrivateKey(String url) throws GitException {
        String host = GitUrl.getHost(url);
        SshKey publicKey;
        SshKey privateKey;

        // check keys existence and generate if need
        try {
            if ((privateKey = sshKeyStore.getPrivateKey(host)) != null) {
                publicKey = sshKeyStore.getPublicKey(host);
                if (publicKey == null) {
                    sshKeyStore.removeKeys(host);
                    SshKeyPair sshKeyPair = sshKeyStore.genKeyPair(host, null, null);
                    publicKey = sshKeyPair.getPublicKey();
                    privateKey = sshKeyPair.getPrivateKey();
                }
            } else {
                SshKeyPair sshKeyPair = sshKeyStore.genKeyPair(host, null, null);
                publicKey = sshKeyPair.getPublicKey();
                privateKey = sshKeyPair.getPrivateKey();
            }
        } catch (SshKeyStoreException e) {
            throw new GitException(e.getMessage(), e);
        }

        SshKeyUploader uploader = null;

        for (Iterator<SshKeyUploader> itr = sshKeyUploaders.iterator(); uploader == null && itr.hasNext(); ) {
            SshKeyUploader next = itr.next();
            if (next.match(url)) {
                uploader = next;
            }
        }

        if (uploader != null) {
            // upload public key
            try {
                uploader.uploadKey(publicKey);
            } catch (IOException e) {
                throw new GitException(e.getMessage(), e);
            } catch (UnauthorizedException e) {
                // Git action might fail without uploaded public SSH key.
                LOG.warn(String.format("Unable upload public SSH key with %s", uploader.getClass().getSimpleName()), e);
            }
        } else {
            // Git action might fail without SSH key.
            LOG.warn(String.format("Not found ssh key uploader for %s", host));
        }
        return privateKey.getBytes();
    }
}