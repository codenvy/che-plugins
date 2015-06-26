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
package org.eclipse.che.ide.ext.runner.client.models;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.api.runner.gwt.client.utils.RunnerUtils;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.util.StringUtils;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.EnumSet;
import java.util.Objects;

import static org.eclipse.che.api.runner.ApplicationStatus.NEW;
import static org.eclipse.che.api.runner.internal.Constants.LINK_REL_RUNNER_RECIPE;
import static org.eclipse.che.api.runner.internal.Constants.LINK_REL_SHELL_URL;
import static org.eclipse.che.api.runner.internal.Constants.LINK_REL_STOP;
import static org.eclipse.che.api.runner.internal.Constants.LINK_REL_VIEW_LOG;
import static org.eclipse.che.api.runner.internal.Constants.LINK_REL_WEB_URL;
import static org.eclipse.che.ide.ext.runner.client.constants.TimeInterval.ONE_SEC;
import static org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter.TIMER_STUB;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.DONE;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.FAILED;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_QUEUE;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.RUNNING;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.STOPPED;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;

/**
 * The class contains methods which allows change settings of runner. By default runner has scope System,
 * because it has read only type.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
public class RunnerImpl implements Runner {

    public static final  DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getFormat("MM-dd-yyyy HH:mm:ss");
    private static final String         RUNNER_NAME      = "Runner ";
    private static final NumberFormat   NUMBER_FORMAT    = NumberFormat.getDecimalFormat();

    private final RunOptions runOptions;

    private String                       title;
    private ApplicationProcessDescriptor descriptor;
    private Status                       status;
    private String                       activeTab;
    private long                         creationTime;
    private int                          ram;
    private Scope                        scope;
    private String                       type;

    /**
     * This runner needs runner options (user configurations). It analyzes all given information and get necessary information.
     *
     * @param locale
     *         localization constants
     * @param runnerCounter
     *         utility that support the counter of runners
     * @param runOptions
     *         options which needs to be used
     */
    @AssistedInject
    public RunnerImpl(@Nonnull RunnerLocalizationConstant locale,
                      @Nonnull RunnerCounter runnerCounter,
                      @Nonnull GetEnvironmentsUtil util,
                      @Nonnull @Assisted RunOptions runOptions) {
        this(locale, runnerCounter, util, runOptions, SYSTEM, null);
    }

    /**
     * This runner needs runner options (user configurations) and environment name (inputted by user).
     * It analyzes all given information and get necessary information.
     *
     * @param locale
     *         localization constants
     * @param runnerCounter
     *         utility that support the counter of runners
     * @param runOptions
     *         options which needs to be used
     * @param environmentName
     *         name of custom configuration
     */
    @AssistedInject
    public RunnerImpl(@Nonnull RunnerLocalizationConstant locale,
                      @Nonnull RunnerCounter runnerCounter,
                      @Nonnull GetEnvironmentsUtil util,
                      @Nonnull @Assisted RunOptions runOptions,
                      @Nonnull @Assisted Scope environmentScope,
                      @Nullable @Assisted String environmentName) {
        this.runOptions = runOptions;
        this.ram = runOptions.getMemorySize();
        this.title = RUNNER_NAME +
                     runnerCounter.getRunnerNumber() +
                     (environmentName == null ? "" : " - " + getCorrectName(environmentName));
        this.activeTab = locale.runnerTabConsole();
        this.status = IN_QUEUE;
        this.scope = environmentScope;

        creationTime = System.currentTimeMillis();

        String environmentId = runOptions.getEnvironmentId();

        if (environmentId == null || environmentId.startsWith("project:/")) {
            this.type = util.getType();
        } else {
            this.type = util.getCorrectCategoryName(runOptions.getEnvironmentId());
        }

        // the environment ID in runOptions should be an URL
        if (environmentId != null) {
            runOptions.setEnvironmentId(environmentId);
        }

    }

    @Nonnull
    private String getCorrectName(@Nonnull String environmentName) {
        int lastIndex = environmentName.lastIndexOf("/") + 1;

        return environmentName.substring(lastIndex, environmentName.length());
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public ApplicationProcessDescriptor getDescriptor() {
        return descriptor;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getActiveTab() {
        return activeTab;
    }

    /** {@inheritDoc} */
    @Override
    public void setActiveTab(@Nonnull String tab) {
        activeTab = tab;
    }

    /** {@inheritDoc} */
    @Override
    public int getRAM() {
        return ram;
    }

    /** {@inheritDoc} */
    @Override
    public void setRAM(@Nonnegative int ram) {
        this.ram = ram;
    }

    /** {@inheritDoc} */
    @Override
    public String getCreationTime() {
        return DATE_TIME_FORMAT.format(new Date(creationTime));
    }

    /** {@inheritDoc} */
    @Override
    public void resetCreationTime() {
        if (FAILED.equals(status) || STOPPED.equals(status) || IN_QUEUE.equals(status)) {
            creationTime = System.currentTimeMillis();
            return;
        }

        creationTime = descriptor == null ? System.currentTimeMillis() : descriptor.getCreationTime();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public String getTimeout() {
        if (!(DONE.equals(status) || RUNNING.equals(status))) {
            return TIMER_STUB;
        }

        RunnerMetric timeoutMetric = getRunnerMetricByName(RunnerMetric.TERMINATION_TIME);

        if (timeoutMetric != null) {
            return getTimeOut(timeoutMetric);
        }

        RunnerMetric lifeTimeMetric = getRunnerMetricByName(RunnerMetric.LIFETIME);

        if (lifeTimeMetric != null && NEW.equals(descriptor.getStatus())) {
            return getLifeTime(lifeTimeMetric);
        }

        return TIMER_STUB;
    }

    @Nonnull
    private String getTimeOut(@Nonnull RunnerMetric timeoutMetric) {
        String timeout = timeoutMetric.getValue();

        if (RunnerMetric.ALWAYS_ON.equals(timeout)) {
            return timeout;
        }

        if (timeout == null) {
            return TIMER_STUB;
        }

        double terminationTime = NUMBER_FORMAT.parse(timeout);
        double terminationTimeout = terminationTime - System.currentTimeMillis();

        if (terminationTimeout <= 0) {
            return TIMER_STUB;
        }

        return StringUtils.timeMlsToHumanReadable((long)terminationTimeout);
    }

    @Nonnull
    private String getLifeTime(@Nonnull RunnerMetric lifeTimeMetric) {
        String lifeTimeValue = lifeTimeMetric.getValue();

        if (RunnerMetric.ALWAYS_ON.equals(lifeTimeValue)) {
            return lifeTimeValue;
        }

        if (lifeTimeValue == null) {
            return TIMER_STUB;
        }
        double lifeTime = NUMBER_FORMAT.parse(lifeTimeValue);

        return StringUtils.timeMlsToHumanReadable((long)lifeTime);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getActiveTime() {
        return isAlive() ? StringUtils.timeSecToHumanReadable((System.currentTimeMillis() - creationTime) / ONE_SEC.getValue())
                         : TIMER_STUB;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getStopTime() {
        if (isAlive()) {
            return TIMER_STUB;
        }

        RunnerMetric stopTimeMetric = getRunnerMetricByName(RunnerMetric.STOP_TIME);

        if (stopTimeMetric == null) {
            return TIMER_STUB;
        }

        String stopTime = stopTimeMetric.getValue();

        if (stopTime == null) {
            return TIMER_STUB;
        }
        double stopTimeMls = NUMBER_FORMAT.parse(stopTime);

        return DATE_TIME_FORMAT.format(new Date((long)stopTimeMls));
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getEnvironmentId() {
        return runOptions.getEnvironmentId();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getTitle() {
        return title;
    }

    /** {@inheritDoc} */
    @Override
    public void setTitle(@Nonnull String title) {
        this.title = title;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Status getStatus() {
        return status;
    }

    /** {@inheritDoc} */
    @Override
    public void setStatus(@Nonnull Status status) {
        this.status = status;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getApplicationURL() {
        if (descriptor == null) {
            return null;
        }

        String appUrl = getUrlByName(LINK_REL_WEB_URL);
        if (appUrl == null) {
            return null;
        }

        return appUrl + getCodeServerParam();
    }

    @Nonnull
    private String getCodeServerParam() {
        String codeServerHref = getUrlByName("code server");
        if (codeServerHref == null) {
            return "";
        }

        int colon = codeServerHref.lastIndexOf(':');

        String hostParam = "?h=" + codeServerHref.substring(0, colon);
        String portParam = "";

        if (colon > 0) {
            portParam = "&p=" + codeServerHref.substring(colon + 1);
        }

        return hostParam + portParam;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTerminalURL() {
        return getUrlByName(LINK_REL_SHELL_URL);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public Link getLogUrl() {
        return RunnerUtils.getLink(descriptor, LINK_REL_VIEW_LOG);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getDockerUrl() {
        return getUrlByName(LINK_REL_RUNNER_RECIPE);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public Link getStopUrl() {
        return RunnerUtils.getLink(descriptor, LINK_REL_STOP);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public Scope getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    @Override
    public void setScope(@Nonnull Scope scope) {
        this.scope = scope;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAlive() {
        return EnumSet.range(RUNNING, DONE).contains(status);
    }

    @Nullable
    private String getUrlByName(@Nonnull String name) {
        Link link = RunnerUtils.getLink(descriptor, name);
        return link == null ? null : link.getHref();
    }

    /** {@inheritDoc} */
    @Override
    public void setProcessDescriptor(@Nullable ApplicationProcessDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /** {@inheritDoc} */
    @Override
    public long getProcessId() {
        Objects.requireNonNull(descriptor);
        return descriptor.getProcessId();
    }

    @Nullable
    private RunnerMetric getRunnerMetricByName(@Nonnull String name) {
        if (descriptor == null) {
            return null;
        }

        for (RunnerMetric stat : descriptor.getRunStats()) {
            if (name.equals(stat.getName())) {
                return stat;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public RunOptions getOptions() {
        return runOptions;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RunnerImpl runner = (RunnerImpl)o;

        return creationTime == runner.creationTime && Objects.equals(title, runner.title);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

}