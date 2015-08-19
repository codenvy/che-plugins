/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.angularjs.core.client.javascript;

import com.google.gwt.core.client.JsArray;

import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.web.js.editor.JsCodeAssistProcessor;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.util.AbstractTrie;
import org.eclipse.che.plugin.angularjs.completion.dto.Method;
import org.eclipse.che.plugin.angularjs.completion.dto.Param;
import org.eclipse.che.plugin.angularjs.completion.dto.TemplateDotProvider;
import org.eclipse.che.plugin.angularjs.completion.dto.Templating;
import org.eclipse.che.plugin.angularjs.core.client.javascript.contentassist.ContextFactory;
import org.eclipse.che.plugin.angularjs.core.client.javascript.contentassist.IContentAssistProvider;
import org.eclipse.che.plugin.angularjs.core.client.javascript.contentassist.IContext;
import org.eclipse.che.plugin.angularjs.core.client.javascript.contentassist.JSNIContextFactory;
import org.eclipse.che.plugin.angularjs.core.client.javascript.contentassist.JsProposal;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Florent Benoit
 */
public class JavaScriptCodeAssistProcessor implements JsCodeAssistProcessor {

    protected static final char ACTIVATION_CHARACTER = '.';

    private native IContentAssistProvider getNativeProvider()/*-{
                                                             return $wnd.jsEsprimaContentAssistProvider;
                                                             }-*/;

    private IContentAssistProvider provider;

    private Templating templating;

    private AbstractTrie<TemplateDotProvider> trie;

    @Inject
    private DtoFactory dtoFactory;

    private JavaScriptResources javaScriptResources;

    private ContextFactory contextFactory;

    /**
     * Sets the javascript resources Also if resources is injected it means we're in GWT so initialize()
     * 
     * @param javaScriptResources
     */
    @Inject
    public void setJavaScriptResources(JavaScriptResources javaScriptResources) {
        this.javaScriptResources = javaScriptResources;

        // Initialize
        init();
    }


    protected void init() {
        setProvider(getNativeProvider());
        setTemplating(dtoFactory.createDtoFromJson(javaScriptResources.completionTemplatingJson().getText(), Templating.class));

        // build trie
        buildTrie();

        // set context factory using JSNI
        setContextFactory(new JSNIContextFactory());
    }

    protected void setTemplating(Templating templating) {
        this.templating = templating;
    }

    protected void setProvider(IContentAssistProvider provider) {
        this.provider = provider;
    }

    public void setContextFactory(ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }


    /**
     * Complete will all stuff except directives for now
     */
    protected void buildTrie() {
        this.trie = new AbstractTrie<>();
        for (TemplateDotProvider provider : templating.getTemplateDotProviders()) {
            if ("DIRECTIVE".equals(provider.getType())) {
                continue;
            }
            trie.put(provider.getName(), provider);
        }
    }

    @Override
    public void computeCompletionProposals(TextEditor textEditor, int offset, CodeAssistCallback codeAssistCallback) {
        IContext context = contextFactory.create();
        String prefix = computePrefix(textEditor.getDocument(), offset);
        context.setPrefix(prefix);
        List<CompletionProposal> prop = new ArrayList<>();


        String templatePrefix = computeTemplatePrefix(textEditor.getDocument(), offset);

        int dot = 0;
        int lastDot = Integer.MAX_VALUE;


        if (templatePrefix.length() > 0) {
            dot = templatePrefix.indexOf('.');
            lastDot = templatePrefix.lastIndexOf('.');
        }


        if (dot != -1 && dot == lastDot) {
            // get the current template provider
            String prefixVal = templatePrefix.substring(0, dot).trim();
            String suffixVal = templatePrefix.substring(dot + 1, templatePrefix.length());
            AbstractTrie<String> subTrie = new AbstractTrie<>();

            for (TemplateDotProvider provider : templating.getTemplateDotProviders()) {
                if (prefixVal.equals(provider.getName())) {
                    // add all methods
                    List<Method> methods = provider.getMethods();
                    if (methods != null) {
                        for (Method m : methods) {
                            String fullName = m.getName();
                            if (m.getParams() != null && m.getParams().size() > 0) {
                                fullName = fullName.concat("(");
                                int i = 1;
                                for (Param param : m.getParams()) {
                                    fullName = fullName.concat(param.getName());
                                    if (i < m.getParams().size()) {
                                        fullName = fullName.concat(",");
                                    }
                                    i++;
                                }
                                fullName = fullName.concat(")");
                            } else {
                                fullName = fullName.concat("()");
                            }
                            subTrie.put(m.getName(), fullName);
                        }
                    }
                }
            }
            List<String> result = subTrie.search(suffixVal);
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
            for (String st : result) {
                TemplateProposal templateProposal =
                                                    new TemplateProposal(templatePrefix, st, prefixVal.concat(".").concat(st), offset,
                                                                         javaScriptResources);
                templateProposal.setMethod();
                prop.add(templateProposal);
            }
        } else if (dot == -1) {
            // Perform completion only if there is no dot
            List<TemplateDotProvider> result = trie.search(prefix);
            Collections.sort(result, new Comparator<TemplateDotProvider>() {
                @Override
                public int compare(TemplateDotProvider o1, TemplateDotProvider o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (TemplateDotProvider provider : result) {
                prop.add(new TemplateProposal(templatePrefix, provider.getName(), provider.getName(), offset, javaScriptResources));
            }


        }


        try {
            JsArray<JsProposal> jsProposals = provider.computeProposals(textEditor.getDocument().getContents(), offset, context);
            if (jsProposals != null && jsProposals.length() != 0) {
                for (int i = 0; i < jsProposals.length(); i++) {
                    JsProposal jsProposal = jsProposals.get(i);
                    CompletionProposal proposal = new JavaScriptProposal(prefix, jsProposal, offset, javaScriptResources);
                    prop.add(proposal);
                }
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        List<CompletionProposal> proposals = new ArrayList<>();
        for (int i = 0; i < prop.size(); i++) {
            proposals.add(prop.get(i));
        }

        codeAssistCallback.proposalComputed(proposals);

    }

    /**
     * @param document
     * @param offset
     * @return
     */
    private String computeTemplatePrefix(EmbeddedDocument document, int offset) {
        final TextPosition textPosition = document.getPositionFromIndex(offset);
        final String line = document.getLineContent(textPosition.getLine());
        final LinearRange lineRange = document.getLinearRangeForLine(textPosition.getLine());
        return line.substring(0, offset - lineRange.getStartOffset());
    }


    /**
     * @param document
     * @param offset
     * @return
     */
    private String computePrefix(EmbeddedDocument document, int offset) {
        final TextPosition textPosition = document.getPositionFromIndex(offset);
        final String line = document.getLineContent(textPosition.getLine());
        final LinearRange lineRange = document.getLinearRangeForLine(textPosition.getLine());
        String partLine = line.substring(0, offset - lineRange.getStartOffset());
        for (int i = partLine.length() - 1; i >= 0; i--) {
            switch (partLine.charAt(i)) {
                case '.':
                    break;
                case ' ':
                case '(':
                case ')':
                case '{':
                case '}':
                case ';':
                case '[':
                case ']':
                case '"':
                case '\'':
                    return partLine.substring(i + 1);
                default:
                    break;
            }
        }
        return partLine;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}
