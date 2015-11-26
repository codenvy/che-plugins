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
public interface Image {
    ObjectMeta getMetadata();

    void setMetadata(ObjectMeta metadata);

    Image withMetadata(ObjectMeta metadata);

    DockerImageMetadata getDockerImageMetadata();

    void setDockerImageMetadata(DockerImageMetadata dockerImageMetadata);

    Image withDockerImageMetadata(DockerImageMetadata dockerImageMetadata);

    String getApiVersion();

    void setApiVersion(String apiVersion);

    Image withApiVersion(String apiVersion);

    String getKind();

    void setKind(String kind);

    Image withKind(String kind);

    String getDockerImageReference();

    void setDockerImageReference(String dockerImageReference);

    Image withDockerImageReference(String dockerImageReference);

    String getDockerImageMetadataVersion();

    void setDockerImageMetadataVersion(String dockerImageMetadataVersion);

    Image withDockerImageMetadataVersion(String dockerImageMetadataVersion);

    String getDockerImageManifest();

    void setDockerImageManifest(String dockerImageManifest);

    Image withDockerImageManifest(String dockerImageManifest);

}
