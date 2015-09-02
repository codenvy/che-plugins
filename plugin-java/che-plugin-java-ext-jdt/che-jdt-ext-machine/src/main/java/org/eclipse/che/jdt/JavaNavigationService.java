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
package org.eclipse.che.jdt;

import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.ext.java.shared.OpenDeclarationDescriptor;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Evgen Vidolob
 */
@Path("jdt/{wsId}/navigation")
public class JavaNavigationService {
    JavaModel MODEL = JavaModelManager.getJavaModelManager().getJavaModel();

    @Inject
    private JavaNavigation navigation;

    @GET
    @Path("find-declaration")
    @Produces("application/json")
    public OpenDeclarationDescriptor findDeclaration(@QueryParam("projectpath") String projectPath, @QueryParam("fqn") String fqn,
                                                     @QueryParam("offset") int offset)
            throws JavaModelException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        return navigation.findDeclaration(project, fqn, offset);
    }

    @GET
    @Path("libraries")
    @Produces("application/json")
    public List<Jar> getExternalLibraries(@QueryParam("projectpath") String projectPath) throws JavaModelException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        return navigation.getProjectDependecyJars(project);
    }

    @GET
    @Path("lib/children")
    @Produces("application/json")
    public List<JarEntry> getLibraryChildren(@QueryParam("projectpath") String projectPath, @QueryParam("root") int rootId)
            throws JavaModelException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        return navigation.getPackageFragmentRootContent(project, rootId);
    }

    @GET
    @Path("children")
    @Produces("application/json")
    public List<JarEntry> getChildren(@QueryParam("projectpath") String projectPath, @QueryParam("path") String path,
                                      @QueryParam("root") int rootId) throws JavaModelException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        return navigation.getChildren(project, rootId, path);
    }

    @GET
    @Path("content")
    public Response getContent(@QueryParam("projectpath") String projectPath, @QueryParam("path") String path,
                               @QueryParam("root") int rootId) throws CoreException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        String content = navigation.getContent(project, rootId, path);
        return Response.ok().entity(content).build();
    }

    @GET
    @Path("entry")
    public JarEntry getEntry(@QueryParam("projectpath") String projectPath, @QueryParam("path") String path,
                               @QueryParam("root") int rootId) throws CoreException {
        IJavaProject project = MODEL.getJavaProject(projectPath);
        return navigation.getEntry(project, rootId, path);
    }

    @GET
    @Path("get/projects/and/packages")
    @Produces("application/json")
    public List<JavaProject> getProjectsAndPackages(@QueryParam("includepackages") boolean includePackages) throws JavaModelException {
        return navigation.getAllProjectsAndPackages(includePackages);
    }
}
