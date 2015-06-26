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
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: BranchListRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@DTO
@ApiModel
public interface BranchListRequest extends GitRequest {
    /**
     * Show both remote and local branches. <br/>
     * Corresponds to -a option in C git.
     */
    public static final String LIST_ALL    = "a";
    /**
     * Show both remote branches. <br/>
     * Corresponds to -r option in C git.
     */
    public static final String LIST_REMOTE = "r";
    public static final String LIST_LOCAL  = null;

    /** @return branches list mode */
    @ApiModelProperty(allowableValues="[a,r,null]")
    String getListMode();
    
    void setListMode(String listMode);
    
    BranchListRequest withListMode(String listMode);
}