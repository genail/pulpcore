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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.util.ByteArray;

/**
    The CoreFont class is a image-based font used for drawing text.
*/
public class CoreFont {
    
    private static final long MAGIC = 0x70756c70666e740bL; // "pulpfnt" 0.11
    
    // HashMap<String, WeakReference<CoreFont>>
    private static HashMap loadedFonts = new HashMap();
    private static CoreFont systemFont;
    
    private CoreImage image;
    
    private char firstChar;
    private char lastChar;
    /* package-private */ int[] charPositions;
    private boolean uppercaseOnly;
    /** Extra horizontal pixel space between chars. Can be negative. */
    private int tracking;
    private int[] bearingLeft;
    private int[] bearingRight;
    
    // HashMap<Integer, SoftReference<CoreFont>>
    private HashMap tintedFonts;
    
    public static CoreFont getSystemFont() {
        if (systemFont == null) {
            systemFont = load("system.font.png");
        }
        
        return systemFont;
    }
    
    /**
        Loads a PNG-based image font from the asset catalog.
        <p>
        This method never returns {@code null}.
        If the image is not found, or the PNG image does not have font information, 
        an error is printed to the log
        and the system font is returned. If the system font cannot be found, an Error is thrown.
        <p>
        PNG-based image fonts are created from a font.properties file. See the Text example
        in the distribution archive.
        <p>
        Fonts are internally cached (using a WeakReference), and if the font was previously 
        loaded, this method may return the same reference.
        @param fontAsset The name of a PNG image file with font information.
        @return The font, or a broken image if the font cannot be found.
    */
    public static CoreFont load(String fontAsset) {
        
        // Attempt to load from the cache
        WeakReference fontRef = (WeakReference)loadedFonts.get(fontAsset);
        if (fontRef != null) {
            CoreFont font = (CoreFont)fontRef.get();
            if (font != null) {
                return font;
            }
            else {
                loadedFonts.remove(fontAsset);
            }
        }
        
        CoreFont font = new CoreFont();
        CoreImage.load(fontAsset, font);
        
        if (font.image != null) {
            loadedFonts.put(fontAsset, new WeakReference(font));
            return font;
        }
        else if ("system.font.png".equals(fontAsset)) {
            if (Build.DEBUG) CoreSystem.print("Can't load system.font.png. Quitting");
            throw new Error();
        }
        else {
            return getSystemFont();
        }
    }
    
    private CoreFont() { }
     
    private CoreFont(CoreFont font) {
        this.image = font.image;
        this.firstChar = font.firstChar;
        this.lastChar = font.lastChar;
        this.charPositions = font.charPositions;
        this.bearingLeft = font.bearingLeft;
        this.bearingRight = font.bearingRight;
        this.uppercaseOnly = font.uppercaseOnly;
        this.tracking = font.tracking;
    }
    
    // Callback from CoreImage.load()
    void set(CoreImage image, ByteArray in) throws IOException {
        
        long magic = in.readLong();
            
        if (magic != MAGIC) {
            if (Build.DEBUG) {
                CoreSystem.print("CoreFont.set() - Bad magic number (try recompiling game assets)");
            }
            throw new IOException();
        }
        
        firstChar = (char)in.readShort();
        lastChar = (char)in.readShort();
        tracking = in.readShort();
        boolean hasBearing = in.readBoolean();
        
        int numChars = lastChar - firstChar + 1;
        charPositions = new int[numChars + 1];
        bearingLeft = new int[numChars];
        bearingRight = new int[numChars];
        
        for (int i=0; i<charPositions.length; i++) {
            charPositions[i] = in.readShort() & 0xffff;
        }
        
        if (hasBearing) {
            for (int i = 0; i < bearingLeft.length; i++) {
                bearingLeft[i] = in.readShort();
                bearingRight[i] = in.readShort();
            }
        }
            
        uppercaseOnly = (lastChar < 'a');
        
        this.image = image;
    }
    
    public boolean canDisplay(char ch) {
        if (uppercaseOnly && ch >= 'a' && ch <= 'z') {
            ch += 'A' - 'a';
        }
        if (ch < firstChar || ch > lastChar) {
            return false;
        }
        
        int index = ch - firstChar;
        int charWidth = charPositions[index+1] - charPositions[index];
        return (charWidth > 0);
    }
    
    /**
        Gets the total width of all the characters in a string. Tracking between 
        characters is included.
        If a character isn't valid for this font, the last character in the set is used.
        <p>
        Equivalent to calling getStringWidth(s, 0, s.length())
    */
    public int getStringWidth(String s) {
        return getStringWidth(s, 0, s.length());
    }
    
    /**
        Gets the total width of a range of characters in a string. Tracking 
        and kerning between characters is included.
        If a character isn't valid for this font, the last character in the set is used.
        @param beginIndex the beginning index, inclusive.
        @param endIndex the ending index, exclusive.
    */
    public int getStringWidth(String s, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return 0;
        }
        int stringWidth = 0;
        
        int lastIndex = -1;
        for (int i = beginIndex; i < endIndex; i++) {
            int index = getCharIndex(s.charAt(i));
            int charWidth = charPositions[index+1] - charPositions[index];
            
            if (lastIndex != -1) {
                stringWidth += getKerning(lastIndex, index);
            }
            stringWidth += charWidth;
            lastIndex = index;
        }
        return stringWidth;
    }
    
    public int getCharPosition(String s, int beginIndex, int charIndex) {
        if (charIndex <= beginIndex) {
            return 0;
        }
        return getStringWidth(s, beginIndex, charIndex + 1) - getCharWidth(s.charAt(charIndex));
    }
    
    /* package-private */ int getCharIndex(char ch) {
        if (uppercaseOnly && ch >= 'a' && ch <= 'z') {
            ch += 'A' - 'a';
        }
        if (ch < firstChar || ch > lastChar) {
            ch = lastChar;
        }
        return ch - firstChar;
    }
    
    /**
        Gets the width of the specified character. Tracking and kerning is not included.
        If a character isn't valid for this font, the last character in the set is used.
    */
    public int getCharWidth(char ch) {
        int index = getCharIndex(ch);
        return (charPositions[index+1] - charPositions[index]);
    }
    
    public int getKerning(char left, char right) {
        return getKerning(getCharIndex(left), getCharIndex(right));
    }
    
    /* package-private */ int getKerning(int leftIndex, int rightIndex) {
        // Future versions of this method might handle kerning pairs, like "WA" and "Yo"
        if (tracking != 0 && (shouldIgnoreTracking(rightIndex) || 
            shouldIgnoreTracking(leftIndex))) 
        {
            return bearingRight[leftIndex] + bearingLeft[rightIndex];
        }
        else {
            return bearingRight[leftIndex] + tracking + bearingLeft[rightIndex];
        }
    }
    
    public int getHeight() {
        return image.getHeight();
    }
    
    public CoreImage getImage() {
        return image;
    }
    
    private boolean shouldIgnoreTracking(int index) {
        int width = (charPositions[index+1] - charPositions[index]);
        int lsb = bearingLeft[index];
        int rsb = bearingRight[index];
        int advance = width + lsb + rsb;
        return advance < width/2;
    }
    
    //
    //
    //
    
    /**
        Returns a new CoreFont with every pixel set to the specified color,
        without changing the alpha of each pixel. 
        <p>
        Tinted fonts are internally cached using SoftReferences.
    */
    public CoreFont tint(int rgbColor) {
        
        Integer color = new Integer(rgbColor);
        
        if (tintedFonts == null) {
            tintedFonts = new HashMap();
        }
        else {
            // Attempt to load from the cache
            SoftReference fontRef = (SoftReference)tintedFonts.get(color);
            if (fontRef != null) {
                CoreFont font = (CoreFont)fontRef.get();
                if (font != null) {
                    return font;
                }
                else {
                    tintedFonts.remove(color);
                }
            }
        }
        
        // Create a new tinted font
        CoreFont tintedFont = new CoreFont(this);
        tintedFont.image = image.tint(rgbColor);
        tintedFonts.put(color, new SoftReference(tintedFont));
        return tintedFont;
    }
   
    /**
        @deprecated Background colors do not appear correctly
    */
    public CoreFont background(int argbColor) {
        CoreFont newFont = new CoreFont(this);
        newFont.image = image.background(argbColor);
        return newFont;
    }
    
    /**
        @deprecated Fade dynamically using a Label instead
    */
    public CoreFont fade(int alpha) {
        CoreFont fadedFont = new CoreFont(this);
        fadedFont.image = image.fade(alpha);
        return fadedFont;
    }
    
    /**
        Creates a scaled instance of this font.
        @deprecated Scale dynamically using a Label instead
    */
    public CoreFont scale(double scale) {
        
        int numChars = lastChar - firstChar + 1;
        
        // Determine new char positions
        int[] scaledCharPositions = new int[charPositions.length];
        int position = 0;
        for (int i = 0; i < numChars; i++) {
            scaledCharPositions[i] = position;
            int charWidth = charPositions[i+1] - charPositions[i];
            int scaledWidth = (int)Math.round(charWidth * scale);
            position += scaledWidth;
        }
        scaledCharPositions[numChars] = position;
        
        // Scale each character image
        CoreImage scaledImage = new CoreImage(position, 
            (int)Math.round(getHeight() * scale), false);
        CoreGraphics g = scaledImage.createGraphics();
        for (int i = 0; i < numChars; i++) {
            int oldWidth = charPositions[i+1] - charPositions[i];
            int newWidth = scaledCharPositions[i+1] - scaledCharPositions[i];
            g.drawScaledImage(image, scaledCharPositions[i], 0, newWidth, scaledImage.getHeight(),
                charPositions[i], 0, oldWidth, getHeight());
        }
        
        // Scale the bearing
        int[] scaledBearingLeft = new int[bearingLeft.length];
        int[] scaledBearingRight = new int[bearingRight.length];
        for (int i = 0; i < bearingLeft.length; i++) {
            scaledBearingLeft[i] = (int)Math.round(bearingLeft[i] * scale);
            scaledBearingRight[i] = (int)Math.round(bearingRight[i] * scale);
        }
        
        // Create the new font
        CoreFont scaledFont = new CoreFont(this);
        scaledFont.charPositions = scaledCharPositions;
        scaledFont.bearingLeft = scaledBearingLeft;
        scaledFont.bearingRight = scaledBearingRight;
        scaledFont.image = scaledImage;
        scaledFont.tracking = (int)Math.round(tracking * scale);
        
        return scaledFont;
    }
}
