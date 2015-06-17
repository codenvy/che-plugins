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
package org.eclipse.che.ide.ext.git.server.jgit.ssh;

import org.eclipse.che.ide.ext.ssh.server.SshKey;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStoreException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * SSH session factory that use SshKeyProvider to get access to private keys. Factory does not support user interactivity (e.g. password
 * authentication).
 * 
 * @author <a href="mailto:aparfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class IdeSshSessionFactory extends JschConfigSessionFactory {//implements Startable {
    private final SshKeyStore keyProvider;

    public IdeSshSessionFactory(SshKeyStore keyProvider) {
        this.keyProvider = keyProvider;
        init();
    }

    /** Initial this SshSessionFactory. By default turn off using "know-hosts" file. */
    protected void init() {
        JSch.setConfig("StrictHostKeyChecking", "no");
    }

    /**
     * @see org.eclipse.jgit.transport.JschConfigSessionFactory#configure(org.eclipse.jgit.transport.OpenSshConfig.Host,
     *      com.jcraft.jsch.Session)
     */
    @Override
    protected void configure(OpenSshConfig.Host hc, Session session) {
    }

    /**
     * @see org.eclipse.jgit.transport.JschConfigSessionFactory#getJSch(org.eclipse.jgit.transport.OpenSshConfig.Host,
     *      org.eclipse.jgit.util.FS)
     */
    @Override
    protected final JSch getJSch(OpenSshConfig.Host hc, FS fs) throws JSchException {
        try {
            String host = hc.getHostName();
            SshKey key = keyProvider.getPrivateKey(host);
            if (key == null) {
                throw new JSchException("SSH connection failed. Key file not found. ");
            }
            JSch jsch = new JSch();
            jsch.addIdentity(key.getIdentifier(), key.getBytes(), null, null);
            return jsch;
        } catch (SshKeyStoreException e) {
            throw new JSchException(e.getMessage(), e);
        }
    }

//    /** @see org.picocontainer.Startable#start() */
//    @Override
//    public void start() {
//        SshSessionFactory.setInstance(this);
//    }
//
//    /** @see org.picocontainer.Startable#stop() */
//    @Override
//    public void stop() {
//        SshSessionFactory.setInstance(null);
//    }
}
