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
package org.eclipse.che.plugin.angularjs.core.client.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.che.ide.ext.web.html.editor.HTMLCodeAssistProcessor;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.inject.Inject;

/**
 * CodeAssist processor will manage AngularJS directives proposals.
 * 
 * @author Florent Benoit
 */
public class AngularJSHtmlCodeAssistProcessor implements HTMLCodeAssistProcessor {

    /**
     * Space separator.
     */
    private static final RegExp REGEXP_SPACES = RegExp.compile("\\s+");

    /**
     * Property separator.
     */
    private static final RegExp REGEXP_PROPERTY = RegExp.compile("=");

    /**
     * Resources like images.
     */
    private AngularJSResources angularJSResources;

    /**
     * Build a new processor based on the given resources.
     * 
     * @param angularJSResources the AngularJS resource
     */
    @Inject
    public AngularJSHtmlCodeAssistProcessor(AngularJSResources angularJSResources) {
        this.angularJSResources = angularJSResources;
    }


    /**
     * Build a query for the given editor.
     * 
     * @param textEditor the editor containing the user selection
     * @return the query used to provide completion
     */
    protected AngularJSQuery getQuery(TextEditor textEditor) {

        // document being edited
        EmbeddedDocument document = textEditor.getDocument();

        // current cursor position
        TextPosition cursorPosition = document.getCursorPosition();


        int totalNumberOfLines = document.getLineCount();

        // search the HTML element being managed
        int line = cursorPosition.getLine();
        int column = cursorPosition.getCharacter();
        int lineWithCursor = line;

        String textBefore = "";

        boolean parsingLineWithCursor = true;

        // search the beginning of the element or stop if no element has been found
        while ((line >= 0) && (!textBefore.contains("<"))) {
            String text;
            int lastOpen;

            if (parsingLineWithCursor) {
                // start of line until column=cursor
                final String lineContent = document.getLineContent(line);
                text = lineContent.substring(0, column);
                parsingLineWithCursor = false;
            } else {
                // whole line
                text = document.getLineContent(line);
            }

            textBefore = text.concat(textBefore);
            lastOpen = text.lastIndexOf('<');

            // Element was not opened on this line, go the the previous one
            if (lastOpen == -1) {
                line--;
            }
        }

        // No completion on end elements
        if (textBefore.startsWith("</")) {
            return null;
        }

        // search the end of the HTML element (or break when number of lines is reached)
        String textAfter = "";
        line = lineWithCursor;
        while (line < totalNumberOfLines && !textAfter.contains(">")) {
            int lastOpen;
            int lastClose;

            String text;
            final String lineContent = document.getLineContent(line);
            if (parsingLineWithCursor) {
                // end of line starting at column=cursor
                text = lineContent.substring(column);
                parsingLineWithCursor = false;
            } else {
                // whole line but trimmed
                text = lineContent.trim();
            }

            String newText = textAfter + text;
            lastClose = newText.lastIndexOf('>');
            lastOpen = newText.lastIndexOf('<');

            // We have a new element and the current one is not finished
            if (lastClose != -1 && lastOpen != -1 && lastOpen < lastClose) {
                // remove the next new element which is being started
                return getQuery(textBefore, newText.substring(0, lastOpen));
            }


            // We don't have a closed element
            line++;
        }

        // Remove < and > from the beginning of the element and the end of the element
        if (textBefore.contains("<")) {
            textBefore = textBefore.substring(textBefore.indexOf('<') + 1);
        }
        if (textAfter.contains(">")) {
            textAfter = textAfter.substring(0, textAfter.indexOf('>'));
        }

        return getQuery(textBefore, textAfter);

    }

    /**
     * Gets a list of angularjs attributes from the given text
     * 
     * @param text the text
     * @param skipLast if the last attribute needs to be skipped (for example if it's the current attribute)
     * @return the list of attributes
     */
    protected List<String> getAngularAttributes(String text, boolean skipLast) {
        // Text that needs to be analyzed

        // First, split on space in order to get attributes
        SplitResult fullAttributes = REGEXP_SPACES.split(text);

        // init list
        List<String> attributesName = new ArrayList<>();

        // now, for each attribute, gets only the name of the attribute
        if (fullAttributes.length() > 0) {
            for (int i = 0; i < fullAttributes.length(); i++) {
                if (i == fullAttributes.length() - 1 && skipLast) {
                    continue;
                }


                String attribute = fullAttributes.get(i);
                // Get only the attribute name
                SplitResult attributeNameSplit = REGEXP_PROPERTY.split(attribute);

                // first part is the attribute name
                attributesName.add(attributeNameSplit.get(0));
            }
        }
        return attributesName;

    }


    /**
     * Gets query with the given text.
     * 
     * @param textBefore which is text before cursor
     * @param textAfter which is text after cursor
     * @return a new query
     */
    protected AngularJSQuery getQuery(String textBefore, String textAfter) {

        AngularJSQuery query = new AngularJSQuery();

        List<String> attributes = new ArrayList<>();
        List<String> attributesBefore = getAngularAttributes(textBefore, true);

        attributes.addAll(attributesBefore);
        attributes.addAll(getAngularAttributes(textAfter, false));

        for (String attributeName : attributes) {
            if (!"".equals(attributeName)) {
                query.getExistingAttributes().add(attributeName);
            }
        }

        // needs to find the prefix.
        // The prefix is the last text entered after a space
        if (textBefore.length() > 0) {
            SplitResult splitBefore = REGEXP_SPACES.split(textBefore);
            if (splitBefore.length() > 0) {
                query.setPrefix(splitBefore.get(splitBefore.length() - 1));
            }
        } else {
            // no prefix
            query.setPrefix("");
        }


        return query;
    }


    /**
     * Interface API for computing the code completion
     * 
     * @param textEditor the editor
     * @param offset an offset within the document for which completions should be computed
     * @param codeAssistCallback the callback used to provide code completion
     */
    @Override
    public void computeCompletionProposals(TextEditor textEditor, int offset, CodeAssistCallback codeAssistCallback) {

        // Avoid completion when text is selected
        if (textEditor.getSelectedLinearRange().getLength() > 0) {
            codeAssistCallback.proposalComputed(null);
            return;
        }


        // Get current text
        AngularJSQuery query = getQuery(textEditor);

        if (query.getPrefix() == null) {
            codeAssistCallback.proposalComputed(null);
            return;
        }

        InvocationContext invocationContext = new InvocationContext(query, offset, angularJSResources, textEditor);
        List<AngularJSCompletionProposal> completionProposals = AngularJSTrie.findAndFilterAutocompletions(query);
        codeAssistCallback.proposalComputed(jsToList(completionProposals, invocationContext));
    }

    @Override
    public String getErrorMessage() {
        return null;
    }


    /**
     * Convert Javascript array and apply invocation context
     * 
     * @param autocompletions the list of auto completion proposals
     * @param context the given invocation context
     * @return the array
     */
    private List<CompletionProposal> jsToList(List<AngularJSCompletionProposal> autocompletions,
                                              InvocationContext context) {
        final List<CompletionProposal> proposals = new ArrayList<>();
        if (autocompletions != null) {
            for (AngularJSCompletionProposal proposal : autocompletions) {
                proposals.add(proposal);
                proposal.setInvocationContext(context);
            }
        }
        return proposals;
    }
}
