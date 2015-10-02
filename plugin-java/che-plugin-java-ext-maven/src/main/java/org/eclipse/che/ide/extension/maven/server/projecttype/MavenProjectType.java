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
package org.eclipse.che.ide.extension.maven.server.projecttype;

import com.google.inject.Inject;

import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.ext.java.server.projecttype.JavaProjectType;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import javax.inject.Singleton;

/**
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MavenProjectType extends ProjectType {

    public static final String DEFAULT_RECIPE =
            "https://gist.githubusercontent.com/gazarenkov/9f11a85a157ab399aca5/raw/b9f66169588f5394f84a1893cfeccd10e18d7fc8/maven";
    @Inject
    public MavenProjectType(MavenValueProviderFactory mavenValueProviderFactory, JavaProjectType javaProjectType) {

        super(MavenAttributes.MAVEN_ID, MavenAttributes.MAVEN_NAME, true, false, true, DEFAULT_RECIPE);

        addVariableDefinition(MavenAttributes.GROUP_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.ARTIFACT_ID, "", true, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.VERSION, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_VERSION, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_ARTIFACT_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_GROUP_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PACKAGING, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.SOURCE_FOLDER, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.TEST_SOURCE_FOLDER, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.RESOURCE_FOLDER, "", false, mavenValueProviderFactory);

        addParent(javaProjectType);
    }
}
