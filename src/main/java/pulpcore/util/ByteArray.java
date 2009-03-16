/*
    Copyright (c) 2008, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package pulpcore.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import pulpcore.util.crypt.ARC4;

/**
    The ByteArray class encapsulates reading data from and writing data to an array of bytes.
    <p>
    The ByteArray class is designed to combine the functionality of ByteArrayInputStream,
    ByteArrayOutputStream, DataInputStream, and DataOutputStream, and adds functionality
    like compression and encryption.
    <p>
    Both little endian and big endian byte orders are provided, and can be toggled at any time
    during the read/write process. The default is big endian (the same byte order as 
    DataInputStream and DataOutputStream).
    <p>
    Note that, when the position is at the end, the length of the underlying data array is 
    increased each time a write method is called. It can often be more economical to first 
    allocate a larger array, perform the writes, and then call {@link #truncate()}:
    <pre>
    ByteArray byteArray = new ByteArray(1000);
    for (int i = 0; i < 100; i++) {
        byteArray.writeInt(i);
    }
    byteArray.truncate();
    </pre>
*/
public class ByteArray {
    
    /** 
        Big Endian is the byte order where data is stored Most Significant Byte (MSB) first.
        Big Endian is Java's internal byte order, common in pre-Intel Macs, and 
        is the common network byte order. 
    */
    public static final int BIG_ENDIAN = 0;
    
    /** 
        Little Endian is the byte order where data is stored Least Significant Byte (LSB) first.
        Little Endian is common on PCs and Intel-based Macs. 
    */
    public static final int LITTLE_ENDIAN = 1;
    
    
    private byte[] data;
    private int position;
    private int byteOrder;
    

    /**
        Creates a new ByteArray with a length of zero.
    */
    public ByteArray() {
        this(new byte[0]);
    }
    
    
    /**
        Creates a new ByteArray with the specified length. The bytes are filled with zeros.
    */
    public ByteArray(int length) {
        this(new byte[length]);
    }
    
    
    /**
        Creates a new ByteArray of the specified data. The data array is initially shared.
    */
    public ByteArray(byte[] data) {
        this.data = data;
        reset();
    }
    
    
    /**
        Sets the byte order to BIG_ENDIAN and the position to zero.
    */
    public void reset() {
        position = 0;
        byteOrder = BIG_ENDIAN;
    }
    
    
    /**
        Gets the current underlying data array.
    */
    public byte[] getData() {
        return data;
    }
    
    
    /**
        Gets the current byte order for reading or writing data.
        @return {@link #BIG_ENDIAN} or {@link #LITTLE_ENDIAN}
    */
    public int getByteOrder() {
        return byteOrder;
    }
    
    
    /**
        Sets the current byte order for reading or writing data.
        @param byteOrder {@link #BIG_ENDIAN} or {@link #LITTLE_ENDIAN}
    */
    public void setByteOrder(int byteOrder) {
        this.byteOrder = byteOrder;
    }
    
    
    /**
        Returns the length of the underlying data. 
    */
    public int length() {
        return data.length;
    }
    
    
    /**
        Sets the length of the underlying data. If the new length is shorter, the data is 
        truncated and if the position is greater than the new length, it is set to the new length.
    */
    public void setLength(int length) {
        if (length != data.length) {
            byte[] oldData = data;
            data = new byte[length];
            System.arraycopy(oldData, 0, data, 0, Math.min(oldData.length, length));
            if (position > length) {
                position = length;
            }
        }
    }
    
    
    /**
        Gets the current position where data is read from and write to.
    */
    public int position() {
        return position;
    }
    
    
    /**
        Sets the current position where data is read from and write to. 
        @throws IndexOutOfBoundsException If the new position is less than zero or greater than the 
        data length;
    */
    public void setPosition(int position) throws IndexOutOfBoundsException {
        if (position < 0 || position > data.length) {
            throw new IndexOutOfBoundsException();
        }
        
        this.position = position;
    }
    
    
    /**
        Sets the length of the data of this ByteArray to the current position.
    */
    public void truncate() {
        setLength(position);
    }
    
    
    /**
        Returns the number of bytes that can be read from the current position to the length of 
        the data.
    */
    public int available() {
        return length() - position();
    }
    
    
    /**
        Crypts the entire byte array using the specified cipher. The position is not changed.
    */
    public void crypt(ARC4 cipher) {
        cipher.reset();
        cipher.crypt(data);
    }
    
    
    //
    // Compression methods
    //
    
    
    /**
        Compresses this entire byte array using ZLIB compression. The position is set to the
        end of the compressed data.
    */
    public void compress() {
        compress(new Deflater());
    }
    
    
    /**
        Compresses this entire byte array using ZLIB compression. The position is set to the
        end of the compressed data.
    */
    public void compress(Deflater deflater) {
        ByteArray newData = compress(deflater, data, 0, data.length);
        this.data = newData.data;
        this.position = newData.position;
    }
    
    
    private static ByteArray compress(Deflater deflater, byte[] data, int offset, int length) {
        ByteArray newData = new ByteArray();
        byte[] buffer = new byte[4096];
        deflater.reset();
        deflater.setInput(data, offset, length);
        deflater.finish();
        while (!deflater.finished()) {
            int bytesRead = deflater.deflate(buffer);
            newData.write(buffer, 0, bytesRead);
        }
        deflater.end();
        return newData;
    }
    
    
    /**
        Decompresses this entire byte array using ZLIB compression. The position is set to the
        end of the decompressed data.
        @throws IOException if the decompression failed.
    */
    public void decompress() throws IOException {
        decompress(new Inflater());
    }
    
        
    /**
        Decompresses this entire byte array using ZLIB compression. The position is set to 0.
        @throws IOException if the decompression failed.
    */
    public void decompress(Inflater inflater) throws IOException {
        ByteArray newData = decompress(inflater, data, 0, data.length);
        if (newData == null) {
            throw new IOException();
        }
        else {
            this.data = newData.data;
            this.position = 0;
        }
    }
    
    
    private static ByteArray decompress(Inflater inflater, byte[] data, int offset, int length) {
        ByteArray newData = new ByteArray();
        byte[] buffer = new byte[4096];
        inflater.reset();
        inflater.setInput(data, offset, length);
        try {
            while (!inflater.needsInput()) {
                int bytesRead = inflater.inflate(buffer);
                newData.write(buffer, 0, bytesRead);
            }
            inflater.end();
        }
        catch (DataFormatException ex) {
            return null;
        }
        return newData;
    }
        
    
    //
    // Read methods
    //
    
    
    private void checkAvailable(int length) throws IndexOutOfBoundsException {
        if (available() < length) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    
    public byte readByte() throws IndexOutOfBoundsException {
        checkAvailable(1);
        return data[position++];
    }
    
    
    /**
        Reads bytes from the current position in this ByteArray into a buffer.
        @return the number of bytes read (always returns the length of the byte array)
        @throws IndexOutOfBoundsException if there aren't enough remaining bytes in this ByteArray to 
        fill the buffer
    */
    public int read(byte[] buffer) throws IndexOutOfBoundsException {
        return read(buffer, 0, buffer.length);
    }
    
    
    /**
        Reads bytes from the current position in this ByteArray into a buffer.
        @return the number of bytes read (always returns the specified length)
        @throws IndexOutOfBoundsException if there aren't enough remaining bytes in this ByteArray to 
        read the specified number of bytes.
    */
    public int read(byte[] buffer, int offset, int length) throws IndexOutOfBoundsException {
        if (length == 0) {
            return 0;
        }
        checkAvailable(length);
        System.arraycopy(data, position, buffer, offset, length);
        position += length;
        return length;
    }
    
    
    public void read(OutputStream out) throws IOException {
        out.write(data, position, data.length - position);
        position = data.length;
    }
    
    
    public boolean readBoolean() throws IndexOutOfBoundsException {
        return (readByte() != 0);
    }
    
    
    public short readShort() throws IndexOutOfBoundsException {
        checkAvailable(2);
        if (byteOrder == LITTLE_ENDIAN) {
            return (short)(
                (data[position++] & 0xff) |
                ((data[position++] & 0xff) << 8));
        }
        else {
            return (short)(
                ((data[position++] & 0xff) << 8) | 
                (data[position++] & 0xff)); 
        }
    }
    
    
    public int readInt() throws IndexOutOfBoundsException {
        checkAvailable(4);
        if (byteOrder == LITTLE_ENDIAN) {
            return 
                (data[position++] & 0xff) |
                ((data[position++] & 0xff) << 8) | 
                ((data[position++] & 0xff) << 16) | 
                ((data[position++] & 0xff) << 24); 
        }
        else {
            return 
                ((data[position++] & 0xff) << 24) |
                ((data[position++] & 0xff) << 16) | 
                ((data[position++] & 0xff) << 8) | 
                (data[position++] & 0xff); 
        }
    }
    
    
    public long readLong() throws IndexOutOfBoundsException {
        checkAvailable(8);
        if (byteOrder == LITTLE_ENDIAN) {
            return (readInt() & 0xffffffffL) | ((readInt() & 0xffffffffL) << 32L);
        }
        else {
            return ((readInt() & 0xffffffffL) << 32L) | (readInt() & 0xffffffffL);
        }
    }
    
    
    public float readFloat() throws IndexOutOfBoundsException {
        return Float.intBitsToFloat(readInt());
    }
    
    
    public double readDouble() throws IndexOutOfBoundsException {
        return Double.longBitsToDouble(readLong());
    }
    
    
    /**
        Reads a string in the same modified UTF-8 format used in DataInputStream.
    */
    public String readUTF() throws IndexOutOfBoundsException, UTFDataFormatException {
        checkAvailable(2);
        int utfLength = readShort() & 0xffff;
        checkAvailable(utfLength);
        
        int goalPosition = position() + utfLength;
        
        StringBuffer string = new StringBuffer(utfLength);
        while (position() < goalPosition) {
            int a = readByte() & 0xff;
            if ((a & 0x80) == 0) {
                // One-byte
                string.append((char)a);
            }
            else {
                int b = readByte() & 0xff;
                if ((b & 0xc0) != 0x80) {
                    throw new UTFDataFormatException();
                }
                
                if ((a & 0xe0) == 0xc0) {
                    // Two-byte
                    char ch = (char)(((a & 0x1f) << 6) | (b & 0x3f));
                    string.append(ch);
                }
                else if ((a & 0xf0) == 0xe0) {
                    // Three-byte
                    int c = readByte() & 0xff;
                    if ((c & 0xc0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    char ch = (char)(((a & 0x0f) << 12) | ((b & 0x3f) << 6) | (c & 0x3f));
                    string.append(ch);
                }
                else {
                    throw new UTFDataFormatException();
                }
            }
        }
        return string.toString();
    }
    
    
    //
    // Write methods
    //
    
    
    private void ensureCapacity(int dataSize) {
        if (position + dataSize > data.length) {
            setLength(position + dataSize);
        }
    }


    public void writeByte(int v) {
        ensureCapacity(1);
        data[position++] = (byte)v;
    }
    
    
    public void write(byte[] buffer) {
        write(buffer, 0, buffer.length);
    }

    
    public void write(byte[] buffer, int offset, int length) {
        if (length == 0) {
            return;
        }
        ensureCapacity(length);
        System.arraycopy(buffer, offset, data, position, length);
        position += length;
    }
    
    
    public void write(InputStream in) throws IOException {
        byte[] buffer = new byte[4096];
        
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                return;
            }
            write(buffer, 0, bytesRead);
        }
    }
    
    
    public void writeBoolean(boolean v) {
        writeByte(v ? -1 : 0);
    }


    public void writeShort(int v) {
        ensureCapacity(2);
        if (byteOrder == LITTLE_ENDIAN) {
            data[position++] = (byte)(v & 0xff);
            data[position++] = (byte)((v >> 8) & 0xff);
        }
        else {
            data[position++] = (byte)((v >> 8) & 0xff);
            data[position++] = (byte)(v & 0xff);
        }
    }
    
    
    public void writeInt(int v) {
        ensureCapacity(4);
        if (byteOrder == LITTLE_ENDIAN) {
            data[position++] = (byte)(v & 0xff);
            data[position++] = (byte)((v >> 8) & 0xff);
            data[position++] = (byte)((v >> 16) & 0xff);
            data[position++] = (byte)(v >>> 24);
        }
        else {
            data[position++] = (byte)(v >>> 24);
            data[position++] = (byte)((v >> 16) & 0xff);
            data[position++] = (byte)((v >> 8) & 0xff);
            data[position++] = (byte)(v & 0xff);
        }
    }
    
    
    public void writeLong(long v) {
        ensureCapacity(8);
        if (byteOrder == LITTLE_ENDIAN) {
            writeInt((int)(v & 0xffffffffL));
            writeInt((int)(v >>> 32));
        }
        else {
            writeInt((int)(v >>> 32));
            writeInt((int)(v & 0xffffffffL));
        }
    }
   
    
    public void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }
    
    
    public void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }
    
    
    /**
        Writes a string in the same modified UTF-8 format used by DataOutputStream.
        @throws UTFDataFormatException if the encoded string is longer than 65535 bytes.
    */
    public void writeUTF(String s) throws UTFDataFormatException {
        
        // Find the UTF length
        int utfLength = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch > 0 && ch < 0x80) {
                utfLength++;
            }
            else if (ch == 0 || (ch >= 0x80 && ch < 0x800)) {
                utfLength += 2;
            }
            else {
                utfLength += 3;
            }
        }
        
        if (utfLength > 65535) {
            throw new UTFDataFormatException();
        }
        
        ensureCapacity(2 + utfLength);
        writeShort(utfLength);
        
        // Write the UTF values
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            if (ch > 0 && ch < 0x80) {
                writeByte(ch);
            }
            else if (ch == 0 || (ch >= 0x80 && ch < 0x800)) {
                writeByte(0xc0 | (0x1f & (ch >> 6)));
                writeByte(0x80 | (0x3f & ch));
            }
            else {
                writeByte(0xe0 | (0x0f & (ch >> 12)));
                writeByte(0x80 | (0x3f & (ch >> 6)));
                writeByte(0x80 | (0x3f & ch));
            }
        }
    }
}
