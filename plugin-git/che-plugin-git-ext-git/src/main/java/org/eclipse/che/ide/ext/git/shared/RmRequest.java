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
 * Request to remove files.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: RmRequest.java 22817 2011-03-22 09:17:52Z andrew00x $
 */
@ApiModel
@DTO
public interface RmRequest extends GitRequest {
    /** @return files to remove */
	@ApiModelProperty("List of items to remove")
    List<String> getItems();
    
    void setItems(List<String> items);
    
    RmRequest withItems(List<String> items);

    /** @return is RmRequest represents remove from index only */
    @ApiModelProperty("If true, remove only from the index")
    boolean isCached();
    
    void setCached(boolean isCached);
    
    RmRequest withCached(boolean cached);

    @ApiModelProperty("if true, apply recursively")
    boolean isRecursively();

    void setRecursively(boolean isRecursively);

    RmRequest withRecursively(boolean isRecursively);
}