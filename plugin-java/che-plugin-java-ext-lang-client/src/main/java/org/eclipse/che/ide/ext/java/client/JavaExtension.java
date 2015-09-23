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
package org.eclipse.che.ide.ext.java.client;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.api.keybinding.KeyBuilder;
import org.eclipse.che.ide.ext.java.client.action.NewJavaSourceFileAction;
import org.eclipse.che.ide.ext.java.client.action.NewPackageAction;
import org.eclipse.che.ide.ext.java.client.action.OpenDeclarationAction;
import org.eclipse.che.ide.ext.java.client.action.QuickDocumentationAction;
import org.eclipse.che.ide.ext.java.client.refactoring.move.MoveAction;
import org.eclipse.che.ide.ext.java.client.refactoring.rename.RenameRefactoringAction;
import org.eclipse.che.ide.ext.java.shared.Constants;
import org.eclipse.che.ide.util.browser.UserAgent;
import org.eclipse.che.ide.util.input.KeyCodeMap;

import static org.eclipse.che.ide.api.action.IdeActions.GROUP_CODE;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_FILE_NEW;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_REFACTORING;

/** @author Evgen Vidolob */
@Extension(title = "Java", version = "3.0.0")
public class JavaExtension {


    @Inject
    public JavaExtension(FileTypeRegistry fileTypeRegistry,
                         @Named("JavaFileType") FileType javaFile,
                         @Named("JavaClassFileType") FileType classFile,
                         @Named("JspFileType") FileType jspFile) {
        JavaResources.INSTANCE.css().ensureInjected();

        fileTypeRegistry.registerFileType(javaFile);
        fileTypeRegistry.registerFileType(jspFile);
        fileTypeRegistry.registerFileType(classFile);
    }

    /** For test use only. */
    public JavaExtension() {
    }


    @Inject
    private void prepareActions(JavaLocalizationConstant localizationConstant,
                                NewPackageAction newPackageAction,
                                KeyBindingAgent keyBinding,
                                NewJavaSourceFileAction newJavaSourceFileAction,
                                ActionManager actionManager,
                                MoveAction moveAction,
                                RenameRefactoringAction renameRefactoringAction,
                                QuickDocumentationAction quickDocumentationAction,
                                OpenDeclarationAction openDeclarationAction) {
        // add actions in File -> New group
        actionManager.registerAction(localizationConstant.actionNewPackageId(), newPackageAction);
        actionManager.registerAction(localizationConstant.actionNewClassId(), newJavaSourceFileAction);
        DefaultActionGroup newGroup = (DefaultActionGroup)actionManager.getAction(GROUP_FILE_NEW);
        newGroup.addSeparator();
        newGroup.add(newJavaSourceFileAction);
        newGroup.add(newPackageAction);

        DefaultActionGroup refactorGroup = (DefaultActionGroup)actionManager.getAction(GROUP_REFACTORING);
        refactorGroup.addSeparator();
        refactorGroup.add(moveAction);
        refactorGroup.add(renameRefactoringAction);

        actionManager.registerAction("showQuickDoc", quickDocumentationAction);
        actionManager.registerAction("openJavaDeclaration", openDeclarationAction);
        actionManager.registerAction("javaRenameRefactoring", renameRefactoringAction);

        DefaultActionGroup codeGroup = (DefaultActionGroup)actionManager.getAction(GROUP_CODE);
        codeGroup.add(quickDocumentationAction, Constraints.LAST);
        codeGroup.add(openDeclarationAction, Constraints.LAST);
        if (UserAgent.isMac()) {
            keyBinding.getGlobal().addKey(new KeyBuilder().control().charCode('j').build(), "showQuickDoc");
        } else {
            keyBinding.getGlobal().addKey(new KeyBuilder().action().charCode('q').build(), "showQuickDoc");
        }
        keyBinding.getGlobal().addKey(new KeyBuilder().none().charCode(KeyCodeMap.F4).build(), "openJavaDeclaration");
        keyBinding.getGlobal().addKey(new KeyBuilder().shift().charCode(KeyCodeMap.F6).build(), "javaRenameRefactoring");
    }

    @Inject
    private void registerIcons(IconRegistry iconRegistry, JavaResources resources) {
        // icons for project tree nodes
        iconRegistry.registerIcon(new Icon("java.package", resources.packageIcon()));
        iconRegistry.registerIcon(new Icon("java.sourceFolder", resources.sourceFolder()));
        iconRegistry.registerIcon(new Icon("java.libraries", resources.librariesIcon()));
        iconRegistry.registerIcon(new Icon("java.jar", resources.jarIcon()));
        iconRegistry.registerIcon(new Icon("java.class", resources.javaClassIcon()));
        // icon for category in Wizard
        iconRegistry.registerIcon(new Icon(Constants.JAVA_CATEGORY + ".samples.category.icon", resources.javaCategoryIcon()));
    }
}
