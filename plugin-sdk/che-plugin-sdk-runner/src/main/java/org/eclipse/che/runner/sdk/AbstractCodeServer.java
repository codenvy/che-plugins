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
package org.eclipse.che.runner.sdk;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.ide.commons.GwtXmlUtils;
import org.eclipse.che.ide.maven.tools.Dependency;
import org.eclipse.che.ide.maven.tools.Model;
import org.eclipse.che.ide.maven.tools.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * GWT code server abstract implementation.
 *
 * @author Artem Zatsarynnyy
 */
public abstract class AbstractCodeServer implements CodeServer {
    private static final String ADD_SOURCES_PROFILE_ID          = "customExtensionSources";
    protected static final String UNABELE_UPDATE_SCRIPT_ATTRIBUTE = "Unable to update attributes of the startup script";
    /** Id of Maven POM profile used to add (re)sources of custom extension to code server recompilation process. */

    /**
     * Prepare GWT code server for launching.
     *
     * @param workDirPath
     *         root directory for code server
     * @param runnerConfiguration
     *         runner configuration
     * @param extensionDescriptor
     *         descriptor of extension for which code server should be prepared
     * @param executor
     *         executor service
     * @return {@link CodeServerProcess} that may be launched
     * @throws RunnerException
     */
    public CodeServerProcess prepare(Path workDirPath,
                                     final SDKRunnerConfiguration runnerConfiguration,
                                     Utils.ExtensionDescriptor extensionDescriptor,
                                     final ExecutorService executor) throws RunnerException {
        try {
            ZipUtils.unzip(Utils.getCodenvyPlatformBinaryDistribution().openStream(), workDirPath.toFile());

            final File pom = workDirPath.resolve("pom.xml").toFile();
            final Model model = Model.readFrom(pom);

            model.dependencies()
                 .add(new Dependency(extensionDescriptor.groupId,
                                     extensionDescriptor.artifactId,
                                     extensionDescriptor.version));
            model.writeTo(pom);

            GwtXmlUtils.inheritGwtModule(IoUtil.findFile(SDKRunner.IDE_GWT_XML_FILE_NAME, workDirPath.toFile()).toPath(),
                                         extensionDescriptor.gwtModuleName);
            setCodeServerConfiguration(workDirPath.resolve("pom.xml"), workDirPath, runnerConfiguration);

            // get initial copy of project sources
            final ProjectDescriptor projectDescriptor = runnerConfiguration.getRequest().getProjectDescriptor();
            final Path extensionSourcesPath = Files.createDirectory(workDirPath.resolve("extension-sources"));
            final java.io.File file = Utils.exportProject(projectDescriptor, extensionSourcesPath.toFile());
            ZipUtils.unzip(file, extensionSourcesPath.toFile());

            return createProcess(workDirPath.toFile(), runnerConfiguration, extensionSourcesPath, projectDescriptor.getBaseUrl(), executor);
        } catch (IOException e) {
            throw new RunnerException(e);
        }
    }

    /**
     * Create new gwt CodeServerProcess
     * @param codeServerWorkDir code server working directory
     * @param runnerConfiguration sdk runner configuration
     * @param extensionSourcesPath path to extension source
     * @param projectApiBaseUrl base project api url
     * @param executor executor service
     * @return new CodeServerProcess
     * @throws RunnerException
     */
    protected abstract CodeServerProcess createProcess(File codeServerWorkDir,
                                                       SDKRunnerConfiguration runnerConfiguration,
                                                       Path extensionSourcesPath,
                                                       String projectApiBaseUrl,
                                                       ExecutorService executor) throws RunnerException;

    /** Set the GWT code server configuration in the specified pom.xml file. */
    private void setCodeServerConfiguration(Path pom,
                                            Path codeServerWorkDir,
                                            SDKRunnerConfiguration runnerConfiguration) throws RunnerException {
        try {
            final Model model = Model.readFrom(pom);
            final Plugin gwtPlugin = model.getBuild()
                                          .getPluginsAsMap()
                                          .get("org.codehaus.mojo:gwt-maven-plugin");
            if (codeServerWorkDir != null) {
                gwtPlugin.setConfigProperty("codeServerWorkDir", codeServerWorkDir.toString());
            }

            final String bindAddress = runnerConfiguration.getCodeServerBindAddress();
            if (bindAddress != null) {
                gwtPlugin.setConfigProperty("bindAddress", bindAddress);
            }

            final int port = runnerConfiguration.getCodeServerPort();
            if (port != -1) {
                gwtPlugin.setConfigProperty("codeServerPort", Integer.toString(port));
            }

            model.save();
        } catch (IOException e) {
            throw new RunnerException(e);
        }
    }

    protected String generateSources() {
        return String.format("mvn clean generate-sources gwt:run-codeserver -Dgwt.compiler.incremental=false " +
                             "-Dgwt.module=org.eclipse.che.ide.IDEPlatform -P%s > stdout.log &\n",
                             ADD_SOURCES_PROFILE_ID);
    }
}
