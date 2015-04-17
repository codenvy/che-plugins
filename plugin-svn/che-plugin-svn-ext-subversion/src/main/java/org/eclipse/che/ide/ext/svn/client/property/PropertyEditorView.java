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
package org.eclipse.che.ide.ext.svn.client.property;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;
import org.eclipse.che.ide.ext.svn.shared.Depth;

/**
 * View for {@link org.eclipse.che.ide.ext.svn.client.property.PropertyEditorPresenter}.
 *
 * @author Vladyslav Zhukovskyi
 */
public interface PropertyEditorView extends View<PropertyEditorView.ActionDelegate> {

    public interface ActionDelegate extends BaseActionDelegate {
        void onCancelClicked();

        void onOkClicked();
    }

    /** Perform actions when close window performed. */
    void onClose();

    /** Perform actions when open window performed. */
    void onShow();

    /** Return selected user's property. */
    String getSelectedProperty();

    /** Get property depth. */
    Depth getDepth();

    /** Get property value. */
    String getPropertyValue();

    /** Return true if user selected property edit. */
    boolean isEditPropertySelected();

    /** Return true if user selected property delete. */
    boolean isDeletePropertySelected();

    /** Return true if user selected forcing. */
    boolean isForceSelected();
}
