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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.server.nativegit.commands.ListFilesCommand;
import org.eclipse.che.ide.ext.git.shared.AddRequest;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import com.google.common.collect.ImmutableList;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Eugene Voevodin
 */
public class AddTest extends BaseTest {

    @Test
    public void testSimpleAdd() throws GitException, IOException {
        addFile(getRepository(), "testAdd", CONTENT);
        getConnection().add(newDTO(AddRequest.class).withFilepattern(AddRequest.DEFAULT_PATTERN));
        //check added files
        ListFilesCommand command = new ListFilesCommand(getRepository().toFile());
        command.execute();
        assertTrue(command.getLines().contains("testAdd"));
    }

    @Test(expectedExceptions = GitException.class, groups = "nojgit")
    public void testNoAddWithWrongFilePattern() throws GitException {
        getConnection().add(newDTO(AddRequest.class).withFilepattern(ImmutableList.of("otherFile")).withUpdate(false));
    }

    @Test
    public void testAddUpdate() throws GitException, IOException {
        addFile(getRepository(), "README.txt", CONTENT);
        getConnection().add(newDTO(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        getConnection().commit(newDTO(CommitRequest.class).withMessage("Initial add"));

        //modify README.txt
        addFile(getRepository(), "README.txt", "SOME NEW CONTENT");
        ListFilesCommand command = new ListFilesCommand(getRepository().toFile()).setModified(true);
        command.execute();
        //modified but not added to stage
        assertTrue(command.getText().contains("README.txt"));
        getConnection().add(newDTO(AddRequest.class).withFilepattern(AddRequest.DEFAULT_PATTERN).withUpdate(true));
        command.execute();
        //added to stage
        assertFalse(command.getText().contains("README.txt"));
    }
}
