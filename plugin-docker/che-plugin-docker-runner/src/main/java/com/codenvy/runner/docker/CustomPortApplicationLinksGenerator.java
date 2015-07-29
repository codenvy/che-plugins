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
package com.codenvy.runner.docker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Link generator that uses predefined URL template that allows to customize application port.
 * <p/>
 * Template must follow java.util.Formatter rules.
 *
 * @author andrew00x
 */
@Singleton
public class CustomPortApplicationLinksGenerator implements ApplicationLinksGenerator {
    private final String applicationLinkTemplate;
    private final String webShellLinkTemplate;

    @Inject
    public CustomPortApplicationLinksGenerator(@Named("runner.docker.application_link_template") String applicationLinkTemplate,
                                               @Named("runner.docker.web_shell_link_template") String webShellLinkTemplate) {
        this.applicationLinkTemplate = applicationLinkTemplate;
        this.webShellLinkTemplate = webShellLinkTemplate;
    }

    @Override
    public String createApplicationLink(String workspace, String project, String user, int port) {
        return String.format(applicationLinkTemplate, port);
    }

    @Override
    public String createWebShellLink(String workspace, String project, String user, int port) {
        return String.format(webShellLinkTemplate, port);
    }
}
