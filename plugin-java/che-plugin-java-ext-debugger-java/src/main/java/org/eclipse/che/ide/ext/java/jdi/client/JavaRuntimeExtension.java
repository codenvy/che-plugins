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
package org.eclipse.che.ide.ext.java.jdi.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Constants;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.ext.java.jdi.client.actions.DebugAction;
import org.eclipse.che.ide.ext.java.jdi.client.actions.RemoteDebugAction;
import org.eclipse.che.ide.ext.java.jdi.client.debug.DebuggerPresenter;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.JavaFqnResolver;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import static org.eclipse.che.ide.MimeType.TEXT_X_JAVA;
import static org.eclipse.che.ide.MimeType.TEXT_X_JAVA_SOURCE;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RIGHT_TOOLBAR;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN_CONTEXT_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN_TOOLBAR;
import static org.eclipse.che.ide.ext.runner.client.constants.ActionId.RUN_WITH;

/**
 * Extension allows debug Java web applications.
 *
 * @author Andrey Plotnikov
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 */
@Singleton
@Extension(title = "Java Debugger", version = "3.0.0")
public class JavaRuntimeExtension {
    /** Channel for the messages containing debugger events. */
    public static final String EVENTS_CHANNEL     = "debugger:events:";
    /** Channel for the messages containing message which informs about debugger is disconnected. */
    public static final String DISCONNECT_CHANNEL = "debugger:disconnected:";

    private static final String REMOTE_DEBUG_ID = "remoteDebug";

    @Inject
    public JavaRuntimeExtension(ActionManager actionManager,
                                DebugAction debugAction,
                                RemoteDebugAction remoteDebugAction,
                                DebuggerManager debuggerManager,
                                DebuggerPresenter debuggerPresenter,
                                FqnResolverFactory resolverFactory,
                                JavaFqnResolver javaFqnResolver,
                                JavaRuntimeLocalizationConstant localizationConstant) {
        // register actions
        actionManager.registerAction(localizationConstant.debugAppActionId(), debugAction);
        actionManager.registerAction(REMOTE_DEBUG_ID, remoteDebugAction);

        // add actions in main toolbar
        DefaultActionGroup runToolbarGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RUN_TOOLBAR);
        if (runToolbarGroup == null) {
            DefaultActionGroup rightToolbarGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RIGHT_TOOLBAR);
            runToolbarGroup = new DefaultActionGroup(IdeActions.GROUP_RUN_TOOLBAR, false, actionManager);
            rightToolbarGroup.add(runToolbarGroup);
        }
        runToolbarGroup.add(debugAction, new Constraints(Anchor.BEFORE, "chooseRunner"));

        // add actions in main menu
        DefaultActionGroup runMenuActionGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RUN);
        runMenuActionGroup.add(debugAction, new Constraints(Anchor.BEFORE, RUN_WITH.getId()));
        runMenuActionGroup.add(remoteDebugAction, new Constraints(Anchor.AFTER, RUN_WITH.getId()));

        // add actions in context menu
        DefaultActionGroup runContextGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RUN_CONTEXT_MENU);
        runContextGroup.add(debugAction);

        debuggerManager.registeredDebugger(MavenAttributes.MAVEN_ID, debuggerPresenter);
        debuggerManager.registeredDebugger(Constants.CODENVY_PLUGIN_ID, debuggerPresenter);
        resolverFactory.addResolver(TEXT_X_JAVA, javaFqnResolver);
        resolverFactory.addResolver("application/java", javaFqnResolver);
        resolverFactory.addResolver(TEXT_X_JAVA_SOURCE, javaFqnResolver);
    }
}