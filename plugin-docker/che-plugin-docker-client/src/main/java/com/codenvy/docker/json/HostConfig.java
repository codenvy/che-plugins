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
package com.codenvy.docker.json;

import java.util.Arrays;
import java.util.Map;

/** @author andrew00x */
public class HostConfig {
    private String[]                   binds;
    private String[]                   links;
    private LxcConfParam[]             lxcConf;
    private Map<String, PortBinding[]> portBindings;
    private boolean                    publishAllPorts;
    private boolean                    privileged;
    private String[]                   dns;
    private String[]                   dnsSearch;
    private String[]                   extraHosts;
    private String[]                   volumesFrom;
    private String[]                   capAdd;
    private String[]                   capDrop;
    private RestartPolicy              restartPolicy;
    private String                     networkMode;
    private String[]                   devices;
    private String                     containerIDFile;

    public String[] getBinds() {
        return binds;
    }

    public void setBinds(String[] binds) {
        this.binds = binds;
    }

    public LxcConfParam[] getLxcConf() {
        return lxcConf;
    }

    public void setLxcConf(LxcConfParam[] lxcConf) {
        this.lxcConf = lxcConf;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    public Map<String, PortBinding[]> getPortBindings() {
        return portBindings;
    }

    public void setPortBindings(Map<String, PortBinding[]> portBindings) {
        this.portBindings = portBindings;
    }

    public boolean isPublishAllPorts() {
        return publishAllPorts;
    }

    public void setPublishAllPorts(boolean publishAllPorts) {
        this.publishAllPorts = publishAllPorts;
    }

    @Override
    public String toString() {
        return "HostConfig{" +
               "binds=" + Arrays.toString(binds) +
               ", links=" + Arrays.toString(links) +
               ", lxcConf=" + Arrays.toString(lxcConf) +
               ", portBindings=" + portBindings +
               ", publishAllPorts=" + publishAllPorts +
               ", privileged=" + privileged +
               ", dns=" + Arrays.toString(dns) +
               ", dnsSearch=" + Arrays.toString(dnsSearch) +
               ", extraHosts=" + Arrays.toString(extraHosts) +
               ", volumesFrom=" + Arrays.toString(volumesFrom) +
               ", capAdd=" + Arrays.toString(capAdd) +
               ", capDrop=" + Arrays.toString(capDrop) +
               ", restartPolicy=" + restartPolicy +
               ", networkMode='" + networkMode + '\'' +
               ", devices=" + Arrays.toString(devices) +
               ", containerIDFile=" + containerIDFile +
               '}';
    }

    // ----------------------

    public HostConfig withBinds(String... binds) {
        this.binds = binds;
        return this;
    }

    public HostConfig withLxcConf(LxcConfParam... lxcConf) {
        this.lxcConf = lxcConf;
        return this;
    }

    public HostConfig withPrivileged(boolean privileged) {
        this.privileged = privileged;
        return this;
    }

    public HostConfig withPortBindings(Map<String, PortBinding[]> portBindings) {
        this.portBindings = portBindings;
        return this;
    }

    public HostConfig withPublishAllPorts(boolean publishAllPorts) {
        this.publishAllPorts = publishAllPorts;
        return this;
    }

    public String[] getDevices() {
        return devices;
    }

    public void setDevices(String[] devices) {
        this.devices = devices;
    }

    public HostConfig withDevices(String[] devices) {
        this.devices = devices;
        return this;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public HostConfig withNetworkMode(String networkMode) {
        this.networkMode = networkMode;
        return this;
    }

    public RestartPolicy getRestartPolicy() {
        return restartPolicy;
    }

    public void setRestartPolicy(RestartPolicy restartPolicy) {
        this.restartPolicy = restartPolicy;
    }

    public HostConfig withRestartPolicy(RestartPolicy restartPolicy) {
        this.restartPolicy = restartPolicy;
        return this;
    }

    public String[] getCapDrop() {
        return capDrop;
    }

    public void setCapDrop(String[] capDrop) {
        this.capDrop = capDrop;
    }

    public HostConfig withCapDrop(String[] capDrop) {
        this.capDrop = capDrop;
        return this;
    }

    public String[] getCapAdd() {
        return capAdd;
    }

    public void setCapAdd(String[] capAdd) {
        this.capAdd = capAdd;
    }

    public HostConfig withCapAdd(String[] capAdd) {
        this.capAdd = capAdd;
        return this;
    }

    public String[] getVolumesFrom() {
        return volumesFrom;
    }

    public void setVolumesFrom(String[] volumesFrom) {
        this.volumesFrom = volumesFrom;
    }

    public HostConfig withVolumesFrom(String[] volumesFrom) {
        this.volumesFrom = volumesFrom;
        return this;
    }

    public String[] getExtraHosts() {
        return extraHosts;
    }

    public void setExtraHosts(String[] extraHosts) {
        this.extraHosts = extraHosts;
    }

    public HostConfig withExtraHosts(String[] extraHosts) {
        this.extraHosts = extraHosts;
        return this;
    }

    public String[] getDnsSearch() {
        return dnsSearch;
    }

    public void setDnsSearch(String[] dnsSearch) {
        this.dnsSearch = dnsSearch;
    }

    public HostConfig withDnsSearch(String[] dnsSearch) {
        this.dnsSearch = dnsSearch;
        return this;
    }

    public String[] getDns() {
        return dns;
    }

    public void setDns(String[] dns) {
        this.dns = dns;
    }

    public HostConfig withDns(String[] dns) {
        this.dns = dns;
        return this;
    }

    public String[] getLinks() {
        return links;
    }

    public void setLinks(String[] links) {
        this.links = links;
    }

    public HostConfig withLinks(String[] links) {
        this.links = links;
        return this;
    }

    public String getContainerIDFile() {
        return containerIDFile;
    }

    public void setContainerIDFile(String containerIDFile) {
        this.containerIDFile = containerIDFile;
    }
}
