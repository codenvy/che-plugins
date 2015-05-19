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
package org.eclipse.che.ide.extension.machine.client.command.arbitrary;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.extension.machine.client.command.ConfigurationPage;

import javax.annotation.Nonnull;

/**
 * Page allows to edit arbitrary command.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ArbitraryPagePresenter implements ArbitraryPageView.ActionDelegate, ConfigurationPage<ArbitraryCommandConfiguration> {

    private final ArbitraryPageView             view;
    private       ArbitraryCommandConfiguration configuration;

    @Inject
    public ArbitraryPagePresenter(ArbitraryPageView view) {
        this.view = view;
        view.setDelegate(this);
    }

    @Override
    public void resetFrom(@Nonnull ArbitraryCommandConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);

        view.setCommandLine(configuration.getCommandLine());
    }

    @Override
    public void onCommandLineChanged(String commandLine) {
        configuration.setCommandLine(commandLine);
    }
}
