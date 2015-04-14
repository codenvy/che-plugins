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
package org.eclipse.che.ide.ext.runner.client.runneractions.impl;

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.runner.dto.ResourcesDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.AbstractRunnerAction;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_4096;

/**
 * This action executes a request on the server side for getting resources of project. These resources are used for checking RAM and
 * running a runner with the custom memory size. Action uses {@link RunAction}
 *
 * @author Artem Zatsarynnyy
 * @author Andrey Parfonov
 * @author Roman Nikitenko
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
//Todo CheckRamAndRunAction is too difficult. This class should improve.
public class CheckRamAndRunAction extends AbstractRunnerAction {
    private final RunnerServiceClient                                 service;
    private final AppContext                                          appContext;
    private final DialogFactory                                       dialogFactory;
    private final Provider<AsyncCallbackBuilder<ResourcesDescriptor>> callbackBuilderProvider;
    private final RunnerLocalizationConstant                          constant;
    private final RunnerUtil                                          runnerUtil;
    private final RunAction                                           runAction;
    private final RunnerManagerPresenter                              managerPresenter;
    private final ConsoleContainer                                    consoleContainer;

    private RunnerConfiguration runnerConfiguration;
    private CurrentProject      project;
    private Runner              runner;

    @Inject
    public CheckRamAndRunAction(RunnerServiceClient service,
                                AppContext appContext,
                                DialogFactory dialogFactory,
                                ConsoleContainer consoleContainer,
                                Provider<AsyncCallbackBuilder<ResourcesDescriptor>> callbackBuilderProvider,
                                RunnerLocalizationConstant constant,
                                RunnerUtil runnerUtil,
                                RunnerActionFactory actionFactory,
                                RunnerManagerPresenter managerPresenter) {
        this.service = service;
        this.appContext = appContext;
        this.callbackBuilderProvider = callbackBuilderProvider;
        this.constant = constant;
        this.runnerUtil = runnerUtil;
        this.runAction = actionFactory.createRun();
        this.dialogFactory = dialogFactory;
        this.consoleContainer = consoleContainer;
        this.managerPresenter = managerPresenter;

        addAction(runAction);
    }

    /** {@inheritDoc} */
    @Override
    public void perform(@Nonnull final Runner runner) {
        this.runner = runner;

        project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        AsyncRequestCallback<ResourcesDescriptor> callback = callbackBuilderProvider
                .get()
                .unmarshaller(ResourcesDescriptor.class)
                .success(new SuccessCallback<ResourcesDescriptor>() {
                    @Override
                    public void onSuccess(ResourcesDescriptor resourcesDescriptor) {
                        checkRamAndRunProject(resourcesDescriptor);
                    }
                })
                .failure(new FailureCallback() {
                    @Override
                    public void onFailure(@Nonnull Throwable reason) {
                        runnerUtil.showError(runner, constant.getResourcesFailed(), reason);
                    }
                })
                .build();

        service.getResources(callback);
    }

    private void checkRamAndRunProject(@Nonnull ResourcesDescriptor resourcesDescriptor) {
        int totalMemory = Integer.valueOf(resourcesDescriptor.getTotalMemory());
        int usedMemory = Integer.valueOf(resourcesDescriptor.getUsedMemory());

        int overrideMemory = getOverrideMemory();
        int requiredMemory = runnerConfiguration != null ? runnerConfiguration.getRam() : 0;

        if (!isSufficientMemory(totalMemory, usedMemory, requiredMemory)) {
            runner.setStatus(Runner.Status.FAILED);

            managerPresenter.update(runner);
            return;
        }

        if (overrideMemory > 0) {
            if (!isOverrideMemoryCorrect(totalMemory, usedMemory, overrideMemory)) {
                runner.setStatus(Runner.Status.FAILED);

                managerPresenter.update(runner);
                return;
            }

            if (overrideMemory < requiredMemory) {
                runProjectWithRequiredMemory(requiredMemory, overrideMemory);
                return;
            }

            runner.setRAM(overrideMemory);

            runAction.perform(runner);
            return;
        }

        if (requiredMemory > 0) {
            runner.setRAM(requiredMemory);
        }

        /* Do not provide any value runnerMemorySize if:
        * - overrideMemory = 0
        * - requiredMemory = 0
        * - requiredMemory > workspaceMemory
        * - requiredMemory > availableMemory
        */
        runAction.perform(runner);
    }

    @Nonnegative
    private int getOverrideMemory() {
        ProjectDescriptor projectDescriptor = project.getProjectDescription();

        initializeRunnerConfiguration(projectDescriptor);

        return runner.getRAM();
    }

    private void initializeRunnerConfiguration(@Nonnull ProjectDescriptor projectDescriptor) {
        RunnersDescriptor runners = projectDescriptor.getRunners();

        if (runners == null) {
            return;
        }

        runnerConfiguration = runners.getConfigs().get(runner.getEnvironmentId());

        if (runnerConfiguration == null) {
            runnerConfiguration = runners.getConfigs().get(runners.getDefault());
        }
    }

    private void runProjectWithRequiredMemory(@Nonnegative final int requiredMemory, @Nonnegative int overrideMemory) {
        /*Offer the user to run an application with requiredMemory
        * If the user selects OK, then runnerMemory = requiredMemory
        * Else we should terminate the Runner process*/
        final ConfirmDialog confirmDialog = dialogFactory.createConfirmDialog(
                constant.titlesWarning(),
                constant.messagesOverrideMemory(), new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        runner.setRAM(requiredMemory);

                        runAction.perform(runner);
                    }
                }, null);

        final MessageDialog messageDialog = dialogFactory.createMessageDialog(
                constant.titlesWarning(),
                constant.messagesOverrideLessRequiredMemory(overrideMemory, requiredMemory),
                new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        confirmDialog.show();
                    }
                });

        messageDialog.show();
    }

    private boolean isSufficientMemory(@Nonnegative int totalMemory,
                                       @Nonnegative int usedMemory,
                                       @Nonnegative final int requiredMemory) {
        int availableMemory = totalMemory - usedMemory;
        if (totalMemory < requiredMemory) {
            runnerUtil.showWarning(constant.messagesTotalLessRequiredMemory(totalMemory, requiredMemory));
            return false;
        }

        if (availableMemory < requiredMemory) {
            dialogFactory.createChoiceDialog(constant.messagesAvailableLessOverrideMemoryTitle(), constant.messagesAvailableLessOverrideMemoryContent(),
                    constant.messagesAvailableLessOverrideMemorySettingsLink(), constant.messagesAvailableLessOverrideMemoryBackToConfig(),
                    new ConfirmCallback() {
                        @Override
                        public void accepted() {
                            Window.open("/dashboard/#/organizations", "_blank", null);
                        }
                    }, null);
            return false;
        }

        return true;
    }

    private boolean isOverrideMemoryCorrect(@Nonnegative int totalMemory,
                                            @Nonnegative int usedMemory,
                                            @Nonnegative final int overrideMemory) {
        int availableMemory = totalMemory - usedMemory;

        if (availableMemory < overrideMemory) {
            dialogFactory.createChoiceDialog(constant.messagesAvailableLessOverrideMemoryTitle(), constant.messagesAvailableLessOverrideMemoryContent(),
                    constant.messagesAvailableLessOverrideMemorySettingsLink(), constant.messagesAvailableLessOverrideMemoryBackToConfig(),
                    new ConfirmCallback() {
                        @Override
                        public void accepted() {
                            Window.open("/dashboard/#/organizations", "_blank", null);
                        }
                    }, null);
            return false;
        }

        if (overrideMemory > MB_4096.getValue()) {
            consoleContainer.printInfo(runner, constant.messagesLargeMemoryRequest());
        }

        return true;
    }

}