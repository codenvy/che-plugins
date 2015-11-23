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
package org.eclipse.che.ide.ext.openshift.server.rest;

import org.eclipse.che.ide.ext.openshift.server.ClientFactory;
import org.eclipse.che.ide.ext.openshift.shared.dto.OpenshiftServerInfo;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Sergii Leschenko
 */
@Path("/openshift/{ws-id}/")
public class OpenshiftApiInfoService {
    private final ClientFactory clientFactory;

    @Inject
    public OpenshiftApiInfoService(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OpenshiftServerInfo getServerInfo() {
        return clientFactory.getClientInfo();
    }
}
