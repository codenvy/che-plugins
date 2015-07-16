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
package org.eclipse.che.ide.ext.java.worker;

import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;

import java.util.HashMap;

/**
 * Cache for CompilationUnit
 * @author Evgen Vidolob
 */
public class WorkerCuCache {

    private HashMap<String, CompilationUnit> cache = new HashMap<>(5);

    private HashMap<String, String> sourceCache = new HashMap<>(5);

    public CompilationUnit getCompilationUnit(String filePath) {
        return cache.get(filePath);
    }

    public void putCompilationUnit(String filePath, CompilationUnit compilationUnit, String source) {
        cache.put(filePath, compilationUnit);
        sourceCache.put(filePath, source);
    }

    public void removeCompilationUnit(String filePath) {
        cache.remove(filePath);
        sourceCache.remove(filePath);
    }

    public String getSource(String filePath) {
        return sourceCache.get(filePath);
    }
}
