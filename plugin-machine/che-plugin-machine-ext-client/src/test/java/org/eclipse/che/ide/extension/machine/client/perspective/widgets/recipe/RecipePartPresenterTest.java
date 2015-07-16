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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe;

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.recipe.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeUpdate;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.MachineResources;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.container.RecipesContainerPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor.RecipeEditorPanel;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.entry.RecipeWidget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RecipePartPresenterTest {
    private static final String RECIPE_TYPE = "docker";

    //constructor mocks
    @Mock
    private RecipePartView              recipePartView;
    @Mock
    private MachineResources            machineResources;
    @Mock
    private EventBus                    eventBus;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private RecipeServiceClient         service;
    @Mock
    private MachineLocalizationConstant locale;
    @Mock
    private RecipesContainerPresenter   recipesContainerPresenter;
    @Mock
    private DtoFactory                  dtoFactory;

    //additional mocks
    @Mock
    private RecipeDescriptor     recipeDescriptor1;
    @Mock
    private RecipeDescriptor     recipeDescriptor2;
    @Mock
    private RecipeWidget         recipeWidget;
    @Mock
    private RecipeEditorPanel    recipeEditorPanel;
    @Mock
    private OMSVGSVGElement      omsvgsvgElement;
    @Mock
    private SVGResource          svgResource;
    @Mock
    private MachineResources.Css css;

    @Mock
    private Promise<List<RecipeDescriptor>> recipePromises;
    @Mock
    private Promise<RecipeDescriptor>       recipeDescriptorPromise;
    @Mock
    private Promise<RecipeDescriptor>       savedRecipeDescriptorPromise;
    @Mock
    private Promise<Void>                   deletePromise;

    @Captor
    private ArgumentCaptor<Operation<List<RecipeDescriptor>>> operationListDescriptorCaptor;
    @Captor
    private ArgumentCaptor<Operation<RecipeDescriptor>>       operationDescriptorCaptor;
    @Captor
    private ArgumentCaptor<Operation<RecipeDescriptor>>       savedDescriptorCaptor;
    @Captor
    private ArgumentCaptor<Operation<Void>>                   deleteCaptor;

    RecipePartPresenter recipePartPresenter;

    @Before
    public void setUp() throws Exception {
        when(machineResources.projectPerspective()).thenReturn(svgResource);
        when(svgResource.getSvg()).thenReturn(omsvgsvgElement);
        when(machineResources.getCss()).thenReturn(css);

        recipePartPresenter = new RecipePartPresenter(recipePartView,
                                                      machineResources,
                                                      eventBus,
                                                      notificationManager,
                                                      locale,
                                                      recipesContainerPresenter,
                                                      dtoFactory,
                                                      service);
    }

    @Test
    public void constructorShouldBePerform() throws Exception {
        verify(recipePartView).setDelegate(recipePartPresenter);
        verify(eventBus).addHandler(ActivePartChangedEvent.TYPE, recipePartPresenter);
    }

    @Test
    public void recipesShouldBeShowed() throws Exception {
        when(service.getAllRecipes()).thenReturn(recipePromises);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        recipePartPresenter.showRecipes();

        verify(service).getAllRecipes();

        verify(recipePromises).then(operationListDescriptorCaptor.capture());
        operationListDescriptorCaptor.getValue().apply(Arrays.asList(recipeDescriptor1, recipeDescriptor2));

        verify(recipePartView).clear();
        verify(recipePartView, times(2)).addRecipe(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter, times(2)).addRecipePanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter).showEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter).getEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipeEditorPanel).setDelegate(recipePartPresenter);
    }

    @Test
    public void stubPanelShouldBeShowed() throws Exception {
        when(service.getAllRecipes()).thenReturn(recipePromises);
        when(recipesContainerPresenter.getEditorStubPanel()).thenReturn(recipeEditorPanel);

        recipePartPresenter.showRecipes();

        verify(service).getAllRecipes();

        verify(recipePromises).then(operationListDescriptorCaptor.capture());
        operationListDescriptorCaptor.getValue().apply(Collections.<RecipeDescriptor>emptyList());

        verify(recipePartView).clear();
        verify(recipesContainerPresenter).getEditorStubPanel();
        verify(recipeEditorPanel).setDelegate(recipePartPresenter);
        verify(recipesContainerPresenter).showEditorStubPanel();
    }

    @Test
    public void firstRecipeShouldBeCreated() throws Exception {
        NewRecipe newRecipe = mock(NewRecipe.class);
        when(dtoFactory.createDto(NewRecipe.class)).thenReturn(newRecipe);
        when(newRecipe.withType(anyString())).thenReturn(newRecipe);
        when(newRecipe.withScript(anyString())).thenReturn(newRecipe);
        when(newRecipe.withTags(Matchers.<List<String>>any())).thenReturn(newRecipe);
        when(service.createRecipe(newRecipe)).thenReturn(recipeDescriptorPromise);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        recipePartPresenter.onCreateButtonClicked();

        verify(service).createRecipe(newRecipe);
        verify(newRecipe).withType(RECIPE_TYPE);
        verify(newRecipe).withScript("{}");
        assertTrue(newRecipe.getTags().isEmpty());

        verify(recipeDescriptorPromise).then(operationDescriptorCaptor.capture());
        operationDescriptorCaptor.getValue().apply(recipeDescriptor1);

        verify(recipePartView).addRecipe(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter).addRecipePanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter).showEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter).getEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipeEditorPanel).setDelegate(recipePartPresenter);
    }

    @Test
    public void secondRecipeShouldBeCreated() throws Exception {
        NewRecipe newRecipe = mock(NewRecipe.class);
        when(dtoFactory.createDto(NewRecipe.class)).thenReturn(newRecipe);
        when(newRecipe.withType(anyString())).thenReturn(newRecipe);
        when(newRecipe.withScript(anyString())).thenReturn(newRecipe);
        when(newRecipe.withTags(Matchers.<List<String>>any())).thenReturn(newRecipe);
        when(service.createRecipe(newRecipe)).thenReturn(recipeDescriptorPromise);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        recipePartPresenter.onCreateButtonClicked();

        verify(recipeDescriptorPromise).then(operationDescriptorCaptor.capture());
        operationDescriptorCaptor.getValue().apply(recipeDescriptor1);

        recipePartPresenter.onCreateButtonClicked();

        verify(recipeDescriptor1).getType();
        verify(recipeDescriptor1).getScript();
        verify(recipeDescriptor1).getTags();

        verify(newRecipe).withType(RECIPE_TYPE);
        verify(newRecipe).withScript("{}");
        assertTrue(newRecipe.getTags().isEmpty());

        verify(service, times(2)).createRecipe(newRecipe);

        verify(recipeDescriptorPromise, times(2)).then(operationDescriptorCaptor.capture());
        operationDescriptorCaptor.getValue().apply(recipeDescriptor1);

        verify(recipePartView, times(2)).addRecipe(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter, times(2)).addRecipePanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter, times(2)).showEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipesContainerPresenter, times(2)).getEditorPanel(Matchers.<RecipeWidget>any());
        verify(recipeEditorPanel, times(2)).setDelegate(recipePartPresenter);
    }

    @Test
    public void recipeShouldBeDeleted() throws Exception {
        NewRecipe newRecipe = mock(NewRecipe.class);
        when(dtoFactory.createDto(NewRecipe.class)).thenReturn(newRecipe);
        when(newRecipe.withType(anyString())).thenReturn(newRecipe);
        when(newRecipe.withScript(anyString())).thenReturn(newRecipe);
        when(newRecipe.withTags(Matchers.<List<String>>any())).thenReturn(newRecipe);
        when(service.createRecipe(newRecipe)).thenReturn(recipeDescriptorPromise);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        when(recipeDescriptor1.getId()).thenReturn("id");
        when(service.removeRecipe(anyString())).thenReturn(deletePromise);

        recipePartPresenter.onCreateButtonClicked();

        verify(recipeDescriptorPromise).then(operationDescriptorCaptor.capture());
        operationDescriptorCaptor.getValue().apply(recipeDescriptor1);

        recipePartPresenter.onDeleteButtonClicked();

        verify(deletePromise).then(deleteCaptor.capture());
    }

    @Test
    public void recipeShouldBeSaved() throws Exception {
        NewRecipe newRecipe = mock(NewRecipe.class);
        List<String> tags = Arrays.asList("tag");

        when(dtoFactory.createDto(NewRecipe.class)).thenReturn(newRecipe);
        when(newRecipe.withType(anyString())).thenReturn(newRecipe);
        when(newRecipe.withScript(anyString())).thenReturn(newRecipe);
        when(newRecipe.withTags(Matchers.<List<String>>any())).thenReturn(newRecipe);
        when(service.createRecipe(newRecipe)).thenReturn(recipeDescriptorPromise);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        RecipeUpdate recipeUpdate = mock(RecipeUpdate.class);
        when(dtoFactory.createDto(RecipeUpdate.class)).thenReturn(recipeUpdate);
        when(recipeUpdate.withType(anyString())).thenReturn(recipeUpdate);
        when(recipeUpdate.withScript(anyString())).thenReturn(recipeUpdate);
        when(recipeUpdate.withTags(Matchers.<List<String>>any())).thenReturn(recipeUpdate);
        when(recipeDescriptor1.getId()).thenReturn("id");
        when(recipeDescriptor1.getScript()).thenReturn("script");
        when(recipeDescriptor1.getTags()).thenReturn(tags);
        when(service.updateRecipe("id", recipeUpdate)).thenReturn(savedRecipeDescriptorPromise);
        when(recipeDescriptor1.getType()).thenReturn(RECIPE_TYPE);
        when(recipeEditorPanel.getScript()).thenReturn("script");
        when(recipeEditorPanel.getTags()).thenReturn(tags);

        recipePartPresenter.onCreateButtonClicked();

        verify(recipeDescriptorPromise).then(operationDescriptorCaptor.capture());
        operationDescriptorCaptor.getValue().apply(recipeDescriptor1);

        recipePartPresenter.onSaveButtonClicked();

        verify(service).updateRecipe("id", recipeUpdate);
        verify(savedRecipeDescriptorPromise).then(savedDescriptorCaptor.capture());
        savedDescriptorCaptor.getValue().apply(recipeDescriptor1);
        verify(recipeDescriptor1).setScript("script");
        verify(recipeDescriptor1).setTags(recipeDescriptor1.getTags());
        verify(notificationManager).showInfo("Recipe id was saved.");
    }

    @Test
    public void titleShouldBeReturned() throws Exception {
        recipePartPresenter.getTitle();

        verify(locale).viewRecipePanelTitle();
    }

    @Test
    public void panelShouldBeHidden() throws Exception {
        recipePartPresenter.setVisible(false);

        verify(recipePartView).setVisible(false);
    }

    @Test
    public void toolTipShouldBeShowed() throws Exception {
        recipePartPresenter.getTitleToolTip();

        verify(locale).viewRecipePanelTooltip();
    }

    @Test
    public void panelShouldBeShowedWhenPartIsActivated() throws Exception {
        ActivePartChangedEvent event = mock(ActivePartChangedEvent.class);
        when(event.getActivePart()).thenReturn(recipePartPresenter);
        when(recipesContainerPresenter.getEditorPanel(Matchers.<RecipeWidget>any())).thenReturn(recipeEditorPanel);

        recipePartPresenter.onActivePartChanged(event);

        verify(recipeEditorPanel).setDelegate(recipePartPresenter);
    }
}