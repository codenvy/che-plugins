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
public interface BuildConfigSpec {
    BuildOutput getOutput();

    void setOutput(BuildOutput output);

    BuildConfigSpec withOutput(BuildOutput output);

    Integer getCompletionDeadlineSeconds();

    void setCompletionDeadlineSeconds(Integer completionDeadlineSeconds);

    BuildConfigSpec withCompletionDeadlineSeconds(Integer completionDeadlineSeconds);

    ResourceRequirements getResources();

    void setResources(ResourceRequirements resources);

    BuildConfigSpec withResources(ResourceRequirements resources);

    String getServiceAccount();

    void setServiceAccount(String serviceAccount);

    BuildConfigSpec withServiceAccount(String serviceAccount);

    BuildSource getSource();

    void setSource(BuildSource source);

    BuildConfigSpec withSource(BuildSource source);

    List<BuildTriggerPolicy> getTriggers();

    void setTriggers(List<BuildTriggerPolicy> triggers);

    BuildConfigSpec withTriggers(List<BuildTriggerPolicy> triggers);

    BuildStrategy getStrategy();

    void setStrategy(BuildStrategy strategy);

    BuildConfigSpec withStrategy(BuildStrategy strategy);

    SourceRevision getRevision();

    void setRevision(SourceRevision revision);

    BuildConfigSpec withRevision(SourceRevision revision);

}
