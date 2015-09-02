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

package org.eclipse.che.jdt.refactoring;

import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.internal.ui.refactoring.PreviewNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DtoConverter {

    public static RefactoringStatus toRefactoringStatusDto(org.eclipse.ltk.core.refactoring.RefactoringStatus refactoringStatus) {
        RefactoringStatus status = DtoFactory.newDto(RefactoringStatus.class);
        status.setSeverity(refactoringStatus.getSeverity());
        List<RefactoringStatusEntry> entryList = Arrays.stream(refactoringStatus.getEntries()).map(refactoringStatusEntry -> {
            RefactoringStatusEntry entry = DtoFactory.newDto(RefactoringStatusEntry.class);
            entry.setSeverity(refactoringStatusEntry.getSeverity());
            entry.setMessage(refactoringStatusEntry.getMessage());
            return entry;
        }).collect(Collectors.toList());
        status.setEntries(entryList);
        return status;
    }

    public static RefactoringPreview toRefactoringPreview(PreviewNode node) {
        RefactoringPreview dto = DtoFactory.newDto(RefactoringPreview.class);
        dto.setText(node.getText());
        dto.setImage(node.getImageDescriptor().getImage());
        dto.setEnabled(true);
        PreviewNode[] children = node.getChildren();
        if(children != null && children.length > 0) {
            List<RefactoringPreview> list = new ArrayList<>(children.length);
            for (PreviewNode child : children) {
                list.add(toRefactoringPreview(child));
            }
            dto.setChildrens(list);
        }
        return dto;
    }
}