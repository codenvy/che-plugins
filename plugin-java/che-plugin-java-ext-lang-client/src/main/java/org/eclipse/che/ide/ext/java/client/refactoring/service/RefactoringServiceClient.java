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
package org.eclipse.che.ide.ext.java.client.refactoring.service;

import com.google.inject.ImplementedBy;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateMoveRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.MoveSettings;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;

/**
 * Provides methods which allow send requests to special refactoring service to do refactoring.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(RefactoringServiceClientImpl.class)
public interface RefactoringServiceClient {

    /**
     * Creates move refactoring and returns special refactoring session id which will be need to continue setup refactoring steps.
     *
     * @param moveRefactoring
     *         special object which contains information about items which will be refactored
     * @return an instance of refactoring session id
     */
    Promise<String> createMoveRefactoring(CreateMoveRefactoring moveRefactoring);

    Promise<RefactoringStatus> setDestination(ReorgDestination destination);

    Promise<Void> setMoveSettings(MoveSettings settings);

    Promise<ChangeCreationResult> createChange(RefactoringSession session);

    Promise<RefactoringPreview> getRefactoringPreview(RefactoringSession session);

    Promise<RefactoringStatus> applyRefactoring(RefactoringSession session);
}
