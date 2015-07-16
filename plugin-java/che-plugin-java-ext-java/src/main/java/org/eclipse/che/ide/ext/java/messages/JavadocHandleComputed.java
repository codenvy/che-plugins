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
public class JavadocHandleComputed extends MessageImpl {
    protected JavadocHandleComputed() {
    }

    public static native JavadocHandleComputed make() /*-{
        return {
            _type : 16
        }
    }-*/;

    public final native String getFqn() /*-{
        return this["fqn"];
    }-*/;

    public final native JavadocHandleComputed setFqn(String fqn) /*-{
        this["fqn"] = fqn;
        return this;
    }-*/;

    public final native String getKey() /*-{
        return this["key"];
    }-*/;

    public final native JavadocHandleComputed setKey(String key) /*-{
        this["key"] = key;
        return this;
    }-*/;

    public final native int getOffset() /*-{
        return this["offset"];
    }-*/;

    public final native JavadocHandleComputed setOffset(int offset) /*-{
        this["offset"] = offset;
        return this;
    }-*/;

    public final native boolean isSource() /*-{
        return this["source"];
    }-*/;

    public final native JavadocHandleComputed setSource(boolean source) /*-{
        this["source"] = source;
        return this;
    }-*/;

    public final native boolean isDeclaration() /*-{
        return this["declaration"];
    }-*/;

    public final native JavadocHandleComputed setDeclaration(boolean declaration) /*-{
        this["declaration"] = declaration;
        return this;
    }-*/;

    public final native java.lang.String getId() /*-{
        return this["id"];
    }-*/;

    public final native JavadocHandleComputed setId(java.lang.String id) /*-{
        this["id"] = id;
        return this;
    }-*/;

    public final native boolean hasId() /*-{
        return this.hasOwnProperty("id");
    }-*/;
}
