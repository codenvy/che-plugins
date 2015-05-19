/*******************************************************************************
 * Copyright (c) 2014-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.codemirror.style.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface CodeMirrorResource extends ClientBundle {

    @Source({"cm_highlight.css", "org/eclipse/che/ide/api/ui/style.css"})
    CssResource highlightStyle();

    @Source({"cm_theme.css", "org/eclipse/che/ide/api/ui/style.css"})
    CssResource editorStyle();

    @Source({"cm_gutters.css", "org/eclipse/che/ide/api/ui/style.css"})
    CssResource gutterStyle();

    @Source({"cm_simplescrollbars.css", "org/eclipse/che/ide/api/ui/style.css"})
    CssResource scrollStyle();

    @Source("org/eclipse/che/ide/texteditor/squiggle.gif")
    ImageResource squiggle();

    @Source({"cm_highlight_dockerfile.css", "org/eclipse/che/ide/api/ui/style.css"})
    CssResource dockerfileModeStyle();
}
