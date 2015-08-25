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
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import org.eclipse.che.core.internal.resources.ResourcesPlugin;
import org.eclipse.che.inject.DynaModule;
import org.eclipse.che.jdt.rest.CodeAssistService;
import org.eclipse.che.jdt.rest.CompilerSetupService;
import org.eclipse.che.jdt.rest.JavaClasspathService;
import org.eclipse.che.jdt.rest.JavaReconcileService;
import org.eclipse.core.internal.filebuffers.FileBuffersPlugin;
import org.eclipse.ide.ext.machine.ProjectEventListener;
import org.eclipse.jdt.internal.corext.format.FormatService;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * @author Evgen Vidolob
 */
@DynaModule
public class JdtGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JavadocService.class);
        bind(JavaNavigationService.class);
        bind(JavaReconcileService.class);
        bind(JavaClasspathService.class);
        bind(FormatService.class);
        bind(CodeAssistService.class);
        bind(CompilerSetupService.class);
        bind(ResourcesPlugin.class).asEagerSingleton();
        bind(JavaPlugin.class).asEagerSingleton();
        bind(FileBuffersPlugin.class).asEagerSingleton();
        bind(ProjectListeners.class).asEagerSingleton();
        Multibinder<ProjectEventListener> listenerMultibinder = Multibinder.newSetBinder(binder(), ProjectEventListener.class);
        listenerMultibinder.addBinding().to(ProjectListeners.class);
    }

    @Provides
    @Named("che.workspace.path")
    @Singleton
    protected String provideWorkspace() {
        return "/projects";
    }

    @Provides
    @Named("che.jdt.settings.dir")
    @Singleton
    protected String provideSettings(@Named("che.settings.folder") String pathToCheFolder) {
        return pathToCheFolder + "/settings";
    }

    @Provides
    @Named("che.jdt.workspace.index.dir")
    @Singleton
    protected String provideIndex(@Named("che.settings.folder") String pathToCheFolder) {
        return pathToCheFolder + "/index";
    }
}
