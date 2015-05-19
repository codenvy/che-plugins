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
package org.eclipse.che.ide.ext.java.client.format;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;

/**
 * @author Roman Nikitenko
 */
public class FormatController {

    private FormatClientService service;

    @Inject
    public FormatController(/*JavaParserWorker worker,*/ FormatClientService formatClientService, EventBus eventBus) {
        this.service = formatClientService;
//        this.worker = worker;
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                getFormattingCodenvySettings();
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {

            }
        });
    }

    private void getFormattingCodenvySettings() {
//        service.formattingCodenvySettings(new AsyncRequestCallback<String>(new org.eclipse.che.ide.rest.StringUnmarshaller()) {
//            @Override
//            protected void onSuccess(String result) {
//                JsoStringMap<String> mapSettings = Jso.deserialize(result).cast();
//                worker.preferenceFormatSettings(mapSettings);
//            }
//
//            @Override
//            protected void onFailure(Throwable throwable) {
//                Log.error(getClass(), "Can not get formatting settings from file 'codenvy-codestyle-eclipse_.xml'");
//            }
//        });
    }
}
