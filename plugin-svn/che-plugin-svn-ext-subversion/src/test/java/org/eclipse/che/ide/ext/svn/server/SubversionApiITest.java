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


import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.vfs.server.ContentStream;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.repository.RepositoryUrlProvider;
import org.eclipse.che.ide.ext.svn.shared.*;
import org.eclipse.che.ide.ext.svn.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link SubversionApi}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SubversionApiITest {

    @Mock
    private CredentialsProvider credentialsProvider;
    @Mock
    private RepositoryUrlProvider repositoryUrlProvider;

    private SubversionApi subversionApi;
    private File          repoRoot;
    private Path          tmpDir;

    @Before
    public void setUp() throws Exception {
        repoRoot = TestUtils.createGreekTreeRepository();
        tmpDir = Files.createTempDirectory(SubversionApiITest.class.getName() + "-");

        tmpDir.toFile().deleteOnExit();

        this.subversionApi = new SubversionApi(credentialsProvider, repositoryUrlProvider);
    }

    /**
     * Tests for {@link SubversionApi#checkout(CheckoutRequest)}.
     *
     * @throws Exception
     *         if anything goes wrong
     */
    @Test
    public void testCheckout() throws Exception {
        CLIOutputWithRevisionResponse response =
                this.subversionApi.checkout(DtoFactory.getInstance()
                                                      .createDto(CheckoutRequest.class)
                                                      .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                                      .withUrl("file://" + repoRoot.getAbsolutePath())
                                                      .withDepth("immediates"));

        assertTrue(response.getRevision() > -1);
    }

    /**
     * Tests for {@link SubversionApi#copy(CopyRequest)}.
     *
     * @throws Exception
     *         if anything goes wrong
     */
    @Test
    public void testCopy() throws Exception {
        this.subversionApi.checkout(DtoFactory.getInstance()
                                              .createDto(CheckoutRequest.class)
                                              .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                              .withUrl("file://" + repoRoot.getAbsolutePath()));

        CLIOutputResponse response = this.subversionApi.copy(DtoFactory.getInstance()
                                                                       .createDto(CopyRequest.class)
                                                                       .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                                                       .withSource("A/B/lambda")
                                                                       .withDestination("A/B/E/lambda"));
        assertEquals(response.getOutput().size(), 1);
        assertTrue(response.getErrOutput().isEmpty());
        assertEquals(response.getOutput().get(0), "A         A/B/E/lambda");
    }

    /**
     * Tests for {@link SubversionApi#move(MoveRequest)}.
     *
     * @throws Exception
     *         if anything goes wrong
     */
    @Test
    public void testMove() throws Exception {
        this.subversionApi.checkout(DtoFactory.getInstance()
                                              .createDto(CheckoutRequest.class)
                                              .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                              .withUrl("file://" + repoRoot.getAbsolutePath()));

        CLIOutputResponse response = this.subversionApi.move(DtoFactory.getInstance()
                                                                       .createDto(MoveRequest.class)
                                                                       .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                                                       .withSource(Collections.singletonList("A/B/lambda"))
                                                                       .withDestination("A/B/E/lambda"));
        assertEquals(response.getOutput().size(), 2);
        assertTrue(response.getErrOutput().isEmpty());
        assertEquals(response.getOutput().get(0), "A         A/B/E/lambda");
        assertEquals(response.getOutput().get(1), "D         A/B/lambda");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExport() throws Exception {
        this.subversionApi.checkout(DtoFactory.getInstance()
                                              .createDto(CheckoutRequest.class)
                                              .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                              .withUrl("file://" + repoRoot.getAbsolutePath()));

        Response response = this.subversionApi.exportPath(tmpDir.toFile().getAbsolutePath(), "A/B/lambda", null, newServiceContext(), "ws");

        Link link = DtoFactory.getInstance().createDto(Link.class)
                              .withHref("http://localhost:8080/api/svn/ws/export/download/ws")
                              .withRel(Constants.REL_DOWNLOAD_EXPORT_PATH)
                              .withProduces("application/zip")
                              .withMethod("GET");
        Hyperlinks hyperlinks = DtoFactory.getInstance().createDto(Hyperlinks.class).withLinks(Collections.singletonList(link));

        assertEquals(response.getEntity(), hyperlinks);
    }

    private ServiceContext newServiceContext() {
        ServiceContext sc = mock(ServiceContext.class);
        doReturn(UriBuilder.fromUri("http://localhost:8080/api")).when(sc).getBaseUriBuilder();
        doReturn(UriBuilder.fromUri("http://localhost:8080/api/svn/" + "ws")).when(sc).getServiceUriBuilder();
        return sc;
    }

    /**
     * Tests for {@link SubversionApi#update(UpdateRequest)}.
     *
     * @throws Exception
     *         if anything goes wrong
     */
    @Test
    public void testUpdate() throws Exception {
        final long coRevision = this.subversionApi.checkout(DtoFactory.getInstance()
                                                                      .createDto(CheckoutRequest.class)
                                                                      .withProjectPath(tmpDir.toFile().getAbsolutePath())
                                                                      .withUrl("file://" + repoRoot.getAbsolutePath())
                                                                      .withDepth("immediates"))
                                                  .getRevision();
        final CLIOutputWithRevisionResponse response =
                this.subversionApi.update(DtoFactory.getInstance()
                                                    .createDto(UpdateRequest.class)
                                                    .withProjectPath(tmpDir.toFile().getAbsolutePath()));

        assertTrue(coRevision > -1);
        assertTrue(response.getRevision() > -1);

        assertTrue(coRevision <= response.getRevision());
    }

}
