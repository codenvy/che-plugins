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
package org.eclipse.che.ide.ext.openshift.client.deploy._new;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftLocalizationConstant;
import org.eclipse.che.ide.ext.openshift.client.OpenshiftServiceClient;
import org.eclipse.che.ide.ext.openshift.client.ValidateAuthenticationPresenter;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthenticator;
import org.eclipse.che.ide.ext.openshift.client.oauth.OpenshiftAuthorizationHandler;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildOutput;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildStrategy;
import org.eclipse.che.ide.ext.openshift.shared.dto.BuildTriggerPolicy;
import org.eclipse.che.ide.ext.openshift.shared.dto.Container;
import org.eclipse.che.ide.ext.openshift.shared.dto.ContainerPort;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfig;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentConfigSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentTriggerImageChangeParams;
import org.eclipse.che.ide.ext.openshift.shared.dto.DeploymentTriggerPolicy;
import org.eclipse.che.ide.ext.openshift.shared.dto.EnvVar;
import org.eclipse.che.ide.ext.openshift.shared.dto.GitBuildSource;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageChangeTrigger;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStream;
import org.eclipse.che.ide.ext.openshift.shared.dto.ImageStreamTag;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectMeta;
import org.eclipse.che.ide.ext.openshift.shared.dto.ObjectReference;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.PodTemplateSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Project;
import org.eclipse.che.ide.ext.openshift.shared.dto.ProjectRequest;
import org.eclipse.che.ide.ext.openshift.shared.dto.Route;
import org.eclipse.che.ide.ext.openshift.shared.dto.RouteSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.Service;
import org.eclipse.che.ide.ext.openshift.shared.dto.ServicePort;
import org.eclipse.che.ide.ext.openshift.shared.dto.ServiceSpec;
import org.eclipse.che.ide.ext.openshift.shared.dto.SourceBuildStrategy;
import org.eclipse.che.ide.ext.openshift.shared.dto.WebHookTrigger;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.CREATE_NEW_PROJECT;
import static org.eclipse.che.ide.ext.openshift.client.deploy._new.NewApplicationView.Mode.SELECT_EXISTING_PROJECT;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_APPLICATION_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_NAMESPACE_VARIABLE_NAME;
import static org.eclipse.che.ide.ext.openshift.shared.OpenshiftProjectTypeConstants.OPENSHIFT_PROJECT_TYPE_ID;

/**
 * Presenter for deploying Che project to new OpenShift application.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewApplicationPresenter extends ValidateAuthenticationPresenter implements NewApplicationView.ActionDelegate {
    private final NewApplicationView            view;
    private final AppContext                    appContext;
    private final DialogFactory                 dialogFactory;
    private final OpenshiftLocalizationConstant locale;
    private final GitServiceClient              gitService;
    private final DtoUnmarshallerFactory        dtoUnmarshaller;
    private final OpenshiftServiceClient        osService;
    private final DtoFactory                    dtoFactory;
    private final ProjectServiceClient          projectService;
    private final EventBus                      eventBus;
    private final NotificationManager           notificationManager;
    private       List<Project>                 osProjects;
    private       List<Remote>                  projectRemotes;
    private       List<ImageStream>             osImageStreams;
    private       ImageStreamTag                osActiveStreamTag;
    private       String                        osAppName;
    private       NewApplicationView.Mode       mode;

    public static final String API_VERSION = "v1";

    @Inject
    public NewApplicationPresenter(final NewApplicationView view,
                                   final AppContext appContext,
                                   final DialogFactory dialogFactory,
                                   final OpenshiftLocalizationConstant locale,
                                   final GitServiceClient gitService,
                                   final DtoUnmarshallerFactory dtoUnmarshaller,
                                   final OpenshiftServiceClient osService,
                                   final DtoFactory dtoFactory,
                                   final ProjectServiceClient projectService,
                                   final EventBus eventBus,
                                   final NotificationManager notificationManager,
                                   OpenshiftAuthenticator openshiftAuthenticator,
                                   OpenshiftAuthorizationHandler openshiftAuthorizationHandler) {
        super(openshiftAuthenticator, openshiftAuthorizationHandler, locale, notificationManager);
        this.view = view;
        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.locale = locale;
        this.gitService = gitService;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.osService = osService;
        this.dtoFactory = dtoFactory;
        this.projectService = projectService;
        this.eventBus = eventBus;
        this.notificationManager = notificationManager;
        view.setDelegate(this);
    }

    private void reset() {
        osProjects = null;
        osImageStreams = null;
        osActiveStreamTag = null;
        osAppName = null;
        projectRemotes = null;

        view.setDeployButtonEnabled(false);
        view.setLabels(Collections.<Pair<String, String>>emptyList());
        view.setEnvironmentVariables(Collections.<Pair<String, String>>emptyList());
        view.setApplicationName(null);
        view.setOpenShiftProjectDisplayName(null);
        view.setOpenShiftProjectDescription(null);
        view.setMode(CREATE_NEW_PROJECT);
        view.setImages(Collections.<String>emptyList());
        view.setProjects(Collections.<Project>emptyList());
    }

    @Override
    protected void onSuccessAuthentication() {
        reset();

        if (appContext.getCurrentProject() != null) {
            //Check is Git repository:
            List<String> listVcsProvider = appContext.getCurrentProject().getRootProject().getAttributes().get("vcs.provider.name");
            if (listVcsProvider != null && !listVcsProvider.isEmpty() && listVcsProvider.contains("git")) {
                getGitRemoteRepositories();
            } else {
                dialogFactory.createMessageDialog(locale.notGitRepositoryWarningTitle(), locale.notGitRepositoryWarning(
                        appContext.getCurrentProject().getProjectDescription().getName()), null).show();
            }
        }

    }

    private void getGitRemoteRepositories() {
        gitService.remoteList(appContext.getCurrentProject().getProjectDescription(), null, true,
                              new AsyncRequestCallback<List<Remote>>(dtoUnmarshaller.newListUnmarshaller(Remote.class)) {
                                  @Override
                                  protected void onSuccess(List<Remote> result) {
                                      if (!result.isEmpty()) {
                                          projectRemotes = unmodifiableList(result);
                                          loadOpenShiftData();
                                      } else {
                                          dialogFactory.createMessageDialog(locale.noGitRemoteRepositoryWarningTitle(),
                                                                            locale.noGitRemoteRepositoryWarning(
                                                                                    appContext.getCurrentProject().getProjectDescription()
                                                                                              .getName()), null).show();
                                      }
                                  }

                                  @Override
                                  protected void onFailure(Throwable exception) {
                                      notificationManager.showError(locale.getGitRemoteRepositoryError(
                                              appContext.getCurrentProject().getProjectDescription().getName()));
                                  }
                              });
    }

    private void loadOpenShiftData() {
        final ProjectDescriptor descriptor = checkNotNull(appContext.getCurrentProject()).getProjectDescription();
        view.show();
        view.setApplicationName(descriptor.getName());

        osService.getProjects().then(new Operation<List<Project>>() {
            @Override
            public void apply(List<Project> projects) throws OperationException {
                if (projects == null || projects.isEmpty()) {
                    return;
                }

                osProjects = unmodifiableList(projects);
                view.setProjects(osProjects);
            }
        }).then(osService.getImageStreams("openshift", null).then(new Operation<List<ImageStream>>() {
            @Override
            public void apply(List<ImageStream> streams) throws OperationException {
                if (streams == null || streams.isEmpty()) {
                    return;
                }

                osImageStreams = unmodifiableList(streams);

                final List<String> imageNames = Lists.transform(osImageStreams, new com.google.common.base.Function<ImageStream, String>() {
                    @Override
                    public String apply(ImageStream input) {
                        return input.getMetadata().getName();
                    }
                });

                view.setImages(imageNames);
                view.setLabels(Collections.<Pair<String, String>>emptyList());
            }
        }));
    }


    @Override
    public void onCancelClicked() {
        view.hide();
    }

    @Override
    public void onDeployClicked() {
        Promise<Project> projectPromise = null;

        if (mode == CREATE_NEW_PROJECT) {
            String osProjectName = view.getOpenShiftProjectName();
            String osProjectDisplayName = view.getOpenShiftProjectDisplayName();
            String osProjectDescription = view.getOpenShiftProjectDescription();
            //create new project
            ProjectRequest request = newDto(ProjectRequest.class)
                    .withApiVersion(API_VERSION)
                    .withDisplayName(osProjectDisplayName)
                    .withDescription(osProjectDescription)
                    .withMetadata(newDto(ObjectMeta.class)
                                          .withName(osProjectName));
            projectPromise = osService.createProject(request);
        } else if (mode == SELECT_EXISTING_PROJECT) {
            Project osSelectedProject = view.getOpenShiftSelectedProject();
            checkNotNull(osSelectedProject);
            projectPromise = Promises.resolve(osSelectedProject);
        }

        checkNotNull(projectPromise);
        checkNotNull(osAppName);

        projectPromise.then(new Operation<Project>() {
            @Override
            public void apply(final Project project) throws OperationException {
                final Map<String, String> labels = new HashMap<>();
                labels.put("generatedby", "Che");
                labels.put("application", osAppName);

                for (Pair<String, String> label : view.getLabels()) {
                    labels.put(label.getFirst(), label.getSecond());
                }

                Object exposedPorts =
                        (osActiveStreamTag.getDockerImageMetadata().getConfig() != null) ? osActiveStreamTag.getDockerImageMetadata()
                                                                                                            .getConfig().getExposedPorts()
                                                                                         : osActiveStreamTag.getDockerImageMetadata()
                                                                                                            .getContainerConfig()
                                                                                                            .getExposedPorts();
                List<ContainerPort> ports = parsePorts(exposedPorts);

                String namespace = project.getMetadata().getName();

                List<Promise<?>> promises = new ArrayList();
                promises.add(osService.createImageStream(generateImageStream(namespace, labels)));
                promises.add(osService.createBuildConfig(generateBuildConfig(namespace, labels)));
                promises.add(osService.createDeploymentConfig(generateDeploymentConfig(namespace, labels)));
                promises.add(osService.createRoute(generateRoute(namespace, labels)));

                if (!ports.isEmpty()) {
                    promises.add(osService.createService(generateService(namespace, getFirstPort(ports), labels)));
                }

                Promises.all(promises.toArray(new Promise[promises.size()]))
                        .then(new Operation<JsArrayMixed>() {
                            @Override
                            public void apply(JsArrayMixed arg) throws OperationException {
                                view.hide();
                                notificationManager
                                        .showInfo(locale.deployProjectSuccess(appContext.getCurrentProject().getRootProject().getName()));
                                setupMixin(project);
                            }
                        }).catchError(new Operation<PromiseError>() {
                    @Override
                    public void apply(PromiseError arg) throws OperationException {
                        //TODO remove notification, show error only on popup:
                        final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                        notificationManager.showError(serviceError.getMessage());
                        view.showError(serviceError.getMessage());
                    }
                });
            }
        });
    }

    private void setupMixin(Project project) {
        final ProjectDescriptor projectDescriptor = appContext.getCurrentProject().getRootProject();

        List<String> mixins = projectDescriptor.getMixins();
        if (!mixins.contains(OPENSHIFT_PROJECT_TYPE_ID)) {
            mixins.add(OPENSHIFT_PROJECT_TYPE_ID);
        }

        Map<String, List<String>> attributes = projectDescriptor.getAttributes();
        attributes.put(OPENSHIFT_APPLICATION_VARIABLE_NAME, newArrayList(osAppName));
        attributes.put(OPENSHIFT_NAMESPACE_VARIABLE_NAME, newArrayList(project.getMetadata().getName()));

        newPromise(new RequestCall<ProjectDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProjectDescriptor> callback) {
                projectService.updateProject(projectDescriptor.getPath(),
                                             projectDescriptor,
                                             newCallback(callback, dtoUnmarshaller.newUnmarshaller(ProjectDescriptor.class)));
            }
        }).then(new Operation<ProjectDescriptor>() {
            @Override
            public void apply(ProjectDescriptor project) throws OperationException {
                eventBus.fireEvent(new ProjectUpdatedEvent(appContext.getCurrentProject().getRootProject().getPath(), project));
                notificationManager.showInfo(
                        locale.linkProjectWithExistingSuccess(appContext.getCurrentProject().getRootProject().getName(), osAppName));
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                final ServiceError serviceError = dtoFactory.createDtoFromJson(arg.getMessage(), ServiceError.class);
                notificationManager.showError(serviceError.getMessage());
            }
        });
    }

    @Override
    public void onProjectNameChanged(String name) {
        validate();
    }

    @Override
    public void onApplicationNameChanged(String name) {
        osAppName = name;
        validate();
    }

    @Override
    public void onActiveProjectChanged(Project project) {
        validate();
    }

    private Promise<ImageStreamTag> setActiveImageTag(final ImageStream stream) {
        return osService.getImageStreamTag("openshift", stream.getMetadata().getName(), "latest").thenPromise(
                new Function<ImageStreamTag, Promise<ImageStreamTag>>() {
                    @Override
                    public Promise<ImageStreamTag> apply(ImageStreamTag streamTag) throws FunctionException {
                        osActiveStreamTag = streamTag;
                        List<String> envs =
                                (streamTag.getDockerImageMetadata().getConfig() != null) ? streamTag.getDockerImageMetadata().getConfig()
                                                                                                    .getEnv()
                                                                                         : streamTag.getDockerImageMetadata()
                                                                                                    .getContainerConfig().getEnv();
                        List<Pair<String, String>> variables = new ArrayList<>();
                        for (String env : envs) {
                            String[] keyValuePair = env.split("=");
                            if (keyValuePair.length != 2) {
                                continue;
                            }
                            variables.add(new Pair<>(keyValuePair[0], keyValuePair[1]));
                        }
                        view.setEnvironmentVariables(variables);
                        return Promises.resolve(streamTag);
                    }
                });
    }

    @Override
    public void onImageStreamChanged(String stream) {
        for (ImageStream osStream : osImageStreams) {
            if (stream.equals(osStream.getMetadata().getName())) {
                setActiveImageTag(osStream);
                break;
            }
        }
        validate();
    }

    @Override
    public void onModeChanged(NewApplicationView.Mode mode) {
        this.mode = mode;
        validate();
    }


    private void validate() {
        boolean valid = true;
        String osProjectName = view.getOpenShiftProjectName();
        //TODO add check for application name

        if (mode == CREATE_NEW_PROJECT) {
            //TODO check existing OpenShift projects
            if (Strings.isNullOrEmpty(osProjectName) || !osProjectName.matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?")) {
                valid = false;
            }
        }

        view.setDeployButtonEnabled(valid && osImageStreams != null && view.getActiveImage() != null);
    }

    private ImageStream generateImageStream(String namespace, Map<String, String> labels) {
        checkNotNull(namespace);
        return newDto(ImageStream.class)
                .withApiVersion(API_VERSION)
                .withKind("ImageStream")
                .withMetadata(newDto(ObjectMeta.class)
                                      .withName(osAppName)
                                      .withLabels(labels)
                                      .withNamespace(namespace));
    }


    private BuildConfig generateBuildConfig(String namespace, Map<String, String> labels) {
        checkNotNull(osActiveStreamTag);
        checkNotNull(namespace);

        BuildSource source = newDto(BuildSource.class).withType("Git")
                                                      .withGit(newDto(GitBuildSource.class)
                                                                       .withRef("master") //load branch
                                                                       .withUri(projectRemotes.get(0).getUrl()));

        SourceBuildStrategy sourceStrategy = newDto(SourceBuildStrategy.class)
                .withFrom(newDto(ObjectReference.class)
                                  .withKind("ImageStreamTag")
                                  .withName(osActiveStreamTag.getMetadata().getName())
                                  .withNamespace("openshift"));

        BuildStrategy strategy = newDto(BuildStrategy.class)
                .withType("Source")
                .withSourceStrategy(sourceStrategy);

        BuildTriggerPolicy generic = newDto(BuildTriggerPolicy.class)
                .withType("generic")
                .withGeneric(newDto(WebHookTrigger.class)
                                     .withSecret(generateSecret()));

        BuildTriggerPolicy github = newDto(BuildTriggerPolicy.class)
                .withType("github")
                .withGithub(newDto(WebHookTrigger.class)
                                    .withSecret(generateSecret()));

        BuildTriggerPolicy imageChange = newDto(BuildTriggerPolicy.class)
                .withType("imageChange")
                .withImageChange(newDto(ImageChangeTrigger.class));

        List<BuildTriggerPolicy> triggers = newArrayList(generic, github, imageChange);

        BuildOutput output = newDto(BuildOutput.class)
                .withTo(newDto(ObjectReference.class)
                                .withName(osAppName + ":latest")
                                .withKind("ImageStreamTag"));

        BuildConfigSpec spec = newDto(BuildConfigSpec.class)
                .withSource(source)
                .withStrategy(strategy)
                .withTriggers(triggers)
                .withOutput(output);

        return newDto(BuildConfig.class)
                .withApiVersion(API_VERSION)
                .withKind("BuildConfig")
                .withMetadata(newDto(ObjectMeta.class).withName(osAppName)
                                                      .withLabels(labels)
                                                      .withNamespace(namespace))
                .withSpec(spec);
    }

    private <T> T newDto(Class<T> clas) {
        return dtoFactory.createDto(clas);
    }

    private DeploymentConfig generateDeploymentConfig(String namespace, Map<String, String> labels) {
        checkNotNull(osActiveStreamTag);
        checkNotNull(namespace);

        Object exposedPorts = osActiveStreamTag.getDockerImageMetadata().getContainerConfig().getExposedPorts();
        List<ContainerPort> ports = parsePorts(exposedPorts);

        Map<String, String> templateLabels = new HashMap<>(labels);
        templateLabels.put("deploymentconfig", osAppName);

        List<EnvVar> env = newArrayList();

        for (Pair<String, String> variable : view.getEnvironmentVariables()) {
            env.add(newDto(EnvVar.class)
                            .withName(variable.getFirst())
                            .withValue(variable.getSecond()));
        }

        PodSpec podSpec = newDto(PodSpec.class)
                .withContainers(newArrayList(newDto(Container.class)
                                                     .withImage(osActiveStreamTag.getMetadata().getName())
                                                     .withName(osAppName)
                                                     .withPorts(ports)
                                                     .withEnv(env)));

        PodTemplateSpec template = newDto(PodTemplateSpec.class)
                .withMetadata(newDto(ObjectMeta.class)
                                      .withLabels(templateLabels))
                .withSpec(podSpec);

        DeploymentTriggerImageChangeParams imageChangeParams = newDto(DeploymentTriggerImageChangeParams.class)
                .withAutomatic(true)
                .withContainerNames(newArrayList(osAppName))
                .withFrom(newDto(ObjectReference.class)
                                  .withKind("ImageStreamTag")
                                  .withName(osActiveStreamTag.getMetadata()
                                                             .getName()));

        DeploymentTriggerPolicy imageChange = newDto(DeploymentTriggerPolicy.class)
                .withType("ImageChange")
                .withImageChangeParams(imageChangeParams);

        DeploymentTriggerPolicy configChange = newDto(DeploymentTriggerPolicy.class)
                .withType("ConfigChange");

        DeploymentConfigSpec spec = newDto(DeploymentConfigSpec.class)
                .withReplicas(1)
                .withSelector(Collections.singletonMap("deploymentconfig", osAppName))
                .withTemplate(template)
                .withTriggers(newArrayList(imageChange, configChange));

        return newDto(DeploymentConfig.class)
                .withApiVersion(API_VERSION)
                .withKind("DeploymentConfig")
                .withMetadata(newDto(ObjectMeta.class)
                                      .withName(osAppName)
                                      .withLabels(labels)
                                      .withNamespace(namespace))
                .withSpec(spec);
    }

    private Service generateService(String namespace, ContainerPort port, Map<String, String> labels) {
        if (port == null) {
            return null;
        }

        checkNotNull(namespace);

        return newDto(Service.class)
                .withApiVersion(API_VERSION)
                .withKind("Service")
                .withMetadata(newDto(ObjectMeta.class)
                                      .withName(osAppName)
                                      .withLabels(labels)
                                      .withNamespace(namespace))
                .withSpec(newDto(ServiceSpec.class)
                                  .withPorts(newArrayList(newDto(ServicePort.class)
                                                                  .withPort(port.getContainerPort())
                                                                  .withTargetPort(port.getContainerPort())
                                                                  .withProtocol(port.getProtocol())))
                                  .withSelector(singletonMap("deploymentconfig", osAppName)));
    }

    private Route generateRoute(String namespace, Map<String, String> labels) {
        checkNotNull(namespace);
        return newDto(Route.class)
                .withKind("Route")
                .withApiVersion(API_VERSION)
                .withMetadata(newDto(ObjectMeta.class)
                                      .withName(osAppName)
                                      .withLabels(labels)
                                      .withNamespace(namespace))
                .withSpec(newDto(RouteSpec.class)
                                  .withTo(newDto(ObjectReference.class)
                                                  .withKind("Service")
                                                  .withName(osAppName)));
    }

    private List<ContainerPort> parsePorts(Object exposedPorts) {
        if (!(exposedPorts instanceof JSONObject)) {
            return emptyList();
        }

        JSONObject ports = (JSONObject)exposedPorts;
        Jso jso = ports.getJavaScriptObject().cast();

        JsoArray<String> keys = jso.getKeys();

        List<ContainerPort> containerPorts = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            String[] split = keys.get(i).split("/");
            if (split.length != 2) {
                continue;
            }
            containerPorts.add(newDto(ContainerPort.class)
                                       .withContainerPort(Integer.valueOf(split[0]))
                                       .withProtocol(split[1].toUpperCase()));
        }

        return containerPorts;
    }

    /**
     * @return the lowest container port
     */
    private ContainerPort getFirstPort(List<ContainerPort> ports) {
        if (ports.isEmpty()) {
            return null;
        }

        final Iterator<ContainerPort> portsIterator = ports.iterator();

        ContainerPort firstPort = portsIterator.next();
        while (portsIterator.hasNext()) {
            final ContainerPort port = portsIterator.next();
            if (port.getContainerPort() < firstPort.getContainerPort()) {
                firstPort = port;
            }
        }

        return firstPort;
    }

    /**
     * Generates secret key.
     *
     * @return secret key
     */
    private native String generateSecret() /*-{
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }

        return s4() + s4() + s4() + s4();
    }-*/;
}
