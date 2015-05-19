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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.extension.machine.client.command.CommandConfiguration;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;
import org.eclipse.che.ide.extension.machine.client.command.ConfigurationFactory;
import org.eclipse.che.ide.extension.machine.client.command.ConfigurationPage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Arbitrary command type.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ArbitraryCommandType implements CommandType {

    private static final String ID           = "arbitrary";
    private static final String DISPLAY_NAME = "Arbitrary command";

    private final MachineResources              resources;
    private final ArbitraryConfigurationFactory configurationFactory;

    private final Collection<ConfigurationPage<? extends CommandConfiguration>> pages;

    @Inject
    public ArbitraryCommandType(MachineResources resources, ArbitraryPagePresenter page) {
        this.resources = resources;
        configurationFactory = new ArbitraryConfigurationFactory(this);
        pages = new LinkedList<>();
        pages.add(page);
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nonnull
    @Override
    public SVGResource getIcon() {
        return resources.arbitraryCommandType();
    }

    @Nonnull
    @Override
    public Collection<ConfigurationPage<? extends CommandConfiguration>> getConfigurationPages() {
        return pages;
    }

    @Nonnull
    @Override
    public ConfigurationFactory<ArbitraryCommandConfiguration> getConfigurationFactory() {
        return configurationFactory;
    }
}
