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
