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
public interface RollingDeploymentStrategyParams {
    String getMaxSurge();

    void setMaxSurge(String maxSurge);

    RollingDeploymentStrategyParams withMaxSurge(String maxSurge);

    Integer getUpdatePeriodSeconds();

    void setUpdatePeriodSeconds(Integer updatePeriodSeconds);

    RollingDeploymentStrategyParams withUpdatePeriodSeconds(Integer updatePeriodSeconds);

    Integer getUpdatePercent();

    void setUpdatePercent(Integer updatePercent);

    RollingDeploymentStrategyParams withUpdatePercent(Integer updatePercent);

    LifecycleHook getPre();

    void setPre(LifecycleHook pre);

    RollingDeploymentStrategyParams withPre(LifecycleHook pre);

    LifecycleHook getPost();

    void setPost(LifecycleHook post);

    RollingDeploymentStrategyParams withPost(LifecycleHook post);

    String getMaxUnavailable();

    void setMaxUnavailable(String maxUnavailable);

    RollingDeploymentStrategyParams withMaxUnavailable(String maxUnavailable);

    Integer getTimeoutSeconds();

    void setTimeoutSeconds(Integer timeoutSeconds);

    RollingDeploymentStrategyParams withTimeoutSeconds(Integer timeoutSeconds);

    Integer getIntervalSeconds();

    void setIntervalSeconds(Integer intervalSeconds);

    RollingDeploymentStrategyParams withIntervalSeconds(Integer intervalSeconds);

}
