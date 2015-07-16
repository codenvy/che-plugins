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

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.vfs.server.util.DeleteOnCloseFileInputStream;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.jdt.internal.core.JavaProject;
import org.eclipse.che.jdt.internal.core.SearchableEnvironment;
import org.eclipse.che.jdt.internal.core.SourceTypeElementInfo;
import org.eclipse.che.vfs.impl.fs.LocalFSMountStrategy;
import com.google.inject.name.Named;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CodenvyCompilationUnitResolver;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rest service for WorkerNameEnvironment
 * The name environment provides a callback API that the compiler can use to look up types, compilation units, and packages in the
 * current environment
 *
 * @author Evgen Vidolob
 */
@javax.ws.rs.Path("java-name-environment/{ws-id}")
public class RestNameEnvironment {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(RestNameEnvironment.class);

    @Inject
    private LocalFSMountStrategy fsMountStrategy;

    @Inject
    private JavaProjectService javaProjectService;

    @Context
    private HttpServletRequest request;

    @Inject
    @Named("che.java.codeassistant.index.dir")
    private String temp;

    @PathParam("ws-id")
    @Inject
    private String wsId;

    @Inject
    @Named("api.endpoint")
    private String apiUrl;

    private static String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("findTypeCompound")
    public String findTypeCompound(@QueryParam("compoundTypeName") String compoundTypeName, @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        try {
            NameEnvironmentAnswer answer = environment.findType(getCharArrayFrom(compoundTypeName));
            if (answer == null && compoundTypeName.contains("$")) {
                String innerName = compoundTypeName.substring(compoundTypeName.indexOf('$') + 1, compoundTypeName.length());
                compoundTypeName = compoundTypeName.substring(0, compoundTypeName.indexOf('$'));
                answer = environment.findType(getCharArrayFrom(compoundTypeName));
                if (!answer.isCompilationUnit()) return null;
                ICompilationUnit compilationUnit = answer.getCompilationUnit();
                CompilationUnit result = getCompilationUnit(javaProject, environment, compilationUnit);
                AbstractTypeDeclaration o = (AbstractTypeDeclaration)result.types().get(0);
                ITypeBinding typeBinding = o.resolveBinding();

                for (ITypeBinding binding : typeBinding.getDeclaredTypes()) {
                    if (binding.getBinaryName().endsWith(innerName)) {
                        typeBinding = binding;
                        break;
                    }
                }
                Map<TypeBinding, ?> bindings = (Map<TypeBinding, ?>)result.getProperty("compilerBindingsToASTBindings");
                SourceTypeBinding binding = null;
                for (Map.Entry<TypeBinding, ?> entry : bindings.entrySet()) {
                    if (entry.getValue().equals(typeBinding)) {
                        binding = (SourceTypeBinding)entry.getKey();
                        break;
                    }
                }
                return TypeBindingConvector.toJsonBinaryType(binding);
            }

            return processAnswer(answer, javaProject, environment);
        } catch (JavaModelException e) {
            LOG.debug("Can't parse class: ", e);
            throw new WebApplicationException();
        }
    }

    private JavaProject getJavaProject(String projectPath) {
        return javaProjectService.getOrCreateJavaProject(wsId, projectPath);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("findType")
    public String findType(@QueryParam("typename") String typeName, @QueryParam("packagename") String packageName,
                           @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();

        NameEnvironmentAnswer answer = environment.findType(typeName.toCharArray(), getCharArrayFrom(packageName));
        try {
            return processAnswer(answer, javaProject, environment);
        } catch (JavaModelException e) {
           LOG.debug("Can't parse class: ", e);
           throw new WebApplicationException(e);
        }
    }

    @GET
    @javax.ws.rs.Path("package")
    @Produces("text/plain")
    public String isPackage(@QueryParam("packagename") String packageName, @QueryParam("parent") String parentPackageName,
                            @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        return String.valueOf(environment.isPackage(getCharArrayFrom(parentPackageName), packageName.toCharArray()));
    }

    @GET
    @Path("findPackages")
    @Produces(MediaType.APPLICATION_JSON)
    public String findPackages(@QueryParam("packagename") String packageName, @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        JsonSearchRequester requestor = new JsonSearchRequester();
        environment.findPackages(packageName.toCharArray(), requestor);
        return requestor.toJsonString();
    }

    @GET
    @javax.ws.rs.Path("findConstructor")
    @Produces(MediaType.APPLICATION_JSON)
    public String findConstructorDeclarations(@QueryParam("prefix") String prefix,
                                              @QueryParam("camelcase") boolean camelCaseMatch,
                                              @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        JsonSearchRequester searchRequester = new JsonSearchRequester();
        environment.findConstructorDeclarations(prefix.toCharArray(), camelCaseMatch, searchRequester, null);
        return searchRequester.toJsonString();
    }

    @GET
    @javax.ws.rs.Path("findTypes")
    @Produces(MediaType.APPLICATION_JSON)
    public String findTypes(@QueryParam("qualifiedname") String qualifiedName, @QueryParam("findmembers") boolean findMembers,
                            @QueryParam("camelcase") boolean camelCaseMatch,
                            @QueryParam("searchfor") int searchFor,
                            @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        JsonSearchRequester searchRequester = new JsonSearchRequester();
        environment.findTypes(qualifiedName.toCharArray(), findMembers, camelCaseMatch, searchFor, searchRequester);
        return searchRequester.toJsonString();
    }

    @GET
    @javax.ws.rs.Path("findExactTypes")
    @Produces(MediaType.APPLICATION_JSON)
    public String findExactTypes(@QueryParam("missingsimplename") String missingSimpleName, @QueryParam("findmembers") boolean findMembers,
                                 @QueryParam("searchfor") int searchFor,
                                 @QueryParam("projectpath") String projectPath) {
        JavaProject javaProject = getJavaProject(projectPath);
        SearchableEnvironment environment = javaProject.getNameEnvironment();
        JsonSearchRequester searchRequester = new JsonSearchRequester();
        environment.findExactTypes(missingSimpleName.toCharArray(), findMembers, searchFor, searchRequester);
        return searchRequester.toJsonString();
    }

    @GET
    @javax.ws.rs.Path("/update-dependencies-launch-task")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor updateDependency(@QueryParam("projectpath") String projectPath, @QueryParam("force") boolean force,
                                                @Context UriInfo uriInfo) throws Exception {

        //project already has updated dependency's, so skip build
        if (javaProjectService.isProjectDependencyExist(wsId, projectPath) && !force) {
            BuildTaskDescriptor descriptor = DtoFactory.getInstance().createDto(BuildTaskDescriptor.class);
            descriptor.setStatus(BuildStatus.SUCCESSFUL);
            return descriptor;
        }
        File workspace = fsMountStrategy.getMountPath(wsId);
        File project = new File(workspace, projectPath);
        if (!project.exists()) {
            LOG.warn("Project doesn't exist in workspace: " + wsId + ", path: " + projectPath);
            throw new CodeAssistantException(500, "Project doesn't exist");
        }

        String url = apiUrl + "/builder/" + wsId + "/dependencies";
        return getDependencies(url, projectPath, "copy", null);
    }

    /** Get list of all package names in project */
    @POST
    @javax.ws.rs.Path("/update-dependencies-wait-build-end")
    @Produces(MediaType.APPLICATION_JSON)
    public void waitUpdateDependencyBuildEnd(@QueryParam("projectpath") String projectPath,
                                             BuildTaskDescriptor descriptor,
                                             @Context UriInfo uriInfo) throws Exception {
        // call to wait-for-build-finish method
        try {
            BuildTaskDescriptor finishedBuildStatus = waitTaskFinish(descriptor);
            if (finishedBuildStatus.getStatus() == BuildStatus.FAILED) {
                buildFailed(finishedBuildStatus);
            }
            javaProjectService.removeProject(wsId, projectPath);

            File projectDepDir = new File(temp, wsId + projectPath);
            projectDepDir.mkdirs();

            Link downloadLink = findLink("download result", finishedBuildStatus.getLinks());
            if (downloadLink != null) {
                File zip = doDownload(downloadLink.getHref(), projectPath, "dependencies.zip");
                ZipUtils.unzip(new DeleteOnCloseFileInputStream(zip), projectDepDir);
            }

            BuildOptions buildOptions = DtoFactory.getInstance().createDto(BuildOptions.class);
            buildOptions.getOptions().put("-Dclassifier", "sources");
            String url = apiUrl + "/builder/" + wsId + "/dependencies";
            BuildTaskDescriptor dependencies = getDependencies(url, projectPath, "copy", buildOptions);
            BuildTaskDescriptor buildTaskDescriptor = waitTaskFinish(dependencies);
            if (finishedBuildStatus.getStatus() == BuildStatus.FAILED) {
                buildFailed(finishedBuildStatus);
            }
            File projectSourcesJars = new File(projectDepDir, "sources");
            projectSourcesJars.mkdirs();
            downloadLink = findLink("download result", buildTaskDescriptor.getLinks());
            if (downloadLink != null) {
                File zip = doDownload(downloadLink.getHref(), projectPath, "sources.zip");
                ZipUtils.unzip(new DeleteOnCloseFileInputStream(zip), projectSourcesJars);
            }
            //create JavaProject adn put it into cache
            javaProjectService.getOrCreateJavaProject(wsId, projectPath);

        } catch (Throwable debug) {
            LOG.warn("RestNameEnvironment", debug);
            throw new WebApplicationException(debug);
        }
    }

    private File doDownload(String downloadURL, String projectPath, String zipName) throws IOException {
        HttpURLConnection http = null;
        HttpStream stream = null;
        try {
            URI uri = UriBuilder.fromUri(downloadURL).queryParam("token", getAuthenticationToken()).build();
            http = (HttpURLConnection)uri.toURL().openConnection();
            http.setRequestMethod("GET");
            int responseCode = http.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Your project referenced a zipped dependency that cannot be downloaded.");
            }
            // Connection closed automatically when input stream closed.
            // If IOException or BuilderException occurs then connection closed immediately.
            stream = new HttpStream(http);
            java.nio.file.Path path = Paths.get(temp, wsId + projectPath + "/" + zipName);
            Files.copy(stream, path);
            return path.toFile();
        } catch (MalformedURLException e) {
            throw e;
        } catch (IOException ioe) {
            if (http != null) {
                http.disconnect();
            }
            throw ioe;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

    }

    private void buildFailed(@Nullable BuildTaskDescriptor buildStatus) throws BuilderException {
        if (buildStatus != null) {
            Link logLink = findLink("view build log", buildStatus.getLinks());
            LOG.warn("Build failed see more detail here: " + logLink.getHref());
            throw new BuilderException(
                    "Build failed see more detail here: <a href=\"" + logLink.getHref() + "\" target=\"_blank\">" + logLink.getHref() +
                    "</a>."
            );
        }
        throw new BuilderException("Build failed");
    }

    @Nullable
    private Link findLink(@Nonnull String rel, List<Link> links) {
        for (Link link : links) {
            if (link.getRel().equals(rel)) {
                return link;
            }
        }
        return null;
    }

    @Nonnull
    private BuildTaskDescriptor waitTaskFinish(@Nonnull BuildTaskDescriptor buildDescription) throws Exception {
        BuildTaskDescriptor request = buildDescription;
        final int sleepTime = 500;

        Link statusLink = findLink("get status", buildDescription.getLinks());

        if (statusLink != null) {
            while (request.getStatus() == BuildStatus.IN_PROGRESS || request.getStatus() == BuildStatus.IN_QUEUE) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
                request = HttpJsonHelper.request(BuildTaskDescriptor.class, statusLink);
            }
        }

        return request;
    }


    @Nonnull
    private BuildTaskDescriptor getDependencies(@Nonnull String url, @Nonnull String projectName, @Nonnull String analyzeType, @Nullable
    BuildOptions options)
            throws Exception {
        Pair<String, String> projectParam = Pair.of("project", projectName);
        Pair<String, String> typeParam = Pair.of("type", analyzeType);
        return HttpJsonHelper.request(BuildTaskDescriptor.class, url, "POST", options, projectParam, typeParam);
    }


    private String processAnswer(NameEnvironmentAnswer answer, IJavaProject project, INameEnvironment environment)
            throws JavaModelException {
        if (answer == null) return null;
        if (answer.isBinaryType()) {
            IBinaryType binaryType = answer.getBinaryType();
            return BinaryTypeConvector.toJsonBinaryType(binaryType);
        } else if (answer.isCompilationUnit()) {
            ICompilationUnit compilationUnit = answer.getCompilationUnit();
            return getSourceTypeInfo(project, environment, compilationUnit);
        } else if (answer.isSourceType()) {
            ISourceType[] sourceTypes = answer.getSourceTypes();
            if (sourceTypes.length == 1) {
                ISourceType sourceType = sourceTypes[0];
                SourceTypeElementInfo elementInfo = (SourceTypeElementInfo)sourceType;
                IType handle = elementInfo.getHandle();
                org.eclipse.jdt.core.ICompilationUnit unit = handle.getCompilationUnit();
                return getSourceTypeInfo(project, environment, (ICompilationUnit)unit);
            }
        }
        return null;
    }

    private String getSourceTypeInfo(IJavaProject project, INameEnvironment environment, ICompilationUnit compilationUnit)
            throws JavaModelException {
        CompilationUnit result = getCompilationUnit(project, environment, compilationUnit);

        BindingASTVisitor visitor = new BindingASTVisitor();
        result.accept(visitor);
        Map<TypeBinding, ?> bindings = (Map<TypeBinding, ?>)result.getProperty("compilerBindingsToASTBindings");
        SourceTypeBinding binding = null;
        for (Map.Entry<TypeBinding, ?> entry : bindings.entrySet()) {
            if (entry.getValue().equals(visitor.typeBinding)) {
                binding = (SourceTypeBinding)entry.getKey();
                break;
            }
        }
        if (binding == null) return null;
        return TypeBindingConvector.toJsonBinaryType(binding);
    }

    private CompilationUnit getCompilationUnit(IJavaProject project, INameEnvironment environment,
                                               ICompilationUnit compilationUnit) throws JavaModelException {
        int flags = 0;
        flags |= org.eclipse.jdt.core.ICompilationUnit.ENABLE_STATEMENTS_RECOVERY;
        flags |= org.eclipse.jdt.core.ICompilationUnit.IGNORE_METHOD_BODIES;
        flags |= org.eclipse.jdt.core.ICompilationUnit.ENABLE_BINDINGS_RECOVERY;
        HashMap<String, String> opts = new HashMap<>(javaProjectService.getOptions());
        CompilationUnitDeclaration compilationUnitDeclaration =
                CodenvyCompilationUnitResolver.resolve(compilationUnit, project, environment, opts, flags, null);
        return CodenvyCompilationUnitResolver.convert(
                compilationUnitDeclaration,
                compilationUnit.getContents(),
                flags, opts);
    }

    private char[][] getCharArrayFrom(String list) {
        if(list.isEmpty()){
            return null;
        }
        String[] strings = list.split(",");
        char[][] arr = new char[strings.length][];
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            arr[i] = s.toCharArray();
        }
        return arr;
    }

    /** Stream that automatically close HTTP connection when all data ends. */
    private static class HttpStream extends FilterInputStream {
        private final HttpURLConnection http;

        private boolean closed;

        private HttpStream(HttpURLConnection http) throws IOException {
            super(http.getInputStream());
            this.http = http;
        }

        @Override
        public int read() throws IOException {
            int r = super.read();
            if (r == -1) {
                close();
            }
            return r;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int r = super.read(b);
            if (r == -1) {
                close();
            }
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = super.read(b, off, len);
            if (r == -1) {
                close();
            }
            return r;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            try {
                super.close();
            } finally {
                http.disconnect();
                closed = true;
            }
        }
    }

}
