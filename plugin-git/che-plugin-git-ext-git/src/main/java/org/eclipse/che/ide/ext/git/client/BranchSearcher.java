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
package org.eclipse.che.ide.ext.git.client;

import org.eclipse.che.api.git.shared.Branch;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergii Leschenko
 */
public class BranchSearcher {

    /**
     * Get values of remote branches: filter remote branches due to selected remote repository.
     *
     * @param remoteName
     *         remote name for filtering
     * @param remoteBranches
     *         remote branches
     */
    @Nonnull
    public List<String> getRemoteBranchesToDisplay(@Nonnull String remoteName, @Nonnull List<Branch> remoteBranches) {
        return getRemoteBranchesToDisplay(new BranchFilterByRemote(remoteName), remoteBranches);
    }

    /**
     * Get simple names of remote branches: filter remote branches due to selected remote repository.
     */
    @Nonnull
    public List<String> getRemoteBranchesToDisplay(BranchFilterByRemote filterByRemote, @Nonnull List<Branch> remoteBranches) {
        List<String> branches = new ArrayList<>();

        if (remoteBranches.isEmpty()) {
            branches.add("master");
            return branches;
        }

        for (int i = 0; i < remoteBranches.size(); i++) {
            Branch branch = remoteBranches.get(i);
            if (filterByRemote.isLinkedTo(branch)) {
                branches.add(filterByRemote.getBranchNameWithoutRefs(branch));
            }
        }

        if (branches.isEmpty()) {
            branches.add("master");
        }
        return branches;
    }

    /**
     * Get simple names of local branches.
     *
     * @param localBranches
     *         local branches
     */
    @Nonnull
    public List<String> getLocalBranchesToDisplay(@Nonnull List<Branch> localBranches) {
        List<String> branches = new ArrayList<>();

        if (localBranches.isEmpty()) {
            branches.add("master");
            return branches;
        }

        for (Branch branch : localBranches) {
            branches.add(branch.getDisplayName());
        }

        return branches;
    }
}
