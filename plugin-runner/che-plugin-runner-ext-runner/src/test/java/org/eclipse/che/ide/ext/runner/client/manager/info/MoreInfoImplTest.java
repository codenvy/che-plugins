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
package org.eclipse.che.ide.ext.runner.client.manager.info;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.PortMapping;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.manager.info.MoreInfoImpl.PORT_STUB;
import static org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerPresenter.TIMER_STUB;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class MoreInfoImplTest {
    private static final String SOME_TEXT = "some text";

    @Mock
    private Runner       runner;
    @InjectMocks
    private MoreInfoImpl widget;

    @Test
    public void contentShouldBeUpdatedWhenRunnerIsExisted() throws Exception {
        when(runner.getCreationTime()).thenReturn(SOME_TEXT);
        when(runner.getStopTime()).thenReturn(SOME_TEXT);
        when(runner.getTimeout()).thenReturn(SOME_TEXT);
        when(runner.getActiveTime()).thenReturn(SOME_TEXT);
        when(runner.getRAM()).thenReturn(128);

        widget.update(runner);

        verify(widget.started).setText(SOME_TEXT);
        verify(widget.finished).setText(SOME_TEXT);
        verify(widget.timeout).setText(SOME_TEXT);
        verify(widget.activeTime).setText(SOME_TEXT);
        verify(widget.ram).setText("128MB");
    }

    @Test
    public void contentShouldBeUpdatedWhenRunnerIsNull() throws Exception {
        widget.update(null);

        verify(widget.started).setText(TIMER_STUB);
        verify(widget.finished).setText(TIMER_STUB);
        verify(widget.timeout).setText(TIMER_STUB);
        verify(widget.activeTime).setText(TIMER_STUB);
        verify(widget.ram).setText("0MB");
    }

    @Test
    public void portsShouldBeClear() throws Exception {
        reset(widget.firstPort);
        reset(widget.secondPort);
        reset(widget.thirdPort);

        widget.update(null);

        verify(widget.firstPort).setText(eq(PORT_STUB));
        verify(widget.secondPort).setText(eq(PORT_STUB));
        verify(widget.thirdPort).setText(eq(PORT_STUB));
    }

    @Test
    public void portsShouldBeUpdated() throws Exception {
        String port = "7777";
        String raw = port + PORT_STUB + port;
        Map<String, String> ports = new HashMap<>();
        ports.put(port, port);
        ApplicationProcessDescriptor runnerDescriptor = mock(ApplicationProcessDescriptor.class);
        PortMapping portMapping = mock(PortMapping.class);
        when(runnerDescriptor.getPortMapping()).thenReturn(portMapping);
        when(runner.getDescriptor()).thenReturn(runnerDescriptor);
        when(portMapping.getPorts()).thenReturn(ports);
        reset(widget.firstPort);
        reset(widget.secondPort);
        reset(widget.thirdPort);

        widget.update(runner);

        verify(widget.firstPort).setText(eq(PORT_STUB));
        verify(widget.secondPort).setText(eq(PORT_STUB));
        verify(widget.thirdPort).setText(eq(PORT_STUB));
        verify(widget.firstPort).setText(eq(raw));
    }

}