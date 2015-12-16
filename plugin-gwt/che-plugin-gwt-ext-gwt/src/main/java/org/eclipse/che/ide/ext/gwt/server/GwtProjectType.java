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
package org.eclipse.che.ide.ext.gwt.server;

import com.google.inject.Inject;

import org.eclipse.che.api.project.server.type.AbstractProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenProjectType;

import static org.eclipse.che.ide.ext.gwt.shared.Constants.GWT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.ext.gwt.shared.Constants.GWT_PROJECT_TYPE_NAME;

/** @author Artem Zatsarynnyi */
public class GwtProjectType extends AbstractProjectType {

    @Inject
    public GwtProjectType(MavenProjectType mavenProjectType) {
        super(GWT_PROJECT_TYPE_ID, GWT_PROJECT_TYPE_NAME, true, false, true);
        addParent(mavenProjectType);
    }
}
