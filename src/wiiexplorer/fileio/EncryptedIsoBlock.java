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
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedIsoBlock extends MemoryFile
{
    public EncryptedIsoBlock(FileBase backend, long offset, byte[] key) throws IOException
    {
        this.backend = backend;
        this.blockOffset = offset;
        this.key = key;
        
        backend.position(offset);
        buffer = backend.readBytes(0x8000);
        logicalSize = 0x8000;
        
        try
        {
            byte[] emptyiv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            
            position(0x3D0);
            userIV = readBytes(16);
            
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(emptyiv);
            IvParameterSpec ivspec2 = new IvParameterSpec(userIV);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
            for (int i = 0; i < 0x400; i += 16)
            {
                position(i);
                byte[] data = readBytes(16);
                data = cipher.update(data);
                position(i);
                writeBytes(data);
            }
            
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec2);
            for (int i = 0x400; i < 0x7FF0; i += 16)
            {
                position(i);
                byte[] data = readBytes(16);
                data = cipher.update(data);
                position(i);
                writeBytes(data);
            }
            position(0x7FF0);
            byte[] data = readBytes(16);
            data = cipher.doFinal(data);
            position(0x7FF0);
            writeBytes(data);
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to decrypt: "+ex.getMessage());
        }
    }
    
    @Override
    public void save() throws IOException
    {
        try
        {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            //
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to rehash: "+ex.getMessage());
        }
        
        backend.position(blockOffset);
        position(0);
        
        try
        {
            byte[] emptyiv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivspec = new IvParameterSpec(emptyiv);
            IvParameterSpec ivspec2 = new IvParameterSpec(userIV);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            for (int i = 0; i < 0x400; i += 16)
            {
                byte[] data = readBytes(16);
                data = cipher.update(data);
                backend.writeBytes(data);
            }
            
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec2);
            for (int i = 0x400; i < 0x7FF0; i += 16)
            {
                byte[] data = readBytes(16);
                data = cipher.update(data);
                backend.writeBytes(data);
            }
            byte[] data = readBytes(16);
            data = cipher.doFinal(data);
            backend.writeBytes(data);
        }
        catch (Exception ex)
        {
            throw new IOException("Failed to encrypt: "+ex.getMessage());
        }
    }
    
    @Override
    public void setLength(long length) throws IOException
    {
        throw new IOException("EncryptedIsoBlock can't be resized");
    }
    
    @Override
    protected void resizeBuffer(int newsize) throws IOException
    {
        throw new IOException(String.format("Out-of-bounds write in EncryptedIsoBlock: %1$08X", position()));
    }
    
    
    private FileBase backend;
    private long blockOffset;
    
    private byte[] key;
    private byte[] userIV;
}
