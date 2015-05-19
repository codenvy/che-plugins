/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.codemirror.client.minimap;

/**
 * Interface for the view component of the minimap.
 */
public interface MinimapView {

    /**
     * Sets the action delegate.
     * 
     * @param delegate the new value
     */
    void setDelegate(Delegate delegate);

    void addMark(double ratio, String style, int offset, String title);

    void addMark(double relativePos, String style, int offset, Integer level, String title);

    void clearMarks();

    void removeMarks(int lineStart, int lineEnd);

    /**
     * Action delegate for the view.
     */
    public interface Delegate {

        /**
         * Handles a click on the minimap.
         * 
         * @param verticalPosition the position of the click on the map, between 0 and 1
         */
        void handleClick(double verticalPosition);

        void handleMarkClick(int offset);
    }
}
