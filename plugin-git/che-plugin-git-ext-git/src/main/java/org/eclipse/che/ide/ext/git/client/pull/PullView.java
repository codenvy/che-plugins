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
package org.eclipse.che.ide.ext.git.client.pull;

import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.collections.Array;

import javax.annotation.Nonnull;

/**
 * The view of {@link PullPresenter}.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public interface PullView extends View<PullView.ActionDelegate> {
    /** Needs for delegate some function into Pull view. */
    public interface ActionDelegate {
        /** Performs any actions appropriate in response to the user having pressed the Pull button. */
        void onPullClicked();

        /** Performs any actions appropriate in response to the user having pressed the Cancel button. */
        void onCancelClicked();
        
        /** Performs any actions appropriate in response to the remote branch value changed. */
        void onRemoteBranchChanged();
    }

    /**
     * Returns selected repository name.
     *
     * @return repository name.
     */
    @Nonnull
    String getRepositoryName();

    /**
     * Returns selected repository url.
     *
     * @return repository url.
     */
    @Nonnull
    String getRepositoryUrl();

    /**
     * Sets available repositories.
     *
     * @param repositories
     *         available repositories
     */
    void setRepositories(@Nonnull Array<Remote> repositories);

    /** @return local branch */
    @Nonnull
    String getLocalBranch();
    
    /** 
     * Selects pointed local branch
     * 
     * @param branch local branch to select
     */
    void selectLocalBranch(@Nonnull String branch);
    
    /** 
     * Selects pointed remote branch
     * 
     * @param branch remote branch to select
     */
    void selectRemoteBranch(@Nonnull String branch);

    /**
     * Set local branches into view.
     *
     * @param branches
     *         local branches
     */
    void setLocalBranches(@Nonnull Array<String> branches);

    /** @return remote branches */
    @Nonnull
    String getRemoteBranch();

    /**
     * Set remote branches into view.
     *
     * @param branches
     *         remote branches
     */
    void setRemoteBranches(@Nonnull Array<String> branches);

    /**
     * Change the enable state of the push button.
     *
     * @param enabled
     *         <code>true</code> to enable the button, <code>false</code> to disable it
     */
    void setEnablePullButton(boolean enabled);

    /** Close dialog. */
    void close();

    /** Show dialog. */
    void showDialog();
}