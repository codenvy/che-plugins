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
package org.eclipse.che.ide.ext.svn.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.repository.RepositoryUrlProvider;
import org.eclipse.che.ide.ext.svn.server.rest.SubversionService;
import org.eclipse.che.ide.ext.svn.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;

@RunWith(MockitoJUnitRunner.class)
public class SubversionProjectImporterTest {

    @Mock
    private UserProfileDao userProfileDao;

    @Mock
    private CredentialsProvider credentialsProvider;

    @Mock
    private RepositoryUrlProvider repositoryUrlProvider;

    private File repoRoot;
    private VirtualFileSystem vfs;
    private SubversionProjectImporter projectImporter;

    @Before
    public void setUp() throws Exception {
        // Bind components
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), ProjectImporter.class).addBinding().to(SubversionProjectImporter.class);
                Multibinder.newSetBinder(binder(), ProjectType.class).addBinding().to(SubversionProjectType.class);
                Multibinder.newSetBinder(binder(), ValueProviderFactory.class).addBinding()
                           .to(SubversionValueProviderFactory.class);

                bind(SubversionService.class);
                bind(UserProfileDao.class).toInstance(userProfileDao);
                bind(CredentialsProvider.class).toInstance(credentialsProvider);
                bind(RepositoryUrlProvider.class).toInstance(repositoryUrlProvider);
            }
        });

        // Init virtual file system
        vfs = TestUtils.createVirtualFileSystem();

        // Create the test user
        TestUtils.createTestUser(userProfileDao);

        // Create the Subversion repository
        repoRoot = TestUtils.createGreekTreeRepository();

        projectImporter = injector.getInstance(SubversionProjectImporter.class);
    }

    /**
     * Test for {@link SubversionProjectImporter#getCategory()}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetCategory() throws Exception {
        assertEquals(projectImporter.getCategory(), ProjectImporter.ImporterCategory.SOURCE_CONTROL);
    }

    /**
     * Test for {@link SubversionProjectImporter#getDescription()}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetDescription() throws Exception {
        assertEquals(projectImporter.getDescription(), "Import project from Subversion repository URL.");
    }

    /**
     * Test for {@link SubversionProjectImporter#getId()}
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetId() throws Exception {
        assertEquals(projectImporter.getId(), "subversion");
    }

    /**
     * Test for {@link SubversionProjectImporter#isInternal()}.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIsInternal() throws Exception {
        assertEquals(projectImporter.isInternal(), false);
    }

    /**
     * Test for {@link SubversionProjectImporter#importSources(org.eclipse.che.api.project.server.FolderEntry, String, java.util.Map, org.eclipse.che.api.core.util.LineConsumerFactory)}
     * invalid url.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testInvalidImportSources() throws Exception {
        final FolderEntry project = new FolderEntry("plugin-svn-test",
                                                    vfs.getMountPoint().getRoot().createFolder("project"));

        try {
            String fakeUrl = Paths.get(repoRoot.getAbsolutePath()).toUri() + "fake";
            projectImporter.importSources(project, fakeUrl, null, new TestUtils.SystemOutLineConsumerFactory());

            fail("The code above should had failed");
        } catch (SubversionException e) {
            final String message = e.getMessage();

            boolean assertBoolean = Pattern.matches("svn: (E[0-9]{6}: )?URL 'file://.*/fake' doesn't exist\n?", message.trim());
            assertTrue(message, assertBoolean);
        }
    }

    /**
     * Test for {@link SubversionProjectImporter#importSources(org.eclipse.che.api.project.server.FolderEntry, String, java.util.Map, org.eclipse.che.api.core.util.LineConsumerFactory)}
     * with a valid url.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testValidImportSources() throws Exception {
        final FolderEntry project = new FolderEntry("plugin-svn-test",
                                                    vfs.getMountPoint().getRoot().createFolder("project"));

        String repoUrl = Paths.get(repoRoot.getAbsolutePath()).toUri().toString();
        projectImporter.importSources(project, repoUrl, null, new TestUtils.SystemOutLineConsumerFactory());

        assertTrue(project.getChild(".svn").isFolder());
        assertTrue(project.getChild("A").isFolder());
        assertTrue(project.getChild("iota").isFile());
    }

}
