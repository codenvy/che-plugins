package org.eclipse.che.jdt.core;

import org.eclipse.che.jdt.internal.codeassist.InternalCompletionProposal;
import org.eclipse.jdt.core.CompletionProposal;

/**
 * @author Evgen Vidolob
 */
public class CompletionProposalFactory {

    /**
     * Creates a basic completion proposal. All instance
     * field have plausible default values unless otherwise noted.
     * <p>
     * Note that the constructors for this class are internal to the
     * Java model implementation. Clients cannot directly create
     * CompletionProposal objects.
     * </p>
     *
     * @param kind one of the kind constants declared on this class
     * @param completionOffset original offset of code completion request
     * @return a new completion proposal
     */
    public static CompletionProposal create(int kind, int completionOffset) {
        return new InternalCompletionProposal(kind, completionOffset);
    }
}
