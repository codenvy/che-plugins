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
package org.eclipse.che.ide.ext.svn.client;

import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.ext.svn.client.action.AddAction;
import org.eclipse.che.ide.ext.svn.client.action.ApplyPatchAction;
import org.eclipse.che.ide.ext.svn.client.action.BranchTagAction;
import org.eclipse.che.ide.ext.svn.client.action.ChangeCredentialsAction;
import org.eclipse.che.ide.ext.svn.client.action.CleanupAction;
import org.eclipse.che.ide.ext.svn.client.action.CommitAction;
import org.eclipse.che.ide.ext.svn.client.action.CopyAction;
import org.eclipse.che.ide.ext.svn.client.action.CreatePatchAction;
import org.eclipse.che.ide.ext.svn.client.action.DiffAction;
import org.eclipse.che.ide.ext.svn.client.action.ExportAction;
import org.eclipse.che.ide.ext.svn.client.action.LockAction;
import org.eclipse.che.ide.ext.svn.client.action.LogAction;
import org.eclipse.che.ide.ext.svn.client.action.MergeAction;
import org.eclipse.che.ide.ext.svn.client.action.MoveAction;
import org.eclipse.che.ide.ext.svn.client.action.PropertiesAction;
import org.eclipse.che.ide.ext.svn.client.action.RelocateAction;
import org.eclipse.che.ide.ext.svn.client.action.RemoveAction;
import org.eclipse.che.ide.ext.svn.client.action.RenameAction;
import org.eclipse.che.ide.ext.svn.client.action.ResolveAction;
import org.eclipse.che.ide.ext.svn.client.action.RevertAction;
import org.eclipse.che.ide.ext.svn.client.action.StatusAction;
import org.eclipse.che.ide.ext.svn.client.action.SwitchAction;
import org.eclipse.che.ide.ext.svn.client.action.UnlockAction;
import org.eclipse.che.ide.ext.svn.client.action.UpdateAction;
import org.eclipse.che.ide.ext.svn.client.action.UpdateToRevisionAction;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Extension adding <a href="http://subversion.apache.org">Subversion</a> support to the
 * <a href="http://codenvy.com">Codenvy</a> IDE.
 *
 * @author Jeremy Whitlock
 */
@Singleton
@Extension(title = "Subversion Support", version = "1.0.0")
public class SubversionExtension {

    public final String FILE_COMMAND_GROUP          = "SvnFileCommandGroup";
    public final String REMOTE_COMMAND_GROUP        = "SvnRemoteCommandGroup";
    public final String REPOSITORY_COMMAND_GROUP    = "SvnRepositoryCommandGroup";
    public final String ADD_COMMAND_GROUP           = "SvnAddCommandGroup";
    public final String MISCELLANEOUS_COMMAND_GROUP = "SvnMiscellaneousCommandGroup";
    public final String CREDENTIALS_COMMAND_GROUP   = "SvnCredentialsCommandGroup";
    public final String SVN_GROUP_MAIN_MENU;

    /**
     * Constructor.
     */
    @Inject
    public SubversionExtension(final ActionManager actionManager,
                               final AddAction addAction,
                               final ApplyPatchAction applyPatchAction,
                               final BranchTagAction branchTagAction,
                               final ChangeCredentialsAction changeCredentialsAction,
                               final CleanupAction cleanupAction,
                               final CommitAction commitAction,
                               final CopyAction copyAction,
                               final MoveAction moveAction,
                               final CreatePatchAction createPatchAction,
                               final DiffAction diffAction,
                               final ExportAction exportAction,
                               final LockAction lockAction,
                               final LogAction logAction,
                               final MergeAction mergeAction,
                               final PropertiesAction propertiesAction,
                               final RelocateAction relocateAction,
                               final RemoveAction removeAction,
                               final RenameAction renameAction,
                               final ResolveAction resolveAction,
                               final RevertAction revertAction,
                               final StatusAction statusAction,
                               final SwitchAction switchAction,
                               final UnlockAction unlockAction,
                               final UpdateAction updateAction,
                               final UpdateToRevisionAction updateToRevisionAction,
                               final SubversionExtensionLocalizationConstants constants,
                               final SubversionExtensionResources resources) {
        SVN_GROUP_MAIN_MENU = constants.subversionLabel();

        final Constraints beforeWindow = new Constraints(Anchor.BEFORE, IdeActions.GROUP_WINDOW);
        final DefaultActionGroup addCommandGroup = new DefaultActionGroup(ADD_COMMAND_GROUP, false, actionManager);
        final DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_MAIN_MENU);
        final DefaultActionGroup fileCommandGroup = new DefaultActionGroup(FILE_COMMAND_GROUP, false, actionManager);
        final DefaultActionGroup miscellaneousCommandGroup = new DefaultActionGroup(MISCELLANEOUS_COMMAND_GROUP, false,
                                                                                    actionManager);
        final DefaultActionGroup remoteCommandGroup = new DefaultActionGroup(REMOTE_COMMAND_GROUP, false,
                                                                             actionManager);
        final DefaultActionGroup repositoryCommandGroup = new DefaultActionGroup(REPOSITORY_COMMAND_GROUP, false,
                                                                                 actionManager);
        final DefaultActionGroup credentialsCommandGroup = new DefaultActionGroup(CREDENTIALS_COMMAND_GROUP, false,
                                                                                  actionManager);
        final DefaultActionGroup svnMenu = new DefaultActionGroup(SVN_GROUP_MAIN_MENU, true, actionManager);

        resources.subversionCSS().ensureInjected();

        // Register action groups
        actionManager.registerAction(SVN_GROUP_MAIN_MENU, svnMenu);
        mainMenu.add(svnMenu, beforeWindow);

        actionManager.registerAction(REMOTE_COMMAND_GROUP, remoteCommandGroup);
        svnMenu.add(remoteCommandGroup);
        svnMenu.addSeparator();

        actionManager.registerAction(FILE_COMMAND_GROUP, fileCommandGroup);
        svnMenu.add(fileCommandGroup);
        svnMenu.addSeparator();

        actionManager.registerAction(REPOSITORY_COMMAND_GROUP, repositoryCommandGroup);
        svnMenu.add(repositoryCommandGroup);
        svnMenu.addSeparator();

        actionManager.registerAction(ADD_COMMAND_GROUP, addCommandGroup);
        svnMenu.add(addCommandGroup);
        svnMenu.addSeparator();

        actionManager.registerAction(MISCELLANEOUS_COMMAND_GROUP, miscellaneousCommandGroup);
        svnMenu.add(miscellaneousCommandGroup);
        svnMenu.addSeparator();

        actionManager.registerAction(CREDENTIALS_COMMAND_GROUP, credentialsCommandGroup);
        svnMenu.add(credentialsCommandGroup);

        // Register actions

        // Commands that provide status of project or files
        actionManager.registerAction("SvnStatus", statusAction);
        remoteCommandGroup.add(statusAction);
        actionManager.registerAction("SvnViewLog", logAction);
        remoteCommandGroup.add(logAction);
        actionManager.registerAction("SvnDiff", diffAction);
        remoteCommandGroup.add(diffAction);

        // Commands that manage pull and push of changes
        actionManager.registerAction("SvnUpdate", updateAction);
        fileCommandGroup.add(updateAction);
        actionManager.registerAction("SvnUpdateToRevision",
                updateToRevisionAction);
        fileCommandGroup.add(updateToRevisionAction);
        actionManager.registerAction("SvnCommit", commitAction);
        fileCommandGroup.add(commitAction);
        actionManager.registerAction("SvnResolve", resolveAction);
        fileCommandGroup.add(resolveAction);
        actionManager.registerAction("SvnCopy", copyAction);
        fileCommandGroup.add(copyAction);
        actionManager.registerAction("SvnMove", moveAction);
        fileCommandGroup.add(moveAction);

        // Commands that interact with the repository
        // actionManager.registerAction("SvnMerge", mergeAction);
        // repositoryCommandGroup.add(mergeAction);
        actionManager.registerAction("SvnExport", exportAction);
        repositoryCommandGroup.add(exportAction);
        // actionManager.registerAction("SvnBranchTag", branchTagAction);
        // repositoryCommandGroup.add(branchTagAction);

        // Commands for miscellany
        // actionManager.registerAction("SvnCreatePatch", createPatchAction);
        // miscellaneousCommandGroup.add(createPatchAction);
        // actionManager.registerAction("SvnApplyPatch", applyPatchAction);
        // miscellaneousCommandGroup.add(applyPatchAction);
        // actionManager.registerAction("SvnProperties", propertiesAction);
        // miscellaneousCommandGroup.add(propertiesAction);

        // Commands that manage working copy
        actionManager.registerAction("SvnAdd", addAction);
        addCommandGroup.add(addAction);
        actionManager.registerAction("SvnRemove", removeAction);
        addCommandGroup.add(removeAction);
        actionManager.registerAction("SvnRevert", revertAction);
        addCommandGroup.add(revertAction);
        // actionManager.registerAction("SvnRename", renameAction);
        // addCommandGroup.add(renameAction);
        actionManager.registerAction("SvnLock", lockAction);
        addCommandGroup.add(lockAction);
        actionManager.registerAction("SvnUnlock", unlockAction);
        addCommandGroup.add(unlockAction);
        actionManager.registerAction("SvnCleanup", cleanupAction);
        addCommandGroup.add(cleanupAction);

        actionManager.registerAction("SvnChangeCredentials", changeCredentialsAction);
        credentialsCommandGroup.add(changeCredentialsAction);
    }

}
