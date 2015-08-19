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

import javax.validation.constraints.NotNull;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.datasource.shared.ssl.SslKeyStoreEntry;

import java.util.List;

public interface SslKeyStoreManagerView extends View<SslKeyStoreManagerView.ActionDelegate> {
    public interface ActionDelegate {

        void onClientKeyDeleteClicked(@NotNull SslKeyStoreEntry key);

        void onClientKeyUploadClicked();

        void onServerCertDeleteClicked(@NotNull SslKeyStoreEntry key);

        void onServerCertUploadClicked();
    }

    void setClientKeys(@NotNull List<SslKeyStoreEntry> keys);

    void setServerCerts(List<SslKeyStoreEntry> keys);
}
