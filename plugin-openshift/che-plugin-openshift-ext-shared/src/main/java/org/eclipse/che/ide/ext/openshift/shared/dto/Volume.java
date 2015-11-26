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
package org.eclipse.che.ide.ext.openshift.shared.dto;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface Volume {
    GCEPersistentDiskVolumeSource getGcePersistentDisk();

    void setGcePersistentDisk(GCEPersistentDiskVolumeSource gcePersistentDisk);

    Volume withGcePersistentDisk(GCEPersistentDiskVolumeSource gcePersistentDisk);

    MetadataVolumeSource getMetadata();

    void setMetadata(MetadataVolumeSource metadata);

    Volume withMetadata(MetadataVolumeSource metadata);

    CinderVolumeSource getCinder();

    void setCinder(CinderVolumeSource cinder);

    Volume withCinder(CinderVolumeSource cinder);

    DownwardAPIVolumeSource getDownwardAPI();

    void setDownwardAPI(DownwardAPIVolumeSource downwardAPI);

    Volume withDownwardAPI(DownwardAPIVolumeSource downwardAPI);

    AWSElasticBlockStoreVolumeSource getAwsElasticBlockStore();

    void setAwsElasticBlockStore(AWSElasticBlockStoreVolumeSource awsElasticBlockStore);

    Volume withAwsElasticBlockStore(AWSElasticBlockStoreVolumeSource awsElasticBlockStore);

    SecretVolumeSource getSecret();

    void setSecret(SecretVolumeSource secret);

    Volume withSecret(SecretVolumeSource secret);

    FlockerVolumeSource getFlocker();

    void setFlocker(FlockerVolumeSource flocker);

    Volume withFlocker(FlockerVolumeSource flocker);

    ISCSIVolumeSource getIscsi();

    void setIscsi(ISCSIVolumeSource iscsi);

    Volume withIscsi(ISCSIVolumeSource iscsi);

    CephFSVolumeSource getCephfs();

    void setCephfs(CephFSVolumeSource cephfs);

    Volume withCephfs(CephFSVolumeSource cephfs);

    RBDVolumeSource getRbd();

    void setRbd(RBDVolumeSource rbd);

    Volume withRbd(RBDVolumeSource rbd);

    EmptyDirVolumeSource getEmptyDir();

    void setEmptyDir(EmptyDirVolumeSource emptyDir);

    Volume withEmptyDir(EmptyDirVolumeSource emptyDir);

    String getName();

    void setName(String name);

    Volume withName(String name);

    NFSVolumeSource getNfs();

    void setNfs(NFSVolumeSource nfs);

    Volume withNfs(NFSVolumeSource nfs);

    GlusterfsVolumeSource getGlusterfs();

    void setGlusterfs(GlusterfsVolumeSource glusterfs);

    Volume withGlusterfs(GlusterfsVolumeSource glusterfs);

    PersistentVolumeClaimVolumeSource getPersistentVolumeClaim();

    void setPersistentVolumeClaim(PersistentVolumeClaimVolumeSource persistentVolumeClaim);

    Volume withPersistentVolumeClaim(PersistentVolumeClaimVolumeSource persistentVolumeClaim);

    GitRepoVolumeSource getGitRepo();

    void setGitRepo(GitRepoVolumeSource gitRepo);

    Volume withGitRepo(GitRepoVolumeSource gitRepo);

    FCVolumeSource getFc();

    void setFc(FCVolumeSource fc);

    Volume withFc(FCVolumeSource fc);

    HostPathVolumeSource getHostPath();

    void setHostPath(HostPathVolumeSource hostPath);

    Volume withHostPath(HostPathVolumeSource hostPath);

}
