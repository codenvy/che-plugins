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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.java.client.refactoring.RefactorInfo;
import org.eclipse.che.ide.ext.java.client.refactoring.move.wizard.MovePresenter;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
public class PreviewPresenter implements PreviewView.ActionDelegate {

    private final PreviewView             view;
    private final Provider<MovePresenter> movePresenterProvider;

    private RefactorInfo refactorInfo;

    @Inject
    public PreviewPresenter(PreviewView view, Provider<MovePresenter> movePresenterProvider) {
        this.view = view;
        this.view.setDelegate(this);

        this.movePresenterProvider = movePresenterProvider;
    }

    public void show(String refactoringSessionId, RefactorInfo refactorInfo) {
        this.refactorInfo = refactorInfo;

        view.show();
    }

    @Override
    public void onAcceptButtonClicked() {

    }

    @Override
    public void onBackButtonClicked() {
        MovePresenter movePresenter = movePresenterProvider.get();

        movePresenter.show(refactorInfo);

        view.hide();
    }
}
