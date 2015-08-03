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

import org.eclipse.che.api.runner.RunnerException;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * GWT code server.
 *
 * @author Artem Zatsarynnyy
 * @author Alexander Andrienko
 */
public interface CodeServer {
    CodeServerProcess prepare(Path workDirPath,
                              SDKRunnerConfiguration runnerConfiguration,
                              Utils.ExtensionDescriptor extensionDescriptor,
                              ExecutorService executor) throws RunnerException;
}
