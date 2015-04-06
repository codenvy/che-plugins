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

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.server.nativegit.commands.TagListCommand;
import org.eclipse.che.ide.ext.git.shared.GitUser;
import org.eclipse.che.ide.ext.git.shared.Tag;
import org.eclipse.che.ide.ext.git.shared.TagDeleteRequest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;


/**
 * @author Eugene Voevodin
 */
public class TagDeleteTest extends BaseTest {

    @Test
    public void testDeleteTag() throws GitException {
        //given
        //create tags
        GitUser committer = DtoFactory.getInstance().createDto(GitUser.class).withName("n").withEmail("e@gs.com");
        NativeGit defaultGit = new NativeGit(getRepository().toFile());
        defaultGit.createTagCreateCommand().setName("first-tag").setCommitter(committer).execute();
        defaultGit.createTagCreateCommand().setName("second-tag").setCommitter(committer).execute();

        TagListCommand tagListCommand = new TagListCommand(getRepository().toFile());
        assertTrue(tagExists(tagListCommand.execute(), "first-tag"));
        assertTrue(tagExists(tagListCommand.execute(), "second-tag"));
        //when
        //delete first-tag
        TagDeleteRequest request = newDTO(TagDeleteRequest.class);
        request.setName("first-tag");
        getConnection().tagDelete(request);
        //then
        //check not exists more
        assertFalse(tagExists(tagListCommand.execute(), "first-tag"));
        assertTrue(tagExists(tagListCommand.execute(), "second-tag"));
    }

    private boolean tagExists(List<Tag> list, String name) {
        for (Tag tag : list) {
            if (tag.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
