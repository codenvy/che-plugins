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

package org.eclipse.che.ide.ext.java.shared.dto.refactoring;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.che.ide.ext.java.shared.dto.LinkedModeModel;

/**
 * Answer for {@link CreateRenameRefactoring}
 * @author Evgen Vidolob
 */
@DTO
public interface RenameRefactoringSession extends RefactoringSession {

    /**
     * if true caller must show rename wizard according to the wizard type
     */
    boolean isMastShowWizard();

    void setMastShowWizard(boolean showWizard);

    RenameWizard getWizardType();

    void setWizardType(RenameWizard type);

    /**
     * Linked edit model, not null if refactoring performed from editor and wizard is not necessary.
     */
    LinkedModeModel getLinkedModeModel();

    void setLinkedModeModel(LinkedModeModel model);


    enum RenameWizard{
        PACKAGE,
        COMPILATION_UNIT,
        TYPE,
        FIELD,
        ENUM_CONSTANT,
        TYPE_PARAMETER,
        METHOD,
        LOCAL_VARIABLE
    }
}
