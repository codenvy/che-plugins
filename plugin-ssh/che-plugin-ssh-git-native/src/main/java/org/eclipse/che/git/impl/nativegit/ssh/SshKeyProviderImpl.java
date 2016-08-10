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
package org.eclipse.che.git.impl.nativegit.ssh;

import com.google.inject.Inject;

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.exception.GitException;;
import org.eclipse.che.api.git.GitUrlUtils;
import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;
import org.eclipse.che.ide.ext.ssh.server.SshKeyUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
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
        String host = GitUrlUtils.getHost(url);
        SshKey publicKey;
        SshKey privateKey;

        // check keys existence
        try {
            if ((privateKey = sshKeyStore.getPrivateKey(host)) != null) {
                publicKey = sshKeyStore.getPublicKey(host);
            } else {
                throw new GitException("Unable get private ssh key");
            }
        } catch (SshKeyStoreException e) {
            throw new GitException(e.getMessage(), e);
        }

        final Optional<SshKeyUploader> optionalKeyUploader = sshKeyUploaders.stream()
                                                                            .filter(keyUploader -> keyUploader.match(url))
                                                                            .findFirst();
        if (optionalKeyUploader.isPresent()) {
            final SshKeyUploader uploader = optionalKeyUploader.get();
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
