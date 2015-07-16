/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.corext.refactoring.sorround;

import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Block;
import org.eclipse.che.ide.ext.java.jdt.core.dom.BodyDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ConstructorInvocation;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Expression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ExpressionStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Initializer;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Message;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Statement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclaration;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.codemanipulation.ASTResolving;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.dom.Selection;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.refactoring.util.CodeAnalyzer;
import org.eclipse.che.ide.ext.java.jdt.text.Document;
import org.eclipse.che.ide.runtime.CoreException;

import java.util.List;

public class SurroundWithAnalyzer extends CodeAnalyzer {

    private VariableDeclaration[] fLocals;

    public SurroundWithAnalyzer(Document document, Selection selection) throws CoreException {
        super(document, selection, false);
    }

    public Statement[] getSelectedStatements() {
        if (hasSelectedNodes()) {
            return internalGetSelectedNodes().toArray(new Statement[internalGetSelectedNodes().size()]);
        } else {
            return new Statement[0];
        }
    }

    public VariableDeclaration[] getAffectedLocals() {
        return fLocals;
    }

    public BodyDeclaration getEnclosingBodyDeclaration() {
        ASTNode node = getFirstSelectedNode();
        if (node == null)
            return null;

        return ASTResolving.findParentBodyDeclaration(node);
    }

    @Override
    protected boolean handleSelectionEndsIn(ASTNode node) {
        return true;
    }

    @Override
    public void endVisit(CompilationUnit node) {
        postProcessSelectedNodes(internalGetSelectedNodes());
        BodyDeclaration enclosingNode = null;
        superCall:
        {
            if (getStatus().hasFatalError())
                break superCall;
            if (!hasSelectedNodes()) {
                ASTNode coveringNode = getLastCoveringNode();
                if (coveringNode instanceof Block) {
                    Block block = (Block)coveringNode;
                    Message[] messages = ASTNodes.getMessages(block, ASTNodes.NODE_ONLY);
                    if (messages.length > 0) {
                        invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_compile_errors());
                        break superCall;
                    }
                }
                invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_doesNotCover());
                break superCall;
            }
            enclosingNode = ASTResolving.findParentBodyDeclaration(getFirstSelectedNode());
            if (!(enclosingNode instanceof MethodDeclaration) && !(enclosingNode instanceof Initializer)) {
                invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_doesNotContain());
                break superCall;
            }
            if (!onlyStatements()) {
                invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_onlyStatements());
            }
            fLocals = LocalDeclarationAnalyzer.perform(enclosingNode, getSelection());
        }
        super.endVisit(node);
    }

    @Override
    public void endVisit(SuperConstructorInvocation node) {
        if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
            invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_cannotHandleSuper());

        }
        super.endVisit(node);
    }

    @Override
    public void endVisit(ConstructorInvocation node) {
        if (getSelection().getEndVisitSelectionMode(node) == Selection.SELECTED) {
            invalidSelection(RefactoringCoreMessages.INSTANCE.SurroundWithTryCatchAnalyzer_cannotHandleThis());
        }
        super.endVisit(node);
    }

    protected void postProcessSelectedNodes(List<ASTNode> selectedNodes) {
        if (selectedNodes == null || selectedNodes.size() == 0)
            return;
        if (selectedNodes.size() == 1) {
            ASTNode node = selectedNodes.get(0);
            if (node instanceof Expression && node.getParent() instanceof ExpressionStatement) {
                selectedNodes.clear();
                selectedNodes.add(node.getParent());
            }
        }
    }

    private boolean onlyStatements() {
        ASTNode[] nodes = getSelectedNodes();
        for (int i = 0; i < nodes.length; i++) {
            if (!(nodes[i] instanceof Statement))
                return false;
        }
        return true;
    }

}
