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
public interface RouteSpec {
    String getPath();

    void setPath(String path);

    RouteSpec withPath(String path);

    RoutePort getPort();

    void setPort(RoutePort port);

    RouteSpec withPort(RoutePort port);

    String getHost();

    void setHost(String host);

    RouteSpec withHost(String host);

    TLSConfig getTls();

    void setTls(TLSConfig tls);

    RouteSpec withTls(TLSConfig tls);

    ObjectReference getTo();

    void setTo(ObjectReference to);

    RouteSpec withTo(ObjectReference to);

}
