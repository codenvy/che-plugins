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
package org.eclipse.che.ide.ext.runner.client.inject.factories;

import org.eclipse.che.ide.ext.runner.client.manager.button.ButtonWidget;
import org.eclipse.che.ide.ext.runner.client.manager.info.MoreInfo;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.console.button.ConsoleButton;
import org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Console;
import org.eclipse.che.ide.ext.runner.client.tabs.console.panel.FullLogMessageWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.Background;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.history.runner.RunnerWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.button.PropertyButtonWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel.Terminal;
import org.eclipse.che.ide.ext.runner.client.util.annotations.EnvironmentProperties;
import org.eclipse.che.ide.ext.runner.client.util.annotations.RunnerProperties;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;

/**
 * The factory for creating an instances of the widget.
 *
 * @author Dmitry Shnurenko
 */
public interface WidgetFactory {

    /**
     * Creates button widget with special icon.
     *
     * @param prompt
     *         prompt for current button which is displayed on special popup widget
     * @param resource
     *         icon which need set to button
     * @return an instance of {@link ButtonWidget}
     */
    @Nonnull
    ButtonWidget createButton(@Nonnull String prompt, @Nonnull SVGResource resource);

    /**
     * Creates console button widget with special icon.
     *
     * @param prompt
     *         prompt for current button which is displayed on special popup widget
     * @param resource
     *         icon which need set to button
     * @return an instance of {@link ConsoleButton}
     */
    @Nonnull
    ConsoleButton createConsoleButton(@Nonnull String prompt, @Nonnull SVGResource resource);

    /**
     * Creates tab widget with special title.
     *
     * @param title
     *         title which need set to widget's special place
     * @param tabType
     *         enum which contains values of height and width
     * @return an instance of {@link TabWidget}
     */
    @Nonnull
    TabWidget createTab(@Nonnull String title, @Nonnull TabType tabType);

    /**
     * Creates runner widget.
     *
     * @return an instance of {@link RunnerWidget}
     */
    @Nonnull
    RunnerWidget createRunner();

    /**
     * Creates environment widget.
     *
     * @return an instance of {@link EnvironmentWidget}
     */
    @Nonnull
    EnvironmentWidget createEnvironment();

    /**
     * Creates a console widget for a given runner.
     *
     * @param runner
     *         runner that needs to be bound with a widget
     * @return an instance of {@link org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Console}
     */
    @Nonnull
    Console createConsole(@Nonnull Runner runner);

    /**
     * Creates terminal widget.
     *
     * @return an instance of {@link Terminal}
     */
    @Nonnull
    Terminal createTerminal();

    /**
     * Creates a properties panel widget for a given runner.
     *
     * @param runner
     *         runner that needs to be bound with a widget
     * @return an instance of {@link PropertiesPanel}
     */
    @Nonnull
    @RunnerProperties
    PropertiesPanel createPropertiesPanel(@Nonnull Runner runner);

    /**
     * Creates a properties panel widget for a given environment.
     *
     * @param environment
     *         environment that needs to be bound with a widget
     * @return an instance of {@link PropertiesPanel}
     */
    @Nonnull
    @EnvironmentProperties
    PropertiesPanel createPropertiesPanel(@Nonnull Environment environment);

    /**
     * Creates stab of properties panel widget
     *
     * @return an instance of {@link PropertiesPanel}
     */
    @Nonnull
    PropertiesPanel createPropertiesPanel();

    /**
     * Creates more info popup widget.
     *
     * @return an instance of {@link MoreInfo}
     */
    @Nonnull
    MoreInfo createMoreInfo();

    /**
     * Creates message widget that need to be displayed in the console.
     *
     * @param logUrl
     *         url where full log is located
     * @return an instance of {@link FullLogMessageWidget}
     */
    @Nonnull
    FullLogMessageWidget createFullLogMessage(@Nonnull String logUrl);

    /**
     * Creates property button widget.
     *
     * @param title
     *         title of button
     * @param background
     *         background of button
     * @return an instance of {@link org.eclipse.che.ide.ext.runner.client.tabs.properties.button.PropertyButtonWidget}
     */
    @Nonnull
    PropertyButtonWidget createPropertyButton(@Nonnull String title, @Nonnull Background background);

}