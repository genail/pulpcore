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

package pulpcore.image;

import pulpcore.util.ByteArray;

/**
    Converts a CoreImage to the PNG file format. 
    <p>
    This class only encodes PNG files in 24-bit RGB or 32-bit RGBA format and performs no
    advanced compression techniques.
*/
public class PNGWriter {
    
    private static final long SIGNATURE = 0x89504e470d0a1a0aL;
    
    private static final int CHUNK_IHDR = 0x49484452;
    private static final int CHUNK_IDAT = 0x49444154;
    private static final int CHUNK_IEND = 0x49454e44;
    
    private static final byte COLOR_TYPE_RGB = 2;
    private static final byte COLOR_TYPE_RGB_WITH_ALPHA = 6;
    
    //
    // CRC algorithm from http://www.w3.org/TR/PNG-CRCAppendix.html
    //
    
    /** Table of CRCs of all 8-bit messages. */
    private static final int[] CRC_TABLE = new int[256];
   
    static {
        // Make the table for a fast CRC. 
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) {
                if ((c & 1) == 1) {
                    c = 0xedb88320 ^ (c >>> 1);
                }
                else {
                    c >>>= 1;
                }
            }
            CRC_TABLE[n] = c;
        }
    }
    
    // Prevent instantiation
    private PNGWriter() { }

    private static int getCRC(byte[] buf, int offset, int length) {
        int crc = 0xffffffff;
        for (int n = offset; n < offset + length; n++) {
            crc = CRC_TABLE[(crc ^ buf[n]) & 0xff] ^ (crc >>> 8);
        }
        return crc ^ 0xffffffff;
    }

    /**
        Encodes the image as a PNG file and return the results as a byte array, or null if there
        was an error writing the image.
    */
    public static ByteArray write(CoreImage image) {
        
        int width = image.getWidth();
        int height = image.getHeight();
        byte colorType = image.isOpaque() ? COLOR_TYPE_RGB : COLOR_TYPE_RGB_WITH_ALPHA;
        int bytesPerPixel = image.isOpaque() ? 3 : 4;
        
        // Create IHDR chunk data
        ByteArray ihdr = new ByteArray(13);
        ihdr.writeInt(width);
        ihdr.writeInt(height);
        ihdr.writeByte(8); // Bit depth
        ihdr.writeByte(colorType);
        ihdr.writeByte(0); // Compression method (deflate)
        ihdr.writeByte(0); // Filter method
        ihdr.writeByte(0); // Interlace method (none)
       
        // Create IDAT chunk data
        ByteArray idat = new ByteArray((width * bytesPerPixel + 1) * height);
        int[] rgbData = image.getData();
        int index = 0;
        for (int i = 0; i < height; i++) {
            idat.writeByte(0); // Filter type
            for (int j = 0; j < width; j++) {
                int argbPixel = rgbData[index++];
                if (colorType == COLOR_TYPE_RGB_WITH_ALPHA) {
                    argbPixel = Colors.unpremultiply(argbPixel);
                    idat.writeByte(((argbPixel >> 16) & 0xff)); // red
                    idat.writeByte(((argbPixel >> 8) & 0xff)); // green
                    idat.writeByte((argbPixel & 0xff)); // blue
                    idat.writeByte((argbPixel >>> 24)); // alpha
                }
                else {
                    idat.writeByte(((argbPixel >> 16) & 0xff)); // red
                    idat.writeByte(((argbPixel >> 8) & 0xff)); // green
                    idat.writeByte((argbPixel & 0xff)); // blue
                }
            }
        }
        idat.compress();
        
        // Create IEND chunk data
        ByteArray iend = new ByteArray(0);
        
        // Write PNG file
        ByteArray out = new ByteArray(8 + ihdr.length() + 12 + idat.length() + 12 + 
            iend.length() + 12);
        out.writeLong(SIGNATURE);
        writeChunk(out, CHUNK_IHDR, ihdr);
        writeChunk(out, CHUNK_IDAT, idat);
        writeChunk(out, CHUNK_IEND, iend);
        
        return out;
    }
   
    
    private static void writeChunk(ByteArray out, int chunkType, ByteArray data) { 
        int len = data.length();
        out.writeInt(len);
        out.writeInt(chunkType);
        out.write(data.getData(), 0, len);
        out.writeInt(getCRC(out.getData(), out.position() - len - 4, len + 4));
    }
    
}
