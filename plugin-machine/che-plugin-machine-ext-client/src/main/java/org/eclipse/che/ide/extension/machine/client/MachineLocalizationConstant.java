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

import com.google.gwt.i18n.client.Messages;

/**
 * I18n constants for the Machine extension.
 *
 * @author Artem Zatsarynnyy
 */
public interface MachineLocalizationConstant extends Messages {

    /* Buttons */
    @Key("button.apply")
    String applyButton();

    @Key("button.ok")
    String okButton();

    @Key("button.cancel")
    String cancelButton();


    /* Actions */
    @Key("mainMenu.run.name")
    String mainMenuRunName();

    @Key("main.menu.machine")
    String mainMenuMachine();

    @Key("control.selectCommand.text")
    String selectCommandControlTitle();

    @Key("control.selectCommand.description")
    String selectCommandControlDescription();

    @Key("control.runSelectedCommand.text")
    String executeSelectedCommandControlTitle();

    @Key("control.runSelectedCommand.description")
    String executeSelectedCommandControlDescription();

    @Key("control.editConfigurations.text")
    String editConfigurationsControlTitle();

    @Key("control.editConfigurations.description")
    String editConfigurationsControlDescription();

    @Key("control.clearMachineConsole.text")
    String clearConsoleControlTitle();

    @Key("control.clearMachineConsole.description")
    String clearConsoleControlDescription();

    @Key("control.machine.create.text")
    String machineCreateTitle();

    @Key("control.machine.create.description")
    String machineCreateDescription();

    @Key("control.machine.destroy.text")
    String machineDestroyTitle();

    @Key("control.machine.destroyByName.text")
    String machineDestroyTitle(String machineName);

    @Key("control.machine.destroy.description")
    String machineDestroyDescription();


    /* Messages */
    @Key("messages.noDevMachine")
    String noDevMachine();

    @Key("messages.devMachineCanNotBeDestroyed")
    String devMachineCanNotBeDestroyed();

    @Key("messages.devMachineCanNotBeDestroyed.title")
    String devMachineCanNotBeDestroyedTitle();


    /* MachineStateNotifier */
    @Key("notification.creatingMachine")
    String notificationCreatingMachine(String machineId);

    @Key("notification.machineIsRunning")
    String notificationMachineIsRunning(String machineId);

    @Key("notification.destroyingMachine")
    String notificationDestroyingMachine(String machineId);

    @Key("notification.machineDestroyed")
    String notificationMachineDestroyed(String machineId);


    /* MachinePanelPresenter */
    @Key("view.machinePanel.title")
    String machinePanelTitle();

    @Key("view.machinePanel.tooltip")
    String machinePanelTooltip();


    /* MachineConsoleView */
    @Key("view.machineConsole.title")
    String machineConsoleViewTitle();

    @Key("view.machineConsole.tooltip")
    String machineConsoleViewTooltip();


    /* OutputsContainerView */
    @Key("view.outputsConsole.title")
    String outputsConsoleViewTitle();

    @Key("view.outputsConsole.tooltip")
    String outputsConsoleViewTooltip();

    @Key("view.createMachine.title")
    String viewCreateMachineTitle();

    @Key("view.createMachine.button.create")
    String viewCreateMachineButtonCreate();

    @Key("view.createMachine.button.replace")
    String viewCreateMachineButtonReplace();

    @Key("view.createMachine.message.urlIsNotValid")
    String viewCreateMachineUrlIsNotValid();

    @Key("view.createMachine.name")
    String viewCreateMachineName();

    @Key("view.createMachine.recipeURL")
    String viewCreateMachineRecipeURL();


    /* EditConfigurationsView */
    @Key("view.editConfigurations.title")
    String editConfigurationsViewTitle();

    @Key("view.editConfigurations.hint")
    String editConfigurationsViewHint();

    @Key("view.editConfigurations.name.text")
    String editConfigurationsViewNameText();

    @Key("view.editConfigurations.add.text")
    String editConfigurationsViewAddText();

    @Key("view.editConfigurations.add.title")
    String editConfigurationsViewAddTitle();

    @Key("view.editConfigurations.remove.text")
    String editConfigurationsViewRemoveText();

    @Key("view.editConfigurations.remove.title")
    String editConfigurationsViewRemoveTitle();

    @Key("view.editConfigurations.remove.confirmation")
    String editConfigurationsRemoveConfirmation(String commandName);

    @Key("view.editConfigurations.saveChanges.title")
    String editConfigurationsSaveChangesTitle();

    @Key("view.editConfigurations.saveChanges.text")
    String editConfigurationsSaveChangesConfirmation(String commandName);

    @Key("view.editConfigurations.saveChanges.save")
    String editConfigurationsSaveChangesSave();

    @Key("view.editConfigurations.saveChanges.discard")
    String editConfigurationsSaveChangesDiscard();


    @Key("process.active")
    String processActive();

    @Key("process.table.name")
    String processTableName();

    @Key("process.table.protocol")
    String processTableProtocol();

    @Key("process.table.port")
    String processTablePort();

    @Key("process.table.time")
    String processTableTime();

    @Key("process.table.active")
    String processTableActive();

    @Key("tab.terminal")
    String tabTerminal();

    @Key("perspective.project.action.tooltip")
    String perspectiveProjectActionTooltip();

    @Key("perspective.machine.action.tooltip")
    String perspectiveMachineActionTooltip();

    @Key("tab.info")
    String tabInfo();

    @Key("info.name")
    String infoName();

    @Key("info.machine.id")
    String infoMachineId();

    @Key("info.owner")
    String infoOwner();

    @Key("info.status")
    String infoStatus();

    @Key("info.type")
    String infoType();

    @Key("info.ws.bound")
    String infoWsBound();

    @Key("info.ws.id")
    String infoWsId();

    @Key("info.server.port")
    String infoServerPort();

    @Key("info.server.address")
    String infoServerAddress();

    @Key("tab.server")
    String tabServer();

    @Key("info.server.ref")
    String infoServerRef();

    @Key("info.server.url")
    String infoServerUrl();

    @Key("unavailable.machine.info")
    String unavailableMachineInfo();

    @Key("control.machine.restart.text")
    String controlMachineRestartText();

    @Key("control.machine.restart.tooltip")
    String controlMachineRestartTooltip();

    @Key("control.machine.restart.text.by.name")
    String machineRestartTextByName(String text);

    @Key("notification.machine.restarting")
    String notificationMachineRestarting(String text);

    @Key("machine.restarted")
    String machineRestarted(String text);

    @Key("view.recipePanel.title")
    String viewRecipePanelTitle();

    @Key("view.recipePanel.tooltip")
    String viewRecipePanelTooltip();

    @Key("editor.button.create")
    String editorButtonCreate();

    @Key("editor.button.save")
    String editorButtonSave();

    @Key("editor.button.delete")
    String editorButtonDelete();

    @Key("editor.button.cancel")
    String editorButtonCancel();
}
