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
package org.eclipse.che.plugin.docker.runner;

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
