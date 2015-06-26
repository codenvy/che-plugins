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
package org.eclipse.che.ide.ext.git.shared;

import org.eclipse.che.dto.shared.DTO;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Request to update tracked repositories.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: RemoteUpdateRequest.java 67651 2011-03-25 16:15:36Z andrew00x $
 */
@ApiModel
@DTO
public interface RemoteUpdateRequest extends GitRequest {
    /** @return remote name */
	@ApiModelProperty("The remote name")
    String getName();
    
    void setName(String name);

    /** @return list tracked branches */
    @ApiModelProperty("List of tracked branches")
    List<String> getBranches();

    void setBranches(List<String> branches);
    
    /**
     * @return if <code>true</code> then {@link #branches} instead of replacing the list of currently tracked branches,
     *         added to that list
     */
    @ApiModelProperty("True to add the provided list of branches to the tracked branches list instead of replacing them")
    boolean isAddBranches();
    
    void setAddBranches(boolean isAddBranches);

    /** @return remote URLs to be added */
    @ApiModelProperty("List of remote URLs to add")
    List<String> getAddUrl();
    
    void setAddUrl(List<String> addUrl);

    /** @return remote URLs to be removed */
    @ApiModelProperty("List of remote URLs to be removed")
    List<String> getRemoveUrl();

    void setRemoveUrl(List<String> removeUrl);
    
    /** @return remote push URLs to be added */
    @ApiModelProperty("List of remote push URLs to add")
    List<String> getAddPushUrl();
    
    void setAddPushUrl(List<String> addPushUrl);

    /** @return remote push URLs to be removed */
    @ApiModelProperty("List of remote push URLs to remove")
    List<String> getRemovePushUrl();
    
    void setRemovePushUrl(List<String> removePushUrl);
}