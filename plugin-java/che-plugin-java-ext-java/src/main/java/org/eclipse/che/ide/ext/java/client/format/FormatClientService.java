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

import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Roman Nikitenko
 */
public class FormatClientService {


    public static final String CODENVY = "/codenvy";

    private final AsyncRequestFactory asyncRequestFactory;
    private final String              baseHttpUrl;
    public final  String              formatServicePath;

    @Inject
    public FormatClientService(@RestContext String baseHttpUrl,
                               AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.formatServicePath = "/code-formatting";
        this.baseHttpUrl = baseHttpUrl;
    }

    public void formattingCodenvySettings(AsyncRequestCallback<String> callback) {
        String url = baseHttpUrl + formatServicePath + CODENVY;
        asyncRequestFactory.createGetRequest(url).send(callback);
    }
}
