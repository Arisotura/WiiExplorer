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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class WiiIsoFilesystem implements FilesystemBase
{
    private void readPartition(long offset, int type, int n, int n2) throws IOException
    {
        file.position(offset + 0x1BF);
        byte[] partkey = file.readBytes(16);
        file.position(offset + 0x1DC);
        byte[] keyiv = new byte[16];
        for (int i = 0; i < 8; i++) keyiv[i] = file.readByte();
        for (int i = 8; i < 16; i++) keyiv[i] = 0;
        
        file.position(offset + 0x2B8);
        long dataoffset = (long)file.readInt() << 2;
        long datasize = (long)file.readInt() << 2;
        System.out.println(String.format("PARTITION %1$d/%2$d %5$016X -> -> %3$016X %4$016X", n, n2, dataoffset, datasize, offset));
        
        try
        {
            SecretKeySpec keyspec = new SecretKeySpec(masterkey, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(keyiv);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            partkey = cipher.doFinal(partkey);

            EncryptedIsoBlock eib = new EncryptedIsoBlock(file, offset + 0x20000, partkey);
            eib.position(0x400 + 0x424);
            long fsoffset = eib.readInt() << 2;
            
            long lolz = fsoffset & ~0x7FFF;
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to decrypt: "+ex.getMessage());
        }
    }
    
    private void readPartitionTable(long offset, int numparts, int n) throws IOException
    {
        for (int p = 0; p < numparts; p++)
        {
            file.position(offset + p*8);
            long partoffset = (long)file.readInt() << 2;
            int parttype = file.readInt();
            
            readPartition(partoffset, parttype, n, p);
        }
    }
    
    private EncryptedIsoBlock getBlock(long offset)
    {
        if (blockCache.containsKey(offset))
            return blockCache.get(offset);
        
        /*EncryptedIsoBlock block = new EncryptedIsoBlock(file, offset, partkey);
        blockCache.put(offset, block);
        return block;*/
        return null;
    }
    
    
    public WiiIsoFilesystem(FileBase _file) throws IOException
    {
        if (masterkey == null)
        {
            try
            {
                FileBase keyfile = new ExternalFile("key.bin", "r");
                masterkey = keyfile.getContents();
                keyfile.close();
            }
            catch (IOException ex)
            {
                throw new IOException("WiiIsoFilesystem: Failed to load the key: "+ex.getMessage());
            }
        }
        
        blockCache = new HashMap<>(32);
        
        file = _file;
        file.setBigEndian(true);
        
        file.position(0x18);
        int tag = file.readInt();
        if (tag != 0x5D1C9EA3)
            throw new IOException(String.format("!File isn't a Wii ISO (tag 0x%1$08X, expected 0x5D1C9EA3)", tag));
        
        for (int p = 0; p < 4; p++)
        {
            file.position(0x40000 + p*8);
            int numparts = file.readInt();
            long partsoffset = (long)file.readInt() << 2;
            readPartitionTable(partsoffset, numparts, p);
        }
    }

    @Override
    public void save() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRoot()
    {
        return "";
    }

    @Override
    public List<String> getDirectories(String directory)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean directoryExists(String directory)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createDirectory(String parent, String newdir)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void renameDirectory(String directory, String newname)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteDirectory(String directory)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getFiles(String directory)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean fileExists(String directory)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int fileSize(String filename)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileBase openFile(String filename) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createFile(String parent, String newfile, FileBase thedata)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void renameFile(String file, String newname)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteFile(String file)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    private FileBase file;
    private static byte[] masterkey = null;
    
    private HashMap<Long, EncryptedIsoBlock> blockCache;
}
