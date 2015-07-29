/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.runner.docker;

import com.codenvy.docker.AuthConfigs;
import com.codenvy.docker.DockerConnector;
import com.codenvy.docker.DockerException;
import com.codenvy.docker.DockerFileException;
import com.codenvy.docker.DockerImage;
import com.codenvy.docker.Dockerfile;
import com.codenvy.docker.ProgressLineFormatterImpl;
import com.codenvy.docker.ProgressMonitor;
import com.codenvy.docker.json.ContainerConfig;
import com.codenvy.docker.json.ContainerCreated;
import com.codenvy.docker.json.ExposedPort;
import com.codenvy.docker.json.HostConfig;
import com.codenvy.docker.json.ImageInfo;
import com.codenvy.docker.json.PortBinding;
import com.codenvy.docker.json.ProgressStatus;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.CustomPortService;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.project.server.ProjectEvent;
import org.eclipse.che.api.project.server.ProjectEventListener;
import org.eclipse.che.api.project.server.ProjectEventService;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.api.runner.internal.ApplicationLogger;
import org.eclipse.che.api.runner.internal.ApplicationLogsPublisher;
import org.eclipse.che.api.runner.internal.ApplicationProcess;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.DeploymentSources;
import org.eclipse.che.api.runner.internal.Disposer;
import org.eclipse.che.api.runner.internal.ResourceAllocators;
import org.eclipse.che.api.runner.internal.Runner;
import org.eclipse.che.api.runner.internal.RunnerConfiguration;
import org.eclipse.che.api.runner.internal.RunnerConfigurationFactory;
import org.eclipse.che.api.runner.internal.RunnerEvent;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.TarUtils;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author andrew00x */
public abstract class BaseDockerRunner extends Runner {
    static final Logger LOG = LoggerFactory.getLogger(BaseDockerRunner.class);

    public static final String HOST_NAME                     = "runner.docker.host_name";
    public static final String WATCH_UPDATE_OF_PROJECT_TYPES = "runner.docker.watch_update_project_types";

    protected static final String VALID_PORT_RANGE_PATTERN =
            "([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]|655[0-2][0-9]|6553[0-5])";

    /**
     * Env variable for the user auth token.
     */
    protected static final String CODENVY_TOKEN = "CODENVY_TOKEN";

    /**
     * Env variable for the workspace ID
     */
    protected static final String CODENVY_WORKSPACE_ID = "CODENVY_WORKSPACE_ID";

    /**
     * Env variable for the project name
     */
    protected static final String CODENVY_PROJECT_NAME = "CODENVY_PROJECT_NAME";

    /**
     * Env variable for the project encoded ID
     */
    protected static final String CODENVY_PROJECT_ID = "CODENVY_PROJECT_ID";

    /**
     * Pattern for ports in range [0;65535]. Public HTTP ports of application should be defined as environment variables in dockerfile in
     * following format:
     * <pre>
     * ENV CODENVY_APP_PORT_{NUMBER}_HTTP {NUMBER}
     * </pre>
     * For example:
     * <pre>
     * ENV CODENVY_APP_PORT_8080_HTTP 8080
     * </pre>
     * See docker docs for details about format of dockerfile.
     * With Docker remote API environment variables defined as array of string in format: VAR=VALUE
     */
    protected static final Pattern APP_HTTP_PORT_PATTERN  =
            Pattern.compile("(CODENVY_APP_PORT_" + VALID_PORT_RANGE_PATTERN + "_HTTP=)" + VALID_PORT_RANGE_PATTERN);
    /**
     * Pattern for debug ports in range [0;65535]. For example:
     * <pre>
     * ENV CODENVY_APP_PORT_8000_DEBUG 8000
     * </pre>
     *
     * @see #APP_HTTP_PORT_PATTERN
     */
    protected static final Pattern APP_DEBUG_PORT_PATTERN =
            Pattern.compile("(CODENVY_APP_PORT_" + VALID_PORT_RANGE_PATTERN + "_DEBUG=)" + VALID_PORT_RANGE_PATTERN);
    /**
     * Pattern for for web shell ports in range [0;65535]. For example:
     * <pre>
     * ENV CODENVY_WEB_SHELL_PORT 4200
     * </pre>
     *
     * @see #APP_HTTP_PORT_PATTERN
     */
    protected static final Pattern WEB_SHELL_PORT_PATTERN = Pattern.compile("(CODENVY_WEB_SHELL_PORT=)" + VALID_PORT_RANGE_PATTERN);

    protected static final Pattern BUILD_BIND_DIRECTORY = Pattern.compile("(CODENVY_APP_BIND_DIR=)(.+)");

    private final String                    hostName;
    private final CustomPortService         portService;
    private final ApplicationLinksGenerator applicationLinksGenerator;
    private final Set<String>               watchUpdateProjectTypes;
    private final ProjectEventService       projectEventService;
    private final DockerConnector           dockerConnector;

    /**
     * Allow to hash with sha-1
     */
    private HashFunction sha1HashFunction = Hashing.sha1();

    protected BaseDockerRunner(java.io.File deployDirectoryRoot,
                               int cleanupDelay,
                               String hostName,
                               Set<String> watchUpdateProjectTypes,
                               ResourceAllocators allocators,
                               CustomPortService portService,
                               DockerConnector dockerConnector,
                               EventService eventService,
                               ApplicationLinksGenerator applicationLinksGenerator) {
        super(deployDirectoryRoot, cleanupDelay, allocators, eventService);
        this.hostName = hostName;
        this.watchUpdateProjectTypes = watchUpdateProjectTypes;
        this.portService = portService;
        this.applicationLinksGenerator = applicationLinksGenerator;
        this.dockerConnector = dockerConnector;
        projectEventService = new ProjectEventService(eventService);
    }

    /**
     * Get description of additional properties of docker environment. This method might return {@code null} is specified environment is
     * not configured for the project.
     *
     * @see Mapper
     */
    protected abstract DockerEnvironment getDockerEnvironment(RunRequest request) throws IOException, RunnerException;

    protected abstract AuthConfigs getAuthConfigs(RunRequest request) throws IOException, RunnerException;

    public static class DockerRunnerConfiguration extends RunnerConfiguration {
        public DockerRunnerConfiguration(int memory, RunRequest request) {
            super(memory, request);
        }
    }

    @Override
    public RunnerConfigurationFactory getRunnerConfigurationFactory() {
        return new RunnerConfigurationFactory() {
            @Override
            public RunnerConfiguration createRunnerConfiguration(RunRequest request) throws RunnerException {
                final DockerRunnerConfiguration configuration = new DockerRunnerConfiguration(request.getMemorySize(), request);
                configuration.setHost(hostName);
                configuration.setDebugHost(hostName);
                return configuration;
            }
        };
    }

    @Override
    public List<RunnerMetric> getStats() throws RunnerException {
        final List<RunnerMetric> stats = super.getStats();
        try {
            final com.codenvy.docker.json.SystemInfo systemInfo = dockerConnector.getSystemInfo();
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            final String dataSpaceTotal = systemInfo.statusField(com.codenvy.docker.json.SystemInfo.DRIVER_STATE_DATA_SPACE_TOTAL);
            if (dataSpaceTotal != null) {
                stats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.DISK_SPACE_TOTAL).withValue(dataSpaceTotal));
            }
            final String dataSpaceUsed = systemInfo.statusField(com.codenvy.docker.json.SystemInfo.DRIVER_STATE_DATA_SPACE_USED);
            if (dataSpaceUsed != null) {
                stats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.DISK_SPACE_USED).withValue(dataSpaceUsed));
            }
        } catch (IOException e) {
            throw new RunnerException(e.getMessage(), e);
        }
        return stats;
    }

    @Override
    protected ApplicationProcess newApplicationProcess(DeploymentSources toDeploy, RunnerConfiguration runnerCfg) throws RunnerException {
        try {
            // It always should be DockerRunnerConfiguration.
            final DockerRunnerConfiguration dockerRunnerCfg = (DockerRunnerConfiguration)runnerCfg;
            final RunRequest request = dockerRunnerCfg.getRequest();
            final DockerEnvironment dockerEnvironment = getDockerEnvironment(request);
            final Mapper mapper = dockerEnvironment.getMapper();
            final AuthConfigs authConfigs = getAuthConfigs(request);
            final Dockerfile dockerfileModel = getDockerfileModel(dockerEnvironment);
            final DockerImage dockerImageModel = getDockerImageModel(dockerfileModel);
            dockerfileModel.getParameters().put("debug", request.isInDebugMode());
            dockerfileModel.getParameters().putAll(request.getOptions());
            dockerfileModel.getParameters().putAll(request.getVariables());

            final java.io.File application = toDeploy.getFile();
            final String applicationFilename = getFilenameWithoutExtension(application);

            java.io.File unpackedApplication = null;
            final java.io.File workDir = application.getParentFile();
            final List<java.io.File> files = new LinkedList<>();
            final boolean isApplicationBinary = request.getBuildTaskDescriptor() != null;
            if (isApplicationBinary) {
                files.add(application);
                dockerfileModel.getParameters().put("build", application.getName());
            }
            // IDEX-1597: kept for back compatibility
            if (needUnpackApplicationWhenAdd(dockerImageModel)) {
                if (application.isDirectory()) {
                    unpackedApplication = application;
                } else {
                    unpackedApplication = unpackArchive(application, workDir, applicationFilename + "_unpack");
                }
                files.add(unpackedApplication);
                dockerfileModel.getParameters().put("app", unpackedApplication.getName());
            } else {
                files.add(application);
                dockerfileModel.getParameters().put("app", application.getName());
            }
            if (needAddProjectSources(dockerImageModel)) {
                java.io.File sources;
                if (isApplicationBinary) {
                    sources = getProjectSources(request, workDir, applicationFilename + "_sources.zip");
                } else {
                    sources = application;
                }
                if (needUnpackProjectSourcesWhenAdd(dockerImageModel) && !sources.isDirectory()) {
                    sources = unpackArchive(sources, workDir, getFilenameWithoutExtension(sources) + "_unpack");
                }
                dockerfileModel.getParameters().put("src", sources.getName());
                // IDEX-1597: kept for back compatibility
                dockerfileModel.getParameters().put("app_src", sources.getName());
                files.add(sources);
            }
            final java.io.File dockerfile = new java.io.File(workDir, "Dockerfile");
            dockerfileModel.writeDockerfile(dockerfile);
            files.add(dockerfile);
            dockerRunnerCfg.setRecipeFile(dockerfile);

            getEventService().publish(RunnerEvent.preparationStartedEvent(request.getId(), request.getWorkspace(), request.getProject()));

            final ApplicationLogsPublisher logsPublisher = new ApplicationLogsPublisher(ApplicationLogger.DUMMY,
                                                                                        getEventService(),
                                                                                        request.getId(),
                                                                                        request.getWorkspace(),
                                                                                        request.getProject());
            final long startTime = System.currentTimeMillis();
            logsPublisher.writeLine(String.format("[INFO] Starting Runner @ %1$ta %1$tb %1$td %1$tT %1$tZ %1$tY", startTime));
            final String dockerRepoName = String.format("%s/%s", getDockerNamespace(request), getDockerRepositoryName(request));
            final ImageIdentifier imageIdentifier =
                    createImage(dockerRepoName, logsPublisher, authConfigs, files.toArray(new java.io.File[files.size()]));
            final long initImageTime = System.currentTimeMillis() - startTime;

            final ImageInfo imageDetails = dockerConnector.inspectImage(imageIdentifier.id);
            final CodenvyPortMappings portMappings = createPortMapping(imageDetails, mapper, request.isInDebugMode());
            addApplicationLinks(portMappings, dockerRunnerCfg);
            addDebugConfiguration(portMappings, dockerRunnerCfg);
            final HostConfig hostConfig = new HostConfig();
            final List<String> env = new LinkedList<>();
            setupEnvironmentVariables(env, dockerRunnerCfg, portMappings);
            addPortBinding(portMappings.getExposedPortMapping(), hostConfig);
            addPortMapping(portMappings.getExposedPortMapping(), dockerRunnerCfg);

            final String applicationBindDir = getApplicationDirectoryBindTarget(imageDetails, mapper);
            final ValueHolder<ApplicationUpdater> updaterHolder = new ValueHolder<>();
            final ProjectDescriptor projectDescriptor = request.getProjectDescriptor();
            if (applicationBindDir != null) {
                if (unpackedApplication == null) {
                    if (application.isDirectory()) {
                        unpackedApplication = application;
                    } else {
                        unpackedApplication = unpackArchive(application, workDir, applicationFilename + "_unpack");
                    }
                }
                hostConfig.setBinds(new String[]{String.format("%s:%s", unpackedApplication.getAbsolutePath(), applicationBindDir)});
                if (watchUpdateProjectTypes.contains(projectDescriptor.getType())) {
                    updaterHolder.set(new ApplicationUpdater(unpackedApplication, projectDescriptor.getPath(),
                                                             projectDescriptor.getBaseUrl(), request.getUserToken(), getExecutor()));
                }
            }
            final ContainerConfig containerConfig = new ContainerConfig().withImage(imageIdentifier.id)
                                                                         .withMemory((long)runnerCfg.getMemory() * 1024 * 1024)
                                                                         .withCpuShares(1)
                                                                         .withEnv(env.toArray(new String[env.size()]));
            // Listens start and stop.
            final ApplicationProcess.Callback callback = new ApplicationProcess.Callback() {
                @Override
                public void started() {
                    final ApplicationUpdater applicationUpdater = updaterHolder.get();
                    if (applicationUpdater != null) {
                        // Start listen updates of project sources
                        projectEventService.addListener(request.getWorkspace(), request.getProject(), applicationUpdater);
                    }
                }

                @Override
                public void stopped() {
                    for (Pair<String, Integer> p : portMappings.getExposedPortMapping()) {
                        // Release allocated ports
                        portService.release(p.second);
                    }
                    final ApplicationUpdater applicationUpdater = updaterHolder.get();
                    if (applicationUpdater != null) {
                        // Stop listening updates of project sources
                        projectEventService.removeListener(request.getWorkspace(), request.getProject(), applicationUpdater);
                    }
                    final long endTime = System.currentTimeMillis();
                    try {
                        logsPublisher.writeLine(String.format("[INFO] Run ended @ %1$ta %1$tb %1$td %1$tT %1$tZ %1$tY", endTime));
                    } catch (IOException ignored) {
                    }
                }
            };
            final DockerProcess docker =
                    new DockerProcess(request, containerConfig, hostConfig, logsPublisher, imageIdentifier, initImageTime, callback);
            registerDisposer(docker, new Disposer() {
                @Override
                public void dispose() {
                    try {
                        if (docker.isRunning()) {
                            docker.stop();
                        }
                        dockerConnector.removeContainer(docker.container, false, true);
                        LOG.debug("Remove docker container: {}", docker.container);
                        dockerConnector.removeImage(docker.imageIdentifier.fullName, false);
                        LOG.debug("Remove docker image, name: {}, id: {}", docker.imageIdentifier.fullName, docker.imageIdentifier.id);
                    } catch (DockerException e) {
                        final int status = e.getStatus();
                        if (status == 409) {
                            LOG.warn(e.getMessage(), e);
                        } else if (status != 404) {
                            LOG.error(e.getMessage(), e);
                        }
                    } catch (Exception e) {
                        LOG.error("Docker problem: " + e.getMessage(), e);
                    }
                }
            });
            return docker;
        } catch (IOException e) {
            throw new RunnerException(e.getMessage(), e);
        }
    }

    /**
     * Setup environment variables
     * @param env the environment to setup
     * @param dockerRunnerConfiguration the docker runner configuration
     * @param portMappings the port mappings
     */
    protected void setupEnvironmentVariables(List<String> env, DockerRunnerConfiguration dockerRunnerConfiguration, CodenvyPortMappings portMappings) {

        // extract request from runner configuration
        RunRequest request = dockerRunnerConfiguration.getRequest();

        // hostname
        env.add("CODENVY_HOSTNAME=" + dockerRunnerConfiguration.getHost());

        // Add port mapping data
        for (Pair<String, Integer> p : portMappings.getExposedPortMapping()) {
            env.add(String.format("CODENVY_PORT_%s=%d", p.first.split("/")[0], p.second));
        }

        // add user token
        env.add(CODENVY_TOKEN + "=" + request.getUserToken());

        // add data related to workspaces and projects
        String projectName = request.getProject().substring(1);
        String workspaceId = request.getWorkspace();
        env.add(CODENVY_PROJECT_NAME + "=" + projectName);
        env.add(CODENVY_WORKSPACE_ID + "=" + workspaceId);
        String fullSha1 = "p" + sha1HashFunction.hashString(workspaceId + projectName, Charset.defaultCharset()).toString();
        env.add(CODENVY_PROJECT_ID + "=" + fullSha1.substring(0, 7));


        // add WebShell theme
        final String webShellTheme = request.getShellOptions().get("WebShellTheme");
        if (webShellTheme != null) {
            env.add("WEB_SHELL_THEME=" + webShellTheme);
        }
    }


    private String getFilenameWithoutExtension(java.io.File file) {
        String filename = file.getName();
        final int i = filename.lastIndexOf('.');
        return i == -1 ? filename : filename.substring(0, i);
    }

    private Dockerfile getDockerfileModel(DockerEnvironment dockerEnvironment) throws RunnerException {
        try {
            return dockerEnvironment.getDockerfile();
        } catch (DockerFileException e) {
            throw new RunnerException(e.getMessage());
        }
    }

    private DockerImage getDockerImageModel(Dockerfile dockerfile) throws RunnerException {
        final List<DockerImage> images = dockerfile.getImages();
        if (images.isEmpty()) {
            throw new RunnerException(
                    "Unable create environment for starting application. Dockerfile exists but doesn't contains any images.");
        }
        return images.get(0);
    }

    private boolean needUnpackApplicationWhenAdd(DockerImage dockerImage) {
        boolean needUnpack = false;
        for (Pair<String, String> add : dockerImage.getAdd()) {
            if (add.first.startsWith("$app$/") || (add.first.equals("$app$") && add.second.endsWith("/"))) {
                needUnpack |= true;
            }
        }
        return needUnpack;
    }

    private boolean needAddProjectSources(DockerImage dockerImage) {
        boolean addSources = false;
        for (Pair<String, String> add : dockerImage.getAdd()) {
            if (add.first.startsWith("$src$")) {
                addSources |= true;
            } else if (add.first.startsWith("$app_src$")) {
                // IDEX-1597: kept for back compatibility
                addSources |= true;
            }
        }
        return addSources;
    }

    private boolean needUnpackProjectSourcesWhenAdd(DockerImage dockerImage) {
        boolean needUnpack = false;
        for (Pair<String, String> add : dockerImage.getAdd()) {
            if (add.first.startsWith("$src$/") || (add.first.equals("$src$") && add.second.endsWith("/"))) {
                needUnpack |= true;
            } else if (add.first.startsWith("$app_src$/") || (add.first.equals("$app_src$") && add.second.endsWith("/"))) {
                // IDEX-1597: kept for back compatibility
                needUnpack |= true;
            }
        }
        return needUnpack;
    }

    private java.io.File getProjectSources(RunRequest request, java.io.File downloadDir, String sourcesFilename) throws IOException {
        final String zipballLinkHref =
                request.getProjectDescriptor().getLink(org.eclipse.che.api.project.server.Constants.LINK_REL_EXPORT_ZIP).getHref();
        final String projectSrcUrl;
        if (zipballLinkHref.indexOf('?') > 0) {
            projectSrcUrl = zipballLinkHref + "&token=" + request.getUserToken();
        } else {
            projectSrcUrl = zipballLinkHref + "?token=" + request.getUserToken();
        }
        return downloadFile(projectSrcUrl, downloadDir, sourcesFilename, false);
    }

    private java.io.File unpackArchive(java.io.File archive, java.io.File directory, String unpackDirName)
            throws IOException, RunnerException {
        if (ZipUtils.isZipFile(archive) || TarUtils.isTarFile(archive)) {
            final java.io.File unpacked = new java.io.File(directory, unpackDirName);
            if (ZipUtils.isZipFile(archive)) {
                ZipUtils.unzip(archive, unpacked);
            } else if (TarUtils.isTarFile(archive)) {
                TarUtils.untar(archive, unpacked);
            }
            return unpacked;
        }
        throw new RunnerException(
                String.format("Unable unpack file %s. Not supported type of archive only zip and tar archives are supported.",
                              archive.getName()));
    }

    private String getDockerNamespace(RunRequest request) {
        final String userName = request.getUserId();
        final int maxSize = 30;
        final StringBuilder sb = new StringBuilder(maxSize);
        // From docker source code: ^([a-z0-9_]{4,30})$
        for (int i = 0; i < maxSize && i < userName.length(); i++) {
            char c = userName.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (Character.isUpperCase(c)) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private String getDockerRepositoryName(RunRequest request) {
        final String workspace = request.getWorkspace();
        String project = request.getProject();
        final StringBuilder sb = new StringBuilder(workspace.length() + project.length() + 1);
        // From docker source code: ^([a-z0-9-_.]+)$
        sb.append(workspace);
        sb.append('_');
        for (int i = project.charAt(0) == '/' ? 1 : 0; i < project.length(); i++) {
            char c = project.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (Character.isUpperCase(c)) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            } else if (c == '/') {
                sb.append(".");
            } else if (c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private ImageIdentifier createImage(String dockerRepoName, final ApplicationLogsPublisher logsPublisher, AuthConfigs authConfigs, java.io.File... files)
            throws IOException, RunnerException {
        final String fullImageName = dockerRepoName + ':' + NameGenerator.generate(null, 8);
        final long startTime = System.currentTimeMillis();
        String imageId;
        final CreateImageLogger output = new CreateImageLogger(logsPublisher);
        try {
            imageId = dockerConnector.buildImage(fullImageName, output, authConfigs, files);
        } catch (InterruptedException e) {
            throw new RunnerException("Interrupted while waiting for creation of docker image. ");
        } catch (IOException e) {
            throw new RunnerException(e.getLocalizedMessage());
        }
        final long buildDuration = System.currentTimeMillis() - startTime;
        LOG.debug("Create new image {}, id {} in {} ms", fullImageName, imageId, buildDuration);
        return new ImageIdentifier(fullImageName, imageId);
    }

    private CodenvyPortMappings createPortMapping(ImageInfo imageDetails, Mapper mapper, boolean inDebugMode) throws IOException {
        final CodenvyPortMappings portMapping = new CodenvyPortMappings();
        final Map<String, ExposedPort> exposedPorts = imageDetails.getConfig().getExposedPorts();
        if (exposedPorts == null || exposedPorts.isEmpty()) {
            return portMapping;
        }
        String privateWebPort = mapper.getWebPort() >= 0 ? Integer.toString(mapper.getWebPort()) : null;
        String privateDebugPort = inDebugMode && mapper.getDebugPort() >= 0 ? Integer.toString(mapper.getDebugPort()) : null;
        String privateShellPort = mapper.getShellPort() >= 0 ? Integer.toString(mapper.getShellPort()) : null;
        final String[] env = imageDetails.getConfig().getEnv();
        if (env != null && env.length > 0) {
            for (String pair : env) {
                Matcher matcher;
                if ((matcher = APP_HTTP_PORT_PATTERN.matcher(pair)).matches()) {
                    privateWebPort = matcher.group(3);
                } else if (inDebugMode && (matcher = APP_DEBUG_PORT_PATTERN.matcher(pair)).matches()) {
                    privateDebugPort = matcher.group(3);
                } else if ((matcher = WEB_SHELL_PORT_PATTERN.matcher(pair)).matches()) {
                    privateShellPort = matcher.group(2);
                }
            }
        }
        for (String rawPort : exposedPorts.keySet()) {
            final String noProtocolPort = rawPort.split("/")[0];
            final int publicPort = portService.acquire();
            final Pair<String, Integer> mapping = Pair.of(rawPort, publicPort);
            portMapping.getExposedPortMapping().add(mapping);
            if (noProtocolPort.equals(privateWebPort)) {
                portMapping.setWebPortMapping(mapping);
            } else if (noProtocolPort.equals(privateDebugPort)) {
                portMapping.setDebugPortMapping(mapping);
            } else if (noProtocolPort.equals(privateShellPort)) {
                portMapping.setShellPortMapping(mapping);
            }
        }
        return portMapping;
    }

    private void addPortBinding(List<Pair<String, Integer>> exposedPortMapping, HostConfig hostConfig) {
        if (exposedPortMapping.isEmpty()) {
            return;
        }
        final Map<String, PortBinding[]> portBinding = new HashMap<>(exposedPortMapping.size());
        for (Pair<String, Integer> p : exposedPortMapping) {
            portBinding.put(p.first, new PortBinding[]{new PortBinding().withHostPort(Integer.toString(p.second))});
        }
        hostConfig.setPortBindings(portBinding);
    }

    private void addPortMapping(List<Pair<String, Integer>> exposedPortMapping, DockerRunnerConfiguration dockerRunnerCfg) {
        if (exposedPortMapping.isEmpty()) {
            return;
        }
        Map<String, String> portMapping = dockerRunnerCfg.getPortMapping();
        for (Pair<String, Integer> p : exposedPortMapping) {
            String exposedPort = p.first.split("/")[0]; // p.first would look like 8080/tcp
            portMapping.put(exposedPort, Integer.toString(p.second));
        }
    }

    private void addApplicationLinks(CodenvyPortMappings portMappings, DockerRunnerConfiguration dockerRunnerCfg) {
        final RunRequest request = dockerRunnerCfg.getRequest();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final Pair<String, Integer> web = portMappings.getWebPortMapping();
        final Pair<String, Integer> shell = portMappings.getShellPortMapping();
        if (web != null) {
            dockerRunnerCfg.getLinks().add(dtoFactory.createDto(Link.class)
                                                     .withRel(Constants.LINK_REL_WEB_URL)
                                                     .withHref(applicationLinksGenerator
                                                                       .createApplicationLink(request.getWorkspace(), request.getProject(),
                                                                                              request.getUserId(), web.second)));
        }
        if (shell != null) {
            dockerRunnerCfg.getLinks().add(dtoFactory.createDto(Link.class)
                                                     .withRel(Constants.LINK_REL_SHELL_URL)
                                                     .withHref(applicationLinksGenerator
                                                                       .createWebShellLink(request.getWorkspace(), request.getProject(),
                                                                                           request.getUserId(), shell.second)));
        }
    }

    private void addDebugConfiguration(CodenvyPortMappings portMappings, DockerRunnerConfiguration dockerRunnerCfg) {
        final Pair<String, Integer> debug = portMappings.getDebugPortMapping();
        if (debug != null) {
            dockerRunnerCfg.setDebugHost(dockerRunnerCfg.getDebugHost());
            dockerRunnerCfg.setDebugPort(debug.second);
        }
    }



    private String getApplicationDirectoryBindTarget(ImageInfo imageDetails, Mapper mapper) {
        final String[] env = imageDetails.getConfig().getEnv();
        if (env != null && env.length > 0) {
            for (String pair : env) {
                Matcher matcher;
                if ((matcher = BUILD_BIND_DIRECTORY.matcher(pair)).matches()) {
                    return matcher.group(2);
                }
            }
        }
        return mapper.getBindApplicationDir();
    }

    private static class ApplicationUpdater implements ProjectEventListener {
        final java.io.File    workDir;
        final String          project;
        final String          baseUrl;
        final ExecutorService executor;
        final String          userToken;

        ApplicationUpdater(java.io.File workDir, String project, String projectBaseUrl, String userToken, ExecutorService executor) {
            this.workDir = workDir;
            this.project = project;
            this.baseUrl = projectBaseUrl.substring(0, projectBaseUrl.lastIndexOf(project));
            this.userToken = userToken;
            this.executor = executor;
        }

        @Override
        public void onEvent(final ProjectEvent event) {
            switch (event.getType()) {
                case UPDATED:
                case CREATED:
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final java.io.File file = new java.io.File(workDir, event.getPath());
                            // EventType 'UPDATED' might be generated only for files but not folder.
                            if (event.getType() == ProjectEvent.EventType.CREATED && event.isFolder()) {
                                if (!file.mkdirs()) {
                                    LOG.error(String.format("Unable create %s", event.getPath()));
                                }
                            } else {
                                final String url = String.format("%s/file%s/%s?token=%s", baseUrl, project, event.getPath(), userToken);
                                try (InputStream in = URI.create(url).toURL().openStream()) {
                                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                            }
                        }
                    });
                    break;
                case DELETED:
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (!IoUtil.deleteRecursive(new java.io.File(workDir, event.getPath()))) {
                                LOG.error(String.format("Unable delete %s", event.getPath()));
                            }
                        }
                    });
                    break;
            }
        }
    }

    protected static class CodenvyPortMappings {
        private Pair<String, Integer> webPortMapping;
        private Pair<String, Integer> debugPortMapping;
        private Pair<String, Integer> shellPortMapping;
        private final List<Pair<String, Integer>> exposedPortMapping = new LinkedList<>();

        Pair<String, Integer> getWebPortMapping() {
            return webPortMapping;
        }

        void setWebPortMapping(Pair<String, Integer> webPortMapping) {
            this.webPortMapping = webPortMapping;
        }

        Pair<String, Integer> getDebugPortMapping() {
            return debugPortMapping;
        }

        void setDebugPortMapping(Pair<String, Integer> debugPortMapping) {
            this.debugPortMapping = debugPortMapping;
        }

        Pair<String, Integer> getShellPortMapping() {
            return shellPortMapping;
        }

        void setShellPortMapping(Pair<String, Integer> shellPortMapping) {
            this.shellPortMapping = shellPortMapping;
        }

        public List<Pair<String, Integer>> getExposedPortMapping() {
            return exposedPortMapping;
        }
    }

    private static class CreateImageLogger extends ProgressLineFormatterImpl implements ProgressMonitor {
        final ApplicationLogsPublisher logsPublisher;

        // max rate is 1 event each 2 seconds
        int  eventPeriod               = 2000;
        // Limits rate of image events
        long nextDownloadProgressEvent = 0;

        CreateImageLogger(ApplicationLogsPublisher logsPublisher) {
            this.logsPublisher = logsPublisher;
        }

        @Override
        public void updateProgress(ProgressStatus currentProgressStatus) {
            try {
                // format/beatify logs
                final String message = format(currentProgressStatus);
                if (message != null) {
                    logsPublisher.writeLine(message);
                }
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }

        @Override
        public String format(ProgressStatus progressStatus) {
            final StringBuilder sb = new StringBuilder();
            sb.append("[DOCKER] ");
            final String stream = progressStatus.getStream();
            final String status = progressStatus.getStatus();
            final String error = progressStatus.getError();
            if (error != null) {
                sb.append("[ERROR] ");
                sb.append(error);
            } else if (stream != null) {
                sb.append(stream.trim());
            } else if (status != null) {
                final String id = progressStatus.getId();
                if ("Downloading".equals(status)) {
                    if (System.currentTimeMillis() - nextDownloadProgressEvent >= 0) {
                        sb.append(String.format("%s: Downloading %s", id, parseProgressText(progressStatus)));
                        nextDownloadProgressEvent = System.currentTimeMillis() + eventPeriod;
                    } else {
                        return null;
                    }
                } else {
                    if (id != null) {
                        sb.append(id);
                        sb.append(':');
                        sb.append(' ');
                    }
                    sb.append(status);
                    sb.append(' ');
                    sb.append(parseProgressText(progressStatus));
                }
            }
            return sb.toString();
        }
    }

    private static class ImageIdentifier {
        // Image name that contains repository name and tag in format: repoName + ":" + tag
        final String fullName;
        final String id;

        ImageIdentifier(String fullName, String id) {
            this.fullName = fullName;
            this.id = id;
        }
    }

    private class DockerProcess extends ApplicationProcess {
        final RunRequest               request;
        final ContainerConfig          containerCfg;
        final HostConfig               hostCfg;
        final ApplicationLogsPublisher logsPublisher;
        final ImageIdentifier          imageIdentifier;
        final Callback                 callback;
        final long                     imageInitDuration;
        final AtomicBoolean            started;
        String       container;
        DockerLogger logger;

        DockerProcess(RunRequest request,
                      ContainerConfig containerCfg,
                      HostConfig hostCfg,
                      ApplicationLogsPublisher logsPublisher,
                      ImageIdentifier imageIdentifier,
                      long imageInitDuration,
                      Callback callback) {
            this.request = request;
            this.containerCfg = containerCfg;
            this.hostCfg = hostCfg;
            this.logsPublisher = logsPublisher;
            this.imageIdentifier = imageIdentifier;
            this.callback = callback;
            this.imageInitDuration = imageInitDuration;
            started = new AtomicBoolean(false);
        }

        @Override
        public void start() throws RunnerException {
            if (started.compareAndSet(false, true)) {
                try {
                    final ContainerCreated response = dockerConnector.createContainer(containerCfg, null);
                    dockerConnector.startContainer(response.getId(), hostCfg, new LogMessagePrinter(logsPublisher));
                    container = response.getId();
                    LOG.info("EVENT#configure-docker-started# WS#{}# USER#{}# ID#{}#", request.getWorkspace(), request.getUserId(),
                             container);
                    getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LOG.debug("Attach to container {}", container);
                                dockerConnector.attachContainer(container, new LogMessagePrinter(logsPublisher), true);
                                LOG.debug("Detach from container {}", container);
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    });
                    logger = new DockerLogger(dockerConnector, container);
                    if (callback != null) {
                        callback.started();
                    }
                } catch (IOException e) {
                    if (callback != null) {
                        callback.stopped();
                    }
                    throw new RunnerException(e);
                }
            } else {
                throw new IllegalStateException("Process is already started");
            }
        }

        @Override
        public void stop() throws RunnerException {
            if (started.get()) {
                try {
                    dockerConnector.stopContainer(container, 3, TimeUnit.SECONDS);
                    LOG.info("EVENT#configure-docker-finished# WS#{}# USER#{}# ID#{}#", request.getWorkspace(), request.getUserId(),
                             container);
                } catch (IOException e) {
                    throw new RunnerException(e);
                }
            } else {
                throw new IllegalStateException("Process is not started yet");
            }
        }

        @Override
        public int waitFor() throws RunnerException {
            if (started.get()) {
                try {
                    return dockerConnector.waitContainer(container);
                } catch (IOException e) {
                    throw new RunnerException(e);
                } finally {
                    if (callback != null) {
                        callback.stopped();
                    }
                }
            }
            throw new IllegalStateException("Process is not started yet");
        }

        @Override
        public int exitCode() throws RunnerException {
            if (started.get()) {
                try {
                    return dockerConnector.inspectContainer(container).getState().getExitCode();
                } catch (IOException e) {
                    throw new RunnerException(e);
                }
            }
            return -1;
        }

        @Override
        public boolean isRunning() throws RunnerException {
            if (started.get()) {
                try {
                    return dockerConnector.inspectContainer(container).getState().isRunning();
                } catch (ConnectException e) {
                    // If connection to docker daemon is lost.
                    LOG.error(e.getMessage(), e);
                } catch (IOException e) {
                    throw new RunnerException(e);
                }
            }
            return false;
        }

        @Override
        public ApplicationLogger getLogger() throws RunnerException {
            if (started.get()) {
                return logger;
            }
            return ApplicationLogger.DUMMY;
        }

        private class DockerLogger implements ApplicationLogger {
            final DockerConnector connector;
            final String          container;

            DockerLogger(DockerConnector connector, String container) {
                this.connector = connector;
                this.container = container;
            }

            @Override
            public void getLogs(final Appendable output) throws IOException {
                connector.attachContainer(container, new LogMessagePrinter(new LineConsumer() {
                    @Override
                    public void writeLine(String s) throws IOException {
                        output.append(s);
                        output.append('\n');
                    }

                    @Override
                    public void close() throws IOException {
                        // noop
                    }
                }), false);
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public void writeLine(String line) throws IOException {
                logsPublisher.writeLine(line);
            }

            @Override
            public void close() throws IOException {
            }
        }
    }
}
