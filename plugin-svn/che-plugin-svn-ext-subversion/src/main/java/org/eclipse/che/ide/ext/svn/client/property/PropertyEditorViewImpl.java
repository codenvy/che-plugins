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
package org.eclipse.che.ide.ext.svn.client.property;

import com.google.common.base.Strings;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.svn.client.SubversionExtensionLocalizationConstants;
import org.eclipse.che.ide.ext.svn.client.SubversionExtensionResources;
import org.eclipse.che.ide.ext.svn.shared.Depth;
import org.eclipse.che.ide.ui.window.Window;

import java.util.ArrayList;
import java.util.List;


/**
 * Implementation of {@link org.eclipse.che.ide.ext.svn.client.property.PropertyEditorView}.
 *
 * @author Vladyslav Zhukovskyi
 */
@Singleton
public class PropertyEditorViewImpl extends Window implements PropertyEditorView {
    interface PropertyEditorViewImplUiBinder extends UiBinder<Widget, PropertyEditorViewImpl> {
    }

    Button btnOk;
    Button btnCancel;

    @UiField(provided = true)
    SuggestBox propertyList;

    @UiField
    RadioButton editProperty;

    @UiField
    RadioButton deleteProperty;

    @UiField
    TextArea propertyValue;

    @UiField
    ListBox depth;

    @UiField
    CheckBox force;

    @UiField(provided = true)
    SubversionExtensionResources             resources;
    @UiField(provided = true)
    SubversionExtensionLocalizationConstants constants;

    private PropertyEditorView.ActionDelegate delegate;

    @Inject
    public PropertyEditorViewImpl(SubversionExtensionResources resources,
                                  SubversionExtensionLocalizationConstants constants,
                                  PropertyEditorViewImplUiBinder uiBinder) {
        this.resources = resources;
        this.constants = constants;


        this.ensureDebugId("svn-property-edit-window");
        this.setTitle("Properties");

        btnCancel = createButton(constants.buttonCancel(), "svn-property-edit-cancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        btnCancel.setTabIndex(6);
        getFooter().add(btnCancel);

        btnOk = createButton("Ok", "svn-property-edit-ok", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onOkClicked();
            }
        });
        btnOk.setTabIndex(5);
        getFooter().add(btnOk);

        final String[] props = new String[]{
                "svn:ignore",
                "svn:keywords",
                "svn:mime-type",
                "svn:mergeinfo"
        };

        propertyList = new SuggestBox(new SuggestOracle() {
            @Override
            public void requestSuggestions(final Request request, Callback callback) {
                final List<Suggestion> suggestions = new ArrayList<>();
                for (final String prop : props) {
                    if (!prop.startsWith(request.getQuery())) {
                        continue;
                    }

                    suggestions.add(new SuggestOracle.Suggestion() {
                        /** {@inheritDoc} */
                        @Override
                        public String getDisplayString() {
                            return prop;
                        }

                        /** {@inheritDoc} */
                        @Override
                        public String getReplacementString() {
                            return prop;
                        }
                    });
                }

                callback.onSuggestionsReady(request, new Response(suggestions));
            }
        });

        propertyList.getValueBox().addKeyUpHandler(new KeyUpHandler() {
            /** {@inheritDoc} */
            @Override
            public void onKeyUp(KeyUpEvent event) {
                btnOk.setEnabled(!Strings.isNullOrEmpty(propertyList.getValue()));
            }
        });

        this.setWidget(uiBinder.createAndBindUi(this));

        for (Depth depth : Depth.values()) {
            this.depth.addItem(depth.getDescription(), depth.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
        hide();
        propertyList.setEnabled(false);
    }

    /** {@inheritDoc} */
    @Override
    public void onShow() {
        editProperty.setValue(true);
        propertyValue.setEnabled(true);
        propertyValue.setText(null);
        deleteProperty.setValue(false);
        propertyList.setEnabled(true);
        btnOk.setEnabled(false);
        force.setValue(false);
        depth.setSelectedIndex(0);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                propertyList.getValueBox().setValue("");
                propertyList.setFocus(true);
            }
        });
        show();
    }

    /** {@inheritDoc} */
    @Override
    public String getSelectedProperty() {
        return propertyList.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public Depth getDepth() {
        return Depth.from(depth.getSelectedValue());
    }

    /** {@inheritDoc} */
    @Override
    public String getPropertyValue() {
        return propertyValue.getText();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEditPropertySelected() {
        return editProperty.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeletePropertySelected() {
        return deleteProperty.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForceSelected() {
        return force.getValue();
    }

    @UiHandler({"editProperty", "deleteProperty"})
    @SuppressWarnings("unused")
    public void onModeChanged(ClickEvent event) {
        if (editProperty.getValue()) {
            propertyValue.setEnabled(true);
        } else if (deleteProperty.getValue()) {
            propertyValue.setEnabled(false);
        }
    }
}
