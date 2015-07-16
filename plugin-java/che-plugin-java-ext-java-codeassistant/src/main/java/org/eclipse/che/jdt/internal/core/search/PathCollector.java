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
package org.eclipse.che.jdt.internal.core.search;

import org.eclipse.che.jdt.core.search.SearchParticipant;
import org.eclipse.che.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;

import java.util.HashSet;

/**
 * Collects the resource paths reported by a client to this search requestor.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PathCollector extends IndexQueryRequestor {

	/* a set of resource paths */
	public HashSet paths = new HashSet(5);

	/* (non-Javadoc)
	 * @see IndexQueryRequestor#acceptIndexMatch(String, SearchPattern, SearchParticipant, AccessRuleSet)
	 */
	public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
		this.paths.add(documentPath);
		return true;
	}

	/**
	 * Returns the paths that have been collected.
	 */
	public String[] getPaths() {
		return (String[])this.paths.toArray(new String[this.paths.size()]);
	}
}
