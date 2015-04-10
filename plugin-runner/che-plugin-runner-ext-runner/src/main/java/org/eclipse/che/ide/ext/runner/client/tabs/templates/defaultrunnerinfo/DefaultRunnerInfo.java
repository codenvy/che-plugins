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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.defaultrunnerinfo;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.ext.runner.client.models.Environment;

import javax.annotation.Nonnull;

/**
 * Provides methods which allows change information about default environment.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(DefaultRunnerInfoImpl.class)
public interface DefaultRunnerInfo extends IsWidget {

    /**
     * Updates information about default runner which is displayed on special popup window.
     *
     * @param environment
     *         default environment for which need displays info
     */
    void update(@Nonnull Environment environment);
}
