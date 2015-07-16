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
package org.eclipse.che.ide.ext.java.messages;

import com.google.gwt.webworker.client.messages.MessageImpl;

/**
 * @author Evgen Vidolob
 */
public class ComputeJavadocHandle  extends MessageImpl{
    protected ComputeJavadocHandle() {
    }

    public static native ComputeJavadocHandle make() /*-{
        return {
            _type : 15
        }
    }-*/;

    public final native int getOffset() /*-{
        return this["offset"];
    }-*/;

    public final native ComputeJavadocHandle setOffset(int offset) /*-{
        this["offset"] = offset;
        return this;
    }-*/;

    public final native String getFilePath() /*-{
        return this["filePath"];
    }-*/;

    public final native ComputeJavadocHandle setFilePath(String filePath) /*-{
        this["filePath"] = filePath;
        return this;
    }-*/;

    public final native java.lang.String id() /*-{
        return this["id"];
    }-*/;

    public final native ComputeJavadocHandle setId(java.lang.String id) /*-{
        this["id"] = id;
        return this;
    }-*/;

    public final native boolean hasId() /*-{
        return this.hasOwnProperty("id");
    }-*/;

}
