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
package org.eclipse.che.ide.ext.datasource.client.sqleditor.codeassist;

import org.eclipse.che.ide.util.AbstractTrie;

import java.util.Arrays;
import java.util.List;

public class SqlCodeTemplateTrie {
    private static final List<String> ELEMENTS = Arrays.asList(
            "SELECT * FROM aTable;",//
            "SELECT * FROM aTable INNER JOIN anotherTable ON aTable.id = anotherTable.id;",//
            "SELECT * FROM aTable WHERE column = 'value';",//
            "SELECT COUNT(1) FROM aTable;",//
            "INSERT INTO aTable (column1, column2) VALUES ('value1', 0);",//
            "DELETE FROM aTable WHERE column = 'value';",//
            "UPDATE aTable SET column2 = 30 WHERE column1 = 'value1';",//
            "SELECT * FROM aTable WHERE column LIKE '%rg%';",//
            "CREATE TABLE aTable (IntColumn int, VarcharColumn1 varchar(255), VarcharColumn2 varchar(255));"
                                                          );

    private static final AbstractTrie<SqlCodeCompletionProposal> sqlCodeTrie = createTrie();

    private static AbstractTrie<SqlCodeCompletionProposal> createTrie() {
        AbstractTrie<SqlCodeCompletionProposal> result = new AbstractTrie<>();
        for (String name : ELEMENTS) {
            result.put(name.toLowerCase(), new SqlCodeCompletionProposal(name));
        }
        return result;
    }

    public static List<SqlCodeCompletionProposal> findAndFilterAutocompletions(SqlCodeQuery query) {
        String prefix = query.getLastQueryPrefix();
        // remove leading trailing space
        prefix = prefix.replaceAll("^\\s*", "").toLowerCase();

        return sqlCodeTrie.search(prefix);
    }
}
