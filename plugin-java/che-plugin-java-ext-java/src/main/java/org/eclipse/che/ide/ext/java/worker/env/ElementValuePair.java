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
package org.eclipse.che.ide.ext.java.worker.env;

import org.eclipse.che.ide.ext.java.jdt.core.compiler.CharOperation;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.che.ide.ext.java.worker.env.json.ElementValuePairJso;

import java.util.Arrays;

/**
 * @author Evgen Vidolob
 */
public class ElementValuePair implements IBinaryElementValuePair {

    private ElementValuePairJso jso;

    public ElementValuePair(ElementValuePairJso jso) {
        this.jso = jso;
    }

    @Override
    public char[] getName() {
        if(jso.getName() == null) return null;
        return jso.getName().toCharArray();
    }

    @Override
    public Object getValue() {
        return Util.getDefaultValue(jso.getValue());
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CharOperation.hashCode(this.getName());
        result = prime * result + ((this.getValue() == null) ? 0 : this.getValue().hashCode());
        return result;
    }
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ElementValuePair other = (ElementValuePair) obj;
        if (!Arrays.equals(this.getName(), other.getName())) {
            return false;
        }
        if (this.getValue() == null) {
            if (other.getValue() != null) {
                return false;
            }
        } else if (!this.getValue().equals(other.getValue())) {
            return false;
        }
        return true;
    }
}
