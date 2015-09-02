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
package org.eclipse.che.ide.extension.machine.client.machine.extserver;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.ExtServerStateHandler;
import org.eclipse.che.ide.api.notification.NotificationManager;

/**
 * Notifies about changing extension server.
 *
 * @author Valeriy Svydenko
 */
public class ExtServerStateNotifier implements ExtServerStateHandler {

    private NotificationManager notificationManager;

    @Inject
    public ExtServerStateNotifier(EventBus eventBus, NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
    }

    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        notificationManager.showInfo("Extension server started");
    }

    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        notificationManager.showWarning("Extension server stopped");
    }
}
