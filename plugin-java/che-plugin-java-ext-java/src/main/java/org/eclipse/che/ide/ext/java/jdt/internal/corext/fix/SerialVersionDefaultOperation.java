/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.internal.corext.fix;

import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Expression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.che.ide.runtime.Assert;


/** Proposal for a default serial version id. */
public final class SerialVersionDefaultOperation extends AbstractSerialVersionOperation {

    /**
     * Creates a new serial version default proposal.
     *
     * @param unit
     *         the compilation unit
     * @param nodes
     *         the originally selected nodes
     */
    public SerialVersionDefaultOperation(ASTNode[] nodes) {
        super(nodes);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean addInitializer(final VariableDeclarationFragment fragment, final ASTNode declarationNode) {
        Assert.isNotNull(fragment);

        final Expression expression = fragment.getAST().newNumberLiteral(DEFAULT_EXPRESSION);
        if (expression != null)
            fragment.setInitializer(expression);
        return true;
    }

    //   /**
    //    * {@inheritDoc}
    //    */
    //   @Override
    //   protected void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment,
    //      final LinkedProposalModel positionGroups)
    //   {
    //
    //      Assert.isNotNull(rewrite);
    //      Assert.isNotNull(fragment);
    //
    //      final Expression initializer = fragment.getInitializer();
    //      if (initializer != null)
    //      {
    //         LinkedProposalPositionGroup group = new LinkedProposalPositionGroup(GROUP_INITIALIZER);
    //         group.addPosition(rewrite.track(initializer), true);
    //         positionGroups.addPositionGroup(group);
    //      }
    //   }

}
