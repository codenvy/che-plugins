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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;

import javax.annotation.Nonnull;

/**
 * Describes methods which allows change view representation of filter panel.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(FilterWidgetImpl.class)
public interface FilterWidget extends View<FilterWidget.ActionDelegate> {

    /** @return boolean matches project's type. */
    boolean getMatchesProjectType();

    /** @param matches  whether matches project's type. */
    void setMatchesProjectType(boolean matches);

    interface ActionDelegate {
        /** Performs some actions in response to user's changing filter configuration. */
        void onValueChanged();
    }
}