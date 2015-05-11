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
package org.eclipse.che.ide.ext.svn.server;

import org.eclipse.che.ide.ext.svn.server.credentials.CredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.credentials.CurrentUserPreferencesAccess;
import org.eclipse.che.ide.ext.svn.server.credentials.CurrentUserPreferencesAccessImpl;
import org.eclipse.che.ide.ext.svn.server.credentials.PreferencesCredentialsProvider;
import org.eclipse.che.ide.ext.svn.server.repository.RepositoryUrlProvider;
import org.eclipse.che.ide.ext.svn.server.repository.RepositoryUrlProviderImpl;
import org.eclipse.che.ide.ext.svn.server.rest.SubversionService;

import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Module for wiring up the server-side portion of this extension.
 *
 * @author <a href="mailto:jwhitlock@apache.org">Jeremy Whitlock</a>
 */
@DynaModule
public class SubversionModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ProjectImporter.class).addBinding().to(SubversionProjectImporter.class);
        Multibinder.newSetBinder(binder(), ProjectType.class).addBinding().to(SubversionProjectType.class);
        Multibinder.newSetBinder(binder(), ValueProviderFactory.class).addBinding()
                   .to(SubversionValueProviderFactory.class);

        bind(SubversionService.class);
        bind(CredentialsProvider.class).to(PreferencesCredentialsProvider.class);
        bind(RepositoryUrlProvider.class).to(RepositoryUrlProviderImpl.class);
        bind(CurrentUserPreferencesAccess.class).to(CurrentUserPreferencesAccessImpl.class);

        bind(SubversionConfigurationChecker.class).asEagerSingleton();
    }
}
