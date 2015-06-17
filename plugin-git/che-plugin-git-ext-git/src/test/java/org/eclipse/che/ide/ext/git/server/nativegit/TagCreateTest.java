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
import org.eclipse.che.ide.ext.git.server.nativegit.commands.TagListCommand;
import org.eclipse.che.ide.ext.git.shared.TagCreateRequest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Eugene Voevodin
 */
public class TagCreateTest extends BaseTest {

    @Test
    public void testCreateTag() throws GitException {
        //given
        NativeGit defaultGit = new NativeGit(getRepository().toFile());
        TagListCommand tagList = defaultGit.createTagListCommand();
        int beforeTagCount = tagList.execute().size();
        TagCreateRequest request = newDTO(TagCreateRequest.class);
        request.setName("v1");
        request.setMessage("first version");
        //when
        getConnection().tagCreate(request);
        //then
        int afterTagCount = tagList.execute().size();
        assertEquals(afterTagCount, beforeTagCount + 1);
    }

    // TODO fails in JGit internally, probably due to a race condition.
	@Test(groups = "nojgit")
    public void testCreateTagForce() throws GitException {
        NativeGit defaultGit = new NativeGit(getRepository().toFile());
        TagCreateRequest request = newDTO(TagCreateRequest.class);
        request.setName("v1");
        request.setMessage("first version");
        getConnection().tagCreate(request);
        try {
            //try add same tag
            getConnection().tagCreate(request);
            fail("It is not force, should be exception.");
        } catch (GitException ignored) {
        }
        //try again with force
        request.setMessage("first version");
        request.setForce(true);
        getConnection().tagCreate(request);
        assertTrue(defaultGit.createTagListCommand().execute().get(0).getName().equals("v1"));
    }
}
