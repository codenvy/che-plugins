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
package org.eclipse.che.ide.ext.runner.client.manager;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.inject.factories.ModelsFactory;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.models.RunnerCounter;
import org.eclipse.che.ide.ext.runner.client.runneractions.RunnerAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.CheckRamAndRunAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.GetRunningProcessesAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.StopAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetSystemEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.LaunchAction;
import org.eclipse.che.ide.ext.runner.client.selection.Selection;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.state.PanelState;
import org.eclipse.che.ide.ext.runner.client.state.State;
import org.eclipse.che.ide.ext.runner.client.tabs.common.Tab;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabBuilder;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.history.HistoryPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.container.TerminalContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;
import org.eclipse.che.ide.ext.runner.client.util.annotations.RightPanel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.ext.runner.client.constants.TimeInterval.ONE_SEC;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_QUEUE;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.RUNNER;
import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;
import static org.eclipse.che.ide.ext.runner.client.tabs.common.Tab.VisibleState.REMOVABLE;
import static org.eclipse.che.ide.ext.runner.client.tabs.common.Tab.VisibleState.VISIBLE;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer.TabSelectHandler;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType.LEFT;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType.RIGHT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;

/**
 * The class provides much business logic:
 * 1. Provides possibility to launch/start a new runner. It means execute request on the server (communication with server part) and change
 * UI part.
 * 2. Manage runners (stop runner, get different information about runner and etc).
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class RunnerManagerPresenter extends BasePresenter implements RunnerManager,
                                                                     RunnerManagerView.ActionDelegate,
                                                                     ProjectActionHandler,
                                                                     SelectionManager.SelectionChangeListener {
    public static final String TIMER_STUB = "--:--:--";

    private final RunnerManagerView           view;
    private final DtoFactory                  dtoFactory;
    private final AppContext                  appContext;
    private final ModelsFactory               modelsFactory;
    private final RunnerActionFactory         actionFactory;
    private final GetSystemEnvironmentsAction getSystemEnvironmentsAction;
    private final Map<Runner, RunnerAction>   runnerActions;
    private final Timer                       runnerTimer;
    private final RunnerLocalizationConstant  locale;
    private final HistoryPanel                history;
    private final SelectionManager            selectionManager;
    private final TerminalContainer           terminalContainer;
    private final TabContainer                rightTabContainer;
    private final ConsoleContainer            consoleContainer;
    private final PropertiesContainer         propertiesContainer;
    private final TemplatesContainer          templateContainer;
    private final PanelState                  panelState;
    private final RunnerCounter               runnerCounter;
    private final Set<Long>                   runnersId;
    private final RunnerUtil                  runnerUtil;

    private GetRunningProcessesAction getRunningProcessAction;

    private Runner      selectedRunner;
    private Environment selectedEnvironment;

    @Inject
    public RunnerManagerPresenter(final RunnerManagerView view,
                                  RunnerActionFactory actionFactory,
                                  ModelsFactory modelsFactory,
                                  AppContext appContext,
                                  DtoFactory dtoFactory,
                                  EventBus eventBus,
                                  RunnerLocalizationConstant locale,
                                  @LeftPanel TabContainer leftTabContainer,
                                  @RightPanel TabContainer rightTabContainer,
                                  PanelState panelState,
                                  Provider<TabBuilder> tabBuilderProvider,
                                  ConsoleContainer consoleContainer,
                                  TerminalContainer terminalContainer,
                                  PropertiesContainer propertiesContainer,
                                  HistoryPanel history,
                                  TemplatesContainer templateContainer,
                                  RunnerCounter runnerCounter,
                                  SelectionManager selectionManager,
                                  TimerFactory timerFactory,
                                  GetSystemEnvironmentsAction getSystemEnvironmentsAction,
                                  RunnerUtil runnerUtil) {
        this.view = view;
        this.view.setDelegate(this);
        this.locale = locale;
        this.dtoFactory = dtoFactory;
        this.actionFactory = actionFactory;
        this.modelsFactory = modelsFactory;
        this.appContext = appContext;
        this.runnerCounter = runnerCounter;
        this.getSystemEnvironmentsAction = getSystemEnvironmentsAction;
        this.runnerUtil = runnerUtil;

        this.selectionManager = selectionManager;
        this.selectionManager.addListener(this);

        this.history = history;

        this.panelState = panelState;

        this.consoleContainer = consoleContainer;
        this.templateContainer = templateContainer;
        this.terminalContainer = terminalContainer;
        this.propertiesContainer = propertiesContainer;

        this.rightTabContainer = rightTabContainer;

        this.runnerActions = new HashMap<>();

        this.runnerTimer = timerFactory.newInstance(new TimerFactory.TimerCallBack() {
            @Override
            public void onRun() {
                updateRunnerTimer();

                runnerTimer.schedule(ONE_SEC.getValue());
            }
        });

        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        runnersId = new HashSet<>();

        initializeLeftPanel(panelState, leftTabContainer, tabBuilderProvider, history, templateContainer);
        initializeRightPanel(rightTabContainer, tabBuilderProvider, consoleContainer, terminalContainer, propertiesContainer);

        view.setLeftPanel(leftTabContainer);
        view.setRightPanel(rightTabContainer);
    }

    private void updateRunnerTimer() {
        if (selectedRunner == null) {
            return;
        }
        view.setTimeout(selectedRunner.getTimeout());

        view.updateMoreInfoPopup(selectedRunner);
    }

    private void initializeLeftPanel(@Nonnull final PanelState panelState,
                                     @Nonnull TabContainer container,
                                     @Nonnull Provider<TabBuilder> tabBuilderProvider,
                                     @Nonnull HistoryPanel historyPanel,
                                     @Nonnull final TemplatesContainer templatesContainer) {
        TabSelectHandler historyHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                selectHistoryTab();
            }
        };

        Tab historyTab = tabBuilderProvider.get()
                                           .presenter(historyPanel)
                                           .selectHandler(historyHandler)
                                           .title(locale.runnerTabHistory())
                                           .visible(REMOVABLE)
                                           .scope(EnumSet.allOf(State.class))
                                           .tabType(LEFT)
                                           .build();

        container.addTab(historyTab);

        TabSelectHandler templatesHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                panelState.setState(State.TEMPLATE);

                templatesContainer.selectEnvironment();
                templatesContainer.changeEnableStateRunButton();

                view.hideOtherButtons();
            }
        };

        Tab templateTab = tabBuilderProvider.get()
                                            .presenter(templatesContainer)
                                            .selectHandler(templatesHandler)
                                            .title(locale.runnerTabTemplates())
                                            .visible(REMOVABLE)
                                            .scope(EnumSet.allOf(State.class))
                                            .tabType(LEFT)
                                            .build();

        container.addTab(templateTab);
    }

    private void initializeRightPanel(@Nonnull TabContainer container,
                                      @Nonnull Provider<TabBuilder> tabBuilderProvider,
                                      @Nonnull ConsoleContainer consoleContainer,
                                      @Nonnull final TerminalContainer terminalContainer,
                                      @Nonnull final PropertiesContainer propertiesContainer) {

        final TabSelectHandler consoleHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                if (selectedRunner != null) {
                    selectedRunner.setActiveTab(locale.runnerTabConsole());
                }
            }
        };

        Tab consoleTab = tabBuilderProvider.get()
                                           .presenter(consoleContainer)
                                           .title(locale.runnerTabConsole())
                                           .visible(REMOVABLE)
                                           .selectHandler(consoleHandler)
                                           .scope(EnumSet.of(RUNNERS))
                                           .tabType(RIGHT)
                                           .build();

        container.addTab(consoleTab);

        TabSelectHandler terminalHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                if (selectedRunner != null) {
                    selectedRunner.setActiveTab(locale.runnerTabTerminal());

                    terminalContainer.update(selectedRunner);
                }
            }
        };

        Tab terminalTab = tabBuilderProvider.get()
                                            .presenter(terminalContainer)
                                            .title(locale.runnerTabTerminal())
                                            .visible(VISIBLE)
                                            .selectHandler(terminalHandler)
                                            .scope(EnumSet.of(RUNNERS))
                                            .tabType(RIGHT)
                                            .build();

        container.addTab(terminalTab);

        TabSelectHandler propertiesHandler = new TabSelectHandler() {
            @Override
            public void onTabSelected() {
                if (RUNNERS.equals(panelState.getState())) {
                    propertiesContainer.show(selectedRunner);

                    if (selectedRunner != null) {
                        selectedRunner.setActiveTab(locale.runnerTabProperties());
                    }
                } else {
                    propertiesContainer.show(selectedEnvironment);
                }
            }
        };

        Tab propertiesTab = tabBuilderProvider.get()
                                              .presenter(propertiesContainer)
                                              .selectHandler(propertiesHandler)
                                              .title(locale.runnerTabProperties())
                                              .visible(REMOVABLE)
                                              .scope(EnumSet.allOf(State.class))
                                              .tabType(RIGHT)
                                              .build();

        container.addTab(propertiesTab);
    }

    /** @return the GWT widget that is controlled by the presenter */
    @Nonnull
    public RunnerManagerView getView() {
        return view;
    }

    /**
     * Updates runner when runner state changed.
     *
     * @param runner
     *         runner which was changed
     */
    public void update(@Nonnull Runner runner) {
        history.update(runner);

        view.update(runner);

        if (runner.equals(selectedRunner)) {
            changeURLDependingOnState(selectedRunner);
        }
    }

    private void changeURLDependingOnState(@Nonnull Runner runner) {
        switch (runner.getStatus()) {
            case IN_PROGRESS:
                view.setApplicationURl(locale.uplAppWaitingForBoot());
                break;
            case IN_QUEUE:
                view.setApplicationURl(locale.uplAppWaitingForBoot());
                break;
            case STOPPED:
                view.setApplicationURl(locale.urlAppRunnerStopped());
                break;
            case FAILED:
                view.setApplicationURl(null);
                break;
            default:
                String url = runner.getApplicationURL();
                view.setApplicationURl(url == null ? locale.urlAppRunning() : url);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onRunButtonClicked() {
        if (State.TEMPLATE.equals(panelState.getState())) {
            RunOptions runOptions = dtoFactory.createDto(RunOptions.class)
                                              .withOptions(selectedEnvironment.getOptions())
                                              .withEnvironmentId(selectedEnvironment.getId())
                                              .withMemorySize(selectedEnvironment.getRam());

            launchRunner(runOptions, selectedEnvironment.getName());
        } else {
            launchRunner();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onRerunButtonClicked() {
        selectedRunner.setStatus(IN_QUEUE);

        RunnerAction runnerAction = runnerActions.get(selectedRunner);
        if (runnerAction == null || runnerAction instanceof LaunchAction) {
            //Create new CheckRamAndRunAction and update selected runner
            launchRunner(selectedRunner);
        } else {
            runnerAction.perform(selectedRunner);

            update(selectedRunner);
            selectedRunner.resetCreationTime();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onStopButtonClicked() {
        terminalContainer.removeTerminalUrl(selectedRunner);

        stopRunner(selectedRunner);

        view.updateMoreInfoPopup(selectedRunner);
    }

    /** {@inheritDoc} */
    @Override
    public void onLogsButtonClicked() {
        Link logUrl = selectedRunner.getLogUrl();
        if (logUrl == null) {
            return;
        }

        view.showLog(logUrl.getHref());
    }

    /** {@inheritDoc} */
    @Override
    public void stopRunner(@Nonnull Runner runner) {
        RunnerAction runnerAction = runnerActions.get(runner);
        if (runnerAction != null) {
            runnerAction.stop();
        }

        StopAction stopAction = actionFactory.createStop();
        stopAction.perform(runner);
    }

    /** {@inheritDoc} */
    @Override
    public void onMoreInfoBtnMouseOver() {
        view.showMoreInfoPopup(selectedRunner);
    }

    /** {@inheritDoc} */
    @Override
    public Runner launchRunner() {
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("Can't launch runner for current project. Current project is absent...");
        }

        RunOptions runOptions = dtoFactory.createDto(RunOptions.class)
                                          .withSkipBuild(Boolean.valueOf(currentProject.getAttributeValue("runner:skipBuild")))
                                          .withMemorySize(MB_512.getValue());

        return launchRunner(modelsFactory.createRunner(runOptions));
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Runner launchRunner(@Nonnull RunOptions runOptions) {
        return launchRunner(modelsFactory.createRunner(runOptions));
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public Runner launchRunner(@Nonnull RunOptions runOptions, @Nonnull String environmentName) {
        return launchRunner(modelsFactory.createRunner(runOptions, environmentName));
    }

    @Nonnull
    private Runner launchRunner(@Nonnull Runner runner) {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            throw new IllegalStateException("Can't launch runner for current project. Current project is absent...");
        }

        selectedEnvironment = null;

        panelState.setState(RUNNERS);
        view.showOtherButtons();

        history.addRunner(runner);

        CheckRamAndRunAction checkRamAndRunAction = actionFactory.createCheckRamAndRun();
        checkRamAndRunAction.perform(runner);

        runnerActions.put(runner, checkRamAndRunAction);

        runner.resetCreationTime();
        runnerTimer.schedule(ONE_SEC.getValue());

        return runner;
    }

    /** {@inheritDoc} */
    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** Sets active runner panel when runner is started */
    public void setActive() {
        PartPresenter activePart = partStack.getActivePart();
        if (!this.equals(activePart)) {
            partStack.setActivePart(this);
        }
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getTitle() {
        return locale.runnerTitle();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTitleToolTip() {
        return locale.tooltipRunnerPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectOpened(@Nonnull ProjectActionEvent projectActionEvent) {
        view.setEnableReRunButton(false);
        view.setEnableStopButton(false);
        view.setEnableLogsButton(false);

        templateContainer.setVisible(true);

        getRunningProcessAction = actionFactory.createGetRunningProcess();

        boolean isRunOperationAvailable = runnerUtil.hasRunPermission();
        view.setEnableRunButton(isRunOperationAvailable);

        if (!isRunOperationAvailable) {
            return;
        }

        getRunningProcessAction.perform();
        getSystemEnvironmentsAction.perform();

        templateContainer.showEnvironments();

        runnerTimer.schedule(ONE_SEC.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectClosed(@Nonnull ProjectActionEvent projectActionEvent) {
        partStack.hidePart(this);

        selectionManager.setRunner(null);

        templateContainer.setVisible(false);

        view.setEnableRunButton(false);
        view.setEnableReRunButton(false);
        view.setEnableStopButton(false);
        view.setEnableLogsButton(false);

        view.setApplicationURl(null);
        view.setTimeout(TIMER_STUB);

        history.clear();
        runnerActions.clear();

        runnerCounter.reset();
        terminalContainer.reset();
        consoleContainer.reset();
        propertiesContainer.reset();

        getRunningProcessAction.stop();
        propertiesContainer.show((Runner)null);
    }

    /**
     * Adds already running runner.
     *
     * @param processDescriptor
     *         The descriptor of new runner
     * @return instance of new runner
     */
    @Nonnull
    public Runner addRunner(@Nonnull ApplicationProcessDescriptor processDescriptor) {
        RunOptions runOptions = dtoFactory.createDto(RunOptions.class);
        Runner runner = modelsFactory.createRunner(runOptions);
        runnersId.add(processDescriptor.getProcessId());

        runner.setProcessDescriptor(processDescriptor);
        runner.setRAM(processDescriptor.getMemorySize());
        runner.setStatus(Runner.Status.DONE);
        runner.resetCreationTime();

        history.addRunner(runner);

        onSelectionChanged(RUNNER);

        runnerTimer.schedule(ONE_SEC.getValue());

        LaunchAction launchAction = actionFactory.createLaunch();
        runnerActions.put(runner, launchAction);

        launchAction.perform(runner);

        selectHistoryTab();

        return runner;
    }

    /**
     * Adds id of new running runner.
     *
     * @param runnerId
     *         process id of runner
     */
    public void addRunnerId(@Nonnull Long runnerId) {
        runnersId.add(runnerId);
    }

    /**
     * Returns <code>true</code> if runner with current ID was already create, <code>false</code> runner does not exist* *
     *
     * @param runnerId
     *         ID of runner
     */
    public boolean isRunnerExist(@Nonnull Long runnerId) {
        return runnersId.contains(runnerId);
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionChanged(@Nonnull Selection selection) {
        if (RUNNER.equals(selection)) {
            runnerSelected();
        } else {
            environmentSelected();
        }
    }

    private void runnerSelected() {
        selectedRunner = selectionManager.getRunner();
        if (selectedRunner == null) {
            return;
        }

        history.selectRunner(selectedRunner);

        rightTabContainer.showTab(selectedRunner.getActiveTab());

        update(selectedRunner);

        updateRunnerTimer();
    }

    private void environmentSelected() {
        selectedEnvironment = selectionManager.getEnvironment();
        if (selectedEnvironment == null) {
            return;
        }

        templateContainer.select(selectedEnvironment);
    }

    private void selectHistoryTab() {
        panelState.setState(RUNNERS);

        view.setEnableRunButton(runnerUtil.hasRunPermission());

        view.showOtherButtons();
    }
}