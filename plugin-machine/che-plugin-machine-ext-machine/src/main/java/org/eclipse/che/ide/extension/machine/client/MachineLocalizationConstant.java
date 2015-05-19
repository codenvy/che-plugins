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
 * Represents the localization constants contained in resource bundle: 'MachineLocalizationConstant.properties'.
 *
 * @author Artem Zatsarynnyy
 */
public interface MachineLocalizationConstant extends Messages {

    /* Buttons */
    @Key("button.execute")
    String executeButton();

    @Key("button.close")
    String closeButton();


    /* Actions */
    @Key("mainMenu.run.name")
    String mainMenuRunName();

    @Key("control.executeArbitraryCommand.text")
    String executeArbitraryCommandControlTitle();

    @Key("control.executeArbitraryCommand.description")
    String executeArbitraryCommandControlDescription();

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

    @Key("control.terminateMachine.text")
    String terminateMachineControlTitle();

    @Key("control.terminateMachine.description")
    String terminateMachineControlDescription();

    @Key("control.clearMachineConsole.text")
    String clearConsoleControlTitle();

    @Key("control.clearMachineConsole.description")
    String clearConsoleControlDescription();


    /* MachineConsoleView */
    @Key("view.machineConsole.title")
    String machineConsoleViewTitle();

    @Key("view.machineConsole.tooltip")
    String machineConsoleViewTooltip();


    /* ExecuteArbitraryCommandView */
    @Key("view.executeCommand.title")
    String executeCommandViewTitle();


    /* EditConfigurationsView */
    @Key("view.editConfigurations.title")
    String editConfigurationsViewTitle();
}
