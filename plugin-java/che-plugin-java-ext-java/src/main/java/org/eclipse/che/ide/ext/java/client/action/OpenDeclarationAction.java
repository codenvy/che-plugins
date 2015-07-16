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
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.editor.OpenDeclarationFinder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class OpenDeclarationAction extends ProjectAction {

    private EditorAgent           editorAgent;
    private OpenDeclarationFinder declarationFinder;

    private final AnalyticsEventLogger eventLogger;

    @Inject
    public OpenDeclarationAction(JavaLocalizationConstant constant,
                                 EditorAgent editorAgent,
                                 OpenDeclarationFinder declarationFinder,
                                 AnalyticsEventLogger eventLogger) {
        super(constant.actionOpenDeclarationTitle(), constant.actionOpenDeclarationDescription());
        this.editorAgent = editorAgent;
        this.declarationFinder = declarationFinder;
        this.eventLogger = eventLogger;
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        if (editorAgent.getActiveEditor() != null) {
            EditorInput input = editorAgent.getActiveEditor().getEditorInput();
            VirtualFile file = input.getFile();
            String mediaType = file.getMediaType();
            if (mediaType != null && (mediaType.equals(MimeType.TEXT_X_JAVA) ||
                                      mediaType.equals(MimeType.TEXT_X_JAVA_SOURCE) ||
                                      mediaType.equals(MimeType.APPLICATION_JAVA_CLASS))) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }
        }
        e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        declarationFinder.openDeclaration();
    }
}
