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
package org.eclipse.che.ide.ext.runner.client.tabs.templates.environment;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.TestUtil;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.tabs.common.item.ItemWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget.DEFAULT_DESCRIPTION;
import static org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget.CUSTOM_DESCRIPTION;
import static  org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static  org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;

/**
 * @author Andrienko Alexander
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class EnvironmentWidgetTest {
    private static final String TEXT = "text";
    private static final String ID   = "hh/ooo/text";

    @Mock
    private ItemWidget       itemWidget;
    @Mock
    private RunnerResources  resources;
    @Mock
    private SelectionManager selectionManager;
    @Mock
    private AppContext       appContext;

    @Mock
    private RunnerResources.RunnerCss css;
    @Mock
    private Environment               environment;
    @Mock
    private CurrentProject            currentProject;

    private EnvironmentWidget environmentWidget;

    @Before
    public void setUp() {
        SVGResource svgResource = mock(SVGResource.class, RETURNS_DEEP_STUBS);

        when(resources.scopeProject()).thenReturn(svgResource);
        when(resources.scopeSystem()).thenReturn(svgResource);
        when(resources.runnerCss()).thenReturn(css);
        when(css.environmentSvg()).thenReturn(TEXT);
        when(css.environmentSvg()).thenReturn(TEXT);

        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(environment.getId()).thenReturn(TEXT);

        environmentWidget = new EnvironmentWidget(itemWidget, resources, selectionManager, appContext);
    }

    @Test
    public void shouldValidateConstructor() {
        ArgumentCaptor<ItemWidget.ActionDelegate> delegateArgumentCaptor = ArgumentCaptor.forClass(ItemWidget.ActionDelegate.class);

        verify(resources).scopeProject();
        verify(resources).scopeSystem();
        verify(resources, times(2)).runnerCss();
        verify(css, times(2)).environmentSvg();

        verify(itemWidget).setDelegate(delegateArgumentCaptor.capture());
        ItemWidget.ActionDelegate delegate = delegateArgumentCaptor.getValue();
        delegate.onWidgetClicked();

        verify(selectionManager).setEnvironment(any(Environment.class));
    }

    @Test
    public void shouldSelect() {
        environmentWidget.select();

        verify(itemWidget).select();
    }

    @Test
    public void shouldUnSelect() {
        environmentWidget.unSelect();

        verify(itemWidget).unSelect();
    }

    @Test
    public void shouldUpdateWhenDescriptionNotNull() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(TEXT);

        environmentWidget.setScope(PROJECT);
        updateParameters();

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsNullAndCurrentProjectIsNullAndScopeIsProject() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(null);
        when(appContext.getCurrentProject()).thenReturn(null);

        environmentWidget.setScope(PROJECT);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(itemWidget).setDescription(CUSTOM_DESCRIPTION);

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsNullAndScopeIsProject() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(null);

        environmentWidget.setScope(PROJECT);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(currentProject).getRunner();
        verify(itemWidget).setDescription(CUSTOM_DESCRIPTION);

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsNullAndCurrentProjectIsNullAndScopeIsSystem() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(null);
        when(appContext.getCurrentProject()).thenReturn(null);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(itemWidget).setDescription(isNull(String.class));

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsEmptyLineAndCurrentProjectIsNullAndScopeIsSystem() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn("");
        when(appContext.getCurrentProject()).thenReturn(null);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(itemWidget).setDescription("");

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsDefaultScopeIsSystem() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(TEXT);
        when(currentProject.getRunner()).thenReturn(TEXT);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(itemWidget).setDescription(DEFAULT_DESCRIPTION);

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsDefaultScopeIsProject() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(TEXT);
        when(currentProject.getRunner()).thenReturn(TEXT);

        environmentWidget.setScope(PROJECT);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(itemWidget).setDescription(DEFAULT_DESCRIPTION);

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenDescriptionIsNullAndScopeIsSystem() {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(null);

        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(currentProject).getRunner();
        verify(itemWidget).setDescription(isNull(String.class));

        verify(itemWidget).setImage(any(SVGImage.class));
    }

    @Test
    public void shouldUpdateWhenScopeProject() throws Exception {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(TEXT);

        environmentWidget.setScope(PROJECT);
        updateParameters();

        SVGImage imageProjectScope = (SVGImage)TestUtil.getFieldValueByName(environmentWidget, "projectScope");

        verify(itemWidget).setImage(imageProjectScope);
    }

    @Test
    public void shouldUpdateWhenScopeSystem() throws Exception {
        when(environment.getName()).thenReturn(ID);
        when(environment.getDescription()).thenReturn(TEXT);

        environmentWidget.setScope(SYSTEM);
        updateParameters();

        SVGImage imageSystemScope = (SVGImage)TestUtil.getFieldValueByName(environmentWidget, "systemScope");

        verify(itemWidget).setImage(imageSystemScope);
    }

    @Test
    public void shouldGetAsWidget() {
        environmentWidget.asWidget();
        verify(itemWidget).asWidget();
    }

    private void updateParameters() {
        environmentWidget.update(environment);

        verify(environment).getName();

        verify(itemWidget).setName(ID);
        verify(environment).getDescription();
        verify(appContext).getCurrentProject();
        verify(currentProject).getRunner();
        verify(itemWidget).setDescription(TEXT);
    }

}