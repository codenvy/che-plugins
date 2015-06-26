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
 * Request to init git repository.
 *
 * @author andrew00x
 */
@ApiModel
@DTO
public interface InitRequest extends GitRequest {
    /** @return working directory for new git repository */
	@ApiModelProperty("The working directory for the new git repository")
    String getWorkingDir();

    void setWorkingDir(String workingDir);

    InitRequest withWorkingDir(String workingDir);
    
    /** @return <code>true</code> then bare repository created */
    @ApiModelProperty("If true then a bare repository will be created")
    boolean isBare();
    
    void setBare(boolean bare);
    
    InitRequest withBare(boolean bare);

    /** @return <code>true</code> then all files in newly initialized repository will be commited with "init" message  */
    @ApiModelProperty("If true then all files in the newly initialized repository's work directory will be committed with an 'init' message")
    boolean isInitCommit();

    void setInitCommit(boolean initCommit);

    InitRequest withInitCommit(boolean initCommit);
}