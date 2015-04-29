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
package org.eclipse.che.ide.ext.svn.client.inject;

import org.eclipse.che.ide.ext.svn.client.commit.diff.DiffViewerView;
import org.eclipse.che.ide.ext.svn.client.commit.diff.DiffViewerViewImpl;
import org.eclipse.che.ide.ext.svn.client.importer.SubversionProjectImporterView;
import org.eclipse.che.ide.ext.svn.client.importer.SubversionProjectImporterViewImpl;
import org.eclipse.che.ide.ext.svn.client.log.ShowLogsView;
import org.eclipse.che.ide.ext.svn.client.log.ShowLogsViewImpl;
import org.eclipse.che.ide.ext.svn.client.move.MoveView;
import org.eclipse.che.ide.ext.svn.client.move.MoveViewImpl;
import org.eclipse.che.ide.ext.svn.client.property.PropertyEditorView;
import org.eclipse.che.ide.ext.svn.client.property.PropertyEditorViewImpl;
import org.eclipse.che.ide.ext.svn.client.resolve.ResolveView;
import org.eclipse.che.ide.ext.svn.client.resolve.ResolveViewImpl;
import org.eclipse.che.ide.ext.svn.client.SubversionClientService;
import org.eclipse.che.ide.ext.svn.client.SubversionClientServiceImpl;
import org.eclipse.che.ide.ext.svn.client.askcredentials.AskCredentialsPresenter;
import org.eclipse.che.ide.ext.svn.client.askcredentials.AskCredentialsView;
import org.eclipse.che.ide.ext.svn.client.askcredentials.AskCredentialsViewImpl;
import org.eclipse.che.ide.ext.svn.client.commit.CommitView;
import org.eclipse.che.ide.ext.svn.client.commit.CommitViewImpl;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputView;
import org.eclipse.che.ide.ext.svn.client.common.RawOutputViewImpl;
import org.eclipse.che.ide.ext.svn.client.common.threechoices.ChoiceDialog;
import org.eclipse.che.ide.ext.svn.client.common.threechoices.ChoiceDialogFactory;
import org.eclipse.che.ide.ext.svn.client.common.threechoices.ChoiceDialogPresenter;
import org.eclipse.che.ide.ext.svn.client.common.threechoices.ChoiceDialogView;
import org.eclipse.che.ide.ext.svn.client.common.threechoices.ChoiceDialogViewImpl;
import org.eclipse.che.ide.ext.svn.client.copy.CopyView;
import org.eclipse.che.ide.ext.svn.client.copy.CopyViewImpl;
import org.eclipse.che.ide.ext.svn.client.common.filteredtree.FilteredNodeFactory;
import org.eclipse.che.ide.ext.svn.client.common.filteredtree.FilteredTreeStructureProvider;
import org.eclipse.che.ide.ext.svn.client.export.ExportView;
import org.eclipse.che.ide.ext.svn.client.export.ExportViewImpl;
import org.eclipse.che.ide.ext.svn.client.importer.SubversionImportWizardRegistrar;
import org.eclipse.che.ide.ext.svn.client.update.UpdateToRevisionView;
import org.eclipse.che.ide.ext.svn.client.update.UpdateToRevisionViewImpl;
import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.project.wizard.ImportWizardRegistrar;
import org.eclipse.che.ide.api.project.tree.TreeStructureProvider;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;

/**
 * Subversion Gin module.
 *
 * @author Jeremy Whitlock
 */
@ExtensionGinModule
public class SubversionGinModule extends AbstractGinModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(SubversionClientService.class).to(SubversionClientServiceImpl.class).in(Singleton.class);

        GinMultibinder.newSetBinder(binder(), ImportWizardRegistrar.class).addBinding()
                      .to(SubversionImportWizardRegistrar.class);

        bind(SubversionProjectImporterView.class).to(SubversionProjectImporterViewImpl.class).in(Singleton.class);

        bind(RawOutputView.class).to(RawOutputViewImpl.class).in(Singleton.class);
        bind(UpdateToRevisionView.class).to(UpdateToRevisionViewImpl.class).in(Singleton.class);
        bind(ResolveView.class).to(ResolveViewImpl.class).in(Singleton.class);
        bind(CopyView.class).to(CopyViewImpl.class).in(Singleton.class);
        bind(MoveView.class).to(MoveViewImpl.class).in(Singleton.class);
        bind(ExportView.class).to(ExportViewImpl.class).in(Singleton.class);
        bind(ShowLogsView.class).to(ShowLogsViewImpl.class).in(Singleton.class);
        bind(PropertyEditorView.class).to(PropertyEditorViewImpl.class).in(Singleton.class);

        install(new GinFactoryModuleBuilder().build(FilteredNodeFactory.class));
        GinMultibinder.newSetBinder(binder(), TreeStructureProvider.class).addBinding().to(FilteredTreeStructureProvider.class);

        bind(CommitView.class).to(CommitViewImpl.class).in(Singleton.class);
        bind(DiffViewerView.class).to(DiffViewerViewImpl.class).in(Singleton.class);

        bind(AskCredentialsPresenter.class);
        bind(AskCredentialsView.class).to(AskCredentialsViewImpl.class);

        install(new GinFactoryModuleBuilder().implement(ChoiceDialog.class, ChoiceDialogPresenter.class)
                                             .build(ChoiceDialogFactory.class));
        bind(ChoiceDialogView.class).to(ChoiceDialogViewImpl.class);
    }

}
