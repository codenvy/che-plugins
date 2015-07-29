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
package com.codenvy.docker.json;

/**
 * @author andrew00x
 */
public class ImageInfo {
    private String          architecture;
    private String          author;
    private String          comment;
    private ImageConfig     config;
    // Date format: yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX
    private String          created;
    private String          dockerVersion;
    private String          id;
    private String          os;
    private String          parent;
    private long            size;
    private long            virtualSize;
    private String          container;
    private ContainerConfig containerConfig;

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ImageConfig getConfig() {
        return config;
    }

    public void setConfig(ImageConfig config) {
        this.config = config;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }

    public void setDockerVersion(String dockerVersion) {
        this.dockerVersion = dockerVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
               "architecture='" + architecture + '\'' +
               ", author='" + author + '\'' +
               ", comment='" + comment + '\'' +
               ", config=" + config +
               ", created='" + created + '\'' +
               ", dockerVersion='" + dockerVersion + '\'' +
               ", id='" + id + '\'' +
               ", os='" + os + '\'' +
               ", parent='" + parent + '\'' +
               ", size=" + size +
               ", virtualSize=" + virtualSize +
               ", container='" + container + '\'' +
               ", containerConfig=" + containerConfig +
               '}';
    }
}
