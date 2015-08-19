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
package org.eclipse.che.ide.ext.ssh.client;

import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.ssh.dto.GenKeyRequest;
import org.eclipse.che.ide.ext.ssh.dto.KeyItem;
import org.eclipse.che.ide.ext.ssh.dto.PublicKey;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of {@link SshKeyService}.
 *
 * @author Evgen Vidolob
 */
@Singleton
public class SshKeyServiceImpl implements SshKeyService {
    private final String                      baseUrl;
    private final String                      workspaceId;
    private final AsyncRequestLoader          loader;
    private final DtoFactory                  dtoFactory;
    private final AsyncRequestFactory         asyncRequestFactory;
    private final Map<String, SshKeyProvider> sshKeyProviders;

    @Inject
    protected SshKeyServiceImpl(@RestContext String baseUrl,
                                @Named("workspaceId") String workspaceId,
                                AsyncRequestLoader loader,
                                DtoFactory dtoFactory,
                                AsyncRequestFactory asyncRequestFactory) {
        this.baseUrl = baseUrl;
        this.workspaceId = workspaceId;
        this.loader = loader;
        this.dtoFactory = dtoFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.sshKeyProviders = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void getAllKeys(@Nonnull AsyncRequestCallback<List<KeyItem>> callback) {
        loader.show("Getting SSH keys....");
        asyncRequestFactory.createGetRequest(baseUrl + "/ssh-keys/" + workspaceId + "/all").send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void generateKey(@Nonnull String host, @Nonnull AsyncRequestCallback<Void> callback) {
        String url = baseUrl + "/ssh-keys/" + workspaceId + "/gen";

        GenKeyRequest keyRequest = dtoFactory.createDto(GenKeyRequest.class).withHost(host);

        asyncRequestFactory.createPostRequest(url, keyRequest)
                           .loader(loader, "Generate keys for " + host)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getPublicKey(@Nonnull KeyItem keyItem, @Nonnull AsyncRequestCallback<PublicKey> callback) {
        loader.show("Getting public SSH key for " + keyItem.getHost());
        asyncRequestFactory.createGetRequest(keyItem.getPublicKeyUrl()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteKey(@Nonnull KeyItem keyItem, @Nonnull AsyncRequestCallback<Void> callback) {
        loader.show("Deleting SSH keys for " + keyItem.getHost());
        asyncRequestFactory.createGetRequest(keyItem.getRemoteKeyUrl()).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SshKeyProvider> getSshKeyProviders() {
        return sshKeyProviders;
    }

    /** {@inheritDoc} */
    @Override
    public void registerSshKeyProvider(@Nonnull String host, @Nonnull SshKeyProvider sshKeyProvider) {
        sshKeyProviders.put(host, sshKeyProvider);
    }
}