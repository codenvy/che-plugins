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
package org.eclipse.che.ide.ext.java.client.refactoring.preview;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

/**
 * @author Dmitry Shnurenko
 */
@ImplementedBy(PreviewViewImpl.class)
interface PreviewView extends View<PreviewView.ActionDelegate> {
    void hide();

    void show();

    interface ActionDelegate {
        void onAcceptButtonClicked();

        void onBackButtonClicked();
    }
}
