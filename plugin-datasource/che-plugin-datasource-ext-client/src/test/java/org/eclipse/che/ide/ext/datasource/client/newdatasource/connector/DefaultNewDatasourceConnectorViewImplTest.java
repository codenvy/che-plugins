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
package org.eclipse.che.ide.ext.datasource.client.newdatasource.connector;

import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class DefaultNewDatasourceConnectorViewImplTest {

    @Mock
    private KeyPressEvent keyPressEvent;
    @Mock
    private KeyUpEvent    keyUpEvent;
    @Mock
    private DefaultNewDatasourceConnectorView.ActionDelegate delegate;

    @InjectMocks
    private DefaultNewDatasourceConnectorViewImpl view;

    @Before
    public void setUp() {
        view.setDelegate(delegate);
    }

    @Test
    public void notDigitSymbolsShouldBeCanceled() {
        when(keyPressEvent.getCharCode()).thenReturn('v');

        view.onPortFieldChanged(keyPressEvent);

        verify(keyPressEvent).getCharCode();
        verify(view.portField).cancelKey();
    }

    @Test
    public void digitSymbolsShouldNotBeCanceled() {
        when(keyPressEvent.getCharCode()).thenReturn('1');

        view.onPortFieldChanged(keyPressEvent);

        verify(keyPressEvent).getCharCode();
        verify(view.portField, never()).cancelKey();
    }

    @Test
    public void emptyLineShouldBeSetLikePortNumber() {
        when(view.portField.getText()).thenReturn("");

        view.onPortFieldChanged(keyUpEvent);

        verify(view.portField).getText();
        verify(delegate, never()).portChanged(anyInt());
    }

    @Test
    public void portNumberShouldBeSet() {
        when(view.portField.getText()).thenReturn("3306");

        view.onPortFieldChanged(keyUpEvent);

        verify(view.portField).getText();
        verify(delegate).portChanged(3306);
    }
}
