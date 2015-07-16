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
package org.eclipse.che.ide.ext.java.client.editor;

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.collections.js.JsoStringMap;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.IProblem;
import org.eclipse.che.ide.ext.java.jdt.text.edits.TextEdit;
import org.eclipse.che.ide.ext.java.messages.JavadocHandleComputed;
import org.eclipse.che.ide.ext.java.messages.ProblemLocationMessage;
import org.eclipse.che.ide.ext.java.messages.ProposalAppliedMessage;
import org.eclipse.che.ide.ext.java.messages.WorkerProposal;
import org.eclipse.che.ide.ext.java.messages.impl.WorkerCodeBlock;

/**
 * @author Evgen Vidolob
 */
public interface JavaParserWorker {

    void dependenciesUpdated();

    void parse(String content, String fileName, String filePath, String packageName, String projectPath, boolean needParseMethodBody,
               WorkerCallback<IProblem> callback);

    void computeCAProposals(String content, int offset, String fileName, String projectPath, String filePath,
                            WorkerCallback<WorkerProposal> callback);

    void applyCAProposal(String id, Callback<ProposalAppliedMessage> callback);

    void addOutlineUpdateHandler(String filePath, WorkerCallback<WorkerCodeBlock> callback);

    void computeQAProposals(String content, int offset, int selectionLength, boolean updatedContent,
                            JsoArray<ProblemLocationMessage> problems,
                            String filePath, WorkerCallback<WorkerProposal> callback);

    void removeFqnFromCache(String fqn);

    void format(int offset, int length, String content, Callback<TextEdit> callback);

    void preferenceFormatSettings(JsoStringMap<String> settings);

    void computeJavadocHandle(int offset, String filePath, Callback<JavadocHandleComputed> callback);

    void fileClosed(String path);

    public interface WorkerCallback<T> {
        void onResult(Array<T> problems);
    }

    public interface Callback<T>{
        void onCallback(T result);
    }

}
