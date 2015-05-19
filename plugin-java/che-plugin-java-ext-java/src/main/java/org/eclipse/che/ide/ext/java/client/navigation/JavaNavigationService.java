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

/**
 * @author Evgen Vidolob
 */
public interface JavaNavigationService {

    /**
     * Find declaration of the binding key
     * @param projectPath path to the project
     * @param keyBinding binding key
     * @param callback
     */
    void findDeclaration(String projectPath, String fqn, int offset, AsyncRequestCallback<OpenDeclarationDescriptor> callback);


    /**
     * Receive all jar dependency's
     * @param projectPath path to the project
     * @param callback
     */
    void getExternalLibraries(String projectPath, AsyncRequestCallback<Array<Jar>> callback);

    void getLibraryChildren(String projectPath, int libId, AsyncRequestCallback<Array<JarEntry>> callback);

    void getChildren(String projectPath, int libId, String path, AsyncRequestCallback<Array<JarEntry>> callback);

    void getEntry(String projectPath, int libId, String path, AsyncRequestCallback<JarEntry> callback);

    void getContent(String projectPath, int libId, String path, AsyncRequestCallback<String> callback);

    /**
     * @param projectPath
     * @param libId
     * @param path
     * @return
     */
    String getContentUrl(String projectPath, int libId, String path);
}
