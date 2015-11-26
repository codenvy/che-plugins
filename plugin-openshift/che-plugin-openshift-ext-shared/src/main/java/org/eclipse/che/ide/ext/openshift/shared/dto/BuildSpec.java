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
public interface BuildSpec {
    BuildOutput getOutput();

    void setOutput(BuildOutput output);

    BuildSpec withOutput(BuildOutput output);

    Integer getCompletionDeadlineSeconds();

    void setCompletionDeadlineSeconds(Integer completionDeadlineSeconds);

    BuildSpec withCompletionDeadlineSeconds(Integer completionDeadlineSeconds);

    ResourceRequirements getResources();

    void setResources(ResourceRequirements resources);

    BuildSpec withResources(ResourceRequirements resources);

    String getServiceAccount();

    void setServiceAccount(String serviceAccount);

    BuildSpec withServiceAccount(String serviceAccount);

    BuildSource getSource();

    void setSource(BuildSource source);

    BuildSpec withSource(BuildSource source);

    BuildStrategy getStrategy();

    void setStrategy(BuildStrategy strategy);

    BuildSpec withStrategy(BuildStrategy strategy);

    SourceRevision getRevision();

    void setRevision(SourceRevision revision);

    BuildSpec withRevision(SourceRevision revision);

}
