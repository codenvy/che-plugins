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

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.Expression;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;


public class SelectionOnNameOfMemberValuePair extends MemberValuePair {

	public SelectionOnNameOfMemberValuePair(char[] token, int sourceStart, int sourceEnd, Expression value) {
		super(token, sourceStart, sourceEnd, value);
	}

	public StringBuffer print(int indent, StringBuffer output) {
		output.append("<SelectOnName:"); //$NON-NLS-1$
		output.append(this.name);
		output.append(">"); //$NON-NLS-1$
		return output;
	}

	public void resolveTypeExpecting(BlockScope scope, TypeBinding requiredType) {
		super.resolveTypeExpecting(scope, requiredType);

		if(this.binding != null) {
			throw new SelectionNodeFound(this.binding);
		}
		throw new SelectionNodeFound();
	}
}
