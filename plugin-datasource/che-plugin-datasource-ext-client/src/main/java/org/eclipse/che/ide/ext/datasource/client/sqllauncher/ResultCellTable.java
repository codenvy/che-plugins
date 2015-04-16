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
package org.eclipse.che.ide.ext.datasource.client.sqllauncher;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * {@link CellTable} for SQL request results display.
 * 
 * @author "Mickaël Leduque"
 */
public class ResultCellTable extends CellTable<List<String>> {

    @Inject
    public ResultCellTable(final @Assisted int pageSize,
                           final @NotNull CellTableResourcesQueryResults cellTableResources,
                           final @NotNull SqlRequestLauncherConstants constants) {
        super(pageSize, cellTableResources);
        ensureDebugId("sql-table-result");
        final InlineLabel emptyWidget = new InlineLabel(constants.emptyResult());
        setEmptyTableWidget(emptyWidget);
        emptyWidget.setStyleName(cellTableResources.cellTableStyle().emptyTableWidget());

        addCellPreviewHandler(new CellPreviewEvent.Handler<List<String>>() {
            @Override
            public void onCellPreview(CellPreviewEvent<List<String>> event) {
                if ("click".equals(event.getNativeEvent().getType())) {
                    TableCellElement cellElement = getRowElement(event.getIndex()).getCells().getItem(event.getColumn());
                    cellElement.setTitle(cellElement.getInnerText());
                }
            }
        });
    }

}
