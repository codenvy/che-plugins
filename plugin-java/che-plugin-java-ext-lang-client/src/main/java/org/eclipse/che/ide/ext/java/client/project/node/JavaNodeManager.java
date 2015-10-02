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
package org.eclipse.che.ide.ext.java.client.project.node;

import com.google.common.base.Strings;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.settings.SettingsProvider;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.JavaResources;
import org.eclipse.che.ide.ext.java.client.navigation.JavaNavigationService;
import org.eclipse.che.ide.ext.java.client.project.node.jar.JarContainerNode;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettings;
import org.eclipse.che.ide.ext.java.client.project.settings.JavaNodeSettingsProvider;
import org.eclipse.che.ide.ext.java.shared.Constants;
import org.eclipse.che.ide.ext.java.shared.Jar;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.factory.NodeFactory;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class JavaNodeManager extends NodeManager {
    private JavaNavigationService    javaService;
    private JavaNodeFactory          javaNodeFactory;
    private JavaResources            javaResources;
    private EventBus                 eventBus;
    private JavaNodeSettingsProvider settingsProvider;

    public static final String JAVA_MIME_TYPE = "text/x-java-source";
    public static final String JAVA_EXT       = ".java";

    @Inject
    public JavaNodeManager(NodeFactory nodeFactory,
                           ProjectServiceClient projectService,
                           DtoUnmarshallerFactory dtoUnmarshaller,
                           NodesResources nodesResources,
                           SettingsProvider nodeSettingsProvider,
                           DtoFactory dtoFactory,
                           JavaNavigationService javaService,
                           JavaNodeFactory javaNodeFactory,
                           Map<String, SettingsProvider> settingsProviderMap,
                           JavaResources javaResources,
                           EventBus eventBus) {
        super(nodeFactory, projectService, dtoUnmarshaller, nodesResources, nodeSettingsProvider, dtoFactory);

        this.javaService = javaService;
        this.javaNodeFactory = javaNodeFactory;
        this.javaResources = javaResources;
        this.eventBus = eventBus;

        if (!(settingsProviderMap.containsKey("java") || settingsProviderMap.get("java") instanceof JavaNodeSettingsProvider)) {
            throw new IllegalStateException("Java node settings provider was not found");
        }

        this.settingsProvider = (JavaNodeSettingsProvider)settingsProviderMap.get("java");
    }

    /** ************** External Libraries operations ********************* */

    @NotNull
    public Promise<List<Node>> getExternalLibraries(@NotNull ProjectDescriptor descriptor) {
        return AsyncPromiseHelper.createFromAsyncRequest(getExternalLibrariesRC(descriptor.getPath()))
                                 .then(createJarNodes(descriptor, settingsProvider.getSettings()));
    }

    @NotNull
    private RequestCall<List<Jar>> getExternalLibrariesRC(@NotNull final String projectPath) {
        return new RequestCall<List<Jar>>() {
            @Override
            public void makeCall(AsyncCallback<List<Jar>> callback) {
                javaService.getExternalLibraries(projectPath, _callback(callback, dtoUnmarshaller.newListUnmarshaller(Jar.class)));
            }
        };
    }

    @NotNull
    private Function<List<Jar>, List<Node>> createJarNodes(@NotNull final ProjectDescriptor descriptor,
                                                           @NotNull final NodeSettings nodeSettings) {
        return new Function<List<Jar>, List<Node>>() {
            @Override
            public List<Node> apply(List<Jar> jars) throws FunctionException {
                List<Node> nodes = new ArrayList<>(jars.size());

                for (Jar jar : jars) {
                    JarContainerNode jarContainerNode = javaNodeFactory.newJarContainerNode(jar, descriptor, nodeSettings);
                    nodes.add(jarContainerNode);
                }

                return nodes;
            }
        };
    }

    /** ************** Jar Library Children operations ********************* */

    @NotNull
    public Promise<List<Node>> getJarLibraryChildren(@NotNull ProjectDescriptor descriptor, int libId, @NotNull NodeSettings nodeSettings) {
        return AsyncPromiseHelper.createFromAsyncRequest(getLibraryChildrenRC(descriptor.getPath(), libId))
                                 .then(createJarEntryNodes(libId, descriptor, nodeSettings));
    }

    @NotNull
    private RequestCall<List<JarEntry>> getLibraryChildrenRC(@NotNull final String projectPath, final int libId) {
        return new RequestCall<List<JarEntry>>() {
            @Override
            public void makeCall(AsyncCallback<List<JarEntry>> callback) {
                javaService
                        .getLibraryChildren(projectPath, libId, _callback(callback, dtoUnmarshaller.newListUnmarshaller(JarEntry.class)));
            }
        };
    }

    @NotNull
    public Promise<List<Node>> getJarChildren(@NotNull ProjectDescriptor descriptor, int libId, @NotNull String path,
                                              @NotNull NodeSettings nodeSettings) {
        return AsyncPromiseHelper.createFromAsyncRequest(getChildrenRC(descriptor.getPath(), libId, path))
                                 .then(createJarEntryNodes(libId, descriptor, nodeSettings));
    }

    @NotNull
    private RequestCall<List<JarEntry>> getChildrenRC(@NotNull final String projectPath, final int libId, @NotNull final String path) {
        return new RequestCall<List<JarEntry>>() {
            @Override
            public void makeCall(AsyncCallback<List<JarEntry>> callback) {
                javaService
                        .getChildren(projectPath, libId, path, _callback(callback, dtoUnmarshaller.newListUnmarshaller(JarEntry.class)));
            }
        };
    }

    @NotNull
    private Function<List<JarEntry>, List<Node>> createJarEntryNodes(final int libId, @NotNull final ProjectDescriptor descriptor,
                                                                     @NotNull final NodeSettings nodeSettings) {
        return new Function<List<JarEntry>, List<Node>>() {
            @Override
            public List<Node> apply(List<JarEntry> entries) throws FunctionException {

                List<Node> nodes = new ArrayList<>();

                for (JarEntry jarEntry : entries) {
                    Node node = createNode(jarEntry, libId, descriptor, nodeSettings);
                    if (node != null) {
                        nodes.add(node);
                    }
                }

                return nodes;
            }
        };
    }

    private Node createNode(JarEntry entry, int id, ProjectDescriptor descriptor, NodeSettings nodeSettings) {

        if (entry.getType() == JarEntry.JarEntryType.FOLDER || entry.getType() == JarEntry.JarEntryType.PACKAGE) {
            return javaNodeFactory.newJarFolderNode(entry, id, descriptor, nodeSettings);
        } else if (entry.getType() == JarEntry.JarEntryType.FILE || entry.getType() == JarEntry.JarEntryType.CLASS_FILE) {
            return javaNodeFactory.newJarFileNode(entry, id, descriptor, nodeSettings);
        }

        return null;
    }

    /** ************** Common methods ********************* */

    public static boolean isJavaProject(Node node) {
        if (!(node instanceof HasProjectDescriptor)) {
            return false;
        }

        ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
        Map<String, List<String>> attributes = descriptor.getAttributes();

        return attributes.containsKey(Constants.LANGUAGE)
               && attributes.get(Constants.LANGUAGE) != null
               && "java".equals(attributes.get(Constants.LANGUAGE).get(0));
    }

    public JavaResources getJavaNodesResources() {
        return javaResources;
    }

    public JavaNodeFactory getJavaNodeFactory() {
        return javaNodeFactory;
    }

    public JavaNodeSettingsProvider getJavaSettingsProvider() {
        return settingsProvider;
    }

    @Override
    public Node createNodeByType(ItemReference itemReference, ProjectDescriptor descriptor, NodeSettings settings) {
        if ("folder".equals(itemReference.getType()) || "project".equals(itemReference.getType())) {
            return javaNodeFactory.newPackageNode(itemReference, descriptor, (JavaNodeSettings)settingsProvider.getSettings());
        } else if ("file".equals(itemReference.getType())) {
            return createFileNodeByType(itemReference, descriptor, settings);
        }
        return null;
    }

    public Node createFileNodeByType(ItemReference itemReference, ProjectDescriptor descriptor, NodeSettings settings) {
        if (isJavaItemReference(itemReference)) {
            return javaNodeFactory.newJavaFileNode(itemReference, descriptor, (JavaNodeSettings)settingsProvider.getSettings());
        }

        return nodeFactory.newFileReferenceNode(itemReference, descriptor, settings);
    }

    public boolean isJavaItemReference(ItemReference itemReference) {
        final String mimeType = itemReference.getMediaType();

        //first detect by mime type
        if (!Strings.isNullOrEmpty(mimeType) && JAVA_MIME_TYPE.equals(mimeType)) {
            return true;
        }

        return itemReference.getName().endsWith(JAVA_EXT);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public JavaNavigationService getJavaService() {
        return javaService;
    }

    public Promise<Node> getClassNode(final ProjectDescriptor descriptor, final int libId, final String path) {
        return AsyncPromiseHelper.createFromAsyncRequest(new RequestCall<Node>() {
            @Override
            public void makeCall(final AsyncCallback<Node> callback) {
                Unmarshallable<JarEntry> u = dtoUnmarshaller.newUnmarshaller(JarEntry.class);
                javaService.getEntry(descriptor.getPath(), libId, path, new AsyncRequestCallback<JarEntry>(u) {
                    @Override
                    protected void onSuccess(JarEntry entry) {
                        Node node = createNode(entry, libId, descriptor, settingsProvider.getSettings());
                        callback.onSuccess(node);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }

    @Override
    public Function<List<ItemReference>, Promise<List<ItemReference>>> filterItemReference() {
        return new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
            @Override
            public Promise<List<ItemReference>> apply(List<ItemReference> referenceList) throws FunctionException {

                final List<ItemReference> collector = new ArrayList<>();

                Promise<Void> promise = Promises.resolve(null);

                return getNonEmptyChildren(promise, referenceList.listIterator(), collector)
                        .thenPromise(new Function<Void, Promise<List<ItemReference>>>() {
                            @Override
                            public Promise<List<ItemReference>> apply(Void arg) throws FunctionException {
                                return Promises.resolve(collector);
                            }
                        });
            }
        };
    }

    private Promise<Void> getNonEmptyChildren(Promise<Void> promise,
                                              ListIterator<ItemReference> iterator,
                                              final List<ItemReference> collector) {
        if (!iterator.hasNext()) {
            return promise;
        }

        final ItemReference itemReference = iterator.next();

        if (itemReference.getType().equals("file")) {
            collector.add(itemReference);
            return getNonEmptyChildren(promise, iterator, collector);
        }

        final Promise<Void> derivedPromise = promise.thenPromise(new Function<Void, Promise<Void>>() {
            @Override
            public Promise<Void> apply(Void arg) throws FunctionException {
                return foundFirstNonEmpty(itemReference).thenPromise(new Function<List<ItemReference>, Promise<Void>>() {
                    @Override
                    public Promise<Void> apply(List<ItemReference> arg) throws FunctionException {
                        collector.addAll(arg);

                        return Promises.resolve(null);
                    }
                });
            }
        });

        return getNonEmptyChildren(derivedPromise, iterator, collector);
    }

    @Override
    protected Function<List<Node>, Promise<List<Node>>> sortNodes() {
        return new Function<List<Node>, Promise<List<Node>>>() {
            @Override
            public Promise<List<Node>> apply(List<Node> nodes) throws FunctionException {
                Collections.sort(nodes, new FQNComparator());
                return Promises.resolve(nodes);
            }
        };
    }

    private Promise<List<ItemReference>> foundFirstNonEmpty(ItemReference parent) {
        return AsyncPromiseHelper.createFromAsyncRequest(getItemReferenceRC(parent.getPath()))
                                 .thenPromise(checkForEmptiness(parent));
    }

    private Function<List<ItemReference>, Promise<List<ItemReference>>> checkForEmptiness(final ItemReference parent) {
        return new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
            @Override
            public Promise<List<ItemReference>> apply(List<ItemReference> children) throws FunctionException {
                if (children.isEmpty() || children.size() > 1) {
                    List<ItemReference> list = new ArrayList<>();
                    list.add(parent);
                    return Promises.resolve(list);
                }

                if ("file".equals(children.get(0).getType())) {
                    List<ItemReference> list = new ArrayList<>();
                    list.add(parent);
                    return Promises.resolve(list);
                } else {
                    return foundFirstNonEmpty(children.get(0));
                }

            }
        };
    }
}
