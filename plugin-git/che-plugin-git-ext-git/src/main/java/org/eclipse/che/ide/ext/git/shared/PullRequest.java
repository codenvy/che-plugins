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

/**
 * Request to pull (fetch and merge) changes from remote repository to local branch.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: PullRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@ApiModel
@DTO
public interface PullRequest extends GitRequest {
    /** @return refspec to fetch */
	@ApiModelProperty("The refspec to fetch")
    String getRefSpec();
    
    void setRefSpec(String refSpec);
    
    PullRequest withRefSpec(String refSpec);

    /** @return remote name. If <code>null</code> then 'origin' will be used */
    @ApiModelProperty("remote name. If null then 'origin' will be used")
    String getRemote();
    
    void setRemote(String remote);
    
    PullRequest withRemote(String remote);

    /** @return time (in seconds) to wait without data transfer occurring before aborting fetching data from remote repository */
    @ApiModelProperty("time (in seconds) to wait without data transfer occurring before aborting fetching data from remote repository")
    int getTimeout();
    
    void setTimeout(int timeout);
    
    PullRequest withTimeout(int timeout);
}