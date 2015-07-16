/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast;

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ASTVisitor;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.flow.LoopingFlowContext;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.flow.UnconditionalFlowInfo;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.Constant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeIds;

public class DoStatement extends Statement {

    public Expression condition;

    public Statement action;

    private BranchLabel breakLabel, continueLabel;

    // for local variables table attributes
    int mergedInitStateIndex = -1;

    int preConditionInitStateIndex = -1;

    public DoStatement(Expression condition, Statement action, int sourceStart, int sourceEnd) {

        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
        this.condition = condition;
        this.action = action;
        // remember useful empty statement
        if (action instanceof EmptyStatement)
            action.bits |= ASTNode.IsUsefulEmptyStatement;
    }

    public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
        this.breakLabel = new BranchLabel();
        this.continueLabel = new BranchLabel();
        LoopingFlowContext loopingContext =
                new LoopingFlowContext(flowContext, flowInfo, this, this.breakLabel, this.continueLabel, currentScope);

        Constant cst = this.condition.constant;
        boolean isConditionTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
        cst = this.condition.optimizedBooleanConstant();
        boolean isConditionOptimizedTrue = cst != Constant.NotAConstant && cst.booleanValue() == true;
        boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;

        int previousMode = flowInfo.reachMode();

        FlowInfo initsOnCondition = flowInfo;

        UnconditionalFlowInfo actionInfo = flowInfo.nullInfoLessUnconditionalCopy();
        // we need to collect the contribution to nulls of the coming paths through the
        // loop, be they falling through normally or branched to break, continue labels
        // or catch blocks
        if ((this.action != null) && !this.action.isEmptyBlock()) {
            actionInfo = this.action.analyseCode(currentScope, loopingContext, actionInfo).unconditionalInits();

            // code generation can be optimized when no need to continue in the loop
            if ((actionInfo.tagBits & loopingContext.initsOnContinue.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0) {
                this.continueLabel = null;
            }
            if ((this.condition.implicitConversion & TypeIds.UNBOXING) != 0) {
                initsOnCondition =
                        flowInfo.unconditionalInits().addInitializationsFrom(
                                actionInfo.mergedWith(loopingContext.initsOnContinue));
            }
        }
        if ((this.condition.implicitConversion & TypeIds.UNBOXING) != 0) {
            this.condition.checkNPE(currentScope, flowContext, initsOnCondition);
        }
      /* Reset reach mode, to address following scenario.
       *   final blank;
       *   do { if (true) break; else blank = 0; } while(false);
       *   blank = 1; // may be initialized already
       */
        actionInfo.setReachMode(previousMode);

        LoopingFlowContext condLoopContext;
        FlowInfo condInfo =
                this.condition.analyseCode(currentScope, (condLoopContext =
                        new LoopingFlowContext(flowContext, flowInfo, this, null, null, currentScope)), (this.action == null
                                                                                                         ? actionInfo : (actionInfo
                        .mergedWith(loopingContext.initsOnContinue))).copy());
        this.preConditionInitStateIndex = currentScope.methodScope().recordInitializationStates(actionInfo);
        if (!isConditionOptimizedFalse && this.continueLabel != null) {
            loopingContext.complainOnDeferredFinalChecks(currentScope, condInfo);
            condLoopContext.complainOnDeferredFinalChecks(currentScope, condInfo);
            loopingContext.complainOnDeferredNullChecks(currentScope, flowInfo.unconditionalCopy()
                                                                              .addPotentialNullInfoFrom(
                                                                                      condInfo.initsWhenTrue().unconditionalInits()));
            condLoopContext.complainOnDeferredNullChecks(currentScope,
                                                         actionInfo
                                                                 .addPotentialNullInfoFrom(condInfo.initsWhenTrue().unconditionalInits()));
        }
        if (loopingContext.hasEscapingExceptions()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=321926
            FlowInfo loopbackFlowInfo = flowInfo.copy();
            loopbackFlowInfo.mergedWith(condInfo.initsWhenTrue().unconditionalCopy());
            loopingContext.simulateThrowAfterLoopBack(loopbackFlowInfo);
        }
        // end of loop
        FlowInfo mergedInfo =
                FlowInfo.mergedOptimizedBranches(
                        (loopingContext.initsOnBreak.tagBits & FlowInfo.UNREACHABLE) != 0 ? loopingContext.initsOnBreak : flowInfo
                                .unconditionalCopy().addInitializationsFrom(loopingContext.initsOnBreak),
                        // recover upstream null info
                        isConditionOptimizedTrue,
                        (condInfo.tagBits & FlowInfo.UNREACHABLE) == 0 ? flowInfo.addInitializationsFrom(condInfo.initsWhenFalse())
                                                                       : condInfo,
                        // recover null inits from before condition analysis
                        false, // never consider opt false case for DO loop, since break can always occur (47776)
                        !isConditionTrue /*do{}while(true); unreachable(); */);
        this.mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
        return mergedInfo;
    }

    /** Do statement code generation */
    public void generateCode(BlockScope currentScope) {
        if ((this.bits & ASTNode.IsReachable) == 0) {
            return;
        }

        // labels management
        BranchLabel actionLabel = new BranchLabel();
        if (this.action != null)
            actionLabel.tagBits |= BranchLabel.USED;
        boolean hasContinueLabel = this.continueLabel != null;

        // generate action
        if (this.action != null) {
            this.action.generateCode(currentScope);
        }
        // continue label (135602)
        if (hasContinueLabel) {
            // May loose some local variable initializations : affecting the local variable attributes
            // generate condition
            Constant cst = this.condition.optimizedBooleanConstant();
            boolean isConditionOptimizedFalse = cst != Constant.NotAConstant && cst.booleanValue() == false;
            if (isConditionOptimizedFalse) {
                this.condition.generateCode(currentScope, false);
            } else {
                this.condition.generateOptimizedBoolean(currentScope, actionLabel, null, true);
            }
        }
    }

    public StringBuffer printStatement(int indent, StringBuffer output) {
        printIndent(indent, output).append("do"); //$NON-NLS-1$
        if (this.action == null)
            output.append(" ;\n"); //$NON-NLS-1$
        else {
            output.append('\n');
            this.action.printStatement(indent + 1, output).append('\n');
        }
        output.append("while ("); //$NON-NLS-1$
        return this.condition.printExpression(0, output).append(");"); //$NON-NLS-1$
    }

    public void resolve(BlockScope scope) {
        TypeBinding type = this.condition.resolveTypeExpecting(scope, TypeBinding.BOOLEAN);
        this.condition.computeConversion(scope, type, type);
        if (this.action != null)
            this.action.resolve(scope);
    }

    public void traverse(ASTVisitor visitor, BlockScope scope) {
        if (visitor.visit(this, scope)) {
            if (this.action != null) {
                this.action.traverse(visitor, scope);
            }
            this.condition.traverse(visitor, scope);
        }
        visitor.endVisit(this, scope);
    }
}
