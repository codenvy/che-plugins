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
package org.eclipse.che.ide.ext.runner.client.manager.info;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.PortMapping;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.models.Runner;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter.TIMER_STUB;

/**
 * Class provides view representation of panel which contains additional information about runner.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
public class MoreInfoImpl extends Composite implements MoreInfo {

    interface MoreInfoPopupImplUiBinder extends UiBinder<Widget, MoreInfoImpl> {
    }

    private static final MoreInfoPopupImplUiBinder UI_BINDER = GWT.create(MoreInfoPopupImplUiBinder.class);

    public static final String PORT_STUB = " --- ";

    @UiField
    Label     started;
    @UiField
    Label     finished;
    @UiField
    Label     timeout;
    @UiField
    Label     activeTime;
    @UiField
    Label     ram;
    @UiField
    Label     firstPort;
    @UiField
    Label     secondPort;
    @UiField
    Label     thirdPort;

    @UiField(provided = true)
    final RunnerResources            resources;
    @UiField(provided = true)
    final RunnerLocalizationConstant locale;

    @Inject
    public MoreInfoImpl(RunnerResources resources, RunnerLocalizationConstant locale) {
        this.resources = resources;
        this.locale = locale;

        initWidget(UI_BINDER.createAndBindUi(this));
        clearPorts();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nullable Runner runner) {
        clearPorts();
        if (runner == null) {
            started.setText(TIMER_STUB);
            finished.setText(TIMER_STUB);
            timeout.setText(TIMER_STUB);
            activeTime.setText(TIMER_STUB);

            ram.setText(0 + "MB");
        } else {
            started.setText(runner.getCreationTime());
            finished.setText(runner.getStopTime());
            timeout.setText(runner.getTimeout());
            activeTime.setText(runner.getActiveTime());

            ram.setText(runner.getRAM() + "MB");

            ApplicationProcessDescriptor runnerDescriptor = runner.getDescriptor();
            if (runnerDescriptor != null) {
                setPorts(runnerDescriptor);
            }
        }
    }

    private void setPorts(ApplicationProcessDescriptor runnerDescriptor) {
        PortMapping portMapping = runnerDescriptor.getPortMapping();
        if (portMapping == null) {
            return;
        }

        Map<String, String> ports = portMapping.getPorts();
        if (ports == null || ports.isEmpty()) {
            return;
        }

        List<String> raws = new ArrayList<>(3);
        for (String key : ports.keySet()) {
            raws.add(key + PORT_STUB + ports.get(key));
        }

        for (int i = 0; i < raws.size(); i++) {
            switch (i) {
                case 0:
                    firstPort.setText(raws.get(0));
                    break;
                case 1:
                    secondPort.setText(raws.get(1));
                    break;
                case 2:
                    thirdPort.setText(raws.get(2));
                    break;
            }
        }
    }

    private void clearPorts() {
        firstPort.setText(PORT_STUB);
        secondPort.setText(PORT_STUB);
        thirdPort.setText(PORT_STUB);
    }
}