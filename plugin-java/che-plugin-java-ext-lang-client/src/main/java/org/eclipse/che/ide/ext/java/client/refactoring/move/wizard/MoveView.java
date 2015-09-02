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
package org.eclipse.che.ide.ext.java.client.refactoring.move.wizard;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.java.client.refactoring.RefactorInfo;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaProject;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;

import java.util.List;

/**
 * @author Dmitry Shnurenko
 */
@ImplementedBy(MoveViewImpl.class)
interface MoveView extends View<MoveView.ActionDelegate> {

    void show(RefactorInfo refactorInfo);

    void hide();

    void setTreeOfDestinations(List<JavaProject> projects);

    void showStatusMessage(RefactoringStatus status);

    void clearStatusMessage();

    boolean isUpdateReferences();

    boolean isUpdateQualifiedNames();

    String getFilePatterns();

    interface ActionDelegate {
        void onPreviewButtonClicked();

        void onAcceptButtonClicked();

        void setMoveDestinationPath(String path, String projectPath);
    }
}
