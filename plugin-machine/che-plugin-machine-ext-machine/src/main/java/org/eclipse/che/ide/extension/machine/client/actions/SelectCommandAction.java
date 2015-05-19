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
package org.eclipse.che.ide.extension.machine.client.actions;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.CommandServiceClient;
import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.CommandTypeRegistry;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditConfigurationsPresenter;
import org.eclipse.che.ide.ui.dropdown.DropDownHeaderWidget;
import org.eclipse.che.ide.ui.dropdown.DropDownListFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.eclipse.che.ide.extension.machine.client.MachineExtension.GROUP_COMMANDS_LIST;

/**
 * Action that allows user to select command from list of all commands.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class SelectCommandAction extends Action implements CustomComponentAction,
                                                           ProjectActionHandler,
                                                           EditConfigurationsPresenter.ConfigurationsChangedListener {

    public static final String GROUP_COMMANDS = "CommandsGroup";

    private final DropDownHeaderWidget dropDownHeaderWidget;
    private final DropDownListFactory  configRunnerFactory;
    private final ActionManager        actionManager;
    private final CommandServiceClient commandServiceClient;
    private final CommandTypeRegistry  commandTypeRegistry;

    private List<CommandConfiguration> commands;
    private DefaultActionGroup         commandActions;

    @Inject
    public SelectCommandAction(MachineLocalizationConstant locale,
                               ActionManager actionManager,
                               EventBus eventBus,
                               DropDownListFactory dropDownListFactory,
                               CommandServiceClient commandServiceClient,
                               CommandTypeRegistry commandTypeRegistry,
                               EditConfigurationsPresenter editConfigurationsPresenter) {
        super(locale.selectCommandControlTitle(), locale.selectCommandControlDescription(), null);
        this.actionManager = actionManager;
        this.commandServiceClient = commandServiceClient;
        this.commandTypeRegistry = commandTypeRegistry;

        this.configRunnerFactory = dropDownListFactory;
        this.dropDownHeaderWidget = dropDownListFactory.createList(GROUP_COMMANDS_LIST);

        commands = new LinkedList<>();

        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        editConfigurationsPresenter.addConfigurationsChangedListener(this);

        commandActions = new DefaultActionGroup(GROUP_COMMANDS, false, actionManager);
        actionManager.registerAction(GROUP_COMMANDS, commandActions);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public Widget createCustomComponent(Presentation presentation) {
        return (Widget)dropDownHeaderWidget;
    }

    /** Returns selected command. */
    @Nullable
    public CommandConfiguration getSelectedCommand() {
        if (commands.isEmpty()) {
            return null;
        }

        final String selectedCommandName = dropDownHeaderWidget.getSelectedElementName();

        for (CommandConfiguration configuration : commands) {
            if (configuration.getName().equals(selectedCommandName)) {
                return configuration;
            }
        }

        return null;
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {
        loadCommands();
    }

    private void loadCommands() {
        commandServiceClient.getCommands().then(new Function<List<CommandDescriptor>, List<CommandConfiguration>>() {
            @Override
            public List<CommandConfiguration> apply(List<CommandDescriptor> arg) throws FunctionException {
                final List<CommandConfiguration> configurationList = new ArrayList<>();

                for (CommandDescriptor descriptor : arg) {
                    final CommandType type = commandTypeRegistry.getCommandTypeById(descriptor.getType());
                    // skip command if it's type isn't registered
                    if (type != null) {
                        configurationList.add(type.getConfigurationFactory().createFromCommandDescriptor(descriptor));
                    }
                }

                return configurationList;
            }
        }).then(new Operation<List<CommandConfiguration>>() {
            @Override
            public void apply(List<CommandConfiguration> commandConfigurations) throws OperationException {
                setCommandConfigurations(commandConfigurations);
            }
        });
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
        setCommandConfigurations(Collections.<CommandConfiguration>emptyList());
    }

    /**
     * Sets command configurations to the list.
     *
     * @param commandConfigurations
     *         collection of command configurations to set
     */
    private void setCommandConfigurations(@Nonnull Collection<CommandConfiguration> commandConfigurations) {
        final DefaultActionGroup commandsList = (DefaultActionGroup)actionManager.getAction(GROUP_COMMANDS_LIST);

        commands.clear();

        clearCommandActions(commandsList);
        commandActions.removeAll();

        for (CommandConfiguration configuration : commandConfigurations) {
            commandActions.add(configRunnerFactory.createElement(configuration.getName(),
                                                                 configuration.getType().getIcon(),
                                                                 dropDownHeaderWidget));
        }

        commandsList.addAll(commandActions);
        commands.addAll(commandConfigurations);

        selectLastUsedCommand();
    }

    private void clearCommandActions(@Nonnull DefaultActionGroup runnersList) {
        for (Action action : commandActions.getChildActionsOrStubs()) {
            runnersList.remove(action);
        }
    }

    /** Selects last used command. */
    private void selectLastUsedCommand() {
        if (commands.isEmpty()) {
            setEmptyCommand();
        } else {
            // TODO: consider to saving last used command ID somewhere
            // for now, we always select first command
            final CommandConfiguration command = commands.get(0);
            dropDownHeaderWidget.selectElement(command.getType().getIcon(), command.getName());
        }
    }

    /** Clears the selected element in the 'Select Command' menu. */
    private void setEmptyCommand() {
        dropDownHeaderWidget.selectElement(null, "");
    }

    @Override
    public void onConfigurationsChanged() {
        loadCommands();
    }
}
