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
package org.eclipse.che.ide.ext.datasource.client.inject;


import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.preferences.PreferencePagePresenter;
import org.eclipse.che.ide.ext.datasource.client.AvailableJdbcDriversService;
import org.eclipse.che.ide.ext.datasource.client.AvailableJdbcDriversServiceRestImpl;
import org.eclipse.che.ide.ext.datasource.client.DatasourceClientService;
import org.eclipse.che.ide.ext.datasource.client.DatasourceClientServiceImpl;
import org.eclipse.che.ide.ext.datasource.client.DatasourceUiResources;
import org.eclipse.che.ide.ext.datasource.client.SqlEditorExtension;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.DialogFactory;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.confirm.ConfirmWindow;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.confirm.ConfirmWindowFooter;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.confirm.ConfirmWindowPresenter;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.confirm.ConfirmWindowView;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.confirm.ConfirmWindowViewImpl;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.message.MessageWindow;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.message.MessageWindowFooter;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.message.MessageWindowPresenter;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.message.MessageWindowView;
import org.eclipse.che.ide.ext.datasource.client.common.interaction.message.MessageWindowViewImpl;
import org.eclipse.che.ide.ext.datasource.client.discovery.DatasourceDiscovery;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.EditDatasourcesPresenterFactory;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.EditDatasourcesView;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.EditDatasourcesViewImpl;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.celllist.DatasourceCell;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.celllist.DatasourceKeyProvider;
import org.eclipse.che.ide.ext.datasource.client.editdatasource.wizard.EditDatasourceWizardFactory;
import org.eclipse.che.ide.ext.datasource.client.explorer.DatasourceExplorerView;
import org.eclipse.che.ide.ext.datasource.client.explorer.DatasourceExplorerViewImpl;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.NewDatasourceWizardFactory;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.DefaultNewDatasourceConnectorView;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.DefaultNewDatasourceConnectorViewImpl;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.NewDatasourceConnectorAgent;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.NewDatasourceConnectorAgentImpl;
import org.eclipse.che.ide.ext.datasource.client.properties.DataEntityPropertiesView;
import org.eclipse.che.ide.ext.datasource.client.properties.DataEntityPropertiesViewImpl;
import org.eclipse.che.ide.ext.datasource.client.service.FetchMetadataService;
import org.eclipse.che.ide.ext.datasource.client.service.FetchMetadataServiceImpl;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.EditorDatasourceOracle;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.EditorDatasourceOracleImpl;
import org.eclipse.che.ide.ext.datasource.client.sqleditor.SqlEditorConstants;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.RequestResultHeader;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.RequestResultHeaderFactory;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.RequestResultHeaderImpl;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.ResultItemBoxFactory;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.SqlRequestLauncherFactory;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.SqlRequestLauncherView;
import org.eclipse.che.ide.ext.datasource.client.sqllauncher.SqlRequestLauncherViewImpl;
import org.eclipse.che.ide.ext.datasource.client.ssl.SslKeyStoreClientService;
import org.eclipse.che.ide.ext.datasource.client.ssl.SslKeyStoreClientServiceImpl;
import org.eclipse.che.ide.ext.datasource.client.ssl.SslKeyStoreManagerPresenter;
import org.eclipse.che.ide.ext.datasource.client.ssl.SslKeyStoreManagerView;
import org.eclipse.che.ide.ext.datasource.client.ssl.SslKeyStoreManagerViewImpl;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslKeyDialogView;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslKeyDialogViewImpl;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslTrustCertDialogView;
import org.eclipse.che.ide.ext.datasource.client.ssl.upload.UploadSslTrustCertDialogViewImpl;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.util.Arrays;
import java.util.List;

@ExtensionGinModule
public class DatasourceGinModule extends AbstractGinModule {

    /** The name bound to the datasource rest context. */
    public static final String DATASOURCE_CONTEXT_NAME = "datasourceRestContext";

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named(DATASOURCE_CONTEXT_NAME)).to("/datasource");

        bind(DatasourceExplorerView.class).to(DatasourceExplorerViewImpl.class)
                                          .in(Singleton.class);
        bind(DatasourceClientService.class).to(DatasourceClientServiceImpl.class)
                                           .in(Singleton.class);

        install(new GinFactoryModuleBuilder().build(NewDatasourceWizardFactory.class));
        install(new GinFactoryModuleBuilder().build(EditDatasourceWizardFactory.class));

        bind(NewDatasourceConnectorAgent.class).to(NewDatasourceConnectorAgentImpl.class).in(Singleton.class);
        bind(DefaultNewDatasourceConnectorView.class).to(DefaultNewDatasourceConnectorViewImpl.class);

        bind(DataEntityPropertiesView.class).to(DataEntityPropertiesViewImpl.class);

        bind(SqlRequestLauncherView.class).to(SqlRequestLauncherViewImpl.class);

        install(new GinFactoryModuleBuilder().implement(SqlRequestLauncherView.class, SqlRequestLauncherViewImpl.class)
                                             .build(SqlRequestLauncherFactory.class));

        bind(AvailableJdbcDriversService.class).to(AvailableJdbcDriversServiceRestImpl.class).in(Singleton.class);

        bind(FetchMetadataService.class).to(FetchMetadataServiceImpl.class).in(Singleton.class);

        bind(EditorDatasourceOracle.class).to(EditorDatasourceOracleImpl.class).in(Singleton.class);

        // Add and bind ssl keystore manager preference page and views
        final GinMultibinder<PreferencePagePresenter> prefBinder = GinMultibinder.newSetBinder(binder(), PreferencePagePresenter.class);
        prefBinder.addBinding().to(SslKeyStoreManagerPresenter.class);
        bind(SslKeyStoreClientService.class).to(SslKeyStoreClientServiceImpl.class).in(Singleton.class);
        bind(SslKeyStoreManagerView.class).to(SslKeyStoreManagerViewImpl.class).in(Singleton.class);
        bind(UploadSslKeyDialogView.class).to(UploadSslKeyDialogViewImpl.class).in(Singleton.class);
        bind(UploadSslTrustCertDialogView.class).to(UploadSslTrustCertDialogViewImpl.class).in(Singleton.class);

        bind(EditDatasourcesView.class).to(EditDatasourcesViewImpl.class);
        install(new GinFactoryModuleBuilder().build(EditDatasourcesPresenterFactory.class));
        bind(DatasourceKeyProvider.class).annotatedWith(Names.named(DatasourceKeyProvider.NAME))
                                         .to(DatasourceKeyProvider.class)
                                         .in(Singleton.class);
        bind(DatasourceCell.class).in(Singleton.class);

        /* factories for the result zone */
        install(new GinFactoryModuleBuilder().build(ResultItemBoxFactory.class));
        install(new GinFactoryModuleBuilder().implement(RequestResultHeader.class, RequestResultHeaderImpl.class)
                                             .build(RequestResultHeaderFactory.class));

        /* confirmation window */
        bind(ConfirmWindowFooter.class);
        bind(ConfirmWindowView.class).to(ConfirmWindowViewImpl.class);
        /* message window */
        bind(MessageWindowFooter.class);
        bind(MessageWindowView.class).to(MessageWindowViewImpl.class);
        /* factory for these dialogs. */
        install(new GinFactoryModuleBuilder().implement(ConfirmWindow.class, ConfirmWindowPresenter.class)
                                             .implement(MessageWindow.class, MessageWindowPresenter.class)
                                             .build(DialogFactory.class));

        bind(DatasourceDiscovery.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    @Named("SQLFileType")
    protected FileType provideSQLFile(final DatasourceUiResources resources) {
        final SqlEditorConstants constants = GWT.create(SqlEditorConstants.class);
        final List<String> mimetypes = Arrays.asList(SqlEditorExtension.GENERIC_SQL_MIME_TYPE,
                                                     SqlEditorExtension.MSSQL_SQL_MIME_TYPE,
                                                     SqlEditorExtension.MYSQL_SQL_MIME_TYPE,
                                                     SqlEditorExtension.ORACLE_SQL_MIME_TYPE);
        return new FileType(constants.sqlFiletypeContentDescription(), resources.sqlIcon(),
                            mimetypes, SqlEditorExtension.SQL_FILE_EXTENSION);
    }

}
