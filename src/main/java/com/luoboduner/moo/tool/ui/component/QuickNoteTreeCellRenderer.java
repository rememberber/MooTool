package com.luoboduner.moo.tool.ui.component;

import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.ui.form.MainWindow;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * 随手记文件夹树渲染器。
 */
public class QuickNoteTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if (!(value instanceof DefaultMutableTreeNode treeNode)) {
            return component;
        }
        Object userObject = treeNode.getUserObject();
        if (userObject instanceof TQuickNote note) {
            setText(note.getName());
            String color = note.getColor();
            if (StringUtils.isNotEmpty(color)) {
                setForeground(UIManager.getColor(color));
            } else {
                setForeground(MainWindow.getInstance().getMainPanel().getForeground());
            }
            setIcon(UIManager.getIcon("FileView.fileIcon"));
        } else if (userObject instanceof String folderPath) {
            setText(displayFolderName(folderPath));
            setForeground(MainWindow.getInstance().getMainPanel().getForeground());
            setIcon(UIManager.getIcon("FileView.directoryIcon"));
        }
        return component;
    }

    private static String displayFolderName(String folderPath) {
        if (StringUtils.isBlank(folderPath)) {
            return "/";
        }
        int index = folderPath.lastIndexOf('/');
        if (index < 0) {
            return folderPath;
        }
        return folderPath.substring(index + 1);
    }
}
