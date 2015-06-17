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
package org.eclipse.che.ide.ext.git.server;

import org.eclipse.che.ide.ext.git.server.nativegit.CredentialsLoader;

/**
 * Allows configuring the native git with various flavors of GitConnection.
 * 
 * @author Tareq Sharafy (tareq.sharafy@sap.com)
 */
public interface ServerTestConfiguration {

	public String CLASS_NAME_PARAM = "configClass";

	GitConnectionFactory createConnectionFactory(CredentialsLoader loader);

}
