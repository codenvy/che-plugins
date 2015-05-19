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
package org.eclipse.che.plugin.docker.client;

/**
 * @author andrew00x
 */
public interface LogMessageProcessor {
    void process(LogMessage logMessage);

    LogMessageProcessor DEV_NULL = new LogMessageProcessor() {
        @Override
        public void process(LogMessage logMessage) {
        }
    };

}
