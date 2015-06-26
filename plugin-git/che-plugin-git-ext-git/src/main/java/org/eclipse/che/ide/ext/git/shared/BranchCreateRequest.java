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
 * Request to create new branch.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: BranchCreateRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@DTO
@ApiModel
public interface BranchCreateRequest extends GitRequest {
    /** @return name of branch to be created */
	@ApiModelProperty("name of the branch to be created")
    String getName();
    
    void setName(String name);
    
    BranchCreateRequest withName(String name);

    /** @return name of a commit at which to start the new branch. If <code>null</code> the HEAD will be used */
    @ApiModelProperty("name of a commit at which to start the new branch. If <code>null</code> the HEAD will be used")
    String getStartPoint();
    
    void setStartPoint(String startPoint);
    
    BranchCreateRequest withStartPoint(String startPoint);
}