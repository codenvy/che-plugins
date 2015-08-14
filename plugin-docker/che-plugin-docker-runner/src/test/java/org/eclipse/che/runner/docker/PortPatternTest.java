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
package org.eclipse.che.runner.docker;

import org.eclipse.che.runner.docker.BaseDockerRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

/**
 * @author andrew00x
 */
public class PortPatternTest {
    @Test
    public void testMatchedHttpPortPattern() {
        Assert.assertTrue(BaseDockerRunner.APP_HTTP_PORT_PATTERN.matcher("CODENVY_APP_PORT_8080_HTTP=8080").matches());
    }

    @Test
    public void testGetHttpPort() {
        Matcher matcher = BaseDockerRunner.APP_HTTP_PORT_PATTERN.matcher("CODENVY_APP_PORT_8080_HTTP=8080");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("8080", matcher.group(3));
    }

    @Test
    public void testGetHttpPort_fix_IDEX_2418() {
        //see https://github.com/codenvy/plugin-hosted/commit/d957645a1b03fa2994bdbb38d2f7d59acdff4684
        Matcher matcher = BaseDockerRunner.APP_HTTP_PORT_PATTERN.matcher("CODENVY_APP_PORT_25565_HTTP=25565");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("25565", matcher.group(3));
    }

    @Test
    public void testMatchedDebugPortPattern() {
        Assert.assertTrue(BaseDockerRunner.APP_DEBUG_PORT_PATTERN.matcher("CODENVY_APP_PORT_8080_DEBUG=8000").matches());
    }

    @Test
    public void testGetDebugPort() {
        Matcher matcher = BaseDockerRunner.APP_DEBUG_PORT_PATTERN.matcher("CODENVY_APP_PORT_8000_DEBUG=8000");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("8000", matcher.group(3));
    }

    @Test
    public void testMatchedWebShellPortPattern() {
        Assert.assertTrue(BaseDockerRunner.WEB_SHELL_PORT_PATTERN.matcher("CODENVY_WEB_SHELL_PORT=4200").matches());
    }

    @Test
    public void testGetWebShellPort() {
        Matcher matcher = BaseDockerRunner.WEB_SHELL_PORT_PATTERN.matcher("CODENVY_WEB_SHELL_PORT=4200");
        Assert.assertTrue(matcher.matches());
        Assert.assertEquals("4200", matcher.group(2));
    }
}
