/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.che.jdt.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * Utility methods for the Java Model.
 *
 * @see JDTUIHelperClasses
 */
public class JavaModelUtil {
    /**
     * The name of the package-info.java file.
     *
     * @since 3.8
     */
    public static final String PACKAGE_INFO_JAVA = "package-info.java"; //$NON-NLS-1$

    /**
     * The name of the package-info.class file.
     *
     * @since 3.9
     */
    public static final String PACKAGE_INFO_CLASS = "package-info.class"; //$NON-NLS-1$

    /**
     * The name of the package.html file.
     *
     * @since 3.9
     */
    public static final String PACKAGE_HTML = "package.html"; //$NON-NLS-1$

    /**
     * @param type
     *         the type to test
     * @return <code>true</code> iff the type is an interface or an annotation
     * @throws org.eclipse.jdt.core.JavaModelException
     *         thrown when the field can not be accessed
     */
    public static boolean isInterfaceOrAnnotation(IType type) throws JavaModelException {
        return type.isInterface();
    }

    /**
     * @param version1
     *         the first version
     * @param version2
     *         the second version
     * @return <code>true</code> iff version1 is less than version2
     */
    public static boolean isVersionLessThan(String version1, String version2) {
        if (JavaCore.VERSION_CLDC_1_1.equals(version1)) {
            version1 = JavaCore.VERSION_1_1 + 'a';
        }
        if (JavaCore.VERSION_CLDC_1_1.equals(version2)) {
            version2 = JavaCore.VERSION_1_1 + 'a';
        }
        return version1.compareTo(version2) < 0;
    }

    /**
     * Resolves a type name in the context of the declaring type.
     *
     * @param refTypeSig
     *         the type name in signature notation (for example 'QVector') this can also be an array type, but dimensions will be ignored.
     * @param declaringType
     *         the context for resolving (type where the reference was made in)
     * @return returns the fully qualified type name or build-in-type name. if a unresolved type couldn't be resolved null is returned
     * @throws JavaModelException
     *         thrown when the type can not be accessed
     */
    public static String getResolvedTypeName(String refTypeSig, IType declaringType) throws JavaModelException {
        int arrayCount = Signature.getArrayCount(refTypeSig);
        char type = refTypeSig.charAt(arrayCount);
        if (type == Signature.C_UNRESOLVED) {
            String name = ""; //$NON-NLS-1$
            int bracket = refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
            if (bracket > 0)
                name = refTypeSig.substring(arrayCount + 1, bracket);
            else {
                int semi = refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
                if (semi == -1) {
                    throw new IllegalArgumentException();
                }
                name = refTypeSig.substring(arrayCount + 1, semi);
            }
            String[][] resolvedNames = declaringType.resolveType(name);
            if (resolvedNames != null && resolvedNames.length > 0) {
                return JavaModelUtil.concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
            }
            return null;
        } else {
            return Signature.toString(refTypeSig.substring(arrayCount));
        }
    }

    /**
     * Concatenates two names. Uses a dot for separation.
     * Both strings can be empty or <code>null</code>.
     *
     * @param name1
     *         the first name
     * @param name2
     *         the second name
     * @return the concatenated name
     */
    public static String concatenateName(String name1, String name2) {
        StringBuffer buf = new StringBuffer();
        if (name1 != null && name1.length() > 0) {
            buf.append(name1);
        }
        if (name2 != null && name2.length() > 0) {
            if (buf.length() > 0) {
                buf.append('.');
            }
            buf.append(name2);
        }
        return buf.toString();
    }

    /**
     * Returns the package fragment root of <code>IJavaElement</code>. If the given
     * element is already a package fragment root, the element itself is returned.
     *
     * @param element
     *         the element
     * @return the package fragment root of the element or <code>null</code>
     */
    public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
        return (IPackageFragmentRoot)element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    }

    public static boolean isPolymorphicSignature(IMethod method) {
        return method.getAnnotation("java.lang.invoke.MethodHandle$PolymorphicSignature").exists(); //$NON-NLS-1$
    }

    public static boolean is18OrHigher(String compliance) {
        return !isVersionLessThan(compliance, JavaCore.VERSION_1_8);
    }

    /**
     * Checks if the given project or workspace has source compliance 1.8 or greater.
     *
     * @param project
     *         the project to test or <code>null</code> to test the workspace settings
     * @return <code>true</code> if the given project or workspace has source compliance 1.8 or
     * greater.
     */
    public static boolean is18OrHigher(IJavaProject project) {
        return is18OrHigher(getSourceCompliance(project));
    }

    private static String getSourceCompliance(IJavaProject project) {
        return project != null ? project.getOption(JavaCore.COMPILER_SOURCE, true) : JavaCore.getOption(JavaCore.COMPILER_SOURCE);
    }

    /**
     * Finds a type container by container name. The returned element will be of type
     * <code>IType</code> or a <code>IPackageFragment</code>. <code>null</code> is returned if the
     * type container could not be found.
     *
     * @param jproject
     *         The Java project defining the context to search
     * @param typeContainerName
     *         A dot separated name of the type container
     * @return returns the container
     * @throws JavaModelException
     *         thrown when the project can not be accessed
     * @see #getTypeContainerName(IType)
     */
    public static IJavaElement findTypeContainer(IJavaProject jproject, String typeContainerName) throws JavaModelException {
        // try to find it as type
        IJavaElement result = jproject.findType(typeContainerName);
        if (result == null) {
            // find it as package
            IPath path = new Path(typeContainerName.replace('.', '/'));
            result = jproject.findElement(path);
            if (!(result instanceof IPackageFragment)) {
                result = null;
            }

        }
        return result;
    }

}
