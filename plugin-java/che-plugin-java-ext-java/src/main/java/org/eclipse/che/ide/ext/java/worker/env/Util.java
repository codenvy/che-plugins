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

import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.collections.JsonObject;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.ClassSignature;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.env.EnumConstantSignature;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.ByteConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.CharConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.Constant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.DoubleConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.FloatConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.IntConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.LongConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.ShortConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.impl.StringConstant;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.che.ide.ext.java.worker.env.json.AnnotationJso;

/**
 * @author Evgen Vidolob
 */
public class Util {

    public static char[][] arrayStringToCharArray(Array<String> strings) {
        if (strings == null) return null;
        if(strings.size() == 0) return null;
        char[][] result = new char[strings.size()][];
        for (int i = 0; i < strings.size(); i++) {
            result[i] = strings.get(i).toCharArray();
        }
        return result;
    }

    public static Object getDefaultValue(Jso jso){
        if(jso == null) return null;
        if(jso.hasOwnProperty("constant")) {
            return getConstant(jso.getJsObjectField("constant").<Jso>cast());
        } else if(jso.hasOwnProperty("class")) {
            return new ClassSignature(jso.getStringField("class").toCharArray());
        } else if (jso.hasOwnProperty("annotation")) {
            return new BinaryAnnotation(jso.getJsObjectField("annotation").<AnnotationJso>cast());
        }else if(jso.hasOwnProperty("enum")){
            JsonObject anEnum = jso.getObjectField("enum");
            return new EnumConstantSignature(anEnum.getStringField("typeName").toCharArray(),
                                             anEnum.getStringField("constantName").toCharArray());
        }
        else if(jso.hasOwnProperty("array")){
            JsoArray<JsonObject> array = jso.getArrayField("array");
            Object[] arr = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                arr[i] = getDefaultValue((Jso)array.get(i));
            }
            return arr;
        }
        return null;
    }

    public static Constant getConstant(Jso constant) {
        int typeId = constant.getIntField("typeId");
        Constant con = null;
        switch (typeId) {
            case TypeIds.T_int :
                con = IntConstant.fromValue(constant.getIntField("value"));
                break;
            case TypeIds.T_byte :
                con = ByteConstant.fromValue(Byte.parseByte(constant.getStringField("value")));
                break;
            case TypeIds.T_short :
                con = ShortConstant.fromValue((short)constant.getIntField("value"));
                break;
            case TypeIds.T_char :
                con = CharConstant.fromValue(constant.getStringField("value").charAt(0));
                break;
            case TypeIds.T_float :
                con = FloatConstant.fromValue(Float.valueOf(constant.getStringField("value")));
                break;
            case TypeIds.T_double :
                if(constant.hasOwnProperty("NotAConstant")){
                   con = Constant.NotAConstant;
                }
                else {
                    con = DoubleConstant.fromValue(Double.valueOf(constant.getStringField("value")));
                }
                break;
            case TypeIds.T_boolean :
                con = BooleanConstant.fromValue(constant.getBooleanField("value"));
                break;
            case TypeIds.T_long :
                con = LongConstant.fromValue(Long.parseLong(constant.getStringField("value")));
                break;
            case TypeIds.T_JavaLangString :
                con = StringConstant.fromValue(constant.getStringField("value"));
        }
        return con;
    }
}
