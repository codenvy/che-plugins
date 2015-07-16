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

import com.google.gwt.i18n.client.Messages;

/**
 * Localization constants. Interface to represent the constants defined in resource bundle:
 * 'JavaLocalizationConstant.properties'.
 *
 * @author Artem Zatsarynnyy
 */
public interface JavaLocalizationConstant extends Messages {
    /* NewJavaClassView */
    @Key("title")
    String title();

    @Key("ok")
    String buttonOk();

    @Key("cancel")
    String buttonCancel();

    /* Actions */
    @Key("action.newClass.id")
    String actionNewClassId();

    @Key("action.newClass.title")
    String actionNewClassTitle();

    @Key("action.newClass.description")
    String actionNewClassDescription();

    @Key("action.newPackage.id")
    String actionNewPackageId();

    @Key("action.newPackage.title")
    String actionNewPackageTitle();

    @Key("action.newPackage.description")
    String actionNewPackageDescription();

    @Key("messages.newPackage.invalidName")
    String messagesNewPackageInvalidName();

    @Key("messages.file.successfully.parsed")
    String fileSuccessfullyParsed();

    @Key("messages.dependencies.successfully.updated")
    String dependenciesSuccessfullyUpdated();

    @Key("messages.dependencies.updating.dependencies")
    String updatingDependencies();

    @Key("action.quickdoc.title")
    String actionQuickdocTitle();

    @Key("action.quickdoc.description")
    String actionQuickdocDescription();

    @Key("action.openDeclaration.title")
    String actionOpenDeclarationTitle();

    @Key("action.openDeclaration.description")
    String actionOpenDeclarationDescription();
}
