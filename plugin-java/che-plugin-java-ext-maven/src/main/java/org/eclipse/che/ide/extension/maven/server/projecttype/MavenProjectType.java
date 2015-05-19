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
 */
@Singleton
public class MavenProjectType extends ProjectType {

    @Inject
    public MavenProjectType(MavenValueProviderFactory mavenValueProviderFactory, JavaProjectType javaProjectType) {
        super(MavenAttributes.MAVEN_ID, MavenAttributes.MAVEN_NAME, true, false);

        addVariableDefinition(MavenAttributes.GROUP_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.ARTIFACT_ID, "", true, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.VERSION, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_VERSION, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_ARTIFACT_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PARENT_GROUP_ID, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.PACKAGING, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.SOURCE_FOLDER, "", false, mavenValueProviderFactory);
        addVariableDefinition(MavenAttributes.TEST_SOURCE_FOLDER, "", false, mavenValueProviderFactory);

        addParent(javaProjectType);
    }
}
