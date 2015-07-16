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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.mvp.View;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The visual part of recipe that has an ability to show script of a recipe.
 *
 * @author Valeriy Svydenko
 */
@ImplementedBy(RecipeEditorViewImpl.class)
public interface RecipeEditorView extends View<RecipeEditorView.ActionDelegate> {
    /**
     * Sets URL of the recipe's script.
     *
     * @param url
     *         script's url
     */
    void setScriptUrl(@Nonnull String url);

    /**
     * Sets tags of the recipe which used for recipes search.
     *
     * @param tags
     *         list of tags
     */
    void setTags(@Nonnull List<String> tags);

    /** Returns script's url */
    @Nonnull
    String getScriptUrl();

    /** Returns tags which used for recipes search */
    @Nonnull
    List<String> getTags();

    /**
     * Changes enable state of 'Save' button.
     *
     * @param enable
     *         enable state of button
     */
    void setEnableSaveButton(boolean enable);

    /**
     * Changes enable state of 'Cancel' button.
     *
     * @param enable
     *         enable state of button
     */
    void setEnableCancelButton(boolean enable);

    /**
     * Changes enable state of 'Delete' button.
     *
     * @param enable
     *         enable state of button
     */
    void setEnableDeleteButton(boolean enable);

    /**
     * Sets visibility of the button save.
     *
     * @param visible
     *         state button visibility
     */
    void setVisibleSaveButton(boolean visible);

    /**
     * Sets visibility of the button delete.
     *
     * @param visible
     *         state button visibility
     */
    void setVisibleDeleteButton(boolean visible);

    /**
     * Sets visibility of the button cancel.
     *
     * @param visible
     *         state button visibility
     */
    void setVisibleCancelButton(boolean visible);

    /**
     * Show a given editor in the special place on the container.
     *
     * @param editor
     *         editor that needs to be shown
     */
    void showEditor(@Nonnull EditorPartPresenter editor);

    /** Hides panel with property buttons. */
    void hideButtonsPanel();

    interface ActionDelegate {
        void showEditor();

        /** Performs some actions in response to user's clicking on the 'Create' button. */
        void onCreateButtonClicked();

        /** Performs some actions in response to user's clicking on the 'Save' button. */
        void onSaveButtonClicked();

        /** Performs some actions in response to user's clicking on the 'Delete' button. */
        void onDeleteButtonClicked();

        /** Performs some actions in response to user's clicking on the 'Cancel' button. */
        void onCancelButtonClicked();
    }

}