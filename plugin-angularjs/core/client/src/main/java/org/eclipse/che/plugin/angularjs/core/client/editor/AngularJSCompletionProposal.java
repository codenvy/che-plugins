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

import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.jseditor.client.codeassist.Completion;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.util.dom.Elements;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

import elemental.dom.Element;


/**
 * AngularJS completion proposal that is called when the user has selected a completion. It will add the property at the right place
 * 
 * @author Florent Benoit
 */
public class AngularJSCompletionProposal implements CompletionProposal {

    private static final String PROPERTY_TERMINATOR = "\"\"";
    private static final String PROPERTY_SEPARATOR = "=";

    /** The template used to format the additional info snippet. */
    private static final AdditionalInfoTemplate TEMPLATE = GWT.create(AdditionalInfoTemplate.class);

    private String name;
    private String addedString;
    private int jumpLength;
    private InvocationContext invocationContext;

    public AngularJSCompletionProposal(String name) {
        this.name = name;
    }


    @Override
    public Element getAdditionalProposalInfo() {
        // if it's an angular directive, return a link to the documentation
        if (!name.startsWith("ng-")) {
            return null;
        }
        // convert the name ng-xxxx into ngXxxx
        String directiveName = "ng".concat(name.substring(3, 4).toUpperCase()).concat(name.substring(4));
        String link = "http://docs.angularjs.org/api/ng/directive/".concat(directiveName);
        final SafeUri linkUri = UriUtils.fromSafeConstant(link);

        final Element result = Elements.createDivElement();
        result.setInnerHTML(TEMPLATE.additionalInfo(linkUri, link).asString());
        return result;
    }

    @Override
    public String getDisplayString() {
        return new SafeHtmlBuilder().appendEscaped(name).toSafeHtml().asString();
    }

    @Override
    public void getCompletion(CompletionCallback completionCallback) {
        completionCallback.onCompletion(new Completion() {
            /** {@inheritDoc} */
            @Override
            public void apply(EmbeddedDocument document) {
                computeInsertedString();
                final int start = invocationContext.getOffset() - invocationContext.getQuery().getPrefix().length();
                final int length = invocationContext.getQuery().getPrefix().length();
                document.replace(start, length, addedString);
            }

            /** {@inheritDoc} */
            @Override
            public LinearRange getSelection(EmbeddedDocument document) {
                final int start = invocationContext.getOffset() + jumpLength - invocationContext.getQuery().getPrefix().length();
                return LinearRange.createWithStart(start).andLength(0);
            }
        });
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    public void setInvocationContext(InvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    protected void computeInsertedString() {
        this.addedString = name + PROPERTY_SEPARATOR + PROPERTY_TERMINATOR;
        // input should be in the middle of quotes
        this.jumpLength = addedString.length() - 1;
    }


    /** {@inheritDoc} */
    @Override
    public Icon getIcon() {
        if (invocationContext != null) {
            return new Icon("angularjs.property", invocationContext.getResources().property());
        }
        return null;
    }

    interface AdditionalInfoTemplate extends SafeHtmlTemplates {
        @Template("Full documentation available on <a href='{0}' target='_blank'>{1}</a>")
        SafeHtml additionalInfo(SafeUri linkUri, String link);
    }
}
