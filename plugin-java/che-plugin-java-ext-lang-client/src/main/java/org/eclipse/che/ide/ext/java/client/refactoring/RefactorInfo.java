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
package org.eclipse.che.ide.ext.java.client.refactoring;

import org.eclipse.che.ide.ext.java.client.refactoring.move.MoveType;
import org.eclipse.che.ide.ext.java.client.refactoring.move.MovedItemType;

import java.util.List;

/**
 * @author Dmitry Shnurenko
 */
public class RefactorInfo {

    private final MoveType      moveType;
    private final MovedItemType movedItemType;
    private final List<?>       selectedItems;

    public static RefactorInfo of(MoveType moveType, MovedItemType movedItemType, List<?> selectedItems) {
        return new RefactorInfo(moveType, movedItemType, selectedItems);
    }

    private RefactorInfo(MoveType moveType, MovedItemType movedItemType, List<?> selectedItems) {
        this.moveType = moveType;
        this.movedItemType = movedItemType;
        this.selectedItems = selectedItems;
    }

    public List<?> getSelectedItems() {
        return selectedItems;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public MovedItemType getMovedItemType() {
        return movedItemType;
    }
}
