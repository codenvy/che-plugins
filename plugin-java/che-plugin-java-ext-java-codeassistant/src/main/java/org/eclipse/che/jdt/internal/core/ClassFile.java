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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @see org.eclipse.jdt.core.IClassFile
 */

@SuppressWarnings({"rawtypes"})
public class ClassFile extends Openable implements IClassFile, SuffixConstants {

	protected String name;
	protected BinaryType binaryType = null;

	/*
     * Creates a handle to a class file.
     */
	protected ClassFile(PackageFragment parent, JavaModelManager manager, String nameWithoutExtension) {
		super(parent, manager);
		this.name = nameWithoutExtension;
	}

/* package */ static String simpleName(char[] className) {
	if (className == null)
		return null;
	String simpleName = new String(unqualifiedName(className));
	int lastDollar = simpleName.lastIndexOf('$');
	if (lastDollar != -1)
		return Util.localTypeName(simpleName, lastDollar, simpleName.length());
	else
		return simpleName;
}

/**
 * Returns the Java Model representation of the given name
 * which is provided in diet class file format, or <code>null</code>
 * if the given name is <code>null</code>.
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model format is "java.lang.Object".
 */

public static char[] translatedName(char[] name) {
	if (name == null)
		return null;
	int nameLength = name.length;
	char[] newName= new char[nameLength];
	for (int i= 0; i < nameLength; i++) {
		if (name[i] == '/') {
			newName[i]= '.';
		} else {
			newName[i]= name[i];
		}
	}
	return newName;
}

/**
 * Returns the Java Model representation of the given names
 * which are provided in diet class file format, or <code>null</code>
 * if the given names are <code>null</code>.
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model format is "java.lang.Object".
 */

/* package */ static char[][] translatedNames(char[][] names) {
	if (names == null)
		return null;
	int length = names.length;
	char[][] newNames = new char[length][];
	for(int i = 0; i < length; i++) {
		newNames[i] = translatedName(names[i]);
	}
	return newNames;
}

/**
 * Returns the Java Model format of the unqualified class name for the
 * given className which is provided in diet class file format,
 * or <code>null</code> if the given className is <code>null</code>.
 * (This removes the package name, but not enclosing type names).
 *
 * <p><code>ClassFileReader</code> format is similar to "java/lang/Object",
 * and corresponding Java Model simple name format is "Object".
 */

/* package */ static char[] unqualifiedName(char[] className) {
	if (className == null)
		return null;
	int count = 0;
	for (int i = className.length - 1; i > -1; i--) {
		if (className[i] == '/') {
			char[] name = new char[count];
			System.arraycopy(className, i + 1, name, 0, count);
			return name;
		}
		count++;
	}
	return className;
}

	/*
     * @see IClassFile#becomeWorkingCopy(IProblemRequestor, WorkingCopyOwner, IProgressMonitor)
     */
	public ICompilationUnit becomeWorkingCopy(IProblemRequestor problemRequestor, WorkingCopyOwner owner, IProgressMonitor monitor)
			throws JavaModelException {
//		JavaModelManager manager = JavaModelManager.getJavaModelManager();
//		CompilationUnit workingCopy = new ClassFileWorkingCopy(this, owner == null ? DefaultWorkingCopyOwner.PRIMARY : owner);
//		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo =
//				manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true /*record usage*/,
//											  null/*no problem requestor needed*/);
//		if (perWorkingCopyInfo == null) {
//			// close cu and its children
//			close();
//
//			BecomeWorkingCopyOperation operation = new BecomeWorkingCopyOperation(workingCopy, problemRequestor);
//			operation.runOperation(monitor);
//
//			return workingCopy;
//		}
//		return perWorkingCopyInfo.workingCopy;
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates the children elements for this class file adding the resulting
	 * new handles and info objects to the newElements table. Returns true
	 * if successful, or false if an error is encountered parsing the class file.
	 *
	 * @see Openable
	 * @see org.eclipse.jdt.core.Signature
	 */
	protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, File underlyingResource)
			throws JavaModelException {
		IBinaryType typeInfo = getBinaryTypeInfo(/*underlyingResource*/ null);
		if (typeInfo == null) {
			// The structure of a class file is unknown if a class file format errors occurred
			//during the creation of the diet class file representative of this ClassFile.
			info.setChildren(new IJavaElement[]{});
			return false;
		}

		// Make the type
		IType type = getType();
		info.setChildren(new IJavaElement[]{type});
		newElements.put(type, typeInfo);

		// Read children
		((ClassFileInfo)info).readBinaryChildren(this, (HashMap) newElements, typeInfo);

	return true;
}

/**
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.ICompletionRequestor)
 * @deprecated
 */
@Deprecated
public void codeComplete(int offset, ICompletionRequestor requestor) throws JavaModelException {
//	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
	throw new UnsupportedOperationException();
}

/**
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.ICompletionRequestor, org.eclipse.jdt.core.WorkingCopyOwner)
 * @deprecated
 */
@Deprecated
public void codeComplete(int offset, ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
//	if (requestor == null) {
//		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
//	}
//	codeComplete(offset, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), owner);
	throw new UnsupportedOperationException();
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.CompletionRequestor)
 */
public void codeComplete(int offset, CompletionRequestor requestor) throws JavaModelException {
//	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
	throw new UnsupportedOperationException();
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.CompletionRequestor, org.eclipse.core.runtime.IProgressMonitor)
 */
public void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
//	codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY, monitor);
	throw new UnsupportedOperationException();
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.CompletionRequestor, org.eclipse.jdt.core.WorkingCopyOwner)
 */
public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
//	codeComplete(offset, requestor, owner, null);
	throw new UnsupportedOperationException();
}

/* (non-Javadoc)
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.CompletionRequestor, org.eclipse.jdt.core.WorkingCopyOwner, org.eclipse.core.runtime.IProgressMonitor)
 */
public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner, IProgressMonitor monitor) throws
																													  JavaModelException {
//	String source = getSource();
//	if (source != null) {
//		BinaryType type = (BinaryType) getType();
//		BasicCompilationUnit cu =
//			new BasicCompilationUnit(
//				getSource().toCharArray(),
//				null,
//				type.sourceFileName((IBinaryType) type.getElementInfo()),
//				getJavaProject()); // use project to retrieve corresponding .java IFile
//		codeComplete(cu, cu, offset, requestor, owner, null/*extended context isn't computed*/, monitor);
//	}
	throw new UnsupportedOperationException();
}

/**
 * @see org.eclipse.jdt.core.ICodeAssist#codeSelect(int, int)
 */
public IJavaElement[] codeSelect(int offset, int length) throws JavaModelException {
//	return codeSelect(offset, length, DefaultWorkingCopyOwner.PRIMARY);
	throw new UnsupportedOperationException();
}

/**
 * @see org.eclipse.jdt.core.ICodeAssist#codeSelect(int, int, org.eclipse.jdt.core.WorkingCopyOwner)
 */
public IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaModelException {
//	IBuffer buffer = getBuffer();
//	char[] contents;
//	if (buffer != null && (contents = buffer.getCharacters()) != null) {
//	    BinaryType type = (BinaryType) getType();
//		BasicCompilationUnit cu = new BasicCompilationUnit(contents, null, type.sourceFileName((IBinaryType) type.getElementInfo()));
//		return super.codeSelect(cu, offset, length, owner);
//	} else {
//		//has no associated souce
//		return new IJavaElement[] {};
//	}
	throw new UnsupportedOperationException();
}

/**
 * Returns a new element info for this element.
 */
protected Object createElementInfo() {
	return new ClassFileInfo(manager);
}

public boolean equals(Object o) {
	if (!(o instanceof ClassFile)) return false;
	ClassFile other = (ClassFile) o;
	return this.name.equals(other.name) && this.parent.equals(other.parent);
}

public boolean existsUsingJarTypeCache() {
	if (getPackageFragmentRoot().isArchive()) {
//		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		IType type = getType();
		Object info = manager.getInfo(type);
		if (info == JavaModelCache.NON_EXISTING_JAR_TYPE_INFO)
			return false;
		else if (info != null)
			return true;
		// info is null
		JavaElementInfo parentInfo = (JavaElementInfo) manager.getInfo(getParent());
		if (parentInfo != null) {
			// if parent is open, this class file must be in its children
			IJavaElement[] children = parentInfo.getChildren();
			for (int i = 0, length = children.length; i < length; i++) {
				if (this.name.equals(((ClassFile) children[i]).name))
					return true;
			}
			return false;
		}
		try {
			info = getJarBinaryTypeInfo((PackageFragment) getParent(), true/*fully initialize so as to not keep a reference to the byte array*/);
		} catch (CoreException e) {
			// leave info null
		} catch (IOException e) {
			// leave info null
		} catch (ClassFormatException e) {
			// leave info null
		}
		manager.putJarTypeInfo(type, info == null ? JavaModelCache.NON_EXISTING_JAR_TYPE_INFO : info);
		return info != null;
	} else
		return exists();
//	throw new UnsupportedOperationException();
}

/**
 * Finds the deepest <code>IJavaElement</code> in the hierarchy of
 * <code>elt</elt>'s children (including <code>elt</code> itself)
 * which has a source range that encloses <code>position</code>
 * according to <code>mapper</code>.
 */
protected IJavaElement findElement(IJavaElement elt, int position, SourceMapper mapper) {
	SourceRange range = mapper.getSourceRange(elt);
	if (range == null || position < range.getOffset() || range.getOffset() + range.getLength() - 1 < position) {
		return null;
	}
	if (elt instanceof IParent) {
		try {
			IJavaElement[] children = ((IParent) elt).getChildren();
			for (int i = 0; i < children.length; i++) {
				IJavaElement match = findElement(children[i], position, mapper);
				if (match != null) {
					return match;
				}
			}
		} catch (JavaModelException npe) {
			// elt doesn't exist: return the element
		}
	}
	return elt;
}

/**
 * @see org.eclipse.jdt.core.ITypeRoot#findPrimaryType()
 */
public IType findPrimaryType() {
	IType primaryType= getType();
	if (primaryType.exists()) {
		return primaryType;
	}
	return null;
}

public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
	return getType().getAttachedJavadoc(monitor);
}

/**
 * Returns the <code>ClassFileReader</code>specific for this IClassFile, based
 * on its underlying resource, or <code>null</code> if unable to create
 * the diet class file.
 * There are two cases to consider:<ul>
 * <li>a class file corresponding to an IFile resource</li>
 * <li>a class file corresponding to a zip entry in a JAR</li>
 * </ul>
 *
 * @exception org.eclipse.jdt.core.JavaModelException when the IFile resource or JAR is not available
 * or when this class file is not present in the JAR
 */
public IBinaryType getBinaryTypeInfo(IFile file) throws JavaModelException {
	return getBinaryTypeInfo(file, true/*fully initialize so as to not keep a reference to the byte array*/);
}

public IBinaryType getBinaryTypeInfo(IFile file, boolean fullyInitialize) throws JavaModelException {
	JavaElement pkg = (JavaElement) getParent();
	if (pkg instanceof JarPackageFragment) {
		try {
			IBinaryType info = getJarBinaryTypeInfo((PackageFragment) pkg, fullyInitialize);
			if (info == null) {
				throw newNotPresentException();
			}
			return info;
		} catch (ClassFormatException cfe) {
			//the structure remains unknown
			if (JavaCore.getPlugin().isDebugging()) {
				cfe.printStackTrace(System.err);
			}
			return null;
		} catch (IOException ioe) {
			throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
		} catch (CoreException e) {
			if (e instanceof JavaModelException) {
				throw (JavaModelException)e;
			} else {
				throw new JavaModelException(e);
			}
		}
	} else {
//		byte[] contents = Util.getResourceContentsAsByteArray(file);
//		try {
//			return new ClassFileReader(contents, file.getFullPath().toString().toCharArray(), fullyInitialize);
//		} catch (ClassFormatException cfe) {
//			//the structure remains unknown
//			return null;
//		}
		throw new UnsupportedOperationException();
	}
}

public byte[] getBytes() throws JavaModelException {
	JavaElement pkg = (JavaElement) getParent();
	if (pkg instanceof JarPackageFragment) {
		JarPackageFragmentRoot root = (JarPackageFragmentRoot) pkg.getParent();
		ZipFile zip = null;
		try {
			zip = root.getJar();
			String entryName = Util.concatWith(((PackageFragment)pkg).names, getElementName(), '/');
			ZipEntry ze = zip.getEntry(entryName);
			if (ze != null) {
				return org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
			}
			throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this));
		} catch (IOException ioe) {
			throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
		} catch (CoreException e) {
			if (e instanceof JavaModelException) {
				throw (JavaModelException)e;
			} else {
				throw new JavaModelException(e);
			}
		} finally {
			manager.closeZipFile(zip);
		}
	} else {
		IFile file = (IFile) resource();
		return Util.getResourceContentsAsByteArray(file);
	}
}

private IBinaryType getJarBinaryTypeInfo(PackageFragment pkg, boolean fullyInitialize) throws CoreException, IOException,
																							  ClassFormatException {
	JarPackageFragmentRoot root = (JarPackageFragmentRoot) pkg.getParent();
	ZipFile zip = null;
	try {
		zip = root.getJar();
		String entryName = Util.concatWith(pkg.names, getElementName(), '/');
		ZipEntry ze = zip.getEntry(entryName);
		if (ze != null) {
			byte contents[] = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
			String fileName = root.getHandleIdentifier() + IDependent.JAR_FILE_ENTRY_SEPARATOR + entryName;
			return new ClassFileReader(contents, fileName.toCharArray(), fullyInitialize);
		}
	} finally {
		manager.closeZipFile(zip);
	}
	return null;
}

public IBuffer getBuffer() throws JavaModelException {
	IStatus status = validateClassFile();
	if (status.isOK()) {
		return super.getBuffer();
	} else {
		switch (status.getCode()) {
		case IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH: // don't throw a JavaModelException to be able to open .class file outside the classpath (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=138507 )
		case IJavaModelStatusConstants.INVALID_ELEMENT_TYPES: // don't throw a JavaModelException to be able to open .class file in proj==src case without source (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=221904 )
			return null;
		default:
			throw new JavaModelException((IJavaModelStatus) status);
		}
	}
}

/**
 * @see org.eclipse.jdt.core.IMember
 */
public IClassFile getClassFile() {
	return this;
}

/**
 * @see org.eclipse.jdt.core.IMember#getTypeRoot()
 */
public ITypeRoot getTypeRoot() {
	return this;
}

/**
 * A class file has a corresponding resource unless it is contained
 * in a jar.
 *
 * @see org.eclipse.jdt.core.IJavaElement
 */
public IResource getCorrespondingResource() throws JavaModelException {
	IPackageFragmentRoot root= (IPackageFragmentRoot)getParent().getParent();
	if (root.isArchive()) {
		return null;
	} else {
		return getUnderlyingResource();
	}
}

/**
 * @see org.eclipse.jdt.core.IClassFile
 */
public IJavaElement getElementAt(int position) throws JavaModelException {
	IJavaElement parentElement = getParent();
	while (parentElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
		parentElement = parentElement.getParent();
	}
	PackageFragmentRoot root = (PackageFragmentRoot) parentElement;
	SourceMapper mapper = root.getSourceMapper();
	if (mapper == null) {
		return null;
	} else {
		// ensure this class file's buffer is open so that source ranges are computed
		getBuffer();

		IType type = getType();
		return findElement(type, position, mapper);
	}
}

public IJavaElement getElementAtConsideringSibling(int position) throws JavaModelException {
	IPackageFragment fragment = (IPackageFragment)getParent();
	PackageFragmentRoot root = (PackageFragmentRoot) fragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
	SourceMapper mapper = root.getSourceMapper();
	if (mapper == null) {
		return null;
	} else {
		int index = this.name.indexOf('$');
		int prefixLength = index < 0 ? this.name.length() : index;

		IType type = null;
		int start = -1;
		int end = Integer.MAX_VALUE;
		IJavaElement[] children = fragment.getChildren();
		for (int i = 0; i < children.length; i++) {
			String childName = children[i].getElementName();

			int childIndex = childName.indexOf('$');
			int childPrefixLength = childIndex < 0 ? childName.indexOf('.') : childIndex;
			if (prefixLength == childPrefixLength && this.name.regionMatches(0, childName, 0, prefixLength)) {
				IClassFile classFile = (IClassFile) children[i];

				// ensure this class file's buffer is open so that source ranges are computed
				classFile.getBuffer();

				SourceRange range = mapper.getSourceRange(classFile.getType());
				if (range == SourceMapper.UNKNOWN_RANGE) continue;
				int newStart = range.getOffset();
				int newEnd = newStart + range.getLength() - 1;
				if(newStart > start && newEnd < end
						&& newStart <= position && newEnd >= position) {
					type = classFile.getType();
					start = newStart;
					end = newEnd;
				}
			}
		}
		if(type != null) {
			return findElement(type, position, mapper);
		}
		return null;
	}
}

public String getElementName() {
	return this.name + SuffixConstants.SUFFIX_STRING_class;
}

/**
 * @see org.eclipse.jdt.core.IJavaElement
 */
public int getElementType() {
	return CLASS_FILE;
}

/*
 * @see JavaElement
 */
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
	switch (token.charAt(0)) {
		case JEM_TYPE:
			if (!memento.hasMoreTokens()) return this;
			String typeName = memento.nextToken();
			JavaElement type = new BinaryType(this, manager, typeName);
			return type.getHandleFromMemento(memento, owner);
	}
	return null;
}

/**
 * @see JavaElement#getHandleMemento()
 */
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_CLASSFILE;
}

/*
 * @see IJavaElement
 */
public IPath getPath() {
	PackageFragmentRoot root = getPackageFragmentRoot();
	if (root.isArchive()) {
		return root.getPath();
	} else {
		return getParent().getPath().append(getElementName());
	}
}

/*
 * @see IJavaElement
 */
public File resource(PackageFragmentRoot root) {
	return new File(((Openable) this.parent).resource(root), getElementName());
}

/**
 * @see org.eclipse.jdt.core.ISourceReference
 */
public String getSource() throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer == null) {
		return null;
	}
	return buffer.getContents();
}

/**
 * @see org.eclipse.jdt.core.ISourceReference
 */
public ISourceRange getSourceRange() throws JavaModelException {
	IBuffer buffer = getBuffer();
	if (buffer != null) {
		String contents = buffer.getContents();
		if (contents == null) return null;
		return new SourceRange(0, contents.length());
	} else {
		return null;
	}
}

/*
 * Returns the name of the toplevel type of this class file.
 */
public String getTopLevelTypeName() {
    String topLevelTypeName = getElementName();
    int firstDollar = topLevelTypeName.indexOf('$');
    if (firstDollar != -1) {
        topLevelTypeName = topLevelTypeName.substring(0, firstDollar);
    } else {
        topLevelTypeName = topLevelTypeName.substring(0, topLevelTypeName.length()-SUFFIX_CLASS.length);
    }
    return topLevelTypeName;
}

/**
 * @see org.eclipse.jdt.core.IClassFile
 */
public IType getType() {
	if (this.binaryType == null) {
		this.binaryType = new BinaryType(this, manager, getTypeName());
	}
	return this.binaryType;
}

public String getTypeName() {
	// Internal class file name doesn't contain ".class" file extension
	int lastDollar = this.name.lastIndexOf('$');
	return lastDollar > -1 ? Util.localTypeName(this.name, lastDollar, this.name.length()) : this.name;
}

/*
 * @see IClassFile
 */
public ICompilationUnit getWorkingCopy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
//	CompilationUnit workingCopy = new ClassFileWorkingCopy(this, owner == null ? DefaultWorkingCopyOwner.PRIMARY : owner);
//	JavaModelManager manager = JavaModelManager.getJavaModelManager();
//	JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo =
//		manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true/*record usage*/, null/*not used since don't create*/);
//	if (perWorkingCopyInfo != null) {
//		return perWorkingCopyInfo.getWorkingCopy(); // return existing handle instead of the one created above
//	}
//	BecomeWorkingCopyOperation op = new BecomeWorkingCopyOperation(workingCopy, null);
//	op.runOperation(monitor);
//	return workingCopy;
	throw new UnsupportedOperationException();
}

/**
 * @see org.eclipse.jdt.core.IClassFile
 * @deprecated
 */
@Deprecated
public IJavaElement getWorkingCopy(IProgressMonitor monitor, org.eclipse.jdt.core.IBufferFactory factory) throws JavaModelException {
//	return getWorkingCopy(BufferFactoryWrapper.create(factory), monitor);
	throw new UnsupportedOperationException();
}

/**
 * @see Openable
 */
protected boolean hasBuffer() {
	return true;
}

public int hashCode() {
	return Util.combineHashCodes(this.name.hashCode(), this.parent.hashCode());
}

/**
 * @see org.eclipse.jdt.core.IClassFile
 */
public boolean isClass() throws JavaModelException {
	return getType().isClass();
}

/**
 * @see org.eclipse.jdt.core.IClassFile
 */
public boolean isInterface() throws JavaModelException {
	return getType().isInterface();
}

/**
 * Returns true - class files are always read only.
 */
public boolean isReadOnly() {
	return true;
}

private IStatus validateClassFile() {
	IPackageFragmentRoot root = getPackageFragmentRoot();
	try {
		if (root.getKind() != IPackageFragmentRoot.K_BINARY)
			return new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, root);
	} catch (JavaModelException e) {
		return e.getJavaModelStatus();
	}
	IJavaProject project = getJavaProject();
	if(org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(getElementName())){
		return Status.OK_STATUS;
	}
	return Status.CANCEL_STATUS;
//	return JavaConventions.validateClassFileName(getElementName(), project.getOption(JavaCore.COMPILER_SOURCE, true),
//												 project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
}

/**
 * Opens and returns buffer on the source code associated with this class file.
 * Maps the source code to the children elements of this class file.
 * If no source code is associated with this class file,
 * <code>null</code> is returned.
 *
 * @see Openable
 */
protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaModelException {
	// Check the cache for the top-level type first
	IType outerMostEnclosingType = getOuterMostEnclosingType();
	IBuffer buffer = getBufferManager().getBuffer(outerMostEnclosingType.getClassFile());
	if (buffer == null) {
		SourceMapper mapper = getSourceMapper();
		IBinaryType typeInfo = info instanceof IBinaryType ? (IBinaryType) info : null;
		if (mapper != null) {
			buffer = mapSource(mapper, typeInfo, outerMostEnclosingType.getClassFile());
		}
	}
	return buffer;
}

/** Loads the buffer via SourceMapper, and maps it in SourceMapper */
private IBuffer mapSource(SourceMapper mapper, IBinaryType info, IClassFile bufferOwner) {
	char[] contents = mapper.findSource(getType(), info);
	if (contents != null) {
		// create buffer
		IBuffer buffer = BufferManager.createBuffer(bufferOwner);
		if (buffer == null) return null;
		BufferManager bufManager = getBufferManager();
		bufManager.addBuffer(buffer);

		// set the buffer source
		if (buffer.getCharacters() == null){
			buffer.setContents(contents);
		}

		// listen to buffer changes
		buffer.addBufferChangedListener(this);

		// do the source mapping
		mapper.mapSource(getOuterMostEnclosingType(), contents, info);

		return buffer;
	} else {
		// create buffer
		IBuffer buffer = BufferManager.createNullBuffer(bufferOwner);
		if (buffer == null) return null;
		BufferManager bufManager = getBufferManager();
		bufManager.addBuffer(buffer);

		// listen to buffer changes
		buffer.addBufferChangedListener(this);
		return buffer;
	}
}

/** Returns the type of the top-level declaring class used to find the source code */
private IType getOuterMostEnclosingType() {
	IType type = getType();
	IType enclosingType = type.getDeclaringType();
	while (enclosingType != null) {
		type = enclosingType;
		enclosingType = type.getDeclaringType();
	}
	return type;
}

/**
 * @see org.eclipse.jdt.core.ICodeAssist#codeComplete(int, org.eclipse.jdt.core.ICodeCompletionRequestor)
 * @deprecated - should use codeComplete(int, ICompletionRequestor) instead
 */
@Deprecated
public void codeComplete(int offset, final org.eclipse.jdt.core.ICodeCompletionRequestor requestor) throws JavaModelException {

//	if (requestor == null){
//		codeComplete(offset, (ICompletionRequestor)null);
//		return;
//	}
//	codeComplete(
//		offset,
//		new ICompletionRequestor(){
//			public void acceptAnonymousType(char[] superTypePackageName,char[] superTypeName, char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
//				// ignore
//			}
//			public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
//				requestor.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd);
//			}
//			public void acceptError(IProblem error) {
//				// was disabled in 1.0
//			}
//			public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] fieldName, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
//				requestor.acceptField(declaringTypePackageName, declaringTypeName, fieldName, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd);
//			}
//			public void acceptInterface(char[] packageName,char[] interfaceName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
//				requestor.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd);
//			}
//			public void acceptKeyword(char[] keywordName,int completionStart,int completionEnd, int relevance){
//				requestor.acceptKeyword(keywordName, completionStart, completionEnd);
//			}
//			public void acceptLabel(char[] labelName,int completionStart,int completionEnd, int relevance){
//				requestor.acceptLabel(labelName, completionStart, completionEnd);
//			}
//			public void acceptLocalVariable(char[] localVarName,char[] typePackageName,char[] typeName,int modifiers,int completionStart,int completionEnd, int relevance){
//				// ignore
//			}
//			public void acceptMethod(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
//				// skip parameter names
//				requestor.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd);
//			}
//			public void acceptMethodDeclaration(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
//				// ignore
//			}
//			public void acceptModifier(char[] modifierName,int completionStart,int completionEnd, int relevance){
//				requestor.acceptModifier(modifierName, completionStart, completionEnd);
//			}
//			public void acceptPackage(char[] packageName,char[] completionName,int completionStart,int completionEnd, int relevance){
//				requestor.acceptPackage(packageName, completionName, completionStart, completionEnd);
//			}
//			public void acceptType(char[] packageName,char[] typeName,char[] completionName,int completionStart,int completionEnd, int relevance){
//				requestor.acceptType(packageName, typeName, completionName, completionStart, completionEnd);
//			}
//			public void acceptVariableName(char[] typePackageName,char[] typeName,char[] varName,char[] completionName,int completionStart,int completionEnd, int relevance){
//				// ignore
//			}
//		});
	throw  new UnsupportedOperationException();
}

protected IStatus validateExistence(File underlyingResource) {
	// check whether the class file can be opened
	IStatus status = validateClassFile();
	if (!status.isOK())
		return status;
	if (underlyingResource != null) {
		if (!underlyingResource.exists())
			return newDoesNotExistStatus();
		PackageFragmentRoot root;
		if ((underlyingResource instanceof IFolder) && (root = getPackageFragmentRoot()).isArchive()) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=204652
			return root.newDoesNotExistStatus();
		}
	}
	return JavaModelStatus.VERIFIED_OK;
}
public ISourceRange getNameRange() {
	return null;
}
}
