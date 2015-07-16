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
package org.eclipse.che.ide.ext.java.worker.env.json;

import org.eclipse.che.ide.collections.Jso;

/**
 * @author Evgen Vidolob
 */
public class ElementValuePairJso extends Jso {
    protected ElementValuePairJso() {
    }

    public final native String getName() /*-{
        return this["name"];
    }-*/;

    public final native Jso getValue() /*-{
        return this["value"];
    }-*/;
}
