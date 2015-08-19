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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.che.ide.api.text.BadLocationException;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.EditorDatasourceOracle;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.SqlEditorResources;
import org.eclipse.che.ide.ext.datasource.client.store.DatabaseInfoOracle;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.texteditor.ConfigurableTextEditor;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class SqlCodeAssistProcessor implements CodeAssistProcessor {

    private final SqlEditorResources        resources;
    private final DatabaseInfoOracle        databaseInfoOracle;
    private final ConfigurableTextEditor    textEditor;
    private final EditorDatasourceOracle    editorDatasourceOracle;

    private final static RegExp             TABLE_REGEXP_PATTERN                    =
                                                                                      RegExp.compile(".*((from((\\s+(\\w+\\.)*\\w+\\s*,)*))|insert into|alter table|update|join)\\s+(\\w*.*\\w*)$");
    private final static int                TABLE_REGEXP_GROUP                      = 6;
    private final static RegExp             COLUMN_REGEXP_PATTERN                   =
                                                                                      RegExp.compile(".*((from((\\s+((\\w+\\.)*\\w+)\\s*,)*))|insert into|alter table|update)"
                                                                                                         + "\\s+(\\w*\\.?(\\w+))\\s+((.+\\s+)*)"
                                                                                                         + "((where\\s+(.*\\s+)*|\\()(\\w*\\.*\\w*\\.*\\w+,?\\s+)*|set\\s+((.+\\s*)+,\\s*)*)(\\w*\\.*\\w*\\.*\\w*)"
                                                                                                         + "|((from((\\s+((\\w+\\.)*\\w+)\\s*,)*))\\s+(\\w*\\.?(\\w+))\\s+((\\w+\\s+)*)(join\\s+)(\\w*\\.?(\\w+))"
                                                                                                         + "(\\s+on\\s+(.*\\s+)*)(\\w*\\.*\\w*\\.*\\w*))$"
                                                                                                     , "gm");
    private final static int                COLUMN_REGEXP_GROUP                     = 17;
    private final static int                JOIN_COLUMN_REGEXP_GROUP                = 33;
    private final static int                TABLE_IN_COLUMN_REGEXP_GROUP            = 7;
    private final static int                JOIN_TABLE_IN_COLUMN_REGEXP_GROUP       = 29;
    private final static int                PREV_TABLES_IN_COLUMN_REGEXP_GROUP      = 5;
    private final static int                JOIN_PREV_TABLES_IN_COLUMN_REGEXP_GROUP = 24;
    private final static int                STATEMENT_TYPE_REGEXP_GROUP             = 2;
    private final static int                JOIN_STATEMENT_TYPE_REGEXP_GROUP        = 19;

    public SqlCodeAssistProcessor(ConfigurableTextEditor textEditor,
                                  SqlEditorResources resources,
                                  DatabaseInfoOracle databaseInfoOracle,
                                  EditorDatasourceOracle editorDatasourceOracle) {
        this.textEditor = textEditor;
        this.resources = resources;
        this.databaseInfoOracle = databaseInfoOracle;
        this.editorDatasourceOracle = editorDatasourceOracle;
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

        SqlCodeQuery query = buildQuery(textEditor);

        if (query.getLastQueryPrefix() == null) {
            codeAssistCallback.proposalComputed(null);
            return;
        }
        List<SqlCodeCompletionProposal> completionProposals = findAutoCompletions(query);
        InvocationContext invocationContext = new InvocationContext(query, offset, resources, textEditor);
        codeAssistCallback.proposalComputed(jsToList(completionProposals, invocationContext));
    }

    /**
     * Build a query for the given editor. The query object will than be used to search for possible completion proposal through the trie.
     */
    protected SqlCodeQuery buildQuery(TextEditor textEditor) {
        final int cursorPosition = textEditor.getCursorOffset();
        final EmbeddedDocument document = textEditor.getDocument();

        return buildQuery(cursorPosition, document);
    }

    protected SqlCodeQuery buildQuery(final int cursorPosition, final EmbeddedDocument document) {
        try {
            String text = getLastSQLStatementPrefix(cursorPosition, document);
            return getQuery(text);
        } catch (BadLocationException e) {
            Log.error(SqlCodeAssistProcessor.class, e);
            return null;
        }
    }

    protected String getLastSQLStatementPrefix(final int cursorPosition, final EmbeddedDocument document) throws BadLocationException {
        final int line = document.getPositionFromIndex(cursorPosition).getLine();
        final LinearRange range = document.getLinearRangeForLine(line);
        final int column = cursorPosition - range.getStartOffset();

        String textBefore = "";

        boolean parsingLineWithCursor = true;

        int currentLine = line;
        while ((currentLine >= 0) && (!textBefore.contains(";"))) {
            String text;
            int lastStatementSeparatorIndex;

            final LinearRange currentLineRange = document.getLinearRangeForLine(currentLine);
            if (parsingLineWithCursor) {
                text = document.getContentRange(currentLineRange.getStartOffset(), column);
                parsingLineWithCursor = false;
            } else {
                text = document.getContentRange(currentLineRange.getStartOffset(), currentLineRange.getLength()) + "\n";
            }

            lastStatementSeparatorIndex = text.lastIndexOf(';');

            if (lastStatementSeparatorIndex > -1) {
                textBefore = text.substring(lastStatementSeparatorIndex + 1).concat(textBefore);
                break;
            }
            currentLine--;
            textBefore = text.concat(textBefore);
        }
        return textBefore.replaceAll("^\\s*", "");
    }

    protected SqlCodeQuery getQuery(String text) {
        return new SqlCodeQuery(text);
    }

    protected List<SqlCodeCompletionProposal> findAutoCompletions(SqlCodeQuery query) {
        List<SqlCodeCompletionProposal> completionProposals = findTableAutocompletions(query);
        completionProposals.addAll(findColumnAutocompletions(query));
        completionProposals.addAll(findTemplateAutocompletion(query));
        return completionProposals;
    }

    protected List<SqlCodeCompletionProposal> findTableAutocompletions(SqlCodeQuery query) {
        List<SqlCodeCompletionProposal> array = new ArrayList<>();
        String linePrefix = query.getLastQueryPrefix();
        String linePrefixLowerCase = linePrefix.toLowerCase();

        MatchResult matcher = TABLE_REGEXP_PATTERN.exec(linePrefixLowerCase);
        if (matcher != null) {
            String tablePrefix = matcher.getGroup(TABLE_REGEXP_GROUP);
            String lineReplacementPrefix = linePrefix.substring(0, linePrefix.length() - tablePrefix.length());
            String datasourceId = editorDatasourceOracle.getSelectedDatasourceId(textEditor.getEditorInput().getFile().getPath());
            Collection<String> schemas = databaseInfoOracle.getSchemasFor(datasourceId);
            for (String schema : schemas) {
                Collection<String> tables = databaseInfoOracle.getTablesFor(datasourceId, schema);
                for (String table : tables) {
                    String tablePrefixLowerCase = tablePrefix.toLowerCase();
                    String tableLowerCase = table.toLowerCase();
                    String schemaLowerCase = schema.toLowerCase();
                    if (tableLowerCase.startsWith(tablePrefixLowerCase)
                        || (schemaLowerCase + "." + tableLowerCase).startsWith(tablePrefixLowerCase)) {
                        String replacementString = lineReplacementPrefix + schema + "." + table;
                        array.add(new SqlCodeCompletionProposal(schema + "." + table, replacementString, replacementString.length()));
                    }
                }
            }
        }
        return array;
    }

    protected List<SqlCodeCompletionProposal> findColumnAutocompletions(SqlCodeQuery query) {
        List<SqlCodeCompletionProposal> list = new ArrayList<>();
        String linePrefix = query.getLastQueryPrefix();
        String linePrefixLowerCase = linePrefix.toLowerCase();
        MatchResult matcher = COLUMN_REGEXP_PATTERN.exec(linePrefixLowerCase);
        List<String> selectedTables = new ArrayList<String>();
        boolean isSelectStatement = false;

        String columnPrefix = "";
        while (matcher != null) {
            String statementTypeGroup = matcher.getGroup((linePrefixLowerCase.contains("join")) ? JOIN_STATEMENT_TYPE_REGEXP_GROUP
                : STATEMENT_TYPE_REGEXP_GROUP);
            isSelectStatement = (statementTypeGroup != null)
                                && !statementTypeGroup.trim().isEmpty()
                                && statementTypeGroup.startsWith("from");
            String selectedTable = matcher.getGroup((linePrefixLowerCase.contains("join")) ? JOIN_TABLE_IN_COLUMN_REGEXP_GROUP
                : TABLE_IN_COLUMN_REGEXP_GROUP);
            columnPrefix = matcher.getGroup((linePrefixLowerCase.contains("join")) ? JOIN_COLUMN_REGEXP_GROUP : COLUMN_REGEXP_GROUP);
            if (selectedTable != null && !selectedTable.trim().isEmpty() && !selectedTables.contains(selectedTable)) {
                selectedTables.add(selectedTable);
            }
            String prevSelectedTable = matcher.getGroup((linePrefixLowerCase.contains("join")) ? JOIN_PREV_TABLES_IN_COLUMN_REGEXP_GROUP
                : PREV_TABLES_IN_COLUMN_REGEXP_GROUP);
            if (prevSelectedTable != null && !prevSelectedTable.trim().isEmpty() && !selectedTables.contains(prevSelectedTable)) {
                selectedTables.add(prevSelectedTable);
            }

            matcher = COLUMN_REGEXP_PATTERN.exec(linePrefixLowerCase);
        }

        String lineReplacementPrefix = linePrefix.substring(0, linePrefix.length() - columnPrefix.length());
        String datasourceId = editorDatasourceOracle.getSelectedDatasourceId(textEditor.getEditorInput().getFile().getPath());
        Collection<String> schemas = databaseInfoOracle.getSchemasFor(datasourceId);
        for (String schema : schemas) {
            Collection<String> tables = databaseInfoOracle.getTablesFor(datasourceId, schema);
            String schemaLowerCase = schema.toLowerCase();
            for (String table : tables) {
                for (String selectedTable : selectedTables) {
                    String tablePrefixLowerCase = selectedTable.toLowerCase();
                    String tableLowerCase = table.toLowerCase();
                    if (tableLowerCase.startsWith(tablePrefixLowerCase)
                        || (schemaLowerCase + "." + tableLowerCase).startsWith(tablePrefixLowerCase)) {
                        Collection<String> columns = databaseInfoOracle.getColumnsFor(datasourceId, schema, table);
                        for (String column : columns) {
                            String columnPrefixLowerCase = columnPrefix.toLowerCase();
                            String columnLowerCase = column.toLowerCase();
                            if (columnLowerCase.startsWith(columnPrefixLowerCase)
                                || (schemaLowerCase + "." + tableLowerCase + "." + columnLowerCase).startsWith(columnPrefixLowerCase)
                                || (tableLowerCase + "." + columnLowerCase).startsWith(columnPrefixLowerCase)) {
                                String columnReplacementString = isSelectStatement ? (schema + "." + table + "." + column) : column;
                                String replacementString = lineReplacementPrefix + columnReplacementString;
                                list.add(new SqlCodeCompletionProposal(schema + "." + table + "." + column, replacementString,
                                                                        replacementString.length()));
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    protected List<SqlCodeCompletionProposal> findTemplateAutocompletion(SqlCodeQuery query) {
        List<SqlCodeCompletionProposal> findAndFilterAutocompletions = SqlCodeTemplateTrie.findAndFilterAutocompletions(query);
        for (SqlCodeCompletionProposal proposal : findAndFilterAutocompletions) {
            int nextTablePosition = proposal.getReplacementString().indexOf("aTable");
            if (nextTablePosition > 0) {
                proposal.setCursorPosition(nextTablePosition);
                proposal.setSelectionLength((6));
            }
        }
        return findAndFilterAutocompletions;
    }

    /**
     * Convert Javascript array and apply invocation context
     *
     * @param autocompletions the list of auto completion proposals
     * @param context the given invocation context
     * @return the array
     */
    private List<CompletionProposal> jsToList(List<SqlCodeCompletionProposal> autocompletions,
                                                    InvocationContext context) {
        if (autocompletions == null) {
            return null;
        }
        final List<CompletionProposal> proposals = new ArrayList<>();
        for (int i = 0; i < autocompletions.size(); i++) {
            SqlCodeCompletionProposal proposal = autocompletions.get(i);
            proposal.setInvocationContext(context);
            proposals.add(proposal);
        }
        return proposals;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}
