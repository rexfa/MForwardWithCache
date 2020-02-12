package com.bjcsc.pemc.rex.mf.util;

import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;



public class JTreeUtil
{
	   /**
     * 完全展开或关闭一个树,用于递规执行
     * @param tree JTree
     * @param parent 父节点
     * @param expand 为true则表示展开树,否则为关闭整棵树
     */
    public static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
    
    public static String getNodeText(DefaultMutableTreeNode node)
    {
    	   return (String)node.getUserObject();

    }
}
