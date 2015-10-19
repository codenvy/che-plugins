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
package org.eclipse.che.ide.extension.machine.client.inject.annotations;

import com.google.inject.BindingAnnotation;

import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation was added for define {@link Machine} which was created though {@link MachineStateDescriptor}.
 * For our machine model, machine is created if it's {@link MachineStatus} is: RUNNING or DESTROYING.
 * If machine has {@link MachineStatus} CREATING, that's means machine is not created, this is only blank for machine.
 *
 * @author Alexander Andrienko
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@BindingAnnotation
public @interface MachineBlank {
}
