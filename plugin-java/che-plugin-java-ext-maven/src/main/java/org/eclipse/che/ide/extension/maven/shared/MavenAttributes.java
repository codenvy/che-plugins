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
package org.eclipse.che.ide.extension.maven.shared;

/**
 * @author Evgen Vidolob
 */
public interface MavenAttributes {
    final String MAVEN_ID   = "maven";
    final String MAVEN_NAME = "Maven Project";

    final String GENERATION_STRATEGY_OPTION   = "type";

    final String SIMPLE_GENERATION_STRATEGY    = "simple";
    final String ARCHETYPE_GENERATION_STRATEGY = "archetype";

    final String ARCHETYPE_GROUP_ID_OPTION    = "archetypeGroupId";
    final String ARCHETYPE_ARTIFACT_ID_OPTION = "archetypeArtifactId";
    final String ARCHETYPE_VERSION_OPTION     = "archetypeVersion";
    final String ARCHETYPE_REPOSITORY_OPTION  = "archetypeRepository";

    final String GROUP_ID           = "maven.groupId";
    final String ARTIFACT_ID        = "maven.artifactId";
    final String VERSION            = "maven.version";
    final String PACKAGING          = "maven.packaging";
    final String PARENT_GROUP_ID    = "maven.parent.groupId";
    final String PARENT_ARTIFACT_ID = "maven.parent.artifactId";
    final String PARENT_VERSION     = "maven.parent.version";

    final String SOURCE_FOLDER      = "maven.source.folder";
    final String TEST_SOURCE_FOLDER = "maven.test.source.folder";

    final String RESOURCE_FOLDER      = "maven.resource.folder";

    final String DEFAULT_SOURCE_FOLDER      = "src/main/java";
    final String DEFAULT_TEST_SOURCE_FOLDER = "src/test/java";
    final String DEFAULT_VERSION            = "1.0-SNAPSHOT";
}
