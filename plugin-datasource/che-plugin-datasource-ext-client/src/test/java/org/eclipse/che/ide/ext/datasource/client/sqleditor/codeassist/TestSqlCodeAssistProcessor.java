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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.eclipse.che.ide.ext.datasource.client.sqleditor.EditorDatasourceOracle;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.SqlEditorResources;
import org.eclipse.che.ide.ext.datasource.client.store.DatabaseInfoOracle;
import org.eclipse.che.ide.jseditor.client.texteditor.ConfigurableTextEditor;
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * Testing template based, table and column completion processing.
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSqlCodeAssistProcessor {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected ConfigurableTextEditor textEditor;

    @Mock
    protected SqlEditorResources        resources;

    @Mock
    protected DatabaseInfoOracle        databaseInfoOracle;

    @Mock
    protected EditorDatasourceOracle    editorDatasourceOracle;

    protected SqlCodeAssistProcessor    codeAssistProcessor;

    @Before
    public void init() {
        String editorId = "testEditorInputId";
        when(textEditor.getEditorInput().getFile().getPath()).thenReturn(editorId);
        when(editorDatasourceOracle.getSelectedDatasourceId(editorId)).thenReturn("datasourceId");
        when(databaseInfoOracle.getSchemasFor("datasourceId")).thenReturn(Lists.create("public", "meta"));
        when(databaseInfoOracle.getTablesFor("datasourceId", "public")).thenReturn(Lists.create("table", "atable"));
        when(databaseInfoOracle.getTablesFor("datasourceId", "meta")).thenReturn(Lists.create("metaTable", "aMetaTable"));
        when(databaseInfoOracle.getColumnsFor("datasourceId", "public", "atable")).thenReturn(Lists.create("column11", "column12"));
        when(databaseInfoOracle.getColumnsFor("datasourceId", "public", "table")).thenReturn(Lists.create("column21", "column22", "coumn23"));
        when(databaseInfoOracle.getColumnsFor("datasourceId", "meta", "metaTable")).thenReturn(Lists.create("column11", "column12",
                                                                                                            "column222"));
        when(databaseInfoOracle.getColumnsFor("datasourceId", "meta", "aMetaTable")).thenReturn(Lists.create("column11", "column12",
                                                                                                             "column234"));

        codeAssistProcessor = new SqlCodeAssistProcessor(textEditor, resources, databaseInfoOracle, editorDatasourceOracle);
    }


    @Test
    public void completeSelectTemplate() {
        List<SqlCodeCompletionProposal> results = codeAssistProcessor.findAutoCompletions(new SqlCodeQuery("SELEC"));
        Assert.assertEquals("For number of results for SELEC autocompletion, we expect ", 5, results.size());
    }

    @Test
    public void completeSelectTemplateAfter2statements() {
        List<SqlCodeCompletionProposal> results = codeAssistProcessor.findAutoCompletions(new SqlCodeQuery("\nSELEC"));
        Assert.assertEquals("For number of results for SELEC autocompletion after two statements, we expect ", 5, results.size());
    }

    @Test
    public void completeInsertTemplate() {
        List<SqlCodeCompletionProposal> results = codeAssistProcessor.findAutoCompletions(new SqlCodeQuery("inser"));
        assertEquals("For number of results for inser autocompletion, we expect ", 1, results.size());
        assertEquals("Replacement String should be", "INSERT INTO aTable (column1, column2) VALUES ('value1', 0);",
                     results.get(0).getReplacementString());
    }

    @Test
    public void completeTableForSelectFrom() {
        testTableCompletion("Select * from ", "for number of results for table autocompletion, we expect ", 4);
    }

    protected List<SqlCodeCompletionProposal> testTableCompletion(String queryPrefix, String assertMessage, int expectedResultsCount) {
        List<SqlCodeCompletionProposal> results = codeAssistProcessor.findTableAutocompletions(new SqlCodeQuery(queryPrefix));
        assertEquals(assertMessage, expectedResultsCount, results.size());
        return results;
    }

    @Test
    public void completeTableForFirstLetterSelectFrom() {
        List<SqlCodeCompletionProposal> result = testTableCompletion("Select * from t", "for number of results for table autocompletion starting with t, we expect ", 1);
        assertEquals("result completion should be", "Select * from public.table", result.get(0).getReplacementString());
    }

    @Test
    public void completeTableCaseInsensitive() {
        testTableCompletion("Select * from metat", "for number of results for table autocompletion starting with metat, we expect ", 1);
    }

    @Test
    public void completeTableForSelectFromMultipleTable() {
        testTableCompletion("Select * from atable, ",
                            "for number of results for table autocompletion (with multiple tables selected), we expect ",
                            4);
    }

    @Test
    public void completeTableForSelectFromFourthTable() {
        testTableCompletion("Select * from atable, another, table, ",
                            "for number of results for table autocompletion (with 4+ tables selected), we expect ", 4);
    }

    @Test
    public void completeTableForSelectFromTableWithSchema() {
        testTableCompletion("Select * from schema.atable, ",
                            "for number of results for table autocompletion with a table with schema selected), we expect ",
                            4);
    }

    @Test
    public void completeTableForSelectFromWithCarriageRuturns() {
        testTableCompletion("select * \nfrom ",
                            "for number of results for table autocompletion with a statement with CR, we expect ",
                            4);
    }

    @Test
    public void completeTableWithFullName() {
        testTableCompletion(
                            "select * \nfrom pu",
                            "for number of results for table autocompletion when starting writing public, we expect ",
                            2);
    }

    @Test
    public void completeColumnSelectWhere() {
        testColumnCompletion("select * \nfrom public.atable where ",
                             "For the results of a column autocompletion when using a select/where statement, we expect ",
                             2);
    }

    protected void testColumnCompletion(String queryPrefix, String assertMessage, int expectedResultsCount) {
        List<SqlCodeCompletionProposal> results = codeAssistProcessor.findColumnAutocompletions(new SqlCodeQuery(queryPrefix));
        assertEquals(assertMessage, expectedResultsCount, results.size());
    }

    @Test
    public void completeColumnInsertInto() {
        testColumnCompletion("INSERT INTO atable (",
                             "For the results of a column autocompletion using an insert into statement, we expect ",
                             2);
    }

    @Test
    public void completeColumnUpdateTable() {
        testColumnCompletion("UPDATE atable SET ",
                             "For the results of a column autocompletion using an UPDATE/SET statement, we expect ",
                             2);
    }

    @Test
    public void completeColumnSelectMultipleTable() {
        testColumnCompletion("Select * from atable, meta.metaTable WHERE ",
                             "For the results of a column autocompletion using an SELECT/WHERE statement with multiple selected table, we expect ",
                             5);
    }

    @Test
    public void complete2ndColumnSelectWhere() {
        testColumnCompletion("select * \nfrom public.atable where public.atable.column1 = 'test' AND ",
                             "For the results of a column autocompletion when using a select/where statement, we expect ",
                             2);
    }

    @Test
    public void complete2ndColumnInsertInto() {
        testColumnCompletion("INSERT INTO atable (atable.column1, ",
                             "For the results of a column autocompletion using an insert into statement, we expect ",
                             2);
    }

    @Test
    public void complete2ndColumnUpdateTable() {
        testColumnCompletion("UPDATE atable SET column1 = 'value1', ",
                             "For the results of a column autocompletion using an UPDATE/SET statement, we expect ",
                             2);
    }

    @Test
    public void complete2ndColumnSelectMultipleTable() {
        testColumnCompletion("Select * from atable, meta.metaTable WHERE column1 = 'value1' AND ",
                             "For the results of a column autocompletion using an SELECT/WHERE statement with multiple selected table, we expect ",
                             5);
    }

    /*
     * INNER JOIN TESTS
     */
    @Test
    public void completeSelectINNERJOINArgumentTable() {
        testTableCompletion("select * from atable INNER JOIN ",
                            "For the results of a INNER JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectINNERJOINFirstArgumentON() {
        testColumnCompletion("select * from atable INNER JOIN table ON ",
                             "For the results of first argument of ON a INNER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectINNERJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable INNER JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a INNER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * LEFT JOIN TESTS
     */
    @Test
    public void completeSelectLEFTJOINArgumentTable() {
        testTableCompletion("select * from atable LEFT JOIN ",
                            "For the results of a LEFT JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectLEFTJOINFirstArgumentON() {
        testColumnCompletion("select * from atable LEFT JOIN table ON ",
                             "For the results of first argument of ON a LEFT JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectLEFTJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable LEFT JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a LEFT JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * LEFT OUTER JOIN TESTS
     */
    @Test
    public void completeSelectLEFTOUTERJOINArgumentTable() {
        testTableCompletion("select * from atable LEFT OUTER JOIN ",
                            "For the results of a LEFT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectLEFTOUTERJOINFirstArgumentON() {
        testColumnCompletion("select * from atable LEFT OUTER JOIN table ON ",
                             "For the results of first argument of ON a LEFT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectLEFTOUTERJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable LEFT OUTER JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a LEFT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * RIGHT JOIN TESTS
     */
    @Test
    public void completeSelectRIGHTJOINArgumentTable() {
        testTableCompletion("select * from atable RIGHT JOIN ",
                            "For the results of a RIGHT JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectRIGHTJOINFirstArgumentON() {
        testColumnCompletion("select * from atable RIGHT JOIN table ON ",
                             "For the results of first argument of ON a RIGHT JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectRIGHTJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable RIGHT JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a RIGHT JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }


    /*
     * RIGHT OUTER JOIN TESTS
     */
    @Test
    public void completeSelectRIGHTOUTERJOINArgumentTable() {
        testTableCompletion("select * from atable RIGHT OUTER JOIN ",
                            "For the results of a RIGHT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectRIGHTOUTERJOINFirstArgumentON() {
        testColumnCompletion("select * from atable RIGHT OUTER JOIN table ON ",
                             "For the results of first argument of ON a RIGHT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectRIGHTOUTERJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable RIGHT JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a RIGHT OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * FULL JOIN TESTS
     */
    @Test
    public void completeSelectFULLJOINArgumentTable() {
        testTableCompletion("select * from atable FULL JOIN ",
                            "For the results of a FULL JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectFULLJOINFirstArgumentON() {
        testColumnCompletion("select * from atable FULL JOIN table ON ",
                             "For the results of first argument of ON a FULL JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectFULLJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable FULL JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a FULL JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * FULL OUTER JOIN TESTS
     */
    @Test
    public void completeSelectFULLOUTERJOINArgumentTable() {
        testTableCompletion("select * from atable FULL OUTER JOIN ",
                            "For the results of a FULL OUTER JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    @Test
    public void completeSelectFULLOUTERJOINFirstArgumentON() {
        testColumnCompletion("select * from atable FULL OUTER JOIN table ON ",
                             "For the results of first argument of ON a FULL OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    @Test
    public void completeSelectFULLOUTERJOIN2ndArgumentON() {
        testColumnCompletion("select * from atable FULL OUTER JOIN table ON atable.column11 = ",
                             "For the results of second argument of ON  a FULL OUTER JOIN autocompletion using a SELECT statement, we expect ",
                             5);
    }

    /*
     * CROSS JOIN TESTS
     */
    @Test
    public void completeSelectCROSSJOINArgumentTable() {
        testTableCompletion("select * from atable CROSS JOIN ",
                            "For the results of a CROSS JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }

    /*
     * NATURAL JOIN TESTS
     */
    @Test
    public void completeSelectNATURALJOINArgumentTable() {
        testTableCompletion("select * from atable NATURAL JOIN ",
                            "For the results of a NATURAL JOIN autocompletion using a SELECT statement, we expect ",
                            4);
    }
}
