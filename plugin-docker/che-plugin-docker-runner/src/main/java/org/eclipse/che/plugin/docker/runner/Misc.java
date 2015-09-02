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
package org.eclipse.che.plugin.docker.runner;

/**
 * Contains addition information about environment.
 *
 * @author andrew00x
 */
public class Misc {
    private String displayName;
    private String description;

    /** Description for embedded environment. It helps user to understand nature and possibilities of this environment. */
    public String getDescription() {
        return description;
    }

    /** Display name for embedded environment. */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
