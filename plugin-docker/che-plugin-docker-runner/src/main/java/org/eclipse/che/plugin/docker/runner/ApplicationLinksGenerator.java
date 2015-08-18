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

/**
 * @author andrew00x
 */
public interface ApplicationLinksGenerator {
    String createApplicationLink(String workspace, String project, String user, int port);

    String createWebShellLink(String workspace, String project, String user, int port);
}
