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

import org.eclipse.che.jdt.internal.core.util.Util;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class PackageFragmentRoot extends Openable implements IPackageFragmentRoot {

    /*
     * No source attachment property
     */
    public final static    String NO_SOURCE_ATTACHMENT          = ""; //$NON-NLS-1$
    /**
     * The delimiter between the source path and root path in the
     * attachment server property.
     */
    protected final static char   ATTACHMENT_PROPERTY_DELIMITER = '*';
    private final IPath path;

    protected PackageFragmentRoot(File folder, JavaProject project, JavaModelManager manager) {
        super(project, manager);
        path = new Path(folder.getPath());
    }

    /*
 * Returns the exclusion patterns from the classpath entry associated with this root.
 */
    public char[][] fullExclusionPatternChars() {
        try {
            if (getKind() != IPackageFragmentRoot.K_SOURCE) return null;
            ClasspathEntry entry = (ClasspathEntry)getRawClasspathEntry();
            if (entry == null) {
                return null;
            } else {
                return entry.fullExclusionPatternChars();
            }
        } catch (JavaModelException e) {
            return null;
        }
    }

    /*
     * Returns the inclusion patterns from the classpath entry associated with this root.
     */
    public char[][] fullInclusionPatternChars() {
        try {
            if (getKind() != IPackageFragmentRoot.K_SOURCE) return null;
            ClasspathEntry entry = (ClasspathEntry)getRawClasspathEntry();
            if (entry == null) {
                return null;
            } else {
                return entry.fullInclusionPatternChars();
            }
        } catch (JavaModelException e) {
            return null;
        }
    }

    @Override
    public void attachSource(IPath sourcePath, IPath rootPath, IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public void copy(IPath destination, int updateResourceFlags, int updateModelFlags, IClasspathEntry sibling, IProgressMonitor monitor)
            throws JavaModelException {

    }

    @Override
    public IPackageFragment createPackageFragment(String name, boolean force, IProgressMonitor monitor) throws JavaModelException {
        return null;
    }

    @Override
    public void delete(int updateResourceFlags, int updateModelFlags, IProgressMonitor monitor) throws JavaModelException {

    }

    @Override
    public int getKind() throws JavaModelException {
        return IPackageFragmentRoot.K_SOURCE;
    }

    @Override
    public Object[] getNonJavaResources() throws JavaModelException {
        return new Object[0];
    }

    @Override
    public IPackageFragment getPackageFragment(String packageName) {
        // tolerate package names with spaces (e.g. 'x . y') (http://bugs.eclipse.org/bugs/show_bug.cgi?id=21957)
        String[] pkgName = org.eclipse.jdt.internal.core.util.Util.getTrimmedSimpleNames(packageName);
        return getPackageFragment(pkgName);
    }

    @Override
    public IClasspathEntry getRawClasspathEntry() throws JavaModelException {
        IClasspathEntry rawEntry = null;
        JavaProject project = (JavaProject)getJavaProject();
        project.getResolvedClasspath(); // force the reverse rawEntry cache to be populated
        Map rootPathToRawEntries = project.resolvedClasspath().rawReverseMap;
        if (rootPathToRawEntries != null) {
            rawEntry = (IClasspathEntry)rootPathToRawEntries.get(getPath());
        }
        if (rawEntry == null) {
            throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH, this));
        }
        return rawEntry;
    }

    @Override
    public IClasspathEntry getResolvedClasspathEntry() throws JavaModelException {
        IClasspathEntry rawEntry = null;
        JavaProject project = (JavaProject)getJavaProject();
        project.getResolvedClasspath(); // force the reverse rawEntry cache to be populated
        Map rootPathToRawEntries = project.resolvedClasspath().rootPathToResolvedEntries;
        if (rootPathToRawEntries != null) {
            rawEntry = (IClasspathEntry)rootPathToRawEntries.get(getPath());
        }
        if (rawEntry == null) {
            throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH, this));
        }
        return rawEntry;
    }

    @Override
    public IPath getSourceAttachmentPath() throws JavaModelException {
        if (getKind() != K_BINARY) return null;

//        // 1) look source attachment property (set iff attachSource(...) was called
//        IPath path = getPath();
//        String serverPathString= Util.getSourceAttachmentProperty(path);
//        if (serverPathString != null) {
//            int index= serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
//            if (index < 0) {
//                // no root path specified
//                return new Path(serverPathString);
//            } else {
//                String serverSourcePathString= serverPathString.substring(0, index);
//                return new Path(serverSourcePathString);
//            }
//        }

        // 2) look at classpath entry
        IClasspathEntry entry = ((JavaProject)getParent()).getClasspathEntryFor(path);
        IPath sourceAttachmentPath;
        if (entry != null && (sourceAttachmentPath = entry.getSourceAttachmentPath()) != null)
            return sourceAttachmentPath;

        // 3) look for a recommendation
        entry = findSourceAttachmentRecommendation();
        if (entry != null && (sourceAttachmentPath = entry.getSourceAttachmentPath()) != null) {
            return sourceAttachmentPath;
        }

        return null;
    }

    @Override
    public IPath getSourceAttachmentRootPath() throws JavaModelException {
        if (getKind() != K_BINARY) return null;

//        // 1) look source attachment property (set iff attachSource(...) was called
//        IPath path = getPath();
//        String serverPathString= Util.getSourceAttachmentProperty(path);
//        if (serverPathString != null) {
//            int index = serverPathString.lastIndexOf(ATTACHMENT_PROPERTY_DELIMITER);
//            if (index == -1) return null;
//            String serverRootPathString= IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH;
//            if (index != serverPathString.length() - 1) {
//                serverRootPathString= serverPathString.substring(index + 1);
//            }
//            return new Path(serverRootPathString);
//        }

        // 2) look at classpath entry
        IClasspathEntry entry = ((JavaProject)getParent()).getClasspathEntryFor(path);
        IPath sourceAttachmentRootPath;
        if (entry != null && (sourceAttachmentRootPath = entry.getSourceAttachmentRootPath()) != null)
            return sourceAttachmentRootPath;

        // 3) look for a recomendation
        entry = findSourceAttachmentRecommendation();
        if (entry != null && (sourceAttachmentRootPath = entry.getSourceAttachmentRootPath()) != null)
            return sourceAttachmentRootPath;

        return null;
    }

    private IClasspathEntry findSourceAttachmentRecommendation() {
//        try {
//            IPath rootPath = getPath();
//            IClasspathEntry entry;
//
//            // try on enclosing project first
//            JavaProject parentProject = (JavaProject) getJavaProject();
//            try {
//                entry = parentProject.getClasspathEntryFor(rootPath);
//                if (entry != null) {
//                    Object target = JavaModel.getTarget(entry.getSourceAttachmentPath(), true);
//                    if (target != null) {
//                        return entry;
//                    }
//                }
//            } catch(JavaModelException e){
//                // ignore
//            }
//
//            // iterate over all projects
//            IJavaModel model = getJavaModel();
//            IJavaProject[] jProjects = model.getJavaProjects();
//            for (int i = 0, max = jProjects.length; i < max; i++){
//                JavaProject jProject = (JavaProject) jProjects[i];
//                if (jProject == parentProject) continue; // already done
//                try {
//                    entry = jProject.getClasspathEntryFor(rootPath);
//                    if (entry != null){
//                        Object target = JavaModel.getTarget(entry.getSourceAttachmentPath(), true);
//                        if (target != null) {
//                            return entry;
//                        }
//                    }
//                } catch(JavaModelException e){
//                    // ignore
//                }
//            }
//        } catch(JavaModelException e){
//            // ignore
//        }

        return null;
    }

    @Override
    public boolean isArchive() {
        return false;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public void move(IPath destination, int updateResourceFlags, int updateModelFlags, IClasspathEntry sibling, IProgressMonitor monitor)
            throws JavaModelException {

    }

    @Override
    protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, File underlyingResource)
            throws JavaModelException {
        ((PackageFragmentRootInfo)info).setRootKind(determineKind(underlyingResource));
        return computeChildren(info, underlyingResource);
    }

    /**
     * Returns the root's kind - K_SOURCE or K_BINARY, defaults
     * to K_SOURCE if it is not on the classpath.
     *
     * @throws JavaModelException
     *         if the project and root do
     *         not exist.
     */
    protected int determineKind(File underlyingResource) throws JavaModelException {
        IClasspathEntry entry = ((JavaProject)getJavaProject()).getClasspathEntryFor(new Path(underlyingResource.getAbsolutePath()));
        if (entry != null) {
            return entry.getContentKind();
        }
        return IPackageFragmentRoot.K_SOURCE;
    }

    public PackageFragment getPackageFragment(String[] pkgName) {
        return new PackageFragment(this, manager, pkgName);
    }

    /**
     * Compute the package fragment children of this package fragment root.
     *
     * @throws JavaModelException
     *         The resource associated with this package fragment root does not exist
     */
    protected boolean computeChildren(OpenableElementInfo info, File underlyingResource) throws JavaModelException {
        // Note the children are not opened (so not added to newElements) for a regular package fragment root
        // However they are opened for a Jar package fragment root (see JarPackageFragmentRoot#computeChildren)
        try {
            // the underlying resource may be a folder or a project (in the case that the project folder
            // is actually the package fragment root)
            if (underlyingResource.isDirectory() || underlyingResource.isFile()) {
                ArrayList vChildren = new ArrayList(5);
//                IContainer rootFolder = (IContainer) underlyingResource;
                char[][] inclusionPatterns = fullInclusionPatternChars();
                char[][] exclusionPatterns = fullExclusionPatternChars();
                computeFolderChildren(underlyingResource,
                                      !Util.isExcluded(new Path(underlyingResource.getAbsolutePath()), inclusionPatterns, exclusionPatterns,
                                                       true), CharOperation.NO_STRINGS, vChildren, inclusionPatterns, exclusionPatterns);
                IJavaElement[] children = new IJavaElement[vChildren.size()];
                vChildren.toArray(children);
                info.setChildren(children);
            }
        } catch (JavaModelException e) {
            //problem resolving children; structure remains unknown
            info.setChildren(new IJavaElement[]{});
            throw e;
        }
        return true;
    }

    /**
     * Starting at this folder, create package fragments and add the fragments that are not exclused
     * to the collection of children.
     *
     * @throws JavaModelException
     *         The resource associated with this package fragment does not exist
     */
    protected void computeFolderChildren(File folder, boolean isIncluded, String[] pkgName, ArrayList vChildren, char[][] inclusionPatterns,
                                         char[][] exclusionPatterns) throws JavaModelException {

        if (isIncluded) {
            IPackageFragment pkg = getPackageFragment(pkgName);
            vChildren.add(pkg);
        }
        try {
            File[] members = folder.listFiles();
            boolean hasIncluded = isIncluded;
            int length = members.length;
            if (length > 0) {
                // if package fragment root refers to folder in another IProject, then
                // folder.getProject() is different than getJavaProject().getProject()
                // use the other java project's options to verify the name
//                IJavaProject otherJavaProject = JavaCore.create(folder.getProject());
                JavaProject javaProject = (JavaProject)getJavaProject();
                String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
                String complianceLevel = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
//                JavaModelManager manager = JavaModelManager.getJavaModelManager();
                for (int i = 0; i < length; i++) {
                    File member = members[i];
                    String memberName = member.getName();
                    if (member.isDirectory()) {
                        // recurse into sub folders even even parent not included as a sub folder could be included
//                            // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=65637)
                        if (Util.isValidFolderNameForPackage(memberName, sourceLevel, complianceLevel)) {
                            // eliminate binary output only if nested inside direct subfolders
//                                if (javaProject.contains(member)) {
                            String[] newNames = Util.arrayConcat(pkgName, manager.intern(memberName));
                            boolean isMemberIncluded = false;//!Util.isExcluded(member, inclusionPatterns, exclusionPatterns);
                            computeFolderChildren(member, isMemberIncluded, newNames, vChildren, inclusionPatterns, exclusionPatterns);
//                                }
                        }
                    } else {
                        if (!hasIncluded
                            && Util.isValidCompilationUnitName(memberName, sourceLevel, complianceLevel)
                                /*&& !Util.isExcluded(member, inclusionPatterns, exclusionPatterns)*/) {
                            hasIncluded = true;
                            IPackageFragment pkg = getPackageFragment(pkgName);
                            vChildren.add(pkg);
                        }
                    }
//                    switch(member.getType()) {
//
//                        case IResource.FOLDER:
//                            // recurse into sub folders even even parent not included as a sub folder could be included
//                            // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=65637)
//                            if (Util.isValidFolderNameForPackage(memberName, sourceLevel, complianceLevel)) {
//                                // eliminate binary output only if nested inside direct subfolders
//                                if (javaProject.contains(member)) {
//                                    String[] newNames = Util.arrayConcat(pkgName, manager.intern(memberName));
//                                    boolean isMemberIncluded = !Util.isExcluded(member, inclusionPatterns, exclusionPatterns);
//                                    computeFolderChildren((IFolder) member, isMemberIncluded, newNames, vChildren, inclusionPatterns,
// exclusionPatterns);
//                                }
//                            }
//                            break;
//                        case IResource.FILE:
//                            // inclusion filter may only include files, in which case we still want to include the immediate parent
// package (lazily)
//                            if (!hasIncluded
//                                && Util.isValidCompilationUnitName(memberName, sourceLevel, complianceLevel)
//                                && !Util.isExcluded(member, inclusionPatterns, exclusionPatterns)) {
//                                hasIncluded = true;
//                                IPackageFragment pkg = getPackageFragment(pkgName);
//                                vChildren.add(pkg);
//                            }
//                            break;
//                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw new JavaModelException(e,
                                         IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST); // could be thrown by ElementTree when path
                                         // is not found
        } catch (CoreException e) {
            throw new JavaModelException(e);
        }
    }

    @Override
    protected File resource(PackageFragmentRoot root) {
        return path.toFile();
    }

    @Override
    protected IStatus validateExistence(File underlyingResource) {
        return JavaModelStatus.VERIFIED_OK;
    }

    @Override
    public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
        switch (token.charAt(0)) {
            case JEM_PACKAGEFRAGMENT:
                String[] pkgName;
                if (memento.hasMoreTokens()) {
                    token = memento.nextToken();
                    char firstChar = token.charAt(0);
                    if (firstChar == JEM_CLASSFILE || firstChar == JEM_COMPILATIONUNIT || firstChar == JEM_COUNT) {
                        pkgName = CharOperation.NO_STRINGS;
                    } else {
                        pkgName = Util.splitOn('.', token, 0, token.length());
                        token = null;
                    }
                } else {
                    pkgName = CharOperation.NO_STRINGS;
                    token = null;
                }
                JavaElement pkg = getPackageFragment(pkgName);
                if (token == null) {
                    return pkg.getHandleFromMemento(memento, owner);
                } else {
                    return pkg.getHandleFromMemento(token, memento, owner);
                }
        }
        return null;
    }

    @Override
    protected char getHandleMementoDelimiter() {
        return JavaElement.JEM_PACKAGEFRAGMENTROOT;
    }

    /**
     * @see org.eclipse.jdt.internal.core.JavaElement#getHandleMemento(StringBuffer)
     */
    protected void getHandleMemento(StringBuffer buff) {
        IPath path;
        File underlyingResource = resource();
        if (underlyingResource != null) {
            // internal jar or regular root
//            if (resource().getProject().equals(getJavaProject().getProject())) {
//                path = underlyingResource.getProjectRelativePath();
//            } else {
            path = new Path(underlyingResource.getAbsolutePath());
//            }
        } else {
            // external jar
            path = getPath();
        }
        ((JavaElement)getParent()).getHandleMemento(buff);
        buff.append(getHandleMementoDelimiter());
        escapeMementoName(buff, path.toString());
    }

    @Override
    public int getElementType() {
        return PACKAGE_FRAGMENT_ROOT;
    }

    @Override
    public IPath getPath() {
        return path;
    }

    public IPath internalPath() {
        return path;
    }

    SourceMapper createSourceMapper(IPath sourcePath, IPath rootPath) throws JavaModelException {
        IClasspathEntry entry = ((JavaProject)getParent()).getClasspathEntryFor(getPath());
        String encoding = (entry == null) ? null : ((ClasspathEntry)entry).getSourceAttachmentEncoding();
        SourceMapper mapper = new SourceMapper(
                sourcePath,
                rootPath == null ? null : rootPath.toOSString(),
                getJavaProject().getOptions(true),
// cannot use workspace options if external jar is 1.5 jar and workspace options are 1.4 options
                encoding,
                manager);

        return mapper;
    }

    public int hashCode() {
        return resource().getAbsolutePath().hashCode();
    }

    /**
     * Compares two objects for equality;
     * for <code>PackageFragmentRoot</code>s, equality is having the
     * same parent, same resources, and occurrence count.
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PackageFragmentRoot))
            return false;
        PackageFragmentRoot other = (PackageFragmentRoot)o;
        return resource().equals(other.resource()) &&
               this.parent.equals(other.parent);
    }

    /**
     * @see org.eclipse.jdt.internal.core.JavaElement
     */
    public SourceMapper getSourceMapper() {
        SourceMapper mapper;
        try {
            PackageFragmentRootInfo rootInfo = (PackageFragmentRootInfo)getElementInfo();
            mapper = rootInfo.getSourceMapper();
            if (mapper == null) {
                // first call to this method
                IPath sourcePath = getSourceAttachmentPath();
                IPath rootPath = getSourceAttachmentRootPath();
                if (sourcePath == null)
                    mapper = createSourceMapper(getPath(), rootPath); // attach root to itself
                else
                    mapper = createSourceMapper(sourcePath, rootPath);
                rootInfo.setSourceMapper(mapper);
            }
        } catch (JavaModelException e) {
            // no source can be attached
            mapper = null;
        }
        return mapper;
    }

    /**
     * Returns a new element info for this element.
     */
    protected Object createElementInfo() {
        return new PackageFragmentRootInfo();
    }

    /*
 * A version of getKind() that doesn't update the timestamp of the info in the Java model cache
 * to speed things up
 */
    int internalKind() throws JavaModelException {
//        org.eclipse.jdt.internal.core.JavaModelManager manager = org.eclipse.jdt.internal.core.JavaModelManager.getJavaModelManager();
        PackageFragmentRootInfo info = (PackageFragmentRootInfo)manager.peekAtInfo(this);
        if (info == null) {
            info = (PackageFragmentRootInfo)openWhenClosed(createElementInfo(), false, null);
        }
        return info.getRootKind();
    }
}
