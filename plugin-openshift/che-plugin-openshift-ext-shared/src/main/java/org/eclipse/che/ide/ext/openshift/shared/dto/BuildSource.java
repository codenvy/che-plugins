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
package org.eclipse.che.ide.ext.openshift.shared.dto;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface BuildSource {
    LocalObjectReference getSourceSecret();

    void setSourceSecret(LocalObjectReference sourceSecret);

    BuildSource withSourceSecret(LocalObjectReference sourceSecret);

    GitBuildSource getGit();

    void setGit(GitBuildSource git);

    BuildSource withGit(GitBuildSource git);

    BinaryBuildSource getBinary();

    void setBinary(BinaryBuildSource binary);

    BuildSource withBinary(BinaryBuildSource binary);

    String getDockerfile();

    void setDockerfile(String dockerfile);

    BuildSource withDockerfile(String dockerfile);

    String getType();

    void setType(String type);

    BuildSource withType(String type);

    String getContextDir();

    void setContextDir(String contextDir);

    BuildSource withContextDir(String contextDir);

}
