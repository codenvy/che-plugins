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
package org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast;

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ASTVisitor;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;

public class FalseLiteral extends MagicLiteral {

    static final char[] source = {'f', 'a', 'l', 's', 'e'};

    public FalseLiteral(int s, int e) {
        super(s, e);
    }

    @Override
    public void computeConstant() {
        this.constant = BooleanConstant.fromValue(false);
    }

    /**
     * Code generation for false literal
     *
     * @param currentScope
     *         org.eclipse.che.ide.java.client.internal.compiler.lookup.BlockScope
     * @param codeStream
     *         org.eclipse.che.ide.java.client.internal.compiler.codegen.CodeStream
     * @param valueRequired
     *         boolean
     */
    @Override
    public void generateCode(BlockScope currentScope, boolean valueRequired) {

    }

    @Override
    public void generateOptimizedBoolean(BlockScope currentScope, BranchLabel trueLabel, BranchLabel falseLabel,
                                         boolean valueRequired) {

    }

    @Override
    public TypeBinding literalType(BlockScope scope) {
        return TypeBinding.BOOLEAN;
    }

    /**
     *
     */
    @Override
    public char[] source() {
        return source;
    }

    @Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {
        visitor.visit(this, scope);
        visitor.endVisit(this, scope);
    }
}
