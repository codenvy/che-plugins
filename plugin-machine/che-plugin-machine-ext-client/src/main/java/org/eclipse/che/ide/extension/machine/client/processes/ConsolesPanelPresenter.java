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
package org.eclipse.che.ide.extension.machine.client.processes;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateHandler;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.inject.factories.EntityFactory;
import org.eclipse.che.ide.extension.machine.client.inject.factories.TerminalFactory;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.outputspanel.console.OutputConsole;
import org.eclipse.che.ide.extension.machine.client.perspective.terminal.TerminalPresenter;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.extension.machine.client.processes.ProcessTreeNode.ProcessNodeType.ROOT_NODE;
import static org.eclipse.che.ide.extension.machine.client.processes.ProcessTreeNode.ProcessNodeType.MACHINE_NODE;
import static org.eclipse.che.ide.extension.machine.client.processes.ProcessTreeNode.ProcessNodeType.COMMAND_NODE;
import static org.eclipse.che.ide.extension.machine.client.processes.ProcessTreeNode.ProcessNodeType.TERMINAL_NODE;
import static org.eclipse.che.ide.extension.machine.client.perspective.terminal.TerminalPresenter.TerminalStateListener;

/**
 * Presenter for managing machines process and terminals.
 *
 * @author Anna Shumilova
 * @author Roman Nikitenko
 */
@Singleton
public class ConsolesPanelPresenter extends BasePresenter implements ConsolesPanelView.ActionDelegate, HasView {

    private static final String DEFAULT_TERMINAL_NAME = "Terminal";

    private final DialogFactory               dialogFactory;
    private final NotificationManager         notificationManager;
    private final MachineLocalizationConstant localizationConstant;
    private final TerminalFactory             terminalFactory;
    private final ConsolesPanelView           view;
    private final MachineResources            resources;
    private final AppContext                  appContext;
    private final MachineServiceClient        machineService;
    private final EntityFactory               entityFactory;

    ProcessTreeNode                rootNode;
    Map<String, TerminalPresenter> terminals;
    Map<String, OutputConsole>     commandConsoles;

    @Inject
    public ConsolesPanelPresenter(ConsolesPanelView view,
                                  EventBus eventBus,
                                  TerminalFactory terminalFactory,
                                  DialogFactory dialogFactory,
                                  NotificationManager notificationManager,
                                  MachineLocalizationConstant localizationConstant,
                                  MachineServiceClient machineService,
                                  EntityFactory entityFactory,
                                  MachineResources resources,
                                  AppContext appContext) {
        this.view = view;
        this.terminalFactory = terminalFactory;
        this.dialogFactory = dialogFactory;
        this.notificationManager = notificationManager;
        this.localizationConstant = localizationConstant;
        this.resources = resources;
        this.entityFactory = entityFactory;
        this.appContext = appContext;
        this.machineService = machineService;
        this.terminals = new HashMap<>();
        this.commandConsoles = new HashMap<>();

        this.fetchMachines();
        this.view.setDelegate(this);
        this.view.setTitle(localizationConstant.viewConsolesTitle());

        eventBus.addHandler(DevMachineStateEvent.TYPE, new DevMachineStateHandler() {
            @Override
            public void onMachineStarted(DevMachineStateEvent event) {
                fetchMachines();
            }

            @Override
            public void onMachineDestroyed(DevMachineStateEvent event) {

            }
        });
    }

    @Override
    public View getView() {
        return view;
    }

    @NotNull
    @Override
    public String getTitle() {
        return localizationConstant.viewConsolesTitle();
    }

    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    @Nullable
    @Override
    public SVGResource getTitleSVGImage() {
        return resources.terminal();
    }

    @Override
    public String getTitleToolTip() {
        return localizationConstant.viewProcessesTooltip();
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** Get the list of all available machines. */
    public void fetchMachines() {
        String workspaceId = appContext.getWorkspace().getId();

        Promise<List<MachineDto>> machinesPromise = machineService.getWorkspaceMachines(workspaceId);

        machinesPromise.then(new Operation<List<MachineDto>>() {
            @Override
            public void apply(List<MachineDto> machines) throws OperationException {
                List<ProcessTreeNode> rootChildren = new ArrayList<>();

                rootNode = new ProcessTreeNode(ROOT_NODE, null, null, rootChildren);
                for (MachineDto descriptor : machines) {
                    if (descriptor.isDev()) {
                        List<ProcessTreeNode> processTreeNodes = new ArrayList<ProcessTreeNode>();
                        ProcessTreeNode machineNode = new ProcessTreeNode(MACHINE_NODE, rootNode, descriptor, processTreeNodes);
                        rootChildren.add(machineNode);
                        view.setProcessesData(rootNode);
                    }
                }

            }
        });
    }

    /**
     * Adds command node to process tree and displays command output
     *
     * @param machineId
     *         id of machine in which the command will be executed
     * @param configuration
     *         command configuration of the command which will be executed
     * @param outputConsole
     *         the console for command output
     */

    public void addCommand(@NotNull String machineId, @NotNull final CommandConfiguration configuration,
                           @NotNull OutputConsole outputConsole) {
        ProcessTreeNode machineTreeNode = findProcessTreeNodeById(machineId);
        if (machineTreeNode == null) {
            notificationManager.showError(localizationConstant.machineNotFound(machineId));
            Log.error(getClass(), localizationConstant.machineNotFound(machineId));
            return;
        }

        String commandId;
        ProcessTreeNode processTreeNode = getProcessTreeNodeByName(configuration.getName(), machineTreeNode);
        if (processTreeNode != null && isCommandStopped(processTreeNode.getId())) {
            commandId = processTreeNode.getId();
            view.hideProcessOutput(commandId);
            updateCommandOutput(commandId, outputConsole);
        } else {
            ProcessTreeNode commandNode = new ProcessTreeNode(COMMAND_NODE, machineTreeNode, configuration, null);
            commandId = commandNode.getId();
            view.addProcessNode(commandNode);
            addChildToMachineNode(commandNode, machineTreeNode);
            updateCommandOutput(commandId, outputConsole);
        }
    }

    /**
     * Adds new terminal to the processes panel
     *
     * @param machineId
     *         id of machine in which the terminal will be added
     */

    @Override
    public void onAddTerminal(@NotNull final String machineId) {
        machineService.getMachine(machineId).then(new Operation<MachineDto>() {
            @Override
            public void apply(MachineDto arg) throws OperationException {
                Machine machine = entityFactory.createMachine(arg);
                final ProcessTreeNode machineTreeNode = findProcessTreeNodeById(machineId);

                if (machineTreeNode == null) {
                    notificationManager.showError(localizationConstant.machineNotFound(machineId));
                    Log.error(getClass(), localizationConstant.machineNotFound(machineId));
                    return;
                }

                final TerminalPresenter newTerminal = terminalFactory.create(machine);
                final IsWidget terminalWidget = newTerminal.getView();
                final String terminalName = getUniqueTerminalName(machineTreeNode);
                final ProcessTreeNode terminalNode = new ProcessTreeNode(TERMINAL_NODE, machineTreeNode, terminalName, null);
                addChildToMachineNode(terminalNode, machineTreeNode);

                final String terminalId = terminalNode.getId();
                terminals.put(terminalId, newTerminal);
                view.addProcessWidget(terminalId, terminalWidget);
                view.addProcessNode(terminalNode);

                newTerminal.setVisible(true);
                newTerminal.connect();
                newTerminal.setListener(new TerminalStateListener() {
                    @Override
                    public void onExit() {
                        onStopProcess(terminalNode);
                        terminals.remove(terminalId);
                    }
                });
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.showError(localizationConstant.machineNotFound(machineId));
                Log.error(getClass(), "Can not get machine " + machineId);
            }
        });

    }

    @Override
    public void onCommandSelected(@NotNull String commandId) {
        view.showProcessOutput(commandId);
    }

    @Override
    public void onCloseCommandConsole(@NotNull ProcessTreeNode node) {
        String commandId = node.getId();
        if (!commandConsoles.containsKey(commandId)) {
            return;
        }

        OutputConsole outputConsole = commandConsoles.get(commandId);
        if (outputConsole.isFinished()) {
            onStopProcess(node);
            commandConsoles.remove(commandId);
            return;
        }

        dialogFactory.createConfirmDialog("", localizationConstant.outputsConsoleViewStopProcessConfirmation(outputConsole.getTitle()),
                                          getConfirmCloseConsoleCallback(outputConsole, node), null)
                     .show();

    }

    private ConfirmCallback getConfirmCloseConsoleCallback(final OutputConsole outputConsole, final ProcessTreeNode node) {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                outputConsole.onClose();
                onStopProcess(node);
                commandConsoles.remove(node.getId());
            }
        };
    }

    @Override
    public void onTerminalSelected(@NotNull String terminalId) {
        view.showProcessOutput(terminalId);
    }

    @Override
    public void onCloseTerminal(@NotNull ProcessTreeNode node) {
        String terminalId = node.getId();
        if (terminals.containsKey(terminalId)) {
            onStopProcess(node);
            terminals.get(terminalId).stopTerminal();
            terminals.remove(terminalId);
        }
    }

    private void onStopProcess(@NotNull ProcessTreeNode node) {
        String processId = node.getId();
        ProcessTreeNode parentNode = node.getParent();

        int processIndex = view.getNodeIndex(processId);
        if (processIndex == -1) {
            return;
        }

        int countWidgets = terminals.size() + commandConsoles.size();
        if (countWidgets == 1) {
            view.hideProcessOutput(processId);
            removeChildFromMachineNode(node, parentNode);
            return;
        }

        int neighborIndex = processIndex > 0 ? processIndex - 1 : processIndex + 1;
        ProcessTreeNode neighborNode = view.getNodeByIndex(neighborIndex);
        removeChildFromMachineNode(node, parentNode);
        view.selectNode(neighborNode);

        view.showProcessOutput(neighborNode.getId());
        view.hideProcessOutput(processId);
    }

    private void addChildToMachineNode(ProcessTreeNode childNode, ProcessTreeNode machineTreeNode) {
        machineTreeNode.getChildren().add(childNode);
        view.setProcessesData(rootNode);
        view.selectNode(childNode);
    }

    private void removeChildFromMachineNode(ProcessTreeNode childNode, ProcessTreeNode machineTreeNode) {
        view.removeProcessNode(childNode);
        machineTreeNode.getChildren().remove(childNode);
        view.setProcessesData(rootNode);
    }

    private ProcessTreeNode findProcessTreeNodeById(@NotNull String id) {
        for (ProcessTreeNode processTreeNode : rootNode.getChildren()) {
            if (id.equals(processTreeNode.getId())) {
                return processTreeNode;
            }
        }
        return null;
    }

    private String getUniqueTerminalName(ProcessTreeNode machineNode) {
        String terminalName = DEFAULT_TERMINAL_NAME;
        if (!isTerminalNameExist(machineNode, terminalName)) {
            return DEFAULT_TERMINAL_NAME;
        }

        int counter = 2;
        do {
            terminalName = localizationConstant.viewProcessesTerminalNodeTitle(String.valueOf(counter));
            counter++;
        } while (isTerminalNameExist(machineNode, terminalName));
        return terminalName;
    }

    private boolean isTerminalNameExist(ProcessTreeNode machineNode, String terminalName) {
        for (ProcessTreeNode node : machineNode.getChildren()) {
            if (TERMINAL_NODE == node.getType() && node.getName().equals(terminalName)) {
                return true;
            }
        }
        return false;
    }

    private ProcessTreeNode getProcessTreeNodeByName(String processName, ProcessTreeNode machineTreeNode) {
        for (ProcessTreeNode processTreeNode : machineTreeNode.getChildren()) {
            if (processTreeNode.getName().equals(processName)) {
                return processTreeNode;
            }
        }
        return null;
    }

    private boolean isCommandStopped(String commandId) {
        return commandConsoles.containsKey(commandId) && commandConsoles.get(commandId).isFinished();
    }

    private void updateCommandOutput(@NotNull final String commandId, @NotNull OutputConsole outputConsole) {
        commandConsoles.put(commandId, outputConsole);
        outputConsole.go(new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget widget) {
                view.addProcessWidget(commandId, widget);
                view.selectNode(view.getNodeById(commandId));
            }
        });
    }
}
