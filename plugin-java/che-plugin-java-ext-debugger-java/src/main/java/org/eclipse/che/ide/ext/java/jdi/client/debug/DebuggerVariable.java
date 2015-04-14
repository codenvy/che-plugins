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
package org.eclipse.che.ide.ext.java.jdi.client.debug;

import org.eclipse.che.ide.ext.java.jdi.shared.Variable;
import org.eclipse.che.ide.ext.java.jdi.shared.VariablePath;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The class is wrapper over Variable dto object. This class uses as key in hash map in data adapter and we need override hashcode method.
 *
 * @author Dmitry Shnurenko
 */
public class DebuggerVariable {

    private final Variable variable;

    public DebuggerVariable(@Nonnull Variable variable) {
        this.variable = variable;
    }

    @Nonnull
    public Variable getVariable() {
        return variable;
    }

    @Nonnull
    public String getName() {
        return variable.getName();
    }

    public void setName(@Nonnull String name) {
        variable.setName(name);
    }

    @Nonnull
    public String getValue() {
        return variable.getValue();
    }

    public void setValue(@Nonnull String value) {
        variable.setValue(value);
    }

    @Nonnull
    public String getType() {
        return variable.getType();
    }

    public void setType(@Nonnull String type) {
        variable.setType(type);
    }

    @Nonnull
    public VariablePath getVariablePath() {
        return variable.getVariablePath();
    }

    public boolean isPrimitive() {
        return variable.isPrimitive();
    }

    @Nonnull
    public List<DebuggerVariable> getVariables() {
        List<DebuggerVariable> variables = new ArrayList<>();

        for (Variable innerVariable : variable.getVariables()) {
            variables.add(new DebuggerVariable(innerVariable));
        }

        return variables;
    }

    public void setVariables(@Nonnull List<DebuggerVariable> debuggerVariables) {
        List<Variable> variables = new ArrayList<>();

        for (DebuggerVariable debuggerVariable : debuggerVariables) {
            variables.add(debuggerVariable.getVariable());
        }

        variable.setVariables(variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable.getName(), variable.getValue(), variable.getVariablePath());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null || object.getClass() != this.getClass()) {
            return false;
        }

        DebuggerVariable var = (DebuggerVariable)object;

        return this.getName().equals(var.getName())
               && this.getValue().equals(var.getValue())
               && this.getVariablePath().equals(var.getVariablePath());
    }
}
