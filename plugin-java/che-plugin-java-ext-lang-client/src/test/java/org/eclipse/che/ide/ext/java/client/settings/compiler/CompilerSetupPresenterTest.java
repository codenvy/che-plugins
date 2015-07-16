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
package org.eclipse.che.ide.ext.java.client.settings.compiler;

import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.inject.factories.PropertyWidgetFactory;
import org.eclipse.che.ide.ext.java.client.settings.property.PropertyWidget;
import org.eclipse.che.ide.ext.java.client.settings.service.SettingsServiceClient;
import org.eclipse.che.ide.settings.common.SettingsPagePresenter.DirtyStateListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.COMPARING_IDENTICAL_VALUES;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.COMPILER_UNUSED_IMPORT;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.COMPILER_UNUSED_LOCAL;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.DEAD_CODE;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.FIELD_HIDES_ANOTHER_VARIABLE;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.METHOD_WITH_CONSTRUCTOR_NAME;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.MISSING_DEFAULT_CASE;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.MISSING_OVERRIDE_ANNOTATION;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.MISSING_SERIAL_VERSION_UID;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.NO_EFFECT_ASSIGNMENT;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.NULL_POINTER_ACCESS;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.POTENTIAL_NULL_POINTER_ACCESS;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.REDUNDANT_NULL_CHECK;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.TYPE_PARAMETER_HIDE_ANOTHER_TYPE;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.UNCHECKED_TYPE_OPERATION;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.UNNECESSARY_ELSE_STATEMENT;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.UNUSED_PRIVATE_MEMBER;
import static org.eclipse.che.ide.ext.java.client.settings.compiler.CompilerOptions.USAGE_OF_RAW_TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class CompilerSetupPresenterTest {

    private static final String ID_1 = "id1";
    private static final String ID_2 = "id2";

    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";

    //constructor mocks
    @Mock
    private CompilerSetupView        view;
    @Mock
    private SettingsServiceClient    service;
    @Mock
    private PropertyWidgetFactory    propertyFactory;
    @Mock
    private JavaLocalizationConstant locale;

    @Mock
    private DirtyStateListener           dirtyStateListener;
    @Mock
    private Promise<Map<String, String>> mapPromise;
    @Mock
    private AcceptsOneWidget             container;
    @Mock
    private PropertyWidget               widget;

    @Captor
    private ArgumentCaptor<Map<String, String>>            mapCaptor;
    @Captor
    private ArgumentCaptor<Operation<Map<String, String>>> operationCaptor;

    @InjectMocks
    private CompilerSetupPresenter presenter;

    @Before
    public void setUp() {
        when(propertyFactory.create(anyString())).thenReturn(widget);
        when(service.getCompileParameters()).thenReturn(mapPromise);

        presenter.setUpdateDelegate(dirtyStateListener);

    }

    @Test
    public void constructorShouldBeVerified() {
        verify(locale).compilerSetup();
    }

    @Test
    public void pageShouldNotBeDirty() {
        boolean isDirty = presenter.isDirty();

        assertThat(isDirty, equalTo(false));
    }

    @Test
    public void changedValuesShouldBeSaved() {
        presenter.onPropertyChanged(ID_1, VALUE_1);
        presenter.onPropertyChanged(ID_2, VALUE_2);

        presenter.storeChanges();

        verify(service).applyCompileParameters(Matchers.<Map<String, String>>anyObject());

        assertThat(presenter.isDirty(), equalTo(false));
    }

    @Test
    public void changesShouldBeReverted() throws Exception {
        presenter.go(container);

        verify(mapPromise).then(operationCaptor.capture());
        operationCaptor.getValue().apply(getAllProperties());

        presenter.onPropertyChanged(COMPILER_UNUSED_IMPORT, VALUE_1);
        reset(widget);

        presenter.revertChanges();

        verify(widget, times(18)).selectPropertyValue(anyString());

        assertThat(presenter.isDirty(), equalTo(false));
    }

    private Map<String, String> getAllProperties() {
        Map<String, String> allProperties = new HashMap<>();

        allProperties.put(COMPILER_UNUSED_LOCAL, VALUE_1);
        allProperties.put(COMPILER_UNUSED_IMPORT, VALUE_2);
        allProperties.put(DEAD_CODE, VALUE_1);
        allProperties.put(METHOD_WITH_CONSTRUCTOR_NAME, VALUE_2);
        allProperties.put(UNNECESSARY_ELSE_STATEMENT, VALUE_1);
        allProperties.put(COMPARING_IDENTICAL_VALUES, VALUE_2);
        allProperties.put(NO_EFFECT_ASSIGNMENT, VALUE_1);
        allProperties.put(MISSING_SERIAL_VERSION_UID, VALUE_2);
        allProperties.put(TYPE_PARAMETER_HIDE_ANOTHER_TYPE, VALUE_1);
        allProperties.put(FIELD_HIDES_ANOTHER_VARIABLE, VALUE_2);
        allProperties.put(MISSING_DEFAULT_CASE, VALUE_1);
        allProperties.put(UNUSED_PRIVATE_MEMBER, VALUE_2);
        allProperties.put(UNCHECKED_TYPE_OPERATION, VALUE_1);
        allProperties.put(USAGE_OF_RAW_TYPE, VALUE_2);
        allProperties.put(MISSING_OVERRIDE_ANNOTATION, VALUE_1);
        allProperties.put(NULL_POINTER_ACCESS, VALUE_2);
        allProperties.put(POTENTIAL_NULL_POINTER_ACCESS, VALUE_1);
        allProperties.put(REDUNDANT_NULL_CHECK, VALUE_2);

        return allProperties;
    }

    @Test
    public void propertyShouldBeChanged() {
        presenter.onPropertyChanged(COMPILER_UNUSED_IMPORT, VALUE_2);

        verify(dirtyStateListener).onDirtyChanged();

        assertThat(presenter.isDirty(), equalTo(true));
    }

    @Test
    public void propertiesShouldBeDisplayed() throws Exception {
        presenter.go(container);

        verify(mapPromise).then(operationCaptor.capture());
        operationCaptor.getValue().apply(getAllProperties());

        verify(propertyFactory, times(18)).create(anyString());
        verify(widget, times(18)).selectPropertyValue(anyString());
        verify(widget, times(18)).setDelegate(presenter);
        verify(view, times(18)).addProperty(widget);
    }
}