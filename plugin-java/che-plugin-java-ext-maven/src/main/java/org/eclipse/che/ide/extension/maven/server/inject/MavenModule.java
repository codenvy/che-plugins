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
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.ide.ext.java.server.classpath.ClassPathBuilder;
import org.eclipse.che.ide.extension.maven.server.core.MavenClassPathBuilder;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenValueProviderFactory;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.AddMavenModuleHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.ArchetypeGenerationStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GeneratorStrategy;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.GetMavenModulesHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectCreatedHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectGenerator;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.MavenProjectImportedHandler;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.ProjectHasBecomeMaven;
import org.eclipse.che.ide.extension.maven.server.projecttype.handler.RemoveMavenModuleHandler;

/** @author Artem Zatsarynnyi */
public class MavenModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ValueProviderFactory.class).addBinding().to(MavenValueProviderFactory.class);

        Multibinder.newSetBinder(binder(), ProjectType.class).addBinding().to(MavenProjectType.class);

        Multibinder<ProjectHandler> projectHandlerMultibinder = Multibinder.newSetBinder(binder(), ProjectHandler.class);
        projectHandlerMultibinder.addBinding().to(MavenProjectGenerator.class);
        projectHandlerMultibinder.addBinding().to(AddMavenModuleHandler.class);
        projectHandlerMultibinder.addBinding().to(RemoveMavenModuleHandler.class);
        projectHandlerMultibinder.addBinding().to(MavenProjectImportedHandler.class);
        projectHandlerMultibinder.addBinding().to(ProjectHasBecomeMaven.class);
        projectHandlerMultibinder.addBinding().to(GetMavenModulesHandler.class);
        projectHandlerMultibinder.addBinding().to(MavenProjectCreatedHandler.class);

        Multibinder.newSetBinder(binder(), GeneratorStrategy.class).addBinding().to(ArchetypeGenerationStrategy.class);
        bind(ClassPathBuilder.class).to(MavenClassPathBuilder.class).in(Singleton.class);
    }
}
