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

import org.eclipse.che.ide.ext.java.jdt.core.compiler.CharOperation;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeReference;

public class SelectionOnFieldType extends FieldDeclaration {
	public SelectionOnFieldType(TypeReference type) {
		super();
		this.sourceStart = type.sourceStart;
		this.sourceEnd = type.sourceEnd;
		this.type = type;
		this.name = CharOperation.NO_CHAR;
	}
	public StringBuffer printStatement(int tab, StringBuffer output) {
		return this.type.print(tab, output).append(';');
	}
}
