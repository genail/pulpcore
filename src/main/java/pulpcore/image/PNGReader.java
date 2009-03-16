/*
    Copyright (c) 2007, Interactive Pulp, LLC
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

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.util.ByteArray;

/**
    Loads a PNG file. 
    <p>
    This PNG decoder can only read a subset of the PNG spec.
    Only are the following color formats are supported: 
    <ul>
    <li>8-bit grayscale (ignores single-color transparency)</li>
    <li>8-bit grayscale with full alpha</li>
    <li>8-bit RGB (ignores no single-color transparency)</li>
    <li>8-bit RGB with full alpha</li>
    <li>8-bit palette</li>
    <li>8-bit palette with transparency</li>
    <li>4-bit palette</li>
    <li>4-bit palette with transparency</li>
    </ul>
    Additionally, this PNG decoder ignores color space information and suggested palettes, 
    and it cannot read interlaced PNGs or PNGs with multiple IDAT chunks.
    <p>
    Use the assettools image converter to ensure images are compliant with this decoder.
    <p>
    This class is package-private - use CoreImage.load() or CoreFont.load() instead. 
*/
class PNGReader {
    
    private static final long PNG_SIGNATURE = 0x89504e470d0a1a0aL;
    
    private static final String PNG_ERROR_MESSAGE = "Error reading PNG file";
    private static final String ZLIB_ERROR_MESSAGE = "Invalid ZLIB data";

    private static final int CHUNK_IHDR = 0x49484452; // "IHDR"
    private static final int CHUNK_PLTE = 0x504c5445; // "PLTE"
    private static final int CHUNK_TRNS = 0x74524e53; // "tRNS"
    private static final int CHUNK_IDAT = 0x49444154; // "IDAT"
    private static final int CHUNK_IEND = 0x49454e44; // "IEND"
    
    /** Private ancillary chunk, "foNT", for PulpCore font information. */ 
    private static final int CHUNK_FONT = 0x666f4e74;
    
    /** Private ancillary chunk, "hoTS", for PulpCore hot spot information. */
    private static final int CHUNK_HOTS = 0x686f5473;
    
    /** Private ancillary chunk "anIM", for PulpCore animation information. */
    private static final int CHUNK_ANIM = 0x616e496d;
    
    private static final byte COLOR_TYPE_GRAYSCALE = 0;
    private static final byte COLOR_TYPE_RGB = 2;
    private static final byte COLOR_TYPE_PALETTE = 3;
    private static final byte COLOR_TYPE_GRAYSCALE_WITH_ALPHA = 4;
    private static final byte COLOR_TYPE_RGB_WITH_ALPHA = 6;
    private static final int[] SAMPLES_PER_PIXEL = { 1, 0, 3, 1, 2, 0, 4 };
    
    private CoreImage image;
    private int bitDepth;
    private int colorType;
    private int[] palette;
    
    
    public CoreImage read(ByteArray in, CoreFont font) throws IOException {
        
        long sig = in.readLong();
        if (sig != PNG_SIGNATURE) {
            if (Build.DEBUG) CoreSystem.print("Bad PNG signature: 0x" + Long.toString(sig, 16));
            throw new IOException(PNG_ERROR_MESSAGE);
        }
        
        while (true) {
            int length = in.readInt();
            int chunkType = in.readInt();
            
            if (chunkType == CHUNK_IHDR) {
                readHeader(in);
            }
            else if (chunkType == CHUNK_HOTS) {
                image.setHotspot(in.readInt(), in.readInt());
            }
            else if (chunkType == CHUNK_PLTE) {
                readPalette(in, length);
            }
            else if (chunkType == CHUNK_TRNS) {
                readTransparency(in, length);
            }
            else if (chunkType == CHUNK_IDAT) {
                readData(in, length);
            }
            else if (chunkType == CHUNK_ANIM) {
                readAnimation(in);
            }
            else if (chunkType == CHUNK_FONT && font != null) {
                font.set(image, in);
            }
            else if (chunkType != CHUNK_IEND) {
                if (Build.DEBUG) {
                    CoreSystem.print("Ignoring PNG chunk: " + chunkTypeToString(chunkType));
                }
                in.setPosition(in.position() + length);
            }
            
            int crc = in.readInt();
            
            if (chunkType == CHUNK_IEND) {
                break;
            }
        }
        
        return image;
    }
    
    
    private void readHeader(ByteArray in) throws IOException {
        int width = in.readInt();
        int height = in.readInt();
        bitDepth = in.readByte();
        colorType = in.readByte();
        int compressionMethod = in.readByte();
        int filterMethod = in.readByte();
        int interlaceMethod = in.readByte();
        boolean supportedBitDepth = 
            (bitDepth == 8 || (bitDepth == 4 && colorType == COLOR_TYPE_PALETTE));
            
        if (Build.DEBUG) {
            // More detailed error messages in DEBUG mode
            if (compressionMethod != 0) {
                throw new IOException("Invalid compression method: " + compressionMethod);
            }
            else if (filterMethod != 0) {
                throw new IOException("Invalid filter method: " + filterMethod);
            }
            else if (interlaceMethod != 0) {
                throw new IOException("Unsupported interlace method: " + interlaceMethod);
            }
            else if (!supportedBitDepth) {
                throw new IOException("Unsupported bit depth: " + bitDepth + " (color type: " +
                    colorType + ")");
            }
            else if (colorType != COLOR_TYPE_GRAYSCALE && 
                colorType != COLOR_TYPE_RGB && 
                colorType != COLOR_TYPE_PALETTE && 
                colorType != COLOR_TYPE_GRAYSCALE_WITH_ALPHA && 
                colorType != COLOR_TYPE_RGB_WITH_ALPHA)
            {
                throw new IOException("Invalid color type: " + colorType);
            }
        }
        else {
            if (compressionMethod != 0 || filterMethod != 0 || interlaceMethod != 0 ||
                !supportedBitDepth || (
                colorType != COLOR_TYPE_GRAYSCALE && 
                colorType != COLOR_TYPE_RGB && 
                colorType != COLOR_TYPE_PALETTE && 
                colorType != COLOR_TYPE_GRAYSCALE_WITH_ALPHA && 
                colorType != COLOR_TYPE_RGB_WITH_ALPHA))
            {
                throw new IOException(PNG_ERROR_MESSAGE);
            }
        }
        
        boolean isOpaque = true;
        if (colorType == COLOR_TYPE_GRAYSCALE_WITH_ALPHA || 
            colorType == COLOR_TYPE_RGB_WITH_ALPHA)
        {
            isOpaque = false;
        }
        
        image = new CoreImage(width, height, isOpaque);
    }
    
    
    private void readPalette(ByteArray in, int length) throws IOException {
        
        palette = new int[length/3];
        
        if (palette.length * 3 != length) {
            if (Build.DEBUG) {
                throw new IOException("Invalid palette length: " + length);
            }
            else {
                throw new IOException(PNG_ERROR_MESSAGE);
            }
        }
        
        for (int i = 0; i < palette.length; i++) {
            int a = 0xff;
            int r = in.readByte() & 0xff;
            int g = in.readByte() & 0xff;
            int b = in.readByte() & 0xff;
            
            palette[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    
    
    private void readTransparency(ByteArray in, int length) throws IOException {
        if (Build.DEBUG) {
            if (palette == null) {
                throw new IOException("Transparency unsupported without a palette. " + 
                    "Color type = " + colorType);
            }
            
            if (length > palette.length) {
                throw new IOException("Invalid transparency length: " + length);
            }
        }
        else if (palette == null || length > palette.length) {
            throw new IOException(PNG_ERROR_MESSAGE);
        }
        
        for (int i = 0; i < length; i++) {
            int a = in.readByte() & 0xff;
            if (a < 0xff) {
                image.setOpaque(false);
            }
            palette[i] = Colors.premultiply((a << 24) | (palette[i] & 0xffffff));
        }
    }
    
    
    private void readAnimation(ByteArray in) throws IOException {
        int numFramesAcross = in.readShort();
        int numFramesDown = in.readShort();
        boolean loop = in.readBoolean();
        int frames = in.readShort();
        int[] frameSequence = new int[frames];
        int[] frameDuration = new int[frames];
        
        for (int i = 0; i < frames; i++) {
            frameSequence[i] = in.readShort();
        }
        
        for (int i = 0; i < frames; i++) {
            frameDuration[i] = in.readShort();
        }
        
        AnimatedImage animImage = new AnimatedImage(image, numFramesAcross, numFramesDown);
        animImage.setSequence(frameSequence, frameDuration, loop);
        
        image = animImage;
    }
    
    
    private void readData(ByteArray in, int length) throws IOException {
        
        Inflater inflater = new Inflater();
        inflater.setInput(in.getData(), in.position(), length);
        
        int bitsPerPixel = bitDepth * SAMPLES_PER_PIXEL[colorType];
        int width = image.getWidth();
        int height = image.getHeight();
        int[] dataARGB = image.getData();
        int bytesPerPixel = (bitsPerPixel + 7) / 8;
        int bytesPerScanline = (width * bitsPerPixel + 7) / 8;
        byte[] prevScanline = new byte[bytesPerScanline];
        byte[] currScanline = new byte[bytesPerScanline];
        byte[] filterBuffer = new byte[1];
        int index = 0;
        
        for (int i = 0; i < height; i++) {
            inflateFully(inflater, filterBuffer);
            inflateFully(inflater, currScanline);
            int filter = filterBuffer[0];
            
            // Apply filter
            if (filter > 0 && filter < 5) {
                decodeFilter(currScanline, prevScanline, filter, bytesPerPixel);
            }
            else if (filter != 0) {
                if (Build.DEBUG) {
                    throw new IOException("Illegal filter type: " + filter);
                }
                else {
                    throw new IOException(PNG_ERROR_MESSAGE);
                }
            }
            
            // Convert bytes into ARGB pixels
            int srcIndex = 0;
            switch (colorType) {
                default: case COLOR_TYPE_GRAYSCALE:
                    for (int j = 0; j < width; j++) {
                        int v = currScanline[j] & 0xff;
                        dataARGB[index++] = (0xff << 24) | (v << 16) | (v << 8) | v;
                    }
                    break;
                    
                case COLOR_TYPE_RGB:
                    for (int j = 0; j < width; j++) {
                        int r = currScanline[srcIndex++] & 0xff;
                        int g = currScanline[srcIndex++] & 0xff;
                        int b = currScanline[srcIndex++] & 0xff;
                        dataARGB[index++] = (0xff << 24) | (r << 16) | (g << 8) | b;
                    }
                    break;
                    
                case COLOR_TYPE_PALETTE:
                    if (bitDepth == 8) {
                        for (int j = 0; j < width; j++) {
                            dataARGB[index++] = palette[currScanline[j] & 0xff];
                        }
                    }
                    else {
                        // Assume bitDepth == 4
                        boolean isOdd = (width & 1) == 1;
                        int s = width & ~1;
                        for (int j = 0; j < s; j+=2) {
                            int b = currScanline[srcIndex++] & 0xff;
                            dataARGB[index++] = palette[b >> 4];
                            dataARGB[index++] = palette[b & 0x0f];
                        }
                        if (isOdd) {
                            int b = currScanline[srcIndex++] & 0xff;
                            dataARGB[index++] = palette[b >> 4];
                        }
                    }
                    break;
                    
                case COLOR_TYPE_GRAYSCALE_WITH_ALPHA:
                    for (int j = 0; j < width; j++) {
                        int v = currScanline[srcIndex++] & 0xff;
                        int a = currScanline[srcIndex++] & 0xff;
                        dataARGB[index++] = (a << 24) | (v << 16) | (v << 8) | v;
                    }
                    break;
                    
                case COLOR_TYPE_RGB_WITH_ALPHA:
                    for (int j = 0; j < width; j++) {
                        int r = currScanline[srcIndex++] & 0xff;
                        int g = currScanline[srcIndex++] & 0xff;
                        int b = currScanline[srcIndex++] & 0xff;
                        int a = currScanline[srcIndex++] & 0xff;
                        dataARGB[index++] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    break;                
            }
            
            // Swap curr and prev scanlines
            byte[] temp = currScanline;
            currScanline = prevScanline;
            prevScanline = temp;
        }
        
        if (colorType == COLOR_TYPE_GRAYSCALE_WITH_ALPHA || 
            colorType == COLOR_TYPE_RGB_WITH_ALPHA)
        {
            for (int i = 0; i < dataARGB.length; i++) {
                dataARGB[i] = Colors.premultiply(dataARGB[i]);
            }
        }
        
        inflater.end();
        in.setPosition(in.position() + length);
    }
    
   
    private void inflateFully(Inflater inflater, byte[] result) throws IOException {
        int bytesRead = 0;
        
        while (bytesRead < result.length) {
            if (inflater.needsInput()) {
                throw new IOException(ZLIB_ERROR_MESSAGE);
            }
            
            try {
                bytesRead += inflater.inflate(result, bytesRead, result.length - bytesRead);
            }
            catch (DataFormatException ex) {
                throw new IOException(ZLIB_ERROR_MESSAGE);
            }
        }
    }
    
    
    private void decodeFilter(byte[] curr, byte[] prev, int filter, int bpp) {
        int length = curr.length;
        
        if (filter == 1) {
            // Input = Sub
            // Raw(x) = Sub(x) + Raw(x-bpp)
            // For all x < 0, assume Raw(x) = 0.
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] + curr[i - bpp]);
            }
        }
        else if (filter == 2) {
            // Input = Up
            // Raw(x) = Up(x) + Prior(x)
            for (int i = 0; i < length; i++) {
                curr[i] = (byte)(curr[i] + prev[i]);
            }
        }
        else if (filter == 3) {
            // Input = Average
            // Raw(x) = Average(x) + floor((Raw(x-bpp)+Prior(x))/2)
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte)(curr[i] + ((prev[i] & 0xff) >> 1));
            }
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] + (((curr[i - bpp] & 0xff) + (prev[i] & 0xff)) >> 1));
            }
        }
        else if (filter == 4) {
            // Input = Paeth
            // Raw(x) = Paeth(x) + PaethPredictor(Raw(x-bpp), Prior(x), Prior(x-bpp))
            for (int i = 0; i < bpp; i++) {
                curr[i] = (byte)(curr[i] + prev[i]);
            }
            for (int i = bpp; i < length; i++) {
                curr[i] = (byte)(curr[i] + 
                    paethPredictor(curr[i - bpp] & 0xff, prev[i] & 0xff, prev[i - bpp] & 0xff));
            }
        }
    }
    
    
    // a = left, b = above, c = upper left
    private int paethPredictor(int a, int b, int c) {
        
        // Initial estimate
        int p = a + b - c;
        
        // Distances to a, b, c
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
    
        // Return nearest of a,b,c, breaking ties in order a,b,c.
        if (pa <= pb && pa <= pc) {
            return a;
        }
        else if (pb <= pc) {
            return b;
        }
        else {
            return c;
        }
    }
    
    
    // 
    // Debug only
    //
    
    
    private String chunkTypeToString(int chunkType) {
        
        char ch1 = (char)(chunkType >>> 24);
        char ch2 = (char)((chunkType >> 16) & 0xff);
        char ch3 = (char)((chunkType >> 8) & 0xff);
        char ch4 = (char)(chunkType & 0xff);
        
        return "" + ch1 + ch2 + ch3 + ch4;
    }
}
