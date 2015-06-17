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

import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.GitConnection;
import org.eclipse.che.ide.ext.git.server.GitConnectionFactory;
import org.eclipse.che.ide.ext.git.server.GitException;
import org.eclipse.che.ide.ext.git.server.ServerTestConfiguration;
import org.eclipse.che.ide.ext.git.server.nativegit.commands.EmptyGitCommand;
import org.eclipse.che.ide.ext.git.server.nativegit.commands.ListFilesCommand;
import org.eclipse.che.ide.ext.git.shared.AddRequest;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import org.eclipse.che.ide.ext.git.shared.GitUser;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.api.core.util.LineConsumerFactory.NULL;
import static org.eclipse.che.commons.lang.IoUtil.deleteRecursive;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Eugene Voevodin
 */
@Listeners(MockitoTestNGListener.class)
public abstract class BaseTest {
    protected final String CONTENT      = "git repository content\n";
    protected final String DEFAULT_URI  = "user@host.com:login/repo";
    protected final List<File> forClean = new ArrayList<>();
    protected GitConnectionFactory connectionFactory;

    private GitUser              user;
    private GitConnection        connection;
    private Path                 target;
    /**
     * The test parameter 'configClass' tells the tests which implementation if ServerTestConfiguration to
     * instantiate. The configuration instance provides the correct implementation of GitConnectionFactory,
     * basically allowing the flexibility of externally choosing which git connection to run the tests on.
     */
    private ServerTestConfiguration config;

    @Mock
    private CredentialsLoader loader;

    @BeforeMethod
	@Parameters({ ServerTestConfiguration.CLASS_NAME_PARAM })
    public void initRepository(String configClass) throws Exception {
        final File repository = getTarget().resolve("repository").toFile();
        if (!repository.exists()) {
            assertTrue(repository.mkdir());
        }
        init(repository);
        // Intantiate a configuration object
		config = (ServerTestConfiguration) Class.forName(configClass).newInstance();
        //setup connection
        user = newDTO(GitUser.class).withName("test_name").withEmail("test@email");
        connectionFactory = config.createConnectionFactory(loader);
        connection = connectionFactory.getConnection(repository, user, NULL);
        addFile(repository.toPath(), "README.txt", CONTENT);
        connection.add(newDTO(AddRequest.class).withFilepattern(Arrays.asList("README.txt")));
        connection.commit(newDTO(CommitRequest.class).withMessage("Initial commit"));
        forClean.add(connection.getWorkingDir());
        EnvironmentContext.getCurrent().setUser(
                new UserImpl("codenvy", "codenvy", null, Arrays.asList("workspace/developer"), false));
    }

    @AfterMethod
    public void removeRepository() throws IOException {
        for (File file : forClean) {
            deleteRecursive(file);
        }
        forClean.clear();
    }

    protected Path getTarget() throws URISyntaxException {
        if (target == null) {
            final URL targetParent = Thread.currentThread().getContextClassLoader().getResource(".");
            assertNotNull(targetParent);
            target = Paths.get(targetParent.toURI()).getParent();
        }
        return target;
    }

    protected GitUser getUser() {
        return user;
    }

    protected Path getRepository() {
        return getConnection().getWorkingDir().toPath();
    }

    protected void addFile(String name, String content) throws IOException {
        addFile(getRepository(), name, content);
    }

    protected void deleteFile(String name) throws IOException {
        delete(getRepository().resolve(name));
    }

    protected File addFile(Path parent, String name, String content) throws IOException {
        if (!exists(parent)) {
            createDirectories(parent);
        }
        return write(parent.resolve(name), content.getBytes()).toFile();
    }

    protected String readFile(File file) throws IOException {
        if (file.isDirectory())
            throw new IllegalArgumentException("Can't read content from directory " + file.getAbsolutePath());
        FileReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new FileReader(file);
            int ch = -1;
            while ((ch = reader.read()) != -1)
                content.append((char) ch);
        } finally {
            if (reader != null)
                reader.close();
        }
        return content.toString();
    }

    protected GitConnection getConnection() {
        return connection;
    }

    protected <T> T newDTO(Class<T> dtoInterface) {
        return DtoFactory.getInstance().createDto(dtoInterface);
    }

    private void init(File repository) throws GitException {
        final NativeGit git = new NativeGit(repository);
        git.createInitCommand().execute();
    }

    protected int getCountOfCommitsInCurrentBranch(File repo) throws GitException {
        EmptyGitCommand emptyGitCommand = new EmptyGitCommand(repo);
        emptyGitCommand.setNextParameter("rev-list")
                       .setNextParameter("HEAD")
                       .setNextParameter("--count")
                       .execute();
        return Integer.parseInt(emptyGitCommand.getText());
    }

    protected void validateBranchList(List<Branch> toValidate, List<Branch> pattern) {
        l1:
        for (Branch tb : toValidate) {
            for (Branch pb : pattern) {
                if (tb.getName().equals(pb.getName()) //
                        && tb.getDisplayName().equals(pb.getDisplayName()) //
                        && tb.isActive() == pb.isActive())
                    continue l1;
            }
            fail("List of branches is not matches to expected. Branch " + tb + " is not expected in result. ");
        }
    }

    protected void checkNotCached(File repository, String... fileNames) throws GitException {
        ListFilesCommand lf = new ListFilesCommand(repository);
        lf.setCached(true).execute();
        List<String> output = lf.getLines();
        for (String fName : fileNames) {
            if (output.contains(fName)) {
                fail("Cache contains " + fName);
            }
        }
    }

    protected void checkCached(File repository, String... fileNames) throws GitException {
        ListFilesCommand lf = new ListFilesCommand(repository);
        lf.setCached(true).execute();
        List<String> output = lf.getLines();
        for (String fName : fileNames) {
            if (!output.contains(fName)) {
                fail("Cache not contains " + fName);
            }
        }
    }
    
    protected static <T extends Comparable<T>> void assertUnorderedEquals(Collection<T> actual, List<T> expectedSorted) {
        List<T> sortedActual = new ArrayList<T>(actual);
        Collections.sort(sortedActual);
        assertEquals(sortedActual, expectedSorted);
    }
}
