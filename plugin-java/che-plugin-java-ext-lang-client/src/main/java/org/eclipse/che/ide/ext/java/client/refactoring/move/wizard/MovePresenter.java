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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.projecttree.JavaSourceFolderUtil;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.PackageNode;
import org.eclipse.che.ide.ext.java.client.projecttree.nodes.SourceFileNode;
import org.eclipse.che.ide.ext.java.client.refactoring.RefactorInfo;
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

/**
 * @author Dmitry Shnurenko
 */
@Singleton
public class MovePresenter implements MoveView.ActionDelegate {

    private final MoveView                 view;
    private EventBus eventBus;
    private final AppContext               appContext;
    private final PreviewPresenter         previewPresenter;
    private final DtoFactory               dtoFactory;
    private final RefactoringServiceClient refactorService;
    private final JavaNavigationService    navigationService;

    private RefactorInfo refactorInfo;
    private String       refactoringSessionId;

    @Inject
    public MovePresenter(MoveView view,
                         AppContext appContext,
                         PreviewPresenter previewPresenter,
                         RefactoringServiceClient refactorService,
                         JavaNavigationService navigationService,
                         EventBus eventBus,
                         DtoFactory dtoFactory) {
        this.view = view;
        this.eventBus = eventBus;
        this.view.setDelegate(this);

        this.appContext = appContext;
        this.previewPresenter = previewPresenter;
        this.refactorService = refactorService;
        this.navigationService = navigationService;
        this.dtoFactory = dtoFactory;
    }

    public void show(final RefactorInfo refactorInfo) {
        this.refactorInfo = refactorInfo;

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
            StorableNode<?> storableNode = (StorableNode)node;

            JavaElement element = dtoFactory.createDto(JavaElement.class);

            if (storableNode instanceof PackageNode) {
                element.setPath(storableNode.getPath());
                element.setPack(true);
            }

            if (storableNode instanceof SourceFileNode) {
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
                view.setTreeOfDestinations(projects);
                view.show(refactorInfo);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onPreviewButtonClicked() {
        previewPresenter.show(refactoringSessionId, refactorInfo);

        view.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void onAcceptButtonClicked() {
        MoveSettings moveSettings = dtoFactory.createDto(MoveSettings.class);
        moveSettings.setSessionId(refactoringSessionId);
        boolean updateReferences = view.isUpdateReferences();
        moveSettings.setUpdateReferences(updateReferences);
        moveSettings.setUpdateQualifiedNames(view.isUpdateQualifiedNames());
        if(moveSettings.isUpdateQualifiedNames()){
            moveSettings.setFilePatterns(view.getFilePatterns());
        }
        final RefactoringSession session = dtoFactory.createDto(RefactoringSession.class);
        session.setSessionId(refactoringSessionId);
        refactorService.setMoveSettings(moveSettings).thenPromise(new Function<Void, Promise<ChangeCreationResult>>() {
            @Override
            public Promise<ChangeCreationResult> apply(Void arg) throws FunctionException {

                return refactorService.createChange(session);
            }
        }).then(new Operation<ChangeCreationResult>() {
            @Override
            public void apply(ChangeCreationResult arg) throws OperationException {
                if(arg.isCanShowPreviewPage()){
                    refactorService.applyRefactoring(session).then(new Operation<RefactoringStatus>() {
                        @Override
                        public void apply(RefactoringStatus arg) throws OperationException {
                            if(arg.getSeverity() == RefactoringStatus.OK){
                                view.hide();
                                eventBus.fireEvent(new RefreshProjectTreeEvent());
                            } else {
                                view.showStatusMessage(arg);
                            }
                        }
                    });
                } else {
                    view.showStatusMessage(arg.getStatus());
                }
            }
        });

    }

    @Override
    public void setMoveDestinationPath(String path, String projectPath) {
        ReorgDestination destination = dtoFactory.createDto(ReorgDestination.class);
        destination.setType(ReorgDestination.DestinationType.PACKAGE);
        destination.setSessionId(refactoringSessionId);
        destination.setProjectPath(projectPath);
        destination.setDestination(path);
        Promise<RefactoringStatus> promise = refactorService.setDestination(destination);
        promise.then(new Operation<RefactoringStatus>() {
            @Override
            public void apply(RefactoringStatus arg) throws OperationException {
                if (arg.getSeverity() != RefactoringStatus.OK) {
                    view.showStatusMessage(arg);
                } else {
                    view.clearStatusMessage();
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {

            }
        });
    }
}
