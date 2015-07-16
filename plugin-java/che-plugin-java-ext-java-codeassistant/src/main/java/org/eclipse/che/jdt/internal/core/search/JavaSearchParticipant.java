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
package org.eclipse.che.jdt.internal.core.search;

import org.eclipse.che.jdt.core.search.IJavaSearchScope;
import org.eclipse.che.jdt.core.search.SearchDocument;
import org.eclipse.che.jdt.core.search.SearchParticipant;
import org.eclipse.che.jdt.core.search.SearchPattern;
import org.eclipse.che.jdt.core.search.SearchRequestor;
import org.eclipse.che.jdt.internal.core.JavaProject;
import org.eclipse.che.jdt.internal.core.search.indexing.BinaryIndexer;
import org.eclipse.che.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.che.jdt.internal.core.search.indexing.SourceIndexer;
import org.eclipse.che.jdt.internal.core.search.matching.MatchLocator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.core.index.IndexLocation;

/**
 * A search participant describes a particular extension to a generic search mechanism, allowing thus to
 * perform combined search actions which will involve all required participants
 * <p/>
 * A search scope defines which participants are involved.
 * <p/>
 * A search participant is responsible for holding index files, and selecting the appropriate ones to feed to
 * index queries. It also can map a document path to an actual document (note that documents could live outside
 * the workspace or no exist yet, and thus aren't just resources).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaSearchParticipant extends SearchParticipant {

    private ThreadLocal indexSelector = new ThreadLocal();
    private SourceIndexer sourceIndexer;
    private IndexManager  indexManager;
    private JavaProject   javaProject;

    public JavaSearchParticipant(IndexManager indexManager, JavaProject javaProject) {
        this.indexManager = indexManager;
        this.javaProject = javaProject;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#beginSearching()
     */
    public void beginSearching() {
        super.beginSearching();
        this.indexSelector.set(null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#doneSearching()
     */
    public void doneSearching() {
        this.indexSelector.set(null);
        super.doneSearching();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#getName()
     */
    public String getDescription() {
        return "Java"; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#getDocument(String)
     */
    public SearchDocument getDocument(String documentPath) {
        return new JavaSearchDocument(documentPath, this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#indexDocument(SearchDocument)
     */
    public void indexDocument(SearchDocument document, IPath indexPath) {
        // TODO must verify that the document + indexPath match, when this is not called from scheduleDocumentIndexing
        document.removeAllIndexEntries(); // in case the document was already indexed

        String documentPath = document.getPath();
        if (org.eclipse.che.jdt.internal.core.search.Util.isJavaLikeFileName(documentPath)) {
            this.sourceIndexer = new SourceIndexer(document, indexManager, javaProject);
            this.sourceIndexer.indexDocument();
        } else if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(documentPath)) {
            new BinaryIndexer(document).indexDocument();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#indexResolvedDocument(SearchDocument, IPath)
     */
    @Override
    public void indexResolvedDocument(SearchDocument document, IPath indexPath) {
        String documentPath = document.getPath();
        if (org.eclipse.che.jdt.internal.core.search.Util.isJavaLikeFileName(documentPath)) {
            if (this.sourceIndexer != null)
                this.sourceIndexer.indexResolvedDocument();
            this.sourceIndexer = null;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#resolveDocument(SearchDocument document)
     */
    public void resolveDocument(SearchDocument document) {
        String documentPath = document.getPath();
        if (org.eclipse.che.jdt.internal.core.search.Util.isJavaLikeFileName(documentPath)) {
            if (this.sourceIndexer != null)
                this.sourceIndexer.resolveDocument();
        }
    }

    /* (non-Javadoc)
     * @see SearchParticipant#locateMatches(SearchDocument[], SearchPattern, IJavaSearchScope, SearchRequestor, IProgressMonitor)
     */
    public void locateMatches(SearchDocument[] indexMatches, SearchPattern pattern,
                              IJavaSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {

        MatchLocator matchLocator =
                new MatchLocator(
                        pattern,
                        requestor,
                        scope,
                        monitor
                );

		/* eliminating false matches and locating them */
        if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
        matchLocator.locateMatches(indexMatches);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.search.SearchParticipant#selectIndexes(org.eclipse.jdt.core.search.SearchQuery, org.eclipse.jdt.core
     * .search.SearchContext)
     */
    public IPath[] selectIndexes(SearchPattern pattern, IJavaSearchScope scope) {
        IndexSelector selector = (IndexSelector)this.indexSelector.get();
        if (selector == null) {
            selector = new IndexSelector(scope, pattern, indexManager);
            this.indexSelector.set(selector);
        }
        IndexLocation[] urls = selector.getIndexLocations();
        IPath[] paths = new IPath[urls.length];
        for (int i = 0; i < urls.length; i++) {
            paths[i] = new Path(urls[i].getIndexFile().getPath());
        }
        return paths;
    }

    public IndexLocation[] selectIndexURLs(SearchPattern pattern, IJavaSearchScope scope) {
        IndexSelector selector = (IndexSelector)this.indexSelector.get();
        if (selector == null) {
            selector = new IndexSelector(scope, pattern, indexManager);
            this.indexSelector.set(selector);
        }
        return selector.getIndexLocations();
    }

}
