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
package org.eclipse.che.ide.extension.machine.client.command;

import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.eclipse.che.ide.api.mvp.Presenter;

import javax.annotation.Nonnull;

/**
 * Page allows to configure specific command parameters.
 *
 * @param <T>
 *         type of the command configuration which this page should edit
 * @author Artem Zatsarynnyy
 */
public interface ConfigurationPage<T extends CommandConfiguration> extends Presenter {

    /**
     * Resets the page from the given {@code configuration}.
     * <p/>
     * This method is called every time when user selects
     * an appropriate command configuration in 'Command Configuration'
     * dialog and before actual displaying this page.
     */
    void resetFrom(@Nonnull T configuration);

    /**
     * This method is called every time when user selects an appropriate
     * command configuration in 'Command Configuration' dialog.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    void go(final AcceptsOneWidget container);
}
