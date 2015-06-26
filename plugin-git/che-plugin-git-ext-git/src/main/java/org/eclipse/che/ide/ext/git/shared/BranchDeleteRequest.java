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
 * Request to delete branch.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: BranchDeleteRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@ApiModel
@DTO
public interface BranchDeleteRequest extends GitRequest {
    /** @return name of branch to delete */
	@ApiModelProperty("name of the branch to delete")
    String getName();
    
    void setName(String name);
    
    BranchDeleteRequest withName(String name);

    /** @return if <code>true</code> then delete branch {@link #name} even if it is not fully merged */
    @ApiModelProperty("If true then delete the branch even if it is not fully merged")
    boolean isForce();
    
    void setForce(boolean isForce);
    
    BranchDeleteRequest withForce(boolean force);
}