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

@DTO
public interface InfoRequest {

    String getPath();

    void setPath(String path);

    InfoRequest withPath(String path);

    String getProjectPath();

    void setProjectPath(String projectPath);

    InfoRequest withProjectPath(String projectPath);
}
