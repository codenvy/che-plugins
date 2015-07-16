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
package org.eclipse.che.ide.ext.java.client.settings.compiler;

/**
 * The interface contains constants' ids to setup compiler.
 *
 * @author Dmitry Shnurenko
 */
public interface CompilerOptions {

    String COMPILER_UNUSED_LOCAL = "org.eclipse.jdt.core.compiler.problem.unusedLocal";

    String COMPILER_UNUSED_IMPORT = "org.eclipse.jdt.core.compiler.problem.unusedImport";

    String DEAD_CODE = "org.eclipse.jdt.core.compiler.problem.deadCode";

    String METHOD_WITH_CONSTRUCTOR_NAME = "org.eclipse.jdt.core.compiler.problem.methodWithConstructorName";

    String UNNECESSARY_ELSE_STATEMENT = "org.eclipse.jdt.core.compiler.problem.unnecessaryElse";

    String COMPARING_IDENTICAL_VALUES = "org.eclipse.jdt.core.compiler.problem.comparingIdentical";

    String NO_EFFECT_ASSIGNMENT = "org.eclipse.jdt.core.compiler.problem.noEffectAssignment";

    String MISSING_SERIAL_VERSION_UID = "org.eclipse.jdt.core.compiler.problem.missingSerialVersion";

    String TYPE_PARAMETER_HIDE_ANOTHER_TYPE = "org.eclipse.jdt.core.compiler.problem.typeParameterHiding";

    String FIELD_HIDES_ANOTHER_VARIABLE = "org.eclipse.jdt.core.compiler.problem.fieldHiding";

    String MISSING_DEFAULT_CASE = "org.eclipse.jdt.core.compiler.problem.missingDefaultCase";

    String UNUSED_PRIVATE_MEMBER = "org.eclipse.jdt.core.compiler.problem.unusedPrivateMember";

    String UNCHECKED_TYPE_OPERATION = "org.eclipse.jdt.core.compiler.problem.uncheckedTypeOperation";

    String USAGE_OF_RAW_TYPE = "org.eclipse.jdt.core.compiler.problem.rawTypeReference";

    String MISSING_OVERRIDE_ANNOTATION = "org.eclipse.jdt.core.compiler.problem.missingOverrideAnnotation";

    String NULL_POINTER_ACCESS = "org.eclipse.jdt.core.compiler.problem.nullReference";

    String POTENTIAL_NULL_POINTER_ACCESS = "org.eclipse.jdt.core.compiler.problem.potentialNullReference";

    String REDUNDANT_NULL_CHECK = "org.eclipse.jdt.core.compiler.problem.redundantNullCheck";
}
