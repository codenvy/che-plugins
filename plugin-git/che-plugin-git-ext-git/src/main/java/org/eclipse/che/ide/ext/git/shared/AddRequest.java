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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Request to add content of working tree to Git index. This action prepares content to next commit.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: AddRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@ApiModel
@DTO
public interface AddRequest extends GitRequest {
    /** Default file pattern that will be used if {@link #filepattern} is not set. All content of working tree will be added in index. */
    List<String> DEFAULT_PATTERN = new ArrayList<String>(Arrays.asList("."));

    /** @return files to add content from */
    @ApiModelProperty("A list of files to add to the index")
    List<String> getFilepattern();
    
    void setFilepattern(List<String> pattern);
    
    AddRequest withFilepattern(List<String> filepattern);

    /**
     * @return if <code>true</code> than never stage new files, but stage modified new contents of tracked files. It will remove files from
     *         the index if the corresponding files in the working tree have been removed. If <code>false</code> then new files and
     *         modified
     *         files added to the index.
     */
    @ApiModelProperty("if true then never stage new files, only new contents of tracked files. If false then new and modified files are added to the index")
    boolean isUpdate();
    
    void setUpdate(boolean isUpdate);
    
    AddRequest withUpdate(boolean isUpdate);
}