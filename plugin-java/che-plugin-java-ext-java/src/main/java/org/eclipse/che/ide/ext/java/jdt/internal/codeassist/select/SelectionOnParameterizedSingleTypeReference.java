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

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnParameterizedSingleTypeReference extends ParameterizedSingleTypeReference {
	public SelectionOnParameterizedSingleTypeReference(char[] name, TypeReference[] typeArguments, long pos){
		super(name, typeArguments, 0, pos);
	}

	public TypeBinding resolveType(BlockScope scope, boolean checkBounds) {
		super.resolveType(scope, checkBounds);
		throw new SelectionNodeFound(this.resolvedType);
	}

	public TypeBinding resolveType(ClassScope scope) {
		super.resolveType(scope);
		throw new SelectionNodeFound(this.resolvedType);
	}

	public StringBuffer printExpression(int indent, StringBuffer output){
		output.append("<SelectOnType:");//$NON-NLS-1$
		output.append(this.token);
		output.append('<');
		int max = this.typeArguments.length - 1;
		for (int i= 0; i < max; i++) {
			this.typeArguments[i].print(0, output);
			output.append(", ");//$NON-NLS-1$
		}
		this.typeArguments[max].print(0, output);
		output.append('>');
		output.append('>');
		return output;
	}
}
