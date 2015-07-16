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

import org.eclipse.che.jdt.core.JavaConventions;
import org.eclipse.che.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.che.jdt.internal.core.search.IRestrictedAccessTypeRequestor;
import org.eclipse.che.jdt.internal.core.search.JavaWorkspaceScope;
import org.eclipse.che.jdt.internal.core.search.Util;
import org.eclipse.che.jdt.internal.core.search.indexing.IndexManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.util.HashtableOfArrayToObject;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.WeakHashSet;
import org.eclipse.jdt.internal.core.util.WeakHashSetOfCharArray;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class JavaModelManager {

    private final static String             INDEXED_SECONDARY_TYPES        = "#@*_indexing secondary cache_*@#"; //$NON-NLS-1$
    public static        boolean            ZIP_ACCESS_VERBOSE             = false;
    public static        boolean            VERBOSE                        = false;
    public final static  ICompilationUnit[] NO_WORKING_COPY                = new ICompilationUnit[0];
    /**
     * A set of java.io.Files used as a cache of external jars that
     * are known to be existing.
     * Note this cache is kept for the whole session.
     */
    public static        HashSet<File>      existingExternalFiles          = new HashSet<>();
//    /**
//     * The singleton manager
//     */
//    private static JavaModelManager MANAGER                        = new JavaModelManager();
    /**
     * A set of external files ({@link #existingExternalFiles}) which have
     * been confirmed as file (i.e. which returns true to {@link java.io.File#isFile()}.
     * Note this cache is kept for the whole session.
     */
    public static        HashSet<File>      existingExternalConfirmedFiles = new HashSet<>();
    /* whether an AbortCompilationUnit should be thrown when the source of a compilation unit cannot be retrieved */
    public               ThreadLocal        abortOnMissingSource           = new ThreadLocal();
    /**
     * Set of elements which are out of sync with their buffers.
     */
    protected            HashSet            elementsOutOfSynchWithBuffers  = new HashSet(11);
    /**
     * Table from WorkingCopyOwner to a table of ICompilationUnit (working copy handle) to PerWorkingCopyInfo.
     * NOTE: this object itself is used as a lock to synchronize creation/removal of per working copy infos
     */
    protected            Map                perWorkingCopyInfos            = new HashMap(5);
    /**
     * List of IPath of jars that are known to be invalid - such as not being a valid/known format
     */
    private Set<IPath> invalidArchives;
    /**
     * A cache of opened zip files per thread.
     * (for a given thread, the object value is a HashMap from IPath to java.io.ZipFile)
     */
    private ThreadLocal<ZipCache>  zipFiles         = new ThreadLocal<>();
    /*
 * Temporary cache of newly opened elements
 */
    private ThreadLocal            temporaryCache   = new ThreadLocal();
    /*
     * Pools of symbols used in the Java model.
     * Used as a replacement for String#intern() that could prevent garbage collection of strings on some VMs.
     */
    private WeakHashSet            stringSymbols    = new WeakHashSet(5);
    private WeakHashSetOfCharArray charArraySymbols = new WeakHashSetOfCharArray(5);
    /**
     * Infos cache.
     */
    private JavaModelCache       cache;
    public  IndexManager         indexManager;
    private PerProjectInfo       info;
    private BufferManager        DEFAULT_BUFFER_MANAGER;
    /**
     * Holds the state used for delta processing.
     */
    public  DeltaProcessingState deltaState;
    /**
     * Unique handle onto the JavaModel
     */
    final   JavaModel            javaModel;
    /**
     * A weak set of the known search scopes.
     */
    protected WeakHashMap searchScopes = new WeakHashMap();

    /*
     * The unique workspace scope
     */
    public JavaWorkspaceScope workspaceScope;
    private JavaProject javaProject;

//    public static JavaModelManager getJavaModelManager() {
//        return MANAGER;
//    }

    public JavaModelManager() {
        // initialize Java model cache
        this.cache = new JavaModelCache();
        javaModel = new JavaModel(this);
        deltaState = new DeltaProcessingState(this);
    }

    /**
     * Helper method - returns the targeted item (IResource if internal or java.io.File if external),
     * or null if unbound
     * Internal items must be referred to using container relative paths.
     */
    public static Object getTarget(IPath path, boolean checkResourceExistence) {
        File externalFile = new File(path.toOSString());
        if (!checkResourceExistence) {
            return externalFile;
        } else if (existingExternalFilesContains(externalFile)) {
            return externalFile;
        } else {
            if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
                System.out.println("(" + Thread.currentThread() + ") [JavaModel.getTarget...)] Checking existence of " +
                                   path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (externalFile.isFile()) { // isFile() checks for existence (it returns false if a directory)
                // cache external file
                existingExternalFilesAdd(externalFile);
                return externalFile;
            } else {
                if (externalFile.exists()) {
                    existingExternalFilesAdd(externalFile);
                    return externalFile;
                }
            }
        }
        return null;
    }

    public IndexManager getIndexManager() {
        return indexManager;
    }

    public JavaModel getJavaModel() {
        return javaModel;
    }

    private synchronized static void existingExternalFilesAdd(File externalFile) {
        existingExternalFiles.add(externalFile);
    }

    private synchronized static boolean existingExternalFilesContains(File externalFile) {
        return existingExternalFiles.contains(externalFile);
    }

    /**
     * Flushes the cache of external files known to be existing.
     */
    public static void flushExternalFileCache() {
        existingExternalFiles = new HashSet<>();
        existingExternalConfirmedFiles = new HashSet<>();
    }

    /**
     * Helper method - returns whether an object is afile (i.e. which returns true to {@link java.io.File#isFile()}.
     */
    public static boolean isFile(Object target) {
        return getFile(target) != null;
    }

    /**
     * Helper method - returns the file item (i.e. which returns true to {@link java.io.File#isFile()},
     * or null if unbound
     */
    public static synchronized File getFile(Object target) {
        if (existingExternalConfirmedFiles.contains(target))
            return (File)target;
        if (target instanceof File) {
            File f = (File)target;
            if (f.isFile()) {
                existingExternalConfirmedFiles.add(f);
                return f;
            }
        }

        return null;
    }

    /**
     * Creates and returns a compilation unit element for the given <code>.java</code>
     * file, its project being the given project. Returns <code>null</code> if unable
     * to recognize the compilation unit.
     */
    public static ICompilationUnit createCompilationUnitFrom(File file, IJavaProject project) {

        if (file == null) return null;

//        if (project == null) {
//            project = JavaCore.create(file.getProject());
//        }
        IPackageFragment pkg = (IPackageFragment)determineIfOnClasspath(file, (JavaProject)project);
        if (pkg == null) {
            // not on classpath - make the root its folder, and a default package
            PackageFragmentRoot root = (PackageFragmentRoot)project.getPackageFragmentRoot(file.getParent());
            pkg = root.getPackageFragment(CharOperation.NO_STRINGS);

            if (VERBOSE) {
                System.out.println("WARNING : creating unit element outside classpath (" + Thread.currentThread() + "): " +
                                   file.getAbsolutePath()); //$NON-NLS-1$//$NON-NLS-2$
            }
        }
        return pkg.getCompilationUnit(file.getName());
    }

    /**
     * Returns the package fragment root represented by the resource, or
     * the package fragment the given resource is located in, or <code>null</code>
     * if the given resource is not on the classpath of the given project.
     */
    public static IJavaElement determineIfOnClasspath(File resource, JavaProject project) {
        IPath resourcePath = new Path(resource.getAbsolutePath());
        boolean isExternal = false; //ExternalFoldersManager.isInternalPathForExternalFolder(resourcePath);
//        if (isExternal)
//            resourcePath = resource.getLocation();

        try {
            JavaProjectElementInfo projectInfo = (JavaProjectElementInfo)((JavaProject)project).manager.getInfo(project);
            JavaProjectElementInfo.ProjectCache projectCache = projectInfo == null ? null : projectInfo.projectCache;
            HashtableOfArrayToObject allPkgFragmentsCache = projectCache == null ? null : projectCache.allPkgFragmentsCache;
            boolean isJavaLike = Util.isJavaLikeFileName(resourcePath.lastSegment());
            IClasspathEntry[] entries =
                    isJavaLike ? project.getRawClasspath() // JAVA file can only live inside SRC folder (on the raw path)
                               : ((JavaProject)project).getResolvedClasspath();

            int length = entries.length;
            if (length > 0) {
                String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
                String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
                for (int i = 0; i < length; i++) {
                    IClasspathEntry entry = entries[i];
                    if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) continue;
                    IPath rootPath = entry.getPath();
                    if (rootPath.equals(resourcePath)) {
                        if (isJavaLike)
                            return null;
                        return project.getPackageFragmentRoot(resource);
                    } else if (rootPath.isPrefixOf(resourcePath)) {
                        // allow creation of package fragment if it contains a .java file that is included
                        if (!Util
                                .isExcluded(resourcePath, ((ClasspathEntry)entry).fullInclusionPatternChars(),
                                            ((ClasspathEntry)entry).fullExclusionPatternChars(), true)) {
                            // given we have a resource child of the root, it cannot be a JAR pkg root
                            PackageFragmentRoot root =
//                                    isExternal ?
//                                    new ExternalPackageFragmentRoot(rootPath, (JavaProject) project) :
                                    (PackageFragmentRoot)((JavaProject)project).getFolderPackageFragmentRoot(rootPath);
                            if (root == null) return null;
                            IPath pkgPath = resourcePath.removeFirstSegments(rootPath.segmentCount());

                            if (resource.isFile()) {
                                // if the resource is a file, then remove the last segment which
                                // is the file name in the package
                                pkgPath = pkgPath.removeLastSegments(1);
                            }
                            String[] pkgName = pkgPath.segments();

                            // if package name is in the cache, then it has already been validated
                            // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=133141)
                            if (allPkgFragmentsCache != null && allPkgFragmentsCache.containsKey(pkgName))
                                return root.getPackageFragment(pkgName);

                            if (pkgName.length != 0 && JavaConventions.validatePackageName(
                                    Util.packageName(pkgPath, sourceLevel, complianceLevel), sourceLevel,
                                    complianceLevel).getSeverity() == IStatus.ERROR) {
                                return null;
                            }
                            return root.getPackageFragment(pkgName);
                        }
                    }
                }
            }
        } catch (JavaModelException npe) {
            return null;
        }
        return null;
    }

    public synchronized char[] intern(char[] array) {
        return this.charArraySymbols.add(array);
    }

    public synchronized String intern(String s) {
        // make sure to copy the string (so that it doesn't hold on the underlying char[] that might be much bigger than necessary)
        return (String)this.stringSymbols.add(new String(s));

        // Note1: String#intern() cannot be used as on some VMs this prevents the string from being garbage collected
        // Note 2: Instead of using a WeakHashset, one could use a WeakHashMap with the following implementation
        // 			   This would costs more per entry (one Entry object and one WeakReference more))

		/*
        WeakReference reference = (WeakReference) this.symbols.get(s);
		String existing;
		if (reference != null && (existing = (String) reference.get()) != null)
			return existing;
		this.symbols.put(s, new WeakReference(s));
		return s;
		*/
    }

    /**
     * Returns the set of elements which are out of synch with their buffers.
     */
    protected HashSet getElementsOutOfSynchWithBuffers() {
        return this.elementsOutOfSynchWithBuffers;
    }

    /**
     * Returns the open ZipFile at the given path. If the ZipFile
     * does not yet exist, it is created, opened, and added to the cache
     * of open ZipFiles.
     * <p/>
     * The path must be a file system path if representing an external
     * zip/jar, or it must be an absolute workspace relative path if
     * representing a zip/jar inside the workspace.
     *
     * @throws org.eclipse.core.runtime.CoreException
     *         If unable to create/open the ZipFile
     */
    public ZipFile getZipFile(IPath path) throws CoreException {

        if (isInvalidArchive(path))
            throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.status_IOException, new ZipException()));

        ZipCache zipCache;
        ZipFile zipFile;
        if ((zipCache = this.zipFiles.get()) != null
            && (zipFile = zipCache.getCache(path)) != null) {
            return zipFile;
        }
        File localFile = null;
//        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
//        IResource file = root.findMember(path);
//        if (file != null) {
//            // internal resource
//            URI location;
//            if (file.getType() != IResource.FILE || (location = file.getLocationURI()) == null) {
//                throw new CoreException(
//                        new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.bind(Messages.file_notFound, path.toString()), null));
//            }
//            localFile = Util.toLocalFile(location, null*//*no progress availaible*//*);
//            if (localFile == null)
//                throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.bind(Messages.file_notFound,
// path.toString()), null));
//        } else {
//            // external resource -> it is ok to use toFile()
        localFile = path.toFile();
//        }
        if (!localFile.exists()) {
            throw new CoreException(
                    new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.bind(Messages.file_notFound, path.toString()), null));
        }

        try {
            if (ZIP_ACCESS_VERBOSE) {
                System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.getZipFile(IPath)] Creating ZipFile on " +
                                   localFile); //$NON-NLS-1$ //$NON-NLS-2$
            }
            zipFile = new ZipFile(localFile);
            if (zipCache != null) {
                zipCache.setCache(path, zipFile);
            }
            return zipFile;
        } catch (IOException e) {
            addInvalidArchive(path);
            throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, -1, Messages.status_IOException, e));
        }
    }

    public boolean isInvalidArchive(IPath path) {
        return this.invalidArchives != null && this.invalidArchives.contains(path);
    }

    public void removeFromInvalidArchiveCache(IPath path) {
        if (this.invalidArchives != null) {
            this.invalidArchives.remove(path);
        }
    }

    public void addInvalidArchive(IPath path) {
        // unlikely to be null
        if (this.invalidArchives == null) {
            this.invalidArchives = Collections.synchronizedSet(new HashSet<IPath>());
        }
        if (this.invalidArchives != null) {
            this.invalidArchives.add(path);
        }
    }

    public ICompilationUnit[] getWorkingCopies(DefaultWorkingCopyOwner primary, boolean b) {
        return null;
    }

    public ICompilationUnit[] getWorkingCopies(WorkingCopyOwner workingCopyOwner, boolean b) {
        return null;
    }

    /**
     * Returns the info for the element.
     */
    public synchronized Object getInfo(IJavaElement element) {
        HashMap tempCache = (HashMap)this.temporaryCache.get();
        if (tempCache != null) {
            Object result = tempCache.get(element);
            if (result != null) {
                return result;
            }
        }
        return this.cache.getInfo(element);
    }

    /**
     * Returns the info for this element without
     * disturbing the cache ordering.
     */
    protected synchronized Object peekAtInfo(IJavaElement element) {
        HashMap tempCache = (HashMap)this.temporaryCache.get();
        if (tempCache != null) {
            Object result = tempCache.get(element);
            if (result != null) {
                return result;
            }
        }
        return this.cache.peekAtInfo(element);
    }

    /*
	 * Removes all cached info for the given element (including all children)
	 * from the cache.
	 * Returns the info for the given element, or null if it was closed.
	 */
    public synchronized Object removeInfoAndChildren(JavaElement element) throws JavaModelException {
        Object info = this.cache.peekAtInfo(element);
        if (info != null) {
            boolean wasVerbose = false;
            try {
                if (JavaModelCache.VERBOSE) {
                    String elementType;
                    switch (element.getElementType()) {
                        case IJavaElement.JAVA_PROJECT:
                            elementType = "project"; //$NON-NLS-1$
                            break;
                        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                            elementType = "root"; //$NON-NLS-1$
                            break;
                        case IJavaElement.PACKAGE_FRAGMENT:
                            elementType = "package"; //$NON-NLS-1$
                            break;
                        case IJavaElement.CLASS_FILE:
                            elementType = "class file"; //$NON-NLS-1$
                            break;
                        case IJavaElement.COMPILATION_UNIT:
                            elementType = "compilation unit"; //$NON-NLS-1$
                            break;
                        default:
                            elementType = "element"; //$NON-NLS-1$
                    }
                    System.out.println(Thread.currentThread() + " CLOSING " + elementType + " " +
                                       element.toStringWithAncestors());  //$NON-NLS-1$//$NON-NLS-2$
                    wasVerbose = true;
                    JavaModelCache.VERBOSE = false;
                }
                element.closing(info);
                if (element instanceof IParent) {
                    closeChildren(info);
                }
                this.cache.removeInfo(element);
                if (wasVerbose) {
                    System.out.println(this.cache.toStringFillingRation("-> ")); //$NON-NLS-1$
                }
            } finally {
                JavaModelCache.VERBOSE = wasVerbose;
            }
            return info;
        }
        return null;
    }
    public void removePerProjectInfo(JavaProject javaProject, boolean removeExtJarInfo) {
        //todo
//        synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
//            IProject project = javaProject.getProject();
//            PerProjectInfo info= (PerProjectInfo) this.perProjectInfos.get(project);
//            if (info != null) {
//                this.perProjectInfos.remove(project);
//                if (removeExtJarInfo) {
//                    info.forgetExternalTimestampsAndIndexes();
//                }
//            }
//        }
//        resetClasspathListCache();
    }

    /*
 * The given project is being removed. Remove all containers for this project from the cache.
 */
    public synchronized void containerRemove(IJavaProject project) {
        //TODO
//        Map initializations = (Map) this.containerInitializationInProgress.get();
//        if (initializations != null) {
//            initializations.remove(project);
//        }
//        this.containers.remove(project);
    }

    /*
 * Returns whether there is a temporary cache for the current thread.
 */
    public boolean hasTemporaryCache() {
        return this.temporaryCache.get() != null;
    }

    /**
     * Returns the temporary cache for newly opened elements for the current thread.
     * Creates it if not already created.
     */
    public HashMap getTemporaryCache() {
        HashMap result = (HashMap)this.temporaryCache.get();
        if (result == null) {
            result = new HashMap();
            this.temporaryCache.set(result);
        }
        return result;
    }

    /*
	 * Puts the infos in the given map (keys are IJavaElements and values are JavaElementInfos)
	 * in the Java model cache in an atomic way if the info is not already present in the cache.
	 * If the info is already present in the cache, it depends upon the forceAdd parameter.
	 * If forceAdd is false it just returns the existing info and if true, this element and it's children are closed and then
	 * this particular info is added to the cache.
	 */
    protected synchronized Object putInfos(IJavaElement openedElement, Object newInfo, boolean forceAdd, Map newElements) {
        // remove existing children as the are replaced with the new children contained in newElements
        Object existingInfo = this.cache.peekAtInfo(openedElement);
        if (existingInfo != null && !forceAdd) {
            // If forceAdd is false, then it could mean that the particular element
            // wasn't in cache at that point of time, but would have got added through
            // another thread. In that case, removing the children could remove it's own
            // children. So, we should not remove the children but return the already existing
            // info.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=372687
            return existingInfo;
        }
        if (openedElement instanceof IParent) {
            closeChildren(existingInfo);
        }

        // Need to put any JarPackageFragmentRoot in first.
        // This is due to the way the LRU cache flushes entries.
        // When a JarPackageFragment is flushed from the LRU cache, the entire
        // jar is flushed by removing the JarPackageFragmentRoot and all of its
        // children (see ElementCache.close()). If we flush the JarPackageFragment
        // when its JarPackageFragmentRoot is not in the cache and the root is about to be
        // added (during the 'while' loop), we will end up in an inconsistent state.
        // Subsequent resolution against package in the jar would fail as a result.
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=102422
        // (theodora)
        for (Iterator it = newElements.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            IJavaElement element = (IJavaElement)entry.getKey();
            if (element instanceof JarPackageFragmentRoot) {
                Object info = entry.getValue();
                it.remove();
                this.cache.putInfo(element, info);
            }
        }

        Iterator iterator = newElements.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            this.cache.putInfo((IJavaElement)entry.getKey(), entry.getValue());
        }
        return newInfo;
    }

    private void closeChildren(Object info) {
        if (info instanceof JavaElementInfo) {
            IJavaElement[] children = ((JavaElementInfo)info).getChildren();
            for (int i = 0, size = children.length; i < size; ++i) {
                JavaElement child = (JavaElement)children[i];
                try {
                    child.close();
                } catch (JavaModelException e) {
                    // ignore
                }
            }
        }
    }

    /*
 * Returns the per-working copy info for the given working copy at the given path.
 * If it doesn't exist and if create, add a new per-working copy info with the given problem requestor.
 * If recordUsage, increment the per-working copy info's use count.
 * Returns null if it doesn't exist and not create.
 */
    public PerWorkingCopyInfo getPerWorkingCopyInfo(CompilationUnit workingCopy, boolean create, boolean recordUsage,
                                                    IProblemRequestor problemRequestor) {
        synchronized (this.perWorkingCopyInfos) { // use the perWorkingCopyInfo collection as its own lock
            WorkingCopyOwner owner = workingCopy.owner;
            Map workingCopyToInfos = (Map)this.perWorkingCopyInfos.get(owner);
            if (workingCopyToInfos == null && create) {
                workingCopyToInfos = new HashMap();
                this.perWorkingCopyInfos.put(owner, workingCopyToInfos);
            }

            PerWorkingCopyInfo info = workingCopyToInfos == null ? null : (PerWorkingCopyInfo)workingCopyToInfos.get(workingCopy);
            if (info == null && create) {
                info = new PerWorkingCopyInfo(workingCopy, problemRequestor);
                workingCopyToInfos.put(workingCopy, info);
            }
            if (info != null && recordUsage) info.useCount++;
            return info;
        }
    }

    /*
 * Returns the per-project info for the given project. If specified, create the info if the info doesn't exist.
 */
    public PerProjectInfo getPerProjectInfo(boolean create) {
//        synchronized(this.perProjectInfos) { // use the perProjectInfo collection as its own lock
//            PerProjectInfo info= (PerProjectInfo) this.perProjectInfos.get(project);
        if (info == null && create) {
            info = new PerProjectInfo();
//                this.perProjectInfos.put(project, info);
        }
        return info;
//        }
    }

    /*
     * Returns  the per-project info for the given project.
     * If the info doesn't exist, check for the project existence and create the info.
     * @throws JavaModelException if the project doesn't exist.
     */
    public PerProjectInfo getPerProjectInfoCheckExistence() throws JavaModelException {
        JavaModelManager.PerProjectInfo info = getPerProjectInfo(false /* don't create info */);
        if (info == null) {
//            if (!JavaProject.hasJavaNature(project)) {
//                throw ((JavaProject)JavaCore.create(project)).newNotPresentException();
//            }
            info = getPerProjectInfo(true /* create info */);
        }
        return info;
    }

    /**
     * Get all secondary types for a project and store result in per project info cache.
     * <p>
     * This cache is an <code>Hashtable&lt;String, HashMap&lt;String, IType&gt;&gt;</code>:
     * <ul>
     * <li>key: package name
     * <li>value:
     * <ul>
     * <li>key: type name
     * <li>value: java model handle for the secondary type
     * </ul>
     * </ul>
     * Hashtable was used to protect callers from possible concurrent access.
     * </p>
     * Note that this map may have a specific entry which key is {@link #INDEXED_SECONDARY_TYPES }
     * and value is a map containing all secondary types created during indexing.
     * When this key is in cache and indexing is finished, returned map is merged
     * with the value of this special key. If indexing is not finished and caller does
     * not wait for the end of indexing, returned map is the current secondary
     * types cache content which may be invalid...
     *
     * @param project
     *         Project we want get secondary types from
     * @return HashMap Table of secondary type names->path for given project
     */
    public Map secondaryTypes(IJavaProject project, boolean waitForIndexes, IProgressMonitor monitor) throws JavaModelException {
        if (VERBOSE) {
            StringBuffer buffer = new StringBuffer("JavaModelManager.secondaryTypes("); //$NON-NLS-1$
            buffer.append(project.getElementName());
            buffer.append(',');
            buffer.append(waitForIndexes);
            buffer.append(')');
            Util.verbose(buffer.toString());
        }

        // Return cache if not empty and there's no new secondary types created during indexing
        final PerProjectInfo projectInfo = getPerProjectInfoCheckExistence();
        Map indexingSecondaryCache =
                projectInfo.secondaryTypes == null ? null : (Map)projectInfo.secondaryTypes.get(INDEXED_SECONDARY_TYPES);
        if (projectInfo.secondaryTypes != null && indexingSecondaryCache == null) {
            return projectInfo.secondaryTypes;
        }

        // Perform search request only if secondary types cache is not initialized yet (this will happen only once!)
        if (projectInfo.secondaryTypes == null) {
            return secondaryTypesSearching(project, waitForIndexes, monitor, projectInfo);
        }

        // New secondary types have been created while indexing secondary types cache
        // => need to know whether the indexing is finished or not
        boolean indexing = this.indexManager.awaitingJobsCount() > 0;
        if (indexing) {
            if (!waitForIndexes) {
                // Indexing is running but caller cannot wait => return current cache
                return projectInfo.secondaryTypes;
            }

            // Wait for the end of indexing or a cancel
            while (this.indexManager.awaitingJobsCount() > 0) {
                if (monitor != null && monitor.isCanceled()) {
                    return projectInfo.secondaryTypes;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return projectInfo.secondaryTypes;
                }
            }
        }

        // Indexing is finished => merge caches and return result
        return secondaryTypesMerging(projectInfo.secondaryTypes);
    }

    /*
     * Return secondary types cache merged with new secondary types created while indexing
     * Note that merge result is directly stored in given parameter map.
     */
    private Hashtable secondaryTypesMerging(Hashtable secondaryTypes) {
        if (VERBOSE) {
            Util.verbose("JavaModelManager.getSecondaryTypesMerged()"); //$NON-NLS-1$
            Util.verbose("	- current cache to merge:"); //$NON-NLS-1$
            Iterator entries = secondaryTypes.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                String packName = (String)entry.getKey();
                Util.verbose("		+ " + packName + ':' + entry.getValue()); //$NON-NLS-1$
            }
        }

        // Return current cache if there's no indexing cache (double check, this should not happen)
        HashMap indexedSecondaryTypes = (HashMap)secondaryTypes.remove(INDEXED_SECONDARY_TYPES);
        if (indexedSecondaryTypes == null) {
            return secondaryTypes;
        }

        // Merge indexing cache in secondary types one
        Iterator entries = indexedSecondaryTypes.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry)entries.next();
            File file = (File)entry.getKey();

            // Remove all secondary types of indexed file from cache
            secondaryTypesRemoving(secondaryTypes, file);

            // Add all indexing file secondary types in given secondary types cache
            HashMap fileSecondaryTypes = (HashMap)entry.getValue();
            Iterator entries2 = fileSecondaryTypes.entrySet().iterator();
            while (entries2.hasNext()) {
                Map.Entry entry2 = (Map.Entry)entries2.next();
                String packageName = (String)entry2.getKey();
                HashMap cachedTypes = (HashMap)secondaryTypes.get(packageName);
                if (cachedTypes == null) {
                    secondaryTypes.put(packageName, entry2.getValue());
                } else {
                    HashMap types = (HashMap)entry2.getValue();
                    Iterator entries3 = types.entrySet().iterator();
                    while (entries3.hasNext()) {
                        Map.Entry entry3 = (Map.Entry)entries3.next();
                        String typeName = (String)entry3.getKey();
                        cachedTypes.put(typeName, entry3.getValue());
                    }
                }
            }
        }
        if (VERBOSE) {
            Util.verbose("	- secondary types cache merged:"); //$NON-NLS-1$
            entries = secondaryTypes.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                String packName = (String)entry.getKey();
                Util.verbose("		+ " + packName + ':' + entry.getValue()); //$NON-NLS-1$
            }
        }
        return secondaryTypes;
    }

    /**
     * Remove from secondary types cache all types belonging to a given file.
     * Clean secondary types cache built while indexing if requested.
     * <p/>
     * Project's secondary types cache is found using file location.
     *
     * @param file
     *         File to remove
     */
    public void secondaryTypesRemoving(File file, boolean cleanIndexCache) {
        if (VERBOSE) {
            StringBuffer buffer = new StringBuffer("JavaModelManager.removeFromSecondaryTypesCache("); //$NON-NLS-1$
            buffer.append(file.getName());
            buffer.append(')');
            Util.verbose(buffer.toString());
        }
        if (file != null) {
            PerProjectInfo projectInfo = getPerProjectInfo(false);
            if (projectInfo != null && projectInfo.secondaryTypes != null) {
                if (VERBOSE) {
                    Util.verbose("-> remove file from cache of project: " + file.getAbsolutePath()); //$NON-NLS-1$
                }

                // Clean current cache
                secondaryTypesRemoving(projectInfo.secondaryTypes, file);

                // Clean indexing cache if necessary
                HashMap indexingCache = (HashMap)projectInfo.secondaryTypes.get(INDEXED_SECONDARY_TYPES);
                if (!cleanIndexCache) {
                    if (indexingCache == null) {
                        // Need to signify that secondary types indexing will happen before any request happens
                        // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=152841
                        projectInfo.secondaryTypes.put(INDEXED_SECONDARY_TYPES, new HashMap());
                    }
                    return;
                }
                if (indexingCache != null) {
                    Set keys = indexingCache.keySet();
                    int filesSize = keys.size(), filesCount = 0;
                    File[] removed = null;
                    Iterator cachedFiles = keys.iterator();
                    while (cachedFiles.hasNext()) {
                        File cachedFile = (File)cachedFiles.next();
                        if (file.equals(cachedFile)) {
                            if (removed == null) removed = new File[filesSize];
                            filesSize--;
                            removed[filesCount++] = cachedFile;
                        }
                    }
                    if (removed != null) {
                        for (int i = 0; i < filesCount; i++) {
                            indexingCache.remove(removed[i]);
                        }
                    }
                }
            }
        }
    }

    /*
     * Remove from a given cache map all secondary types belonging to a given file.
	 * Note that there can have several secondary types per file...
	 */
    private void secondaryTypesRemoving(Hashtable secondaryTypesMap, File file) {
        if (VERBOSE) {
            StringBuffer buffer = new StringBuffer("JavaModelManager.removeSecondaryTypesFromMap("); //$NON-NLS-1$
            Iterator entries = secondaryTypesMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                String qualifiedName = (String)entry.getKey();
                buffer.append(qualifiedName + ':' + entry.getValue());
            }
            buffer.append(',');
            buffer.append(file.getAbsolutePath());
            buffer.append(')');
            Util.verbose(buffer.toString());
        }
        Set packageEntries = secondaryTypesMap.entrySet();
        int packagesSize = packageEntries.size(), removedPackagesCount = 0;
        String[] removedPackages = null;
        Iterator packages = packageEntries.iterator();
        while (packages.hasNext()) {
            Map.Entry entry = (Map.Entry)packages.next();
            String packName = (String)entry.getKey();
            if (packName != INDEXED_SECONDARY_TYPES) { // skip indexing cache entry if present (!= is intentional)
                HashMap types = (HashMap)entry.getValue();
                Set nameEntries = types.entrySet();
                int namesSize = nameEntries.size(), removedNamesCount = 0;
                String[] removedNames = null;
                Iterator names = nameEntries.iterator();
                while (names.hasNext()) {
                    Map.Entry entry2 = (Map.Entry)names.next();
                    String typeName = (String)entry2.getKey();
                    JavaElement type = (JavaElement)entry2.getValue();
                    if (file.equals(type.resource())) {
                        if (removedNames == null) removedNames = new String[namesSize];
                        namesSize--;
                        removedNames[removedNamesCount++] = typeName;
                    }
                }
                if (removedNames != null) {
                    for (int i = 0; i < removedNamesCount; i++) {
                        types.remove(removedNames[i]);
                    }
                }
                if (types.size() == 0) {
                    if (removedPackages == null) removedPackages = new String[packagesSize];
                    packagesSize--;
                    removedPackages[removedPackagesCount++] = packName;
                }
            }
        }
        if (removedPackages != null) {
            for (int i = 0; i < removedPackagesCount; i++) {
                secondaryTypesMap.remove(removedPackages[i]);
            }
        }
        if (VERBOSE) {
            Util.verbose("	- new secondary types map:"); //$NON-NLS-1$
            Iterator entries = secondaryTypesMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry)entries.next();
                String qualifiedName = (String)entry.getKey();
                Util.verbose("		+ " + qualifiedName + ':' + entry.getValue()); //$NON-NLS-1$
            }
        }
    }

    /**
     * Returns the existing element in the cache that is equal to the given element.
     */
    public synchronized IJavaElement getExistingElement(IJavaElement element) {
        return this.cache.getExistingElement(element);
    }

    /**
     * Remember the info for the jar binary type
     */
    protected synchronized void putJarTypeInfo(IJavaElement type, Object info) {
        this.cache.jarTypeCache.put(type, info);
    }

    /*
     * Perform search request to get all secondary types of a given project.
     * If not waiting for indexes and indexing is running, will return types found in current built indexes...
     */
    private Map secondaryTypesSearching(IJavaProject project, boolean waitForIndexes, IProgressMonitor monitor,
                                        final PerProjectInfo projectInfo) throws JavaModelException {
        if (VERBOSE || BasicSearchEngine.VERBOSE) {
            StringBuffer buffer = new StringBuffer("JavaModelManager.secondaryTypesSearch("); //$NON-NLS-1$
            buffer.append(project.getElementName());
            buffer.append(',');
            buffer.append(waitForIndexes);
            buffer.append(')');
            Util.verbose(buffer.toString());
        }

        final Hashtable secondaryTypes = new Hashtable(3);
        IRestrictedAccessTypeRequestor nameRequestor = new IRestrictedAccessTypeRequestor() {
            public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path,
                                   AccessRestriction access) {
                String key = packageName == null ? "" : new String(packageName); //$NON-NLS-1$
                HashMap types = (HashMap)secondaryTypes.get(key);
                if (types == null) types = new HashMap(3);
                types.put(new String(simpleTypeName), path);
                secondaryTypes.put(key, types);
            }
        };

        // Build scope using prereq projects but only source folders
        IPackageFragmentRoot[] allRoots = project.getAllPackageFragmentRoots();
        int length = allRoots.length, size = 0;
        IPackageFragmentRoot[] allSourceFolders = new IPackageFragmentRoot[length];
        for (int i = 0; i < length; i++) {
            if (allRoots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
                allSourceFolders[size++] = allRoots[i];
            }
        }
        if (size < length) {
            System.arraycopy(allSourceFolders, 0, allSourceFolders = new IPackageFragmentRoot[size], 0, size);
        }

        // Search all secondary types on scope
        new BasicSearchEngine(indexManager, (JavaProject)project)
                .searchAllSecondaryTypeNames(allSourceFolders, nameRequestor, waitForIndexes, monitor);

        // Build types from paths
        Iterator packages = secondaryTypes.values().iterator();
        while (packages.hasNext()) {
            HashMap types = (HashMap)packages.next();
            HashMap tempTypes = new HashMap(types.size());
            Iterator names = types.entrySet().iterator();
            while (names.hasNext()) {
                Map.Entry entry = (Map.Entry)names.next();
                String typeName = (String)entry.getKey();
                String path = (String)entry.getValue();
                names.remove();
                if (Util.isJavaLikeFileName(path)) {
                    File file = new File(path);// ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
                    ICompilationUnit unit = JavaModelManager.createCompilationUnitFrom(file, null);
                    IType type = unit.getType(typeName);
                    tempTypes.put(typeName, type);
                }
            }
            types.putAll(tempTypes);
        }

        // Store result in per project info cache if still null or there's still an indexing cache (may have been set by another thread...)
        if (projectInfo.secondaryTypes == null || projectInfo.secondaryTypes.get(INDEXED_SECONDARY_TYPES) != null) {
            projectInfo.secondaryTypes = secondaryTypes;
            if (VERBOSE || BasicSearchEngine.VERBOSE) {
                System.out.print(Thread.currentThread() + "	-> secondary paths stored in cache: ");  //$NON-NLS-1$
                System.out.println();
                Iterator entries = secondaryTypes.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry)entries.next();
                    String qualifiedName = (String)entry.getKey();
                    Util.verbose("		- " + qualifiedName + '-' + entry.getValue()); //$NON-NLS-1$
                }
            }
        }
        return projectInfo.secondaryTypes;
    }

    /*
 * Resets the temporary cache for newly created elements to null.
 */
    public void resetTemporaryCache() {
        this.temporaryCache.set(null);
    }

    public synchronized String cacheToString(String prefix) {
        return this.cache.toStringFillingRation(prefix);
    }

    public void closeZipFile(ZipFile zipFile) {
        if (zipFile == null) return;
        if (this.zipFiles.get() != null) {
            return; // zip file will be closed by call to flushZipFiles
        }
        try {
            if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
                System.out.println("(" + Thread.currentThread() + ") [JavaModelManager.closeZipFile(ZipFile)] Closing ZipFile on " +
                                   zipFile.getName()); //$NON-NLS-1$	//$NON-NLS-2$
            }
            zipFile.close();
        } catch (IOException e) {
            // problem occured closing zip file: cannot do much more
        }
    }

    public void setIndexManager(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    public synchronized BufferManager getDefaultBufferManager() {
        if (DEFAULT_BUFFER_MANAGER == null) {
            DEFAULT_BUFFER_MANAGER = new BufferManager();
        }
        return DEFAULT_BUFFER_MANAGER;
    }

    /**
     * Returns the Java element corresponding to the given resource, or
     * <code>null</code> if unable to associate the given resource
     * with a Java element.
     * <p>
     * The resource must be one of:<ul>
     *	<li>a project - the element returned is the corresponding <code>IJavaProject</code></li>
     *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
     *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
     *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
     *  <li>a folder - the element returned is the corresponding <code>IPackageFragmentRoot</code>
     *			or <code>IPackageFragment</code></li>
     *  <li>the workspace root resource - the element returned is the <code>IJavaModel</code></li>
     *	</ul>
     * <p>
     * Creating a Java element has the side effect of creating and opening all of the
     * element's parents if they are not yet open.
     */
    public static IJavaElement create(File resource, IJavaProject project) {
        if (resource == null) {
            return null;
        }
        if(resource.isFile()) {
            return createFromFile(resource, project);
        } else {
            return createFromDirectory(resource, project);
        }
//        int type = resource.getType();
//        switch (type) {
//            case IResource.PROJECT :
//                return JavaCore.create((IProject) resource);
//            case IResource.FILE :
//                return create((IFile) resource, project);
//            case IResource.FOLDER :
//                return create((IFolder) resource, project);
//            case IResource.ROOT :
//                return JavaCore.create((IWorkspaceRoot) resource);
//            default :
//                return null;
//        }
    }

    /**
     * Returns the Java element corresponding to the given file, its project being the given
     * project.
     * Returns <code>null</code> if unable to associate the given file
     * with a Java element.
     *
     * <p>The file must be one of:<ul>
     *	<li>a <code>.java</code> file - the element returned is the corresponding <code>ICompilationUnit</code></li>
     *	<li>a <code>.class</code> file - the element returned is the corresponding <code>IClassFile</code></li>
     *	<li>a ZIP archive (e.g. a <code>.jar</code>, a <code>.zip</code> file, etc.) - the element returned is the corresponding <code>IPackageFragmentRoot</code></li>
     *	</ul>
     * <p>
     * Creating a Java element has the side effect of creating and opening all of the
     * element's parents if they are not yet open.
     */
    public static IJavaElement createFromFile(File file, IJavaProject project) {
        if (file == null) {
            return null;
        }
//        if (project == null) {
//            project = JavaCore.create(file.getProject());
//        }

//        if (file.getFileExtension() != null) {
            String name = file.getName();
            if (Util.isJavaLikeFileName(name))
                return createCompilationUnitFrom(file, project);
            if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(name))
                return createClassFileFrom(file, project);
            return createJarPackageFragmentRootFrom(file, (JavaProject)project);
//        }
//        return null;
    }

    /**
     * Creates and returns a class file element for the given <code>.class</code> file,
     * its project being the given project. Returns <code>null</code> if unable
     * to recognize the class file.
     */
    public static IClassFile createClassFileFrom(File file, IJavaProject project ) {
        if (file == null) {
            return null;
        }
//        if (project == null) {
//            project = JavaCore.create(file.getProject());
//        }
        IPackageFragment pkg = (IPackageFragment) determineIfOnClasspath(file, (JavaProject)project);
        if (pkg == null) {
            // fix for 1FVS7WE
            // not on classpath - make the root its folder, and a default package
            PackageFragmentRoot root = (PackageFragmentRoot) project.getPackageFragmentRoot(file.getParent());
            pkg = root.getPackageFragment(CharOperation.NO_STRINGS);
        }
        return pkg.getClassFile(file.getName());
    }

    /**
     * Creates and returns a handle for the given JAR file, its project being the given project.
     * The Java model associated with the JAR's project may be
     * created as a side effect.
     * Returns <code>null</code> if unable to create a JAR package fragment root.
     * (for example, if the JAR file represents a non-Java resource)
     */
    public static IPackageFragmentRoot createJarPackageFragmentRootFrom(File file, JavaProject project) {
        if (file == null) {
            return null;
        }
//        if (project == null) {
//            project = JavaCore.create(file.getProject());
//        }

        // Create a jar package fragment root only if on the classpath
        IPath resourcePath = new Path(file.getPath());
        try {
            IClasspathEntry entry = project.getClasspathEntryFor(resourcePath);
            if (entry != null) {
                return project.getPackageFragmentRoot(file);
            }
        } catch (JavaModelException e) {
            // project doesn't exist: return null
        }
        return null;
    }
    /**
     * Returns the package fragment or package fragment root corresponding to the given folder,
     * its parent or great parent being the given project.
     * or <code>null</code> if unable to associate the given folder with a Java element.
     * <p>
     * Note that a package fragment root is returned rather than a default package.
     * <p>
     * Creating a Java element has the side effect of creating and opening all of the
     * element's parents if they are not yet open.
     */
    public static IJavaElement createFromDirectory(File folder, IJavaProject project) {
        if (folder == null) {
            return null;
        }
        IJavaElement element;
//        if (project == null) {
//            project = JavaCore.create(folder.getProject());
//            element = determineIfOnClasspath(folder, project);
//            if (element == null) {
//                // walk all projects and find one that have the given folder on its classpath
//                IJavaProject[] projects;
//                try {
//                    projects = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProjects();
//                } catch (JavaModelException e) {
//                    return null;
//                }
//                for (int i = 0, length = projects.length; i < length; i++) {
//                    project = projects[i];
//                    element = determineIfOnClasspath(folder, project);
//                    if (element != null)
//                        break;
//                }
//            }
//        } else {
            element = determineIfOnClasspath(folder, (JavaProject)project);
//        }
        return element;
    }

    public boolean forceBatchInitializations(boolean initAfterLoad) {
        return false;
    }

    public void setJavaProject(JavaProject javaProject) {
        this.javaProject = javaProject;
    }

    public JavaProject getJavaProject() {
        return javaProject;
    }

    /**
     * Define a zip cache object.
     */
    static class ZipCache {
        Object owner;
        private Map<IPath, ZipFile> map;

        ZipCache(Object owner) {
            this.map = new HashMap<>();
            this.owner = owner;
        }

        public void flush() {
            Thread currentThread = Thread.currentThread();
            for (ZipFile zipFile : this.map.values()) {
                try {
                    if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
                        System.out.println("(" + currentThread + ") [JavaModelManager.flushZipFiles()] Closing ZipFile on " +
                                           zipFile.getName()); //$NON-NLS-1$//$NON-NLS-2$
                    }
                    zipFile.close();
                } catch (IOException e) {
                    // problem occured closing zip file: cannot do much more
                }
            }
        }

        public ZipFile getCache(IPath path) {
            return this.map.get(path);
        }

        public void setCache(IPath path, ZipFile zipFile) {
            this.map.put(path, zipFile);
        }
    }

    public static class PerProjectInfo {
        static final         IJavaModelStatus NEED_RESOLUTION            = new JavaModelStatus();
        private static final int              JAVADOC_CACHE_INITIAL_SIZE = 10;
        //        public IProject          project;
        public Object            savedState;
        public boolean           triedRead;
        public IClasspathEntry[] rawClasspath;
        public IClasspathEntry[] referencedEntries;
        public IJavaModelStatus  rawClasspathStatus;
        public int     rawTimeStamp         = 0;
        public boolean writtingRawClasspath = false;
        public IClasspathEntry[] resolvedClasspath;
        public IJavaModelStatus  unresolvedEntryStatus;
        public Map               rootPathToRawEntries; // reverse map from a package fragment root's path to the raw entry
        public Map               rootPathToResolvedEntries; // map from a package fragment root's path to the resolved entry
        public IPath             outputLocation;

        //        public IEclipsePreferences preferences;
        public Hashtable options;
        public Hashtable secondaryTypes;
        public LRUCache  javadocCache;

        public PerProjectInfo(/*IProject project*/) {

            this.triedRead = false;
            this.savedState = null;
//            this.project = project;
            this.javadocCache = new LRUCache(JAVADOC_CACHE_INITIAL_SIZE);
        }

        public synchronized IClasspathEntry[] getResolvedClasspath() {
            if (this.unresolvedEntryStatus == NEED_RESOLUTION)
                return null;
            return this.resolvedClasspath;
        }

        public void forgetExternalTimestampsAndIndexes() {
//            IClasspathEntry[] classpath = this.resolvedClasspath;
//            if (classpath == null) return;
//            JavaModelManager manager = JavaModelManager.getJavaModelManager();
//            IndexManager indexManager = manager.indexManager;
//            Map externalTimeStamps = manager.deltaState.getExternalLibTimeStamps();
//            HashMap rootInfos = JavaModelManager.getDeltaState().otherRoots;
//            for (int i = 0, length = classpath.length; i < length; i++) {
//                IClasspathEntry entry = classpath[i];
//                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
//                    IPath path = entry.getPath();
//                    if (rootInfos.get(path) == null) {
//                        externalTimeStamps.remove(path);
//                        indexManager.removeIndex(
//                                path); // force reindexing on next reference (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=250083 )
//                    }
//                }
//            }
            throw new UnsupportedOperationException();
        }

        public void rememberExternalLibTimestamps() {
//            IClasspathEntry[] classpath = this.resolvedClasspath;
//            if (classpath == null) return;
//            Map externalTimeStamps = JavaModelManager.getJavaModelManager().deltaState.getExternalLibTimeStamps();
//            for (int i = 0, length = classpath.length; i < length; i++) {
//                IClasspathEntry entry = classpath[i];
//                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
//                    IPath path = entry.getPath();
//                    if (externalTimeStamps.get(path) == null) {
//                        Object target = JavaModel.getExternalTarget(path, true);
//                        if (target instanceof File) {
//                            long timestamp = DeltaProcessor.getTimeStamp((java.io.File)target);
//                            externalTimeStamps.put(path, new Long(timestamp));
//                        }
//                    }
//                }
//            }
            throw new UnsupportedOperationException();
        }

//        public synchronized ClasspathChange resetResolvedClasspath() {
//            // clear non-chaining jars cache and invalid jars cache
//            JavaModelManager.getJavaModelManager().resetClasspathListCache();
//
//            // null out resolved information
//            return setResolvedClasspath(null, null, null, null, this.rawTimeStamp, true/*add classpath change*/);
//            throw new UnsupportedOperationException();
//        }

//        private ClasspathChange setClasspath(IClasspathEntry[] newRawClasspath, IClasspathEntry[] referencedEntries,
//                                             IPath newOutputLocation, IJavaModelStatus newRawClasspathStatus,
//                                             IClasspathEntry[] newResolvedClasspath, Map newRootPathToRawEntries,
//                                             Map newRootPathToResolvedEntries, IJavaModelStatus newUnresolvedEntryStatus,
//                                             boolean addClasspathChange) {
//            ClasspathChange classpathChange = addClasspathChange ? addClasspathChange() : null;
//
//            if (referencedEntries != null) this.referencedEntries = referencedEntries;
//            if (this.referencedEntries == null) this.referencedEntries = org.eclipse.jdt.internal.core.ClasspathEntry.NO_ENTRIES;
//            this.rawClasspath = newRawClasspath;
//            this.outputLocation = newOutputLocation;
//            this.rawClasspathStatus = newRawClasspathStatus;
//            this.resolvedClasspath = newResolvedClasspath;
//            this.rootPathToRawEntries = newRootPathToRawEntries;
//            this.rootPathToResolvedEntries = newRootPathToResolvedEntries;
//            this.unresolvedEntryStatus = newUnresolvedEntryStatus;
//            this.javadocCache = new LRUCache(JAVADOC_CACHE_INITIAL_SIZE);
//
//            return classpathChange;
//        }
//
//        protected ClasspathChange addClasspathChange() {
////            // remember old info
////            JavaModelManager manager = JavaModelManager.getJavaModelManager();
////            ClasspathChange classpathChange =
////                    manager.deltaState.addClasspathChange(this.project, this.rawClasspath, this.outputLocation, this.resolvedClasspath);
////            return classpathChange;
//            throw new UnsupportedOperationException();
//        }
//
//        public ClasspathChange setRawClasspath(IClasspathEntry[] newRawClasspath, IPath newOutputLocation,
//                                               IJavaModelStatus newRawClasspathStatus) {
//            return setRawClasspath(newRawClasspath, null, newOutputLocation, newRawClasspathStatus);
//        }
//
//        public synchronized ClasspathChange setRawClasspath(IClasspathEntry[] newRawClasspath, IClasspathEntry[] referencedEntries,
//                                                            IPath newOutputLocation, IJavaModelStatus newRawClasspathStatus) {
//            this.rawTimeStamp++;
//            return setClasspath(newRawClasspath, referencedEntries, newOutputLocation, newRawClasspathStatus, null/*resolved classpath*/,
//                                null/*root to raw map*/, null/*root to resolved map*/, null/*unresolved status*/, true/*add classpath
//                                change*/);
//        }
//
//        public ClasspathChange setResolvedClasspath(IClasspathEntry[] newResolvedClasspath, Map newRootPathToRawEntries,
//                                                    Map newRootPathToResolvedEntries, IJavaModelStatus newUnresolvedEntryStatus,
//                                                    int timeStamp, boolean addClasspathChange) {
//            return setResolvedClasspath(newResolvedClasspath, null, newRootPathToRawEntries, newRootPathToResolvedEntries,
//                                        newUnresolvedEntryStatus, timeStamp, addClasspathChange);
//        }
//
//        public synchronized ClasspathChange setResolvedClasspath(IClasspathEntry[] newResolvedClasspath,
//                                                                 IClasspathEntry[] referencedEntries, Map newRootPathToRawEntries,
//                                                                 Map newRootPathToResolvedEntries,
//                                                                 IJavaModelStatus newUnresolvedEntryStatus, int timeStamp,
//                                                                 boolean addClasspathChange) {
//            if (this.rawTimeStamp != timeStamp)
//                return null;
//            return setClasspath(this.rawClasspath, referencedEntries, this.outputLocation, this.rawClasspathStatus, newResolvedClasspath,
//                                newRootPathToRawEntries, newRootPathToResolvedEntries, newUnresolvedEntryStatus, addClasspathChange);
//        }

        /**
         * Reads the classpath and caches the entries. Returns a two-dimensional array, where the number of elements in the row is fixed
         * to 2.
         * The first element is an array of raw classpath entries and the second element is an array of referenced entries that may have
         * been stored
         * by the client earlier. See {@link IJavaProject#getReferencedClasspathEntries()} for more details.
         */
        public synchronized IClasspathEntry[][] readAndCacheClasspath(JavaProject javaProject) {
//            // read file entries and update status
//            IClasspathEntry[][] classpath;
//            IJavaModelStatus status;
//            try {
//                classpath = javaProject.readFileEntriesWithException(null/*not interested in unknown elements*/);
//                status = JavaModelStatus.VERIFIED_OK;
//            } catch (CoreException e) {
//                classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH,
//                                                    ClasspathEntry.NO_ENTRIES};
//                status =
//                        new JavaModelStatus(
//                                IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
//                                Messages.bind(Messages.classpath_cannotReadClasspathFile, javaProject.getElementName()));
//            } catch (IOException e) {
//                classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH,
//                                                    ClasspathEntry.NO_ENTRIES};
//                if (Messages.file_badFormat.equals(e.getMessage()))
//                    status =
//                            new JavaModelStatus(
//                                    IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
//                                    Messages.bind(Messages.classpath_xmlFormatError, javaProject.getElementName(),
//                                                  Messages.file_badFormat));
//                else
//                    status =
//                            new JavaModelStatus(
//                                    IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
//                                    Messages.bind(Messages.classpath_cannotReadClasspathFile, javaProject.getElementName()));
//            } catch (ClasspathEntry.AssertionFailedException e) {
//                classpath = new IClasspathEntry[][]{JavaProject.INVALID_CLASSPATH,
//                                                    ClasspathEntry.NO_ENTRIES};
//                status =
//                        new JavaModelStatus(
//                                IJavaModelStatusConstants.INVALID_CLASSPATH_FILE_FORMAT,
//                                Messages.bind(Messages.classpath_illegalEntryInClasspathFile,
//                                              new String[]{javaProject.getElementName(), e.getMessage()}));
//            }
//
//            // extract out the output location
//            int rawClasspathLength = classpath[0].length;
//            IPath output = null;
//            if (rawClasspathLength > 0) {
//                IClasspathEntry entry = classpath[0][rawClasspathLength - 1];
//                if (entry.getContentKind() == ClasspathEntry.K_OUTPUT) {
//                    output = entry.getPath();
//                    IClasspathEntry[] copy = new IClasspathEntry[rawClasspathLength - 1];
//                    System.arraycopy(classpath[0], 0, copy, 0, copy.length);
//                    classpath[0] = copy;
//                }
//            }
//
//            // store new raw classpath, new output and new status, and null out resolved info
////            setRawClasspath(classpath[0], classpath[1], output, status);
//
//            return classpath;
            throw new UnsupportedOperationException();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Info for "); //$NON-NLS-1$
//            buffer.append(this.project.getFullPath());
            buffer.append("\nRaw classpath:\n"); //$NON-NLS-1$
            if (this.rawClasspath == null) {
                buffer.append("  <null>\n"); //$NON-NLS-1$
            } else {
                for (int i = 0, length = this.rawClasspath.length; i < length; i++) {
                    buffer.append("  "); //$NON-NLS-1$
                    buffer.append(this.rawClasspath[i]);
                    buffer.append('\n');
                }
            }
            buffer.append("Resolved classpath:\n"); //$NON-NLS-1$
            IClasspathEntry[] resolvedCP = this.resolvedClasspath;
            if (resolvedCP == null) {
                buffer.append("  <null>\n"); //$NON-NLS-1$
            } else {
                for (int i = 0, length = resolvedCP.length; i < length; i++) {
                    buffer.append("  "); //$NON-NLS-1$
                    buffer.append(resolvedCP[i]);
                    buffer.append('\n');
                }
            }
            buffer.append("Resolved classpath status: "); //$NON-NLS-1$
            if (this.unresolvedEntryStatus == NEED_RESOLUTION)
                buffer.append("NEED RESOLUTION"); //$NON-NLS-1$
            else
                buffer.append(this.unresolvedEntryStatus == null ? "<null>\n" : this.unresolvedEntryStatus.toString()); //$NON-NLS-1$
            buffer.append("Output location:\n  "); //$NON-NLS-1$
            if (this.outputLocation == null) {
                buffer.append("<null>"); //$NON-NLS-1$
            } else {
                buffer.append(this.outputLocation);
            }
            return buffer.toString();
        }

        public boolean writeAndCacheClasspath(
                JavaProject javaProject,
                final IClasspathEntry[] newRawClasspath,
                IClasspathEntry[] newReferencedEntries,
                final IPath newOutputLocation) throws JavaModelException {
            try {
                this.writtingRawClasspath = true;
                if (newReferencedEntries == null) newReferencedEntries = this.referencedEntries;

//                // write .classpath
//                if (!javaProject.writeFileEntries(newRawClasspath, newReferencedEntries, newOutputLocation)) {
//                    return false;
//                }
//                // store new raw classpath, new output and new status, and null out resolved info
//                setRawClasspath(newRawClasspath, newReferencedEntries, newOutputLocation, JavaModelStatus.VERIFIED_OK);
            } finally {
                this.writtingRawClasspath = false;
            }
            return true;
        }

        public boolean writeAndCacheClasspath(JavaProject javaProject, final IClasspathEntry[] newRawClasspath,
                                              final IPath newOutputLocation) throws JavaModelException {
            return writeAndCacheClasspath(javaProject, newRawClasspath, null, newOutputLocation);
        }

    }

    public static class PerWorkingCopyInfo implements IProblemRequestor {
        int useCount = 0;
        IProblemRequestor problemRequestor;
        CompilationUnit   workingCopy;

        public PerWorkingCopyInfo(CompilationUnit workingCopy, IProblemRequestor problemRequestor) {
            this.workingCopy = workingCopy;
            this.problemRequestor = problemRequestor;
        }

        public void acceptProblem(IProblem problem) {
            IProblemRequestor requestor = getProblemRequestor();
            if (requestor == null) return;
            requestor.acceptProblem(problem);
        }

        public void beginReporting() {
            IProblemRequestor requestor = getProblemRequestor();
            if (requestor == null) return;
            requestor.beginReporting();
        }

        public void endReporting() {
            IProblemRequestor requestor = getProblemRequestor();
            if (requestor == null) return;
            requestor.endReporting();
        }

        public IProblemRequestor getProblemRequestor() {
            if (this.problemRequestor == null && this.workingCopy.owner != null) {
                return this.workingCopy.owner.getProblemRequestor(this.workingCopy);
            }
            return this.problemRequestor;
        }

        public ICompilationUnit getWorkingCopy() {
            return this.workingCopy;
        }

        public boolean isActive() {
            IProblemRequestor requestor = getProblemRequestor();
            return requestor != null && requestor.isActive();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("Info for "); //$NON-NLS-1$
            buffer.append(((JavaElement)this.workingCopy).toStringWithAncestors());
            buffer.append("\nUse count = "); //$NON-NLS-1$
            buffer.append(this.useCount);
            buffer.append("\nProblem requestor:\n  "); //$NON-NLS-1$
            buffer.append(this.problemRequestor);
            if (this.problemRequestor == null) {
                IProblemRequestor requestor = getProblemRequestor();
                buffer.append("\nOwner problem requestor:\n  "); //$NON-NLS-1$
                buffer.append(requestor);
            }
            return buffer.toString();
        }
    }
}
