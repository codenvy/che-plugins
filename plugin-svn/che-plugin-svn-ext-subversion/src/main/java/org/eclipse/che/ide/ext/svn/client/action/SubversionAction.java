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
package org.eclipse.che.ide.ext.svn.client.action;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;
import org.eclipse.che.ide.ext.svn.shared.SubversionTypeConstant;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.List;

/**
 * Extension of {@link ProjectAction} that all Subversion extensions will extend.
 */
public abstract class SubversionAction extends ProjectAction {

    protected final AnalyticsEventLogger                     eventLogger;
    private         ProjectExplorerPresenter                 projectExplorerPresenter;
    protected final AppContext                               appContext;
    protected final SubversionExtensionLocalizationConstants constants;
    protected final SubversionExtensionResources             resources;
    protected final String                                   title;

    /**
     * Constructor.
     */
    public SubversionAction(final String title,
                            final String description,
                            final SVGResource svgIcon,
                            final AnalyticsEventLogger eventLogger,
                            final AppContext appContext,
                            final SubversionExtensionLocalizationConstants constants,
                            final SubversionExtensionResources resources,
                            final ProjectExplorerPresenter projectExplorerPresenter) {
        super(title, description, svgIcon);

        this.constants = constants;
        this.resources = resources;
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.projectExplorerPresenter = projectExplorerPresenter;
        this.title = title;
    }

    @Override
    public void updateProjectAction(ActionEvent e) {
        e.getPresentation().setVisible(getActiveProject() != null);
        e.getPresentation().setEnabled(isSubversionWC());

        if (e.getPresentation().isEnabled() && isSelectionRequired()) {
            e.getPresentation().setEnabled(isItemSelected());
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        //Window.alert(constants.actionNotImplemented());
    }

    /**
     * @return the active project or null if there is none
     */
    protected CurrentProject getActiveProject() {
        return appContext.getCurrentProject();
    }

    /**
     * @return if there is currently an item selected
     */
    protected boolean isItemSelected() {
        final Selection<?> selection = projectExplorerPresenter.getSelection();

        return selection != null && selection.getHeadElement() != null &&
               selection.getHeadElement() instanceof HasStorablePath;
    }

    /**
     * @return true if the project is a Subversion working copy or false otherwise
     */
    protected boolean isSubversionWC() {
        // TODO: We should probably cache this

        final CurrentProject currentProject = getActiveProject();
        boolean isSubversionWC = false;

        if (currentProject != null) {
            final ProjectDescriptor rootProjectDescriptor = currentProject.getRootProject();
            final List<String> mixins = rootProjectDescriptor.getMixins();
            if (mixins != null && mixins.contains(SubversionTypeConstant.SUBVERSION_MIXIN_TYPE)) {
                isSubversionWC = true;
            }
        }

        return isSubversionWC;
    }

    /**
     * @return whether or not a selection is required
     */
    protected boolean isSelectionRequired() {
        return false;
    }

}
