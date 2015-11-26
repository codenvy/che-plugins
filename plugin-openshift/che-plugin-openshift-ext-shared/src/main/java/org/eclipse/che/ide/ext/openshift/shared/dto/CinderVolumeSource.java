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
public interface CinderVolumeSource {
    String getVolumeID();

    void setVolumeID(String volumeID);

    CinderVolumeSource withVolumeID(String volumeID);

    boolean getReadOnly();

    void setReadOnly(boolean readOnly);

    CinderVolumeSource withReadOnly(boolean readOnly);

    String getFsType();

    void setFsType(String fsType);

    CinderVolumeSource withFsType(String fsType);

}
