/*
    Copyright 2012 The WiiExplorer team

    This file is part of WiiExplorer.

    WiiExplorer is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    WiiExplorer is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with WiiExplorer. If not, see http://www.gnu.org/licenses/.
*/

package wiiexplorer;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class FileTreeRenderer extends DefaultTreeCellRenderer
{
    public FileTreeRenderer()
    {
        folderOpenIcon = new ImageIcon(WiiExplorer.class.getResource("/Resources/folder_blue_open.png"));
        folderClosedIcon = new ImageIcon(WiiExplorer.class.getResource("/Resources/folder_blue.png"));
        fileIcon = new ImageIcon(WiiExplorer.class.getResource("/Resources/binary.png"));
        
        setOpenIcon(folderOpenIcon);
        setClosedIcon(folderClosedIcon);
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        // hack
        TreePath path = tree.getPathForRow(row);
        if (path != null)
        {
            TreeNode tn = (TreeNode)path.getLastPathComponent();
            if (tn != null && tn.getClass() == FileTreeNode.class)
            {
                if (((FileTreeNode)tn).isFile)
                    setLeafIcon(fileIcon);
                else
                    setLeafIcon(folderClosedIcon);
            }
        }
        
        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }
    
    
    private ImageIcon folderOpenIcon, folderClosedIcon, fileIcon;
}
