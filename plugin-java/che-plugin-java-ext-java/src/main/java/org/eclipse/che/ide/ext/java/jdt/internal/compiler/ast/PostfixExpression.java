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
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;

public class PostfixExpression extends CompoundAssignment {

    public PostfixExpression(Expression lhs, Expression expression, int operator, int pos) {
        super(lhs, expression, operator, pos);
        this.sourceStart = lhs.sourceStart;
        this.sourceEnd = pos;
    }

    @Override
    public boolean checkCastCompatibility() {
        return false;
    }

    /**
     * Code generation for PostfixExpression
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
        // various scenarii are possible, setting an array reference,
        // a field reference, a blank final field reference, a field of an enclosing instance or
        // just a local variable.

        ((Reference)this.lhs).generatePostIncrement(currentScope, this, valueRequired);
    }

    @Override
    public String operatorToString() {
        switch (this.operator) {
            case PLUS:
                return "++"; //$NON-NLS-1$
            case MINUS:
                return "--"; //$NON-NLS-1$
        }
        return "unknown operator"; //$NON-NLS-1$
    }

    @Override
    public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
        return this.lhs.printExpression(indent, output).append(' ').append(operatorToString());
    }

    @Override
    public boolean restrainUsageToNumericTypes() {
        return true;
    }

    @Override
    public void traverse(ASTVisitor visitor, BlockScope scope) {

        if (visitor.visit(this, scope)) {
            this.lhs.traverse(visitor, scope);
        }
        visitor.endVisit(this, scope);
    }
}
