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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author andrew00x
 */
public class CustomPortApplicationLinksGeneratorTest {

    private CustomPortApplicationLinksGenerator linksGenerator;

    @Before
    public void beforeTest() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ApplicationLinksGenerator.class).to(CustomPortApplicationLinksGenerator.class);
                bindConstant().annotatedWith(Names.named("runner.docker.web_shell_link_template")).to("https://ws-%d-runner1.codenvy.com");
                bindConstant().annotatedWith(Names.named("runner.docker.application_link_template")).to("http://runner1.codenvy.com:%d");
            }
        });
        linksGenerator = (CustomPortApplicationLinksGenerator)injector.getInstance(ApplicationLinksGenerator.class);
    }

    @Test
    public void testApplicationLink() {
        Assert.assertEquals("http://runner1.codenvy.com:49681", linksGenerator.createApplicationLink(null, null, null, 49681));
    }

    @Test
    public void testWebShellLink() {
        Assert.assertEquals("https://ws-49679-runner1.codenvy.com", linksGenerator.createWebShellLink(null, null, null, 49679));
    }
}
