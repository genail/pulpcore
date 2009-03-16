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

import pulpcore.Build;
import pulpcore.CoreSystem;


/**
    Base64 encoding and decoding for network transport. This class follows
    the spec in <a href="http://www.faqs.org/rfcs/rfc4648.html">RFC 4648</a>.
*/
public class Base64 {
    
    private static final char PADDING = '=';
    
    private static final String STANDARD_CHARS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URLSAFE_CHARS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        
    private static final char[] STANDARD_ENCODE_MAP = STANDARD_CHARS.toCharArray();
    private static final char[] URLSAFE_ENCODE_MAP = URLSAFE_CHARS.toCharArray();
    
    private static final int[] STANDARD_DECODE_MAP = new int[128];
    private static final int[] URLSAFE_DECODE_MAP = new int[128];
    
    static {
        for (int i = 0; i < STANDARD_DECODE_MAP.length; i++) {
            STANDARD_DECODE_MAP[i] = STANDARD_CHARS.indexOf((char)i);
            URLSAFE_DECODE_MAP[i] = URLSAFE_CHARS.indexOf((char)i);
        }
    }
    
    // Prevent instantiation
    private Base64() { }
    
    
    /**
        Encodes binary data using the standard Base64 alphabet. The padding character
        ("=") is used.
    */
    public static String encode(byte[] data) {
        return encode(data, STANDARD_ENCODE_MAP, true);
    }
    
    
    /**
        Encodes binary data using the standard Base64 alphabet. The padding character
        ("=") is optionally used.
    */
    public static String encode(byte[] data, boolean addPadding) {
        return encode(data, STANDARD_ENCODE_MAP, addPadding);
    }
    
    
    /**
        Decodes a Base64 string using the standard Base64 alphabet. The padding character
        ("=") is not required.
        @return the decoded string, or null if data is null, or null if
        the string is not a Base64 string.
    */
    public static byte[] decode(String data) {
        return decode(data, STANDARD_DECODE_MAP);
    }
    
    
    /**
        Encodes binary data using the URL-safe Base64 alphabet. The padding character
        ("=") is used.
    */
    public static String encodeURLSafe(byte[] data) {
        return encode(data, URLSAFE_ENCODE_MAP, false);
    }
    
    
    /**
        Encodes binary data using the URL-safe Base64 alphabet. The padding character
        ("=") is optionally used.
    */
    public static String encodeURLSafe(byte[] data, boolean addPadding) {
        return encode(data, URLSAFE_ENCODE_MAP, addPadding);
    }
    
    
    /**
        Decodes a Base64 string using the URL-safe Base64 alphabet. The padding character
        ("=") is not required.
        @return the decoded string, or null if data is null, or null if
        the string is not a Base64 string.
    */
    public static byte[] decodeURLSafe(String data) {
        return decode(data, URLSAFE_DECODE_MAP);
    }
    
    
    private static String encode(byte[] srcData, char[] encodeMap, boolean pad) {
        int srcLength = srcData.length;
        int destLength = (srcLength * 4 + 2) / 3;
        int padAmount = pad ? ((4 - destLength) & 3) : 0;
        char[] destData = new char[destLength + padAmount];
        int srcIndex = 0;
        int destIndex = 0;
        
        while (srcIndex < srcLength) {
            // Get 3 bytes
            int src0 = srcData[srcIndex++] & 0xff;
            int src1 = (srcIndex < srcLength) ? srcData[srcIndex++] & 0xff : 0;
            int src2 = (srcIndex < srcLength) ? srcData[srcIndex++] & 0xff : 0;
            
            // Convert to 4 chars
            destData[destIndex++] = encodeMap[src0 >> 2];
            destData[destIndex++] = encodeMap[((src0 & 3) << 4) | (src1 >> 4)];
            if (destIndex < destLength) {
                destData[destIndex++] = encodeMap[((src1 & 0xf) << 2) | (src2 >> 6)];
            }
            if (destIndex < destLength) {
                destData[destIndex++] = encodeMap[src2 & 0x3f];
            }
        }
        
        for (int i = 0; i < padAmount; i++) {
            destData[destIndex++] = PADDING;
        }
            
        return new String(destData);
    }
    
    
    private static int decode(char ch, int[] decodeMap) {
        if (ch < 0 || ch >= decodeMap.length) {
            return -1;
        }
        else {
            return decodeMap[ch];
        }
    }
    
    
    private static byte[] decode(String srcData, int[] decodeMap) {
        if (srcData == null) {
            return null;
        }
        
        // Remove all carriage returns, spaces
        srcData = StringUtil.replace(srcData, "\n", "");
        srcData = StringUtil.replace(srcData, "\r", "");
        srcData = StringUtil.replace(srcData, " ", "");
        
        // Ignore padding at end
        int srcLength = srcData.length();
        while (srcLength > 0 && srcData.charAt(srcLength - 1) == PADDING) {
            srcLength--;
        }
        
        int destLength = srcLength * 3 / 4;
        byte[] destData = new byte[destLength];
        int srcIndex = 0;
        int destIndex = 0;
        
        while (srcIndex < srcLength && destIndex < destLength) {
            // Get 4 chars
            int src0 = decode(srcData.charAt(srcIndex++), decodeMap);
            int src1 = (srcIndex < srcLength) ? decode(srcData.charAt(srcIndex++), decodeMap) : 0;
            int src2 = (srcIndex < srcLength) ? decode(srcData.charAt(srcIndex++), decodeMap) : 0;
            int src3 = (srcIndex < srcLength) ? decode(srcData.charAt(srcIndex++), decodeMap) : 0;
            
            if (src0 < 0 || src1 < 0 || src2 < 0 || src3 < 0) {
                if (Build.DEBUG) CoreSystem.print("Base64 decoder: Illegal char");
                return null;
            }
            
            // Convert to 3 bytes
            destData[destIndex++] = (byte)((src0 << 2) | (src1 >> 4));
            if (destIndex < destLength) {
                destData[destIndex++] = (byte)(((src1 & 0xf) << 4) | (src2 >> 2));
            }
            if (destIndex < destLength) {
                destData[destIndex++] = (byte)(((src2 & 3) << 6) | src3);
            }
        }
        return destData;
    }
}
