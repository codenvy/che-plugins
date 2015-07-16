/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.compiler.codegen;

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ClassFileConstants;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeIds;

/** This type is used to store all the constant pool entries. */
public class ConstantPool implements ClassFileConstants, TypeIds {
    public static final char[] Close = "close".toCharArray(); //$NON-NLS-1$

    public static final char[] Init = "<init>".toCharArray(); //$NON-NLS-1$

    public static final char[] JavaLangStringSignature = "Ljava/lang/String;".toCharArray(); //$NON-NLS-1$

    public static final char[] ObjectSignature = "Ljava/lang/Object;".toCharArray(); //$NON-NLS-1$
}
