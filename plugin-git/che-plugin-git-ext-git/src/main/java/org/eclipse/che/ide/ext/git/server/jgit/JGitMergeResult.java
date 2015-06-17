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
package org.eclipse.che.ide.ext.git.server.jgit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.che.ide.ext.git.shared.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.ResolveMerger;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: JGitMergeResult.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
public class JGitMergeResult implements MergeResult {
    private final org.eclipse.jgit.api.MergeResult jgitMergeResult;

    /**
     * @param jgitMergeResult back-end instance
     */
    public JGitMergeResult(org.eclipse.jgit.api.MergeResult jgitMergeResult) {
        this.jgitMergeResult = jgitMergeResult;
    }

    /** @see org.exoplatform.ide.git.shared.MergeResult#getNewHead() */
    @Override
    public String getNewHead() {
        ObjectId newHead = jgitMergeResult.getNewHead();
        if (newHead != null) {
            return newHead.getName();
        }
        // Merge failed.
        return null;
    }

    /** @see org.exoplatform.ide.git.shared.MergeResult#getMergeStatus() */
    @Override
    public MergeStatus getMergeStatus() {
        switch (jgitMergeResult.getMergeStatus()) {
            case ALREADY_UP_TO_DATE:
                return MergeStatus.ALREADY_UP_TO_DATE;
            case CONFLICTING:
                return MergeStatus.CONFLICTING;
            case FAILED:
                return MergeStatus.FAILED;
            case FAST_FORWARD:
                return MergeStatus.FAST_FORWARD;
            case MERGED:
                return MergeStatus.MERGED;
            case NOT_SUPPORTED:
                return MergeStatus.NOT_SUPPORTED;
            case CHECKOUT_CONFLICT:
                return MergeStatus.CONFLICTING;
        }
        throw new IllegalStateException("Unknown merge status " + jgitMergeResult.getMergeStatus());
    }

    /** @see org.exoplatform.ide.git.shared.MergeResult#getMergedCommits() */
    @Override
    public List<String> getMergedCommits() {
        ObjectId[] jgitMergedCommits = jgitMergeResult.getMergedCommits();
        List<String> mergedCommits = new ArrayList<String>();
        if (jgitMergedCommits != null) {
            for (ObjectId objectId : jgitMergedCommits) {
                mergedCommits.add(objectId.getName());
            }
        }
        return mergedCommits;
    }

    /** @see org.exoplatform.ide.git.shared.MergeResult#getConflicts() */
    @Override
    public List<String> getConflicts() {
        if (jgitMergeResult.getMergeStatus().equals(org.eclipse.jgit.api.MergeResult.MergeStatus.CHECKOUT_CONFLICT)) {
            return jgitMergeResult.getCheckoutConflicts();
        }
        Map<String, int[][]> conflicts = jgitMergeResult.getConflicts();
        List<String> files = null;
        if (conflicts != null) {
            files = new ArrayList<String>(conflicts.size());
            files.addAll(conflicts.keySet());
        }
        return files;
    }

    /** @see org.exoplatform.ide.git.shared.MergeResult#getFailed() */
    @Override
    public List<String> getFailed() {
        List<String> files = null;
        Map<String, ResolveMerger.MergeFailureReason> failing = jgitMergeResult.getFailingPaths();
        if (failing != null) {
            files = new ArrayList<String>(failing.keySet());
        }
        return files;
    }

    /** @see java.lang.Object#toString() */
    @Override
    public String toString() {
        return "JGitMergeResult [getNewHead()=" + getNewHead() + ", getMergeStatus()=" + getMergeStatus()
                + ", getMergedCommits()=" + Arrays.toString(getMergedCommits().toArray()) + ", getConflicts()="
                + Arrays.toString(getConflicts().toArray()) + "]";
    }
}
