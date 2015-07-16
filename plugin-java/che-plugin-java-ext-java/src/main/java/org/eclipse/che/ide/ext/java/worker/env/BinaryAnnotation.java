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

import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.CharOperation;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.che.ide.ext.java.worker.env.json.AnnotationJso;
import org.eclipse.che.ide.ext.java.worker.env.json.ElementValuePairJso;

import java.util.Arrays;

/**
 * @author Evgen Vidolob
 */
public class BinaryAnnotation implements IBinaryAnnotation {

    private AnnotationJso jso;

    public BinaryAnnotation(AnnotationJso jso) {
        this.jso = jso;
    }

    @Override
    public char[] getTypeName() {
        if(jso.getTypeName() == null) return null;
        return jso.getTypeName().toCharArray();
    }

    @Override
    public IBinaryElementValuePair[] getElementValuePairs() {
        if(jso.getElementValuePairs() == null) return null;

        JsoArray<ElementValuePairJso> elementValuePairs = jso.getElementValuePairs();
        IBinaryElementValuePair[] pairs = new IBinaryElementValuePair[elementValuePairs.size()];
        for (int i = 0; i < elementValuePairs.size(); i++) {
            pairs[i] = new ElementValuePair(elementValuePairs.get(i));
        }
        return pairs;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + org.eclipse.che.ide.ext.java.jdt.internal.compiler.util.Util.hashCode(this.getElementValuePairs());
        result = prime * result + CharOperation.hashCode(this.getTypeName());
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
        BinaryAnnotation other = (BinaryAnnotation) obj;
        if (!Arrays.equals(this.getElementValuePairs(), other.getElementValuePairs())) {
            return false;
        }
        if (!Arrays.equals(this.getTypeName(), other.getTypeName())) {
            return false;
        }
        return true;
    }
}
