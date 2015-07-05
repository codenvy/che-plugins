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
package org.eclipse.che.ide.ext.help.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

public interface Resources extends ClientBundle {

    @Source("about/logo.png")
    ImageResource logo();

    @Source("actions/about.svg")
    SVGResource about();

    @Source("actions/help.svg")
    SVGResource help();

    @Source("actions/forums.svg")
    SVGResource forums();

    @Source("actions/feature-vote.svg")
    SVGResource featureVote();
}