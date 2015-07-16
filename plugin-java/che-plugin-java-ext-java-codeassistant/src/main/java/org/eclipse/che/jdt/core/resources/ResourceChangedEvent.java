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

package org.eclipse.che.jdt.core.resources;

import org.eclipse.che.api.vfs.server.observation.VirtualFileEvent;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IResourceDelta;

import java.io.File;

/**
 * @author Evgen Vidolob
 */
public class ResourceChangedEvent implements IResourceChangeEvent {


    private VirtualFileEvent event;
    private ResourceDeltaImpl resourceDelta;

    public ResourceChangedEvent(File workspace, VirtualFileEvent event) {
        this.event = event;
        resourceDelta = new ResourceDeltaImpl(workspace, event);

    }

    @Override
    public IMarkerDelta[] findMarkerDeltas(String s, boolean b) {
        return new IMarkerDelta[0];
    }

    @Override
    public int getBuildKind() {
        return 0;
    }

    @Override
    public IResourceDelta getDelta() {
        return resourceDelta;
    }

    @Override
    public IResource getResource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getType() {
        return POST_CHANGE;
    }
}
