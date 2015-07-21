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
package org.eclipse.che.ide.ext.java.client.format;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.shared.dto.Change;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;

import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * @author Roman Nikitenko
 */
public class FormatClientServiceImpl implements FormatClientService {
    private final AsyncRequestFactory      asyncRequestFactory;
    private final String                   restContext;
    public final  String                   formatServicePath;
    private final Provider<MachineManager> managerProvider;

    @Inject
    public FormatClientServiceImpl(@Named("cheExtensionPath") String restContext,
                                   AsyncRequestFactory asyncRequestFactory,
                                   Provider<MachineManager> managerProvider) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.managerProvider = managerProvider;
        this.formatServicePath = "/code-formatting";
        this.restContext = restContext;
    }

    private String getContext() {
        MachineManager machineManager = managerProvider.get();

        if (machineManager.getDeveloperMachineId() == null) {
            throw new IllegalStateException("Developer machine ID is null. Can't create request URL");
        }
        return restContext + "/" + machineManager.getDeveloperMachineId();
    }

    @Override
    public void format(int offset, int length, String content, AsyncRequestCallback<Array<Change>> callback) {
        String url = getContext() + formatServicePath + "/format?offset=" + offset + "&length=" + length;
        asyncRequestFactory.createPostRequest(url, null)
                           .header(CONTENT_TYPE, MimeType.TEXT_PLAIN)
                           .data(content)
                           .send(callback);
    }
}
