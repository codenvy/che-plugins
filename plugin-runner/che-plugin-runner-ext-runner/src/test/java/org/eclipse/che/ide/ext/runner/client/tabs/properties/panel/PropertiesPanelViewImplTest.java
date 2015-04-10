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
package org.eclipse.che.ide.ext.runner.client.tabs.properties.panel;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.inject.factories.WidgetFactory;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.Background;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.button.PropertyButtonWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Boot;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Shutdown;
import org.eclipse.che.ide.ui.switcher.Switcher;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.EnumSet;

import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.Background.BLUE;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.Background.GREY;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.DEFAULT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_1024;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_128;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_512;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_8192;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Shutdown.ALWAYS_ON;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Shutdown.BY_TIMEOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PropertiesPanelViewImplTest {

    private static final String TEXT   = "some text";
    private static final String CREATE = "Create";
    private static final String SAVE   = "Save";
    private static final String DELETE = "DELETE";
    private static final String CANCEL = "CANCEL";

    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private RunnerResources            resources;
    @Mock
    private WidgetFactory              widgetFactory;
    @Mock
    private Switcher                   switcher;

    @Mock
    private PropertyButtonWidget               createButtonWidget;
    @Mock
    private PropertyButtonWidget               saveButtonWidget;
    @Mock
    private PropertyButtonWidget               deleteButtonWidget;
    @Mock
    private PropertyButtonWidget               cancelButtonWidget;
    @Mock
    private PropertiesPanelView.ActionDelegate delegate;
    @Mock
    private EditorPartPresenter                editor;
    @Mock
    private ValueChangeEvent<Boolean>          changeEvent;

    @Captor
    private ArgumentCaptor<PropertyButtonWidget.ActionDelegate> captor;
    @Captor
    private ArgumentCaptor<ValueChangeHandler<Boolean>>         valueChangeCaptor;

    @Mock
    private RunnerResources.RunnerCss runnerCss;

    private PropertiesPanelViewImpl view;


    @Before
    public void setUp() {
        when(resources.runnerCss()).thenReturn(runnerCss);
        when(locale.editorNotReady()).thenReturn(TEXT);

        when(runnerCss.fullSize()).thenReturn(TEXT);
        when(runnerCss.unAvailableMessage()).thenReturn(TEXT);

        when(locale.propertiesButtonCreate()).thenReturn(CREATE);
        when(locale.propertiesButtonSave()).thenReturn(SAVE);
        when(locale.propertiesButtonDelete()).thenReturn(DELETE);
        when(locale.propertiesButtonCancel()).thenReturn(CANCEL);

        when(widgetFactory.createPropertyButton(CREATE, BLUE)).thenReturn(createButtonWidget);
        when(widgetFactory.createPropertyButton(SAVE, GREY)).thenReturn(saveButtonWidget);
        when(widgetFactory.createPropertyButton(DELETE, GREY)).thenReturn(deleteButtonWidget);
        when(widgetFactory.createPropertyButton(CANCEL, GREY)).thenReturn(cancelButtonWidget);

        when(changeEvent.getValue()).thenReturn(true);

        view = new PropertiesPanelViewImpl(locale, resources, widgetFactory, switcher);
        view.setDelegate(delegate);

        when(view.name.getText()).thenReturn(TEXT);
    }

    @Test
    public void switcherValueShouldBeChanged() throws Exception {
        verify(switcher).addValueChangeHandler(valueChangeCaptor.capture());

        valueChangeCaptor.getValue().onValueChange(changeEvent);

        verify(delegate).onSwitcherChanged(true);
        verify(changeEvent).getValue();
        verify(view.switcherPanel).add(switcher);
    }

    @Test
    public void prepareActionShouldBePerformed() {
        ramItemsShouldBeAdded();
        scopeShouldBeAdded();
        bootStateShouldBeAdded();
        shutdownStateShouldBeAdded();

        verify(locale).editorNotReady();
        verify(resources, times(2)).runnerCss();
        verify(runnerCss).fullSize();
        verify(runnerCss).unAvailableMessage();

        verify(view.editorPanel).setWidget(any(Label.class));

        verify(locale).propertiesButtonCreate();
        verify(locale).propertiesButtonSave();
        verify(locale).propertiesButtonCancel();
        verify(locale).propertiesButtonDelete();

        view.setDelegate(delegate);

        PropertyButtonWidget.ActionDelegate createDelegate = buttonShouldBeCreated(CREATE, BLUE, createButtonWidget, createButtonWidget);
        createDelegate.onButtonClicked();
        verify(delegate).onCopyButtonClicked();

        PropertyButtonWidget.ActionDelegate saveDelegate = buttonShouldBeCreated(SAVE, GREY, saveButtonWidget, saveButtonWidget);
        saveDelegate.onButtonClicked();
        verify(delegate).onSaveButtonClicked();

        PropertyButtonWidget.ActionDelegate deleteDelegate = buttonShouldBeCreated(CANCEL, GREY, cancelButtonWidget, deleteButtonWidget);
        deleteDelegate.onButtonClicked();
        verify(delegate).onDeleteButtonClicked();

        PropertyButtonWidget.ActionDelegate cancelDelegate = buttonShouldBeCreated(DELETE, GREY, deleteButtonWidget, cancelButtonWidget);
        cancelDelegate.onButtonClicked();
        verify(delegate).onCancelButtonClicked();
    }

    private void ramItemsShouldBeAdded() {
        for (Enum item : EnumSet.range(MB_128, MB_8192)) {
            verify(view.ram).addItem(item.toString());
        }
    }

    private void scopeShouldBeAdded() {
        for (Enum item : EnumSet.range(PROJECT, SYSTEM)) {
            verify(view.scope).addItem(item.toString());
        }
    }

    private void bootStateShouldBeAdded() {
        for (Enum item : EnumSet.allOf(Boot.class)) {
            verify(view.boot).addItem(item.toString());
        }
    }

    private void shutdownStateShouldBeAdded() {
        for (Enum item : EnumSet.allOf(Shutdown.class)) {
            verify(view.shutdown).addItem(item.toString());
        }
    }

    private PropertyButtonWidget.ActionDelegate buttonShouldBeCreated(String title,
                                                                      Background background,
                                                                      PropertyButtonWidget button,
                                                                      PropertyButtonWidget buttonWidget) {
        verify(widgetFactory).createPropertyButton(title, background);
        verify(buttonWidget).setDelegate(captor.capture());
        verify(view.buttonsPanel).add(button);

        return captor.getValue();
    }

    @Test
    public void nameShouldBeReturned() {
        assertThat(view.getName(), is(TEXT));

        verify(view.name).getText();
    }

    @Test
    public void nameShouldBeAdded() {
        view.setName(TEXT);
        verify(view.name).setText(TEXT);
    }

    @Test
    public void ramShouldBeReturned() {
        when(view.ram.getSelectedIndex()).thenReturn(2);
        when(view.ram.getValue(2)).thenReturn(MB_512.toString());

        assertThat(view.getRam(), is(MB_512));

        verify(view.ram).getSelectedIndex();
        verify(view.ram).getValue(2);
    }

    @Test
    public void defaultAmountMemoryShouldBeSelected() {
        when(view.ram.getValue(MB_512.ordinal())).thenReturn("512");
        when(view.ram.getItemCount()).thenReturn(RAM.values().length);

        view.selectMemory(DEFAULT);

        verify(view.ram).getItemCount();
        verify(view.ram, times(3)).getValue(anyInt());
        verify(view.ram).setItemSelected(MB_512.ordinal(), true);
    }

    @Test
    public void defaultAmountMemoryShouldNotBeSelected() {
        when(view.ram.getValue(1)).thenReturn("0");
        when(view.ram.getItemCount()).thenReturn(RAM.values().length);

        view.selectMemory(DEFAULT);

        verify(view.ram).getItemCount();
        verify(view.ram, times(RAM.values().length)).getValue(anyInt());
    }

    @Test
    public void amountMemoryShouldBeSelected() {
        view.selectMemory(MB_1024);

        verify(view.ram).setItemSelected(MB_1024.ordinal(), true);
    }

    @Test
    public void scopeShouldBeReturned() {
        when(view.scope.getSelectedIndex()).thenReturn(1);
        when(view.scope.getValue(1)).thenReturn(SYSTEM.toString());

        assertThat(view.getScope(), is(SYSTEM));

        verify(view.scope).getSelectedIndex();
    }

    @Test
    public void scopeShouldBeSelected() {
        view.selectScope(SYSTEM);

        verify(view.scope).setItemSelected(SYSTEM.ordinal(), true);
    }

    @Test
    public void typeShouldBeReturned() {
        when(view.type.getText()).thenReturn(TEXT);

        assertThat(view.getType(), is(TEXT));
    }

    @Test
    public void typeShouldBeSet() {
        view.setType(TEXT);

        verify(view.type).setText(TEXT);
    }

    @Test
    public void bootShouldBeReturned() {
        when(view.boot.getSelectedIndex()).thenReturn(1);
        when(view.boot.getValue(1)).thenReturn(Boot.RUNNER_START.toString());

        assertThat(view.getBoot(), CoreMatchers.is(Boot.RUNNER_START));
    }

    @Test
    public void bootShouldBeSelected() {
        view.selectBoot(Boot.IDE_OPENS);

        verify(view.boot).setItemSelected(Boot.IDE_OPENS.ordinal(), true);
    }

    @Test
    public void shutDownParameterShouldBeReturned() {
        when(view.shutdown.getSelectedIndex()).thenReturn(1);
        when(view.shutdown.getValue(1)).thenReturn(BY_TIMEOUT.toString());

        assertThat(view.getShutdown(), is(BY_TIMEOUT));
    }

    @Test
    public void shutDownShouldBeSelected() {
        view.selectShutdown(ALWAYS_ON);

        verify(view.shutdown).setItemSelected(ALWAYS_ON.ordinal(), true);
    }

    @Test
    public void buttonSaveShouldBeEnable() {
        view.setEnableSaveButton(true);

        verify(saveButtonWidget).setEnable(true);
    }

    @Test
    public void buttonSaveShouldNotBeEnable() {
        view.setEnableSaveButton(false);

        verify(saveButtonWidget).setEnable(false);
    }

    @Test
    public void buttonCancelShouldBeEnable() {
        view.setEnableCancelButton(true);

        verify(cancelButtonWidget).setEnable(true);
    }

    @Test
    public void buttonCancelShouldNotBeEnable() {
        view.setEnableCancelButton(false);

        verify(cancelButtonWidget).setEnable(false);
    }

    @Test
    public void buttonDeleteShouldBeEnable() {
        view.setEnableDeleteButton(true);

        verify(deleteButtonWidget).setEnable(true);
    }

    @Test
    public void buttonDeleteShouldNotBeEnable() {
        view.setEnableDeleteButton(false);

        verify(deleteButtonWidget).setEnable(false);
    }

    @Test
    public void namePropertyShouldBeEnable() {
        view.setEnableNameProperty(true);

        verify(view.name).setEnabled(true);
    }

    @Test
    public void namePropertyShouldNotBeEnable() {
        view.setEnableNameProperty(false);

        verify(view.name).setEnabled(false);
    }

    @Test
    public void ramPropertyShouldBeEnable() {
        view.setEnableRamProperty(true);

        verify(view.ram).setEnabled(true);
    }

    @Test
    public void ramPropertyShouldNotBeEnable() {
        view.setEnableRamProperty(false);

        verify(view.ram).setEnabled(false);
    }

    @Test
    public void bootPropertyShouldBeEnable() {
        view.setEnableBootProperty(true);

        verify(view.boot).setEnabled(true);
    }

    @Test
    public void bootPropertyShouldNotBeEnable() {
        view.setEnableBootProperty(false);

        verify(view.boot).setEnabled(false);
    }

    @Test
    public void shutdownPropertyShouldBeEnable() {
        view.setEnableShutdownProperty(true);

        verify(view.shutdown).setEnabled(true);
    }

    @Test
    public void shutdownPropertyShouldNotBeEnable() {
        view.setEnableShutdownProperty(false);

        verify(view.shutdown).setEnabled(false);
    }

    @Test
    public void scopePropertyShouldBeEnable() {
        view.setEnableScopeProperty(true);

        verify(view.scope).setEnabled(true);
    }

    @Test
    public void scopePropertyShouldNotBeEnable() {
        view.setEnableScopeProperty(false);

        verify(view.scope).setEnabled(false);
    }

    @Test
    public void buttonSaveShouldBeVisible() {
        view.setVisibleSaveButton(true);

        verify(saveButtonWidget).setVisible(true);
    }

    @Test
    public void buttonSaveShouldNotBeVisible() {
        view.setVisibleSaveButton(false);

        verify(saveButtonWidget).setVisible(false);
    }

    @Test
    public void buttonDeleteShouldBeVisible() {
        view.setVisibleSaveButton(true);

        verify(saveButtonWidget).setVisible(true);
    }

    @Test
    public void buttonDeleteShouldNotBeVisible() {
        view.setVisibleDeleteButton(false);

        verify(deleteButtonWidget).setVisible(false);
    }

    @Test
    public void buttonCancelShouldBeVisible() {
        view.setVisibleCancelButton(true);

        verify(cancelButtonWidget).setVisible(true);
    }

    @Test
    public void buttonCancelShouldNotBeVisible() {
        view.setVisibleCancelButton(false);

        verify(cancelButtonWidget).setVisible(false);
    }

    @Test
    public void buttonPanelShouldBeHided() {
        view.hideButtonsPanel();

        verify(view.propertiesPanel).setWidgetHidden(view.buttonsPanel, true);
    }

    @Test
    public void unAvailableMessageShouldBeShownIfEditorIsNull() {
        reset(view.editorPanel);

        view.showEditor(null);

        verify(view.editorPanel).setWidget(any(Label.class));
    }

    @Test
    public void editorShouldBeShown() {
        view.showEditor(editor);

        verify(editor).go(view.editorPanel);
    }

    @Test
    public void configurationShouldBeChangedIfTextInputted() {
        KeyUpEvent event = mock(KeyUpEvent.class);
        view.setDelegate(delegate);
        view.onTextInputted(event);

        verify(delegate).onConfigurationChanged();
    }

    @Test
    public void changeShouldBeHandled() {
        ChangeEvent event = mock(ChangeEvent.class);
        view.setDelegate(delegate);
        view.handleChange(event);

        verify(delegate).onConfigurationChanged();
    }

    @Test
    public void elementsShouldBeHideWhenScopeIsProject() throws Exception {
        when(resources.runnerCss().hideElement()).thenReturn(TEXT);

        view.hideSwitcher();

        verify(switcher).addStyleName(TEXT);
        verify(resources.runnerCss(), times(2)).hideElement();
    }

}