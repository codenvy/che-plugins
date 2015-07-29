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
public class ProgressStatus {
    private String         id;
    private String         status;
    private String         progress;
    private String         stream;
    private String         error;
    private ProgressDetail progressDetail;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public ProgressDetail getProgressDetail() {
        return progressDetail;
    }

    public void setProgressDetail(ProgressDetail progressDetail) {
        this.progressDetail = progressDetail;
    }

    @Override
    public String toString() {
        return "ProgressStatus{" +
               "id='" + id + '\'' +
               ", status='" + status + '\'' +
               ", progress='" + progress + '\'' +
               ", stream='" + stream + '\'' +
               ", error='" + error + '\'' +
               ", progressDetail=" + progressDetail +
               '}';
    }
}
