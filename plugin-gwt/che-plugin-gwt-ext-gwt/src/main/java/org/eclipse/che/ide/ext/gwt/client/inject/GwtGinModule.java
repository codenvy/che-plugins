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
package org.eclipse.che.ide.ext.gwt.client.inject;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.ext.gwt.client.command.GwtCommandType;
import org.eclipse.che.ide.ext.gwt.client.wizard.GwtProjectWizardRegistrar;
import org.eclipse.che.ide.extension.machine.client.command.CommandType;

/**
 * GIN module for Che GWT extension.
 *
 * @author Artem Zatsarynnyi
 */
@ExtensionGinModule
public class GwtGinModule extends AbstractGinModule {

    @Override
    protected void configure() {
        GinMultibinder.newSetBinder(binder(), ProjectWizardRegistrar.class).addBinding().to(GwtProjectWizardRegistrar.class);
        GinMultibinder.newSetBinder(binder(), CommandType.class).addBinding().to(GwtCommandType.class);
    }
}
