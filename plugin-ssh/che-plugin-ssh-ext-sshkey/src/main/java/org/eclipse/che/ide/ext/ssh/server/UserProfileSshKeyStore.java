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
package org.eclipse.che.ide.ext.ssh.server;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** @author andrew00x */
public class UserProfileSshKeyStore implements SshKeyStore {
    private static final int    PRIVATE                      = 0;
    private static final int    PUBLIC                       = 1;
    private static final String KEY_ATTRIBUTE_PREFIX         = "ssh.key.";
    /** Prefix for attribute of user profile that store private SSH key. */
    private static final String PRIVATE_KEY_ATTRIBUTE_PREFIX = KEY_ATTRIBUTE_PREFIX + "private.";
    /** Prefix for attribute of user profile that store public SSH key. */
    private static final String PUBLIC_KEY_ATTRIBUTE_PREFIX  = KEY_ATTRIBUTE_PREFIX + "public.";

    private final JSch          genJsch;
    private final UserDao       userDao;
    private final PreferenceDao preferenceDao;

    @Inject
    public UserProfileSshKeyStore(PreferenceDao preferenceDao, UserDao userDao) {
        this.genJsch = new JSch();
        this.userDao = userDao;
        this.preferenceDao = preferenceDao;
    }

    @Override
    public void addPrivateKey(String host, byte[] key) throws SshKeyStoreException {
        User user = getUser(getUserId());
        try {
            final String sshKeyAttributeName = sshKeyAttributeName(host, PRIVATE);
            final Map<String, String> preferences = preferenceDao.getPreferences(user.getId());
            if (preferences.get(sshKeyAttributeName) != null) {
                throw new SshKeyStoreException(String.format("Private key for host: '%s' already exists. ", host));
            }
            preferences.put(sshKeyAttributeName, new String(key));
            preferenceDao.setPreferences(user.getId(), preferences);
        } catch (NotFoundException | ServerException e) {
            throw new SshKeyStoreException(String.format("Failed to add private key for host '%s'.", host));
        }
    }

    @Override
    public SshKey getPrivateKey(String host) throws SshKeyStoreException {
        return getKey(host, PRIVATE);
    }

    @Override
    public SshKey getPublicKey(String host) throws SshKeyStoreException {
        return getKey(host, PUBLIC);
    }

    private SshKey getKey(String host, int i) throws SshKeyStoreException {
        User user = getUser(getUserId());
        Map<String, String> preferences;
        try {
            preferences = preferenceDao.getPreferences(user.getId());
        } catch (ServerException e) {
            throw new SshKeyStoreException(String.format("Failed to get key for host '%s'.", host));
        }

        String keyIdentifier = sshKeyAttributeName(host, i);
        String keyAsString = preferences.get(keyIdentifier);
        if (keyAsString == null) {
            // Try to find key for parent domain. This is required for openshift integration but may be useful for others also.
            final String attributePrefix = i == PRIVATE ? PRIVATE_KEY_ATTRIBUTE_PREFIX : PUBLIC_KEY_ATTRIBUTE_PREFIX;
            for (Iterator<Map.Entry<String, String>> iterator = preferences.entrySet().iterator(); iterator.hasNext()
                                                                                                   && keyAsString == null; ) {
                Map.Entry<String, String> entry = iterator.next();
                String attributeName = entry.getKey();
                if (attributeName.startsWith(attributePrefix)) {
                    // Lets say we found attribute 'ssh.key.private.codenvy.com'
                    // and we are looking for key for host 'my-site.codenvy.com'.
                    // 1. Get domain name - remove prefix 'ssh.key.private.'
                    // 2. We found the key if host name ends with name we got above.
                    if (host.endsWith(attributeName.substring(attributePrefix.length()))) {
                        keyAsString = entry.getValue();
                    }
                }
            }
        }
        return (keyAsString != null) ? new SshKey(keyIdentifier, keyAsString.getBytes()) : null;
    }

    @Override
    public SshKeyPair genKeyPair(String host, String comment, String passPhrase) throws SshKeyStoreException {
        return genKeyPair(host, comment, passPhrase, null);
    }

    @Override
    public SshKeyPair genKeyPair(String host, String comment, String passPhrase, String keyMail) throws SshKeyStoreException {
        User user = getUser(EnvironmentContext.getCurrent().getUser().getName());
        try {
            Map<String, String> preferences = preferenceDao.getPreferences(user.getId());
            if (keyMail == null) {
                keyMail = user.getEmail() != null ? user.getEmail() : user.getId();
            }
            final String sshPrivateKeyAttributeName = sshKeyAttributeName(host, PRIVATE);
            final String sshPublicKeyAttributeName = sshKeyAttributeName(host, PUBLIC);
            // Be sure keys are not created yet.
            if (preferences.get(sshPrivateKeyAttributeName) != null) {
                throw new SshKeyStoreException(String.format("Private key for host: '%s' already exists. ", host));
            }
            if (preferences.get(sshPublicKeyAttributeName) != null) {
                throw new SshKeyStoreException(String.format("Public key for host: '%s' already exists. ", host));
            }
            // Gen key pair.
            KeyPair keyPair;
            try {
                keyPair = KeyPair.genKeyPair(genJsch, 2, 2048);
            } catch (JSchException e) {
                throw new SshKeyStoreException(e.getMessage(), e);
            }
            keyPair.setPassphrase(passPhrase);

            SshKey privateKey;
            SshKey publicKey;
            ByteArrayOutputStream buff = new ByteArrayOutputStream();

            keyPair.writePrivateKey(buff);
            privateKey = new SshKey(sshPrivateKeyAttributeName, buff.toByteArray());
            preferences.put(sshPrivateKeyAttributeName, new String(buff.toByteArray()));

            buff.reset();

            keyPair.writePublicKey(buff,
                                   comment != null ? comment : (keyMail.indexOf('@') > 0 ? keyMail : (keyMail + "@ide.codenvy.local")));
            publicKey = new SshKey(sshPublicKeyAttributeName, buff.toByteArray());
            preferences.put(sshPublicKeyAttributeName, new String(buff.toByteArray()));

            preferenceDao.setPreferences(user.getId(), preferences);

            return new SshKeyPair(publicKey, privateKey);
        } catch (NotFoundException | ServerException e) {
            throw new SshKeyStoreException(String.format("Failed to generate keys for host '%s'.", host));
        }
    }

    @Override
    public void removeKeys(String host) throws SshKeyStoreException {
        User user = getUser(getUserId());
        try {
            Map<String, String> preferences = preferenceDao.getPreferences(user.getId());
            preferences.remove(sshKeyAttributeName(host, PRIVATE));
            preferences.remove(sshKeyAttributeName(host, PUBLIC));
            preferenceDao.setPreferences(user.getId(), preferences);
        } catch (NotFoundException | ServerException e) {
            throw new SshKeyStoreException(String.format("Failed to remove keys for host '%s'.", host));
        }
    }

    @Override
    public Set<String> getAll() throws SshKeyStoreException {
        User user = getUser(getUserId());
        Map<String, String> preferences;
        try {
            preferences = preferenceDao.getPreferences(user.getId());
        } catch (ServerException e) {
            throw new SshKeyStoreException("Failed to get all keys.");
        }


        final Set<String> keys = new HashSet<String>();
        // Check only for private keys.
        for (String str : preferences.keySet()) {
            if (str.startsWith(PRIVATE_KEY_ATTRIBUTE_PREFIX)) {
                keys.add(str.substring(PRIVATE_KEY_ATTRIBUTE_PREFIX.length()));
            }
        }
        return keys;

    }


    /**
     * Name of attribute of user profile to store SSH key.
     *
     * @param host
     *         host name
     * @param i
     *         <code>0</code> if key is private and <code>1</code> if key is public
     * @return user's profile attribute name
     */
    private String sshKeyAttributeName(String host, int i) {
        // Returns something like: ssh.key.private.codenvy.com or ssh.key.public.codenvy.com
        return (i == PRIVATE ? PRIVATE_KEY_ATTRIBUTE_PREFIX : PUBLIC_KEY_ATTRIBUTE_PREFIX) + host;
    }

    private User getUser(String userId) throws SshKeyStoreException {
        User user;
        try {
            user = userDao.getById(userId);
        } catch (NotFoundException | ServerException e) {
            throw new SshKeyStoreException(String.format("Failed to get user. %s", e.getMessage()));
        }
        if (user == null) {
            throw new SshKeyStoreException("User not found.");
        }
        return user;
    }

    private String getUserId() {
        return EnvironmentContext.getCurrent().getUser().getName();
    }
}
