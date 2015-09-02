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
package org.eclipse.ltk.internal.ui.refactoring;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;

import java.util.List;

public final class TextEditGroupNode extends TextEditChangeNode.ChildNode {

	private TextEditBasedChangeGroup fChangeGroup;

	public TextEditGroupNode(PreviewNode parent, TextEditBasedChangeGroup changeGroup) {
		super(parent);
		fChangeGroup = changeGroup;
		Assert.isNotNull(fChangeGroup);
	}

	/**
	 * Returns the <code>TextEditBasedChangeGroup</code> managed by this node.
	 *
	 * @return the <code>TextEditBasedChangeGroup</code>
	 */
	TextEditBasedChangeGroup getChangeGroup() {
		return fChangeGroup;
	}

	public String getText() {
		return fChangeGroup.getName();
	}

	public ImageDescriptor getImageDescriptor() {
		return RefactoringPluginImages.DESC_OBJS_TEXT_EDIT;
	}

//	ChangePreviewViewerDescriptor getChangePreviewViewerDescriptor() throws CoreException {
//		InternalTextEditChangeNode element = getTextEditChangeNode();
//		if (element == null)
//			return null;
//		return element.getChangePreviewViewerDescriptor();
//	}
//
//	void feedInput(IChangePreviewViewer viewer, List categories) throws CoreException {
//		InternalTextEditChangeNode element = getTextEditChangeNode();
//		if (element != null) {
//			Change change = element.getChange();
//			if (change instanceof TextEditBasedChange) {
//				IRegion range = getTextRange(this);
//				ChangePreviewViewerInput input = null;
//				if (range != null) {
//					input = TextEditChangePreviewViewer.createInput(change, new TextEditBasedChangeGroup[]{fChangeGroup}, range);
//				} else {
//					input = TextEditChangePreviewViewer.createInput(change, fChangeGroup, 2);
//				}
//				viewer.setInput(input);
//			}
//		} else {
//			viewer.setInput(null);
//		}
//	}

	public void setEnabled(boolean enabled) {
		fChangeGroup.setEnabled(enabled);
	}

	public void setEnabledShallow(boolean enabled) {
		fChangeGroup.setEnabled(enabled);
	}

	public int getActive() {
		return fChangeGroup.isEnabled() ? PreviewNode.ACTIVE : PreviewNode.INACTIVE;
	}

	public PreviewNode[] getChildren() {
		return PreviewNode.EMPTY_CHILDREN;
	}

	boolean hasOneGroupCategory(List categories) {
		return fChangeGroup.getGroupCategorySet().containsOneCategory(categories);
	}

	public boolean hasDerived() {
		return false;
	}

	GroupCategorySet getGroupCategorySet() {
		return fChangeGroup.getGroupCategorySet();
	}

	private static IRegion getTextRange(PreviewNode element) throws CoreException {
		if (element == null)
			return null;
		if (element instanceof InternalLanguageElementNode) {
			return ((InternalLanguageElementNode)element).getTextRange();
		} else if (element instanceof TextEditChangeNode) {
			return null;
		}
		return getTextRange(element.getParent());
	}
}
