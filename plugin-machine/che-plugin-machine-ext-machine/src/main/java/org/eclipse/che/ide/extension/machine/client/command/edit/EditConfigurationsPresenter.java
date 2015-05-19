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
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.CommandServiceClient;
import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.CommandTypeRegistry;
import org.eclipse.che.ide.extension.machine.client.command.ConfigurationPage;
import org.eclipse.che.ide.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presenter for managing command configurations.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class EditConfigurationsPresenter implements EditConfigurationsView.ActionDelegate {

    private final EditConfigurationsView view;
    private final CommandServiceClient   commandServiceClient;
    private final CommandTypeRegistry    commandTypeRegistry;

    private final Set<ConfigurationsChangedListener> listeners;

    @Inject
    protected EditConfigurationsPresenter(EditConfigurationsView view,
                                          CommandServiceClient commandServiceClient,
                                          CommandTypeRegistry commandTypeRegistry) {
        this.view = view;
        this.commandServiceClient = commandServiceClient;
        this.commandTypeRegistry = commandTypeRegistry;

        listeners = new HashSet<>();

        this.view.setDelegate(this);
    }

    @Override
    public void onCloseClicked() {
        view.close();
    }

    @Override
    public void onAddClicked() {
        final CommandType selectedType = view.getSelectedCommandType();
        if (selectedType == null) {
            return;
        }

        final CommandConfiguration configuration = selectedType.getConfigurationFactory().createFromTemplate("Command-" + UUID.uuid(2));

        final Promise<CommandDescriptor> commandPromise = commandServiceClient.createCommand(configuration.getName(),
                                                                                             configuration.toCommandLine(),
                                                                                             selectedType.getId());
        commandPromise.then(new Operation<CommandDescriptor>() {
            @Override
            public void apply(CommandDescriptor arg) throws OperationException {
                fireConfigurationsChanged();
                refreshView();
            }
        });
    }

    @Override
    public void onDeleteClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            commandServiceClient.removeCommand(selectedConfiguration.getId()).then(new Operation<Void>() {
                @Override
                public void apply(Void arg) throws OperationException {
                    fireConfigurationsChanged();
                    refreshView();
                }
            });
        }
    }

    @Override
    public void onCommandTypeSelected(CommandType type) {
        view.setAddButtonState(true);
        view.setRemoveButtonState(false);
        view.setConfigurationName("");
        view.clearCommandConfigurationsDisplayContainer();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void onConfigurationSelected(CommandConfiguration configuration) {
        view.setAddButtonState(true);
        view.setRemoveButtonState(true);
        view.setConfigurationName(configuration.getName());

        final Collection<ConfigurationPage<? extends CommandConfiguration>> pages = configuration.getType().getConfigurationPages();
        for (ConfigurationPage<? extends CommandConfiguration> page : pages) {
            ((ConfigurationPage<CommandConfiguration>)page).resetFrom(configuration);
            page.go(view.getCommandConfigurationsDisplayContainer());
            // TODO: for now show only first page but need to show all pages
            break;
        }
    }

    @Override
    public void onNameChanged(String name) {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            selectedConfiguration.setName(name);
        }
    }

    @Override
    public void onSaveClicked() {
        final CommandConfiguration selectedConfiguration = view.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            commandServiceClient.updateCommand(selectedConfiguration.getId(),
                                               selectedConfiguration.getName(),
                                               selectedConfiguration.toCommandLine()).then(new Operation<CommandDescriptor>() {
                @Override
                public void apply(CommandDescriptor arg) throws OperationException {
                    fireConfigurationsChanged();
                    refreshView();
                }
            });
        }
    }

    /** Show dialog. */
    public void show() {
        refreshView();
        view.show();
    }

    private void refreshView() {
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
                view.setData(commandTypeRegistry.getCommandTypes(), commandConfigurations);
            }
        });
    }

    private void fireConfigurationsChanged() {
        for (ConfigurationsChangedListener listener : listeners) {
            listener.onConfigurationsChanged();
        }
    }

    public void addConfigurationsChangedListener(ConfigurationsChangedListener listener) {
        listeners.add(listener);
    }

    public void removeConfigurationsChangedListener(ConfigurationsChangedListener listener) {
        listeners.remove(listener);
    }

    public interface ConfigurationsChangedListener {
        void onConfigurationsChanged();
    }
}
