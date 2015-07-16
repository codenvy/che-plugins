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
package org.eclipse.che.ide.ext.java.worker;

import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.SelectionEngine;
import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionRequestor;
import org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult;
import org.eclipse.che.ide.ext.java.messages.ComputeJavadocHandle;
import org.eclipse.che.ide.ext.java.messages.JavadocHandleComputed;
import com.google.gwt.webworker.client.messages.MessageFilter;

/**
 * @author Evgen Vidolob
 */
public class WorkerJavadocHandleComputer implements MessageFilter.MessageRecipient<ComputeJavadocHandle> {


    private JavaParserWorker worker;
    private WorkerCuCache    cuCache;

    public WorkerJavadocHandleComputer(JavaParserWorker worker, WorkerCuCache cuCache) {
        this.worker = worker;
        this.cuCache = cuCache;
    }


    @Override
    public void onMessageReceived(ComputeJavadocHandle message) {
        String filePath = message.getFilePath();
        CompilationUnit cu = cuCache.getCompilationUnit(filePath);
        String source = cuCache.getSource(filePath);

        SelectionResult result;
        if (cu == null || source == null) {
            result = null;
        } else {
            SelectionRequestor requestor = new SelectionRequestor(cu, source);
            SelectionEngine selectionEngine = new SelectionEngine(WorkerMessageHandler.get().getNameEnvironment(), requestor, WorkerMessageHandler.get().getOptions());

            String fileName = filePath.substring(
                    filePath.lastIndexOf("/"), filePath.lastIndexOf('.'));
            selectionEngine.select(new org.eclipse.che.ide.ext.java.jdt.compiler.batch.CompilationUnit(source.toCharArray(), fileName, "UTF-8"), message.getOffset(), 0);
            result = requestor.getSelectionResult();
        }

        JavadocHandleComputed respMessage = JavadocHandleComputed.make();
        if(result != null){
           respMessage.setId(message.id()).setKey(result.getKey()).setFqn(result.getFqn()).setOffset(result.getOffset()).setSource(result.isSource()).setDeclaration(result.isDeclaration());
        }
        worker.sendMessage(respMessage.serialize());
    }
}
