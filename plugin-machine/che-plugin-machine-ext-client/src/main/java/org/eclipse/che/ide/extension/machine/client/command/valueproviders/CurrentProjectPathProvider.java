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
package org.eclipse.che.ide.extension.machine.client.command.valueproviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateEvent;
import org.eclipse.che.ide.extension.machine.client.machine.events.MachineStateHandler;

import javax.validation.constraints.NotNull;

/**
 * Provides current project's path.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class CurrentProjectPathProvider implements CommandPropertyValueProvider, MachineStateHandler, ProjectActionHandler {

    private static final String KEY = "${project.current.path}";

    private final AppContext           appContext;
    private final MachineServiceClient machineServiceClient;

    private String value;

    @Inject
    public CurrentProjectPathProvider(EventBus eventBus, AppContext appContext, MachineServiceClient machineServiceClient) {
        this.appContext = appContext;
        this.machineServiceClient = machineServiceClient;
        this.value = "";

        eventBus.addHandler(MachineStateEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        updateValue();
    }

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void onMachineRunning(MachineStateEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        final Machine machine = event.getMachine();
        if (currentProject == null || !machine.isDev()) {
            return;
        }

        final String projectsRoot = machine.getProjectsRoot();
        value = projectsRoot + currentProject.getProjectDescription().getPath();
    }

    @Override
    public void onMachineDestroyed(MachineStateEvent event) {
        if (event.getMachine().isDev()) {
            value = "";
        }
    }

    @Override
    public void onProjectCreated(ProjectActionEvent event) {
        updateValue();
    }

    @Override
    public void onProjectDeleted(ProjectActionEvent event) {
        value = "";
    }

    private void updateValue() {
        final String devMachineId = appContext.getDevMachineId();
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (devMachineId == null || currentProject == null) {
            return;
        }

        machineServiceClient.getMachine(devMachineId).then(new Operation<MachineDescriptor>() {
            @Override
            public void apply(MachineDescriptor arg) throws OperationException {
                final String projectsRoot = arg.getMetadata().projectsRoot();
                value = projectsRoot + currentProject.getProjectDescription().getPath();
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                value = "";
            }
        });
    }
}
