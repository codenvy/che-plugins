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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * The class offers special main panel to add tab container. The class is a wrapper of tab container view.
 *
 * @author Dmitry Shnurenko
 */
public class MachineApplianceViewImpl extends Composite implements MachineApplianceView, PartStackView {
    interface MachineInfoContainerUiBinder extends UiBinder<Widget, MachineApplianceViewImpl> {
    }

    private final static MachineInfoContainerUiBinder UI_BINDER = GWT.create(MachineInfoContainerUiBinder.class);

    private final Label unavailableLabel;

    @UiField
    SimplePanel container;

    @Inject
    public MachineApplianceViewImpl(MachineResources resources, Label unavailableLabel, MachineLocalizationConstant locale) {
        initWidget(UI_BINDER.createAndBindUi(this));

        this.unavailableLabel = unavailableLabel;
        this.unavailableLabel.addStyleName(resources.getCss().unavailableLabel());
        this.unavailableLabel.setText(locale.unavailableMachineInfo());
    }

    /** {@inheritDoc} */
    @Override
    public void showContainer(@Nullable IsWidget tabContainer) {
        container.setWidget(tabContainer != null ? tabContainer : unavailableLabel);
    }

    /** {@inheritDoc} */
    @Override
    public void addTab(@Nonnull TabItem tabItem, @Nonnull PartPresenter presenter) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void removeTab(@Nonnull PartPresenter partPresenter) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void selectTab(@Nonnull PartPresenter partPresenter) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPositions(List<PartPresenter> partPositions) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus(boolean focused) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void updateTabItem(@Nonnull PartPresenter partPresenter) {
        //to do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        //to do nothing
    }
}