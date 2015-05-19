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

package org.eclipse.swt.graphics;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * @author Evgen Vidolob
 */
public class Image {
    private final String img;
    public Image(ImageDescriptor key) {
        img = key.getImage();
    }

    public String getImg() {
        return img;
    }
}
