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
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.ext.java.jdi.client.actions.RemoteDebugAction;
import org.eclipse.che.ide.ext.java.jdi.client.debug.DebuggerPresenter;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import org.eclipse.che.ide.ext.java.jdi.client.fqn.JavaFqnResolver;
import org.eclipse.che.ide.extension.maven.shared.MavenAttributes;

import static org.eclipse.che.ide.MimeType.TEXT_X_JAVA;
import static org.eclipse.che.ide.MimeType.TEXT_X_JAVA_SOURCE;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_CENTER_TOOLBAR;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_CODE;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_MAIN_MENU;
import static org.eclipse.che.ide.api.action.IdeActions.GROUP_RUN_CONTEXT_MENU;
import static org.eclipse.che.ide.api.constraints.Anchor.AFTER;

/**
 * Extension allows debug Java web applications.
 *
 * @author Andrey Plotnikov
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 * @author Anatoliy Bazko
 */
@Singleton
@Extension(title = "Java Debugger", version = "3.0.0")
public class JavaRuntimeExtension {
    /** Channel for the messages containing debugger events. */
    public static final String EVENTS_CHANNEL     = "debugger:events:";
    /** Channel for the messages containing message which informs about debugger is disconnected. */
    public static final String DISCONNECT_CHANNEL = "debugger:disconnected:";

    private static final String REMOTE_DEBUG_ID     = "remoteDebug";
    public static final  String GROUP_DEBUG_TOOLBAR = "DebugGroupToolbar";

    @Inject
    public JavaRuntimeExtension(ActionManager actionManager,
                                RemoteDebugAction remoteDebugAction,
                                DebuggerManager debuggerManager,
                                DebuggerPresenter debuggerPresenter,
                                FqnResolverFactory resolverFactory,
                                JavaFqnResolver javaFqnResolver,
                                JavaRuntimeLocalizationConstant localizationConstant) {
        final DefaultActionGroup mainMenu = (DefaultActionGroup)actionManager.getAction(GROUP_MAIN_MENU);
        final DefaultActionGroup debugMenu = new DefaultActionGroup(localizationConstant.mainMenuDebugName(), true, actionManager);

        // register actions
        actionManager.registerAction(REMOTE_DEBUG_ID, remoteDebugAction);

        // add actions in main menu
        mainMenu.add(debugMenu, new Constraints(AFTER, GROUP_CODE));
        debugMenu.add(remoteDebugAction);

        // add actions on center part of toolbar
        final DefaultActionGroup debugToolbarGroup = new DefaultActionGroup(GROUP_DEBUG_TOOLBAR, false, actionManager);
        actionManager.registerAction(GROUP_DEBUG_TOOLBAR, debugToolbarGroup);
        debugToolbarGroup.add(remoteDebugAction);
        final DefaultActionGroup centerToolbarGroup = (DefaultActionGroup)actionManager.getAction(GROUP_CENTER_TOOLBAR);
        centerToolbarGroup.add(debugToolbarGroup);

        // add actions in context menu
        DefaultActionGroup runContextGroup = (DefaultActionGroup)actionManager.getAction(GROUP_RUN_CONTEXT_MENU);
        runContextGroup.add(remoteDebugAction);

        debuggerManager.registeredDebugger(MavenAttributes.MAVEN_ID, debuggerPresenter);
        debuggerManager.registeredDebugger(Constants.CODENVY_PLUGIN_ID, debuggerPresenter);
        resolverFactory.addResolver(TEXT_X_JAVA, javaFqnResolver);
        resolverFactory.addResolver("application/java", javaFqnResolver);
        resolverFactory.addResolver(TEXT_X_JAVA_SOURCE, javaFqnResolver);
    }
}