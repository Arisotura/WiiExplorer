/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package wiiexplorer.fileio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface FilesystemBase
{
    public void save() throws IOException;
    public void close() throws IOException;
    
    public String getRoot();
    public List<String> getDirectories(String directory);
    public boolean directoryExists(String directory);
    public void createDirectory(String parent, String newdir);
    public void renameDirectory(String directory, String newname);
    public void deleteDirectory(String directory);
    
    public List<String> getFiles(String directory);
    public boolean fileExists(String directory);
    public int fileSize(String filename);
    public FileBase openFile(String filename) throws FileNotFoundException;
    public void createFile(String parent, String newfile, FileBase thedata);
    public void renameFile(String file, String newname);
    public void deleteFile(String file);
}
