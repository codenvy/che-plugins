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
package org.eclipse.che.ide.ext.runner.client.manager.preferences;

import com.google.inject.ImplementedBy;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.*;

import javax.annotation.Nonnull;

/**
 * @author Ann Shumilova
 */
@ImplementedBy(RunnerPreferencesViewImpl.class)
public interface RunnerPreferencesView extends View<RunnerPreferencesView.ActionDelegate> {

    /**
     * Select a given value into Shutdown field.
     *
     * @param shutdown
     *         value that needs to be chosen
     */
    void selectShutdown(@Nonnull Shutdown shutdown);

    /** @return chosen value of Shutdown field */
    @Nonnull
    Shutdown getShutdown();

    /**
     * Change state of set timeout button.
     *
     * @param isEnabled
     *         enabled state
     */
    void enableSetButton(boolean isEnabled);

    public interface ActionDelegate {

        void onValueChanged();

        void onSetShutdownClicked();
    }
}
