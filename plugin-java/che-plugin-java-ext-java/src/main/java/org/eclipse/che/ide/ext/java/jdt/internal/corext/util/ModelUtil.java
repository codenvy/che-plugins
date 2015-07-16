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
package org.eclipse.che.ide.ext.java.jdt.internal.corext.util;

import org.eclipse.che.ide.ext.java.jdt.core.Signature;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: 12:14:05 PM 34360 2009-07-22 23:58:59Z evgen $
 */
public class ModelUtil {
    public static boolean isImplicitImport(String string, CompilationUnit fCompilationUnit) {
        if ("java.lang".equals(Signature.getQualifier(string))) {
            return true;
        }
        return false;
    }
}
