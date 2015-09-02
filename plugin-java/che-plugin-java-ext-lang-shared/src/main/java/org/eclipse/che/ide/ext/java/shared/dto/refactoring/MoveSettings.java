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

/**
 * @author Evgen Vidolob
 */
@DTO
public interface MoveSettings extends RefactoringSession {
    boolean isUpdateReferences();

    void setUpdateReferences(boolean updateReferences);

    /**
     * Used to ask the refactoring object whether references
     * in non Java files should be updated.
     * @return
     */
    boolean isUpdateQualifiedNames();

    /**
     * Used to inform the refactoring object whether
     * references in non Java files should be updated.
     * @param update
     */
    void setUpdateQualifiedNames(boolean update);

    String getFilePatterns();

    void setFilePatterns(String patterns);

}
