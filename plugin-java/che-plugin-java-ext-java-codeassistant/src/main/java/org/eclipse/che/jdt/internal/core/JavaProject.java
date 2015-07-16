/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.jdt.internal.core;

import org.eclipse.che.api.project.server.Constants;
import org.eclipse.che.api.project.server.ProjectJson;
import org.eclipse.che.api.project.shared.Builders;
import org.eclipse.che.ide.ant.tools.AntUtils;
import org.eclipse.che.jdt.core.JavaCore;
import org.eclipse.che.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.che.jdt.internal.core.util.JavaElementFinder;
import org.eclipse.che.ide.maven.tools.MavenUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.eval.IEvaluationContext;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JavaProject extends Openable implements IJavaProject {

    /**
     * Value of the project's raw classpath if the .classpath file contains invalid entries.
     */
    public static final  IClasspathEntry[]                          INVALID_CLASSPATH = new IClasspathEntry[0];
    private static final Logger                                     LOG               = LoggerFactory.getLogger(JavaProject.class);
    private final        DirectoryStream.Filter<java.nio.file.Path> jarFilter         = new DirectoryStream.Filter<java.nio.file.Path>() {
        @Override
        public boolean accept(java.nio.file.Path entry) throws IOException {
            return entry.getFileName().toString().endsWith("jar");
        }
    };
    private final String workspacePath;
    private volatile SearchableEnvironment nameEnvironment;
    private String                    projectPath;
    private String                    tempDir;
    private String                    wsId;
    private String                    projectName;
    private File                      projectDir;
    private Map<String, String>       options;
    private IClasspathEntry[]         rawClassPath;
    private ResolvedClasspath         resolvedClasspath;
    private IndexManager              indexManager;

    public JavaProject(File root, String projectPath, String tempDir, String ws, Map<String, String> options) {
        super(null, new JavaModelManager());
        manager.setJavaProject(this);
        this.projectPath = projectPath;
        this.tempDir = tempDir;
        wsId = ws;
        workspacePath = root.getPath();
        int index = projectPath.lastIndexOf('/');
        projectName = index < 0 ? projectPath : projectPath.substring(index + 1);
        this.projectDir = new File(root, projectPath);
        this.options = options;
        List<IClasspathEntry> paths = new LinkedList<>();
        try {
            if (index <= 0) {
                // project in root folder
                addSources(projectDir, paths);
            } else {
                // module of project - add this module and all modules in parent projects
                index = projectPath.indexOf('/', 1);
                File parent = new File(root, projectPath.substring(1, index));
                LinkedList<File> q = new LinkedList<>();
                q.add(parent);
                while (!q.isEmpty()) {
                    File f = q.poll();
                    addSources(f, paths);
                    File[] l = f.listFiles();
                    for (File c : l) {
                        if (c.isDirectory()
                            && new File(c, Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH).exists()) {

                            q.add(c);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Can't find sources folder attribute", e);
        }

        paths.add(JavaCore.newContainerEntry(new Path("codenvy:Jre")));
        File depDir = new File(tempDir, wsId + projectPath);
        try {
            if (depDir.exists()) {
                try (DirectoryStream<java.nio.file.Path> deps =
                             Files.newDirectoryStream(depDir.toPath(), jarFilter)) {

                    for (java.nio.file.Path dep : deps) {
                        String name = dep.getFileName().toString();
                        File srcJar =
                                new File(dep.getParent().toFile(), "sources/" + name.substring(0, name.lastIndexOf('.')) + "-sources.jar");
                        IPath srcPath = null;
                        if (srcJar.exists()) {
                            srcPath = new Path(srcJar.getAbsolutePath());
                        }
                        paths.add(JavaCore.newLibraryEntry(new Path(dep.toAbsolutePath().toString()), srcPath, null));
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Can't find jar dependency's: ", e);
        }
        rawClassPath = paths.toArray(new IClasspathEntry[paths.size()]);
        indexManager = new IndexManager(tempDir + "/indexes/" + ws + projectPath + "/", this);
        indexManager.reset();
        indexManager.indexAll(this);
        indexManager.saveIndexes();
        manager.setIndexManager(indexManager);
        creteNewNameEnvironment();
    }

    /**
     * Returns true if the given project is accessible and it has
     * a java nature, otherwise false.
     *
     * @param project
     *         IProject
     * @return boolean
     */
    public static boolean hasJavaNature(IProject project) {
//        try {
//            return project.hasNature(JavaCore.NATURE_ID);
//        } catch (CoreException e) {
//            if (ExternalJavaProject.EXTERNAL_PROJECT_NAME.equals(project.getName()))
//                return true;
//            // project does not exist or is not open
//        }
//        return false;
        return true;
    }

    public SearchableEnvironment getNameEnvironment() {
        return nameEnvironment;
    }

    private void addSources(File projectDir, List<IClasspathEntry> paths) throws IOException {
        File codenvy = new File(projectDir, Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        List<File> sources = new LinkedList<>();

        final ProjectJson projectJson;
        try (FileInputStream in = new FileInputStream(codenvy)) {
            projectJson = ProjectJson.load(in);
        }

        Builders defBuilder = projectJson.getBuilders();

        if (defBuilder != null) {
            if ("maven".equals(defBuilder.getDefault())) {
                File pom = new File(projectDir, "pom.xml");
                if (pom.exists()) {
                    for (String src : MavenUtils.getSourceDirectories(pom)) {
                        sources.add(new File(projectDir, src));
                    }
                }
            } else if ("ant".equals(defBuilder.getDefault())) {
                File build = new File(projectDir, "build.xml");
                if (build.exists()) {
                    for (String src : AntUtils.getSourceDirectories(build)) {
                        sources.add(new File(projectDir, src));
                    }
                }
            }
        }

        if (sources.isEmpty()) {
            sources.add(projectDir);
        }

        for (File source : sources) {
            if (source.exists()) {
                paths.add(JavaCore.newSourceEntry(new Path(source.getAbsolutePath())));
            }
        }
    }

    /**
     * Returns the raw classpath for the project, as a list of classpath
     * entries. This corresponds to the exact set of entries which were assigned
     * using <code>setRawClasspath</code>, in particular such a classpath may
     * contain classpath variable and classpath container entries. Classpath
     * variable and classpath container entries can be resolved using the
     * helper method <code>getResolvedClasspath</code>; classpath variable
     * entries also can be resolved individually using
     * <code>JavaCore#getClasspathVariable</code>).
     * <p>
     * Both classpath containers and classpath variables provides a level of
     * indirection that can make the <code>.classpath</code> file stable across
     * workspaces.
     * As an example, classpath variables allow a classpath to no longer refer
     * directly to external JARs located in some user specific location.
     * The classpath can simply refer to some variables defining the proper
     * locations of these external JARs. Similarly, classpath containers
     * allows classpath entries to be computed dynamically by the plug-in that
     * defines that kind of classpath container.
     * </p>
     * <p>
     * Note that in case the project isn't yet opened, the classpath will
     * be read directly from the associated <tt>.classpath</tt> file.
     * </p>
     *
     * @return the raw classpath for the project, as a list of classpath entries
     * @throws org.eclipse.jdt.core.JavaModelException
     *         if this element does not exist or if an
     *         exception occurs while accessing its corresponding resource
     * @see org.eclipse.jdt.core.IClasspathEntry
     */
    public IClasspathEntry[] getRawClasspath() throws JavaModelException {
        return rawClassPath;
    }

    @Override
    public String[] getRequiredProjectNames() throws JavaModelException {
        return new String[0];
    }

    @Override
    public IClasspathEntry[] getResolvedClasspath(boolean ignoreUnresolvedEntry) throws JavaModelException {
        return new IClasspathEntry[0];
    }

    @Override
    public boolean hasBuildState() {
        return false;
    }

    @Override
    public boolean hasClasspathCycle(IClasspathEntry[] entries) {
        return false;
    }

    @Override
    public boolean isOnClasspath(IJavaElement element) {
        return false;
    }

    @Override
    public boolean isOnClasspath(IResource resource) {
        return false;
    }

    @Override
    public IEvaluationContext newEvaluationContext() {
        return null;
    }

    @Override
    public ITypeHierarchy newTypeHierarchy(IRegion region, IProgressMonitor monitor) throws JavaModelException {
        return null;
    }

    @Override
    public ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
        return null;
    }

    @Override
    public ITypeHierarchy newTypeHierarchy(IType type, IRegion region, IProgressMonitor monitor) throws JavaModelException {
        return null;
    }

    @Override
    public ITypeHierarchy newTypeHierarchy(IType type, IRegion region, WorkingCopyOwner owner, IProgressMonitor monitor)
            throws JavaModelException {
        return null;
    }

    @Override
    public IPath readOutputLocation() {
        return null;
    }

    @Override
    public IClasspathEntry[] readRawClasspath() {
        return new IClasspathEntry[0];
    }

    @Override
    public void setOption(String optionName, String optionValue) {

    }

    @Override
    public void setOptions(Map newOptions) {

    }

    @Override
    public void setOutputLocation(IPath path, IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation, boolean canModifyResources, IProgressMonitor monitor)
            throws JavaModelException {

    }

    @Override
    public void setRawClasspath(IClasspathEntry[] entries, boolean canModifyResources, IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public void setRawClasspath(IClasspathEntry[] entries, IClasspathEntry[] referencedEntries, IPath outputLocation,
                                IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public IClasspathEntry[] getReferencedClasspathEntries() throws JavaModelException {
        return new IClasspathEntry[0];
    }

    @Override
    public void setRawClasspath(IClasspathEntry[] entries, IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation, IProgressMonitor monitor) throws JavaModelException {

    }

    /**
     * This is a helper method returning the resolved classpath for the project
     * as a list of simple (non-variable, non-container) classpath entries.
     * All classpath variable and classpath container entries in the project's
     * raw classpath will be replaced by the simple classpath entries they
     * resolve to.
     * <p>
     * The resulting resolved classpath is accurate for the given point in time.
     * If the project's raw classpath is later modified, or if classpath
     * variables are changed, the resolved classpath can become out of date.
     * Because of this, hanging on resolved classpath is not recommended.
     * </p>
     * <p>
     * Note that if the resolution creates duplicate entries
     * (i.e. {@link IClasspathEntry entries} which are {@link Object#equals(Object)}),
     * only the first one is added to the resolved classpath.
     * </p>
     *
     * @see IClasspathEntry
     */
    public IClasspathEntry[] getResolvedClasspath() throws JavaModelException {
        if (resolvedClasspath == null) {
            ResolvedClasspath result = new ResolvedClasspath();
            LinkedHashSet<IClasspathEntry> resolvedEntries = new LinkedHashSet<>();
            for (IClasspathEntry entry : getRawClasspath()) {
                switch (entry.getEntryKind()) {
                    case IClasspathEntry.CPE_LIBRARY:
                    case IClasspathEntry.CPE_SOURCE:
                        addToResult(entry, entry, result, resolvedEntries);
                        break;
                    case IClasspathEntry.CPE_CONTAINER:
                        IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), this);
                        for (IClasspathEntry classpathEntry : container.getClasspathEntries()) {
                            addToResult(entry, classpathEntry, result, resolvedEntries);
                        }
                        break;
                }
            }
            result.resolvedClasspath = new IClasspathEntry[resolvedEntries.size()];
            resolvedEntries.toArray(result.resolvedClasspath);
            resolvedClasspath = result;
        }

        return resolvedClasspath.resolvedClasspath;
    }

    public ResolvedClasspath resolvedClasspath() {
        return resolvedClasspath;
    }

    /**
     * Returns the classpath entry that refers to the given path
     * or <code>null</code> if there is no reference to the path.
     *
     * @param path
     *         IPath
     * @return IClasspathEntry
     * @throws JavaModelException
     */
    public IClasspathEntry getClasspathEntryFor(IPath path) throws JavaModelException {
        getResolvedClasspath(); // force resolution
        if (resolvedClasspath == null) {
            return null;
        }
        Map rootPathToResolvedEntries = resolvedClasspath.rootPathToResolvedEntries;
        if (rootPathToResolvedEntries == null)
            return null;
        IClasspathEntry classpathEntry = (IClasspathEntry)rootPathToResolvedEntries.get(path);
//        if (classpathEntry == null) {
//            path = getProject().getWorkspace().getRoot().getLocation().append(path);
//            classpathEntry = (IClasspathEntry)rootPathToResolvedEntries.get(path);
//        }
        return classpathEntry;
    }

    public String getName() {
        return projectName;
    }

    public IPath getFullPath() {
        return new Path(projectDir.getPath());
    }

    @Override
    public IClasspathEntry decodeClasspathEntry(String encodedEntry) {
        return null;
    }

    @Override
    public String encodeClasspathEntry(IClasspathEntry classpathEntry) {
        return null;
    }

    @Override
    public IJavaElement findElement(IPath path) throws JavaModelException {
        return null;
    }

    @Override
    public IJavaElement findElement(IPath path, WorkingCopyOwner owner) throws JavaModelException {
        return null;
    }

    @Override
    public IJavaElement findElement(String bindingKey, WorkingCopyOwner owner) throws JavaModelException {
        JavaElementFinder elementFinder = new JavaElementFinder(bindingKey, this, owner);
        elementFinder.parse();
        if (elementFinder.exception != null)
            throw elementFinder.exception;
        return elementFinder.element;
    }

    public IJavaElement findPackageFragment(String packageName)
            throws JavaModelException {
        NameLookup lookup = newNameLookup((WorkingCopyOwner)null/*no need to look at working copies for pkgs*/);
        IPackageFragment[] pkgFragments = lookup.findPackageFragments(packageName, false);
        if (pkgFragments == null) {
            return null;

        } else {
            // try to return one that is a child of this project
            for (int i = 0, length = pkgFragments.length; i < length; i++) {

                IPackageFragment pkgFragment = pkgFragments[i];
                if (equals(pkgFragment.getParent().getParent())) {
                    return pkgFragment;
                }
            }
            // default to the first one
            return pkgFragments[0];
        }
    }

    @Override
    public IPackageFragment findPackageFragment(IPath path) throws JavaModelException {
        return null;
    }

    @Override
    public IPackageFragmentRoot findPackageFragmentRoot(IPath path) throws JavaModelException {
        return null;
    }

    @Override
    public IPackageFragmentRoot[] findPackageFragmentRoots(IClasspathEntry entry) {
        return new IPackageFragmentRoot[0];
    }

    @Override
    public IType findType(String fullyQualifiedName) throws JavaModelException {
        return findType(fullyQualifiedName, DefaultWorkingCopyOwner.PRIMARY);
    }

    @Override
    public IType findType(String fullyQualifiedName, IProgressMonitor progressMonitor) throws JavaModelException {
        return findType(fullyQualifiedName, DefaultWorkingCopyOwner.PRIMARY, progressMonitor);
    }

    /*
 * Internal findType with instanciated name lookup
 */
    IType findType(String fullyQualifiedName, NameLookup lookup, boolean considerSecondaryTypes, IProgressMonitor progressMonitor)
            throws JavaModelException {
        NameLookup.Answer answer = lookup.findType(
                fullyQualifiedName,
                false,
                org.eclipse.jdt.internal.core.NameLookup.ACCEPT_ALL,
                considerSecondaryTypes,
                true, /* wait for indexes (only if consider secondary types)*/
                false/*don't check restrictions*/,
                progressMonitor);
        if (answer == null) {
            // try to find enclosing type
            int lastDot = fullyQualifiedName.lastIndexOf('.');
            if (lastDot == -1) return null;
            IType type = findType(fullyQualifiedName.substring(0, lastDot), lookup, considerSecondaryTypes, progressMonitor);
            if (type != null) {
                type = type.getType(fullyQualifiedName.substring(lastDot + 1));
                if (!type.exists()) {
                    return null;
                }
            }
            return type;
        }
        return answer.type;
    }

    @Override
    public IType findType(String fullyQualifiedName, WorkingCopyOwner owner) throws JavaModelException {
        NameLookup lookup = newNameLookup(owner);
        return findType(fullyQualifiedName, lookup, false, null);
    }

    @Override
    public IType findType(String fullyQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor) throws JavaModelException {
        NameLookup lookup = newNameLookup(owner);
        return findType(fullyQualifiedName, lookup, true, progressMonitor);
    }

    @Override
    public IType findType(String packageName, String typeQualifiedName) throws JavaModelException {
        return findType(packageName, typeQualifiedName, DefaultWorkingCopyOwner.PRIMARY);
    }

    @Override
    public IType findType(String packageName, String typeQualifiedName, IProgressMonitor progressMonitor) throws JavaModelException {
        return findType(packageName, typeQualifiedName, DefaultWorkingCopyOwner.PRIMARY, progressMonitor);
    }

    @Override
    public IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner) throws JavaModelException {
        NameLookup lookup = newNameLookup(owner);
        return findType(
                packageName,
                typeQualifiedName,
                lookup,
                false, // do not consider secondary types
                null);
    }

    @Override
    public IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner, IProgressMonitor progressMonitor)
            throws JavaModelException {
        NameLookup lookup = newNameLookup(owner);
        return findType(
                packageName,
                typeQualifiedName,
                lookup,
                true, // consider secondary types
                progressMonitor);
    }

    /*
 * Internal findType with instanciated name lookup
 */
    IType findType(String packageName, String typeQualifiedName, NameLookup lookup, boolean considerSecondaryTypes,
                   IProgressMonitor progressMonitor) throws JavaModelException {
        NameLookup.Answer answer = lookup.findType(
                typeQualifiedName,
                packageName,
                false,
                NameLookup.ACCEPT_ALL,
                considerSecondaryTypes,
                true, // wait for indexes (in case we need to consider secondary types)
                false/*don't check restrictions*/,
                progressMonitor);
        return answer == null ? null : answer.type;
    }

    /*
 * Returns a new name lookup. This name lookup first looks in the working copies of the given owner.
 */
    public NameLookup newNameLookup(WorkingCopyOwner owner) throws JavaModelException {

//        JavaModelManager manager = org.eclipse.jdt.internal.core.JavaModelManager.getJavaModelManager();
        ICompilationUnit[] workingCopies = owner == null ? null : manager.getWorkingCopies(owner, true/*add primary WCs*/);
        return newNameLookup(workingCopies);
    }

    @Override
    public IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaModelException {
        return getAllPackageFragmentRoots(null /*no reverse map*/);
    }

    public IPackageFragmentRoot[] getAllPackageFragmentRoots(Map rootToResolvedEntries) throws JavaModelException {

        return computePackageFragmentRoots(getResolvedClasspath(), true/*retrieveExportedRoots*/, rootToResolvedEntries);
    }

    /**
     * Returns (local/all) the package fragment roots identified by the given project's classpath.
     * Note: this follows project classpath references to find required project contributions,
     * eliminating duplicates silently.
     * Only works with resolved entries
     *
     * @param resolvedClasspath
     *         IClasspathEntry[]
     * @param retrieveExportedRoots
     *         boolean
     * @return IPackageFragmentRoot[]
     * @throws JavaModelException
     */
    public IPackageFragmentRoot[] computePackageFragmentRoots(
            IClasspathEntry[] resolvedClasspath,
            boolean retrieveExportedRoots,
            Map rootToResolvedEntries) throws JavaModelException {

        ObjectVector accumulatedRoots = new ObjectVector();
        computePackageFragmentRoots(
                resolvedClasspath,
                accumulatedRoots,
                new HashSet(5), // rootIDs
                null, // inside original project
                retrieveExportedRoots,
                rootToResolvedEntries);
        IPackageFragmentRoot[] rootArray = new IPackageFragmentRoot[accumulatedRoots.size()];
        accumulatedRoots.copyInto(rootArray);
        return rootArray;
    }

    /**
     * Returns (local/all) the package fragment roots identified by the given project's classpath.
     * Note: this follows project classpath references to find required project contributions,
     * eliminating duplicates silently.
     * Only works with resolved entries
     *
     * @param resolvedClasspath
     *         IClasspathEntry[]
     * @param accumulatedRoots
     *         ObjectVector
     * @param rootIDs
     *         HashSet
     * @param referringEntry
     *         project entry referring to this CP or null if initial project
     * @param retrieveExportedRoots
     *         boolean
     * @throws JavaModelException
     */
    public void computePackageFragmentRoots(
            IClasspathEntry[] resolvedClasspath,
            ObjectVector accumulatedRoots,
            HashSet rootIDs,
            IClasspathEntry referringEntry,
            boolean retrieveExportedRoots,
            Map rootToResolvedEntries) throws JavaModelException {

        if (referringEntry == null) {
            rootIDs.add(rootID());
        }
        for (int i = 0, length = resolvedClasspath.length; i < length; i++) {
            computePackageFragmentRoots(
                    resolvedClasspath[i],
                    accumulatedRoots,
                    rootIDs,
                    referringEntry,
                    retrieveExportedRoots,
                    rootToResolvedEntries);
        }
    }

    /**
     * Returns the package fragment roots identified by the given entry. In case it refers to
     * a project, it will follow its classpath so as to find exported roots as well.
     * Only works with resolved entry
     *
     * @param resolvedEntry
     *         IClasspathEntry
     * @param accumulatedRoots
     *         ObjectVector
     * @param rootIDs
     *         HashSet
     * @param referringEntry
     *         the CP entry (project) referring to this entry, or null if initial project
     * @param retrieveExportedRoots
     *         boolean
     * @throws JavaModelException
     */
    public void computePackageFragmentRoots(
            IClasspathEntry resolvedEntry,
            ObjectVector accumulatedRoots,
            HashSet rootIDs,
            IClasspathEntry referringEntry,
            boolean retrieveExportedRoots,
            Map rootToResolvedEntries) throws JavaModelException {

        String rootID = ((ClasspathEntry)resolvedEntry).rootID();
        if (rootIDs.contains(rootID)) return;

        IPath projectPath = this.getFullPath();
        IPath entryPath = resolvedEntry.getPath();
//        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPackageFragmentRoot root = null;

        switch (resolvedEntry.getEntryKind()) {

            // source folder
            case IClasspathEntry.CPE_SOURCE:

//                if (projectPath.isPrefixOf(entryPath)) {
                Object target1 = JavaModelManager.getTarget(entryPath, true/*check existency*/);
                if (target1 == null) return;

                if (target1 instanceof File && ((File)target1).isDirectory()) {
                    root = getPackageFragmentRoot((File)target1);
                }
//                }
                break;

            // internal/external JAR or folder
            case IClasspathEntry.CPE_LIBRARY:
                if (referringEntry != null && !resolvedEntry.isExported())
                    return;
                Object target = JavaModelManager.getTarget(entryPath, true/*check existency*/);
                if (target == null)
                    return;

//                if (target instanceof IResource){
//                    // internal target
//                } else
                if (target instanceof File) {
                    // external target
                    if (JavaModelManager.isFile(target)) {
                        root = new JarPackageFragmentRoot((File)target, this, manager);
                    } else if (((File)target).isDirectory()) {
                        root = getPackageFragmentRoot((File)target, entryPath);
//                        root = new ExternalPackageFragmentRoot(entryPath, this);
                    }
                }
                break;

            // recurse into required project
            case IClasspathEntry.CPE_PROJECT:

                if (!retrieveExportedRoots) return;
                if (referringEntry != null && !resolvedEntry.isExported()) return;
                //todo multiproject
//                IResource member = workspaceRoot.findMember(entryPath);
//                if (member != null && member.getType() == IResource.PROJECT) {// double check if bound to project (23977)
//                    IProject requiredProjectRsc = (IProject)member;
//                    if (JavaProject.hasJavaNature(requiredProjectRsc)) { // special builder binary output
//                        rootIDs.add(rootID);
//                        JavaProject requiredProject = (JavaProject)JavaCore.create(requiredProjectRsc);
//                        requiredProject.computePackageFragmentRoots(
//                                requiredProject.getResolvedClasspath(),
//                                accumulatedRoots,
//                                rootIDs,
//                                rootToResolvedEntries == null ? resolvedEntry
//                                                              : ((org.eclipse.jdt.internal.core.ClasspathEntry)resolvedEntry)
//                                        .combineWith((org.eclipse.jdt.internal.core.ClasspathEntry)referringEntry),
//                                // only combine if need to build the reverse map
//                                retrieveExportedRoots,
//                                rootToResolvedEntries);
//                    }
//                    break;
//                }
        }
        if (root != null) {
            accumulatedRoots.add(root);
            rootIDs.add(rootID);
            if (rootToResolvedEntries != null) rootToResolvedEntries
                    .put(root, ((ClasspathEntry)resolvedEntry).combineWith((ClasspathEntry)referringEntry));
        }
    }

    /**
     * @see IJavaProject
     */
    public IPackageFragmentRoot getPackageFragmentRoot(File resource) {
        return getPackageFragmentRoot(resource, null/*no entry path*/);
    }

    private IPackageFragmentRoot getPackageFragmentRoot(File resource, IPath entryPath) {
        if (resource.isDirectory()) {
//            if (ExternalFoldersManager.isInternalPathForExternalFolder(resource.getFullPath()))
//                return new ExternalPackageFragmentRoot(resource, entryPath, this);
            return new PackageFragmentRoot(resource, this, manager);
        } else {
            return new JarPackageFragmentRoot(resource, this, manager);
        }
//        return null;

    }

    /**
     * Answers an ID which is used to distinguish project/entries during package
     * fragment root computations
     *
     * @return String
     */
    public String rootID() {
        return "[PRJ]" + this.getFullPath(); //$NON-NLS-1$
    }

    @Override
    public Object[] getNonJavaResources() throws JavaModelException {
        return new Object[0];
    }

    @Override
    public String getOption(String optionName, boolean inheritJavaCoreOptions) {
        return options.get(optionName);
    }

    public Map getOptions(boolean b) {
        return options;
    }

    @Override
    public IPath getOutputLocation() throws JavaModelException {
        return null;
    }

    @Override
    public IPackageFragmentRoot getPackageFragmentRoot(String externalLibraryPath) {
        File file = new File(externalLibraryPath);
        if(file.isFile()) {
            return new JarPackageFragmentRoot(file, this, manager);
        } else {
            return new PackageFragmentRoot(file, this, manager);
        }
    }

    @Override
    public IPackageFragmentRoot getPackageFragmentRoot(IResource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPackageFragmentRoot[] getPackageFragmentRoots(IClasspathEntry entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPackageFragment[] getPackageFragments() throws JavaModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProject getProject() {
        return null;
    }

    @Override
    protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, File underlyingResource)
            throws JavaModelException {
        // cannot refresh cp markers on opening (emulate cp check on startup) since can create deadlocks (see bug 37274)
        IClasspathEntry[] resolvedClasspath = getResolvedClasspath();

        // compute the pkg fragment roots
        info.setChildren(computePackageFragmentRoots(resolvedClasspath, false, null /*no reverse map*/));

        return true;
    }

    /*
	 * Returns whether the given resource is accessible through the children or the non-Java resources of this project.
	 * Returns true if the resource is not in the project.
	 * Assumes that the resource is a folder or a file.
	 */
    public boolean contains(File resource) {
        return true;
    }

//    @Override
//    public IJavaElement getAncestor(int ancestorType) {
//        return null;
//    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
        return null;
    }

    @Override
    public IResource getCorrespondingResource() throws JavaModelException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getElementName() {
        return projectName;
    }

    @Override
    public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
        switch (token.charAt(0)) {
            case JEM_PACKAGEFRAGMENTROOT:
                String rootPath = IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH;
                token = null;
                while (memento.hasMoreTokens()) {
                    token = memento.nextToken();
                    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=331821
                    if (token == MementoTokenizer.PACKAGEFRAGMENT || token == MementoTokenizer.COUNT) {
                        break;
                    }
                    rootPath += token;
                }
                JavaElement root = (JavaElement)getPackageFragmentRoot(new File(rootPath));
                if (token != null && token.charAt(0) == JEM_PACKAGEFRAGMENT) {
                    return root.getHandleFromMemento(token, memento, owner);
                } else {
                    return root.getHandleFromMemento(memento, owner);
                }
        }
        return null;
    }

    @Override
    public int getElementType() {
        return IJavaElement.JAVA_PROJECT;
    }

    @Override
    protected char getHandleMementoDelimiter() {
        return JEM_JAVAPROJECT;
    }

    @Override
    public IJavaModel getJavaModel() {
        return null;
    }

    public IPath getPath() {
        return new Path(projectDir.getAbsolutePath());
    }

    public String getProjectPath(){
        return projectPath;
    }

    @Override
    public IJavaElement getPrimaryElement() {
        return null;
    }

    @Override
    public IResource getResource() {
        return null;
    }

    @Override
    protected File resource(PackageFragmentRoot root) {
        return projectDir;
    }

    @Override
    public ISchedulingRule getSchedulingRule() {
        return null;
    }

    @Override
    public IResource getUnderlyingResource() throws JavaModelException {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isStructureKnown() throws JavaModelException {
        return false;
    }

    @Override
    public Object getAdapter(Class aClass) {
        return null;
    }

    @Override
    public void close() throws JavaModelException {
        indexManager.shutdown();
        indexManager.deleteIndexFiles();
        nameEnvironment.cleanup();
        File file = new File(tempDir + "/indexes/" + wsId);
        String[] list = file.list();
        if (list == null || list.length == 0) {
            file.delete();
        }
        super.close();
    }

    @Override
    public String findRecommendedLineSeparator() throws JavaModelException {
        return null;
    }

    @Override
    public IBuffer getBuffer() throws JavaModelException {
        return null;
    }

    @Override
    public boolean hasUnsavedChanges() throws JavaModelException {
        return false;
    }

    @Override
    public boolean isConsistent() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void makeConsistent(IProgressMonitor progress) throws JavaModelException {

    }

    @Override
    public void open(IProgressMonitor progress) throws JavaModelException {

    }

    @Override
    public void save(IProgressMonitor progress, boolean force) throws JavaModelException {

    }

    /**
     * The path is known to match a source/library folder entry.
     *
     * @param path
     *         IPath
     * @return IPackageFragmentRoot
     */
    public IPackageFragmentRoot getFolderPackageFragmentRoot(IPath path) {
        if (path.segmentCount() == 1) { // default project root
            return getPackageFragmentRoot();
        }
        return getPackageFragmentRoot(path.toFile());
    }

    public JavaProjectElementInfo.ProjectCache getProjectCache() throws JavaModelException {
        return ((JavaProjectElementInfo)getElementInfo()).getProjectCache(this);
    }

    /**
     * Returns a new element info for this element.
     */
    protected Object createElementInfo() {
        return new JavaProjectElementInfo();
    }

    @Override
    protected IStatus validateExistence(File underlyingResource) {
        return JavaModelStatus.VERIFIED_OK;
    }

    /**
     * @see org.eclipse.jdt.internal.core.JavaElement#getHandleMemento(StringBuffer)
     */
    protected void getHandleMemento(StringBuffer buff) {
//        buff.append(getElementName());
    }

    /*
     * Resets this project's caches
     */
    public void resetCaches() {
        JavaProjectElementInfo
                info = (JavaProjectElementInfo) manager.peekAtInfo(this);
        if (info != null){
            info.resetCaches();
        }
    }

    private void addToResult(IClasspathEntry rawEntry, IClasspathEntry resolvedEntry, ResolvedClasspath result,
                             LinkedHashSet<IClasspathEntry> resolvedEntries) {

        IPath resolvedPath;
        // If it's already been resolved, do not add to resolvedEntries
        if (result.rawReverseMap.get(resolvedPath = resolvedEntry.getPath()) == null) {
            result.rawReverseMap.put(resolvedPath, rawEntry);
            result.rootPathToResolvedEntries.put(resolvedPath, resolvedEntry);
            resolvedEntries.add(resolvedEntry);
        }
    }

    public IndexManager getIndexManager() {
        return indexManager;
    }

    /**
     * Convenience method that returns the specific type of info for a Java project.
     */
    protected JavaProjectElementInfo getJavaProjectElementInfo()
            throws JavaModelException {

        return (JavaProjectElementInfo)getElementInfo();
    }

    public NameLookup newNameLookup(ICompilationUnit[] workingCopies) throws JavaModelException {
        return getJavaProjectElementInfo().newNameLookup(this, workingCopies);
    }

    public SearchableEnvironment newSearchableNameEnvironment(ICompilationUnit[] workingCopies) throws JavaModelException {
        return new SearchableEnvironment(this, workingCopies);
    }

    /**
     * Returns true if this handle represents the same Java project
     * as the given handle. Two handles represent the same
     * project if they are identical or if they represent a project with
     * the same underlying resource and occurrence counts.
     *
     * @see org.eclipse.jdt.internal.core.JavaElement#equals(Object)
     */
    public boolean equals(Object o) {

        if (this == o)
            return true;

        if (!(o instanceof JavaProject))
            return false;

        JavaProject other = (JavaProject)o;
        return this.getFullPath().equals(other.getFullPath());
    }

    /*
     * Returns a new search name environment for this project. This name environment first looks in the working copies
     * of the given owner.
     */
    public SearchableEnvironment newSearchableNameEnvironment(WorkingCopyOwner owner) throws JavaModelException {
        return new SearchableEnvironment(this, owner);
    }

    public String getWsId() {
        return wsId;
    }

    public JavaModelManager getJavaModelManager() {
        return manager;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public synchronized void creteNewNameEnvironment() {
        try {
            nameEnvironment = new SearchableEnvironment(this, (ICompilationUnit[])null);
        } catch (JavaModelException e) {
            LOG.error("Can't create SearchableEnvironment", e);
        }
    }

    public static class ResolvedClasspath {
        IClasspathEntry[] resolvedClasspath;
        IJavaModelStatus                unresolvedEntryStatus     = JavaModelStatus.VERIFIED_OK;
        HashMap<IPath, IClasspathEntry> rawReverseMap             = new HashMap<>();
        Map<IPath, IClasspathEntry>     rootPathToResolvedEntries = new HashMap<>();
        IClasspathEntry[]               referencedEntries         = null;
    }
}
