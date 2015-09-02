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

package org.eclipse.che.jdt.refactoring;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;

import org.eclipse.che.commons.schedule.ScheduleRate;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.MoveSettings;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameRefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;
import org.eclipse.che.ide.util.UUID;
import org.eclipse.che.jdt.refactoring.session.MoveRefactoringSession;
import org.eclipse.che.jdt.refactoring.session.RefactoringSession;
import org.eclipse.che.jdt.refactoring.session.ReorgRefactoringSession;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IConfirmQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.ui.refactoring.RenameSupport;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.internal.ui.refactoring.AbstractChangeNode;
import org.eclipse.ltk.internal.ui.refactoring.PreviewNode;

import java.util.concurrent.TimeUnit;

import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination.DestinationType;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class RefactoringManager {

    private final Cache<String, RefactoringSession> sessions;

    public RefactoringManager() {
        sessions = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).removalListener(notification -> {

        }).build();
    }

    /**
     * Create move refactoring session.
     *
     * @param javaElements the java elements
     * @return the ID of the refactoring session
     */
    public String createMoveRefactoringSession(IJavaElement[] javaElements) throws JavaModelException, RefactoringException {
        IReorgPolicy.IMovePolicy policy = ReorgPolicyFactory.createMovePolicy(new IResource[0], javaElements);
        if (policy.canEnable()) {
            JavaMoveProcessor processor = new JavaMoveProcessor(policy);
            processor.setReorgQueries(new IReorgQueries() {
                @Override
                public IConfirmQuery createSkipQuery(String queryTitle, int queryID) {
                    return null;
                }

                @Override
                public IConfirmQuery createYesNoQuery(String queryTitle, boolean allowCancel, int queryID) {
                    return null;
                }

                @Override
                public IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, boolean allowCancel, int queryID) {
                    return null;
                }
            });
            processor.setCreateTargetQueries(() -> null);
            Refactoring refactoring = new MoveRefactoring(processor);
            MoveRefactoringSession session = new MoveRefactoringSession(refactoring, processor);
            String uuid = UUID.uuid();
            sessions.put(uuid, session);
            return uuid;
        } else {
            throw new RefactoringException("Can't create move refactoring session.");
        }
    }

    /**
     * Periodically cleanup cache, to avoid memory leak.
     */
    @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
    void cacheClenup() {
        sessions.cleanUp();
    }


    public RefactoringStatus setRefactoringDestination(ReorgDestination destination) throws RefactoringException, JavaModelException {
        RefactoringSession session = getRefactoringSession(destination.getSessionId());
        if (!(session instanceof ReorgRefactoringSession)) {
            throw new RefactoringException("Can't set destination on none reorg refactoring session.");
        }

        ReorgRefactoringSession rs = ((ReorgRefactoringSession)session);
        Object dest = getDestination(destination.getProjectPath(), destination.getType(), destination.getDestination());
        org.eclipse.ltk.core.refactoring.RefactoringStatus refactoringStatus = rs.verifyDestination(dest);

        return DtoConverter.toRefactoringStatusDto(refactoringStatus);
    }


    private RefactoringSession getRefactoringSession(String sessionId) throws RefactoringException {
        RefactoringSession session = sessions.getIfPresent(sessionId);
        if (session == null) {
            throw new RefactoringException("Can't find refactoring session.");
        }
        return session;
    }

    private Object getDestination(String projectPath, DestinationType type, String destination)
            throws RefactoringException, JavaModelException {
        IJavaProject javaProject = JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(projectPath);
        if (javaProject == null) {
            throw new RefactoringException("Can't find project: " + projectPath);
        }
        switch (type) {
            case PACKAGE:
               return javaProject.findPackageFragment(new Path(destination));

            case RESOURCE:
            case SOURCE_REFERENCE:
            default:
                throw new UnsupportedOperationException("Can't use destination for 'RESOURCE' or 'SOURCE_REFERENCE'.");

        }
    }

    public void setMoveSettings(MoveSettings settings) throws RefactoringException {
        RefactoringSession session = getRefactoringSession(settings.getSessionId());
        if (!(session instanceof MoveRefactoringSession)) {
            throw new RefactoringException("Can't set move on none move refactoring session.");
        }

        MoveRefactoringSession refactoring = ((MoveRefactoringSession)session);
        refactoring.setUpdateReferences(settings.isUpdateReferences());
        if (settings.isUpdateQualifiedNames()) {
            refactoring.setFilePatterns(settings.getFilePatterns());
        }
        refactoring.setUpdateQualifiedNames(settings.isUpdateQualifiedNames());
    }

    public RefactoringPreview getRefactoringPreview(String sessionId) throws RefactoringException {
        RefactoringSession session = getRefactoringSession(sessionId);
        Change change = session.getChange();
        CompositeChange compositeChange;
        if (change instanceof CompositeChange) {
            compositeChange= (CompositeChange)change;
        } else {
            compositeChange= new CompositeChange("Dummy Change"); //$NON-NLS-1$
            compositeChange.add(change);
        }
        PreviewNode node = AbstractChangeNode.createNode(null, compositeChange);
        return DtoConverter.toRefactoringPreview(node);
    }

    public ChangeCreationResult createChange(String sessionId) throws RefactoringException {
        RefactoringSession session = getRefactoringSession(sessionId);
        return session.createChange();
    }

    public RefactoringStatus applyRefactoring(String sessionId) throws RefactoringException {
        RefactoringSession session = getRefactoringSession(sessionId);
        org.eclipse.ltk.core.refactoring.RefactoringStatus status = session.apply();
        return DtoConverter.toRefactoringStatusDto(status);
    }

    public RenameRefactoringSession createRenameRefactoring(IJavaElement element, boolean lightweight) throws CoreException, RefactoringException {

//        //package fragments are always renamed with wizard
//        if(lightweight && !(element instanceof IPackageFragment)){
//
//        }
//        else {
//            RenameSupport renameSupport = createRenameSupport(element, null, RenameSupport.UPDATE_REFERENCES);
//            if (renameSupport != null && renameSupport.preCheck().isOK()) {
//
//            }
//            throw new RefactoringException("Can't create refactoring session for element: " + element.getElementName());
//        }
        throw new UnsupportedOperationException();
    }

    private static RenameSupport createRenameSupport(IJavaElement element, String newName, int flags) throws CoreException {
        switch (element.getElementType()) {
//            case IJavaElement.JAVA_PROJECT:
//                return RenameSupport.create((IJavaProject) element, newName, flags);
//            case IJavaElement.PACKAGE_FRAGMENT_ROOT:
//                return RenameSupport.create((IPackageFragmentRoot) element, newName);
            case IJavaElement.PACKAGE_FRAGMENT:
                return RenameSupport.create((IPackageFragment) element, newName, flags);
            case IJavaElement.COMPILATION_UNIT:
                return RenameSupport.create((ICompilationUnit) element, newName, flags);
            case IJavaElement.TYPE:
                return RenameSupport.create((IType) element, newName, flags);
            case IJavaElement.METHOD:
                final IMethod method= (IMethod) element;
                if (method.isConstructor())
                    return createRenameSupport(method.getDeclaringType(), newName, flags);
                else
                    return RenameSupport.create((IMethod) element, newName, flags);
            case IJavaElement.FIELD:
                return RenameSupport.create((IField) element, newName, flags);
            case IJavaElement.TYPE_PARAMETER:
                return RenameSupport.create((ITypeParameter) element, newName, flags);
            case IJavaElement.LOCAL_VARIABLE:
                return RenameSupport.create((ILocalVariable) element, newName, flags);
        }
        return null;
    }
}
