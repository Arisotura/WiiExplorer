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
import java.util.*;

public class RarcFilesystem implements FilesystemBase
{
    public RarcFilesystem(FileBase _file) throws IOException
    {
        file = new Yaz0File(_file);
        file.setBigEndian(true);

        file.position(0);
        int tag = file.readInt();
        if (tag != 0x52415243) 
            throw new IOException(String.format("!File isn't a RARC (tag 0x%1$08X, expected 0x52415243)", tag));

        file.position(0xC);
        int fileDataOffset = file.readInt() + 0x20;
        file.position(0x20);
        int numDirNodes = file.readInt();
        int dirNodesOffset = file.readInt() + 0x20;
        int numFileEntries = file.readInt();
        int fileEntriesOffset = file.readInt() + 0x20;
        file.skip(0x4);
        int stringTableOffset = file.readInt() + 0x20;
        unk38 = file.readInt();

        dirEntries = new LinkedHashMap<>(numDirNodes);
        fileEntries = new LinkedHashMap<>(numFileEntries);

        DirEntry root = new DirEntry();
        root.parentDir = null;

        file.position(dirNodesOffset + 0x6);
        int rnoffset = file.readShort();
        file.position(stringTableOffset + rnoffset);
        root.name = file.readString("ASCII", 0);
        root.fullName = "/" + root.name;
        root.tempID = 0;

        dirEntries.put(root.fullName.toLowerCase(), root);

        for (int i = 0; i < numDirNodes; i++)
        {
            DirEntry parentdir = null;
            for (DirEntry de : dirEntries.values())
            {
                if (de.tempID == i)
                {
                    parentdir = de;
                    break;
                }
            }

            file.position(dirNodesOffset + (i * 0x10) + 10);

            short numentries = file.readShort();
            int firstentry = file.readInt();

            for (int j = 0; j < numentries; j++)
            {
                int entryoffset = fileEntriesOffset + ((j + firstentry) * 0x14);
                file.position(entryoffset);

                int fileid = file.readShort() & 0xFFFF;
                file.skip(4);
                int nameoffset = file.readShort() & 0xFFFF;
                int dataoffset = file.readInt();
                int datasize = file.readInt();

                file.position(stringTableOffset + nameoffset);
                String name = file.readString("ASCII", 0);
                if (name.equals(".") || name.equals("..")) continue;

                String fullname = parentdir.fullName + "/" + name;

                if (fileid == 0xFFFF)
                {
                    DirEntry d = new DirEntry();
                    d.parentDir = parentdir;
                    d.name = name;
                    d.fullName = fullname;
                    d.tempID = dataoffset;

                    dirEntries.put(fullname.toLowerCase(), d);
                    parentdir.childrenDirs.add(d);
                }
                else
                {
                    FileEntry f = new FileEntry();
                    f.parentDir = parentdir;
                    f.dataOffset = fileDataOffset + dataoffset;
                    f.dataSize = datasize;
                    f.name = name;
                    f.fullName = fullname;
                    f.data = null;

                    fileEntries.put(fullname.toLowerCase(), f);
                    parentdir.childrenFiles.add(f);
                }
            }
        }
    }
    
    private int align32(int val)
    {
        return (val + 0x1F) & ~0x1F;
    }
    
    private int dirMagic(String name)
    {
        String uppername = name.toUpperCase();
        int ret = 0;
        
        for (int i = 0; i < 4; i++)
        {
            ret <<= 8;
            
            if (i >= uppername.length())
                ret += 0x20;
            else
                ret += uppername.charAt(i);
        }
        
        return ret;
    }
    
    private short nameHash(String name)
    {
        short ret = 0;
        for (char ch : name.toCharArray())
        {
            ret *= 3;
            ret += ch;
        }
        return ret;
    }
    
    @Override
    public void save() throws IOException
    {
        for (FileEntry fe : fileEntries.values())
        {
            if (fe.data != null) continue;
            file.position(fe.dataOffset);
            fe.data = file.readBytes((int)fe.dataSize);
        }
        
        int dirOffset = 0x40;
        int fileOffset = dirOffset + align32(dirEntries.size() * 0x10);
        int stringOffset = fileOffset + align32((fileEntries.size() + (dirEntries.size() * 3) - 1) * 0x14);
        
        int dataOffset = stringOffset;
        int dataLength = 0;
        for (DirEntry de : dirEntries.values())
            dataOffset += de.name.length() + 1;
        for (FileEntry fe : fileEntries.values())
        {
            dataOffset += fe.name.length() + 1;
            dataLength += align32(fe.data.length);
        }
        dataOffset += 5;
        dataOffset = align32(dataOffset);
        
        int dirSubOffset = 0;
        int fileSubOffset = 0;
        int stringSubOffset = 0;
        int dataSubOffset = 0;
        
        file.setLength(dataOffset + dataLength);
        
        // RARC header
        // certain parts of this will have to be written later on
        file.position(0);
        file.writeInt(0x52415243);
        file.writeInt(dataOffset + dataLength);
        file.writeInt(0x00000020);
        file.writeInt(dataOffset - 0x20);
        file.writeInt(dataLength);
        file.writeInt(dataLength);
        file.writeInt(0x00000000);
        file.writeInt(0x00000000);
        file.writeInt(dirEntries.size());
        file.writeInt(dirOffset - 0x20);
        file.writeInt(fileEntries.size() + (dirEntries.size() * 3) - 1);
        file.writeInt(fileOffset - 0x20);
        file.writeInt(dataOffset - stringOffset);
        file.writeInt(stringOffset - 0x20);
        file.writeInt(unk38);
        file.writeInt(0x00000000);
        
        file.position(stringOffset);
        file.writeString("ASCII", ".", 0);
        file.writeString("ASCII", "..", 0);
        stringSubOffset += 5;
        
        Stack<Iterator<DirEntry>> dirstack = new Stack<>();
        Object[] entriesarray = dirEntries.values().toArray();
        DirEntry curdir = (DirEntry)entriesarray[0];
        int c = 1;
        while (curdir.parentDir != null) curdir = (DirEntry)entriesarray[c++];
        short fileid = 0;
        for (;;)
        {
            // write the directory node
            curdir.tempID = dirSubOffset / 0x10;
            file.position(dirOffset + dirSubOffset);
            file.writeInt((curdir.tempID == 0) ? 0x524F4F54 : dirMagic(curdir.name));
            file.writeInt(stringSubOffset);
            file.writeShort(nameHash(curdir.name));
            file.writeShort((short)(2 + curdir.childrenDirs.size() + curdir.childrenFiles.size()));
            file.writeInt(fileSubOffset / 0x14);
            dirSubOffset += 0x10;
            
            if (curdir.tempID > 0)
            {
                file.position(curdir.tempNameOffset);
                file.writeShort((short)stringSubOffset);
                file.writeInt(curdir.tempID);
            }
            file.position(stringOffset + stringSubOffset);
            stringSubOffset += file.writeString("ASCII", curdir.name, 0);
            
            // write the child file/dir entries
            file.position(fileOffset + fileSubOffset);
            for (DirEntry cde : curdir.childrenDirs)
            {
                file.writeShort((short)0xFFFF);
                file.writeShort(nameHash(cde.name));
                file.writeShort((short)0x0200);
                cde.tempNameOffset = (int)file.position();
                file.skip(6);
                file.writeInt(0x00000010);
                file.writeInt(0x00000000);
                fileSubOffset += 0x14;
            }
            
            for (FileEntry cfe : curdir.childrenFiles)
            {
                file.position(fileOffset + fileSubOffset);
                file.writeShort(fileid);
                file.writeShort(nameHash(cfe.name));
                file.writeShort((short)0x1100);
                file.writeShort((short)stringSubOffset);
                file.writeInt(dataSubOffset);
                file.writeInt(cfe.data.length);
                file.writeInt(0x00000000);
                fileSubOffset += 0x14;
                fileid++;

                file.position(stringOffset + stringSubOffset);
                stringSubOffset += file.writeString("ASCII", cfe.name, 0);
                
                file.position(dataOffset + dataSubOffset);
                cfe.dataOffset = (int)file.position();
                cfe.dataSize = cfe.data.length;
                file.writeBytes(cfe.data);
                dataSubOffset += align32(cfe.data.length);
                cfe.data = null;
            }
            
            file.position(fileOffset + fileSubOffset);
            file.writeShort((short)0xFFFF);
            file.writeShort((short)0x002E);
            file.writeShort((short)0x0200);
            file.writeShort((short)0x0000);
            file.writeInt(curdir.tempID);
            file.writeInt(0x00000010);
            file.writeInt(0x00000000);
            file.writeShort((short)0xFFFF);
            file.writeShort((short)0x00B8);
            file.writeShort((short)0x0200);
            file.writeShort((short)0x0002);
            file.writeInt((curdir.parentDir != null) ? curdir.parentDir.tempID : 0xFFFFFFFF);
            file.writeInt(0x00000010);
            file.writeInt(0x00000000);
            fileSubOffset += 0x28;
            
            // determine who's next on the list
            // * if we have a child directory, process it
            // * otherwise, look if we have remaining siblings
            // * and if none, go back to our parent and look for siblings again
            // * until we have done them all
            if (!curdir.childrenDirs.isEmpty())
            {
                dirstack.push(curdir.childrenDirs.iterator());
                curdir = dirstack.peek().next();
            }
            else
            {
                curdir = null;
                while (curdir == null)
                {
                    if (dirstack.empty())
                        break;
                    
                    Iterator<DirEntry> it = dirstack.peek();
                    if (it.hasNext())
                        curdir = it.next();
                    else
                        dirstack.pop();
                }
                
                if (curdir == null)
                    break;
            }
        }
        
        file.save();
    }

    @Override
    public void close() throws IOException
    {
        file.close();
    }

    
    @Override
    public String getRoot()
    {
        Object[] entriesarray = dirEntries.values().toArray();
        DirEntry curdir = (DirEntry)entriesarray[0];
        int c = 1;
        while (curdir.parentDir != null) curdir = (DirEntry)entriesarray[c++];
        return curdir.fullName;
    }

    @Override
    public boolean directoryExists(String directory)
    {
        return dirEntries.containsKey(directory.toLowerCase());
    }

    @Override
    public List<String> getDirectories(String directory)
    {
        if (!dirEntries.containsKey(directory.toLowerCase())) 
            return null;
        
        DirEntry dir = dirEntries.get(directory.toLowerCase());
        
        List<String> ret = new ArrayList<>();
        for (DirEntry de : dir.childrenDirs)
        {
            ret.add(de.name);
        }
        
        return ret;
    }
    
    @Override
    public void createDirectory(String parent, String newdir)
    {
        if (!dirEntries.containsKey(parent.toLowerCase())) return;
        if (dirEntries.containsKey((parent + "/" + newdir).toLowerCase())) return;
        DirEntry parentdir = dirEntries.get(parent.toLowerCase());
        
        DirEntry de = new DirEntry();
        de.childrenDirs = new LinkedList<>();
        de.childrenFiles = new LinkedList<>();
        de.fullName = parent + "/" + newdir;
        de.name = newdir;
        de.parentDir = parentdir;
        
        parentdir.childrenDirs.add(de);
        dirEntries.put(de.fullName.toLowerCase(), de);
    }

    @Override
    public void renameDirectory(String directory, String newname)
    {
        if (!dirEntries.containsKey(directory.toLowerCase())) return;
        DirEntry de = dirEntries.get(directory.toLowerCase());
        DirEntry parent = de.parentDir;
        
        String parentpath = "";
        if (parent != null)
        {
            if (fileEntries.containsKey((parent.fullName + "/" + newname).toLowerCase()) ||
                dirEntries.containsKey((parent.fullName + "/" + newname).toLowerCase())) 
            return;
            
            parentpath = parent.fullName;
        }
        
        dirEntries.remove(de.fullName.toLowerCase());
        
        de.name = newname;
        de.fullName = parentpath + "/" + newname;
        
        dirEntries.put(de.fullName.toLowerCase(), de);
    }

    @Override
    public void deleteDirectory(String directory)
    {
        if (!dirEntries.containsKey(directory.toLowerCase())) return;
        DirEntry de = dirEntries.get(directory.toLowerCase());
        DirEntry parent = de.parentDir;
        
        if (parent != null)
            parent.childrenDirs.remove(de);
        
        dirEntries.remove(directory.toLowerCase());
        
        for (DirEntry cde : de.childrenDirs)
            dirEntries.remove(cde.fullName.toLowerCase());
        for (FileEntry cfe : de.childrenFiles)
            fileEntries.remove(cfe.fullName.toLowerCase());
    }


    @Override
    public boolean fileExists(String filename)
    {
        return fileEntries.containsKey(filename.toLowerCase());
    }
    
    @Override
    public int fileSize(String filename)
    {
        if (!fileEntries.containsKey(filename.toLowerCase())) return 0;
        
        return fileEntries.get(filename.toLowerCase()).dataSize;
    }

    @Override
    public List<String> getFiles(String directory)
    {
        if (!dirEntries.containsKey(directory.toLowerCase())) 
            return null;
        
        DirEntry dir = dirEntries.get(directory.toLowerCase());
        
        List<String> ret = new ArrayList<>();
        for (FileEntry fe : dir.childrenFiles)
        {
            ret.add(fe.name);
        }
        
        return ret;
    }

    @Override
    public FileBase openFile(String filename) throws FileNotFoundException
    {
        if (!fileEntries.containsKey(filename.toLowerCase()))
            throw new FileNotFoundException(filename + " not found in RARC");
        
        try
        {
            return new RarcFile(this, filename);
        }
        catch (IOException ex)
        {
            throw new FileNotFoundException("got IOException");
        }
    }
    
    @Override
    public void createFile(String parent, String newfile, FileBase thedata)
    {
        if (!dirEntries.containsKey(parent.toLowerCase())) return;
        if (fileEntries.containsKey((parent + "/" + newfile).toLowerCase())) return;
        if (dirEntries.containsKey((parent + "/" + newfile).toLowerCase())) return;
        DirEntry parentdir = dirEntries.get(parent.toLowerCase());
        
        FileEntry fe = new FileEntry();
        try { fe.data = thedata.getContents(); }
        catch (IOException ex) { return; }
        
        fe.dataSize = fe.data.length;
        fe.fullName = parent + "/" + newfile;
        fe.name = newfile;
        fe.parentDir = parentdir;
        
        parentdir.childrenFiles.add(fe);
        fileEntries.put(fe.fullName.toLowerCase(), fe);
    }

    @Override
    public void renameFile(String file, String newname)
    {
        if (!fileEntries.containsKey(file.toLowerCase())) return;
        FileEntry fe = fileEntries.get(file.toLowerCase());
        DirEntry parent = fe.parentDir;
        
        if (fileEntries.containsKey((parent.fullName + "/" + newname).toLowerCase()) ||
            dirEntries.containsKey((parent.fullName + "/" + newname).toLowerCase())) 
            return;
        
        fileEntries.remove(fe.fullName.toLowerCase());
        
        fe.name = newname;
        fe.fullName = parent.fullName + "/" + newname;
        
        fileEntries.put(fe.fullName.toLowerCase(), fe);
    }

    @Override
    public void deleteFile(String file)
    {
        if (!fileEntries.containsKey(file.toLowerCase())) return;
        FileEntry fe = fileEntries.get(file.toLowerCase());
        DirEntry parent = fe.parentDir;
        
        parent.childrenFiles.remove(fe);
        fileEntries.remove(file.toLowerCase());
    }


    // support functions for RarcFile
    public byte[] getFileContents(String fullname) throws IOException
    {
        FileEntry fe = fileEntries.get(fullname.toLowerCase());

        file.position(fe.dataOffset);
        return file.readBytes((int)fe.dataSize);
    }

    public void reinsertFile(RarcFile _file) throws IOException
    {
        FileEntry fe = fileEntries.get(_file.fileName.toLowerCase());

        _file.position(0);
        fe.data = _file.readBytes((int)_file.getLength());

        file.save();
    }


    private class FileEntry
    {
        public FileEntry()
        { 
            data = null;
        }
        
        public int dataOffset;
        public int dataSize;

        public DirEntry parentDir;

        public String name;
        public String fullName;
        
        public byte[] data;
    }

    private class DirEntry
    {
        public DirEntry()
        {
            childrenDirs = new LinkedList<>();
            childrenFiles = new LinkedList<>();
        }

        public DirEntry parentDir;
        public LinkedList<DirEntry> childrenDirs;
        public LinkedList<FileEntry> childrenFiles;

        public String name;
        public String fullName;
        
        public int tempID;
        public int tempNameOffset;
    }


    private FileBase file;

    private int unk38;

    private LinkedHashMap<String, FileEntry> fileEntries;
    private LinkedHashMap<String, DirEntry> dirEntries;
}
