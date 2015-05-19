package org.eclipse.che.jdt.dom;

import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

/**
 * Internal helper methods that deal with {@link org.eclipse.jdt.core.dom.ASTNode}s:
 *
 * @author Evgen Vidolob
 */
public class ASTNodes {
    public static String getSimpleNameIdentifier(Name name) {
        if (name.isQualifiedName()) {
            return ((QualifiedName)name).getName().getIdentifier();
        } else {
            return ((SimpleName)name).getIdentifier();
        }
    }

    public static String asString(ASTNode node) {
        ASTFlattener flattener = new ASTFlattener();
        node.accept(flattener);
        return flattener.getResult();
    }

    /**
     * Escapes a string value to a literal that can be used in Java source.
     *
     * @param stringValue
     *         the string value
     * @return the escaped string
     * @see org.eclipse.jdt.core.dom.StringLiteral#getEscapedValue()
     */
    public static String getEscapedStringLiteral(String stringValue) {
        StringLiteral stringLiteral = AST.newAST(ASTProvider.SHARED_AST_LEVEL).newStringLiteral();
        stringLiteral.setLiteralValue(stringValue);
        return stringLiteral.getEscapedValue();
    }

    /**
     * Escapes a character value to a literal that can be used in Java source.
     *
     * @param ch
     *         the character value
     * @return the escaped string
     * @see org.eclipse.jdt.core.dom.CharacterLiteral#getEscapedValue()
     */
    public static String getEscapedCharacterLiteral(char ch) {
        CharacterLiteral characterLiteral = AST.newAST(ASTProvider.SHARED_AST_LEVEL).newCharacterLiteral();
        characterLiteral.setCharValue(ch);
        return characterLiteral.getEscapedValue();
    }

    /**
     * Adds flags to the given node and all its descendants.
     *
     * @param root
     *         The root node
     * @param flags
     *         The flags to set
     */
    public static void setFlagsToAST(ASTNode root, final int flags) {
        root.accept(new GenericVisitor(true) {
            @Override
            protected boolean visitNode(ASTNode node) {
                node.setFlags(node.getFlags() | flags);
                return true;
            }
        });
    }

    /**
     * For {@link Name} or {@link org.eclipse.jdt.core.dom.Type} nodes, returns the topmost {@link org.eclipse.jdt.core.dom.Type} node
     * that shares the same type binding as the given node.
     *
     * @param node
     *         an ASTNode
     * @return the normalized {@link org.eclipse.jdt.core.dom.Type} node or the original node
     */
    public static ASTNode getNormalizedNode(ASTNode node) {
        ASTNode current = node;
        // normalize name
        if (QualifiedName.NAME_PROPERTY.equals(current.getLocationInParent())) {
            current = current.getParent();
        }
        // normalize type
        if (QualifiedType.NAME_PROPERTY.equals(current.getLocationInParent())
            || SimpleType.NAME_PROPERTY.equals(current.getLocationInParent())
            || NameQualifiedType.NAME_PROPERTY.equals(current.getLocationInParent())) {
            current = current.getParent();
        }
        // normalize parameterized types
        if (ParameterizedType.TYPE_PROPERTY.equals(current.getLocationInParent())) {
            current = current.getParent();
        }
        return current;
    }

}
