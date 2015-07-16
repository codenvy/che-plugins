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

package org.eclipse.che.ide.ext.java.jdt.internal.core;

/**
 * @author Evgen Vidolob
 */
public class SelectionResult {

    private Type type;

    private String fqn;

    private String key;

    private int offset;

    private boolean declaration;

    private boolean source;

    public SelectionResult(Type type, String fqn, String key, int offset, boolean declaration, boolean source) {
        this.type = type;
        this.fqn = fqn;
        this.key = key;
        this.offset = offset;
        this.declaration = declaration;
        this.source = source;
    }

    public Type getType() {
        return type;
    }

    public String getFqn() {
        return fqn;
    }

    public String getKey() {
        return key;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isDeclaration() {
        return declaration;
    }

    public boolean isSource() {
        return source;
    }

    public enum Type{
        METHOD, FIELD, CLASS, VARIABLE, TYPE_PARAMETER, METHOD_TYPE_PARAMETER, PACKAGE;
    }
}
