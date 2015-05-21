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
package org.eclipse.che.ide.ext.git.server;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.project.server.FileEntry;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.git.server.nativegit.CredentialsProvider;
import org.eclipse.che.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import org.eclipse.che.ide.ext.git.server.nativegit.SshKeyUploader;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.BranchCheckoutRequest;
import org.eclipse.che.ide.ext.git.shared.BranchCreateRequest;
import org.eclipse.che.ide.ext.git.shared.BranchListRequest;
import org.eclipse.che.ide.ext.git.shared.CommitRequest;
import org.eclipse.che.ide.ext.git.shared.InitRequest;
import org.eclipse.che.ide.ext.git.shared.LogRequest;
import org.eclipse.che.ide.ext.git.shared.Revision;
import org.eclipse.che.ide.ext.ssh.server.SshKeyStore;
import org.eclipse.che.vfs.impl.fs.LocalFileSystemProvider;
import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;
import org.eclipse.che.vfs.impl.fs.WorkspaceHashLocalFSMountStrategy;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
public class GitProjectImporterTest {
    @Mock
    private UserProfileDao userProfileDao;
    @Mock
    private SshKeyStore    sshKeyStore;

    private GitConnectionFactory gitFactory;
    private File                 fsRoot;
    private File                 gitRepo;
    private VirtualFileSystem    vfs;
    private GitProjectImporter   gitProjectImporter;

    private DtoFactory dtoFactory = DtoFactory.getInstance();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Bind components
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder<ProjectImporter> projectImporterMultibinder = Multibinder.newSetBinder(binder(), ProjectImporter.class);
                projectImporterMultibinder.addBinding().to(GitProjectImporter.class);
                bind(GitConnectionFactory.class).to(NativeGitConnectionFactory.class);
                bind(UserProfileDao.class).toInstance(userProfileDao);
                bind(SshKeyStore.class).toInstance(sshKeyStore);
                Multibinder.newSetBinder(binder(), SshKeyUploader.class);
                Multibinder.newSetBinder(binder(), CredentialsProvider.class);
            }
        });

        // Init virtual file system
        File target = new File(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParentFile();
        fsRoot = new File(target, "fs-root");
        Assert.assertTrue(fsRoot.mkdirs());
        VirtualFileSystemRegistry registry = new VirtualFileSystemRegistry();
        WorkspaceHashLocalFSMountStrategy mountStrategy = new WorkspaceHashLocalFSMountStrategy(fsRoot, fsRoot);
        LocalFileSystemProvider vfsProvider = new LocalFileSystemProvider("my_vfs", mountStrategy, new EventService(), null, registry);
        registry.registerProvider("my_vfs", vfsProvider);
        vfs = registry.getProvider("my_vfs").newInstance(URI.create(""));

        // set current user
        EnvironmentContext.getCurrent().setUser(new UserImpl("codenvy", "codenvy", null, Arrays.asList("workspace/developer"), false));

        // rules for mock
        Map<String, String> profileAttributes = new HashMap<>();
        profileAttributes.put("firstName", "Codenvy");
        profileAttributes.put("lastName", "Codenvy");
        profileAttributes.put("email", "codenvy@codenvy.com");
        Mockito.when(userProfileDao.getById("codenvy"))
               .thenReturn(new Profile().withId("codenvy").withUserId("codenvy").withAttributes(profileAttributes));

        // init source git repository
        gitRepo = new File(target, "git");
        Assert.assertTrue(gitRepo.mkdirs());
        Assert.assertTrue(new File(gitRepo, "src").mkdirs());
        try (BufferedWriter w = Files.newBufferedWriter(new File(gitRepo, "src/hello.c").toPath(), Charset.forName("UTF-8"))) {
            w.write("#include <stdio.h>\n\n");
            w.write("int main()\n");
            w.write("{\n");
            w.write("    printf(\"Hello world!\\n\");\n");
            w.write("    return 0;\n");
            w.write("}\n");
        }
        try (BufferedWriter w = Files.newBufferedWriter(new File(gitRepo, "README").toPath(), Charset.forName("UTF-8"))) {
            w.write("test git importer");
        }
        gitFactory = injector.getInstance(GitConnectionFactory.class);
        // init git repository
        GitConnection git = gitFactory.getConnection(gitRepo);
        git.init(dtoFactory.createDto(InitRequest.class).withInitCommit(true));
        git.close();

        gitProjectImporter = injector.getInstance(GitProjectImporter.class);
    }

    static class SystemOutLineConsumer implements LineConsumer {
        @Override
        public void writeLine(String line) throws IOException {
            System.out.println(line);
        }

        @Override
        public void close() throws IOException {
        }
    }

    static class SystemOutLineConsumerFactory implements LineConsumerFactory {
        @Override
        public LineConsumer newLineConsumer() {
            return new SystemOutLineConsumer();
        }
    }

    @After
    public void tearDown() throws Exception {
        Assert.assertTrue(IoUtil.deleteRecursive(fsRoot));
        Assert.assertTrue(IoUtil.deleteRecursive(gitRepo));
    }

    @Test
    public void testImport() throws Exception {
        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        gitProjectImporter
                .importSources(folder, gitRepo.getAbsolutePath(), Collections.<String, String>emptyMap(), new SystemOutLineConsumerFactory());
        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNotNull(folder.getChild(".git"));
        FileEntry readme = (FileEntry)folder.getChild("README");
        Assert.assertEquals("test git importer", new String(readme.contentAsBytes()));
    }

    @Test
    public void testImportCheckoutBranch() throws Exception {
        GitConnection git = gitFactory.getConnection(gitRepo);
        git.branchCreate(dtoFactory.createDto(BranchCreateRequest.class).withName("new_branch"));
        git.branchCheckout(dtoFactory.createDto(BranchCheckoutRequest.class).withName("new_branch"));
        try (BufferedWriter w = Files.newBufferedWriter(new File(gitRepo, "README").toPath(), Charset.forName("UTF-8"))) {
            w.write("test import branch");
        }
        git.commit(dtoFactory.createDto(CommitRequest.class).withAll(true).withMessage("branch"));
        git.close();

        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("branch", "new_branch");
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        GitConnection targetGit = gitFactory.getConnection(((VirtualFileImpl)folder.getVirtualFile()).getIoFile());
        List<Branch> branches = targetGit.branchList(dtoFactory.createDto(BranchListRequest.class));
        targetGit.close();
        Assert.assertEquals(1, branches.size());
        Assert.assertEquals("new_branch", branches.get(0).getDisplayName());

        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNotNull(folder.getChild(".git"));
        FileEntry readme = (FileEntry)folder.getChild("README");
        Assert.assertEquals("test import branch", new String(readme.contentAsBytes()));
    }

    @Test
    public void testImportCheckoutCommit() throws Exception {
        GitConnection git = gitFactory.getConnection(gitRepo);
        try (BufferedWriter w = Files.newBufferedWriter(new File(gitRepo, "README").toPath(), Charset.forName("UTF-8"))) {
            w.write("test import commit");
        }
        git.commit(dtoFactory.createDto(CommitRequest.class).withAll(true).withMessage("commit"));
        LogPage log = git.log(dtoFactory.createDto(LogRequest.class));
        List<Revision> commits = log.getCommits();
        git.close();
        Assert.assertEquals(2, commits.size()); // have two commits now
        Revision commit = commits.get(1); // get first commit

        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("commitId", commit.getId());
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        GitConnection targetGit = gitFactory.getConnection(((VirtualFileImpl)folder.getVirtualFile()).getIoFile());
        final LogPage targetLog = targetGit.log(dtoFactory.createDto(LogRequest.class));
        List<Revision> targetCommits = targetLog.getCommits();
        targetGit.close();
        Assert.assertEquals(1, targetCommits.size()); // skip last commit from the source repository

        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNotNull(folder.getChild(".git"));
        FileEntry readme = (FileEntry)folder.getChild("README");
        // Should have content from previous commit
        Assert.assertEquals("test git importer", new String(readme.contentAsBytes()));
    }


    @Test
    public void testImportCheckoutCommitOtherBranch() throws Exception {
        GitConnection git = gitFactory.getConnection(gitRepo);
        git.branchCreate(dtoFactory.createDto(BranchCreateRequest.class).withName("otherBranchName")
                                   .withStartPoint(git.log(dtoFactory.createDto(LogRequest.class)).commits.get(0).getId()));
        git.branchCheckout(dtoFactory.createDto(BranchCheckoutRequest.class).withName("otherBranchName"));
        try (BufferedWriter w = Files.newBufferedWriter(new File(gitRepo, "README").toPath(), Charset.forName("UTF-8"))) {
            w.write("test import other branch");
        }
        git.commit(dtoFactory.createDto(CommitRequest.class).withAll(true).withMessage("commit"));
        LogPage log = git.log(dtoFactory.createDto(LogRequest.class));
        List<Revision> commits = log.getCommits();
        git.close();
        Assert.assertEquals(2, commits.size()); // have two commits now
        Revision commit = commits.get(0); // get first commit

        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        folder.createFolder(".codenvy");
        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("commitId", commit.getId());
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        GitConnection targetGit = gitFactory.getConnection(((VirtualFileImpl)folder.getVirtualFile()).getIoFile());
        final LogPage targetLog = targetGit.log(dtoFactory.createDto(LogRequest.class));
        List<Revision> targetCommits = targetLog.getCommits();
        targetGit.close();
        Assert.assertEquals(2, targetCommits.size()); // skip last commit from the source repository

        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNotNull(folder.getChild(".git"));
        FileEntry readme = (FileEntry)folder.getChild("README");
        // Should have content from previous commit
        Assert.assertEquals("test import other branch", new String(readme.contentAsBytes()));
    }

    @Test
    public void testImportKeepDirectory() throws Exception {
        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("keepDirectory", "src");
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        Assert.assertEquals(2, folder.getChildren().size());
    }

    @Test
    public void testImportKeepVcs() throws Exception {
        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        Map<String, String> parameters = new HashMap<>(2);
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNotNull(folder.getChild(".git"));
    }

    @Test
    public void testImportRemoveVcs() throws Exception {
        FolderEntry folder = new FolderEntry("my-vfs", vfs.getMountPoint().getRoot().createFolder("project"));
        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("keepVcs", "false");
        gitProjectImporter.importSources(folder, gitRepo.getAbsolutePath(), parameters, new SystemOutLineConsumerFactory());
        Assert.assertNotNull(folder.getChild("src"));
        Assert.assertNotNull(folder.getChild("src/hello.c"));
        Assert.assertNotNull(folder.getChild("README"));
        Assert.assertNull(folder.getChild(".git")); // git must not be removed
    }
}
