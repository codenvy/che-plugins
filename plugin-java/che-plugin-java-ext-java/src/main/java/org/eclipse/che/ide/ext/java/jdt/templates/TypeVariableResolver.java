/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.templates;

import org.eclipse.che.ide.ext.java.jdt.core.Signature;
import org.eclipse.che.ide.ext.java.jdt.templates.CompilationUnitCompletion.Variable;
import org.eclipse.che.ide.ext.java.jdt.templates.api.TemplateContext;
import org.eclipse.che.ide.ext.java.jdt.templates.api.TemplateVariable;
import org.eclipse.che.ide.ext.java.jdt.templates.api.TemplateVariableResolver;

import java.util.List;

/**
 * Resolves to the lower bound of a type argument of another template variable.
 *
 * @since 3.3
 */
public class TypeVariableResolver extends TemplateVariableResolver {

    public TypeVariableResolver() {
    }

    /*
     * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable,
     * org.eclipse.jface.text.templates.TemplateContext)
     * @since 3.3
     */
    @Override
    public void resolve(TemplateVariable variable, TemplateContext context) {
        if (!(variable instanceof MultiVariable)) {
            super.resolve(variable, context);
            return;
        }
        MultiVariable mv = (MultiVariable)variable;
        List<String> params = variable.getVariableType().getParams();
        if (params.isEmpty()) {
            super.resolve(variable, context);
            return;
        }

        JavaContext jc = (JavaContext)context;
        String reference = params.get(0);
        int index = 0;
        if (params.size() > 1) {
            String indexParam = params.get(1);
            try {
                index = Integer.parseInt(indexParam);
            } catch (NumberFormatException x) {
            }
        }
        TemplateVariable refVar = jc.getTemplateVariable(reference);
        if (refVar instanceof JavaVariable) {
            JavaVariable jvar = (JavaVariable)refVar;
            resolve(mv, jvar, index, jc);

            return;
        }

        super.resolve(variable, context);
    }

    private void resolve(MultiVariable mv, JavaVariable master, int index, JavaContext context) {
        Object[] choices = master.getChoices();
        if (choices instanceof Variable[]) {
            context.addDependency(master, mv);
            Variable[] variables = (Variable[])choices;
            String type = master.getParamType();
            for (int i = 0; i < choices.length; i++) {
                String[] bounds = variables[i].getTypeArgumentBoundSignatures(type, index);
                for (int j = 0; j < bounds.length; j++)
                    bounds[j] = Signature.getSignatureSimpleName(bounds[j]);
                mv.setChoices(variables[i], bounds);
            }
            mv.setKey(master.getCurrentChoice());
        } else {
            super.resolve(mv, context);
            return;
        }
    }

}
