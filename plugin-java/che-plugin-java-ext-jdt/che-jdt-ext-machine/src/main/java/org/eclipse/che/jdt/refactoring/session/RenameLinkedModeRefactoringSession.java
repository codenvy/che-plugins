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

import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.jdt.refactoring.RefactoringException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author Evgen Vidolob
 */
public class RenameLinkedModeRefactoringSession extends RefactoringSession {
    public RenameLinkedModeRefactoringSession(Refactoring refactoring) {
        super(refactoring);
    }

    @Override
    public ChangeCreationResult createChange() throws RefactoringException {
        return super.createChange();
    }

    @Override
    public Change getChange() {
        return super.getChange();
    }

    @Override
    public RefactoringStatus apply() {
        return super.apply();
    }
}
