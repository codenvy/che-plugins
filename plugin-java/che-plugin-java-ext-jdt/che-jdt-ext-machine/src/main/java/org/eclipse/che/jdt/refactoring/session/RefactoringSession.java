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

package org.eclipse.che.jdt.refactoring.session;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.jdt.refactoring.DtoConverter;
import org.eclipse.che.jdt.refactoring.RefactoringException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.core.refactoring.Messages;
import org.eclipse.ltk.internal.ui.refactoring.FinishResult;

/**
 * @author Evgen Vidolob
 */
public abstract class RefactoringSession {

    private Refactoring refactoring;
    private RefactoringStatus conditionCheckingStatus;
    private Change change;

    public RefactoringSession(Refactoring refactoring) {
        this.refactoring = refactoring;
    }

    public ChangeCreationResult createChange() throws RefactoringException {
        Change change = createChange(new CreateChangeOperation(
                new CheckConditionsOperation(refactoring, CheckConditionsOperation.FINAL_CONDITIONS),
                RefactoringStatus.FATAL), true);
        // Status has been updated since we have passed true
        RefactoringStatus status = conditionCheckingStatus;
        // Creating the change has been canceled
        if (change == null && status == null) {
            internalSetChange(change);
            throw new RefactoringException("Creating the change has been canceled");
        }

        // Set change if we don't have fatal errors.
        if (!status.hasFatalError()) {
            internalSetChange(change);
        }

        ChangeCreationResult result = DtoFactory.newDto(ChangeCreationResult.class);
        result.setStatus(DtoConverter.toRefactoringStatusDto(status));
        result.setCanShowPreviewPage(status.isOK());
        return result;
    }

    private void internalSetChange(Change change) {
        this.change = change;
    }

    private Change createChange(CreateChangeOperation operation, boolean updateStatus) throws RefactoringException {
        CoreException exception = null;
        try {
            ResourcesPlugin.getWorkspace().run(operation, new NullProgressMonitor());
        } catch (CoreException e) {
            exception = e;
        }

        if (updateStatus) {
            RefactoringStatus status = null;
            if (exception != null) {
                status = new RefactoringStatus();
                String msg = exception.getMessage();
                if (msg != null) {
                    status.addFatalError(Messages.format("{0}. See the error log for more details.", msg));
                } else {
                    status.addFatalError("An unexpected exception occurred while creating a change object. See the error log for more details.");
                }
                JavaPlugin.log(exception);
            } else {
                status= operation.getConditionCheckingStatus();
            }
            setConditionCheckingStatus(status);
        } else {
            if (exception != null)
                throw new RefactoringException(exception);
        }
        Change change= operation.getChange();
        return change;
    }

    public void setConditionCheckingStatus(RefactoringStatus conditionCheckingStatus) {
        this.conditionCheckingStatus = conditionCheckingStatus;
    }

    public Change getChange() {
        return change;
    }

    public RefactoringStatus apply() {
        PerformChangeOperation operation = new PerformChangeOperation(change);
        FinishResult result = internalPerformFinish(operation);
        if (result.isException()) {
            return RefactoringStatus.createErrorStatus("Refactoring failed with Exception.");
        }
        RefactoringStatus validationStatus= operation.getValidationStatus();
        if(validationStatus != null){
            return validationStatus;
        }

        return new RefactoringStatus();
    }

    private final FinishResult internalPerformFinish(PerformChangeOperation op) {
        op.setUndoManager(RefactoringCore.getUndoManager(), refactoring.getName());
        try{
            ResourcesPlugin.getWorkspace().run(op, new NullProgressMonitor());
        } catch (CoreException e) {
            JavaPlugin.log(e);
            return FinishResult.createException();
        }
        return FinishResult.createOK();
    }
}
