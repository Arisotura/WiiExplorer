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

import java.io.IOException;
import java.util.HashMap;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedIsoPartition
{
    public EncryptedIsoPartition(FileBase iso, long offset) throws IOException
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
                throw new IOException("Failed to load the ISO master key: "+ex.getMessage());
            }
        }
        
        this.iso = iso;
        this.offset = offset;
        
        iso.position(offset + 0x1BF);
        key = iso.readBytes(16);
        iso.position(offset + 0x1DC);
        byte[] keyiv = new byte[16];
        for (int i = 0; i < 8; i++) keyiv[i] = iso.readByte();
        for (int i = 8; i < 16; i++) keyiv[i] = 0;
        
        iso.position(offset + 0x2B8);
        dataOffset = (long)iso.readInt() << 2;
        dataSize = (long)iso.readInt() << 2;
        totalLength = (dataSize / 0x8000) * 0x7C00;
        
        try
        {
            SecretKeySpec keyspec = new SecretKeySpec(masterkey, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(keyiv);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            key = cipher.doFinal(key);
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to decrypt: "+ex.getMessage());
        }
        
        curPosition = 0;
        curBlock = new EncryptedIsoBlock(iso, offset, key);
        unsavedWrites = false;
    }
    
    public void save() throws IOException
    {
        curBlock.save();
    }

    public void close() throws IOException
    {
    }


    public long getLength() throws IOException
    {
        return totalLength;
    }

    public void setLength(long length) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long position() throws IOException
    {
        return curPosition;
    }
    
    private void checkBounds(long pos) throws IOException
    {
        if (pos >= totalLength)
            throw new IOException("ISO partition: going out of bounds (needs to be resized first)");
    }

    public void position(long newpos) throws IOException
    {
        checkBounds(newpos);
        
        long curblock = curPosition / 0x7C00;
        long newblock = newpos / 0x7C00;
        
        if (curblock == newblock)
        {
            curPosition = newpos;
            return;
        }
        
        // TODO: resize support
    }

    public void skip(long nbytes) throws IOException
    {
        position(curPosition + nbytes);
    }

    public byte readByte() throws IOException
    {
        checkBounds(curPosition);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public short readShort() throws IOException
    {
        checkBounds(curPosition + 1);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int readInt() throws IOException
    {
        checkBounds(curPosition + 3);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String readString(int length) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public byte[] readBytes(int length) throws IOException
    {
        checkBounds(curPosition + length - 1);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeByte(byte val) throws IOException
    {
        checkBounds(curPosition);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeShort(short val) throws IOException
    {
        checkBounds(curPosition + 1);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeInt(int val) throws IOException
    {
        checkBounds(curPosition + 3);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int writeString(String val, int length) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void writeBytes(byte[] stuff) throws IOException
    {
        checkBounds(curPosition + stuff.length - 1);
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    private FileBase iso;
    private long offset;
    
    private long dataOffset, dataSize, totalLength;
    
    private long curPosition;
    private EncryptedIsoBlock curBlock;
    private boolean unsavedWrites;
    
    private static byte[] masterkey = null;
    private byte[] key;
}
