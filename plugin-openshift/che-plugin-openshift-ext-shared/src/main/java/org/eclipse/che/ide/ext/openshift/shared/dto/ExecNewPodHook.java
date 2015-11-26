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

import java.util.List;

@DTO
public interface ExecNewPodHook {
    String getContainerName();

    void setContainerName(String containerName);

    ExecNewPodHook withContainerName(String containerName);

    List<String> getVolumes();

    void setVolumes(List<String> volumes);

    ExecNewPodHook withVolumes(List<String> volumes);

    List<EnvVar> getEnv();

    void setEnv(List<EnvVar> env);

    ExecNewPodHook withEnv(List<EnvVar> env);

    List<String> getCommand();

    void setCommand(List<String> command);

    ExecNewPodHook withCommand(List<String> command);

}
