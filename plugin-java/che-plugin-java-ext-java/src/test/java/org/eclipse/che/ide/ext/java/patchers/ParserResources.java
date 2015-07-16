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
package org.eclipse.che.ide.ext.java.patchers;

import org.eclipse.che.ide.ext.java.jdt.internal.compiler.parser.RSC;
import com.google.gson.Gson;
import com.googlecode.gwt.test.patchers.PatchClass;
import com.googlecode.gwt.test.patchers.PatchMethod;

import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Evgen Vidolob
 */
@PatchClass(RSC.class)
public class ParserResources {


    @PatchMethod
    public static char[] parser1(RSC rsc) {
      return readFileChars("parser1.rscjson");
    }

    @PatchMethod
    public static char[] parser2(RSC rsc) {
        return readFileChars("parser2.rscjson");
    }

    @PatchMethod
    public static char[] parser3(RSC rsc) {
        return readFileChars("parser3.rscjson");
    }

    @PatchMethod
    public static char[] parser4(RSC rsc) {
        return readFileChars("parser4.rscjson");
    }

    @PatchMethod
    public static char[] parser5(RSC rsc) {
        return readFileChars("parser5.rscjson");
    }

    @PatchMethod
    public static char[] parser6(RSC rsc) {
        return readFileChars("parser6.rscjson");
    }

    @PatchMethod
    public static char[] parser7(RSC rsc) {
        return readFileChars("parser7.rscjson");
    }

    @PatchMethod
    public static char[] parser8(RSC rsc) {
        return readFileChars("parser8.rscjson");
    }

    @PatchMethod
    public static char[] parser9(RSC rsc) {
        return readFileChars("parser9.rscjson");
    }

    @PatchMethod
    public static char[] parser10(RSC rsc) {
        return readFileChars("parser10.rscjson");
    }

    @PatchMethod
    public static char[] parser11(RSC rsc) {
        return readFileChars("parser11.rscjson");
    }

    @PatchMethod
    public static char[] parser12(RSC rsc) {
        return readFileChars("parser12.rscjson");
    }

    @PatchMethod
    public static char[] parser13(RSC rsc) {
        return readFileChars("parser13.rscjson");
    }

    @PatchMethod
    public static char[] parser14(RSC rsc) {
        return readFileChars("parser14.rscjson");
    }

    @PatchMethod
    public static char[] parser15(RSC rsc) {
        return readFileChars("parser15.rscjson");
    }

    @PatchMethod
    public static char[] parser16(RSC rsc) {
        return readFileChars("parser16.rscjson");
    }

    @PatchMethod
    public static byte[] parser17(RSC rsc) {
        return readFileBytes("parser17.rsc");
    }

    @PatchMethod
    public static byte[] parser18(RSC rsc) {
        return readFileBytes("parser18.rsc");
    }

    @PatchMethod
    public static byte[] parser19(RSC rsc) {
        return readFileBytes("parser19.rsc");
    }

    @PatchMethod
    public static char[] parser20(RSC rsc) {
        return readFileChars("parser20.rscjson");
    }

    @PatchMethod
    public static long[] parser21(RSC rsc) {
        return readFileLongs("parser21.rsc");
    }


    @PatchMethod
    public static char[] parser22(RSC rsc) {
        return readFileChars("parser22.rscjson");
    }

    @PatchMethod
    public static char[] parser23(RSC rsc) {
        return readFileChars("parser23.rscjson");
    }

    @PatchMethod
    public static char[] parser24(RSC rsc) {
        return readFileChars("parser24.rscjson");
    }

    @PatchMethod
    public static String[] readableNames(RSC rsc) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/eclipse/che/ide/ext/java/patchers/readableNames");
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(inputStream);
        return  gson.fromJson(reader, String[].class);
    }

    private static long[] readFileLongs(String fileName) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/eclipse/che/ide/ext/java/patchers/" + fileName);
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(inputStream);
        double[] json = gson.fromJson(reader, double[].class);
        return double2long(json);
    }

    private static char[] readFileChars(String fileName){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/eclipse/che/ide/ext/java/patchers/" + fileName);
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(inputStream);
        String[] json = gson.fromJson(reader, String[].class);
        return strings2char(json);
    }
    private static byte[] readFileBytes(String fileName){
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/eclipse/che/ide/ext/java/patchers/" + fileName);
        Gson gson = new Gson();
        InputStreamReader reader = new InputStreamReader(inputStream);
        String[] json = gson.fromJson(reader, String[].class);
        return strings2byte(json);
    }

    private static byte[] strings2byte(String[] json) {
        byte[] result = new byte[json.length];
        for (int i = 0; i < json.length; i++) {
            result[i] = Byte.valueOf(json[i]);
        }
        return result;
    }

    private static char[] strings2char(String[] strings) {
        char[] result = new char[strings.length];
        for (int i = 0; i < strings.length; i++) {
            result[i] = strings[i].charAt(0);
        }
        return result;
    }

    private static long[] double2long(double[] json) {
        long[] result = new long[json.length];
        for (int i = 0; i < json.length; i++) {
            result[i] = (long)json[i];
        }
        return result;
    }

}
