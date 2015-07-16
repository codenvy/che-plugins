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
package org.eclipse.che.ide.ext.java.jdt.internal.core;


import org.eclipse.che.ide.ext.java.jdt.core.Signature;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.CategorizedProblem;
import org.eclipse.che.ide.ext.java.jdt.core.compiler.CharOperation;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.BodyDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.DefaultBindingResolver;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ITypeBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.IVariableBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.che.ide.ext.java.jdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.Argument;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.che.ide.ext.java.jdt.internal.compiler.lookup.TypeVariableBinding;

import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.java.jdt.internal.core.SelectionResult.Type;

/**
 * Implementation of <code>ISelectionRequestor</code> to assist with
 * code resolve in a compilation unit. Translates names to elements.
 */
public class SelectionRequestor implements ISelectionRequestor {
    private CompilationUnit compilationUnit;
    private String          content;
    private SelectionResult selectionResult;
    //    /*
//     * The name lookup facility used to resolve packages
//     */
//    protected NameLookup nameLookup;

    /*
     * The compilation unit or class file we are resolving in
     */
//    protected Openable openable;

    /*
     * The collection of resolved elements.
     */
//    protected IJavaElement[] elements     = JavaElement.NO_ELEMENTS;
//    protected int            elementIndex = -1;

//    protected HandleFactory handleFactory = new HandleFactory();

    /**
     * Creates a selection requestor that uses that given
     * name lookup facility to resolve names.
     * <p/>
     * Fix for 1FVXGDK
     */
    public SelectionRequestor(CompilationUnit compilationUnit, String content) {
        super();
//        this.nameLookup = nameLookup;
//        this.openable = openable;
        this.compilationUnit = compilationUnit;
        this.content = content;
    }

    /**
     * Resolve the type.
     */
    public void acceptType(char[] packageName, char[] typeName, int modifiers, boolean isDeclaration, char[] uniqueKey, int start,
                           int end) {
        String key;
        if (uniqueKey == null) {
            key = "L" + new String(packageName).replaceAll("\\.", "/") + "/" + new String(typeName) + ";";
        } else {
            key = new String(uniqueKey);
        }
        ASTNode node = compilationUnit.findDeclaringNode(key);
        if (node == null) {
            String binaryName = new String(packageName) + ".";
            String name = new String(typeName);
            if (name.contains(".")) {
                name = name.replaceAll("\\.", "\\$");
            }
            binaryName += name;
            DefaultBindingResolver resolver = ((DefaultBindingResolver)compilationUnit.getAST().getBindingResolver());
            Map bindingsToAstNodes = resolver.bindingsToAstNodes;
            for (Object o : bindingsToAstNodes.keySet()) {
                if (o instanceof ITypeBinding) {
                    ITypeBinding binding = ((ITypeBinding)o);
                    if (binding.getBinaryName().equals(binaryName)) {
                        node = (ASTNode)bindingsToAstNodes.get(binding);
                        key = binding.getKey();
                        break;
                    }
                }
            }
        }

        int offset = -1;
        boolean source = false;
        if (node != null) {
            AbstractTypeDeclaration field = (AbstractTypeDeclaration)node;
            offset = field.getName().getStartPosition();
            source = true;
        }
        String fqn = "";
        if (packageName.length > 0) {
            fqn = new String(packageName) + ".";
        }
        fqn += new String(typeName);
        selectionResult = new SelectionResult(Type.CLASS, fqn, key, offset, isDeclaration, source);
    }

    /**
     */
    public void acceptError(CategorizedProblem error) {
        System.out.println(error);
        // do nothing
    }

    /**
     * Resolve the field.
     */
    public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] name, boolean isDeclaration, char[] uniqueKey,
                            int start, int end) {
        ASTNode node = null;
        String key = null;
        String className = new String(declaringTypeName);
        if (uniqueKey != null) {
            key = Signature.getTypeErasure(new String(uniqueKey));
            node = compilationUnit.findDeclaringNode(key);
        } else {
            String fieldName = new String(name);
            DefaultBindingResolver resolver = ((DefaultBindingResolver)compilationUnit.getAST().getBindingResolver());
            Map bindingsToAstNodes = resolver.bindingsToAstNodes;
            for (Object o : bindingsToAstNodes.keySet()) {
                if (o instanceof IVariableBinding) {
                    IVariableBinding binding = (IVariableBinding)o;
                    if (binding.isField()) {
                        if (binding.getDeclaringClass().getName().equals(className) && binding.getName().equals(fieldName)) {
                            node = (ASTNode)bindingsToAstNodes.get(o);
                            break;
                        }
                    }
                }
            }
        }
        int offset = -1;
        boolean source = false;
        if (node != null) {
            VariableDeclarationFragment field = (VariableDeclarationFragment)node;
            offset = field.getName().getStartPosition();
            source = true;
        }
        String fqn = "";
        if (declaringTypePackageName.length > 0) {
            fqn = new String(declaringTypePackageName) + ".";
        }
        fqn += className;
        selectionResult = new SelectionResult(Type.FIELD, fqn, key, offset, isDeclaration, source);
    }

    public void acceptLocalField(FieldBinding fieldBinding) {
        String key = String.valueOf(fieldBinding.computeUniqueKey());

        if (fieldBinding.declaringClass instanceof ParameterizedTypeBinding) {
            LocalTypeBinding localTypeBinding = (LocalTypeBinding)((ParameterizedTypeBinding)fieldBinding.declaringClass).genericType();
            FieldBinding field = localTypeBinding.getField(fieldBinding.name, false);
            key = String.valueOf(field.computeUniqueKey());
        }

        ASTNode node = compilationUnit.findDeclaringNode(key);

        int offset = -1;
        boolean source = false;
        if (node != null) {
            VariableDeclarationFragment field = (VariableDeclarationFragment)node;
            offset = field.getName().getStartPosition();
            source = true;
        }
        String fqn = null;
//    if(fieldBinding.length > 0){
//        fqn = new String(declaringTypePackageName) + ".";
//    }
//    fqn += new String(declaringTypeName);
        selectionResult = new SelectionResult(Type.FIELD, fqn, key, offset, true, source);
    }

    public void acceptLocalMethod(MethodBinding methodBinding) {
        String key = String.valueOf(methodBinding.computeUniqueKey());
        ASTNode node = compilationUnit.findDeclaringNode(key);
        String classSignature = new String(methodBinding.declaringClass.computeUniqueKey());
        String methodSignature = String.valueOf(methodBinding.signature());
        if (node == null) {
            node = compilationUnit.findDeclaringNode(
                    classSignature + ".(" + classSignature.substring(0, classSignature.indexOf("$")) + ";" + methodSignature.substring(1));
        }
        int offset = -1;
        boolean source = false;
        if (node != null) {
            MethodDeclaration declaration = ((MethodDeclaration)node);
            offset = declaration.getName().getStartPosition();
            source = true;

        }
        selectionResult = new SelectionResult(Type.METHOD, Signature.toString(classSignature.replace('/', '.')),
                                              methodSignature, offset, false, source);
    }

    public void acceptLocalType(TypeBinding typeBinding) {
        String key = String.valueOf(typeBinding.computeUniqueKey());
        ASTNode node = compilationUnit.findDeclaringNode(key);
        if (node == null) {
            node = compilationUnit.findDeclaringNode(String.valueOf(typeBinding.signature()));
        }
        if (node == null) {
            if (typeBinding instanceof ParameterizedTypeBinding) {
                LocalTypeBinding localTypeBinding = (LocalTypeBinding)((ParameterizedTypeBinding)typeBinding).genericType();
                node = compilationUnit.findDeclaringNode(new String(localTypeBinding.computeUniqueKey()));
            }
        }
        int offset = -1;
        boolean source = false;
        if (node != null) {
            AbstractTypeDeclaration type = (AbstractTypeDeclaration)node;
            offset = type.getName().getStartPosition();
            source = true;
        }

        String fqn = Signature.toString(new String(typeBinding.signature()).replace('/', '.'));
        selectionResult = new SelectionResult(Type.CLASS, fqn, key, offset, false, source);
    }

    private String getFqnEnclosing(ReferenceBinding binding) {
        String encl = "";
        if (binding.isLocalType()) {
            encl = getFqnEnclosing(binding.enclosingType());
        }
        String s = new String(binding.sourceName());
        if (!encl.isEmpty()) {
            s = encl + "$" + s;
        }
        return s;

    }

    public void acceptLocalTypeParameter(TypeVariableBinding typeVariableBinding) {
//	IJavaElement res;
//	if(typeVariableBinding.declaringElement instanceof ParameterizedTypeBinding) {
//		LocalTypeBinding localTypeBinding = (LocalTypeBinding)((ParameterizedTypeBinding)typeVariableBinding.declaringElement)
// .genericType();
//		res = findLocalElement(localTypeBinding.sourceStart());
//	} else {
//		SourceTypeBinding typeBinding = (SourceTypeBinding)typeVariableBinding.declaringElement;
//		res = findLocalElement(typeBinding.sourceStart());
//	}
//	if (res != null && res.getElementType() == IJavaElement.TYPE) {
//		IType type = (IType) res;
//		ITypeParameter typeParameter = type.getTypeParameter(new String(typeVariableBinding.sourceName));
//		if (typeParameter.exists()) {
//			addElement(typeParameter);
//			if(SelectionEngine.DEBUG){
//				System.out.print("SELECTION - accept type parameter("); //$NON-NLS-1$
//				System.out.print(typeParameter.toString());
//				System.out.println(")"); //$NON-NLS-1$
//			}
//		}
//	}
        System.out.println("SelectionRequestor.acceptLocalTypeParameter");
        System.out.println(typeVariableBinding);
    }

    public void acceptLocalMethodTypeParameter(TypeVariableBinding typeVariableBinding) {
//	MethodBinding methodBinding = (MethodBinding)typeVariableBinding.declaringElement;
//	IJavaElement res = findLocalElement(methodBinding.sourceStart());
//	if(res != null && res.getElementType() == IJavaElement.METHOD) {
//		IMethod method = (IMethod) res;
//
//		ITypeParameter typeParameter = method.getTypeParameter(new String(typeVariableBinding.sourceName));
//		if (typeParameter.exists()) {
//			addElement(typeParameter);
//			if(SelectionEngine.DEBUG){
//				System.out.print("SELECTION - accept type parameter("); //$NON-NLS-1$
//				System.out.print(typeParameter.toString());
//				System.out.println(")"); //$NON-NLS-1$
//			}
//		}
//	}
        System.out.println("SelectionRequestor.acceptLocalMethodTypeParameter");
        System.out.println(typeVariableBinding);
    }

    public void acceptLocalVariable(LocalVariableBinding binding) {
        LocalDeclaration declaration = binding.declaration;
        String key = String.valueOf(binding.computeUniqueKey());
        ASTNode node = compilationUnit.findDeclaringNode(key);
        int offset = -1;
        boolean source = true;
        if (node != null) {
            if (node instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration field = (SingleVariableDeclaration)node;
                offset = field.getName().getStartPosition();
            } else if (node instanceof VariableDeclarationFragment) {
                offset = ((VariableDeclarationFragment)node).getName().getStartPosition();
            }

        } else {
            String substring = content.substring(declaration.declarationSourceStart, declaration.declarationSourceEnd);
            offset = declaration.declarationSourceStart + substring.indexOf(String.valueOf(binding.name));
        }


        selectionResult = new SelectionResult(Type.VARIABLE, null, key, offset, false, source);
    }

    /**
     * Resolve the method
     */
    public void acceptMethod(
            char[] declaringTypePackageName,
            char[] declaringTypeName,
            String enclosingDeclaringTypeSignature,
            char[] selector,
            char[][] parameterPackageNames,
            char[][] parameterTypeNames,
            String[] parameterSignatures,
            char[][] typeParameterNames,
            char[][][] typeParameterBoundNames,
            boolean isConstructor,
            boolean isDeclaration,
            char[] uniqueKey,
            int start,
            int end, TypeDeclaration typeDeclaration) {


        String key = String.valueOf(uniqueKey);
        ASTNode node = compilationUnit.findDeclaringNode(key);
        if (node == null) {
            node = compilationUnit.findDeclaringNode(Signature.getTypeErasure(key));
        }
        int offset = -1;
        boolean source = false;
        if (node != null) {
            MethodDeclaration method = (MethodDeclaration)node;
            offset = method.getName().getStartPosition();
            key = method.resolveBinding().getKey();
            if(method.isConstructor()){
                key = getKeyForConstructor(key, method.resolveBinding().getName());
            }
            source = true;
        } else if (typeDeclaration != null) {
            for (AbstractMethodDeclaration method : typeDeclaration.methods) {
                if (CharOperation.equals(method.selector, selector) && getArgumentsCount(method.arguments) == parameterTypeNames.length) {
                    TypeBinding[] parameters = method.binding.parameters;
                    boolean match = true;
                    for (int i = 0; i < parameters.length; i++) {
                        TypeBinding parameter = parameters[i];
                        String simpleName = new String(parameter.sourceName());//Signature.getSimpleName(name);
                        char[] simpleParameterName = CharOperation.lastSegment(parameterTypeNames[i], '.');
                        if (!simpleName.equals(new String(simpleParameterName))) {
                            match = false;
                            break;
                        }
                    }
                    if (match && !areTypeParametersCompatible(method, typeParameterNames, typeParameterBoundNames)) {
                        match = false;
                    }
                    if (match) {

                        String typeSignature = new String(method.binding.declaringClass.signature());
                        String sig = "";
                        if (method.binding.genericSignature() != null) {

                            sig = getMethodName(method, sig);
                            sig += new String(method.binding.genericSignature());
                        } else {
                            sig = getMethodName(method, sig);
                            sig += new String(method.binding.signature());
                        }

                        String key2 = typeSignature + "." + sig;
                        ASTNode astNode = compilationUnit.findDeclaringNode(key2);

                        if (astNode != null) {
                            if (astNode instanceof AnnotationTypeMemberDeclaration) {
                                AnnotationTypeMemberDeclaration annotationTypeMemberDeclaration = (AnnotationTypeMemberDeclaration)astNode;
                                offset = annotationTypeMemberDeclaration.getName().getStartPosition();
                                key2 = annotationTypeMemberDeclaration.resolveBinding().getKey();
                            } else {
                                MethodDeclaration declaration = ((MethodDeclaration)astNode);
                                offset = declaration.getName().getStartPosition();
                                key2 = declaration.resolveBinding().getKey();
                                if(declaration.isConstructor()){
                                    key2 = getKeyForConstructor(key2, declaration.resolveBinding().getName());
                                }
                            }

                            selectionResult = new SelectionResult(Type.METHOD, Signature.toString(typeSignature.replace('/', '.')),
                                                                  key2, offset, isDeclaration, true);
                            return;
                        }
                    }
                }
            }
            if (typeDeclaration.isSecondary()) {
                acceptLocalType(typeDeclaration.binding);
                return;
            }
        } else {
            char[][] compoundName = CharOperation.splitOn('.', declaringTypeName);
            if (compoundName.length > 0) {
                AbstractTypeDeclaration type = findType(new String(compoundName[0]), compilationUnit.types());
                for (int i = 1, length = compoundName.length; i < length; i++) {
                    if (type != null) {
                        type = findType(new String(compoundName[i]), type.bodyDeclarations());
                    }
                }
                if (type != null) {
                    String methodName = new String(selector);
                    for (Object o : type.bodyDeclarations()) {
                        if (o instanceof MethodDeclaration) {
                            MethodDeclaration methodDeclaration = (MethodDeclaration)o;
                            if (methodDeclaration.getName().getFullyQualifiedName().equals(methodName) &&
                                methodDeclaration.parameters().size() == typeParameterNames.length) {
                                offset = methodDeclaration.getName().getStartPosition();

                                String key1 = methodDeclaration.resolveBinding().getKey();
                                if(methodDeclaration.isConstructor()){
                                    key1 = getKeyForConstructor(key1, methodDeclaration.resolveBinding().getName());
                                }
                                selectionResult = new SelectionResult(Type.METHOD, type.resolveBinding().getQualifiedName(),
                                                                      key1, offset, isDeclaration,
                                                                      true);
                                return;
                            }
                        }
                    }
                }

            }
        }
        String fqn = "";
        if (declaringTypePackageName.length > 0) {
            fqn = new String(declaringTypePackageName) + ".";
        }
        fqn += new String(declaringTypeName);
        if(isConstructor && !source) {
            key = getKeyForConstructor(key, new String(selector));
        }
        selectionResult = new SelectionResult(Type.METHOD, fqn,
                                              key, offset, isDeclaration, source);

    }

    private String getKeyForConstructor(String key, String name) {
        String fqn = key.substring(0, key.indexOf(';') + 1);
        String substring = key.substring(key.indexOf(';') + 1, key.length());
        substring = substring.substring(1);
        substring = "." + name + substring;
        key = fqn + substring;
        return key;
    }

    private String getMethodName(AbstractMethodDeclaration method, String sig) {
        String methodName = new String(method.binding.selector);
        if (!methodName.equals("<init>")) {
            sig = methodName;
        }
        return sig;
    }

    private int getArgumentsCount(Argument[] arguments) {
        if (arguments == null) {
            return 0;
        }
        return arguments.length;
    }

    public SelectionResult getSelectionResult() {
        return selectionResult;
    }

    /**
     * Resolve the package
     */
    public void acceptPackage(char[] packageName) {
        //we don't support package selection
    }


    public void acceptTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] typeParameterName,
                                    boolean isDeclaration, int start, int end) {

        String key = "L" + new String(declaringTypePackageName).replaceAll("\\.", "/") + "/";

        String className = new String(declaringTypeName);
        if (className.contains(".")) {
            className = className.replaceAll("\\.", "\\$");
        }
        key += className + ";:T" +
               new String(typeParameterName) + ";";
        ASTNode node = compilationUnit.findDeclaringNode(key);
        boolean source = false;
        int offset = -1;
        if (node != null) {
            source = true;
            org.eclipse.che.ide.ext.java.jdt.core.dom.TypeParameter parameter = ((org.eclipse.che.ide.ext.java.jdt.core.dom.TypeParameter)node);
            offset = parameter.getName().getStartPosition();
        }
        selectionResult = new SelectionResult(Type.TYPE_PARAMETER, new String(typeParameterName),
                                              key, offset, isDeclaration, source);
    }

    @SuppressWarnings("unchecked")
    public void acceptMethodTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, int selectorStart,
                                          int selectorEnd, char[] typeParameterName, boolean isDeclaration, int start, int end) {
        String className = new String(declaringTypeName);

        MethodDeclaration method = null;
        AbstractTypeDeclaration type = null;
        if (className.contains(".")) {
            String[] split = className.split("\\.");
            List<? extends BodyDeclaration> bodyDeclarations = compilationUnit.types();
            for (String name : split) {
                type = findType(name, bodyDeclarations);
                if (type != null) {
                    bodyDeclarations = type.bodyDeclarations();
                }
            }
        } else {
            type = findType(className, compilationUnit.types());
        }


        if (type != null) {
            List<BodyDeclaration> bodyDeclarations = type.bodyDeclarations();
            for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = (MethodDeclaration)bodyDeclaration;
                    if (methodDeclaration.getName().getIdentifier().equals(new String(selector))) {
                        method = methodDeclaration;
                        break;
                    }
                }
            }
            if (method == null) {
                //todo add type
                return;
            }
            String parameterName = new String(typeParameterName);
            List<org.eclipse.che.ide.ext.java.jdt.core.dom.TypeParameter> list = method.typeParameters();
            for (org.eclipse.che.ide.ext.java.jdt.core.dom.TypeParameter parameter : list) {
                if (parameter.getName().getFullyQualifiedName().equals(parameterName)) {
                    selectionResult =
                            new SelectionResult(Type.METHOD_TYPE_PARAMETER, parameterName, null, parameter.getName().getStartPosition(),
                                                false, true);
                    break;
                }
            }

        }

    }

    private AbstractTypeDeclaration findType(String name, List<? extends BodyDeclaration> declarations) {
        for (BodyDeclaration bodyDeclaration : declarations) {
            if (bodyDeclaration instanceof AbstractTypeDeclaration) {
                if (((AbstractTypeDeclaration)bodyDeclaration).getName().getFullyQualifiedName().equals(name)) {
                    return ((AbstractTypeDeclaration)bodyDeclaration);
                }
            }
        }
        return null;
    }

    private boolean areTypeParametersCompatible(AbstractMethodDeclaration method, char[][] typeParameterNames,
                                                char[][][] typeParameterBoundNames) {
        TypeParameter[] typeParameters = method.typeParameters();
        int length1 = typeParameters == null ? 0 : typeParameters.length;
        int length2 = typeParameterNames == null ? 0 : typeParameterNames.length;
        if (length1 != length2) {
            return false;
        } else {
            for (int j = 0; j < length1; j++) {
                TypeParameter typeParameter = typeParameters[j];
                String typeParameterName = String.valueOf(typeParameter.name);
                if (!typeParameterName.equals(new String(typeParameterNames[j]))) {
                    return false;
                }

                TypeReference[] bounds = typeParameter.bounds == null ? new TypeReference[0] : typeParameter.bounds;
                int boundCount = typeParameterBoundNames[j] == null ? 0 : typeParameterBoundNames[j].length;

                if (bounds.length != boundCount) {
                    return false;
                } else {
                    for (int k = 0; k < boundCount; k++) {
                        String simpleName = Signature.getSimpleName(bounds[k].toString());
                        int index = simpleName.indexOf('<');
                        if (index != -1) {
                            simpleName = simpleName.substring(0, index);
                        }
                        if (!simpleName.equals(new String(typeParameterBoundNames[j][k]))) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
