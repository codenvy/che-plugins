/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.jdt.internal.core.search.matching;

import org.eclipse.che.jdt.internal.core.builder.CodenvyClasspathLocation;
import org.eclipse.che.jdt.internal.core.util.ResourceCompilationUnit;
import org.eclipse.che.jdt.internal.core.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.codeassist.ISearchRequestor;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class ClasspathSourceDirectory extends CodenvyClasspathLocation {
    private static final Logger                    LOG                  = LoggerFactory.getLogger(ClasspathSourceDirectory.class);
    //uses as marker for packages that isn't from this  source directory
    private final        Future<SimpleLookupTable> missingPackageHolder =
            new FutureTask<>(new Callable<SimpleLookupTable>() {
                @Override
                public SimpleLookupTable call() throws Exception {
                    return null;
                }
            });
    File sourceFolder;
    //    SimpleLookupTable directoryCache;
    volatile ConcurrentHashMap<String, Future<SimpleLookupTable>> directoryCache;
    volatile Set<String>                                          packagesCache;
    char[][] fullExclusionPatternChars;
    char[][] fulInclusionPatternChars;

    ClasspathSourceDirectory(File sourceFolder, char[][] fullExclusionPatternChars, char[][] fulInclusionPatternChars) {
        this.sourceFolder = sourceFolder;
        this.directoryCache = new ConcurrentHashMap<>(5);
        this.fullExclusionPatternChars = fullExclusionPatternChars;
        this.fulInclusionPatternChars = fulInclusionPatternChars;
    }

    public void cleanup() {
        this.directoryCache = new ConcurrentHashMap<>(5);
        packagesCache = null;
    }

    SimpleLookupTable directoryTable(final String qualifiedPackageName) {
        final ConcurrentHashMap<String, Future<SimpleLookupTable>> directoryCache = this.directoryCache;
        Future<SimpleLookupTable> future = directoryCache.get(qualifiedPackageName);
        if (future == missingPackageHolder) {
            // package exists in another classpath directory or jar
            return null;
        }
        if (future == null) {
            FutureTask<SimpleLookupTable> newFuture = new FutureTask<>(new Callable<SimpleLookupTable>() {
                @Override
                public SimpleLookupTable call() throws Exception {
                    File container = new File(sourceFolder, qualifiedPackageName);
                    SimpleLookupTable dirTable = new SimpleLookupTable();
                    if (container.isDirectory()) {
                        try (DirectoryStream<Path> members = Files.newDirectoryStream(container.toPath())) {
                            for (Path member : members) {
                                String name;
                                if (!member.toFile().isDirectory()) {
                                    int index = Util.indexOfJavaLikeExtension(name = member.getFileName().toString());
                                    if (index >= 0) {
                                        String fullPath = member.toAbsolutePath().toString();
                                        if (!org.eclipse.jdt.internal.compiler.util.Util
                                                .isExcluded(fullPath.toCharArray(), fulInclusionPatternChars,
                                                            fullExclusionPatternChars, false/*not a folder path*/)) {
                                            dirTable.put(name.substring(0, index), member.toString());
                                        }
                                    }
                                }
                            }
                            return dirTable;
                        }
                    }
                    directoryCache.put(qualifiedPackageName, missingPackageHolder);
                    return null;
                }
            });
            future = directoryCache.putIfAbsent(qualifiedPackageName, newFuture);
            if (future == null) {
                future = newFuture;
                newFuture.run();
            }
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while reading source directory", e);
        }
        directoryCache.put(qualifiedPackageName, missingPackageHolder);
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClasspathSourceDirectory)) return false;

        return this.sourceFolder.equals(((ClasspathSourceDirectory)o).sourceFolder);
    }

    public NameEnvironmentAnswer findClass(String sourceFileWithoutExtension, String qualifiedPackageName,
                                           String qualifiedSourceFileWithoutExtension) {
        SimpleLookupTable dirTable = directoryTable(qualifiedPackageName);
        if (dirTable != null && dirTable.elementSize > 0) {
            String file = (String)dirTable.get(sourceFileWithoutExtension);
            if (file != null) {
                return new NameEnvironmentAnswer(new ResourceCompilationUnit(new File(file)),
                                                 null /* no access restriction */);
            }
        }
        return null;
    }

    public IPath getProjectRelativePath() {
//	return this.sourceFolder.getProjectRelativePath();
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        return this.sourceFolder == null ? super.hashCode() : this.sourceFolder.hashCode();
    }

    public boolean isPackage(String qualifiedPackageName) {
        return directoryTable(qualifiedPackageName) != null;
    }

    public void reset() {
        cleanup();
    }

    public String toString() {
        return "Source classpath directory " + this.sourceFolder.getPath(); //$NON-NLS-1$
    }

    public String debugPathString() {
        return this.sourceFolder.getPath();
    }

    @Override
    public void findPackages(String[] pkgName, ISearchRequestor requestor) {
        Set<String> packages = packagesCache;
        if (packages == null) {
            synchronized (this) {
                packages = packagesCache;
                if (packages == null) {
                    packages = new HashSet<>();
                    packages.add("");
                    fillPackagesCache(sourceFolder, "", packages);
                    packagesCache = packages;
                }
            }
        }

        String pkg = org.eclipse.jdt.internal.core.util.Util.concatWith(pkgName, '.');
        for (String s : packages) {
            if (s.startsWith(pkg)) {
                requestor.acceptPackage(s.toCharArray());
            }
        }
    }

    private void fillPackagesCache(File parentFolder, String parentPackage, Set<String> cache) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parentFolder.toPath())) {
            for (Path path : directoryStream) {
                if (path.toFile().isDirectory()) {
                    if (org.eclipse.jdt.internal.core.util.Util.isValidFolderNameForPackage(path.getFileName().toString(), "1.7", "1.7")) {
                        String pack = parentPackage + "." + path.getFileName();
                        cache.add(pack);
                        fillPackagesCache(path.toFile(), pack, cache);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Can't read packages", e);
        }
    }
}
