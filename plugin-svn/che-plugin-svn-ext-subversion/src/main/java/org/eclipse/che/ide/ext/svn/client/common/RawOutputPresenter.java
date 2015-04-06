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
package org.eclipse.che.ide.ext.svn.client.common;

import org.eclipse.che.ide.api.parts.ConsolePart;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.workspace.WorkBenchPartControllerImpl;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Presenter for the {@link RawOutputView}.
 */
@Singleton
public class RawOutputPresenter extends BasePresenter implements RawOutputView.ActionDelegate, ConsolePart {

    private final RawOutputView view;
    private final String title;

    /**
     * Constructor.
     */
    @Inject
    public RawOutputPresenter(final SubversionExtensionLocalizationConstants constants, final RawOutputView view) {
        this.title = constants.subversionLabel();
        this.view = view;

        this.view.setTitle(title);
        this.view.setDelegate(this);
    }

    @Override
    public void onClearClicked() {
        clear();
    }

    @Override
    public void onOpen() {
        super.onOpen();

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                view.scrollBottom();

                return false;
            }
        }, WorkBenchPartControllerImpl.DURATION);
    }

    @Override
    public void print(@NotNull final String text) {
        final String[] lines = text.split("\n");

        for (final String line : lines) {
            view.print(line.isEmpty() ? " " : line);

        }

        performPostOutputActions();
    }

    public void print(String text, String color) {
        final String[] lines = text.split("\n");

        for (final String line : lines) {
            view.print(line.isEmpty() ? " " : line, color);
        }

        performPostOutputActions();
    }

    @Override
    public void displayException(final Exception e) { }

//    public void printBold(final String text) {
//        view.printBold(text);
//        performPostOutputActions();
//    }

    @Override
    public void printInfo(final String text) {
        view.printInfo(text);
        performPostOutputActions();
    }

    @Override
    public void printError(final String text) {
        view.print(text);
        performPostOutputActions();
    }

    @Override
    public void printWarn(final String text) {
        view.print(text);
        performPostOutputActions();
    }

    @Override
    public void clear() {
        view.clear();
    }

    @Override
    public String getTitle() { return title; }

    @Nullable
    @Override
    public ImageResource getTitleImage() { return null; }

    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    @Nullable
    @Override
    public String getTitleToolTip() {
        return "Displays Subversion command output";
    }

    @Override
    public void go(final AcceptsOneWidget container) {
        container.setWidget(this.view);
    }

    private void performPostOutputActions() {
        final PartPresenter activePart = partStack.getActivePart();

        if (activePart == null || !activePart.equals(this)) {
            partStack.setActivePart(this);
        }

        view.scrollBottom();
    }

}
