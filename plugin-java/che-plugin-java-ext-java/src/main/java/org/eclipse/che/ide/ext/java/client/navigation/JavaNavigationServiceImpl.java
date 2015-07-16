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
package org.eclipse.che.ide.ext.java.client.navigation;

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.ext.java.shared.OpenDeclarationDescriptor;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import com.google.gwt.http.client.URL;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class JavaNavigationServiceImpl implements JavaNavigationService {

    private final String              restContext;
    private final String              workspaceId;
    private       AsyncRequestFactory requestFactory;

    @Inject
    public JavaNavigationServiceImpl(@Named("javaCA") String restContext,
                                     @Named("workspaceId") String workspaceId,
                                     AsyncRequestFactory asyncRequestFactory) {
        this.restContext = restContext;
        this.workspaceId = workspaceId;
        this.requestFactory = asyncRequestFactory;
    }

    @Override
    public void findDeclaration(String projectPath, String keyBinding, AsyncRequestCallback<OpenDeclarationDescriptor> callback) {
        String url =
                restContext + "/navigation/" + workspaceId + "/find-declaration?projectpath=" + projectPath + "&bindingkey=" + URL.encodeQueryString(keyBinding);
        requestFactory.createGetRequest(url).send(callback);
    }

    public void getExternalLibraries(String projectPath, AsyncRequestCallback<Array<Jar>> callback){
        String url =
                restContext + "/navigation/" + workspaceId + "/libraries?projectpath=" + projectPath;
        requestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public void getLibraryChildren(String projectPath, int libId, AsyncRequestCallback<Array<JarEntry>> callback) {
        String url =
                restContext + "/navigation/" + workspaceId + "/lib/children?projectpath=" + projectPath + "&root=" + libId;
        requestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public void getChildren(String projectPath, int libId, String path, AsyncRequestCallback<Array<JarEntry>> callback) {
        String url =
                restContext + "/navigation/" + workspaceId + "/children?projectpath=" + projectPath + "&root=" + libId + "&path=" + path;
        requestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public void getEntry(String projectPath, int libId, String path, AsyncRequestCallback<JarEntry> callback) {
        String url =
                restContext + "/navigation/" + workspaceId + "/entry?projectpath=" + projectPath + "&root=" + libId + "&path=" + path;
        requestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public void getContent(String projectPath, int libId, String path, AsyncRequestCallback<String> callback) {
        String url = getContentUrl(projectPath, libId, path);

        requestFactory.createGetRequest(url).send(callback);
    }

    @Override
    public String getContentUrl(String projectPath, int libId, String path) {
        return restContext + "/navigation/" + workspaceId + "/content?projectpath=" + projectPath + "&root=" + libId + "&path=" + path;
    }
}
