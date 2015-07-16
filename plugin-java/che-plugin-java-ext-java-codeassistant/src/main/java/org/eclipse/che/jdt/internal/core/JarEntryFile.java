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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.JavaModelStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A jar entry that represents a non-java file found in a JAR.
 *
 * @see org.eclipse.core.resources.IStorage
 */
public class JarEntryFile  extends JarEntryResource {
	private static final IJarEntryResource[] NO_CHILDREN = new IJarEntryResource[0];
	private JavaModelManager manager;

	public JarEntryFile(String simpleName, JavaModelManager manager) {
		super(simpleName);
		this.manager = manager;
	}

	public JarEntryResource clone(Object newParent) {
		JarEntryFile file = new JarEntryFile(this.simpleName, this.manager);
		file.setParent(newParent);
		return file;
	}

	public InputStream getContents() throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = getZipFile();
			if (org.eclipse.jdt.internal.core.JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [JarEntryFile.getContents()] Creating ZipFile on " +
								   zipFile.getName()); //$NON-NLS-1$	//$NON-NLS-2$
			}
			String entryName = getEntryName();
			ZipEntry zipEntry = zipFile.getEntry(entryName);
			if (zipEntry == null) {
				throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_PATH, entryName));
			}
			byte[] contents = Util.getZipEntryByteContent(zipEntry, zipFile);
			return new ByteArrayInputStream(contents);
		} catch (IOException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
		} finally {
			// avoid leaking ZipFiles
			manager.closeZipFile(zipFile);
		}
	}

	public IJarEntryResource[] getChildren() {
		return NO_CHILDREN;
	}

	public boolean isFile() {
		return true;
	}

	public String toString() {
		return "JarEntryFile["+getEntryName()+"]"; //$NON-NLS-2$ //$NON-NLS-1$
	}
}
