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
package org.eclipse.che.ide.ext.svn.client.common.threechoices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.assistedinject.Assisted;

public interface ChoiceDialogFactory {
    /**
     * Create a choice dialog with only text as content.
     *
     * @param title
     *         the window title
     * @param content
     *         the window content/text
     * @param firstChoiceLabel
     *         the label for the first choice
     * @param secondChoiceLabel
     *         the label for the second choice
     * @param firstChoiceCallback
     *         the callback used on fist choice
     * @param secondChoiceCallback
     *         the callback used on second choice
     * @return a {@link ConfirmDialog} instance
     */
    ChoiceDialog createChoiceDialog(@Nonnull @Assisted("title") String title,
                                     @Nonnull @Assisted("message") String content,
                                     @Nonnull @Assisted("firstChoice") String firstChoiceLabel,
                                     @Nonnull @Assisted("secondChoice") String secondChoiceLabel,
                                    @Nullable @Assisted("firstCallback") ConfirmCallback firstChoiceCallback,
                                    @Nullable @Assisted("secondCallback") ConfirmCallback secondChoiceCallback);

    /**
     * Create a choice dialog with a widget as content.
     *
     * @param title
     *         the window title
     * @param content
     *         the window content
     * @param firstChoiceLabel
     *         the label for the first choice
     * @param secondChoiceLabel
     *         the label for the first choice
     * @param firstChoiceCallback
     *         the callback used on fist choice
     * @param secondChoiceCallback
     *         the callback used on second choice
     * @return a {@link ConfirmDialog} instance
     */
    ChoiceDialog createChoiceDialog(@Nonnull @Assisted("title") String title,
                                     @Nonnull IsWidget content,
                                     @Nonnull @Assisted("firstChoice") String firstChoiceLabel,
                                     @Nonnull @Assisted("secondChoice") String secondChoiceLabel,
                                    @Nullable @Assisted("firstCallback") ConfirmCallback firstChoiceCallback,
                                    @Nullable @Assisted("secondCallback") ConfirmCallback secondChoiceCallback);
    
    /**
     * Create a choice dialog with only text as content.
     *
     * @param title
     *         the window title
     * @param content
     *         the window content/text
     * @param firstChoiceLabel
     *         the label for the first choice
     * @param secondChoiceLabel
     *         the label for the second choice
     * @param thirdChoiceLabel
     *         the label for the third choice
     * @param firstChoiceCallback
     *         the callback used on fist choice
     * @param secondChoiceCallback
     *         the callback used on second choice
     * @param thirdChoiceCallback
     *         the callback used on third choice
     * @return a {@link ConfirmDialog} instance
     */
    ChoiceDialog createChoiceDialog(@Nonnull @Assisted("title") String title,
                                     @Nonnull @Assisted("message") String content,
                                     @Nonnull @Assisted("firstChoice") String firstChoiceLabel,
                                     @Nonnull @Assisted("secondChoice") String secondChoiceLabel,
                                     @Nonnull @Assisted("thirdChoice") String thirdChoiceLabel,
                                    @Nullable @Assisted("firstCallback") ConfirmCallback firstChoiceCallback,
                                    @Nullable @Assisted("secondCallback") ConfirmCallback secondChoiceCallback,
                                    @Nullable @Assisted("thirdCallback") ConfirmCallback thirdChoiceCallback);
    
    /**
     * Create a choice dialog with a widget as content.
     *
     * @param title
     *         the window title
     * @param content
     *         the window content
     * @param firstChoiceLabel
     *         the label for the first choice
     * @param secondChoiceLabel
     *         the label for the second choice
     * @param thirdChoiceLabel
     *         the label for the third choice
     * @param firstChoiceCallback
     *         the callback used on fist choice
     * @param secondChoiceCallback
     *         the callback used on second choice
     * @param thirdChoiceCallback
     *         the callback used on third choice
     * @return a {@link ConfirmDialog} instance
     */
    ChoiceDialog createChoiceDialog(@Nonnull @Assisted("title") String title,
                                    @Nonnull @Assisted IsWidget content,
                                    @Nonnull @Assisted("firstChoice") String firstChoiceLabel,
                                    @Nonnull @Assisted("secondChoice") String secondChoiceLabel,
                                    @Nonnull @Assisted("thirdChoice") String thirdChoiceLabel,
                                    @Nullable @Assisted("firstCallback") ConfirmCallback firstChoiceCallback,
                                    @Nullable @Assisted("secondCallback") ConfirmCallback secondChoiceCallback,
                                    @Nullable @Assisted("thirdCallback") ConfirmCallback thirdChoiceCallback);
}
