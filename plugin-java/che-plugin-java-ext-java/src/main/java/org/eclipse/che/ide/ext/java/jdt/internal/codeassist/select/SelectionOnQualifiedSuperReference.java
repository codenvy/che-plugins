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
package org.eclipse.che.ide.ext.java.jdt.internal.codeassist.select;

/*
 * Selection node build by the parser in any case it was intending to
 * reduce a qualified super reference containing the assist identifier.
 * e.g.
 *
 *	class X extends Z {
 *    class Y {
 *    	void foo() {
 *      	X.[start]super[end].bar();
 *      }
 *    }
 *  }
 *
 *	---> class X {
 *		   class Y {
 *           void foo() {
 *             <SelectOnQualifiedSuper:X.super>
 *           }
 *         }
 *       }
 *
 */

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnQualifiedSuperReference extends QualifiedSuperReference {
public SelectionOnQualifiedSuperReference(TypeReference name, int pos, int sourceEnd) {
	super(name, pos, sourceEnd);
}
public StringBuffer printExpression(int indent, StringBuffer output) {

	output.append("<SelectOnQualifiedSuper:"); //$NON-NLS-1$
	return super.printExpression(0, output).append('>');
}

public TypeBinding resolveType(BlockScope scope) {
	TypeBinding binding = super.resolveType(scope);

	if (binding == null || !binding.isValidBinding())
		throw new SelectionNodeFound();
	else
		throw new SelectionNodeFound(binding);
}
}
