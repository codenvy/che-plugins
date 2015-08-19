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

import org.eclipse.che.ide.util.AbstractTrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holder of all possible AngularJS attributes.
 *
 * @author Florent Benoit
 */
public class AngularJSTrie {
    private static final List<String> ELEMENTS = Arrays.asList(
            "ng-app",//
            "ng-bind",//
            "ng-bindHtml",//
            "ng-bindTemplate",//
            "ng-blur",//
            "ng-change",//
            "ng-checked",//
            "ng-class",//
            "ng-classEven",//
            "ng-classOdd",//
            "ng-click",//
            "ng-cloak",//
            "ng-controller",//
            "ng-copy",//
            "ng-csp",//
            "ng-cut",//
            "ng-dblclick",//
            "ng-disabled",//
            "ng-focus",//
            "ng-form",//
            "ng-hide",//
            "ng-href",//
            "ng-if",//
            "ng-include",//
            "ng-init",//
            "ng-keydown",//
            "ng-keypress",//
            "ng-keyup",//
            "ng-list",//
            "ng-model",//
            "ng-mousedown",//
            "ng-mouseenter",//
            "ng-mouseleave",//
            "ng-mousemove",//
            "ng-mouseover",//
            "ng-mouseup",//
            "ng-nonBindable",//
            "ng-open",//
            "ng-paste",//
            "ng-pluralize",//
            "ng-readonly",//
            "ng-repeat",//
            "ng-selected",//
            "ng-show",//
            "ng-src",//
            "ng-srcset",//
            "ng-style",//
            "ng-submit",//
            "ng-switch",//
            "ng-transclude",//
            "ng-value"
                                                                         );


    private static final AbstractTrie<AngularJSCompletionProposal> angularJSTrie = createTrie();


    private static AbstractTrie<AngularJSCompletionProposal> createTrie() {
        AbstractTrie<AngularJSCompletionProposal> result = new AbstractTrie<>();
        for (String name : ELEMENTS) {
            result.put(name, new AngularJSCompletionProposal(name));
        }
        return result;
    }

    /**
     * Search available completions and filter out the existing attributes name
     *
     * @param query
     *         the request query
     * @return an array of autocompletions, or an empty array if there are no
     * autocompletion proposals
     */
    public static List<AngularJSCompletionProposal> findAndFilterAutocompletions(AngularJSQuery query) {
        // use tolower case
        String prefix = query.getPrefix();

        // search attributes
        List<AngularJSCompletionProposal> searchedProposals = angularJSTrie.search(prefix);

        // Filter out the existing attributes that may be present in the HTML element
        List<AngularJSCompletionProposal> result = new ArrayList<>();
        for (AngularJSCompletionProposal proposal : searchedProposals) {
            if (!query.getExistingAttributes().contains(proposal.getName())) {
                result.add(proposal);
            }
        }
        return result;


    }
}
