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

package org.eclipse.che.jdt;

import com.google.inject.AbstractModule;

import org.eclipse.che.core.internal.resources.ResourcesPlugin;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.jdt.rest.CodeAssistService;
import org.eclipse.core.internal.filebuffers.FileBuffersPlugin;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * @author Evgen Vidolob
 */
@DynaModule
public class JdtGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ResourcesPlugin.class).asEagerSingleton();
        bind(JavaPlugin.class).asEagerSingleton();
        bind(FileBuffersPlugin.class).asEagerSingleton();
        bind(ProjectListeners.class).asEagerSingleton();
        bind(CodeAssistService.class);
    }
}
