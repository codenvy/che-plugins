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
package org.eclipse.che.ide.extension.machine.client.command.arbitrary;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

/**
 * The view of {@link ArbitraryPagePresenter}.
 *
 * @author Artem Zatsarynnyy
 */
@ImplementedBy(ArbitraryPageViewImpl.class)
public interface ArbitraryPageView extends View<ArbitraryPageView.ActionDelegate> {

    /** Returns command line. */
    String getCommandLine();

    /** Sets command line. */
    void setCommandLine(String commandLine);

    /** Action handler for the view actions/controls. */
    interface ActionDelegate {

        /** Called when command line is changed. */
        void onCommandLineChanged(String commandLine);
    }
}
