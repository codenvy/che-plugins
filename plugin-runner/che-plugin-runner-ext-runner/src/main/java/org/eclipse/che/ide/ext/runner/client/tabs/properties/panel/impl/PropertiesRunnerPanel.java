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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.PortMapping;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEventHandler;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.PropertiesPanelView;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFile;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.docker.DockerFileFactory;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.eclipse.che.ide.ext.runner.client.util.annotations.LeftPanel;

import javax.annotation.Nonnull;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.constants.TimeInterval.ONE_SEC;
import static org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.common.RunnerApplicationStatusEvent.TYPE;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 * @author Alexander Andrienko
 */
public class PropertiesRunnerPanel extends PropertiesPanelPresenter {

    private final Timer                      timer;
    private final TabContainer               tabContainer;
    private final RunnerLocalizationConstant locale;
    private final EventBus                   eventBus;
    private       Runner                     currentRunner;

    @AssistedInject
    public PropertiesRunnerPanel(final PropertiesPanelView view,
                                 @Named("DefaultEditorProvider") final EditorProvider editorProvider,
                                 final FileTypeRegistry fileTypeRegistry,
                                 final DockerFileFactory dockerFileFactory,
                                 AppContext appContext,
                                 TimerFactory timerFactory,
                                 @Assisted @Nonnull final Runner runner,
                                 @LeftPanel TabContainer tabContainer,
                                 RunnerLocalizationConstant locale,
                                 EventBus eventBus) {
        super(view, appContext);

        this.tabContainer = tabContainer;
        this.locale = locale;
        this.eventBus = eventBus;
        this.currentRunner = runner;

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

                VirtualFile file = dockerFileFactory.newInstance(dockerUrl);
                initializeEditor(file, editorProvider, fileTypeRegistry);
                view.selectMemory(runner.getRAM());
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
        this.view.addRamValue(runner.getRAM());
        this.view.selectMemory(runner.getRAM());
        this.view.hideSwitcher();

        configureStatusRunEventHandler();
    }

    @Override
    public void onConfigLinkClicked() {
        tabContainer.showTab(locale.runnerTabTemplates());
    }

    private void configureStatusRunEventHandler() {
        eventBus.addHandler(TYPE, new RunnerApplicationStatusEventHandler() {
            @Override
            public void onRunnerStatusChanged(@Nonnull final Runner runner) {
                if (currentRunner.equals(runner)) {
                    setPorts(runner);
                }
            }
        });
    }

    private void setPorts(final Runner runner) {
        ApplicationProcessDescriptor runnerDescriptor = runner.getDescriptor();
        if (runnerDescriptor == null) {
            return;
        }

        final PortMapping portMapping = runnerDescriptor.getPortMapping();
        Map<String, String> ports = portMapping != null ? portMapping.getPorts() : null;
        view.setPorts(ports);
    }
}