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

package wiiexplorer.fileio;

import java.io.*;

public class Yaz0File extends MemoryFile
{
    public Yaz0File(FileBase backendfile) throws IOException
    {
        super(backendfile.getContents());
        
        buffer = Yaz0.decompress(buffer);
        logicalSize = buffer.length;
        backend = backendfile;
        backend.releaseStorage();
    }
    
    @Override
    public void save() throws IOException
    {
        // TODO: recompress here?
        
        if (backend != null)
        {
            backend.setContents(buffer);
            backend.save();
            backend.releaseStorage();
        }
    }
    
    @Override
    public void close() throws IOException
    {
        if (backend != null)
            backend.close();
    }
    
    
    protected FileBase backend;
}
