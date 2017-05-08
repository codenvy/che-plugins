/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.client.event;

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeInfo;

import java.util.List;

/**
 * Event that notifies of refactoring provided
 *
 * @author Igor Vinokur
 */
public class RefactoringProvidedEvent extends GwtEvent<RefactoringProvidedEventHandler> {

    public static final Type<RefactoringProvidedEventHandler> TYPE = new Type<>();

    private final List<ChangeInfo> refactoringResult;

    public RefactoringProvidedEvent(List<ChangeInfo> refactoringResult) {
        this.refactoringResult = refactoringResult;
    }

    /**
     * Returns refactoring result that contains old and new file path names
     */
    public List<ChangeInfo> getRefactoringResult() {
        return refactoringResult;
    }

    @Override
    public Type<RefactoringProvidedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(RefactoringProvidedEventHandler handler) {
        handler.onRefactoringProvided(this);
    }
}
