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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.ext.java.shared.JarEntry.JarEntryType;
import org.eclipse.che.ide.ext.java.shared.OpenDeclarationDescriptor;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaProject;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.PackageFragment;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.JarEntryDirectory;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.core.JarEntryResource;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.che.jdt.javadoc.JavaElementLabels;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class JavaNavigation {
    private static final Logger               LOG           = LoggerFactory.getLogger(JavaNavigation.class);
    private static final ArrayList<JarEntry>  NO_ENTRIES    = new ArrayList<>(1);
    private static       Comparator<JarEntry> comparator    = new Comparator<JarEntry>() {
        @Override
        public int compare(JarEntry o1, JarEntry o2) {
            if (o1.getType() == JarEntryType.PACKAGE && o2.getType() != JarEntryType.PACKAGE) {
                return 1;
            }

            if (o2.getType() == JarEntryType.PACKAGE && o1.getType() != JarEntryType.PACKAGE) {
                return 1;
            }

            if (o1.getType() == JarEntryType.CLASS_FILE && o2.getType() != JarEntryType.CLASS_FILE) {
                return 1;
            }

            if (o1.getType() != JarEntryType.CLASS_FILE && o2.getType() == JarEntryType.CLASS_FILE) {
                return 1;
            }

            if (o1.getType() == JarEntryType.FOLDER && o2.getType() != JarEntryType.FOLDER) {
                return 1;
            }

            if (o1.getType() != JarEntryType.FOLDER && o2.getType() == JarEntryType.FOLDER) {
                return 1;
            }

            if (o1.getType() == JarEntryType.FILE && o2.getType() != JarEntryType.FILE) {
                return -1;
            }

            if (o1.getType() != JarEntryType.FILE && o2.getType() == JarEntryType.FILE) {
                return -1;
            }


            if (o1.getType() == o2.getType()) {
                return o1.getName().compareTo(o2.getName());
            }

            return 0;
        }
    };
    private              Gson                 gson          = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private              boolean              fFoldPackages = true;
    private SourcesFromBytecodeGenerator sourcesGenerator;

    @Inject
    public JavaNavigation(SourcesFromBytecodeGenerator sourcesGenerator) {
        this.sourcesGenerator = sourcesGenerator;
    }

    /**
     * Utility method to concatenate two arrays.
     *
     * @param a1
     *         the first array
     * @param a2
     *         the second array
     * @return the concatenated array
     */
    protected static Object[] concatenate(Object[] a1, Object[] a2) {
        int a1Len = a1.length;
        int a2Len = a2.length;
        if (a1Len == 0) return a2;
        if (a2Len == 0) return a1;
        Object[] res = new Object[a1Len + a2Len];
        System.arraycopy(a1, 0, res, 0, a1Len);
        System.arraycopy(a2, 0, res, a1Len, a2Len);
        return res;
    }

    private static IPackageFragment getFolded(IJavaElement[] children, IPackageFragment pack) throws JavaModelException {
        while (isEmpty(pack)) {
            IPackageFragment collapsed = findSinglePackageChild(pack, children);
            if (collapsed == null) {
                return pack;
            }
            pack = collapsed;
        }
        return pack;
    }

    private static boolean isEmpty(IPackageFragment fragment) throws JavaModelException {
        return !fragment.containsJavaResources() && fragment.getNonJavaResources().length == 0;
    }

    private static IPackageFragment findSinglePackageChild(IPackageFragment fragment, IJavaElement[] children) {
        String prefix = fragment.getElementName() + '.';
        int prefixLen = prefix.length();
        IPackageFragment found = null;
        for (int i = 0; i < children.length; i++) {
            IJavaElement element = children[i];
            String name = element.getElementName();
            if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('.', prefixLen) == -1) {
                if (found == null) {
                    found = (IPackageFragment)element;
                } else {
                    return null;
                }
            }
        }
        return found;
    }

    public OpenDeclarationDescriptor findDeclaration(IJavaProject project, String fqn, int offset) throws JavaModelException {
        IJavaElement originalElement = null;
        IType type = project.findType(fqn);
        ICodeAssist codeAssist;
        if(type.isBinary()){
            codeAssist = type.getClassFile();
        } else {
            codeAssist = type.getCompilationUnit();
        }

        IJavaElement[] elements = null;
        if(codeAssist != null) {
            elements = codeAssist.codeSelect(offset, 0);
        }

        if(elements != null && elements.length > 0){
            originalElement = elements[0];
        }
        IJavaElement element = originalElement;
        while (element != null) {
            if (element instanceof ICompilationUnit) {
                ICompilationUnit unit = ((ICompilationUnit)element).getPrimary();
                return compilationUnitNavigation(unit, originalElement);
            }

            if (element instanceof IClassFile) {
                return classFileNavigation((IClassFile)element, originalElement);
            }
            element = element.getParent();
        }
        return null;
    }

    public List<Jar> getProjectDependecyJars(IJavaProject project) throws JavaModelException {
        List<Jar> jars = new ArrayList<>();
        for (IPackageFragmentRoot fragmentRoot : project.getAllPackageFragmentRoots()) {
            if (fragmentRoot instanceof JarPackageFragmentRoot) {
                Jar jar = DtoFactory.getInstance().createDto(Jar.class);
                jar.setId(fragmentRoot.hashCode());
                jar.setName(fragmentRoot.getElementName());
                jars.add(jar);
            }
        }

        return jars;
    }

    public List<JarEntry> getPackageFragmentRootContent(IJavaProject project, int hash) throws JavaModelException {
        IPackageFragmentRoot packageFragmentRoot = getPackageFragmentRoot(project, hash);

        if (packageFragmentRoot == null) {
            return NO_ENTRIES;
        }

        Object[] rootContent = getPackageFragmentRootContent(packageFragmentRoot);

        return convertToJarEntry(rootContent, packageFragmentRoot);
    }

    private IPackageFragmentRoot getPackageFragmentRoot(IJavaProject project, int hash) throws JavaModelException {
        IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
        IPackageFragmentRoot packageFragmentRoot = null;
        for (IPackageFragmentRoot root : roots) {
            if (root.hashCode() == hash) {
                packageFragmentRoot = root;
                break;
            }
        }
        return packageFragmentRoot;
    }

    private List<JarEntry> convertToJarEntry(Object[] rootContent, IPackageFragmentRoot root) throws JavaModelException {
        List<JarEntry> result = new ArrayList<>();
        for (Object o : rootContent) {
            if (o instanceof IPackageFragment) {
                JarEntry entry = DtoFactory.getInstance().createDto(JarEntry.class);
                IPackageFragment packageFragment = (IPackageFragment)o;
                entry.setName(getSpecificText((IJavaElement)o));
                entry.setPath(packageFragment.getElementName());
                entry.setType(JarEntryType.PACKAGE);
                result.add(entry);
            }

            if (o instanceof IClassFile) {
                JarEntry entry = getJarClass((IClassFile)o);
                result.add(entry);
            }

            if (o instanceof JarEntryResource) {
                result.add(getJarEntryResource((JarEntryResource)o));
            }
        }
        Collections.sort(result, comparator);
        return result;
    }

    private JarEntry getJarClass(IClassFile classFile) {
        JarEntry entry = DtoFactory.getInstance().createDto(JarEntry.class);
        entry.setType(JarEntryType.CLASS_FILE);
        entry.setName(classFile.getElementName());
        entry.setPath(classFile.getType().getFullyQualifiedName());
        return entry;
    }

    private String getSpecificText(IJavaElement element) {
        if (element instanceof IPackageFragment) {
            IPackageFragment fragment = (IPackageFragment)element;
            Object parent = getHierarchicalPackageParent(fragment);
            if (parent instanceof IPackageFragment) {
                return getNameDelta((IPackageFragment)parent, fragment);
            }
        }

        return JavaElementLabels.getElementLabel(element, 0);
    }

    private String getNameDelta(IPackageFragment parent, IPackageFragment fragment) {
        String prefix = parent.getElementName() + '.';
        String fullName = fragment.getElementName();
        if (fullName.startsWith(prefix)) {
            return fullName.substring(prefix.length());
        }
        return fullName;
    }

    public Object getHierarchicalPackageParent(IPackageFragment child) {
        String name = child.getElementName();
        IPackageFragmentRoot parent = (IPackageFragmentRoot)child.getParent();
        int index = name.lastIndexOf('.');
        if (index != -1) {
            String realParentName = name.substring(0, index);
            IPackageFragment element = parent.getPackageFragment(realParentName);
            if (element.exists()) {
                try {
                    if (fFoldPackages && isEmpty(element) && findSinglePackageChild(element, parent.getChildren()) != null) {
                        return getHierarchicalPackageParent(element);
                    }
                } catch (JavaModelException e) {
                    // ignore
                }
                return element;
            } /*else { // bug 65240
                IResource resource= element.getResource();
                if (resource != null) {
                    return resource;
                }
            }*/
        }
//        if (parent.getResource() instanceof IProject) {
//            return parent.getJavaProject();
//        }
        return parent;
    }

    private JarEntry getJarEntryResource(JarEntryResource resource) {
        JarEntry entry = DtoFactory.getInstance().createDto(JarEntry.class);
        if (resource instanceof JarEntryDirectory) {
            entry.setType(JarEntryType.FOLDER);
        }
        if (resource instanceof JarEntryFile) {
            entry.setType(JarEntryType.FILE);
        }
        entry.setName(resource.getName());
        entry.setPath(resource.getFullPath().toOSString());
        return entry;
    }

    protected Object[] getPackageFragmentRootContent(IPackageFragmentRoot root) throws JavaModelException {

        // hierarchical package mode
        ArrayList<Object> result = new ArrayList<>();
        getHierarchicalPackageChildren(root, null, result);
        Object[] nonJavaResources = root.getNonJavaResources();
        for (int i = 0; i < nonJavaResources.length; i++) {
            result.add(nonJavaResources[i]);
        }
        return result.toArray();
    }

    /* (non-Javadoc)
 * @see org.eclipse.jdt.ui.StandardJavaElementContentProvider#getPackageContent(org.eclipse.jdt.core.IPackageFragment)
 */
    protected Object[] getPackageContent(IPackageFragment fragment) throws JavaModelException {

        // hierarchical package mode
        ArrayList<Object> result = new ArrayList<Object>();

        getHierarchicalPackageChildren((IPackageFragmentRoot)fragment.getParent(), fragment, result);
        IClassFile[] classFiles = fragment.getClassFiles();
        List<IClassFile> filtered = new ArrayList<>();
        //filter inner classes
        for (IClassFile classFile : classFiles) {
            if (!classFile.getElementName().contains("$")) {
                filtered.add(classFile);
            }
        }
        Object[] nonPackages = concatenate(filtered.toArray(), fragment.getNonJavaResources());
        if (result.isEmpty())
            return nonPackages;
        Collections.addAll(result, nonPackages);
        return result.toArray();
    }

    /**
     * Returns the hierarchical packages inside a given fragment or root.
     *
     * @param parent
     *         the parent package fragment root
     * @param fragment
     *         the package to get the children for or 'null' to get the children of the root
     * @param result
     *         Collection where the resulting elements are added
     * @throws JavaModelException
     *         if fetching the children fails
     */
    private void getHierarchicalPackageChildren(IPackageFragmentRoot parent, IPackageFragment fragment, Collection<Object> result)
            throws JavaModelException {
        IJavaElement[] children = parent.getChildren();
        String prefix = fragment != null ? fragment.getElementName() + '.' : ""; //$NON-NLS-1$
        int prefixLen = prefix.length();
        for (int i = 0; i < children.length; i++) {
            IPackageFragment curr = (IPackageFragment)children[i];
            String name = curr.getElementName();
            if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('.', prefixLen) == -1) {
                if (fFoldPackages) {
                    curr = getFolded(children, curr);
                }
                result.add(curr);
            } else if (fragment == null && curr.isDefaultPackage()) {
                IJavaElement[] currChildren = curr.getChildren();
                if (currChildren != null && currChildren.length >= 1) {
                    result.add(curr);
                }
            }
        }
    }

    private OpenDeclarationDescriptor classFileNavigation(IClassFile classFile, IJavaElement element) throws JavaModelException {
        OpenDeclarationDescriptor dto = DtoFactory.getInstance().createDto(OpenDeclarationDescriptor.class);
        dto.setPath(classFile.getType().getFullyQualifiedName());
        dto.setLibId(classFile.getAncestor(IPackageFragmentRoot.PACKAGE_FRAGMENT_ROOT).hashCode());
        dto.setBinary(true);
        if (classFile.getSourceRange() != null) {
            if (element instanceof ISourceReference) {
                ISourceRange nameRange = ((ISourceReference)element).getNameRange();
                dto.setOffset(nameRange.getOffset());
                dto.setLength(nameRange.getLength());
            }
        }
        return dto;
    }

    private OpenDeclarationDescriptor compilationUnitNavigation(ICompilationUnit unit, IJavaElement element)
            throws JavaModelException {
        OpenDeclarationDescriptor dto = DtoFactory.getInstance().createDto(OpenDeclarationDescriptor.class);
        String absolutePath = unit.getPath().toOSString();
        dto.setPath(absolutePath);
        dto.setBinary(false);
        if (element instanceof ISourceReference) {
            ISourceRange nameRange = ((ISourceReference)element).getNameRange();
            dto.setOffset(nameRange.getOffset());
            dto.setLength(nameRange.getLength());
        }

        return dto;
    }

    private Object[] findJarDirectoryChildren(JarEntryDirectory directory, String path) {
        String directoryPath = directory.getFullPath().toOSString();
        if (directoryPath.equals(path)) {
            return directory.getChildren();
        }
        if (path.startsWith(directoryPath)) {
            for (IJarEntryResource resource : directory.getChildren()) {
                String childrenPath = resource.getFullPath().toOSString();
                if (childrenPath.equals(path)) {
                    return resource.getChildren();
                }
                if (path.startsWith(childrenPath) && resource instanceof JarEntryDirectory) {
                    findJarDirectoryChildren((JarEntryDirectory)resource, path);
                }
            }
        }
        return null;
    }

    public List<JarEntry> getChildren(IJavaProject project, int rootId, String path) throws JavaModelException {
        IPackageFragmentRoot root = getPackageFragmentRoot(project, rootId);
        if (root == null) {
            return NO_ENTRIES;
        }

        if (path.startsWith("/")) {
            // jar file and folders
            Object[] resources = root.getNonJavaResources();
            for (Object resource : resources) {
                if (resource instanceof JarEntryDirectory) {
                    JarEntryDirectory directory = (JarEntryDirectory)resource;
                    Object[] children = findJarDirectoryChildren(directory, path);
                    if (children != null) {
                        return convertToJarEntry(children, root);
                    }
                }
            }

        } else {
            // packages and class files
            IPackageFragment fragment = root.getPackageFragment(path);
            if (fragment == null) {
                return NO_ENTRIES;
            }
            return convertToJarEntry(getPackageContent(fragment), root);
        }
        return NO_ENTRIES;
    }

    public String getContent(IJavaProject project, int rootId, String path) throws CoreException {
        IPackageFragmentRoot root = getPackageFragmentRoot(project, rootId);
        if (root == null) {
            return null;
        }

        if (path.startsWith("/")) {
            //non java file
            if (root instanceof JarPackageFragmentRoot) {
                JarPackageFragmentRoot jarPackageFragmentRoot = (JarPackageFragmentRoot)root;
                ZipFile jar = null;
                try {
                    jar = jarPackageFragmentRoot.getJar();
                    ZipEntry entry = jar.getEntry(path.substring(1));
                    if (entry != null) {
                        try (InputStream stream = jar.getInputStream(entry)) {
                            return IoUtil.readStream(stream);
                        } catch (IOException e) {
                            LOG.error("Can't read file content: " + entry.getName(), e);
                        }
                    }
                } finally {
                    if (jar != null) {
                        JavaModelManager.getJavaModelManager().closeZipFile(jar);
                    }
                }
            }
            Object[] resources = root.getNonJavaResources();

            for (Object resource : resources) {
                if (resource instanceof JarEntryFile) {
                    JarEntryFile file = (JarEntryFile)resource;
                    if (file.getFullPath().toOSString().equals(path)) {
                        return readFileContent(file);
                    }
                }
                if (resource instanceof JarEntryDirectory) {
                    JarEntryDirectory directory = (JarEntryDirectory)resource;
                    JarEntryFile file = findJarFile(directory, path);
                    if (file != null) {
                        return readFileContent(file);
                    }
                }
            }
        } else {
            //java class or file
            IType type = project.findType(path);
            if (type != null && type.isBinary()) {
                IClassFile classFile = type.getClassFile();
                if (classFile.getSourceRange() != null) {
                    return classFile.getSource();
                } else {
                    return sourcesGenerator.generateSource(classFile.getType());
                }
            }
        }
        return null;
    }

    private String readFileContent(JarEntryFile file) {
        try (InputStream stream = (file.getContents())) {
            return IoUtil.readStream(stream);
        } catch (IOException | CoreException e) {
            LOG.error("Can't read file content: " + file.getFullPath(), e);
        }
        return null;
    }

    private JarEntryFile findJarFile(JarEntryDirectory directory, String path) {
        for (IJarEntryResource children : directory.getChildren()) {
            if (children.isFile() && children.getFullPath().toOSString().equals(path)) {
                return (JarEntryFile)children;
            }
            if (!children.isFile()) {
                JarEntryFile file = findJarFile((JarEntryDirectory)children, path);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }

    public JarEntry getEntry(IJavaProject project, int rootId, String path) throws CoreException {
        IPackageFragmentRoot root = getPackageFragmentRoot(project, rootId);
        if (root == null) {
            return null;
        }
        if (path.startsWith("/")) {

            JarPackageFragmentRoot jarPackageFragmentRoot = (JarPackageFragmentRoot)root;
            ZipFile jar = null;
            try {
                jar = jarPackageFragmentRoot.getJar();
                ZipEntry entry = jar.getEntry(path.substring(1));
                if (entry != null) {
                    JarEntry result = DtoFactory.getInstance().createDto(JarEntry.class);
                    result.setType(JarEntryType.FILE);
                    result.setPath(path);
                    result.setName(entry.getName().substring(entry.getName().lastIndexOf("/") + 1));
                    return result;
                }
            } finally {
                if (jar != null) {
                    JavaModelManager.getJavaModelManager().closeZipFile(jar);
                }
            }

            Object[] resources = root.getNonJavaResources();

            for (Object resource : resources) {
                if (resource instanceof JarEntryFile) {
                    JarEntryFile file = (JarEntryFile)resource;
                    if (file.getFullPath().toOSString().equals(path)) {
                        return getJarEntryResource(file);
                    }
                }
                if (resource instanceof JarEntryDirectory) {
                    JarEntryDirectory directory = (JarEntryDirectory)resource;
                    JarEntryFile file = findJarFile(directory, path);
                    if (file != null) {
                        return getJarEntryResource(file);
                    }
                }
            }

        } else {
            //java class or file
            IType type = project.findType(path);
            if (type != null && type.isBinary()) {
                IClassFile classFile = type.getClassFile();
                return getJarClass(classFile);
            }
        }

        return null;
    }

    public List<JavaProject> getAllProjectsAndPackages(boolean includePackages) throws JavaModelException {
        JavaModel javaModel = JavaModelManager.getJavaModelManager().getJavaModel();
        IJavaProject[] javaProjects = javaModel.getJavaProjects();
        List<JavaProject> result = new ArrayList<>();
        for (IJavaProject javaProject : javaProjects) {
            if(javaProject.exists()) {
                JavaProject project = DtoFactory.newDto(JavaProject.class);
                project.setName(javaProject.getElementName());
                project.setPath(javaProject.getPath().toOSString());
                project.setPackageFragmentRoots(toPackageRoots(javaProject, includePackages));
                result.add(project);
            }
        }
        return result;
    }

    private List<PackageFragmentRoot> toPackageRoots(IJavaProject javaProject, boolean includePackages) throws JavaModelException {
        IPackageFragmentRoot[] packageFragmentRoots = javaProject.getPackageFragmentRoots();
        List<PackageFragmentRoot> result = new ArrayList<>();
        for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
            if(packageFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE){
                PackageFragmentRoot root = DtoFactory.newDto(PackageFragmentRoot.class);
                root.setPath(packageFragmentRoot.getPath().toOSString());
                root.setProjectPath(packageFragmentRoot.getJavaProject().getPath().toOSString());
                if(includePackages) {
                    root.setPackageFragments(toPackageFragments(packageFragmentRoot));
                }
                result.add(root);
            }
        }
        return result;
    }

    private List<PackageFragment> toPackageFragments(IPackageFragmentRoot packageFragmentRoot) throws JavaModelException {
        IJavaElement[] children = packageFragmentRoot.getChildren();
        if(children == null){
            return null;
        }
        List<PackageFragment> result = new ArrayList<>();
        for (IJavaElement child : children) {
            if(child instanceof IPackageFragment){
                IPackageFragment packageFragment = (IPackageFragment)child;
                PackageFragment fragment = DtoFactory.newDto(PackageFragment.class);
                fragment.setName(packageFragment.getElementName());
                fragment.setPath(packageFragment.getPath().toOSString());
                fragment.setProjectPath(packageFragment.getJavaProject().getPath().toOSString());
                result.add(fragment);
            }
        }
        return result;
    }
}
