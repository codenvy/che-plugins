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
public interface Container {
    String getImage();

    void setImage(String image);

    Container withImage(String image);

    Probe getLivenessProbe();

    void setLivenessProbe(Probe livenessProbe);

    Container withLivenessProbe(Probe livenessProbe);

    String getImagePullPolicy();

    void setImagePullPolicy(String imagePullPolicy);

    Container withImagePullPolicy(String imagePullPolicy);

    boolean getStdin();

    void setStdin(boolean stdin);

    Container withStdin(boolean stdin);

    String getTerminationMessagePath();

    void setTerminationMessagePath(String terminationMessagePath);

    Container withTerminationMessagePath(String terminationMessagePath);

    String getWorkingDir();

    void setWorkingDir(String workingDir);

    Container withWorkingDir(String workingDir);

    ResourceRequirements getResources();

    void setResources(ResourceRequirements resources);

    Container withResources(ResourceRequirements resources);

    SecurityContext getSecurityContext();

    void setSecurityContext(SecurityContext securityContext);

    Container withSecurityContext(SecurityContext securityContext);

    List<ContainerPort> getPorts();

    void setPorts(List<ContainerPort> ports);

    Container withPorts(List<ContainerPort> ports);

    List<EnvVar> getEnv();

    void setEnv(List<EnvVar> env);

    Container withEnv(List<EnvVar> env);

    List<String> getCommand();

    void setCommand(List<String> command);

    Container withCommand(List<String> command);

    List<VolumeMount> getVolumeMounts();

    void setVolumeMounts(List<VolumeMount> volumeMounts);

    Container withVolumeMounts(List<VolumeMount> volumeMounts);

    List<String> getArgs();

    void setArgs(List<String> args);

    Container withArgs(List<String> args);

    Lifecycle getLifecycle();

    void setLifecycle(Lifecycle lifecycle);

    Container withLifecycle(Lifecycle lifecycle);

    String getName();

    void setName(String name);

    Container withName(String name);

    boolean getTty();

    void setTty(boolean tty);

    Container withTty(boolean tty);

    Probe getReadinessProbe();

    void setReadinessProbe(Probe readinessProbe);

    Container withReadinessProbe(Probe readinessProbe);

    boolean getStdinOnce();

    void setStdinOnce(boolean stdinOnce);

    Container withStdinOnce(boolean stdinOnce);

}
