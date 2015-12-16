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
package org.eclipse.che.ide.ext.gwt.client.wizard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.eclipse.che.ide.ext.gwt.shared.Constants.GWT_PROJECT_TYPE_ID;
import static org.eclipse.che.ide.ext.java.shared.Constants.JAVA_CATEGORY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/** @author Artem Zatsarynnyi */
@RunWith(MockitoJUnitRunner.class)
public class GwtProjectWizardRegistrarTest {

    @InjectMocks
    private GwtProjectWizardRegistrar wizardRegistrar;

    @Test
    public void shouldReturnCorrectProjectTypeId() throws Exception {
        assertThat(wizardRegistrar.getProjectTypeId(), equalTo(GWT_PROJECT_TYPE_ID));
    }

    @Test
    public void shouldReturnCorrectCategory() throws Exception {
        assertThat(wizardRegistrar.getCategory(), equalTo(JAVA_CATEGORY));
    }

    @Test
    public void shouldReturnPages() throws Exception {
        assertFalse(wizardRegistrar.getWizardPages().isEmpty());
    }
}
