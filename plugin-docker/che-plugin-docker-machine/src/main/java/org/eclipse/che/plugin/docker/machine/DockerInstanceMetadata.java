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
package org.eclipse.che.plugin.docker.machine;

import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Docker implementation of {@link InstanceMetadata}
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public class DockerInstanceMetadata implements InstanceMetadata {
    private final ContainerInfo info;

    public DockerInstanceMetadata(ContainerInfo containerInfo) throws MachineException {
        this.info = containerInfo;
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> md = new LinkedHashMap<>();
        md.put("id", info.getId());
        md.put("created", info.getCreated());
        md.put("image", info.getImage());
        md.put("path", info.getPath());
        md.put("appArmorProfile", info.getAppArmorProfile());
        md.put("driver", info.getDriver());
        md.put("execDriver", info.getExecDriver());
        md.put("hostnamePath", info.getHostnamePath());
        md.put("hostsPath", info.getHostsPath());
        md.put("mountLabel", info.getMountLabel());
        md.put("name", info.getName());
        md.put("processLabel", info.getProcessLabel());
        md.put("volumesRW", String.valueOf(info.getVolumesRW()));
        md.put("resolvConfPath", info.getResolvConfPath());
        md.put("args", Arrays.toString(info.getArgs()));
        md.put("volumes", String.valueOf(info.getVolumes()));
        md.put("restartCount", String.valueOf(info.getRestartCount()));
        md.put("logPath", String.valueOf(info.getLogPath()));
        md.put("config.domainName", info.getConfig().getDomainName());
        md.put("config.hostname", info.getConfig().getHostname());
        md.put("config.image", info.getConfig().getImage());
        md.put("config.user", info.getConfig().getUser());
        md.put("config.workingDir", info.getConfig().getWorkingDir());
        md.put("config.cmd", Arrays.toString(info.getConfig().getCmd()));
        md.put("config.volumes", String.valueOf(info.getConfig().getVolumes()));
        md.put("config.cpuset", info.getConfig().getCpuset());
        md.put("config.entrypoint", info.getConfig().getEntrypoint());
        md.put("config.exposedPorts", String.valueOf(info.getConfig().getExposedPorts()));
        md.put("config.macAddress", info.getConfig().getMacAddress());
        md.put("config.securityOpts", Arrays.toString(info.getConfig().getSecurityOpts()));
        md.put("config.cpuShares", Integer.toString(info.getConfig().getCpuShares()));
        md.put("config.env", Arrays.toString(info.getConfig().getEnv()));
        md.put("config.memory", Long.toString(info.getConfig().getMemory()));
        md.put("config.memorySwap", Long.toString(info.getConfig().getMemorySwap()));
        md.put("config.attachStderr", Boolean.toString(info.getConfig().isAttachStderr()));
        md.put("config.attachStdin", Boolean.toString(info.getConfig().isAttachStdin()));
        md.put("config.attachStdout", Boolean.toString(info.getConfig().isAttachStdout()));
        md.put("config.networkDisabled", Boolean.toString(info.getConfig().isNetworkDisabled()));
        md.put("config.openStdin", Boolean.toString(info.getConfig().isOpenStdin()));
        md.put("config.stdinOnce", Boolean.toString(info.getConfig().isStdinOnce()));
        md.put("config.tty", Boolean.toString(info.getConfig().isTty()));
        md.put("config.labels", String.valueOf(info.getConfig().getLabels()));
        md.put("state.startedAt", info.getState().getStartedAt());
        md.put("state.exitCode", Integer.toString(info.getState().getExitCode()));
        md.put("state.pid", Integer.toString(info.getState().getPid()));
        md.put("state.running", Boolean.toString(info.getState().isRunning()));
        md.put("state.finishedAt", info.getState().getFinishedAt());
        md.put("state.paused", Boolean.toString(info.getState().isPaused()));
        md.put("state.restarting", Boolean.toString(info.getState().isRestarting()));
        md.put("state.dead", String.valueOf(info.getState().isDead()));
        md.put("state.OOMKilled", String.valueOf(info.getState().isOOMKilled()));
        md.put("state.error", info.getState().getError());
        md.put("network.bridge", info.getNetworkSettings().getBridge());
        md.put("network.gateway", info.getNetworkSettings().getGateway());
        md.put("network.ipAddress", info.getNetworkSettings().getIpAddress());
        md.put("network.ipPrefixLen", Integer.toString(info.getNetworkSettings().getIpPrefixLen()));
        md.put("network.portMappings", Arrays.toString(info.getNetworkSettings().getPortMapping()));
        md.put("network.macAddress", info.getNetworkSettings().getMacAddress());
        md.put("network.ports", String.valueOf(info.getNetworkSettings().getPorts()));
        md.put("network.linkLocalIPv6PrefixLen", String.valueOf(info.getNetworkSettings().getLinkLocalIPv6PrefixLen()));
        md.put("network.globalIPv6Address", info.getNetworkSettings().getGlobalIPv6Address());
        md.put("network.globalIPv6PrefixLen", String.valueOf(info.getNetworkSettings().getGlobalIPv6PrefixLen()));
        md.put("network.iPv6Gateway", info.getNetworkSettings().getiPv6Gateway());
        md.put("network.linkLocalIPv6Address", info.getNetworkSettings().getLinkLocalIPv6Address());
        md.put("hostConfig.cgroupParent", info.getHostConfig().getCgroupParent());
        md.put("hostConfig.containerIDFile", info.getHostConfig().getContainerIDFile());
        md.put("hostConfig.cpusetCpus", info.getHostConfig().getCpusetCpus());
        md.put("hostConfig.ipcMode", info.getHostConfig().getIpcMode());
        md.put("hostConfig.memory", info.getHostConfig().getMemory());
        md.put("hostConfig.networkMode", info.getHostConfig().getNetworkMode());
        md.put("hostConfig.pidMode", info.getHostConfig().getPidMode());
        md.put("hostConfig.binds", Arrays.toString(info.getHostConfig().getBinds()));
        md.put("hostConfig.capAdd", Arrays.toString(info.getHostConfig().getCapAdd()));
        md.put("hostConfig.capDrop", Arrays.toString(info.getHostConfig().getCapDrop()));
        md.put("hostConfig.cpuShares", String.valueOf(info.getHostConfig().getCpuShares()));
        md.put("hostConfig.devices", Arrays.toString(info.getHostConfig().getDevices()));
        md.put("hostConfig.dns", Arrays.toString(info.getHostConfig().getDns()));
        md.put("hostConfig.dnsSearch", Arrays.toString(info.getHostConfig().getDnsSearch()));
        md.put("hostConfig.extraHosts", Arrays.toString(info.getHostConfig().getExtraHosts()));
        md.put("hostConfig.links", Arrays.toString(info.getHostConfig().getLinks()));
        md.put("hostConfig.logConfig", String.valueOf(info.getHostConfig().getLogConfig()));
        md.put("hostConfig.lxcConf", Arrays.toString(info.getHostConfig().getLxcConf()));
        md.put("hostConfig.memorySwap", String.valueOf(info.getHostConfig().getMemorySwap()));
        md.put("hostConfig.portBindings", String.valueOf(info.getHostConfig().getPortBindings()));
        md.put("hostConfig.restartPolicy", String.valueOf(info.getHostConfig().getRestartPolicy()));
        md.put("hostConfig.ulimits", Arrays.toString(info.getHostConfig().getUlimits()));
        md.put("hostConfig.volumesFrom", Arrays.toString(info.getHostConfig().getVolumesFrom()));

        return md;
    }

    @Override
    public String toJson() {
        return info.toString();
    }
}
