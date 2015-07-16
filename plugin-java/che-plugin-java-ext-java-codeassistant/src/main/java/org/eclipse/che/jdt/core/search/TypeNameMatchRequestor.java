/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.jdt.core.search;

/**
 * A <code>TypeNameMatchRequestor</code> collects matches from a <code>searchAllTypeNames</code>
 * query to a <code>SearchEngine</code>. Clients must subclass this abstract class and pass an instance to the
 * {@link org.eclipse.jdt.core.search.SearchEngine#searchAllTypeNames(
 *		char[] packageName,
 *		int packageMatchRule,
 *		char[] typeName,
 *		int typeMatchRule,
 *		int searchFor,
 *        org.eclipse.jdt.core.search.IJavaSearchScope scope,
 *        org.eclipse.jdt.core.search.TypeNameMatchRequestor nameMatchRequestor,
 *		int waitingPolicy,
 * 	org.eclipse.core.runtime.IProgressMonitor monitor)} method.
 * Only top-level and member types are reported. Local types are not reported.
 * <p>
 * While {@link org.eclipse.jdt.core.search.TypeNameRequestor} only reports type names information (e.g. package, enclosing types, simple
 * name, modifiers, etc.),
 * this class reports {@link org.eclipse.jdt.core.search.TypeNameMatch} objects instead, which store this information and can return
 * an {@link org.eclipse.jdt.core.IType} handle.
 * </p>
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @see org.eclipse.jdt.core.search.TypeNameMatch
 * @see org.eclipse.jdt.core.search.TypeNameRequestor
 *
 * @since 3.3
 */
public abstract class TypeNameMatchRequestor {
	/**
	 * Accepts a type name match ({@link org.eclipse.jdt.core.search.TypeNameMatch}) which contains top-level or a member type
	 * information as package name, enclosing types names, simple type name, modifiers, etc.
	 *
	 * @param match the match which contains all type information
	 */
	public abstract void acceptTypeNameMatch(TypeNameMatch match);
}
