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
