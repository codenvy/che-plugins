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
package org.eclipse.che.ide.ext.java.server.projecttype;

import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.ext.java.shared.Constants;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.eclipse.che.ide.api.project.type.RunnerCategory.JAVA;

/**
 * @author gazarenkov
 * @author Dmitry Shnurenko
 */
public class JavaProjectType extends ProjectType {
    private static final Logger LOG = LoggerFactory.getLogger(JavaProjectType.class);

    @Inject
    public JavaProjectType() {
        super("java", "Java", true, false);
        addConstantDefinition(Constants.LANGUAGE, "language", "java");
        addConstantDefinition(Constants.LANGUAGE_VERSION, "language version", "1.6");
        addRunnerCategories(Arrays.asList(JAVA.toString()));
    }

}

