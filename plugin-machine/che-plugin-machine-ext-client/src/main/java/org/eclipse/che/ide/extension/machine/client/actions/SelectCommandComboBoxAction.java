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
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.CommandTypeRegistry;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditCommandsPresenter;
import org.eclipse.che.ide.ui.dropdown.DropDownHeaderWidget;
import org.eclipse.che.ide.ui.dropdown.DropDownListFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.eclipse.che.ide.extension.machine.client.MachineExtension.GROUP_COMMANDS_LIST;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action that allows user to select command from list of all commands.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class SelectCommandComboBoxAction extends AbstractPerspectiveAction implements CustomComponentAction,
                                                                                      ProjectActionHandler,
                                                                                      EditCommandsPresenter.ConfigurationChangedListener {

    public static final  String                           GROUP_COMMANDS     = "CommandsGroup";
    private static final Comparator<CommandConfiguration> commandsComparator = new CommandsComparator();

    private final DropDownHeaderWidget dropDownHeaderWidget;
    private final DropDownListFactory  dropDownListFactory;
    private final AppContext           appContext;
    private final ActionManager        actionManager;
    private final CommandServiceClient commandServiceClient;
    private final CommandTypeRegistry  commandTypeRegistry;

    private List<CommandConfiguration> commands;
    private DefaultActionGroup         commandActions;

    @Inject
    public SelectCommandComboBoxAction(MachineLocalizationConstant locale,
                                       ActionManager actionManager,
                                       EventBus eventBus,
                                       DropDownListFactory dropDownListFactory,
                                       CommandServiceClient commandServiceClient,
                                       CommandTypeRegistry commandTypeRegistry,
                                       EditCommandsPresenter editCommandsPresenter,
                                       AppContext appContext) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID),
              locale.selectCommandControlTitle(),
              locale.selectCommandControlDescription(),
              null, null);
        this.actionManager = actionManager;
        this.commandServiceClient = commandServiceClient;
        this.commandTypeRegistry = commandTypeRegistry;

        this.dropDownListFactory = dropDownListFactory;
        this.appContext = appContext;
        this.dropDownHeaderWidget = dropDownListFactory.createList(GROUP_COMMANDS_LIST);

        commands = new LinkedList<>();

        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        editCommandsPresenter.addConfigurationsChangedListener(this);

        commandActions = new DefaultActionGroup(GROUP_COMMANDS, false, actionManager);
        actionManager.registerAction(GROUP_COMMANDS, commandActions);
    }

    @Override
    public void updateInPerspective(@Nonnull ActionEvent event) {
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

    public void setSelectedCommand(CommandConfiguration command) {
        if (appContext.getCurrentProject() != null) {
            dropDownHeaderWidget.selectElement(command.getType().getIcon(), command.getName());
        }
    }

    @Override
    public void onProjectCreated(ProjectActionEvent event) {
        loadCommands(null);
    }

    @Override
    public void onProjectDeleted(ProjectActionEvent event) {
        setCommandConfigurations(Collections.<CommandConfiguration>emptyList(), null);
    }

    /**
     * Load all saved commands.
     *
     * @param commandToSelect
     *         command that should be selected after loading all commands
     */
    private void loadCommands(@Nullable final CommandConfiguration commandToSelect) {
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
                setCommandConfigurations(commandConfigurations, commandToSelect);
            }
        });
    }

    /**
     * Sets command configurations to the list.
     *
     * @param commandConfigurations
     *         collection of command configurations to set
     * @param commandToSelect
     *         command that should be selected or {@code null} if none
     */
    private void setCommandConfigurations(@Nonnull List<CommandConfiguration> commandConfigurations,
                                          @Nullable CommandConfiguration commandToSelect) {
        final DefaultActionGroup commandsList = (DefaultActionGroup)actionManager.getAction(GROUP_COMMANDS_LIST);

        commands.clear();

        clearCommandActions(commandsList);
        commandActions.removeAll();

        Collections.sort(commandConfigurations, commandsComparator);
        CommandConfiguration prevCommand = null;
        for (CommandConfiguration configuration : commandConfigurations) {
            if (prevCommand != null && !configuration.getType().getId().equals(prevCommand.getType().getId())) {
                commandActions.addSeparator();
            }

            commandActions.add(dropDownListFactory.createElement(configuration.getName(),
                                                                 configuration.getType().getIcon(),
                                                                 dropDownHeaderWidget));
            prevCommand = configuration;
        }

        commandsList.addAll(commandActions);
        commands.addAll(commandConfigurations);

        if (commandToSelect != null) {
            setSelectedCommand(commandToSelect);
        } else {
            selectLastUsedCommand();
        }
    }

    private void clearCommandActions(@Nonnull DefaultActionGroup commandsList) {
        for (Action action : commandActions.getChildActionsOrStubs()) {
            commandsList.remove(action);
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
    public void onConfigurationAdded(CommandConfiguration command) {
        if (appContext.getCurrentProject() != null) {
            loadCommands(command);
        }
    }

    @Override
    public void onConfigurationRemoved(CommandConfiguration command) {
        if (appContext.getCurrentProject() != null) {
            loadCommands(null);
        }
    }

    @Override
    public void onConfigurationsUpdated(CommandConfiguration command) {
        if (appContext.getCurrentProject() != null) {
            loadCommands(command);
        }
    }

    private static class CommandsComparator implements Comparator<CommandConfiguration> {
        @Override
        public int compare(CommandConfiguration o1, CommandConfiguration o2) {
            return o1.getType().getId().compareTo(o2.getType().getId());
        }
    }
}
