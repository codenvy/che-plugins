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
package org.eclipse.che.ide.ext.java.jdt.internal.text.correction;

import org.eclipse.che.ide.ext.java.jdt.Images;
import org.eclipse.che.ide.ext.java.jdt.codeassistant.api.IProblemLocation;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AST;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTNode;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ASTVisitor;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Block;
import org.eclipse.che.ide.ext.java.jdt.core.dom.BodyDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.CompilationUnit;
import org.eclipse.che.ide.ext.java.jdt.core.dom.EnumDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Expression;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ITypeBinding;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Javadoc;
import org.eclipse.che.ide.ext.java.jdt.core.dom.MethodDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.PrimitiveType;
import org.eclipse.che.ide.ext.java.jdt.core.dom.ReturnStatement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TagElement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TextElement;
import org.eclipse.che.ide.ext.java.jdt.core.dom.Type;
import org.eclipse.che.ide.ext.java.jdt.core.dom.TypeDeclaration;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.che.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.codemanipulation.ASTResolving;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.che.ide.ext.java.jdt.internal.corext.dom.Bindings;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.proposals.ASTRewriteCorrectionProposal;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.proposals.MissingReturnTypeCorrectionProposal;
import org.eclipse.che.ide.ext.java.jdt.internal.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.che.ide.ext.java.jdt.internal.ui.BindingLabelProvider;
import org.eclipse.che.ide.ext.java.jdt.quickassist.api.InvocationContext;

import java.util.ArrayList;
import java.util.Collection;

public class ReturnTypeSubProcessor {

    private static class ReturnStatementCollector extends ASTVisitor {
        private ArrayList<ReturnStatement> fResult = new ArrayList<ReturnStatement>();

        public ITypeBinding getTypeBinding(AST ast) {
            boolean couldBeObject = false;
            for (int i = 0; i < fResult.size(); i++) {
                ReturnStatement node = fResult.get(i);
                Expression expr = node.getExpression();
                if (expr != null) {
                    ITypeBinding binding = Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
                    if (binding != null) {
                        return binding;
                    } else {
                        couldBeObject = true;
                    }
                } else {
                    return ast.resolveWellKnownType("void"); //$NON-NLS-1$
                }
            }
            if (couldBeObject) {
                return ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
            }
            return ast.resolveWellKnownType("void"); //$NON-NLS-1$
        }

        @Override
        public boolean visit(ReturnStatement node) {
            fResult.add(node);
            return false;
        }

        @Override
        public boolean visit(AnonymousClassDeclaration node) {
            return false;
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            return false;
        }

        @Override
        public boolean visit(EnumDeclaration node) {
            return false;
        }

        @Override
        public boolean visit(AnnotationTypeDeclaration node) {
            return false;
        }

    }

    public static void addMethodWithConstrNameProposals(InvocationContext context, IProblemLocation problem,
                                                        Collection<ICommandAccess> proposals) {
        //		ICompilationUnit cu= context.getCompilationUnit();

        ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
        if (selectedNode instanceof MethodDeclaration) {
            MethodDeclaration declaration = (MethodDeclaration)selectedNode;

            ASTRewrite rewrite = ASTRewrite.create(declaration.getAST());
            rewrite.set(declaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);

            String label = CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_constrnamemethod_description();
            Images image = Images.correction_change;
            ASTRewriteCorrectionProposal proposal =
                    new ASTRewriteCorrectionProposal(label, rewrite, 5, context.getDocument(), image);
            proposals.add(proposal);
        }

    }

    public static void addVoidMethodReturnsProposals(InvocationContext context, IProblemLocation problem,
                                                     Collection<ICommandAccess> proposals) {

        CompilationUnit astRoot = context.getASTRoot();
        ASTNode selectedNode = problem.getCoveringNode(astRoot);
        if (selectedNode == null) {
            return;
        }

        BodyDeclaration decl = ASTResolving.findParentBodyDeclaration(selectedNode);
        if (decl instanceof MethodDeclaration && selectedNode.getNodeType() == ASTNode.RETURN_STATEMENT) {
            ReturnStatement returnStatement = (ReturnStatement)selectedNode;
            Expression expr = returnStatement.getExpression();
            if (expr != null) {
                AST ast = astRoot.getAST();

                ITypeBinding binding = Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
                if (binding == null) {
                    binding = ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
                }
                if (binding.isWildcardType()) {
                    binding = ASTResolving.normalizeWildcardType(binding, true, ast);
                }

                MethodDeclaration methodDeclaration = (MethodDeclaration)decl;

                ASTRewrite rewrite = ASTRewrite.create(ast);

                String label =
                        CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_voidmethodreturns_description(BindingLabelProvider
                                                                                                                 .getBindingLabel(binding,
                                                                                                                                  BindingLabelProvider.DEFAULT_TEXTFLAGS));
                Images image = Images.correction_change;
                LinkedCorrectionProposal proposal =
                        new LinkedCorrectionProposal(label, rewrite, 6, context.getDocument(), image);
                ImportRewrite imports = proposal.createImportRewrite(astRoot);
                ImportRewriteContext importRewriteContext =
                        new ContextSensitiveImportRewriteContext(methodDeclaration, imports);
                Type newReturnType = imports.addImport(binding, ast, importRewriteContext);

                if (methodDeclaration.isConstructor()) {
                    rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
                    rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
                } else {
                    rewrite.replace(methodDeclaration.getReturnType2(), newReturnType, null);
                }
                String key = "return_type"; //$NON-NLS-1$
                //            proposal.addLinkedPosition(rewrite.track(newReturnType), true, key);
                ITypeBinding[] bindings = ASTResolving.getRelaxingTypes(ast, binding);
                //            for (int i = 0; i < bindings.length; i++)
                //            {
                //               proposal.addLinkedPositionProposal(key, bindings[i]);
                //            }

                Javadoc javadoc = methodDeclaration.getJavadoc();
                if (javadoc != null) {
                    TagElement newTag = ast.newTagElement();
                    newTag.setTagName(TagElement.TAG_RETURN);
                    TextElement commentStart = ast.newTextElement();
                    newTag.fragments().add(commentStart);

                    JavadocTagsSubProcessor.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
                    //               proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$

                }
                proposals.add(proposal);
            }
            ASTRewrite rewrite = ASTRewrite.create(decl.getAST());
            rewrite.remove(returnStatement.getExpression(), null);

            String label = CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_removereturn_description();
            Images image = Images.correction_change;
            ASTRewriteCorrectionProposal proposal =
                    new ASTRewriteCorrectionProposal(label, rewrite, 5, context.getDocument(), image);
            proposals.add(proposal);
        }
    }

    public static void addMissingReturnTypeProposals(InvocationContext context, IProblemLocation problem,
                                                     Collection<ICommandAccess> proposals) {
        CompilationUnit astRoot = context.getASTRoot();
        ASTNode selectedNode = problem.getCoveringNode(astRoot);
        if (selectedNode == null) {
            return;
        }
        BodyDeclaration decl = ASTResolving.findParentBodyDeclaration(selectedNode);
        if (decl instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration)decl;

            ReturnStatementCollector eval = new ReturnStatementCollector();
            decl.accept(eval);

            AST ast = astRoot.getAST();

            ITypeBinding typeBinding = eval.getTypeBinding(decl.getAST());
            typeBinding = Bindings.normalizeTypeBinding(typeBinding);
            if (typeBinding == null) {
                typeBinding = ast.resolveWellKnownType("void"); //$NON-NLS-1$
            }
            if (typeBinding.isWildcardType()) {
                typeBinding = ASTResolving.normalizeWildcardType(typeBinding, true, ast);
            }

            ASTRewrite rewrite = ASTRewrite.create(ast);

            String label =
                    CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_missingreturntype_description(BindingLabelProvider
                                                                                                             .getBindingLabel(typeBinding,
                                                                                                                              BindingLabelProvider.DEFAULT_TEXTFLAGS));
            Images image = Images.correction_change;
            LinkedCorrectionProposal proposal =
                    new LinkedCorrectionProposal(label, rewrite, 6, context.getDocument(), image);

            ImportRewrite imports = proposal.createImportRewrite(astRoot);
            ImportRewriteContext importRewriteContext = new ContextSensitiveImportRewriteContext(decl, imports);
            Type type = imports.addImport(typeBinding, ast, importRewriteContext);

            rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
            rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);

            Javadoc javadoc = methodDeclaration.getJavadoc();
            if (javadoc != null && typeBinding != null) {
                TagElement newTag = ast.newTagElement();
                newTag.setTagName(TagElement.TAG_RETURN);
                TextElement commentStart = ast.newTextElement();
                newTag.fragments().add(commentStart);

                JavadocTagsSubProcessor.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
                //            proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$
            }

            String key = "return_type"; //$NON-NLS-1$
            //         proposal.addLinkedPosition(rewrite.track(type), true, key);
            //         if (typeBinding != null)
            //         {
            //            ITypeBinding[] bindings = ASTResolving.getRelaxingTypes(ast, typeBinding);
            //            for (int i = 0; i < bindings.length; i++)
            //            {
            //               proposal.addLinkedPositionProposal(key, bindings[i]);
            //            }
            //         }

            proposals.add(proposal);

            // change to constructor
            ASTNode parentType = ASTResolving.findParentType(decl);
            if (parentType instanceof AbstractTypeDeclaration) {
                boolean isInterface = parentType instanceof TypeDeclaration && ((TypeDeclaration)parentType).isInterface();
                if (!isInterface) {
                    String constructorName = ((AbstractTypeDeclaration)parentType).getName().getIdentifier();
                    ASTNode nameNode = methodDeclaration.getName();
                    label =
                            CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_wrongconstructorname_description(constructorName);
                    proposals.add(new ReplaceCorrectionProposal(label, nameNode.getStartPosition(), nameNode.getLength(),
                                                                constructorName, 5, context.getDocument()));
                }
            }
        }
    }

    public static void addMissingReturnStatementProposals(InvocationContext context, IProblemLocation problem,
                                                          Collection<ICommandAccess> proposals) {
        ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
        if (selectedNode == null) {
            return;
        }
        BodyDeclaration decl = ASTResolving.findParentBodyDeclaration(selectedNode);
        if (decl instanceof MethodDeclaration) {
            MethodDeclaration methodDecl = (MethodDeclaration)decl;
            Block block = methodDecl.getBody();
            if (block == null) {
                return;
            }
            ReturnStatement existingStatement =
                    (selectedNode instanceof ReturnStatement) ? (ReturnStatement)selectedNode : null;
            proposals
                    .add(new MissingReturnTypeCorrectionProposal(methodDecl, existingStatement, 6, context.getDocument()));

            Type returnType = methodDecl.getReturnType2();
            if (returnType != null && !"void".equals(ASTNodes.asString(returnType))) { //$NON-NLS-1$
                AST ast = methodDecl.getAST();
                ASTRewrite rewrite = ASTRewrite.create(ast);
                rewrite.replace(returnType, ast.newPrimitiveType(PrimitiveType.VOID), null);
                Javadoc javadoc = methodDecl.getJavadoc();
                if (javadoc != null) {
                    TagElement tagElement = JavadocTagsSubProcessor.findTag(javadoc, TagElement.TAG_RETURN, null);
                    if (tagElement != null) {
                        rewrite.remove(tagElement, null);
                    }
                }

                String label = CorrectionMessages.INSTANCE.ReturnTypeSubProcessor_changetovoid_description();
                Images image = Images.correction_change;
                ASTRewriteCorrectionProposal proposal =
                        new ASTRewriteCorrectionProposal(label, rewrite, 5, context.getDocument(), image);
                proposals.add(proposal);
            }
        }
    }

    public static void addMethodRetunsVoidProposals(InvocationContext context, IProblemLocation problem,
                                                    Collection<ICommandAccess> proposals) {
        CompilationUnit astRoot = context.getASTRoot();
        ASTNode selectedNode = problem.getCoveringNode(astRoot);
        if (!(selectedNode instanceof ReturnStatement)) {
            return;
        }
        ReturnStatement returnStatement = (ReturnStatement)selectedNode;
        Expression expression = returnStatement.getExpression();
        if (expression == null) {
            return;
        }
        BodyDeclaration decl = ASTResolving.findParentBodyDeclaration(selectedNode);
        if (decl instanceof MethodDeclaration) {
            MethodDeclaration methDecl = (MethodDeclaration)decl;
            Type retType = methDecl.getReturnType2();
            if (retType == null || retType.resolveBinding() == null) {
                return;
            }
            TypeMismatchSubProcessor.addChangeSenderTypeProposals(context, expression, retType.resolveBinding(), false, 4,
                                                                  proposals);
        }
    }
}
