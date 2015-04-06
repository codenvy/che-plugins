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
package org.eclipse.che.ide.ext.svn.client.commit;

public interface CommitView {

    void setDelegate(CommitViewDelegate delegate);

    public interface CommitViewDelegate {

        void onCancelClicked();

        void onCommitClicked();

        void onValueChanged();

    }

    String getMessage();

    void setMessage(String message);

    boolean isCommitSelection();

    void setCommitSelection(boolean commitSelection);

    void setEnableCommitButton(boolean enable);

    void setKeepLocksState(boolean keepLocks);

    boolean getKeepLocksState();

    void focusInMessageField();

    void close();

    void showDialog();
}
