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
package org.eclipse.che.ide.ext.openshift.server.inject;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.ext.openshift.server.OpenshiftProjectType;
import org.eclipse.che.ide.ext.openshift.server.rest.BuildConfigService;
import org.eclipse.che.ide.ext.openshift.server.rest.BuildService;
import org.eclipse.che.ide.ext.openshift.server.rest.DeploymentConfigService;
import org.eclipse.che.ide.ext.openshift.server.rest.ImageStreamService;
import org.eclipse.che.ide.ext.openshift.server.rest.OpenshiftExceptionMapper;
import org.eclipse.che.ide.ext.openshift.server.rest.OpenshiftApiInfoService;
import org.eclipse.che.ide.ext.openshift.server.rest.ProjectService;
import org.eclipse.che.ide.ext.openshift.server.rest.ReplicationControllerService;
import org.eclipse.che.ide.ext.openshift.server.rest.RouteService;
import org.eclipse.che.ide.ext.openshift.server.rest.ServiceService;
import org.eclipse.che.ide.ext.openshift.server.rest.TemplateService;

/**
 * @author Sergii Leschenko
 */
public class OpenshiftModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ImageStreamService.class);
        bind(DeploymentConfigService.class);
        bind(BuildConfigService.class);
        bind(RouteService.class);
        bind(ServiceService.class);
        bind(TemplateService.class);
        bind(ProjectService.class);
        bind(BuildService.class);
        bind(OpenshiftApiInfoService.class);
        bind(ReplicationControllerService.class);
        bind(OpenshiftExceptionMapper.class);

        final Multibinder<ProjectType> projectTypeMultibinder = Multibinder.newSetBinder(binder(), ProjectType.class);
        projectTypeMultibinder.addBinding().to(OpenshiftProjectType.class);
    }
}
