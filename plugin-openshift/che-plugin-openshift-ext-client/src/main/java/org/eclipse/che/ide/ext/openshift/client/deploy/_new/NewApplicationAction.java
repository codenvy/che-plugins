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
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import javax.validation.constraints.NotNull;

import java.util.Collections;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

/**
 * Action for deploying Che project to OpenShift new application.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewApplicationAction extends AbstractPerspectiveAction {

    private final AnalyticsEventLogger          eventLogger;
    private final NewApplicationPresenter       presenter;
    private final AppContext                    appContext;

    @Inject
    public NewApplicationAction(final AnalyticsEventLogger eventLogger,
                                final NewApplicationPresenter presenter,
                                final AppContext appContext,
                                OpenshiftLocalizationConstant locale) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID), locale.newApplicationAction(), null, null, null);
        this.eventLogger = eventLogger;
        this.presenter = presenter;
        this.appContext = appContext;
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        CurrentProject currentProject = appContext.getCurrentProject();
        event.getPresentation().setVisible(currentProject != null);
        event.getPresentation().setEnabled(currentProject != null
                                           && !currentProject.getRootProject().getMixins().contains(OPENSHIFT_PROJECT_TYPE_ID));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.show();
    }
}
