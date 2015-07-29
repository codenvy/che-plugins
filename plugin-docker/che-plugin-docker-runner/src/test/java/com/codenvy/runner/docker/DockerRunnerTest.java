/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.runner.docker;


import com.codenvy.docker.DockerConnector;
import com.codenvy.docker.InitialAuthConfig;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.internal.ResourceAllocators;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;

/**
 * Allow to test the base docker runner
 * @author Florent Benoit
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerRunnerTest {


    private static final String TOKEN = "abcd-supertoken";
    private static final String HOSTNAME = "mytest.codenvy.com";
    private static final String WORKSPACE_ID = "workspace8abshw8xmtw2j3f1";
    private static final String PROJECT_NAME = "java-console";
    private static final String PROJECT_ID = "p345483";


    private DockerRunner dockerRunner;

    private List<String> env;

    @Mock
    private File deployDirectoryRoot;

    @Mock
    private ResourceAllocators allocators;

    @Mock
    private CustomPortService portService;

    @Mock
    private DockerConnector dockerConnector;

    @Mock
    private EventService eventService;

    @Mock
    private ApplicationLinksGenerator applicationLinksGenerator;

    @Mock
    private BaseDockerRunner.DockerRunnerConfiguration dockerRunnerConfiguration;

    @Mock
    private BaseDockerRunner.CodenvyPortMappings codenvyPortMappings;

    @Mock
    private RunRequest request;

    @Mock
    private InitialAuthConfig initialAuthConfig;

    @Before
    public void beforeTest() {
        this.dockerRunner =
                new DockerRunner(deployDirectoryRoot, 5, HOSTNAME, "localhost:8080/api", new String[]{}, allocators, portService,
                                 initialAuthConfig, dockerConnector, eventService,
                                 applicationLinksGenerator);

        this.env = new ArrayList<>();
        doReturn(HOSTNAME).when(dockerRunnerConfiguration).getHost();
        doReturn(request).when(dockerRunnerConfiguration).getRequest();
        doReturn(WORKSPACE_ID).when(request).getWorkspace();
        doReturn("/" + PROJECT_NAME).when(request).getProject();
        doReturn(TOKEN).when(request).getUserToken();

    }


    /**
     * Check that the host is added in the environment
     */
    @Test
    public void testHostEnvironmentVariables() {
        dockerRunner.setupEnvironmentVariables(env, dockerRunnerConfiguration, codenvyPortMappings);

        // check we have the expected token in the env
        Assert.assertTrue(env.contains("CODENVY_HOSTNAME=" + HOSTNAME));
    }

    /**
     * Check that the token is added in the environment
     */
    @Test
    public void testTokenEnvironmentVariables() {
        dockerRunner.setupEnvironmentVariables(env, dockerRunnerConfiguration, codenvyPortMappings);

        // check we have the expected token in the env
        Assert.assertTrue(env.contains("CODENVY_TOKEN=" + TOKEN));
    }


    /**
     * Check that the workspace/project and encoded projects are added in the environment
     */
    @Test
    public void testProjectsEnvironmentVariables() {
        dockerRunner.setupEnvironmentVariables(env, dockerRunnerConfiguration, codenvyPortMappings);

        // check we have the expected workspace ID in the env
        Assert.assertTrue(env.contains("CODENVY_WORKSPACE_ID=" + WORKSPACE_ID));

        // check we have the expected project name in the env
        Assert.assertTrue(env.contains("CODENVY_PROJECT_NAME=" + PROJECT_NAME));

        // check we have the expected encoded PROJECT ID in the env
        Assert.assertTrue(env.contains("CODENVY_PROJECT_ID=" + PROJECT_ID));


    }

}
