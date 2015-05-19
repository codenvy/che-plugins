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

import org.eclipse.che.core.internal.resources.ResourcesPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;

/**
 * @author Evgen Vidolob
 */
public class ResourceDeltaImpl implements IResourceDelta {

    protected static int KIND_MASK = 0xFF;
    private File workspace;
    private VirtualFileEvent event;
    protected int status;

    public ResourceDeltaImpl(File workspace, VirtualFileEvent event) {
        this.workspace = workspace;
        this.event = event;
//        status|= KIND_MASK;
        switch (event.getType()) {
            case CONTENT_UPDATED:
                status |= CHANGED | CONTENT;
                break;
            case CREATED:
                status |= ADDED;
                break;
            case DELETED:
                status |= REMOVED;
        }
    }

    @Override
    public File getFile() {
        return new File(workspace, event.getPath());
    }

    @Override
    public void accept(IResourceDeltaVisitor iResourceDeltaVisitor) throws CoreException {

    }

    @Override
    public void accept(IResourceDeltaVisitor iResourceDeltaVisitor, boolean b) throws CoreException {

    }

    @Override
    public void accept(IResourceDeltaVisitor visitor, int memberFlags) throws CoreException {
        final boolean includePhantoms = (memberFlags & IContainer.INCLUDE_PHANTOMS) != 0;
//        final boolean includeTeamPrivate = (memberFlags & IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS) != 0;
//        final boolean includeHidden = (memberFlags & IContainer.INCLUDE_HIDDEN) != 0;
//        int mask = includePhantoms ? ALL_WITH_PHANTOMS : REMOVED | ADDED | CHANGED;
//        if ((getKind() & mask) == 0)
//            return;
//        if (!visitor.visit(this))
//            return;
        visitor.visit(this);
    }

    @Override
    public org.eclipse.core.resources.IResourceDelta findMember(IPath iPath) {
        return null;
    }

    @Override
    public org.eclipse.core.resources.IResourceDelta[] getAffectedChildren() {
        return new IResourceDelta[0];
    }

    @Override
    public org.eclipse.core.resources.IResourceDelta[] getAffectedChildren(int i) {
        return new IResourceDelta[0];
    }

    @Override
    public org.eclipse.core.resources.IResourceDelta[] getAffectedChildren(int i, int i1) {
        return new IResourceDelta[0];
    }

    @Override
    public int getFlags() {
        return status & ~KIND_MASK;
    }

    @Override
    public IPath getFullPath() {
        return null;
    }

    @Override
    public int getKind() {
        return status & KIND_MASK;
    }

    @Override
    public IMarkerDelta[] getMarkerDeltas() {
        return new IMarkerDelta[0];
    }

    @Override
    public IPath getMovedFromPath() {
        return null;
    }

    @Override
    public IPath getMovedToPath() {
        return null;
    }

    @Override
    public IPath getProjectRelativePath() {
        return null;
    }

    @Override
    public IResource getResource() {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(event.getPath()));
//        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAdapter(Class aClass) {
        return null;
    }
}
