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
package org.eclipse.che.ide.extension.maven.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenValueProviderFactory;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.AddMavenModuleHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.ArchetypeGenerationStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GeneratorStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GetMavenModulesHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenClassPathCreator;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectGenerator;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectImportedHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.ProjectHasBecomeMaven;
import org.eclipse.che.inject.DynaModule;

/** @author Artem Zatsarynnyy */
@DynaModule
public class MavenModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ValueProviderFactory.class).addBinding().to(MavenValueProviderFactory.class);
        Multibinder.newSetBinder(binder(), ProjectType.class).addBinding().to(MavenProjectType.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(MavenProjectGenerator.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(AddMavenModuleHandler.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(MavenProjectImportedHandler.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(ProjectHasBecomeMaven.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(GetMavenModulesHandler.class);
        Multibinder.newSetBinder(binder(), ProjectHandler.class).addBinding().to(MavenClassPathCreator.class);
        Multibinder.newSetBinder(binder(), GeneratorStrategy.class).addBinding().to(ArchetypeGenerationStrategy.class);
    }
}
