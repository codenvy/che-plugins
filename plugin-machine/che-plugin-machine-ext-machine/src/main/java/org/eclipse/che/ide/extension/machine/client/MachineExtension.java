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
package org.eclipse.che.ide.extension.machine.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.extension.machine.client.actions.SelectCommandAction;
import org.eclipse.che.ide.extension.machine.client.actions.EditCommandsAction;
import org.eclipse.che.ide.extension.machine.client.actions.ExecuteArbitraryCommandAction;
import org.eclipse.che.ide.extension.machine.client.actions.ExecuteSelectedCommandAction;
import org.eclipse.che.ide.extension.machine.client.actions.TerminateMachineAction;
import org.eclipse.che.ide.extension.machine.client.console.ClearConsoleAction;
import org.eclipse.che.ide.extension.machine.client.console.MachineConsolePresenter;
import org.eclipse.che.ide.extension.machine.client.console.MachineConsoleToolbar;
import org.eclipse.che.ide.extension.machine.client.machine.MachineManager;
import org.eclipse.che.ide.ui.toolbar.ToolbarPresenter;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_CODE;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RIGHT_TOOLBAR;
import static org.eclipse.che.ide.api.constraints.Anchor.AFTER;
import static org.eclipse.che.ide.api.constraints.Constraints.FIRST;

/**
 * Machine extension entry point.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
@Extension(title = "Machine", version = "1.0.0")
public class MachineExtension {

    public static final String GROUP_MACHINE_CONSOLE_TOOLBAR = "MachineConsoleToolbar";
    public static final String GROUP_MACHINE_TOOLBAR         = "MachineGroupToolbar";
    public static final String GROUP_COMMANDS_LIST           = "CommandsListGroup";

    @Inject
    public MachineExtension(final MachineResources machineResources,
                            final EventBus eventBus,
                            final AppContext appContext,
                            final MachineServiceClient machineServiceClient,
                            final MachineManager machineManager,
                            final MachineConsolePresenter machineConsolePresenter) {
        machineResources.machine().ensureInjected();

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                final String projectPath = event.getProject().getPath();

                // start machine and bind project
                Promise<Array<MachineDescriptor>> machinesPromise = machineServiceClient.getMachines(appContext.getWorkspace().getId(),
                                                                                                     projectPath);
                machinesPromise.then(new Operation<Array<MachineDescriptor>>() {
                    @Override
                    public void apply(Array<MachineDescriptor> arg) throws OperationException {
                        if (arg.isEmpty()) {
                            machineManager.startMachineAndBindProject(projectPath);
                        } else {
                            machineManager.setCurrentMachineId(arg.get(0).getId());
                        }
                    }
                });
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
                //nothing to do for now
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                machineManager.setCurrentMachineId(null);
                machineConsolePresenter.clear();
            }
        });
    }

    @Inject
    private void prepareActions(MachineLocalizationConstant localizationConstant,
                                ActionManager actionManager,
                                ExecuteArbitraryCommandAction executeArbitraryCommandAction,
                                ExecuteSelectedCommandAction executeSelectedCommandAction,
                                SelectCommandAction selectCommandAction,
                                EditCommandsAction editCommandsAction,
                                TerminateMachineAction terminateMachineAction) {
        final DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(GROUP_MAIN_MENU);
        final DefaultActionGroup runMenu = new DefaultActionGroup(localizationConstant.mainMenuRunName(), true, actionManager);

        // register actions
        actionManager.registerAction("run", runMenu);
        actionManager.registerAction("executeArbitraryCommand", executeArbitraryCommandAction);
        actionManager.registerAction("editCommands", editCommandsAction);
        actionManager.registerAction("terminateMachine", terminateMachineAction);
        actionManager.registerAction("selectCommandAction", selectCommandAction);
        actionManager.registerAction("executeSelectedCommand", executeSelectedCommandAction);

        // add actions in main menu
        mainMenu.add(runMenu, new Constraints(AFTER, GROUP_CODE));
        runMenu.add(executeArbitraryCommandAction);
        runMenu.add(editCommandsAction);
        runMenu.addSeparator();
        runMenu.add(terminateMachineAction);

        // add actions on right toolbar
        final DefaultActionGroup rightToolbarGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RIGHT_TOOLBAR);
        final DefaultActionGroup machineToolbarGroup = new DefaultActionGroup(GROUP_MACHINE_TOOLBAR, false, actionManager);
        actionManager.registerAction(GROUP_MACHINE_TOOLBAR, machineToolbarGroup);
        rightToolbarGroup.add(machineToolbarGroup);
        machineToolbarGroup.add(selectCommandAction);
        machineToolbarGroup.add(executeSelectedCommandAction);

        // add group for command list
        final DefaultActionGroup commandList = new DefaultActionGroup(GROUP_COMMANDS_LIST, false, actionManager);
        actionManager.registerAction(GROUP_COMMANDS_LIST, commandList);
        commandList.add(editCommandsAction, FIRST);
        commandList.addSeparator();
    }

    @Inject
    private void prepareMachineConsole(ActionManager actionManager,
                                       ClearConsoleAction clearConsoleAction,
                                       WorkspaceAgent workspaceAgent,
                                       MachineConsolePresenter machineConsolePresenter,
                                       @MachineConsoleToolbar ToolbarPresenter machineConsoleToolbar) {
        workspaceAgent.openPart(machineConsolePresenter, PartStackType.INFORMATION);

        // add toolbar to Machine console
        final DefaultActionGroup consoleToolbarActionGroup = new DefaultActionGroup(GROUP_MACHINE_CONSOLE_TOOLBAR, false, actionManager);
        consoleToolbarActionGroup.add(clearConsoleAction);
        consoleToolbarActionGroup.addSeparator();
        machineConsoleToolbar.bindMainGroup(consoleToolbarActionGroup);
    }
}
