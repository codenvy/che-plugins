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

import com.google.gwt.webworker.client.messages.Message;
import com.google.gwt.webworker.client.messages.SerializationIndex;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
public interface Change extends Message {

    @SerializationIndex(1)
    int offset();

    @SerializationIndex(2)
    int length();

    @SerializationIndex(3)
    String text();
}
