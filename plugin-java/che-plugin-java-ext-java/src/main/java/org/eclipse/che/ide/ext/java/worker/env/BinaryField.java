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
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.Constant;
import org.eclipse.che.ide.ext.java.worker.env.json.AnnotationJso;
import org.eclipse.che.ide.ext.java.worker.env.json.FieldJso;

/**
 * @author Evgen Vidolob
 */
public class BinaryField implements IBinaryField {

    private FieldJso jso;
    private Constant constant;

    public BinaryField(FieldJso jso) {
        this.jso = jso;
    }

    @Override
    public IBinaryAnnotation[] getAnnotations() {
        JsoArray<AnnotationJso> annotations = jso.getAnnotations();
        if (annotations == null || annotations.isEmpty()) return null;
        IBinaryAnnotation[] binaryAnnotations = new IBinaryAnnotation[annotations.size()];
        for (int i = 0; i < annotations.size(); i++) {
            binaryAnnotations[i] = new BinaryAnnotation(annotations.get(i));
        }
        return binaryAnnotations;
    }

    @Override
    public Constant getConstant() {
        if (jso.getConstant() == null) return null;
        if(constant == null){
           constant = Util.getConstant(jso.getConstant());
        }
        return constant;
    }

    @Override
    public char[] getGenericSignature() {
        if(jso.getGenericSignature() == null) return null;
        return jso.getGenericSignature().toCharArray();
    }

    @Override
    public char[] getName() {
        if(jso.getName() == null) return null;
        return jso.getName().toCharArray();
    }

    @Override
    public long getTagBits() {
        return Long.parseLong(jso.getTagBits());
    }

    @Override
    public char[] getTypeName() {
        if(jso.getTypeName() == null) return null;
        return jso.getTypeName().toCharArray();
    }

    @Override
    public int getModifiers() {
        return jso.getModifiers();
    }

    public boolean equals(Object o) {
        if (!(o instanceof BinaryField)) {
            return false;
        }
        return CharOperation.equals(getName(), ((BinaryField)o).getName());
    }
    public int hashCode() {
        return CharOperation.hashCode(getName());
    }
}
