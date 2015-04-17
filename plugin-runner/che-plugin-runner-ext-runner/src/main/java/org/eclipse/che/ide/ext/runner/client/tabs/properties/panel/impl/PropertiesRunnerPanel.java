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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.impl;

import com.google.gwt.user.client.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.*;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.ext.runner.client.constants.TimeInterval.ONE_SEC;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 * @author Alexander Andrienko
 */
public class PropertiesRunnerPanel extends PropertiesPanelPresenter {

    private final Timer timer;
    private final TabContainer               tabContainer;
    private final RunnerLocalizationConstant locale;

    @AssistedInject
    public PropertiesRunnerPanel(final PropertiesPanelView view,
                                 @Named("DefaultEditorProvider") final EditorProvider editorProvider,
                                 final FileTypeRegistry fileTypeRegistry,
                                 final DockerFileFactory dockerFileFactory,
                                 AppContext appContext,
                                 TimerFactory timerFactory,
                                 @Assisted @Nonnull final Runner runner,
                                 @LeftPanel TabContainer tabContainer,
                                 RunnerLocalizationConstant locale) {
        super(view, appContext);

        this.tabContainer = tabContainer;
        this.locale = locale;

        // We're waiting for getting application descriptor from server. So we can't show editor without knowing about configuration file.
        timer = timerFactory.newInstance(new TimerFactory.TimerCallBack() {
            @Override
            public void onRun() {
                String dockerUrl = runner.getDockerUrl();
                if (dockerUrl == null) {
                    timer.schedule(ONE_SEC.getValue());
                    return;
                }

                timer.cancel();

                DockerFile file = dockerFileFactory.newInstance(dockerUrl);
                initializeEditor(file, editorProvider, fileTypeRegistry);
                view.selectMemory(RAM.detect(runner.getRAM()));
            }
        });
        timer.schedule(ONE_SEC.getValue());

        this.view.setEnableNameProperty(false);
        this.view.setEnableRamProperty(false);
        this.view.setEnableBootProperty(false);
        this.view.setEnableShutdownProperty(false);
        this.view.setEnableScopeProperty(false);

        this.view.setVisibleSaveButton(false);
        this.view.setVisibleDeleteButton(false);
        this.view.setVisibleCancelButton(false);

        this.view.selectShutdown(getTimeout());
        this.view.selectMemory(RAM.detect(runner.getRAM()));
    }

    @Override
    public void onConfigLinkClicked() {
        tabContainer.showTab(locale.runnerTabTemplates());
    }
}