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
package org.eclipse.che.ide.ext.openshift.shared.dto;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface Parameter {
    String getDisplayName();

    void setDisplayName(String displayName);

    Parameter withDisplayName(String displayName);

    String getName();

    void setName(String name);

    Parameter withName(String name);

    String getDescription();

    void setDescription(String description);

    Parameter withDescription(String description);

    String getFrom();

    void setFrom(String from);

    Parameter withFrom(String from);

    String getValue();

    void setValue(String value);

    Parameter withValue(String value);

    String getGenerate();

    void setGenerate(String generate);

    Parameter withGenerate(String generate);

    boolean getRequired();

    void setRequired(boolean required);

    Parameter withRequired(boolean required);

}
