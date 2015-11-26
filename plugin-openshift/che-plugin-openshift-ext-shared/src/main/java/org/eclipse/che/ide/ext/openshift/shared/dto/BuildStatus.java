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
public interface BuildStatus {
    enum Phase {
        New,
        Pending,
        Running,
        Complete,
        Failed
    }

    Phase getPhase();

    void setPhase(Phase phase);

    BuildStatus withPhase(Phase phase);

    String getReason();

    void setReason(String reason);

    BuildStatus withReason(String reason);

    String getCompletionTimestamp();

    void setCompletionTimestamp(String completionTimestamp);

    BuildStatus withCompletionTimestamp(String completionTimestamp);

    boolean getCancelled();

    void setCancelled(boolean cancelled);

    BuildStatus withCancelled(boolean cancelled);

    String getMessage();

    void setMessage(String message);

    BuildStatus withMessage(String message);

    ObjectReference getConfig();

    void setConfig(ObjectReference config);

    BuildStatus withConfig(ObjectReference config);

    String getStartTimestamp();

    void setStartTimestamp(String startTimestamp);

    BuildStatus withStartTimestamp(String startTimestamp);

    String getOutputDockerImageReference();

    void setOutputDockerImageReference(String outputDockerImageReference);

    BuildStatus withOutputDockerImageReference(String outputDockerImageReference);

}
