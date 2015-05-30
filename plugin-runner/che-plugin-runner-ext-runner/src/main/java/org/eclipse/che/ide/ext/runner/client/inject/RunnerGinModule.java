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
package org.eclipse.che.ide.ext.runner.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.preferences.PreferencePagePresenter;
import org.eclipse.che.ide.ext.runner.client.inject.factories.HandlerFactory;
import org.eclipse.che.ide.ext.runner.client.inject.factories.ModelsFactory;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.manager.button.ButtonWidget;
import org.eclipse.che.ide.ext.runner.client.manager.button.ButtonWidgetImpl;
import org.eclipse.che.ide.ext.runner.client.manager.info.MoreInfo;
import org.eclipse.che.ide.ext.runner.client.manager.info.MoreInfoImpl;
import org.eclipse.che.ide.ext.runner.client.manager.menu.MenuWidget;
import org.eclipse.che.ide.ext.runner.client.manager.menu.MenuWidgetImpl;
import org.eclipse.che.ide.ext.runner.client.manager.menu.entry.MenuEntry;
import org.eclipse.che.ide.ext.runner.client.manager.menu.entry.MenuEntryWidget;
import org.eclipse.che.ide.ext.runner.client.manager.preferences.RunnerPreferencesPresenter;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.EnvironmentImpl;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.models.RunnerImpl;
import org.eclipse.che.ide.ext.runner.client.tabs.console.button.ConsoleButton;
import org.eclipse.che.ide.ext.runner.client.tabs.console.button.ConsoleButtonImpl;
import org.eclipse.che.ide.ext.runner.client.tabs.console.panel.Console;
import org.eclipse.che.ide.ext.runner.client.tabs.console.panel.ConsoleImpl;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainerPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabWidgetImpl;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.button.PropertyButtonWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.button.PropertyButtonWidgetImpl;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl.PropertiesEnvironmentPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl.PropertiesRunnerPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl.PropertiesStubPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel.Terminal;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.panel.TerminalImpl;
import org.eclipse.che.ide.ext.runner.client.util.annotations.EnvironmentProperties;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.util.annotations.RightPropertiesPanel;
import org.eclipse.che.ide.ext.runner.client.util.annotations.RunnerProperties;

/**
 * The module that contains configuration of the client side part of the plugin.
 *
 * @author Andrey Plotnikov
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
@ExtensionGinModule
public class RunnerGinModule extends AbstractGinModule {
    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(TabContainer.class).annotatedWith(LeftPanel.class).to(TabContainerPresenter.class).in(Singleton.class);
        bind(TabContainer.class).annotatedWith(RightPropertiesPanel.class).to(TabContainerPresenter.class).in(Singleton.class);
        bind(TabContainer.class).annotatedWith(LeftPropertiesPanel.class).to(TabContainerPresenter.class).in(Singleton.class);

        install(new GinFactoryModuleBuilder().implement(Runner.class, RunnerImpl.class)
                                             .implement(Environment.class, EnvironmentImpl.class)
                                             .build(ModelsFactory.class));

        install(new GinFactoryModuleBuilder().build(HandlerFactory.class));

        install(new GinFactoryModuleBuilder().build(RunnerActionFactory.class));

        install(new GinFactoryModuleBuilder().implement(Terminal.class, TerminalImpl.class)
                                             .implement(Console.class, ConsoleImpl.class)
                                             .implement(ButtonWidget.class, ButtonWidgetImpl.class)
                                             .implement(ConsoleButton.class, ConsoleButtonImpl.class)
                                             .implement(TabWidget.class, TabWidgetImpl.class)
                                             .implement(PropertiesPanel.class,
                                                        EnvironmentProperties.class,
                                                        PropertiesEnvironmentPanel.class)
                                             .implement(PropertiesPanel.class, RunnerProperties.class, PropertiesRunnerPanel.class)
                                             .implement(PropertiesPanel.class, PropertiesStubPanel.class)
                                             .implement(MoreInfo.class, MoreInfoImpl.class)
                                             .implement(PropertyButtonWidget.class, PropertyButtonWidgetImpl.class)
                                             .implement(MenuWidget.class, MenuWidgetImpl.class)
                                             .implement(MenuEntry.class, MenuEntryWidget.class)
                                             .build(WidgetFactory.class));

        GinMultibinder<PreferencePagePresenter> prefBinder = GinMultibinder.newSetBinder(binder(), PreferencePagePresenter.class);
        prefBinder.addBinding().to(RunnerPreferencesPresenter.class);
    }

    /** Provides project-relative path to the folder for project-scoped runner environments. */
    @Provides
    @Named("envFolderPath")
    @Singleton
    protected String provideEnvironmentsFolderRelPath() {
        return ".che/runners/environments";
    }
}
