/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.codeassistant;

import org.eclipse.che.ide.ext.java.jdt.Images;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.ui.StyledString;
import org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal;
import org.eclipse.che.ide.runtime.Assert;


/** Java completion proposal for {@link CompletionProposal#FIELD_REF_WITH_CASTED_RECEIVER}. */
public class JavaFieldWithCastedReceiverCompletionProposal extends JavaCompletionProposalImpl {

    private CompletionProposal fProposal;

    public JavaFieldWithCastedReceiverCompletionProposal(String completion, int start, int length, Images image,
                                                         StyledString label, int relevance, boolean inJavadoc,
                                                         JavaContentAssistInvocationContext invocationContext,
                                                         CompletionProposal proposal) {
        super(completion, start, length, image, label, relevance, inJavadoc, invocationContext);
        Assert.isNotNull(proposal);
        fProposal = proposal;
    }

    /*
     * @see org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal#isPrefix(java.lang.String, java.lang.String)
     */
    @Override
    protected boolean isPrefix(String prefix, String string) {
        if (prefix != null)
            prefix = prefix.substring(fProposal.getReceiverEnd() - fProposal.getReceiverStart() + 1);
        return super.isPrefix(prefix, string);
    }

}
