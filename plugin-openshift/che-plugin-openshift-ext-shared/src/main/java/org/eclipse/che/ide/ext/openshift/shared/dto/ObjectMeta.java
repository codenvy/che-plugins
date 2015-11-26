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

import java.util.Map;

@DTO
public interface ObjectMeta {
    Integer getGeneration();

    void setGeneration(Integer generation);

    ObjectMeta withGeneration(Integer generation);

    Integer getDeletionGracePeriodSeconds();

    void setDeletionGracePeriodSeconds(Integer deletionGracePeriodSeconds);

    ObjectMeta withDeletionGracePeriodSeconds(Integer deletionGracePeriodSeconds);

    String getUid();

    void setUid(String uid);

    ObjectMeta withUid(String uid);

    String getResourceVersion();

    void setResourceVersion(String resourceVersion);

    ObjectMeta withResourceVersion(String resourceVersion);

    String getName();

    void setName(String name);

    ObjectMeta withName(String name);

    String getNamespace();

    void setNamespace(String namespace);

    ObjectMeta withNamespace(String namespace);

    String getCreationTimestamp();

    void setCreationTimestamp(String creationTimestamp);

    ObjectMeta withCreationTimestamp(String creationTimestamp);

    Map<String, String> getAnnotations();

    void setAnnotations(Map<String, String> annotations);

    ObjectMeta withAnnotations(Map<String, String> annotations);

    String getGenerateName();

    void setGenerateName(String generateName);

    ObjectMeta withGenerateName(String generateName);

    String getSelfLink();

    void setSelfLink(String selfLink);

    ObjectMeta withSelfLink(String selfLink);

    String getDeletionTimestamp();

    void setDeletionTimestamp(String deletionTimestamp);

    ObjectMeta withDeletionTimestamp(String deletionTimestamp);

    Map<String, String> getLabels();

    void setLabels(Map<String, String> labels);

    ObjectMeta withLabels(Map<String, String> labels);

}
