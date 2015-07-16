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

import org.eclipse.che.ide.collections.Array;
import com.google.gwt.webworker.client.messages.Message;
import com.google.gwt.webworker.client.messages.SerializationIndex;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
public interface Problem extends Message {

    @SerializationIndex(1)
    String originatingFileName();

    @SerializationIndex(2)
    String message();

    @SerializationIndex(3)
    int id();

    @SerializationIndex(4)
    Array<String> stringArguments();

    @SerializationIndex(5)
    int severity();

    @SerializationIndex(6)
    int startPosition();

    @SerializationIndex(7)
    int endPosition();

    @SerializationIndex(8)
    int line();

    @SerializationIndex(9)
    int column();
}
