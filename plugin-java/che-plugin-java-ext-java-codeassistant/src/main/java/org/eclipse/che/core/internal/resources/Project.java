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

package org.eclipse.che.core.internal.resources;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeMatcher;

import java.net.URI;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
public class Project extends Container implements IProject {

    protected Project(IPath path, Workspace workspace) {
        super(path, workspace);
    }

    @Override
    public void build(int i, String s, Map<String, String> map, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(IBuildConfiguration iBuildConfiguration, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        create(description, IResource.NONE, monitor);
    }

    @Override
    public void create(IProgressMonitor monitor) throws CoreException {
        create(null, monitor);
    }

    @Override
    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        workspace.createResource(this, updateFlags);
    }

    @Override
    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBuildConfiguration getBuildConfig(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectDescription getDescription() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectNature getNature(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPath getPluginWorkingLocation(IPluginDescriptor iPluginDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPath getWorkingLocation(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProject[] getReferencedProjects() throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProject[] getReferencingProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBuildConfiguration[] getReferencedBuildConfigs(String s, boolean b) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasBuildConfig(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNature(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNatureEnabled(String s) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
//        throw new UnsupportedOperationException();
        return true;
    }

    @Override
    public void loadSnapshot(int i, URI uri, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(IProjectDescription iProjectDescription, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open(int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open(IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveSnapshot(int i, URI uri, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(IProjectDescription iProjectDescription, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(IProjectDescription iProjectDescription, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultCharset(boolean b) throws CoreException {
        return "UTF-8";
    }

    @Override
    public int getType() {
        return PROJECT;
    }

    @Override
    public IContainer getParent() {
        return workspace.getRoot();
    }
}
