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
package org.eclipse.che.ide.ext.cpp.server.project.type;

import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.ext.cpp.shared.ProjectAttributes;

/**
 * @author Vitaly Parfonov
 * @author Dmitry Shnurenko
 */
public class CPPProjectType extends ProjectType {

    public CPPProjectType() {
        super(ProjectAttributes.CPP_ID, ProjectAttributes.CPP_NAME, true, false, true, "http://www.test.url/recipe");
        addConstantDefinition("language", "language", ProjectAttributes.PROGRAMMING_LANGUAGE);
    }
}
