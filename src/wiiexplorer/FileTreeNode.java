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

import javax.swing.tree.DefaultMutableTreeNode;
import wiiexplorer.fileio.FilesystemBase;

public class FileTreeNode extends DefaultMutableTreeNode
{
    public FileTreeNode(FilesystemBase fs, String path)
    {
        super(path);
        
        this.fs = fs;
        this.isFile = fs.fileExists(path);
    }
    
    @Override
    public String toString()
    {
        String lol = userObject.toString();
        if (lol.equals("/")) return "[root]";
        return lol.substring(lol.lastIndexOf("/") + 1);
    }
    
    
    private FilesystemBase fs;
    public boolean isFile;
}
