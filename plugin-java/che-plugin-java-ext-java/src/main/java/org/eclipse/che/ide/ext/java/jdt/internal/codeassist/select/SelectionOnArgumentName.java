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

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.Argument;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;

public class SelectionOnArgumentName extends Argument {

	public SelectionOnArgumentName(char[] name , long posNom , TypeReference tr , int modifiers){

		super(name, posNom, tr, modifiers);
	}

	public void bind(MethodScope scope, TypeBinding typeBinding, boolean used) {

		super.bind(scope, typeBinding, used);
		throw new SelectionNodeFound(this.binding);
	}

	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output);
		output.append("<SelectionOnArgumentName:"); //$NON-NLS-1$
		if (this.type != null) this.type.print(0, output).append(' ');
		output.append(this.name);
		if (this.initialization != null) {
			output.append(" = ");//$NON-NLS-1$
			this.initialization.printExpression(0, output);
		}
		return output.append('>');
	}

	public void resolve(BlockScope scope) {

		super.resolve(scope);
		throw new SelectionNodeFound(this.binding);
	}
}
