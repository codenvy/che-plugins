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
package org.eclipse.che.ide.ext.svn.shared;

import java.util.List;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface InfoResponse {

    String getCommandLine();

    InfoResponse withCommandLine(String commandLine);

    String getRepositoryUrl();

    InfoResponse withRepositoryUrl(String repositoryUrl);

    String getRepositoryRoot();

    InfoResponse withRepositoryRoot(String repositoryRootUrl);

    String getRevision();

    InfoResponse withRevision(String revision);

    int getExitCode();

    InfoResponse withExitCode(int exitCode);

    List<String> getErrorOutput();

    InfoResponse withErrorOutput(List<String> stderr);

}
