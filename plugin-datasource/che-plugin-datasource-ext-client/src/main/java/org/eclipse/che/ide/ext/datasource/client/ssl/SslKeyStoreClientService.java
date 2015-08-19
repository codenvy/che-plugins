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
package org.eclipse.che.ide.ext.datasource.client.ssl;

import org.eclipse.che.ide.ext.datasource.shared.ssl.SslKeyStoreEntry;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.List;

public interface SslKeyStoreClientService {

    void getAllClientKeys(AsyncRequestCallback<List<SslKeyStoreEntry>> callback);

    void getAllServerCerts(AsyncRequestCallback<List<SslKeyStoreEntry>> asyncRequestCallback);

    void deleteClientKey(SslKeyStoreEntry entry, AsyncRequestCallback<Void> asyncRequestCallback);

    String getUploadClientKeyAction(String alias);

    void deleteServerCert(SslKeyStoreEntry key, AsyncRequestCallback<Void> asyncRequestCallback);

    String getUploadServerCertAction(String alias);

}
