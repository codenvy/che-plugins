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

import org.eclipse.che.core.internal.utils.Policy;
import org.eclipse.che.core.resources.team.TeamHook;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
public class Workspace implements IWorkspace {
    /**
     * Work manager should never be accessed directly because accessor
     * asserts that workspace is still open.
     */
    protected WorkManager _workManager;
    protected final IWorkspaceRoot defaultRoot = new WorkspaceRoot(Path.ROOT, this);
    private String               wsPath;
    /**
     * Scheduling rule factory. This field is null if the factory has not been used
     * yet.  The accessor method should be used rather than accessing this field
     * directly.
     */
    private IResourceRuleFactory ruleFactory;

    /**
     * The currently installed team hook.
     */
    protected TeamHook teamHook = null;

    public Workspace(String path) {
        this.wsPath = path;
        _workManager = new WorkManager(this);
        _workManager.startup(null);
        _workManager.postWorkspaceStartup();
    }

    public String getAbsoluteWorkspacePath() {
        return wsPath;
    }

    public Resource newResource(IPath path, int type) {
        String message;
        switch (type) {
            case IResource.FOLDER:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FOLDER_SEGMENT_LENGTH) {
                    message = "Path must include project and resource name: " + path.toString(); //$NON-NLS-1$
                    Assert.isLegal(false, message);
                }
                return new Folder(path.makeAbsolute(), this);
            case IResource.FILE:
                if (path.segmentCount() < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
                    message = "Path must include project and resource name: " + path.toString(); //$NON-NLS-1$
                    Assert.isLegal(false, message);
                }
                return new File(path.makeAbsolute(), this);
            case IResource.PROJECT:
                return (Resource)getRoot().getProject(path.toOSString());
            case IResource.ROOT:
                return (Resource)getRoot();
        }
        Assert.isLegal(false);
        // will never get here because of assertion.
        return null;
    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener iResourceChangeListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener iResourceChangeListener, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant iSaveParticipant) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISavedState addSaveParticipant(String s, ISaveParticipant iSaveParticipant) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void build(IBuildConfiguration[] iBuildConfigurations, int i, boolean b, IProgressMonitor iProgressMonitor)
            throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkpoint(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProject[][] computePrerequisiteOrder(IProject[] iProjects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectOrder computeProjectOrder(IProject[] iProjects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus copy(IResource[] iResources, IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus copy(IResource[] iResources, IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus delete(IResource[] iResources, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus delete(IResource[] iResources, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMarkers(IMarker[] iMarkers) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forgetSavedTree(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectNatureDescriptor[] getNatureDescriptors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectNatureDescriptor getNatureDescriptor(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<IProject, IProject[]> getDanglingReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IWorkspaceDescription getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IWorkspaceRoot getRoot() {
        return defaultRoot;
    }

    @Override
    public IResourceRuleFactory getRuleFactory() {
        //note that the rule factory is created lazily because it
        //requires loading the teamHook extension
        if (ruleFactory == null)
            ruleFactory = new Rules(this);
        return ruleFactory;
    }

    @Override
    public ISynchronizer getSynchronizer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAutoBuilding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTreeLocked() {
        //todo
        return false;
    }

    @Override
    public IProjectDescription loadProjectDescription(IPath iPath) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectDescription loadProjectDescription(InputStream inputStream) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus move(IResource[] iResources, IPath iPath, boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus move(IResource[] iResources, IPath iPath, int i, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IBuildConfiguration newBuildConfig(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProjectDescription newProjectDescription(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeResourceChangeListener(IResourceChangeListener iResourceChangeListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSaveParticipant(Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSaveParticipant(String s) {
        throw new UnsupportedOperationException();
    }

    /**
     * Called before checking the pre-conditions of an operation.  Optionally supply
     * a scheduling rule to determine when the operation is safe to run.  If a scheduling
     * rule is supplied, this method will block until it is safe to run.
     *
     * @param rule the scheduling rule that describes what this operation intends to modify.
     */
    public void prepareOperation(ISchedulingRule rule, IProgressMonitor monitor) throws CoreException {
        try {
            //make sure autobuild is not running if it conflicts with this operation
            ISchedulingRule buildRule = getRuleFactory().buildRule();
//            if (rule != null && buildRule != null && (rule.isConflicting(buildRule) || buildRule.isConflicting(rule)))
//                buildManager.interrupt();
        } finally {
            getWorkManager().checkIn(rule, monitor);
        }
//        if (!isOpen()) {
//            String message = Messages.resources_workspaceClosed;
//            throw new ResourceException(IResourceStatus.OPERATION_FAILED, null, message, null);
//        }
    }

    /**
     * We should not have direct references to this field. All references should go through
     * this method.
     */
    public WorkManager getWorkManager() throws CoreException {
        if (_workManager == null) {
            String message = Messages.resources_shutdown;
            throw new ResourceException(new ResourceStatus(IResourceStatus.INTERNAL_ERROR, null, message));
        }
        return _workManager;
    }

    @Override
    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int options, IProgressMonitor monitor)
            throws CoreException {
        monitor = Policy.monitorFor(monitor);
        try {
            monitor.beginTask("", Policy.totalWork); //$NON-NLS-1$
            int depth = -1;
            boolean avoidNotification = (options & IWorkspace.AVOID_UPDATE) != 0;
            try {
                prepareOperation(rule, monitor);
                beginOperation(true);
//                if (avoidNotification)
//                    avoidNotification = notificationManager.beginAvoidNotify();
                depth = getWorkManager().beginUnprotected();
                action.run(Policy.subMonitorFor(monitor, Policy.opWork, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
            } catch (OperationCanceledException e) {
                getWorkManager().operationCanceled();
                throw e;
            } finally {
//                if (avoidNotification)
//                    notificationManager.endAvoidNotify();
                if (depth >= 0)
                    getWorkManager().endUnprotected(depth);
//                endOperation(rule, false, Policy.subMonitorFor(monitor, Policy.endOpWork));
            }
        } finally {
            monitor.done();
        }
    }

    public void beginOperation(boolean createNewTree) throws CoreException {
        WorkManager workManager = getWorkManager();
        workManager.incrementNestedOperations();
        if (!workManager.isBalanced())
            Assert.isTrue(false, "Operation was not prepared."); //$NON-NLS-1$
//        if (workManager.getPreparedOperationDepth() > 1) {
//            if (createNewTree && tree.isImmutable())
//                newWorkingTree();
//            return;
//        }
//        // stash the current tree as the basis for this operation.
//        operationTree = tree;
//        if (createNewTree && tree.isImmutable())
//            newWorkingTree();
    }

    /**
     * End an operation (group of resource changes).
     * Notify interested parties that resource changes have taken place.  All
     * registered resource change listeners are notified.  If autobuilding is
     * enabled, a build is run.
     */
    public void endOperation(ISchedulingRule rule, boolean build, IProgressMonitor monitor) throws CoreException {
        WorkManager workManager = getWorkManager();
        //don't do any end operation work if we failed to check in
        if (workManager.checkInFailed(rule))
            return;
        // This is done in a try finally to ensure that we always decrement the operation count
        // and release the workspace lock.  This must be done at the end because snapshot
        // and "hasChanges" comparison have to happen without interference from other threads.
        boolean hasTreeChanges = false;
        boolean depthOne = false;
        try {
            workManager.setBuild(build);
            // if we are not exiting a top level operation then just decrement the count and return
            depthOne = workManager.getPreparedOperationDepth() == 1;
//            if (!(notificationManager.shouldNotify() || depthOne)) {
//                notificationManager.requestNotify();
//                return;
//            }
            // do the following in a try/finally to ensure that the operation tree is nulled at the end
            // as we are completing a top level operation.
            try {
//                notificationManager.beginNotify();
                // check for a programming error on using beginOperation/endOperation
                Assert.isTrue(workManager.getPreparedOperationDepth() > 0, "Mismatched begin/endOperation"); //$NON-NLS-1$

                // At this time we need to re-balance the nested operations. It is necessary because
                // build() and snapshot() should not fail if they are called.
                workManager.rebalanceNestedOperations();

                //find out if any operation has potentially modified the tree
//                hasTreeChanges = workManager.shouldBuild();
                //double check if the tree has actually changed
//                if (hasTreeChanges)
//                    hasTreeChanges = operationTree != null && ElementTree.hasChanges(tree, operationTree, ResourceComparator
// .getBuildComparator(), true);
//                broadcastPostChange();
//                // Request a snapshot if we are sufficiently out of date.
//                saveManager.snapshotIfNeeded(hasTreeChanges);
            } finally {
//                // make sure the tree is immutable if we are ending a top-level operation.
//                if (depthOne) {
//                    tree.immutable();
//                    operationTree = null;
//                } else
//                    newWorkingTree();
            }
        } finally {
            workManager.checkOut(rule);
        }
//        if (depthOne)
//            buildManager.endTopLevel(hasTreeChanges);
    }


    @Override
    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
        run(action, defaultRoot, IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override
    public IStatus save(boolean b, IProgressMonitor iProgressMonitor) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(IWorkspaceDescription iWorkspaceDescription) throws CoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] sortNatureSet(String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateEdit(IFile[] iFiles, Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateFiltered(IResource iResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateLinkLocation(IResource iResource, IPath iPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateLinkLocationURI(IResource iResource, URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateName(String s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateNatureSet(String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validatePath(String s, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateProjectLocation(IProject iProject, IPath iPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus validateProjectLocationURI(IProject iProject, URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPathVariableManager getPathVariableManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAdapter(Class aClass) {
        throw new UnsupportedOperationException();
    }

    public java.io.File getFile(IPath path) {
        return new java.io.File(wsPath, path.toOSString());
    }

    public ResourceInfo getResourceInfo(IPath path) {
        java.io.File file = getFile(path);
        if (file.exists()) {
            return newElement(getType(file));
        }
        return null;
    }

    private int getType(java.io.File file) {
        if (file.isFile()) {
            return IResource.FILE;
        } else {
            java.io.File codenvy = new java.io.File(file, ".codenvy");
            if (codenvy.exists()) {
                return IResource.PROJECT;
            } else {
                return IResource.FOLDER;
            }
        }
    }

    /**
     * Create and return a new tree element of the given type.
     */
    protected ResourceInfo newElement(int type) {
        ResourceInfo result = null;
        switch (type) {
            case IResource.FILE:
            case IResource.FOLDER:
                result = new ResourceInfo(type);
                break;
            case IResource.PROJECT:
                result = new ResourceInfo(type);
                break;
            case IResource.ROOT:
                result = new ResourceInfo(type);
                break;
        }

        return result;
    }

    public IResource[] getChildren(IPath path) {
        java.io.File file = getFile(path);
        if (file.exists() && file.isDirectory()) {
            java.io.File[] list = file.listFiles();
            if (list != null) {
                IResource[] resources = new IResource[list.length];
                for (int i = 0; i < list.length; i++) {

                    java.io.File child = list[i];
                    IPath iPath = new Path(child.getPath().substring(wsPath.length()));
                    resources[i] = newResource(iPath, getType(child));
                }
                return resources;
            }
        }

        return ICoreConstants.EMPTY_RESOURCE_ARRAY;
    }

//    public java.io.File getWorkspaceFile() {
//        return workspaceFile;
//    }

    public void createResource(IResource resource, int updateFlags) throws CoreException {
        switch (resource.getType()) {
            case IResource.FILE:
                java.io.File file = new java.io.File(wsPath, resource.getFullPath().toOSString());
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new CoreException(new Status(0, JavaPlugin.getPluginId(), e.getMessage(), e));
                }
            case IResource.FOLDER:
                java.io.File folder = new java.io.File(wsPath, resource.getFullPath().toOSString());
                folder.mkdirs();
                break;
            case IResource.PROJECT:
                java.io.File project = new java.io.File(wsPath, resource.getFullPath().toOSString());
                project.mkdirs();
                java.io.File codenvy = new java.io.File(project, ".codenvy");
                codenvy.mkdir();
                break;
            default:
                throw new UnsupportedOperationException();
        }


    }

    public void setFileContent(File file, InputStream content) {
        java.io.File ioFile = getFile(file.getFullPath());
        try (FileOutputStream outputStream = new FileOutputStream(ioFile)) {
            FileUtil.transferStreams(content, outputStream, file.getFullPath().toOSString(), null);
        } catch (IOException | CoreException e) {
            JavaPlugin.log(e);
        }

    }


    public TeamHook getTeamHook() {
        // default to use Core's implementation
        //create anonymous subclass because TeamHook is abstract
        if (teamHook == null)
            teamHook = new TeamHook() {
                // empty
            };
        return teamHook;
    }

    public void delete(Resource resource) {
        java.io.File file = getFile(resource.getFullPath());
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                deleteDirectory(file);
            }

        }
    }

    private static boolean deleteDirectory(java.io.File directory) {
        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    void write(File file, InputStream content, int updateFlags, boolean append, IProgressMonitor monitor) throws CoreException{
        try {
            java.io.File ioFile = getFile(file.getFullPath());
            if (!ioFile.exists()) {
                ioFile.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(ioFile);
            FileUtil.transferStreams(content, outputStream, file.getFullPath().toOSString(), monitor);
        } catch (IOException e) {
            throw new CoreException(new Status(0, "", e.getMessage(), e));
        }
    }

    public void standardMoveFile(IFile file, IFile destination, int updateFlags, IProgressMonitor monitor) throws CoreException{
        java.io.File ioFile = getFile(file.getFullPath());
        java.io.File ioDestination = getFile(destination.getFullPath());
        try {
            Files.move(ioFile.toPath(), ioDestination.toPath());
        } catch (IOException e) {
           throw new CoreException(new Status(IStatus.ERROR, "", "Can't move file: " + file.getFullPath() + " to: " + destination.getFullPath(), e));
        }
    }

    public void standardMoveFolder(IFolder folder, IFolder destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        java.io.File ioFile = getFile(folder.getFullPath());
        java.io.File ioDestination = getFile(destination.getFullPath());
        try {
            Files.move(ioFile.toPath(), ioDestination.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, "", "Can't move folder: " + folder.getFullPath() + " to: " + destination.getFullPath(), e));
        }
    }

    public void standardMoveProject(IProject project, IProjectDescription description, int updateFlags, IProgressMonitor monitor) {
        throw new UnsupportedOperationException("standardMoveProject");
    }
}
