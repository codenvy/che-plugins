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
package org.eclipse.che.ide.ext.openshift.client.project.wizard.page.template;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Label;
import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.SpanElement;
import elemental.html.TableElement;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftResources;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.Parameter;
import org.eclipse.che.ide.ext.openshift.shared.dto.Template;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.util.dom.Elements;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;

/**
 * Implementation of view {@link SelectTemplateView}
 *
 * @author Vlad Zhukovskiy
 * @author Sergii Leschenko
 * @author Vitaliy Guliy
 */
@Singleton
public class SelectTemplateViewImpl implements SelectTemplateView {

    private static SelectTemplateViewImplUiBinder uiBinder = GWT.create(SelectTemplateViewImplUiBinder.class);

    interface SelectTemplateViewImplUiBinder extends UiBinder<DockPanel, SelectTemplateViewImpl> {
    }

    @UiField
    Label templatesLabel;

    @UiField
    Label loadingCategoriesLabel;

    @UiField
    ScrollPanel templatePanel;

    private SimpleList<Template>            templateList;
    private ActionDelegate                  delegate;
    private DockPanel                       widget;
    private DtoFactory                      dtoFactory;
    private OpenshiftLocalizationConstant   localizationConstant;

    @Inject
    public SelectTemplateViewImpl(Resources resources,
                                  final OpenshiftResources openshiftResources,
                                  final OpenshiftLocalizationConstant localizationConstant,
                                  DtoFactory dtoFactory) {
        widget = uiBinder.createAndBindUi(this);

        this.dtoFactory = dtoFactory;
        this.localizationConstant = localizationConstant;

        TableElement breakPointsElement = Elements.createTableElement();
        breakPointsElement.setAttribute("style", "width: 100%");

        templateList = SimpleList.create((SimpleList.View)breakPointsElement, resources.defaultSimpleListCss(),
                                         new SimpleList.ListItemRenderer<Template>() {
                                             @Override
                                             public void render(Element element, Template template) {
                                                 String tags = template.getMetadata().getAnnotations().get("tags");
                                                 tags = (tags != null) ? tags.replace(",", " ") : "";

                                                 DivElement title = Elements.createDivElement(openshiftResources.css().templateSectionTitle());
                                                 title.setTextContent(template.getMetadata().getName());
                                                 element.appendChild(title);

                                                 if (!hasValidBuildConfig(template)) {
                                                     title.getStyle().setColor(org.eclipse.che.ide.api.theme.Style.theme.getErrorColor());
                                                 }

                                                 DivElement description = Elements.createDivElement(openshiftResources.css().templateSectionDescription());
                                                 description.setTextContent(template.getMetadata().getAnnotations().get("description"));
                                                 element.appendChild(description);

                                                 DivElement namespace = Elements.createDivElement();
                                                 SpanElement namespaceTitle = Elements.createSpanElement(openshiftResources.css().templateSectionSecondary());
                                                 namespaceTitle.setTextContent("Namespace:");
                                                 namespaceTitle.getStyle().setMarginRight(10, "px");
                                                 namespace.appendChild(namespaceTitle);
                                                 SpanElement namespaceContent = Elements.createSpanElement();
                                                 namespaceContent.setTextContent(template.getMetadata().getNamespace());
                                                 namespace.appendChild(namespaceContent);
                                                 element.appendChild(namespace);

                                                 DivElement tag = Elements.createDivElement(openshiftResources.css().templateSectionTags(),
                                                                                            openshiftResources.css().templateSectionSecondary());
                                                 tag.setTextContent(tags);
                                                 element.appendChild(tag);

                                                 element.getClassList().add(openshiftResources.css().templateSection());
                                             }
                                         },
                                         new SimpleList.ListEventDelegate<Template>() {
                                             @Override
                                             public void onListItemClicked(Element listItemBase, Template itemData) {
                                                 templateList.getSelectionModel().setSelectedItem(itemData);
                                                 delegate.onTemplateSelected(itemData);
                                             }

                                             @Override
                                             public void onListItemDoubleClicked(Element listItemBase, Template itemData) {

                                             }
                                         });

        templatePanel.add(templateList);
    }

    /**
     * Determines whether template has properly set build configuration and contains Git URL to sources.
     *
     * @param template
     *         template to check
     * @return
     *         {@code true} if template has build configuration and points to sources, otherwise returns {@code false}
     */
    private boolean hasValidBuildConfig(Template template) {
        try {
            for (Object o : template.getObjects()) {
                final JSONObject object = (JSONObject)o;
                final String kind = ((JSONString)object.get("kind")).stringValue();
                if (kind.equals("BuildConfig")) {
                    BuildConfig buildConfig = dtoFactory.createDtoFromJson(object.toString(), BuildConfig.class);
                    String uri = buildConfig.getSpec().getSource().getGit().getUri();
                    if (uri.startsWith("${") && uri.endsWith("}")) {
                        String uriVariableName = uri.substring(2, uri.length() - 1);
                        for (Parameter parameter : template.getParameters()) {
                            if (uriVariableName.equals(parameter.getName())) {
                                return !Strings.isNullOrEmpty(parameter.getValue());
                            }
                        }
                        return false;
                    } else {
                        return !uri.isEmpty();
                    }
                }
            }
        } catch (Exception e) {
            Log.error(getClass(), e.getMessage());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void showLoadingTemplates() {
        // hide templates list
        templateList.asWidget().setVisible(false);
        templatesLabel.setVisible(false);

        // show loading label
        loadingCategoriesLabel.setVisible(true);
    }

    /** {@inheritDoc} */
    @Override
    public void setTemplates(List<Template> templates, boolean keepExisting) {
        // hide loading label
        loadingCategoriesLabel.setVisible(false);

        // show templates list
        templatesLabel.setVisible(true);
        templateList.asWidget().setVisible(true);
        templateList.render(templates);
    }
}
