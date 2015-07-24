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
package org.eclipse.che.ide.ext.datasource.client.newdatasource;

import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.ext.datasource.client.events.DatasourceListChangeEvent;
import org.eclipse.che.ide.ext.datasource.client.newdatasource.connector.AbstractNewDatasourceConnectorPage;
import org.eclipse.che.ide.ext.datasource.client.store.DatasourceManager;
import org.eclipse.che.ide.ext.datasource.shared.DatabaseConfigurationDTO;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

public class NewDatasourceWizard extends AbstractWizard<DatabaseConfigurationDTO> {
    public static final String DATASOURCE_NAME_KEY = "DatasourceName";

    private final NotificationManager notificationManager;
    private final DatasourceManager   datasourceManager;
    private final EventBus            eventBus;

    @Inject
    public NewDatasourceWizard(@Assisted DatabaseConfigurationDTO dataObject,
                               NotificationManager notificationManager,
                               DatasourceManager datasourceManager,
                               EventBus eventBus) {
        super(dataObject);
        this.notificationManager = notificationManager;
        this.datasourceManager = datasourceManager;
        this.eventBus = eventBus;
    }

    @Override
    public void complete(@Nonnull final CompleteCallback callback) {
        Log.debug(AbstractNewDatasourceConnectorPage.class, "Adding datasource with id " + dataObject.getDatasourceId());
        datasourceManager.add(dataObject);

        Log.debug(AbstractNewDatasourceConnectorPage.class, "Persisting datasources...");
        final Notification requestNotification = new Notification("Persisting datasources...",
                                                                  Notification.Status.PROGRESS);
        datasourceManager.persist(new AsyncCallback<ProfileDescriptor>() {

            @Override
            public void onSuccess(ProfileDescriptor result) {
                Log.debug(AbstractNewDatasourceConnectorPage.class, "Datasources persisted.");
                requestNotification.setMessage("Datasources saved");
                requestNotification.setStatus(Notification.Status.FINISHED);
                callback.onCompleted();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(AbstractNewDatasourceConnectorPage.class, "Failed to persist datasources.");
                requestNotification.setStatus(Notification.Status.FINISHED);
                notificationManager.showNotification(new Notification("Failed to persist datasources", Notification.Type.ERROR));
                callback.onFailure(caught);

            }
        });

        eventBus.fireEvent(new DatasourceListChangeEvent());
    }
}
