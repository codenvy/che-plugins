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
 * Request to add remote configuration {@link #name} for repository at {@link #url}.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: RemoteAddRequest.java 67651 2011-03-25 16:15:36Z andrew00x $
 */
@ApiModel
@DTO
public interface RemoteAddRequest extends GitRequest {
    /** @return remote name */
	@ApiModelProperty("The remote name")
    String getName();
    
    void setName(String name);
    
    RemoteAddRequest withName(String name);

    /** @return repository url */
    @ApiModelProperty("The remote repository URL")
    String getUrl();
    
    void setUrl(String url);
    
    RemoteAddRequest withUrl(String url);

    /** @return list of tracked branches in remote repository */
    @ApiModelProperty("The list of tracked branches in the remote repository")
    List<String> getBranches();
    
    void setBranches(List<String> branches);
}