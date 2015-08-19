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
package org.eclipse.che.ide.ext.datasource.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.che.ide.ext.datasource.client.newdatasource.NewDatasourceWizardMessages;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.AbstractNewDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.NewDatasourceConnector;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.NewDatasourceConnectorAgent;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.amazon.ws.mysql.AwsMysqlConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.amazon.ws.oracle.AwsOracleConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.amazon.ws.postgres.AwsPostgresConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.amazon.ws.sqlserver.AwsSqlServerConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.google.cloud.sql.GoogleCloudSqlConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.mssqlserver.MssqlserverDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.mysql.MysqlDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.nuodb.NuoDBDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.oracle.OracleDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.postgres.PostgresDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.shared.DatabaseType;
import org.eclipse.che.ide.util.loging.Log;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ConnectorsInitializer {

    private final List<NewDatasourceConnector> connectors = new ArrayList<>();
    private final NewDatasourceConnectorAgent  connectorAgent;

    @Inject
    public ConnectorsInitializer(final NewDatasourceConnectorAgent connectorAgent,
                                 final DatasourceUiResources resources,
                                 final NewDatasourceWizardMessages dsMessages,
                                 final Provider<PostgresDatasourceConnectorPage> pgConnectorPageProvider,
                                 final Provider<MysqlDatasourceConnectorPage> mysqlConnectorPageProvider,
                                 final Provider<OracleDatasourceConnectorPage> oracleConnectorPageProvider,
                                 final Provider<MssqlserverDatasourceConnectorPage> mssqlserverConnectorPageProvider,
                                 final Provider<NuoDBDatasourceConnectorPage> nuodbConnectorPageProvider,
                                 final Provider<GoogleCloudSqlConnectorPage> googleCloudSqlConnectorPageProvider,
                                 final Provider<AwsPostgresConnectorPage> awsPostgresConnectorPageProvider,
                                 final Provider<AwsMysqlConnectorPage> awsMysqlConnectorPageProvider,
                                 final Provider<AwsOracleConnectorPage> awsOracleConnectorPageProvider,
                                 final Provider<AwsSqlServerConnectorPage> awsSqlServerConnectorPageProvider) {

        this.connectorAgent = connectorAgent;

        // counter to add different priorities to all connectors - to increment after each #register(NewDatasourceConnector)
        int connectorCounter = 0;

        // add a new postgres connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> pgWizardPages = new ArrayList<>();
        pgWizardPages.add(pgConnectorPageProvider);
        NewDatasourceConnector connectorPostgres = new NewDatasourceConnector(DatabaseType.POSTGRES.getConnectorId(),
                                                                              connectorCounter, dsMessages.postgresql(),
                                                                              resources.getPostgreSqlLogo(),
                                                                              "org.postgresql.Driver",
                                                                              pgWizardPages, DatabaseCategoryType.NOTCLOUD);
        this.connectors.add(connectorPostgres);

        connectorCounter++;

        // Add a new mysql connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> mysqlWizardPages = new ArrayList<>();
        mysqlWizardPages.add(mysqlConnectorPageProvider);
        NewDatasourceConnector connectorMysql = new NewDatasourceConnector(DatabaseType.MYSQL.getConnectorId(),
                                                                           connectorCounter,
                                                                           dsMessages.mysql(),
                                                                           resources.getMySqlLogo(),
                                                                           "com.mysql.jdbc.Driver",
                                                                           mysqlWizardPages, DatabaseCategoryType.NOTCLOUD);
        this.connectors.add(connectorMysql);

        connectorCounter++;

        // add a new oracle connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> oracleWizardPages = new ArrayList();
        oracleWizardPages.add(oracleConnectorPageProvider);
        NewDatasourceConnector connectorOracle = new NewDatasourceConnector(DatabaseType.ORACLE.getConnectorId(),
                                                                            connectorCounter,
                                                                            dsMessages.oracle(),
                                                                            resources.getOracleLogo(),
                                                                            "oracle.jdbc.OracleDriver", oracleWizardPages, DatabaseCategoryType.NOTCLOUD);
        this.connectors.add(connectorOracle);

        connectorCounter++;

        // add a new SQLserver connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> sqlServerWizardPages = new ArrayList();
        sqlServerWizardPages.add(mssqlserverConnectorPageProvider);
        NewDatasourceConnector connectorMs = new NewDatasourceConnector(DatabaseType.JTDS.getConnectorId(),
                                                                        connectorCounter,
                                                                        dsMessages.mssqlserver(),
                                                                        resources.getSqlServerLogo(),
                                                                        "net.sourceforge.jtds.jdbc.Driver",
                                                                        sqlServerWizardPages,
                                                                        DatabaseCategoryType.NOTCLOUD);
        this.connectors.add(connectorMs);

        connectorCounter++;

        // add a new NuoDB connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> nuoDBWizardPages = new ArrayList();
        nuoDBWizardPages.add(nuodbConnectorPageProvider);
        NewDatasourceConnector connectorNuoDB = new NewDatasourceConnector(DatabaseType.NUODB.getConnectorId(),
                                                                           connectorCounter,
                                                                           dsMessages.nuodb(),
                                                                           resources.getNuoDBLogo(),
                                                                           "com.nuodb.jdbc.Driver",
                                                                           nuoDBWizardPages, DatabaseCategoryType.NOTCLOUD);
        this.connectors.add(connectorNuoDB);

        connectorCounter++;

        // add a new GoogleCloudSQL connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> googleCloudSQLWizardPages = new ArrayList();
        googleCloudSQLWizardPages.add(googleCloudSqlConnectorPageProvider);
        NewDatasourceConnector connectorGoogle = new NewDatasourceConnector(DatabaseType.GOOGLECLOUDSQL.getConnectorId(),
                                                                            connectorCounter,
                                                                            dsMessages.googlecloudsql(),
                                                                            resources.getGoogleCloudSQLLogo(),
                                                                            "com.mysql.jdbc.Driver",
                                                                            googleCloudSQLWizardPages,
                                                                            DatabaseCategoryType.GOOGLE);
        this.connectors.add(connectorGoogle);

        connectorCounter++;

        // add a new AmazonRDS/Postgres connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> awsPostgresWizardPages = new ArrayList();
        awsPostgresWizardPages.add(awsPostgresConnectorPageProvider);
        NewDatasourceConnector connectorAwsPostg = new NewDatasourceConnector(AwsPostgresConnectorPage.AWSPOSTGRES_DB_ID,
                                                                              connectorCounter,
                                                                              dsMessages.awspg(),
                                                                              resources.getAwsPostgresLogo(),
                                                                              "org.postgresql.Driver",
                                                                              awsPostgresWizardPages,
                                                                              DatabaseCategoryType.AMAZON);
        this.connectors.add(connectorAwsPostg);

        connectorCounter++;

        // add a new AmazonRDS/Mysql connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> awsMysqlWizardPages = new ArrayList();
        awsMysqlWizardPages.add(awsMysqlConnectorPageProvider);
        NewDatasourceConnector connectorAwsMySql = new NewDatasourceConnector(AwsMysqlConnectorPage.AWSMYSQL_DB_ID,
                                                                              connectorCounter,
                                                                              dsMessages.awsmysql(),
                                                                              resources.getAwsMysqlLogo(),
                                                                              "com.mysql.jdbc.Driver",
                                                                              awsMysqlWizardPages,
                                                                              DatabaseCategoryType.AMAZON);
        this.connectors.add(connectorAwsMySql);

        connectorCounter++;

        // add a new AmazonRDS/Oracle connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> awsOracleWizardPages = new ArrayList();
        awsOracleWizardPages.add(awsOracleConnectorPageProvider);
        NewDatasourceConnector connectorAwsOracle = new NewDatasourceConnector(AwsOracleConnectorPage.AWSORACLE_DB_ID,
                                                                               connectorCounter,
                                                                               dsMessages.awsoracle(),
                                                                               resources.getAwsOracleLogo(),
                                                                               "oracle.jdbc.OracleDriver",
                                                                               awsOracleWizardPages,
                                                                               DatabaseCategoryType.AMAZON);
        this.connectors.add(connectorAwsOracle);

        connectorCounter++;

        // add a new AmazonRDS/SqlServer connector
        List<Provider< ? extends AbstractNewDatasourceConnectorPage>> awsSqlServerWizardPages = new ArrayList();
        awsSqlServerWizardPages.add(awsSqlServerConnectorPageProvider);
        NewDatasourceConnector connectorAwsMs = new NewDatasourceConnector(AwsSqlServerConnectorPage.AWSSQLSERVER_DB_ID,
                                                                           connectorCounter,
                                                                           dsMessages.awssqlserver(),
                                                                           resources.getAwsSqlServerLogo(),
                                                                           "net.sourceforge.jtds.jdbc.Driver",
                                                                           awsSqlServerWizardPages,
                                                                           DatabaseCategoryType.AMAZON);
        this.connectors.add(connectorAwsMs);

        connectorCounter++;

        Log.debug(ConnectorsInitializer.class, "Connectors registered : " + connectorCounter);
    }

    public void initConnectors() {
        for (NewDatasourceConnector connector : this.connectors) {
            this.connectorAgent.register(connector);
        }
    }
}
