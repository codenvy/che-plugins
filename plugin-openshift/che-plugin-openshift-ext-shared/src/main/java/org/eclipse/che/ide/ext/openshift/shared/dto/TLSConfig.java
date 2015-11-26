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
public interface TLSConfig {
    String getTermination();

    void setTermination(String termination);

    TLSConfig withTermination(String termination);

    String getInsecureEdgeTerminationPolicy();

    void setInsecureEdgeTerminationPolicy(String insecureEdgeTerminationPolicy);

    TLSConfig withInsecureEdgeTerminationPolicy(String insecureEdgeTerminationPolicy);

    String getCertificate();

    void setCertificate(String certificate);

    TLSConfig withCertificate(String certificate);

    String getDestinationCACertificate();

    void setDestinationCACertificate(String destinationCACertificate);

    TLSConfig withDestinationCACertificate(String destinationCACertificate);

    String getCaCertificate();

    void setCaCertificate(String caCertificate);

    TLSConfig withCaCertificate(String caCertificate);

    String getKey();

    void setKey(String key);

    TLSConfig withKey(String key);

}
