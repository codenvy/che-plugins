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

import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.server.nativegit.commands.GetConfigCommand;
import org.eclipse.che.ide.ext.git.shared.RemoteUpdateRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.testng.Assert.*;

/**
 * @author Eugene Voevodin
 */
public class RemoteUpdateTest extends BaseTest {

    @BeforeMethod
    protected void setUp() throws Exception {
        NativeGit nativeGit = new NativeGit(getRepository().toFile());
        nativeGit.createRemoteAddCommand()
                .setName("newRemote")
                .setUrl("newRemote.url")
                .setBranches(Arrays.asList("branch1"))
                .execute();
    }

    @Test
    public void testUpdateBranches() throws GitException {
        //change branch1 to branch2
        RemoteUpdateRequest request = newDTO(RemoteUpdateRequest.class);
        request.setName("newRemote");
        request.setBranches(Arrays.asList("branch2"));
        getConnection().remoteUpdate(request);
        assertEquals(parseAllConfig(getRepository().toFile()).get("remote.newRemote.fetch").get(0),
                "+refs/heads/branch2:refs/remotes/newRemote/branch2");
    }

//    @Test
//    public void testAddUrl() throws GitException {
//        RemoteUpdateRequest request = newDTO(RemoteUpdateRequest.class);
//        request.setName("newRemote");
//        request.setAddUrl(Arrays.asList("new.com"));
//        getConnection().remoteUpdate(request);
//        assertTrue(parseAllConfig(getRepository().toFile()).get("remote.newRemote.url").contains("new.com"));
//    }
//
//    @Test
//    public void testAddPushUrl() throws GitException {
//        RemoteUpdateRequest request = newDTO(RemoteUpdateRequest.class);
//        request.setName("newRemote");
//        request.setAddPushUrl(Arrays.asList("pushurl1"));
//        getConnection().remoteUpdate(request);
//        assertTrue(parseAllConfig(getRepository().toFile()).get("remote.newRemote.pushurl").contains("pushurl1"));
//    }
//
//    @Test
//    public void testDeleteUrl() throws GitException {
//        //add url
//        new NativeGit(getRepository().toFile()).createRemoteUpdateCommand()
//                .setRemoteName("newRemote")
//                .setAddUrl(Arrays.asList("newurl"))
//                .execute();
//        RemoteUpdateRequest request = newDTO(RemoteUpdateRequest.class);
//        request.setName("newRemote");
//        request.setRemoveUrl(Arrays.asList("newurl"));
//        getConnection().remoteUpdate(request);
//        assertFalse(parseAllConfig(getRepository().toFile()).get("remote.newRemote.url").contains("newurl"));
//    }

    @Test
    public void testDeletePushUrl() throws GitException {
        //add push url
        new NativeGit(getRepository().toFile()).createRemoteUpdateCommand()
                .setRemoteName("newRemote")
                .setAddUrl(Arrays.asList("pushurl"))
                .execute();
        RemoteUpdateRequest request = newDTO(RemoteUpdateRequest.class);
        request.setName("newRemote");
        request.setRemovePushUrl(Arrays.asList("pushurl"));
        getConnection().remoteUpdate(request);
        assertNull(parseAllConfig(getRepository().toFile()).get("remote.newRemote.pushurl"));
    }

    private Map<String, List<String>> parseAllConfig(File repo) throws GitException {
        Map<String, List<String>> config = new HashMap<>();
        GetConfigCommand getConf = new GetConfigCommand(repo).setGetList(true);
        getConf.execute();
        for (String outLine : getConf.getLines()) {
            String[] pair = outLine.split("=");
            List<String> list = config.get(pair[0]);
            if (list == null) {
                list = new LinkedList<>();
            }
            if (pair.length == 2) {
                list.add(pair[1]);
            }
            config.put(pair[0], list);
        }
        return config;
    }
}
