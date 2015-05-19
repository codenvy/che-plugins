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
package org.eclipse.che.plugin.docker.machine.local;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.SnapshotImpl;
import org.eclipse.che.api.machine.server.SnapshotStorage;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import javax.inject.Singleton;
import java.util.List;

/**
 * Dummy implementation of SnapshotStorage.
 * It does nothing, probably will be deleted later.
 * @author gazarenkov
 */
@Singleton
public class DummySnapshotStorage implements SnapshotStorage {

    @Override
    public SnapshotImpl getSnapshot(String snapshotId) throws NotFoundException, ServerException {
        throw new ServerException("Not available for dummy implementation");
    }

    @Override
    public void saveSnapshot(SnapshotImpl snapshot) throws ServerException {
        throw new ServerException("Not available for dummy implementation");
    }

    @Override
    public List<SnapshotImpl> findSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException {
        throw new ServerException("Not available for dummy implementation");
    }

    @Override
    public void removeSnapshot(String snapshotId) throws NotFoundException, ServerException {
        throw new ServerException("Not available for dummy implementation");
    }
}
