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
package org.eclipse.che.ide.ext.java.jdt.codeassistant;

import org.eclipse.che.ide.ext.java.jdt.core.CompletionProposal;
import org.eclipse.che.ide.ext.java.jdt.core.IJavaElement;
import org.eclipse.che.ide.ext.java.jdt.core.Signature;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.util.SignatureUtil;
import org.eclipse.che.ide.ext.java.worker.WorkerTypeInfoStorage;

/** Proposal info that computes the javadoc lazily when it is queried. */
public final class TypeProposalInfo extends MemberProposalInfo {

    /**
     * Creates a new proposal info.
     *
     * @param project
     *         the java project to reference when resolving types
     * @param proposal
     *         the proposal to generate information for
     */
    public TypeProposalInfo(CompletionProposal proposal, String projectId, String docContext, String vfsId) {
        super(proposal, projectId, docContext, vfsId);
    }

    /** @see org.eclipse.che.ide.ext.java.jdt.codeassistant.MemberProposalInfo#getURL() */
    @Override
    protected String getURL() {
        return docContext + Signature.toString(new String(fProposal.getSignature())) + "&projectid=" + projectId
               + "&vfsid=" + vfsId + "&isclass=true";

    }

    /** @see org.eclipse.che.ide.ext.java.jdt.codeassistant.MemberProposalInfo#getJavaElement() */
    @Override
    public IJavaElement getJavaElement() {
        String fqn = String.valueOf(SignatureUtil.stripSignatureToFQN(String.valueOf(fProposal.getSignature())));
        return WorkerTypeInfoStorage.get().getTypeByFqn(fqn);
    }

}
