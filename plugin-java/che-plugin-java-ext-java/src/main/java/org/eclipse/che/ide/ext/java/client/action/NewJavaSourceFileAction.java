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
package org.eclipse.che.ide.ext.java.client.action;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.newsourcefile.NewJavaSourceFilePresenter;
import org.eclipse.che.ide.ext.java.client.project.node.PackageNode;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FolderReferenceNode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * Action to create new Java source file.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NewJavaSourceFileAction extends ProjectAction {
    private final AnalyticsEventLogger       eventLogger;
    private       NewProjectExplorerPresenter             projectExplorer;
    private       NewJavaSourceFilePresenter newJavaSourceFilePresenter;

    @Inject
    public NewJavaSourceFileAction(NewProjectExplorerPresenter projectExplorer,
                                   NewJavaSourceFilePresenter newJavaSourceFilePresenter,
                                   JavaLocalizationConstant constant,
                                   JavaResources resources,
                                   AnalyticsEventLogger eventLogger) {
        super(constant.actionNewClassTitle(), constant.actionNewClassDescription(), resources.javaFile());
        this.newJavaSourceFilePresenter = newJavaSourceFilePresenter;
        this.projectExplorer = projectExplorer;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        newJavaSourceFilePresenter.showDialog();
    }

    @Override
    public void updateProjectAction(ActionEvent e) {

        List<?> selection = projectExplorer.getSelection().getAllElements();

        if (selection == null || selection.isEmpty() || selection.size() > 1) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        Object o = selection.get(0);

        e.getPresentation().setEnabledAndVisible(isSourceFolder(o) || o instanceof PackageNode);
    }

    private boolean isSourceFolder(Object o) {
        if (!(o instanceof FolderReferenceNode)) {
            return false;
        }

        Map<String, List<String>> attributes = ((FolderReferenceNode)o).getAttributes();
        return attributes.containsKey("javaContentRoot");
    }
}
