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
package org.eclipse.jface.resource;

/**
 * @author Evgen Vidolob
 */
public class ImageDescriptor {
    private String image;
    private static final ImageDescriptor missingImageDescriptor = new ImageDescriptor("missing");

    public ImageDescriptor(String image) {
        this.image = image;
    }

    public static ImageDescriptor getMissingImageDescriptor() {
        return missingImageDescriptor;
    }

    public String getImage() {
        return image;
    }
}
