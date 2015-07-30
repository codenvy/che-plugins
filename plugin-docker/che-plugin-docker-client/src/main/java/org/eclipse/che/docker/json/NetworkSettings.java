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
package org.eclipse.che.docker.json;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
public class NetworkSettings {
    private String                         ipAddress;
    private int                            iPPrefixLen;
    private String                         gateway;
    private String                         bridge; // TODO : check it
    private String[]                       portMapping;
    private String                         macAddress;
    private Map<String, List<PortBinding>> ports;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getIpPrefixLen() {
        return iPPrefixLen;
    }

    public void setIpPrefixLen(int iPPrefixLen) {
        this.iPPrefixLen = iPPrefixLen;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getBridge() {
        return bridge;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
    }

    public String[] getPortMapping() {
        return portMapping;
    }

    public void setPortMapping(String[] portMapping) {
        this.portMapping = portMapping;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Map<String, List<PortBinding>> getPorts() {
        return ports;
    }

    public void setPorts(Map<String, List<PortBinding>> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "NetworkSettings{" +
               "ipAddress='" + ipAddress + '\'' +
               ", iPPrefixLen=" + iPPrefixLen +
               ", gateway='" + gateway + '\'' +
               ", bridge='" + bridge + '\'' +
               ", portMapping=" + Arrays.toString(portMapping) +
               ", macAddress=" + macAddress +
               ", ports=" + ports +
               '}';
    }
}
