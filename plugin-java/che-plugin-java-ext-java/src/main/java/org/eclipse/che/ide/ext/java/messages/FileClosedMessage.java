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
public class FileClosedMessage extends MessageImpl {
    protected FileClosedMessage() {
    }

    public static native FileClosedMessage make() /*-{
        return {
            _type : 17
        }
    }-*/;

    public final native String getFilePath() /*-{
        return this["filePath"];
    }-*/;

    public final native FileClosedMessage setFilePath(String filePath) /*-{
        this["filePath"] = filePath;
        return this;
    }-*/;
}
