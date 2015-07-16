/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.ide.ext.java.jdt.internal.text.correction.proposals;

import org.eclipse.che.ide.ext.java.jdt.Images;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Block;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.FieldDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.IBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.IMethodBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ITypeBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.IVariableBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Javadoc;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.PrimitiveType;
import org.eclipse.che.ide.ext.java.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TagElement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TextElement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Type;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.che.ide.ext.java.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.codemanipulation.ASTResolving;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.dom.Bindings;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.CorrectionMessages;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.JavadocTagsSubProcessor;
import org.eclipse.che.ide.ext.java.jdt.internal.ui.BindingLabelProvider;
import org.eclipse.che.ide.ext.java.jdt.internal.ui.JavaElementLabels;
import org.eclipse.che.ide.ext.java.jdt.text.Document;
import org.eclipse.che.ide.runtime.Assert;
import org.eclipse.che.ide.runtime.CoreException;

import java.util.Arrays;
import java.util.Comparator;

public class TypeChangeCorrectionProposal extends LinkedCorrectionProposal {

    private final IBinding fBinding;

    private final CompilationUnit fAstRoot;

    private final ITypeBinding fNewType;

    private final ITypeBinding[] fTypeProposals;

    public TypeChangeCorrectionProposal(IBinding binding, CompilationUnit astRoot, ITypeBinding newType,
                                        boolean offerSuperTypeProposals, int relevance, Document document) {
        super("", null, relevance, document, Images.correction_change); //$NON-NLS-1$

        Assert.isTrue(binding != null && (binding.getKind() == IBinding.METHOD || binding.getKind() == IBinding.VARIABLE)
                      && Bindings.isDeclarationBinding(binding));

        fBinding = binding; // must be generic method or (generic) variable
        fAstRoot = astRoot;

        if (offerSuperTypeProposals) {
            fTypeProposals = ASTResolving.getRelaxingTypes(astRoot.getAST(), newType);
            sortTypes(fTypeProposals);
            fNewType = fTypeProposals[0];
        } else {
            fNewType = newType;
            fTypeProposals = null;
        }

        String typeName = BindingLabelProvider.getBindingLabel(fNewType, JavaElementLabels.ALL_DEFAULT);
        if (binding.getKind() == IBinding.VARIABLE) {
            IVariableBinding varBinding = (IVariableBinding)binding;
            if (varBinding.isField()) {
                setDisplayName(CorrectionMessages.INSTANCE.TypeChangeCompletionProposal_field_name(varBinding.getName(),
                                                                                                   typeName));
            } else if (astRoot.findDeclaringNode(binding) instanceof SingleVariableDeclaration) {
                setDisplayName(CorrectionMessages.INSTANCE.TypeChangeCompletionProposal_param_name(varBinding.getName(),
                                                                                                   typeName));
            } else {
                setDisplayName(CorrectionMessages.INSTANCE.TypeChangeCompletionProposal_variable_name(varBinding.getName(),
                                                                                                      typeName));
            }
        } else {
            String[] args = {binding.getName(), typeName};
            setDisplayName(CorrectionMessages.INSTANCE.TypeChangeCompletionProposal_method_name(binding.getName(),
                                                                                                typeName));
        }
    }

    @Override
    protected ASTRewrite getRewrite() throws CoreException {
        ASTNode boundNode = fAstRoot.findDeclaringNode(fBinding);
        ASTNode declNode = null;
        CompilationUnit newRoot = fAstRoot;
        if (boundNode != null) {
            declNode = boundNode; // is same CU
        } else {
            newRoot = ASTResolving.createQuickFixAST(document);
            declNode = newRoot.findDeclaringNode(fBinding.getKey());
        }
        if (declNode != null) {
            AST ast = declNode.getAST();
            ASTRewrite rewrite = ASTRewrite.create(ast);
            ImportRewrite imports = createImportRewrite(newRoot);

            ImportRewriteContext context =
                    new ContextSensitiveImportRewriteContext(newRoot, declNode.getStartPosition(), imports);
            Type type = imports.addImport(fNewType, ast, context);

            if (declNode instanceof MethodDeclaration) {
                MethodDeclaration methodDecl = (MethodDeclaration)declNode;
                Type origReturnType = methodDecl.getReturnType2();
                rewrite.set(methodDecl, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
                rewrite.set(methodDecl, MethodDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
                // add javadoc tag
                Javadoc javadoc = methodDecl.getJavadoc();
                if (javadoc != null && origReturnType != null && origReturnType.isPrimitiveType()
                    && ((PrimitiveType)origReturnType).getPrimitiveTypeCode() == PrimitiveType.VOID) {

                    TagElement returnTag = JavadocTagsSubProcessor.findTag(javadoc, TagElement.TAG_RETURN, null);
                    if (returnTag == null) {
                        returnTag = ast.newTagElement();
                        returnTag.setTagName(TagElement.TAG_RETURN);
                        TextElement commentStart = ast.newTextElement();
                        returnTag.fragments().add(commentStart);
                        //                  addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$

                        ListRewrite tagsRewriter = rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
                        JavadocTagsSubProcessor.insertTag(tagsRewriter, returnTag, null);
                    }
                }

            } else if (declNode instanceof AnnotationTypeMemberDeclaration) {
                AnnotationTypeMemberDeclaration methodDecl = (AnnotationTypeMemberDeclaration)declNode;
                rewrite.set(methodDecl, AnnotationTypeMemberDeclaration.TYPE_PROPERTY, type, null);
            } else if (declNode instanceof VariableDeclarationFragment) {
                ASTNode parent = declNode.getParent();
                if (parent instanceof FieldDeclaration) {
                    FieldDeclaration fieldDecl = (FieldDeclaration)parent;
                    if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
                        VariableDeclarationFragment placeholder =
                                (VariableDeclarationFragment)rewrite.createMoveTarget(declNode);
                        FieldDeclaration newField = ast.newFieldDeclaration(placeholder);
                        newField.setType(type);
                        AbstractTypeDeclaration typeDecl = (AbstractTypeDeclaration)fieldDecl.getParent();

                        ListRewrite listRewrite = rewrite.getListRewrite(typeDecl, typeDecl.getBodyDeclarationsProperty());
                        if (fieldDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
                            listRewrite.insertBefore(newField, parent, null);
                        } else {
                            listRewrite.insertAfter(newField, parent, null);
                        }
                    } else {
                        rewrite.set(fieldDecl, FieldDeclaration.TYPE_PROPERTY, type, null);
                        rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
                    }
                } else if (parent instanceof VariableDeclarationStatement) {
                    VariableDeclarationStatement varDecl = (VariableDeclarationStatement)parent;
                    if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
                        VariableDeclarationFragment placeholder =
                                (VariableDeclarationFragment)rewrite.createMoveTarget(declNode);
                        VariableDeclarationStatement newStat = ast.newVariableDeclarationStatement(placeholder);
                        newStat.setType(type);

                        ListRewrite listRewrite = rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
                        if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
                            listRewrite.insertBefore(newStat, parent, null);
                        } else {
                            listRewrite.insertAfter(newStat, parent, null);
                        }
                    } else {
                        rewrite.set(varDecl, VariableDeclarationStatement.TYPE_PROPERTY, type, null);
                        rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
                    }
                } else if (parent instanceof VariableDeclarationExpression) {
                    VariableDeclarationExpression varDecl = (VariableDeclarationExpression)parent;

                    rewrite.set(varDecl, VariableDeclarationExpression.TYPE_PROPERTY, type, null);
                    rewrite.set(declNode, VariableDeclarationFragment.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
                }
            } else if (declNode instanceof SingleVariableDeclaration) {
                SingleVariableDeclaration variableDeclaration = (SingleVariableDeclaration)declNode;
                rewrite.set(variableDeclaration, SingleVariableDeclaration.TYPE_PROPERTY, type, null);
                rewrite.set(variableDeclaration, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
            }

            // set up linked mode
            //         final String KEY_TYPE = "type"; //$NON-NLS-1$
            //         addLinkedPosition(rewrite.track(type), true, KEY_TYPE);
            //         if (fTypeProposals != null)
            //         {
            //            for (int i = 0; i < fTypeProposals.length; i++)
            //            {
            //               addLinkedPositionProposal(KEY_TYPE, fTypeProposals[i]);
            //            }
            //         }
            return rewrite;
        }
        return null;
    }

    private void sortTypes(ITypeBinding[] typeProposals) {
        ITypeBinding oldType;
        if (fBinding instanceof IMethodBinding) {
            oldType = ((IMethodBinding)fBinding).getReturnType();
        } else {
            oldType = ((IVariableBinding)fBinding).getType();
        }
        if (!oldType.isParameterizedType())
            return;

        final ITypeBinding oldTypeDeclaration = oldType.getTypeDeclaration();
        Arrays.sort(typeProposals, new Comparator<ITypeBinding>() {
            public int compare(ITypeBinding o1, ITypeBinding o2) {
                return rank(o2) - rank(o1);
            }

            private int rank(ITypeBinding type) {
                if (type.getTypeDeclaration().equals(oldTypeDeclaration))
                    return 1;
                return 0;
            }
        });
    }

}
