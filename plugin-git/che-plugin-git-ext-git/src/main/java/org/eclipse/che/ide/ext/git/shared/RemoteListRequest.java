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
 * Request to get list of remotes. If {@link #remote} is specified then info about this remote only given.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: RemoteListRequest.java 68015 2011-04-06 09:21:31Z anya $
 */
@ApiModel
@DTO
public interface RemoteListRequest extends GitRequest {
    /** @return if <code>true</code> show remote url and name otherwise show remote name only */
	@ApiModelProperty("True to return the remote URL, false to return only the remote name")
    boolean isVerbose();
    
    void setVerbose(boolean isVerbose);
    
    RemoteListRequest withVerbose(boolean verbose);

    /** @return remote name */
    @ApiModelProperty("The remote name")
    String getRemote();
    
    RemoteListRequest withRemote(String remote);
    
    void setRemote(String remote);
}