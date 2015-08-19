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
package org.eclipse.che.ide.ext.datasource.client.ssl;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.ext.datasource.shared.ssl.SslKeyStoreEntry;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SslKeyStoreManagerViewImpl extends Composite implements SslKeyStoreManagerView {
    interface SshKeyManagerViewImplUiBinder extends UiBinder<Widget, SslKeyStoreManagerViewImpl> {
    }

    private static SshKeyManagerViewImplUiBinder ourUiBinder = GWT.create(SshKeyManagerViewImplUiBinder.class);

    @UiField
    Button                                       btnClientUpload;

    @UiField
    Button                                       btnServerUpload;

    @UiField(provided = true)
    CellTable<SslKeyStoreEntry>                  clientKeys;

    @UiField(provided = true)
    CellTable<SslKeyStoreEntry>                  serverCerts;

    @UiField(provided = true)
    final SslMessages                            locale;

    private ActionDelegate                       delegate;

    @Inject
    protected SslKeyStoreManagerViewImpl(SslMessages locale, Resources res) {
        this.locale = locale;
        initSslKeyTable(res);
        initSslCertTable(res);
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    protected void initSslKeyTable(CellTable.Resources res) {
        clientKeys = new CellTable<SslKeyStoreEntry>(15, res);
        addAliasColumn(clientKeys, locale.headerKeyList());
        addTypeColumn(clientKeys, "");
        Column<SslKeyStoreEntry, String> deleteColumn = addDeleteColumn(clientKeys, "");
        // Creates handler on button clicked
        deleteColumn.setFieldUpdater(new FieldUpdater<SslKeyStoreEntry, String>() {
            @Override
            public void update(int index, SslKeyStoreEntry object, String value) {
                delegate.onClientKeyDeleteClicked(object);
            }
        });
        // don't show loading indicator
        clientKeys.setLoadingIndicator(null);
    }

    protected void addAliasColumn(CellTable<SslKeyStoreEntry> cellTable, String columnHeaderName) {
        Column<SslKeyStoreEntry, String> aliasColumn = new Column<SslKeyStoreEntry, String>(new TextCell()) {
            @Override
            public String getValue(SslKeyStoreEntry keyItem) {
                return keyItem.getAlias();
            }
        };
        aliasColumn.setSortable(true);
        cellTable.addColumn(aliasColumn, columnHeaderName);
        cellTable.setColumnWidth(aliasColumn, 50, Style.Unit.PCT);
    }

    protected void addTypeColumn(CellTable<SslKeyStoreEntry> cellTable, String columnHeaderName) {
        Column<SslKeyStoreEntry, String> typeColumn = new Column<SslKeyStoreEntry, String>(new ButtonCell()) {
            @Override
            public String getValue(SslKeyStoreEntry object) {
                return object.getType();
            }

            /** {@inheritDoc} */
            @Override
            public void render(Context context, SslKeyStoreEntry object, SafeHtmlBuilder sb) {
                if (object != null && object.getType() != null) {
                    super.render(context, object, sb);
                }
            }
        };
        cellTable.addColumn(typeColumn, columnHeaderName);
        cellTable.setColumnWidth(typeColumn, 30, Style.Unit.PX);
    }

    protected Column<SslKeyStoreEntry, String> addDeleteColumn(CellTable<SslKeyStoreEntry> cellTable, String columnHeaderName) {
        Column<SslKeyStoreEntry, String> deleteKeyColumn = new Column<SslKeyStoreEntry, String>(new ButtonCell()) {
            @Override
            public String getValue(SslKeyStoreEntry object) {
                return "Delete";
            }
        };
        cellTable.addColumn(deleteKeyColumn, columnHeaderName);
        cellTable.setColumnWidth(deleteKeyColumn, 30, Style.Unit.PX);
        return deleteKeyColumn;
    }

    protected void initSslCertTable(CellTable.Resources res) {
        serverCerts = new CellTable<SslKeyStoreEntry>(15, res);
        addAliasColumn(serverCerts, locale.headerTrustList());
        addTypeColumn(serverCerts, "");
        Column<SslKeyStoreEntry, String> deleteColumn = addDeleteColumn(serverCerts, "");
        // Creates handler on button clicked
        deleteColumn.setFieldUpdater(new FieldUpdater<SslKeyStoreEntry, String>() {
            @Override
            public void update(int index, SslKeyStoreEntry object, String value) {
                delegate.onServerCertDeleteClicked(object);
            }
        });
        // don't show loading indicator
        serverCerts.setLoadingIndicator(null);
    }

    @Override
    public void setClientKeys(@NotNull List<SslKeyStoreEntry> keys) {
        // Wraps Array in java.util.List
        List<SslKeyStoreEntry> appList = new ArrayList<SslKeyStoreEntry>();
        for (int i = 0; i < keys.size(); i++) {
            appList.add(keys.get(i));
        }
        this.clientKeys.setRowData(appList);
    }

    @Override
    public void setServerCerts(@NotNull List<SslKeyStoreEntry> keys) {
        // Wraps Array in java.util.List
        List<SslKeyStoreEntry> appList = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            appList.add(keys.get(i));
        }
        this.serverCerts.setRowData(appList);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("btnClientUpload")
    public void onUploadKeyButtonClicked(ClickEvent event) {
        delegate.onClientKeyUploadClicked();
    }

    @UiHandler("btnServerUpload")
    public void onUploadCertButtonClicked(ClickEvent event) {
        delegate.onServerCertUploadClicked();
    }

}
