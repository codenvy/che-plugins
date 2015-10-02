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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.FileEntry;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.InvalidValueException;
import org.eclipse.che.api.project.server.ValueProvider;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.ValueStorageException;
import org.eclipse.che.api.project.server.VirtualFileEntry;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.commons.xml.XMLTreeException;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;
import org.eclipse.che.ide.maven.tools.Build;
import org.eclipse.che.ide.maven.tools.Model;
import org.eclipse.che.ide.maven.tools.Resource;

import org.eclipse.che.commons.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Evgen Vidolob
 */
public class MavenValueProviderFactory implements ValueProviderFactory {

    private final String xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    protected Model readModel(FolderEntry projectFolder) throws ValueStorageException, ServerException, ForbiddenException, IOException {
        FileEntry pomFile = (FileEntry)projectFolder.getChild("pom.xml");
        if (pomFile == null) {
            throw new ValueStorageException("pom.xml does not exist.");
        }
        return Model.readFrom(pomFile.getInputStream());
    }

    @Nullable
    protected VirtualFile getPom(FolderEntry projectFolder) {
        try {
            final VirtualFileEntry pomFile = projectFolder.getChild("pom.xml");
            if (pomFile != null) {
                return pomFile.getVirtualFile();
            }
            return null;
        } catch (ForbiddenException | ServerException e) {
            return null;
        }
    }

    protected void throwReadException(Exception e) throws ValueStorageException {
        throw new ValueStorageException("Can't read pom.xml : " + e.getMessage());
    }

    protected void throwWriteException(Exception e) throws ValueStorageException {
        throw new ValueStorageException("Can't write pom.xml : " + e.getMessage());
    }

    @Override
    public ValueProvider newInstance(FolderEntry projectFolder) {
        return new MavenValueProvider(projectFolder);
    }

    protected class MavenValueProvider implements ValueProvider {

        protected FolderEntry projectFolder;

        protected MavenValueProvider(FolderEntry projectFolder) {
            this.projectFolder = projectFolder;
        }

        @Override
        public List<String> getValues(String attributeName) throws ValueStorageException {
            try {
                String value = "";
                Model model = readModel(projectFolder);
                if (attributeName.equals(MavenAttributes.ARTIFACT_ID))
                    value = model.getArtifactId();
                if (attributeName.equals(MavenAttributes.GROUP_ID))
                    value = model.getGroupId();
                if (attributeName.equals(MavenAttributes.PACKAGING))
                    value = model.getPackaging();
                if (attributeName.equals(MavenAttributes.VERSION))
                    value = model.getVersion();
                if (attributeName.equals(MavenAttributes.PARENT_ARTIFACT_ID) && model.getParent() != null)
                    value = model.getParent().getArtifactId();
                if (attributeName.equals(MavenAttributes.PARENT_GROUP_ID) && model.getParent() != null)
                    value = model.getParent().getGroupId();
                if (attributeName.equals(MavenAttributes.PARENT_VERSION) && model.getParent() != null)
                    value = model.getParent().getVersion();
                if (attributeName.equals(MavenAttributes.SOURCE_FOLDER)) {
                    Build build = model.getBuild();
                    if (build != null && build.getSourceDirectory() != null) {
                        value = build.getSourceDirectory();
                    } else {
                        value = "src/main/java";
                    }
                }
                if (attributeName.equals(MavenAttributes.TEST_SOURCE_FOLDER)) {
                    Build build = model.getBuild();
                    if(build != null && build.getTestSourceDirectory() != null) {
                        value = build.getTestSourceDirectory();
                    } else {
                        value = "src/test/java";
                    }
                }
                if (attributeName.equals(MavenAttributes.RESOURCE_FOLDER)) {
                    Build build = model.getBuild();
                    if (build != null && build.getResources() != null) {
                        List<Resource> resources = build.getResources();
                        List<String> resourcesDirectory = new ArrayList<>();
                        for (Resource resource : resources) {
                            resourcesDirectory.add(resource.getDirectory());
                        }
                        return resourcesDirectory;
                    } else {
                        return Arrays.asList("src/main/resources", "src/test/resources");
                    }
                }

                return Arrays.asList(value);
            } catch (ServerException | ForbiddenException | IOException e) {
                throwReadException(e);
            } catch (XMLTreeException e) {
                throw new ValueStorageException("Error parsing pom.xml : " + e.getMessage());
            }
            return null;
        }

        @Override
        public void setValues(String attributeName, List<String> value) throws ValueStorageException, InvalidValueException {
            try {
                VirtualFile pom = getPom(projectFolder);
                if(pom == null) {
                    Model model = Model.createModel();
                    model.setModelVersion("4.0.0");
                    pom = projectFolder.createFile("pom.xml", new byte[0], "text/xml").getVirtualFile();
                    model.writeTo(pom);
                }

                if (attributeName.equals(MavenAttributes.ARTIFACT_ID))
                    Model.readFrom(pom).setArtifactId(value.get(0)).writeTo(pom);
                if (attributeName.equals(MavenAttributes.GROUP_ID))
                    Model.readFrom(pom).setGroupId(value.get(0)).writeTo(pom);
                if (attributeName.equals(MavenAttributes.PACKAGING)) {
                    Model model = Model.readFrom(pom);
                    if(model.getPackaging() != null) {
                        model.setPackaging(value.get(0)).writeTo(pom);
                    }
                }
                if (attributeName.equals(MavenAttributes.VERSION))
                    Model.readFrom(pom).setVersion(value.get(0)).writeTo(pom);

            } catch (ForbiddenException | ServerException | IOException | ConflictException e) {
                throwWriteException(e);
            } catch (XMLTreeException e) {
                throw new ValueStorageException("Error parsing pom.xml : " + e.getMessage());
            }
        }
    }
}
