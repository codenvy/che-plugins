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
public interface FCVolumeSource {
    List<String> getTargetWWNs();

    void setTargetWWNs(List<String> targetWWNs);

    FCVolumeSource withTargetWWNs(List<String> targetWWNs);

    Integer getLun();

    void setLun(Integer lun);

    FCVolumeSource withLun(Integer lun);

    boolean getReadOnly();

    void setReadOnly(boolean readOnly);

    FCVolumeSource withReadOnly(boolean readOnly);

    String getFsType();

    void setFsType(String fsType);

    FCVolumeSource withFsType(String fsType);

}
