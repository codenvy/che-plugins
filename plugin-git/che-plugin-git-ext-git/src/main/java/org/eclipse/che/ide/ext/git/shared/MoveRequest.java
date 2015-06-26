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
 * Request to move or rename a file or directory.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: MoveRequest.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
@ApiModel
@DTO
public interface MoveRequest extends GitRequest {
    /** @return source */
	@ApiModelProperty("The source")
    String getSource();
    
    void setSource(String source);

    /** @return target */
    @ApiModelProperty("The target")
    String getTarget();
    
    void setTarget(String target);
}