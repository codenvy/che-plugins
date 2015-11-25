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
package org.eclipse.che.ide.ext.openshift.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.ext.openshift.shared.dto.OpenshiftServerInfo;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides path for websocket connection with openshift server
 *
 * @author Sergii Leschenko
 */
@Singleton
public class OpenshiftWebSocketPathProvider {
    private       String                 openshiftWSPath;
    private final OpenshiftServiceClient openshiftServiceClient;

    @Inject
    public OpenshiftWebSocketPathProvider(OpenshiftServiceClient openshiftServiceClient) {
        this.openshiftServiceClient = openshiftServiceClient;
    }

    /**
     * Returns openshift endpoint for opening WebSocket connections
     */
    public void get(final AsyncCallback<? super String> asyncCallback) {
        if (openshiftWSPath == null) {
            openshiftServiceClient.getServerInfo().then(new Operation<OpenshiftServerInfo>() {
                @Override
                public void apply(OpenshiftServerInfo result) throws OperationException {
                    final String endpoint = result.getOpenshiftEndpoint();
                    final String[] protocolHostPair = endpoint.split("://");
                    if (protocolHostPair.length != 2) {
                        asyncCallback.onFailure(new OperationException("Configured openshift endpoint " + endpoint + " is invalid"));
                        return;
                    }

                    final String protocol = protocolHostPair[0];
                    final String host = protocolHostPair[1];
                    boolean isSecureConnection = "https".equals(protocol);
                    openshiftWSPath = (isSecureConnection ? "wss" : "ws") + "://" + host;
                    asyncCallback.onSuccess(openshiftWSPath);
                }
            }).catchError(new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError arg) throws OperationException {
                    asyncCallback.onFailure(arg.getCause());
                }
            });
        } else {
            asyncCallback.onSuccess(openshiftWSPath);
        }
    }
}
