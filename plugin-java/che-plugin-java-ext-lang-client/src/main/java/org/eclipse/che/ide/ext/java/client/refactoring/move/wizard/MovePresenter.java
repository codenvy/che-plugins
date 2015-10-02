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
package org.eclipse.che.ide.ext.java.client.refactoring.move.wizard;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.project.node.JavaFileNode;
import org.eclipse.che.ide.ext.java.client.project.node.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.refactoring.RefactorInfo;
import org.eclipse.che.ide.ext.java.client.refactoring.RefactoringUpdater;
import org.eclipse.che.ide.ext.java.client.refactoring.preview.PreviewPresenter;
import org.eclipse.che.ide.ext.java.client.refactoring.service.RefactoringServiceClient;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.CreateMoveRefactoring;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaElement;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.JavaProject;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.MoveSettings;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.ERROR;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.FATAL;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.INFO;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.OK;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus.WARNING;

/**
 * The class that manages Move panel widget.
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@Singleton
public class MovePresenter implements MoveView.ActionDelegate {
    private final MoveView                 view;
    private final RefactoringUpdater       refactoringUpdater;
    private final AppContext               appContext;
    private final PreviewPresenter         previewPresenter;
    private final DtoFactory               dtoFactory;
    private final RefactoringServiceClient refactorService;
    private final JavaNavigationService    navigationService;

    private RefactorInfo refactorInfo;
    private String       refactoringSessionId;

    @Inject
    public MovePresenter(MoveView view,
                         RefactoringUpdater refactoringUpdater,
                         AppContext appContext,
                         PreviewPresenter previewPresenter,
                         RefactoringServiceClient refactorService,
                         JavaNavigationService navigationService,
                         DtoFactory dtoFactory) {
        this.view = view;
        this.refactoringUpdater = refactoringUpdater;
        this.view.setDelegate(this);

        this.appContext = appContext;
        this.previewPresenter = previewPresenter;
        this.refactorService = refactorService;
        this.navigationService = navigationService;
        this.dtoFactory = dtoFactory;
    }

    /**
     * Show Move panel with the special information.
     *
     * @param refactorInfo
     *         information about the move operation
     */
    public void show(final RefactorInfo refactorInfo) {
        this.refactorInfo = refactorInfo;
        view.setEnablePreviewButton(false);
        view.setEnableAcceptButton(false);
        view.clearErrorLabel();

        CreateMoveRefactoring moveRefactoring = createMoveDto();

        Promise<String> sessionIdPromise = refactorService.createMoveRefactoring(moveRefactoring);

        sessionIdPromise.then(new Operation<String>() {
            @Override
            public void apply(String sessionId) throws OperationException {
                MovePresenter.this.refactoringSessionId = sessionId;

                showProjectsAndPackages();
            }
        });
    }

    private CreateMoveRefactoring createMoveDto() {
        List<JavaElement> elements = new ArrayList<>();

        for (Object node : refactorInfo.getSelectedItems()) {
            HasStorablePath storableNode = (HasStorablePath)node;

            JavaElement element = dtoFactory.createDto(JavaElement.class);

            if (storableNode instanceof PackageNode) {
                element.setPath(storableNode.getStorablePath());
                element.setPack(true);
            }

            if (storableNode instanceof JavaFileNode) {
                element.setPath(JavaSourceFolderUtil.getFQNForFile((VirtualFile)storableNode));
                element.setPack(false);
            }

            elements.add(element);
        }

        String pathToProject = getPathToProject();

        CreateMoveRefactoring moveRefactoring = dtoFactory.createDto(CreateMoveRefactoring.class);

        moveRefactoring.setElements(elements);
        moveRefactoring.setProjectPath(pathToProject);

        return moveRefactoring;
    }

    private String getPathToProject() {
        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            throw new IllegalArgumentException(getClass() + " Current project undefined...");
        }

        return currentProject.getProjectDescription().getPath();
    }

    private void showProjectsAndPackages() {
        Promise<List<JavaProject>> projectsPromise = navigationService.getProjectsAndPackages(true);

        projectsPromise.then(new Operation<List<JavaProject>>() {
            @Override
            public void apply(List<JavaProject> projects) throws OperationException {
                for (JavaProject project : projects) {
                    if (project.getPath().equals(getPathToProject())) {
                        List<JavaProject> currentProject = new ArrayList<>();
                        currentProject.add(project);

                        view.setTreeOfDestinations(currentProject);
                        view.show(refactorInfo);

                        return;
                    }
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onPreviewButtonClicked() {
        RefactoringSession session = dtoFactory.createDto(RefactoringSession.class);
        session.setSessionId(refactoringSessionId);
        prepareMovingChanges(session).then(new Operation<ChangeCreationResult>() {
            @Override
            public void apply(ChangeCreationResult arg) throws OperationException {
                if (arg.isCanShowPreviewPage()) {
                    previewPresenter.show(refactoringSessionId, refactorInfo);

                    view.hide();
                } else {
                    view.showStatusMessage(arg.getStatus());
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onAcceptButtonClicked() {
        final RefactoringSession session = dtoFactory.createDto(RefactoringSession.class);
        session.setSessionId(refactoringSessionId);
        prepareMovingChanges(session).then(new Operation<ChangeCreationResult>() {
            @Override
            public void apply(ChangeCreationResult arg) throws OperationException {
                if (arg.isCanShowPreviewPage()) {
                    refactorService.applyRefactoring(session).then(new Operation<RefactoringStatus>() {
                        @Override
                        public void apply(RefactoringStatus arg) throws OperationException {
                            if (arg.getSeverity() == OK) {
                                view.hide();
                                refactoringUpdater.refreshProjectTree();
                                refactoringUpdater.refreshOpenEditors();
                            } else {
                                view.showErrorMessage(arg);
                            }
                        }
                    });
                } else {
                    view.showErrorMessage(arg.getStatus());
                }
            }
        });
    }

    private Promise<ChangeCreationResult> prepareMovingChanges(final RefactoringSession session) {
        MoveSettings moveSettings = dtoFactory.createDto(MoveSettings.class);
        moveSettings.setSessionId(refactoringSessionId);
        moveSettings.setUpdateReferences(view.isUpdateReferences());
        moveSettings.setUpdateQualifiedNames(view.isUpdateQualifiedNames());
        if (moveSettings.isUpdateQualifiedNames()) {
            moveSettings.setFilePatterns(view.getFilePatterns());
        }

        return refactorService.setMoveSettings(moveSettings).thenPromise(new Function<Void, Promise<ChangeCreationResult>>() {
            @Override
            public Promise<ChangeCreationResult> apply(Void arg) throws FunctionException {
                return refactorService.createChange(session);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void setMoveDestinationPath(String path, String projectPath) {
        refactoringUpdater.setPathToMove(path);
        ReorgDestination destination = dtoFactory.createDto(ReorgDestination.class);
        destination.setType(ReorgDestination.DestinationType.PACKAGE);
        destination.setSessionId(refactoringSessionId);
        destination.setProjectPath(projectPath);
        destination.setDestination(path);
        Promise<RefactoringStatus> promise = refactorService.setDestination(destination);
        promise.then(new Operation<RefactoringStatus>() {
            @Override
            public void apply(RefactoringStatus arg) throws OperationException {
                view.setEnableAcceptButton(true);
                view.setEnablePreviewButton(true);

                switch (arg.getSeverity()) {
                    case INFO:
                        view.showStatusMessage(arg);
                        break;
                    case WARNING:
                        view.showStatusMessage(arg);
                        break;
                    case ERROR:
                        showErrorMessage(arg);
                        break;
                    case FATAL:
                        showErrorMessage(arg);
                        break;
                    case OK:
                    default:
                        view.clearStatusMessage();
                        break;
                }
            }
        });
    }

    private void showErrorMessage(RefactoringStatus arg) {
        view.showErrorMessage(arg);
        view.setEnableAcceptButton(false);
        view.setEnablePreviewButton(false);
    }

}