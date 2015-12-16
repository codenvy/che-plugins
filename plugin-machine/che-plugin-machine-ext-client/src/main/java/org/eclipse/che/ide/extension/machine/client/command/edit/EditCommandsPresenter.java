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
package org.eclipse.che.ide.extension.machine.client.command.edit;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.actions.SelectCommandComboBoxReady;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfigurationPage;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfigurationPage.DirtyStateListener;
import org.eclipse.che.ide.extension.machine.client.command.CommandManager;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.CommandTypeRegistry;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.choice.ChoiceDialog;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Presenter for managing commands.
 *
 * @author Artem Zatsarynnyi
 * @author Oleksii Orel
 */
@Singleton
public class EditCommandsPresenter implements EditCommandsView.ActionDelegate {

    private final EditCommandsView                     view;
    private final WorkspaceServiceClient               workspaceServiceClient;
    private final CommandManager                       commandManager;
    private final String                               workspaceId;
    private final DtoFactory                           dtoFactory;
    private final CommandTypeRegistry                  commandTypeRegistry;
    private final DialogFactory                        dialogFactory;
    private final MachineLocalizationConstant          machineLocale;
    private final CoreLocalizationConstant             coreLocale;
    private final Provider<SelectCommandComboBoxReady> selectCommandActionProvider;

    private final Set<ConfigurationChangedListener> configurationChangedListeners;

    private       CommandConfigurationPage<CommandConfiguration> editedPage;
    /** Command that being edited. */
    private       CommandConfiguration                           editedCommand;
    /** Name of the edited command before editing. */
    private       String                                         editedCommandOriginName;
    /** Map of the existing command names(command type id as key). */
    private final Map<String, HashSet<String>>                   commandNames;

    @Inject
    protected EditCommandsPresenter(EditCommandsView view,
                                    WorkspaceServiceClient workspaceServiceClient,
                                    CommandTypeRegistry commandTypeRegistry,
                                    DialogFactory dialogFactory,
                                    MachineLocalizationConstant machineLocale,
                                    CoreLocalizationConstant coreLocale,
                                    Provider<SelectCommandComboBoxReady> selectCommandActionProvider,
                                    CommandManager commandManager,
                                    @Named("workspaceId") String workspaceId,
                                    DtoFactory dtoFactory) {
        this.view = view;
        this.workspaceServiceClient = workspaceServiceClient;
        this.commandManager = commandManager;
        this.workspaceId = workspaceId;
        this.dtoFactory = dtoFactory;
        this.commandTypeRegistry = commandTypeRegistry;
        this.dialogFactory = dialogFactory;
        this.machineLocale = machineLocale;
        this.coreLocale = coreLocale;
        this.selectCommandActionProvider = selectCommandActionProvider;
        this.view.setDelegate(this);

        configurationChangedListeners = new HashSet<>();
        commandNames = new HashMap<>();
    }

    @Override
    public void onCloseClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        onNameChanged();
        if (!isViewModified() || selectedConfiguration == null) {
            if (selectedConfiguration != null) {
                selectCommandOnToolbar(selectedConfiguration);
            }
            view.close();
            return;
        }

        updateCommand(selectedConfiguration).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                view.close();
                fireConfigurationUpdated(selectedConfiguration);
            }
        });
    }

    private void selectCommandOnToolbar(CommandConfiguration commandToSelect) {
        selectCommandActionProvider.get().setSelectedCommand(commandToSelect);
    }

    @Override
    public void onSaveClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        onNameChanged();
        if (selectedConfiguration == null) {
            return;
        }

        updateCommand(selectedConfiguration).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                fetchCommands();
                fireConfigurationUpdated(selectedConfiguration);
            }
        });
    }

    private Promise<UsersWorkspaceDto> updateCommand(CommandConfiguration selectedConfiguration) {
        final CommandDto commandDto = dtoFactory.createDto(CommandDto.class)
                                                .withName(selectedConfiguration.getName())
                                                .withCommandLine(selectedConfiguration.toCommandLine())
                                                .withType(selectedConfiguration.getType().getId());

        if (editedCommandOriginName.equals(selectedConfiguration.getName())) {
            return workspaceServiceClient.updateCommand(workspaceId, commandDto);
        } else {
            return workspaceServiceClient.deleteCommand(workspaceId, editedCommandOriginName)
                                         .thenPromise(new Function<UsersWorkspaceDto, Promise<UsersWorkspaceDto>>() {
                                             @Override
                                             public Promise<UsersWorkspaceDto> apply(UsersWorkspaceDto arg) throws FunctionException {
                                                 return workspaceServiceClient.addCommand(workspaceId, commandDto);
                                             }
                                         });
        }
    }

    @Override
    public void onCancelClicked() {
        view.close();
    }

    @Override
    public void onDuplicateClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        createCommand(selectedConfiguration.getType(), selectedConfiguration.toCommandLine(), selectedConfiguration.getName());
    }

    @Override
    public void onAddClicked() {
        final CommandType selectedType = view.getSelectedCommandType();
        if (selectedType == null) {
            return;
        }

        if (!isViewModified()) {
            createCommand(selectedType);
            return;
        }

        final ConfirmCallback saveCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                updateCommand(editedCommand).then(new Operation<UsersWorkspaceDto>() {
                    @Override
                    public void apply(UsersWorkspaceDto arg) throws OperationException {
                        createCommand(selectedType);
                    }
                });
            }
        };

        final ConfirmCallback discardCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                createCommand(selectedType);
            }
        };

        final ChoiceDialog dialog = dialogFactory.createChoiceDialog(
                machineLocale.editCommandsSaveChangesTitle(),
                machineLocale.editCommandsSaveChangesConfirmation(editedCommand.getName()),
                coreLocale.save(),
                machineLocale.editCommandsSaveChangesDiscard(),
                saveCallback,
                discardCallback);
        dialog.show();
    }

    private String getUniqueCommandName(CommandType customType, String customName) {
        final String newCommandName;
        final HashSet<String> typeNames = commandNames.get(customType.getId());

        if (customName != null) {
            if (!typeNames.contains(customName)) {
                return customName;
            }
            newCommandName = customName + " copy";
            if (!typeNames.contains(newCommandName)) {
                return newCommandName;
            }
        } else {
            newCommandName = "new" + customType.getDisplayName();
        }

        int count = 0;
        while (typeNames.contains(newCommandName + "-" + ++count)) {
            if (count > 10000) {
                break;
            }
        }
        return newCommandName + "-" + count;
    }

    private void createCommand(CommandType type) {
        createCommand(type, null, null);
    }

    private void createCommand(CommandType type, String customCommand, String customName) {
        final CommandDto commandDto = dtoFactory.createDto(CommandDto.class)
                                                .withName(getUniqueCommandName(type, customName))
                                                .withCommandLine(customCommand != null ? customCommand : type.getCommandTemplate())
                                                .withType(type.getId());
        workspaceServiceClient.addCommand(workspaceId, commandDto).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                fetchCommands();

                final CommandType type = commandTypeRegistry.getCommandTypeById(commandDto.getType());
                final CommandConfiguration command = type.getConfigurationFactory().createFromDto(commandDto);
                fireConfigurationAdded(command);
                view.selectCommand(command);
            }
        });
    }

    @Override
    public void onRemoveClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        final ConfirmCallback confirmCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                workspaceServiceClient.deleteCommand(workspaceId, selectedConfiguration.getName()).then(new Operation<UsersWorkspaceDto>() {
                    @Override
                    public void apply(UsersWorkspaceDto arg) throws OperationException {
                        fetchCommands();
                        fireConfigurationRemoved(selectedConfiguration);
                    }
                });
            }
        };

        final ConfirmDialog confirmDialog = dialogFactory.createConfirmDialog(
                machineLocale.editCommandsViewRemoveTitle(),
                machineLocale.editCommandsRemoveConfirmation(selectedConfiguration.getName()),
                confirmCallback,
                null);
        confirmDialog.show();
    }

    @Override
    public void onExecuteClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration == null) {
            return;
        }

        if (isViewModified()) {
            dialogFactory.createMessageDialog("", machineLocale.editCommandsExecuteMessage(), null).show();
            return;
        }

        commandManager.execute(selectedConfiguration);
        view.close();
    }

    @Override
    public void onCommandTypeSelected(CommandType type) {
        if (!isViewModified()) {
            reset();
            return;
        }

        final ConfirmCallback saveCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                updateCommand(editedCommand).then(new Operation<UsersWorkspaceDto>() {
                    @Override
                    public void apply(UsersWorkspaceDto arg) throws OperationException {
                        fetchCommands();
                        fireConfigurationUpdated(editedCommand);
                    }
                });
            }
        };

        final ConfirmCallback discardCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                fetchCommands();
            }
        };

        final ChoiceDialog dialog = dialogFactory.createChoiceDialog(
                machineLocale.editCommandsSaveChangesTitle(),
                machineLocale.editCommandsSaveChangesConfirmation(editedCommand.getName()),
                coreLocale.save(),
                machineLocale.editCommandsSaveChangesDiscard(),
                saveCallback,
                discardCallback);
        dialog.show();
    }

    private void reset() {
        editedCommand = null;
        editedCommandOriginName = null;
        editedPage = null;

        view.setConfigurationName("");
        view.clearCommandConfigurationsDisplayContainer();
    }

    @Override
    public void onConfigurationSelected(final CommandConfiguration configuration) {
        if (!isViewModified()) {
            handleCommandSelection(configuration);
            return;
        }

        final ConfirmCallback saveCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                updateCommand(editedCommand).then(new Operation<UsersWorkspaceDto>() {
                    @Override
                    public void apply(UsersWorkspaceDto arg) throws OperationException {
                        fetchCommands();
                        fireConfigurationUpdated(editedCommand);
                        handleCommandSelection(configuration);
                    }
                });
            }
        };

        final ConfirmCallback discardCallback = new ConfirmCallback() {
            @Override
            public void accepted() {
                fetchCommands();
                handleCommandSelection(configuration);
            }
        };

        final ChoiceDialog dialog = dialogFactory.createChoiceDialog(
                machineLocale.editCommandsSaveChangesTitle(),
                machineLocale.editCommandsSaveChangesConfirmation(editedCommand.getName()),
                coreLocale.save(),
                machineLocale.editCommandsSaveChangesDiscard(),
                saveCallback,
                discardCallback);
        dialog.show();
    }

    private void handleCommandSelection(CommandConfiguration configuration) {
        editedCommand = configuration;
        editedCommandOriginName = configuration.getName();

        view.setConfigurationName(configuration.getName());

        final Collection<CommandConfigurationPage<? extends CommandConfiguration>> pages = configuration.getType().getConfigurationPages();
        for (CommandConfigurationPage<? extends CommandConfiguration> page : pages) {
            final CommandConfigurationPage<CommandConfiguration> p = ((CommandConfigurationPage<CommandConfiguration>)page);

            editedPage = p;

            p.setDirtyStateListener(new DirtyStateListener() {
                @Override
                public void onDirtyStateChanged() {
                    view.setCancelButtonState(isViewModified());
                    view.setApplyButtonState(isViewModified());
                }
            });
            p.resetFrom(configuration);
            p.go(view.getCommandConfigurationsDisplayContainer());

            // TODO: for now only the 1'st page is showing but need to show all the pages
            break;
        }
    }

    @Override
    public void onNameChanged() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            selectedConfiguration.setName(getUniqueCommandName(selectedConfiguration.getType(), view.getConfigurationName()));
            view.setCancelButtonState(isViewModified());
            view.setApplyButtonState(isViewModified());
        }
    }

    /** Show dialog. */
    public void show() {
        fetchCommands();
        view.show();
    }

    /**
     * Fetch commands from server and update view.
     */
    private void fetchCommands() {
        reset();

        view.setCancelButtonState(false);
        view.setApplyButtonState(false);

        workspaceServiceClient.getCommands(workspaceId).then(new Function<List<CommandDto>, List<CommandConfiguration>>() {
            @Override
            public List<CommandConfiguration> apply(List<CommandDto> arg) throws FunctionException {
                final List<CommandConfiguration> configurationList = new ArrayList<>();

                for (CommandDto descriptor : arg) {
                    final CommandType type = commandTypeRegistry.getCommandTypeById(descriptor.getType());
                    // skip command if it's type isn't registered
                    if (type != null) {
                        configurationList.add(type.getConfigurationFactory().createFromDto(descriptor));
                    }
                }

                return configurationList;
            }
        }).then(new Operation<List<CommandConfiguration>>() {
            @Override
            public void apply(List<CommandConfiguration> commandConfigurations) throws OperationException {
                commandNames.clear();

                final Map<CommandType, List<CommandConfiguration>> categories = new HashMap<>();

                for (CommandType type : commandTypeRegistry.getCommandTypes()) {
                    final List<CommandConfiguration> settingsCategory = new ArrayList<>();
                    final HashSet<String> names = new HashSet<>();
                    for (CommandConfiguration configuration : commandConfigurations) {
                        if (type.getId().equals(configuration.getType().getId())) {
                            settingsCategory.add(configuration);
                            names.add(configuration.getName());
                        }
                    }
                    Collections.sort(settingsCategory, new Comparator<CommandConfiguration>() {
                        @Override
                        public int compare(CommandConfiguration o1, CommandConfiguration o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                    categories.put(type, settingsCategory);

                    commandNames.put(type.getId(), names);
                }
                view.setData(categories);
                view.setFilterState(!commandConfigurations.isEmpty());
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                dialogFactory.createMessageDialog("Error", arg.toString(), null).show();
            }
        });
    }

    @Override
    public boolean isViewModified() {
        if (editedCommand == null || editedPage == null) {
            return false;
        }
        return editedPage.isDirty() || !editedCommandOriginName.equals(view.getConfigurationName());
    }

    private void fireConfigurationAdded(CommandConfiguration command) {
        for (ConfigurationChangedListener listener : configurationChangedListeners) {
            listener.onConfigurationAdded(command);
        }
    }

    private void fireConfigurationRemoved(CommandConfiguration command) {
        for (ConfigurationChangedListener listener : configurationChangedListeners) {
            listener.onConfigurationRemoved(command);
        }
    }

    private void fireConfigurationUpdated(CommandConfiguration command) {
        for (ConfigurationChangedListener listener : configurationChangedListeners) {
            listener.onConfigurationsUpdated(command);
        }
    }

    public void addConfigurationsChangedListener(ConfigurationChangedListener listener) {
        configurationChangedListeners.add(listener);
    }

    public void removeConfigurationsChangedListener(ConfigurationChangedListener listener) {
        configurationChangedListeners.remove(listener);
    }

    /** Listener that will be called when command configuration changed. */
    public interface ConfigurationChangedListener {
        void onConfigurationAdded(CommandConfiguration command);

        void onConfigurationRemoved(CommandConfiguration command);

        void onConfigurationsUpdated(CommandConfiguration command);
    }
}
