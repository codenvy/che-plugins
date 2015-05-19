package org.eclipse.jface.viewers;

/**
 * @author Evgen Vidolob
 */
public abstract class StyledCellLabelProvider {

    /**
     * Applies decoration styles to the decorated string and adds the styles of the previously
     * undecorated string.
     * <p>
     * If the <code>decoratedString</code> contains the <code>styledString</code>, then the result
     * keeps the styles of the <code>styledString</code> and styles the decorations with the
     * <code>decorationStyler</code>. Otherwise, the decorated string is returned without any
     * styles.
     *
     * @param decoratedString the decorated string
     * @param decorationStyler the styler to use for the decoration or <code>null</code> for no
     *            styles
     * @param styledString the original styled string
     *
     * @return the styled decorated string (can be the given <code>styledString</code>)
     * @since 3.5
     */
    public static StyledString styleDecoratedString(String decoratedString, StyledString.Styler decorationStyler, StyledString styledString) {
        String label= styledString.getString();
        int originalStart= decoratedString.indexOf(label);
        if (originalStart == -1) {
            return new StyledString(decoratedString); // the decorator did something wild
        }

        if (decoratedString.length() == label.length())
            return styledString;

        if (originalStart > 0) {
            StyledString newString= new StyledString(decoratedString.substring(0, originalStart), decorationStyler);
            newString.append(styledString);
            styledString= newString;
        }
        if (decoratedString.length() > originalStart + label.length()) { // decorator appended something
            return styledString.append(decoratedString.substring(originalStart + label.length()), decorationStyler);
        }
        return styledString; // no change
    }
}
