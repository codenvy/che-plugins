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

import org.eclipse.che.ide.ext.git.shared.AddRequest;
import org.eclipse.che.ide.ext.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import org.eclipse.che.ide.ext.git.shared.MergeRequest;
import org.eclipse.che.ide.ext.git.shared.RmRequest;
import org.eclipse.che.ide.ext.git.shared.Status;

import org.testng.annotations.Test;


import static java.util.Arrays.asList;
import static org.eclipse.che.ide.ext.git.shared.StatusFormat.SHORT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class StatusTest extends BaseTest {

    @Test
    public void testEmptyStatus() throws Exception {
        final Status status = getConnection().status(SHORT);

        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testUntracked() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");

        final Status status = getConnection().status(SHORT);

        assertUnorderedEquals(status.getUntracked(), asList("a", "b"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testUntrackedFolder() throws Exception {
        addFile(getRepository().resolve("new_directory"), "a", "content of a");

        final Status status = getConnection().status(SHORT);

        assertEquals(status.getUntrackedFolders(), asList("new_directory"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
    }

    @Test
    public void testAdded() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        addFile("c", "c content");
        //add "a" and "b" files
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));

        final Status status = getConnection().status(SHORT);

        assertUnorderedEquals(status.getAdded(), asList("a", "b"));
        assertEquals(status.getUntracked(), asList("c"));
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testModified() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //modify "a"
        addFile("a", "new content of a");

        final Status status = getConnection().status(SHORT);

        assertEquals(status.getModified(), asList("a"));
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testChanged() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //commit "a" and "b"
        getConnection().commit(newDTO(CommitRequest.class).withMessage("add 2 test files"));
        //modify "a"
        addFile("a", "modified content of a");
        //add "a"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a")));
        //change "a"
        addFile("a", "changed content of a");

        final Status status = getConnection().status(SHORT);

        assertEquals(status.getChanged(), asList("a"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testConflicting() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //commit "a" and "b"
        getConnection().commit(newDTO(CommitRequest.class).withMessage("add 2 test files"));
        //switch to other branch
        getConnection().branchCheckout(newDTO(BranchCheckoutRequest.class).withCreateNew(true)
                                                                          .withName("new_branch"));
        //modify and commit "a"
        addFile("a", "new_branch a content");
        getConnection().commit(newDTO(CommitRequest.class).withAll(true)
                                                          .withMessage("a changed in new_branch"));
        //switch back to master
        getConnection().branchCheckout(newDTO(BranchCheckoutRequest.class).withName("master"));
        //modify and commit "a"
        addFile("a", "master content");
        getConnection().commit(newDTO(CommitRequest.class).withAll(true)
                                                          .withMessage("a changed in master"));
        //merge with "new_branch" to get conflict
        getConnection().merge(newDTO(MergeRequest.class).withCommit("new_branch"));

        final Status status = getConnection().status(SHORT);

        assertEquals(status.getConflicting(), asList("a"));
        assertTrue(status.getModified().isEmpty());
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

	@Test(groups = "nojgit")
    public void testMissing() throws Exception {
        addFile("a", "content of a");
        //add "a"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a")));
        //delete "a"
        deleteFile("a");

        final Status status = getConnection().status(SHORT);

        // TODO native git behaves incorrectly here. In standard git, if a file is added to the index and later
        // deleted from the file system then it should appear in both the 'added' and 'missing' status lists
        assertEquals(status.getMissing(), asList("a"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

	@Test(groups = "nonativegit")
    public void testMissing_JGit() throws Exception {
        addFile("a", "content of a");
        //add "a"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a")));
        //delete "a"
        deleteFile("a");

        final Status status = getConnection().status(SHORT);

        // TODO native git behaves incorrectly here. In standard git, if a file is added to the index and later
        // deleted from the file system then it should appear in both the 'added' and 'missing' status lists
        assertEquals(status.getMissing(), asList("a"));
        assertEquals(status.getAdded(), asList("a"));
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

	@Test(groups = "nojgit")
    public void testRemovedFromFilesSystem() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //commit "a" and "b"
        getConnection().commit(newDTO(CommitRequest.class).withMessage("add 2 test files"));
        //delete "a"
        deleteFile("a");

        final Status status = getConnection().status(SHORT);

        // TODO native git behaves incorrectly here. In standard git, if a committed file is deleted from the
        // file system then it should appear the 'missing' status list only
        assertEquals(status.getRemoved(), asList("a"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

	@Test(groups = "nonativegit")
    public void testRemovedFromFilesSystem_JGit() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //commit "a" and "b"
        getConnection().commit(newDTO(CommitRequest.class).withMessage("add 2 test files"));
        //delete "a"
        deleteFile("a");

        final Status status = getConnection().status(SHORT);

        assertTrue(status.getRemoved().isEmpty());
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertEquals(status.getMissing(), asList("a"));
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }

    @Test
    public void testRemovedFromIndex() throws Exception {
        addFile("a", "a content");
        addFile("b", "b content");
        //add "a" and "b"
        getConnection().add(newDTO(AddRequest.class).withFilepattern(asList("a", "b")));
        //commit "a" and "b"
        getConnection().commit(newDTO(CommitRequest.class).withMessage("add 2 test files"));
        //remove "a" from index
        getConnection().rm(newDTO(RmRequest.class).withItems(asList("a")));

        final Status status = getConnection().status(SHORT);

        assertEquals(status.getRemoved(), asList("a"));
        assertTrue(status.getAdded().isEmpty());
        assertTrue(status.getChanged().isEmpty());
        assertTrue(status.getConflicting().isEmpty());
        assertTrue(status.getMissing().isEmpty());
        assertTrue(status.getUntracked().isEmpty());
        assertTrue(status.getUntrackedFolders().isEmpty());
    }
}
