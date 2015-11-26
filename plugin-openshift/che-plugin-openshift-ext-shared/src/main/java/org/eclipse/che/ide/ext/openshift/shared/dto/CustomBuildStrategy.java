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
public interface CustomBuildStrategy {
    boolean getForcePull();

    void setForcePull(boolean forcePull);

    CustomBuildStrategy withForcePull(boolean forcePull);

    ObjectReference getFrom();

    void setFrom(ObjectReference from);

    CustomBuildStrategy withFrom(ObjectReference from);

    List<EnvVar> getEnv();

    void setEnv(List<EnvVar> env);

    CustomBuildStrategy withEnv(List<EnvVar> env);

    LocalObjectReference getPullSecret();

    void setPullSecret(LocalObjectReference pullSecret);

    CustomBuildStrategy withPullSecret(LocalObjectReference pullSecret);

    boolean getExposeDockerSocket();

    void setExposeDockerSocket(boolean exposeDockerSocket);

    CustomBuildStrategy withExposeDockerSocket(boolean exposeDockerSocket);

    List<SecretSpec> getSecrets();

    void setSecrets(List<SecretSpec> secrets);

    CustomBuildStrategy withSecrets(List<SecretSpec> secrets);

}
