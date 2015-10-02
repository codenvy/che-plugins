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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: $
 */
public interface JavaResources extends ClientBundle {
    JavaResources INSTANCE = GWT.create(JavaResources.class);


    @Source("java.css")
    JavaCss css();

    @Source("org/eclipse/che/ide/ext/java/client/images/annotation.gif")
    ImageResource annotationItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/class.gif")
    ImageResource classItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/innerinterface_public.gif")
    ImageResource interfaceItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/enum_item.gif")
    ImageResource enumItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/default-field.png")
    ImageResource defaultField();

    @Source("org/eclipse/che/ide/ext/java/client/images/private-field.png")
    ImageResource privateField();

    @Source("org/eclipse/che/ide/ext/java/client/images/protected-field.png")
    ImageResource protectedField();

    @Source("org/eclipse/che/ide/ext/java/client/images/public-field.png")
    ImageResource publicField();

    @Source("org/eclipse/che/ide/ext/java/client/images/blank.png")
    ImageResource blankImage();

    @Source("org/eclipse/che/ide/ext/java/client/images/default-method.png")
    ImageResource defaultMethod();

    @Source("org/eclipse/che/ide/ext/java/client/images/private-method.png")
    ImageResource privateMethod();

    @Source("org/eclipse/che/ide/ext/java/client/images/protected-method.png")
    ImageResource protectedMethod();

    @Source("org/eclipse/che/ide/ext/java/client/images/public-method.png")
    ImageResource publicMethod();

    @Source("org/eclipse/che/ide/ext/java/client/images/package.png")
    ImageResource packageItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/import.png")
    ImageResource importItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/imports.png")
    ImageResource imports();

    @Source("org/eclipse/che/ide/ext/java/client/images/local.png")
    ImageResource variable();

    @Source("org/eclipse/che/ide/ext/java/client/images/row-selected.png")
    ImageResource itemSelected();

    @Source("org/eclipse/che/ide/ext/java/client/images/jsp-tag.png")
    ImageResource jspTagItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/class-private.png")
    ImageResource classPrivateItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/class-protected.png")
    ImageResource classProtectedItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/class-default.png")
    ImageResource classDefaultItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/clock.png")
    ImageResource clockItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/groovy-tag.png")
    ImageResource groovyTagItem();

    @Source("org/eclipse/che/ide/ext/java/client/images/java.png")
    ImageResource java();

    @Source("org/eclipse/che/ide/ext/java/client/images/newJavaclass_wiz.gif")
    ImageResource newClassWizz();

    @Source("org/eclipse/che/ide/ext/java/client/images/outline.png")
    ImageResource outline();

    @Source("org/eclipse/che/ide/ext/java/client/images/loader.gif")
    ImageResource loader();

    @Source("org/eclipse/che/ide/ext/java/client/images/template.png")
    ImageResource template();

    @Source("org/eclipse/che/ide/ext/java/client/images/package_Disabled.png")
    ImageResource packageDisabled();

    @Source("org/eclipse/che/ide/ext/java/client/images/breakpoint-current.gif")
    ImageResource breakpointCurrent();

    @Source("org/eclipse/che/ide/ext/java/client/images/breakpoint.gif")
    ImageResource breakpoint();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/correction_change.gif")
    ImageResource correction_change();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/delete_obj.gif")
    ImageResource delete_obj();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/correction_cast.gif")
    ImageResource correction_cast();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/local.png")
    ImageResource local_var();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/correction_delete_import.gif")
    ImageResource correction_delete_import();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/field_public_obj.gif")
    ImageResource field_public();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/imp_obj.gif")
    ImageResource imp_obj();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/add_obj.gif")
    ImageResource add_obj();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/remove_correction.gif")
    ImageResource remove_correction();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/jexception_obj.gif")
    ImageResource exceptionProp();

    @Source("org/eclipse/che/ide/ext/java/client/internal/text/correction/proposals/javadoc.gif")
    ImageResource javadoc();

    @Source("org/eclipse/che/ide/ext/java/client/images/taskmrk.gif")
    ImageResource taskmrk();

    @Source("org/eclipse/che/ide/ext/java/client/images/javaProj.png")
    ImageResource javaProject();

    @Source("org/eclipse/che/ide/ext/java/client/images/newProjJava.png")
    ImageResource newJavaProject();

    @Source("svg/mark-error.svg")
    SVGResource markError();

    @Source("svg/mark-warning.svg")
    SVGResource markWarning();

    @Source("svg/close-folder.svg")
    SVGResource closeFolder();

    @Source("svg/css.svg")
    SVGResource cssFile();

    @Source("svg/html.svg")
    SVGResource htmlFile();

    @Source("svg/image-icon.svg")
    SVGResource imageIcon();

    @Source("svg/java.svg")
    SVGResource javaFile();

    @Source("svg/js.svg")
    SVGResource jsFile();

    @Source("svg/jsf.svg")
    SVGResource jsfFile();

    @Source("svg/json.svg")
    SVGResource jsonFile();

    @Source("svg/jsp.svg")
    SVGResource jspFile();


    @Source("svg/open-folder.svg")
    SVGResource openFolder();

    @Source("svg/package.svg")
    SVGResource packageIcon();

    @Source("svg/source-folder.svg")
    SVGResource sourceFolder();

    @Source("svg/text.svg")
    SVGResource textFile();

    @Source("svg/update-dependencies.svg")
    SVGResource updateDependencies();

    @Source("svg/category/java.svg")
    SVGResource javaCategoryIcon();

    @Source("svg/libraries.svg")
    SVGResource librariesIcon();

    @Source("svg/jar10.svg")
    SVGResource jarIcon();

    @Source("svg/class.svg")
    SVGResource javaClassIcon();

    @Source("internal/text/correction/proposals/correction_linked_rename.gif")
    ImageResource  linkedRename();

    @Source("svg/srcFolder.svg")
    SVGResource srcFolder();

    @Source("svg/testSrcFolder.svg")
    SVGResource testSrcFolder();

    @Source("svg/packageFolder.svg")
    SVGResource packageFolder();

    @Source("svg/fileJava.svg")
    SVGResource fileJava();

    @Source("svg/resourceFolder.svg")
    SVGResource resourceFolder();
}
