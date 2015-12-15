package org.eclipse.che.ide.ext.git.client.compare.changedList;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.AbstractTreeNode;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * @author Igor Vinokur
 */
public class ChangedNode extends AbstractTreeNode implements HasPresentation, HasAction {

    private String name;

    private NodePresentation nodePresentation;

    public ChangedNode(String name) {
        this.name = name;
    }

    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return Promises.resolve(Collections.<Node>emptyList());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        presentation.setPresentableText(name);

        if (name.startsWith("M")) {
            presentation.setPresentableTextCss("color: #69E;");
        } else if (name.startsWith("D")) {
            presentation.setPresentableTextCss("color: red;");
        } else if (name.startsWith("A")) {
            presentation.setPresentableTextCss("color: green;");
        } else if (name.startsWith("C")) {
            presentation.setPresentableTextCss("color: purple;");
        }
    }

    @Override
    public NodePresentation getPresentation(boolean update) {
        if (nodePresentation == null) {
            nodePresentation = new NodePresentation();
            updatePresentation(nodePresentation);
        }

        if (update) {
            updatePresentation(nodePresentation);
        }
        return nodePresentation;
    }

    @Override
    public void actionPerformed() {

    }
}
