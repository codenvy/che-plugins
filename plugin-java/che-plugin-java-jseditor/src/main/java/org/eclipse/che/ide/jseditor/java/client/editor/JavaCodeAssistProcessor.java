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
package org.eclipse.che.ide.jseditor.java.client.editor;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.editor.JavaCodeAssistClient;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.shared.dto.ProposalPresentation;
import org.eclipse.che.ide.ext.java.shared.dto.Proposals;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaCodeAssistProcessor implements CodeAssistProcessor {

    private static Map<String, ImageResource> images;

    private final EditorPartPresenter    editor;
    private       JavaCodeAssistClient   client;
    private final JavaResources          javaResources;
    private       DtoUnmarshallerFactory unmarshallerFactory;
    private final AnalyticsEventLogger eventLogger;


    private String errorMessage;

    @AssistedInject
    public JavaCodeAssistProcessor(@Assisted final EditorPartPresenter editor,
                                   final JavaCodeAssistClient client,
                                   final JavaResources javaResources,
                                   DtoUnmarshallerFactory unmarshallerFactory,
                                   final AnalyticsEventLogger eventLogger) {
        this.editor = editor;
        this.client = client;
        this.javaResources = javaResources;
        this.unmarshallerFactory = unmarshallerFactory;
        this.eventLogger = eventLogger;
        if (images == null) {
            initImages(javaResources);
        }
    }

    private void initImages(JavaResources resources) {
        images = new HashMap<>();
        images.put("template", resources.template());
        images.put("javadoc", resources.javadoc());
        images.put("annotation", resources.annotationItem());
        //todo create images for annotations
        images.put("privateAnnotation", resources.annotationItem());
        images.put("protectedAnnotation", resources.annotationItem());
        images.put("defaultAnnotation", resources.annotationItem());

        images.put("enum", resources.enumItem());
        images.put("defaultEnum", resources.enumItem());
        images.put("privateEnum", resources.enumItem());
        images.put("protectedEnum", resources.enumItem());

        images.put("interface", resources.interfaceItem());
        images.put("defaultInterface", resources.interfaceItem());
        images.put("innerInterfacePublic", resources.interfaceItem());
        images.put("innerInterfacePrivate", resources.interfaceItem());
        images.put("innerInterfaceProtected", resources.interfaceItem());

        images.put("class", resources.classItem());
        images.put("defaultClass", resources.classDefaultItem());
        images.put("innerClassPrivate", resources.classItem());
        images.put("innerClassProtected", resources.classItem());
        images.put("innerClassDefault", resources.classItem());

        images.put("privateMethod", resources.privateMethod());
        images.put("publicMethod", resources.publicMethod());
        images.put("protectedMethod", resources.protectedMethod());
        images.put("defaultMethod", resources.defaultMethod());

        images.put("publicField", resources.publicField());
        images.put("protectedField", resources.protectedField());
        images.put("privateField", resources.privateField());
        images.put("defaultField", resources.defaultField());

        images.put("localVariable", resources.local_var());
        images.put("package", resources.packageItem());

        images.put("correctionLocal", resources.correction_change()); // ????
        images.put("correctionChange", resources.correction_change());
        images.put("correctionAdd", resources.correction_change()); //????
        images.put("jexception", resources.exceptionProp());
        images.put("correctionRemove", resources.remove_correction());
        images.put("correctionCast", resources.correction_cast());
        images.put("correctionMove", resources.remove_correction()); // ????
        images.put("correctionDeleteImport", resources.correction_delete_import());
        images.put("correctionRename", resources.breakpointCurrent()); //????
        images.put("impObj", resources.add_obj()); //????
        images.put("toolDelete", resources.delete_obj()); //????


    }

    public static String insertStyle(final JavaResources javaResources, final String display) {
        if (display.contains("#FQN#")) {
            return display.replace("#FQN#", javaResources.css().fqnStyle());
        } else if (display.contains("#COUNTER#")) {
            return display.replace("#COUNTER#", javaResources.css().counter());
        } else {
            return display;
        }
    }

    public static ImageResource getImage(final JavaResources javaResources, final String image) {
        if (image == null) {
            return null;
        }
        return images.get(image);
    }

    @Override
    public void computeCompletionProposals(final TextEditor textEditor, final int offset,
                                           final CodeAssistCallback callback) {
//        if (buildContext.isBuilding()) {
//            errorMessage = "Code Assistant currently unavailable due to project build.";
//        } else {
//            errorMessage = null;
//        }
        if (errorMessage != null) {
            return;
        }
        this.eventLogger.log(this, "Autocompleting");
        final VirtualFile file = editor.getEditorInput().getFile();
        final String projectPath = file.getProject().getPath();
        String fqn = JavaSourceFolderUtil.getFQNForFile(file);
        Unmarshallable<Proposals> unmarshaller = unmarshallerFactory.newUnmarshaller(Proposals.class);
        client.computeProposals(projectPath, fqn, offset, textEditor.getDocument().getContents(), new AsyncRequestCallback<Proposals>(unmarshaller) {
            @Override
            protected void onSuccess(Proposals proposals) {
                showProposals(callback, proposals);
            }

            @Override
            protected void onFailure(Throwable throwable) {
                Log.error(JavaCodeAssistProcessor.class, throwable);
                com.google.gwt.user.client.Window.alert(throwable.getMessage());
            }
        });
    }

    private void showProposals(final CodeAssistCallback callback, final Proposals respons) {
        List<ProposalPresentation> presentations = respons.getProposals();
        final List<CompletionProposal> proposals = new ArrayList<>(presentations.size());
        for (final ProposalPresentation proposal : presentations) {
            final CompletionProposal completionProposal =
                                                          new JavaCompletionProposal(
                                                                                     proposal.getIndex(),
                                                                                     insertStyle(javaResources, proposal.getDisplayString()),
                                                                                     new Icon("", getImage(javaResources, proposal.getImage())),
                                                                                     client, respons.getSessionId());
            proposals.add(completionProposal);
        }

        callback.proposalComputed(proposals);
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void disableCodeAssistant() {
        this.errorMessage = "Code Assistant currently unavailable due to file parsing. Try again in a moment.";
    }

    public void enableCodeAssistant() {
        this.errorMessage = null;
    }
}
