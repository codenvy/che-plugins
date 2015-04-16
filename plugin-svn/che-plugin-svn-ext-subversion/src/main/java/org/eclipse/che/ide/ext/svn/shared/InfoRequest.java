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

import org.eclipse.che.dto.shared.DTO;

import javax.validation.constraints.NotNull;
import java.util.List;

@DTO
public interface InfoRequest {

    /**
     * @return the project path the request is associated with.
     */
    String getProjectPath();

    /**
     * @param projectPath the project path to set
     */
    void setProjectPath(@NotNull final String projectPath);

    /**
     * @param projectPath the project path to use
     */
    InfoRequest withProjectPath(@NotNull final String projectPath);

    /**
     * @return the paths the request is associated with
     */
    List<String> getPaths();

    /**
     * @param paths the paths to set
     */
    void setPaths(@NotNull final List<String> paths);

    /**
     * @param paths the paths to use
     */
    InfoRequest withPaths(@NotNull final List<String> paths);

    /**
     * @return the revision to update to
     */
    String getRevision();

    /**
     * @param revision the revision to set
     */
    void setRevision(@NotNull final String revision);

    /**
     * @param revision the revision
     *
     * @return the request
     */
    InfoRequest withRevision(@NotNull final String revision);

}
