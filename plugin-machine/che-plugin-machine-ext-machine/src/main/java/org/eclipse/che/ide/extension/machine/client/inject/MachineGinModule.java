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
package org.eclipse.che.ide.extension.machine.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.arbitrary.ArbitraryCommandType;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditConfigurationsView;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditConfigurationsViewImpl;
import org.eclipse.che.ide.extension.machine.client.command.execute.ExecuteArbitraryCommandView;
import org.eclipse.che.ide.extension.machine.client.command.execute.ExecuteArbitraryCommandViewImpl;
import org.eclipse.che.ide.extension.machine.client.console.MachineConsoleToolbar;
import org.eclipse.che.ide.extension.machine.client.console.MachineConsoleView;
import org.eclipse.che.ide.extension.machine.client.console.MachineConsoleViewImpl;
import org.eclipse.che.ide.ui.toolbar.ToolbarPresenter;

/**
 * GIN module for Machine extension.
 *
 * @author Artem Zatsarynnyy
 */
@ExtensionGinModule
public class MachineGinModule extends AbstractGinModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(ToolbarPresenter.class).annotatedWith(MachineConsoleToolbar.class).to(ToolbarPresenter.class).in(Singleton.class);
        bind(MachineConsoleView.class).to(MachineConsoleViewImpl.class).in(Singleton.class);
        bind(ExecuteArbitraryCommandView.class).to(ExecuteArbitraryCommandViewImpl.class).in(Singleton.class);
        bind(EditConfigurationsView.class).to(EditConfigurationsViewImpl.class).in(Singleton.class);

        GinMultibinder.newSetBinder(binder(), CommandType.class).addBinding().to(ArbitraryCommandType.class);
    }
}
