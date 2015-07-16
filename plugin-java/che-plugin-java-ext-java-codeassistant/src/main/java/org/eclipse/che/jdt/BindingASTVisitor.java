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
package org.eclipse.che.jdt;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
* @author Evgen Vidolob
*/
public class BindingASTVisitor extends ASTVisitor {
    ITypeBinding typeBinding;

    public boolean visit(AnnotationTypeDeclaration annotationTypeDeclaration) {
        typeBinding = annotationTypeDeclaration.resolveBinding();
        return false;
    }

    public boolean visit(AnonymousClassDeclaration anonymousClassDeclaration) {
        typeBinding = anonymousClassDeclaration.resolveBinding();
        return false;
    }

    public boolean visit(TypeDeclaration typeDeclaration) {
        typeBinding = typeDeclaration.resolveBinding();
        return false;
    }

    public boolean visit(EnumDeclaration enumDeclaration) {
        typeBinding = enumDeclaration.resolveBinding();
        return false;
    }

    public ITypeBinding getTypeBinding() {
        return typeBinding;
    }
}
