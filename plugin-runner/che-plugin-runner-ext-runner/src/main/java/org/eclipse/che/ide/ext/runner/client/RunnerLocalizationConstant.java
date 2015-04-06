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
package org.eclipse.che.ide.ext.runner.client;

import com.google.gwt.i18n.client.Messages;
import com.google.inject.Singleton;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Contains all names of graphical elements needed for runner plugin.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
@Singleton
public interface RunnerLocalizationConstant extends Messages {

    @Key("unknown.error.message")
    String unknownErrorMessage();

    String environmentCooking(@Nonnull String projectName);

    String applicationStarting(@Nonnull String projectName);

    String applicationStopped(@Nonnull String projectName);

    String applicationFailed(@Nonnull String projectName);

    String applicationCanceled(@Nonnull String projectName);

    String applicationMaybeStarted(@Nonnull String projectName);

    String applicationStarted(@Nonnull String projectName);

    String startApplicationFailed(@Nonnull String projectName);

    String applicationLogsFailed();

    @Key("runner.label.application.info")
    String runnerLabelApplicationInfo();

    @Key("runner.label.timeout.info")
    String runnerLabelTimeoutInfo();

    @Key("messages.totalLessRequiredMemory")
    String messagesTotalLessRequiredMemory(@Nonnegative int totalRAM, @Nonnegative int requestedRAM);

    @Key("messages.availableLessRequiredMemory")
    String messagesAvailableLessRequiredMemory(@Nonnegative int totalRAM, @Nonnegative int usedRAM, @Nonnegative int requestedRAM);

    @Key("messages.totalLessOverrideMemory")
    String messagesTotalLessOverrideMemory(@Nonnegative int overrideRAM, @Nonnegative int totalRAM);

    @Key("messages.availableLessOverrideMemory")
    String messagesAvailableLessOverrideMemory(@Nonnegative int availableRAM);

    @Key("messages.overrideMemory")
    String messagesOverrideMemory();

    @Key("messages.overrideLessRequiredMemory")
    String messagesOverrideLessRequiredMemory(@Nonnegative int overrideRAM, @Nonnegative int requestedRAM);

    @Key("messages.largeMemoryRequest")
    String messagesLargeMemoryRequest();

    @Key("action.project.running.now")
    String projectRunningNow(@Nonnull String project);

    @Key("titles.warning")
    String titlesWarning();

    @Key("runner.tab.console")
    String runnerTabConsole();

    @Key("runner.tab.terminal")
    String runnerTabTerminal();

    @Key("action.run")
    String actionRun();

    @Key("action.run.description")
    String actionRunDescription();

    @Key("get.resources.failed")
    String getResourcesFailed();

    String fullLogTraceConsoleLink();

    @Key("remove.environment")
    String removeEnvironment();

    @Key("remove.environment.message")
    String removeEnvironmentMessage(@Nonnull String environmentName);

    @Key("custom.runner.get.environment.failed")
    String customRunnerGetEnvironmentFailed();

    @Key("messages.un.multiple.ram.value")
    String ramSizeMustBeMultipleOf(@Nonnegative int multiple);

    @Key("messages.incorrect.value")
    String messagesIncorrectValue();

    @Key("messages.total.ram.less.custom")
    String messagesTotalRamLessCustom(@Nonnegative int totalRam, @Nonnegative int customRam);

    @Key("messages.available.ram.less.custom")
    String messagesAvailableRamLessCustom(@Nonnegative int overrideRam, @Nonnegative int total, @Nonnegative int used);

    String runnerNotReady();

    @Key("runners.panel.title")
    String runnersPanelTitle();

    @Key("tooltip.header")
    String tooltipHeader();

    @Key("tooltip.body.started")
    String tooltipBodyStarted();

    @Key("tooltip.body.finished")
    String tooltipBodyFinished();

    @Key("tooltip.body.timeout")
    String tooltipBodyTimeout();

    @Key("tooltip.body.time.active")
    String tooltipBodyTimeActive();

    @Key("tooltip.body.ram")
    String tooltipBodyRam();

    @Key("runner.tab.history")
    String runnerTabHistory();

    @Key("runner.tab.properties")
    String runnerTabProperties();

    @Key("runner.tab.templates")
    String runnerTabTemplates();

    @Key("url.app.waiting.for.boot")
    String uplAppWaitingForBoot();

    @Key("url.app.runner.stopped")
    String urlAppRunnerStopped();

    @Key("url.app.running")
    String urlAppRunning();

    @Key("tooltip.runner.panel")
    String tooltipRunnerPanel();

    @Key("template.match.project.type")
    String matchProjectType();

    @Key("properties.name")
    String propertiesName();

    @Key("properties.ram")
    String propertiesRam();

    @Key("properties.scope")
    String propertiesScope();

    @Key("properties.type")
    String propertiesType();

    @Key("properties.boot")
    String propertiesBoot();

    @Key("properties.shutdown")
    String propertiesShutdown();

    @Key("properties.dockerfile")
    String propertiesDockerfile();

    @Key("properties.button.save")
    String propertiesButtonSave();

    @Key("properties.button.delete")
    String propertiesButtonDelete();

    @Key("properties.button.cancel")
    String propertiesButtonCancel();

    @Key("runner.title")
    String runnerTitle();

    String editorNotReady();

    @Key("tooltip.run.button")
    String tooltipRunButton();

    @Key("tooltip.stop.button")
    String tooltipStopButton();

    @Key("tooltip.docker.button")
    String tooltipDockerButton();

    @Key("console.tooltip.scroll")
    String consoleTooltipScroll();

    @Key("console.tooltip.clear")
    String consoleTooltipClear();

    @Key("action.choose.runner")
    String actionChooseRunner();

    @Key("console.tooltip.wraptext")
    String consoleTooltipWraptext();

    @Key("properties.button.create")
    String propertiesButtonCreate();

    @Key("tooltip.rerun.button")
    String tooltipRerunButton();

    @Key("tooltip.logs.button")
    String tooltipLogsButton();

    @Key("action.run.with")
    String actionRunWith();

    @Key("action.runner.not.specified")
    String actionRunnerNotSpecified();

    @Key("message.runner.shutting.down")
    String messageRunnerShuttingDown();
}