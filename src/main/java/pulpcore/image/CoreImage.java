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
import java.lang.ref.WeakReference;
import java.util.HashMap;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;
import pulpcore.util.ByteArray;

/**
    The CoreImage class contains raster data and provides 
    methods for creating transformed copies of the image. 
    <p>
    Methods like 
    {@link #crop(int, int, int, int)}, {@link #scale(double)}, and {@link #tint(int)} return new
    CoreImages. The image's raster data can be manipulated using the 
    {@link #createGraphics() } method.
*/
public class CoreImage {
    
    // HashMap<String, WeakReference<? extends CoreImage>>
    private static HashMap loadedImages = new HashMap();
    private static CoreImage brokenImage;
    
    private int width;
    private int height;
    private boolean isOpaque;
    private int[] data;
    
    // This might be used in the future
    //private boolean sharedRaster;
    
    /** 
        Hotspot x position. The hotspot is used by default in ImageSprite.
    */
    private int hotspotX;
    
    /** 
        Hotspot y position. The hotspot is used by default in ImageSprite.
    */
    private int hotspotY;
    
    /**
        Creates an opaque image, initially black.
    */
    public CoreImage(int width, int height) {
        this(width, height, true);
    }

    /**
        Creates a blank image. For opaque images, the initial image is black, otherwise the
        initial image is transparent.
    */
    public CoreImage(int width, int height, boolean isOpaque) {
        this(width, height, isOpaque, new int[width * height]);
        if (isOpaque) {
            for (int i = 0; i < data.length; i++) {
                data[i] = 0xff000000;
            }
        }
    }
    
    /**
        Creates a new image using the specified pixel data. The length of the 
        data must be greater than or equal to width * height.
        <p>
        The raster format is premultiplied ARGB (the same as 
        {@link java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE BufferedImage.TYPE_INT_ARGB_PRE}).
        The raster data array is assumed to be unique to this CoreImage, and not used
        in any other CoreImages.
    */
    public CoreImage(int width, int height, boolean isOpaque, int[] data) {
        this.width = width;
        this.height = height;
        this.isOpaque = isOpaque;
        this.data = data;
        //this.sharedRaster = false;
    }

    /**
        Creates a CoreImage with the same properties as the specified 
        CoreImage. The internal raster data array is shared.
    */
    public CoreImage(CoreImage image) {
        this.width = image.width;
        this.height = image.height;
        this.isOpaque = image.isOpaque;
        this.data = image.getData();
        this.hotspotX = image.hotspotX;
        this.hotspotY = image.hotspotY;
        //this.sharedRaster = true;
    }
    
    /**
        Creates a new CoreGaphics context for drawing onto this image. 
    */
    public CoreGraphics createGraphics() {
        return new CoreGraphics(this);
    }
    
    /* package-private */ final void setOpaque(boolean isOpaque) {
        this.isOpaque = isOpaque;
    }
    
    /**
        Returns true if the image is opaque.
    */
    public final boolean isOpaque() {
        return isOpaque;
    }
    
    /**
        Gets the width of the image.
    */
    public final int getWidth() {
        return width;
    }

    /**
        Gets the height of the image.
    */
    public final int getHeight() {
        return height;
    }
    
    /**
        Gets the underlying raster data array. The raster format is premultiplied ARGB (the same as 
        {@link java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE BufferedImage.TYPE_INT_ARGB_PRE}).
    */
    public final int[] getData() {
        return data;
    }
    
    /**
        Checks if the pixel at the specified location is transparent. 
        @return true if the pixel is transpent or if the location is out of bounds.
    */
    public final boolean isTransparent(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return true;
        }
        else if (isOpaque) {
            return false;
        }
        else {
            int pixel = data[x + y * width];
            return (pixel >>> 24) == 0;
        }
    }
    
    /**
        Gets the ARGB color at the specified location.
        @throws IllegalArgumentException if the specified location is out of bounds.
        @return the ARGB color in non-premultiplied format.
    */
    public final int getARGB(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException();
        }
        else {
            return Colors.unpremultiply(data[x + y * width]);
        }
    }
    
    /* package-private */ final void setData(int[] data) {
        this.data = data;
    }
    
    /**
        Sets the hotspot of the image. The hotspot a the point within the image where the 
        rendering is anchored. For photographs, the hotspot is typically the upper-left corner
        (0, 0). For cursor images, the hotspot the point that tracks the cursor's 
        position. An {@link pulpcore.sprite.ImageSprite } also uses the hotspot as the
        point around which rotation occurs.
        @see #getHotspotX()
        @see #getHotspotY()
    */
    public final void setHotspot(int x, int y) {
        hotspotX = x;
        hotspotY = y;
    }
    
    /**
        Gets the x component of the hotspot.
        @see #getHotspotY()
        @see #setHotspot(int, int)
    */
    public final int getHotspotX() {
        return hotspotX; 
    }
    
    /**
        Gets the y component of the hotspot.
        @see #getHotspotX()
        @see #setHotspot(int, int)
    */
    public final int getHotspotY() {
        return hotspotY; 
    }
    
    /**
        Does nothing by default. Subclasses can use this method for
        dynamically generated images or animations.
        @return true if the image changed since the last call to this method.
    */
    public boolean update(int elapsedTime) {
        return false;
    }
    
    /**
        Gets the "broken" image, which is the image the system may use when a specified image
        could not be loaded.
    */
    public static CoreImage getBrokenImage() {
        if (brokenImage == null) {
            CoreImage image = new CoreImage(16, 16, true);
            CoreGraphics g = image.createGraphics();
            g.setColor(Colors.WHITE);
            g.fill();
            g.setColor(Colors.BLACK);
            g.drawRect(0, 0, 16, 16);
            g.setColor(Colors.RED);
            g.drawLine(2, 2, 13, 13);
            g.drawLine(13, 2, 2, 13);
            
            brokenImage = image;
        }
        
        return brokenImage;
    }
    
    //
    // Image Reading
    //
    
    /**
        Loads a PNG or JPEG image from the asset catalog.
        If the PNG file has animation info, an {@link AnimatedImage} is returned.
        <p>
        This method never returns {@code null}.
        If the image is not found, an error is printed to the log
        and a "broken" image is returned. The broken image is similar to a red X image found in
        a web browser.
        <p>
        Images are internally cached (using a WeakReference), and if the image was previously 
        loaded, this method may return an image with the same internal raster data.
        @param imageAsset The name of a PNG or JPEG image file.
        @return The image, (either a CoreImage or an AnimatedImage) 
        or a broken image if the image cannot be found.
    */
    public static CoreImage load(String imageAsset) {
        return load(imageAsset, null);
    }
    
    static CoreImage load(String imageAsset, CoreFont font) {
        
        //if (Build.DEBUG) CoreSystem.print("Loading: " + imageAsset);
        
        // Attempt to load from the cache
        WeakReference imageRef = (WeakReference)loadedImages.get(imageAsset);
        if (imageRef != null) {
            CoreImage image = (CoreImage)imageRef.get();
            if (image != null) {
                if (image instanceof AnimatedImage) {
                    // Create a new copy that has its own timeline
                    // (The raster data is shared)
                    return new AnimatedImage((AnimatedImage)image);
                }
                else {
                    // Create a new copy (The raster data is shared)
                    return new CoreImage(image);
                }
            }
            else {
                loadedImages.remove(imageAsset);
            }
        }
        
        // Attempt to load raw bytes from the asset collection
        ByteArray in = Assets.get(imageAsset);
        if (in == null) {
            return getBrokenImage();
        }
        
        CoreImage image = null;
        // Try the internal image loader
        if (imageAsset.toLowerCase().endsWith(".png")) {
            try {
                PNGReader pngReader = new PNGReader();
                image = pngReader.read(in, font);
            }
            catch (IOException ex) {
                if (Build.DEBUG) CoreSystem.print("Error loading image: " + imageAsset, ex);
            }
        }
        
        // Try again with the system image loader
        if (image == null) {
            in.reset();
            image = CoreSystem.getThisAppContext().loadImage(in);
            if (image == null) {
                if (Build.DEBUG) CoreSystem.print("Could not load image: " + imageAsset);
                return getBrokenImage();
            }
        }
        
        loadedImages.put(imageAsset, new WeakReference(image));
        
        return image;
    }
    
    //
    // Image transforms
    //
    
    /**
        Splits the image into several tiles.
    */
    public CoreImage[] split(int framesAcross) {
        return split(framesAcross, 1);
    }
    
    /**
        Splits the image into several tiles.
    */
    public CoreImage[] split(int framesAcross, int framesDown) {
        
        int numFrames = framesAcross * framesDown;
        int w = width / framesAcross;
        int h = height / framesDown;
        
        CoreImage[] frames = new CoreImage[numFrames];
        
        for (int i = 0; i < numFrames; i++) {
            int x = (i % framesAcross) * w;
            int y = (i / framesAcross) * h;
            
            frames[i] = crop(x, y, w, h);
        }
        
        return frames;
    }
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The borderSize parameter can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int borderSize, int argbColor) {
        return expandCanvas(borderSize, borderSize, borderSize, borderSize, argbColor);
    }
    
    /**
        Create a new image with an expanded canvas size. The borders are filled with the 
        specified color. The top, right, bottom, left parameters can be negative.
        The hotspot is translated accordingly.
    */
    public CoreImage expandCanvas(int top, int right, int bottom, int left, int argbColor) {
        int alpha = argbColor >>> 24;
        boolean isOpaque = this.isOpaque && alpha == 0xff;
        int newWidth = width + left + right;
        int newHeight = height + top + bottom;
        
        argbColor = Colors.premultiply(argbColor);

        CoreImage newImage = new CoreImage(newWidth, newHeight, isOpaque);
        newImage.setHotspot(hotspotX + left, hotspotY + top);
        if (argbColor != 0) {
            int[] destData = newImage.getData();
            for (int i = 0; i < destData.length; i++) {
                destData[i] = argbColor;
            }
        }
        
        CoreGraphics g = newImage.createGraphics();
        g.drawImage(this, left, top);
        return newImage;
    }
    
    /**
        Creates a cropped version of this image. The raster data is not shared.
        The hotspot is copied as-is, with no translation.
    */
    public CoreImage crop(int x, int y, int w, int h) {

        if (x < 0) {
            w += x;
            x = 0;
        }
        if (y < 0) {
            h += y;
            y = 0;
        }
        if (x + w > getWidth()) {
            w = Math.max(0, getWidth() - x);
        }
        if (y + h > getHeight()) {
            h = Math.max(0, getHeight() - y);
        }
        
        CoreImage croppedImage = new CoreImage(w, h, isOpaque);
        croppedImage.setHotspot(hotspotX, hotspotY);
        
        int[] srcData = data;
        int srcOffset = x + y * width;
        int[] destData = croppedImage.getData();
        int destOffset = 0;
        
        for (int i = 0; i < h; i++) {
            System.arraycopy(srcData, srcOffset, destData, destOffset, w);
            srcOffset += width;
            destOffset += w;
        }
        
        return croppedImage;
    }

    /**
        Creates a rotated version of this image. 
        Same as calling rotate(angle, true);
        @param angle an angle, typically in the range from 0 to Math.PI * 2
    */
    public CoreImage rotate(double angle) {
        return rotate(CoreMath.toFixed(angle), true);
    }
    
    /**
        Creates a rotated version of this image.
        @param sizeAsNeeded if true, the resulting image is sized to contain 
        the entire rotated image. If false, the resulting image is the same 
        size as this image. The hotspot is rotated accordingly.
        @param angle an angle, typically in the range from 0 to Math.PI * 2
    */
    public CoreImage rotate(double angle, boolean sizeAsNeeded) {
        return rotate(CoreMath.toFixed(angle), sizeAsNeeded);
    }

    /**
        Creates a rotated version of this image. 
        Same as calling rotate(angle, true);
        @param angle a fixed-point angle, typically in the range from 0 to 
            CoreMath.TWO_PI.
    */
    public CoreImage rotate(int angle) {
        return rotate(angle, true);
    }
    
    /**
        Creates a rotated version of this image.
        @param sizeAsNeeded if true, the resulting image is sized to contain 
        the entire rotated image. If false, the resulting image is the same 
        size as this image. The hotspot is rotated accordingly.
        @param angle a fixed-point angle, typically in the range from 0 to 
            CoreMath.TWO_PI.
    */
    public CoreImage rotate(int angle, boolean sizeAsNeeded) {
        
        int newWidth = width;
        int newHeight = height;
        
        int cos = CoreMath.cos(angle);
        int sin = CoreMath.sin(angle);
        
        if (sizeAsNeeded) {
            newWidth = CoreMath.toIntCeil(Math.abs(width * cos) + Math.abs(height * sin));
            newHeight = CoreMath.toIntCeil(Math.abs(width * sin) + Math.abs(height * cos));
        }
        CoreImage rotatedImage = new CoreImage(newWidth, newHeight, isOpaque);
        
        int x = hotspotX - width/2;
        int y = hotspotY - height/2;
        rotatedImage.setHotspot(
            CoreMath.toIntRound(x * cos - y * sin) + newWidth / 2,
            CoreMath.toIntRound(x * sin + y * cos) + newHeight / 2);
        
        CoreGraphics g = rotatedImage.createGraphics();
        g.drawRotatedImage(this, (newWidth - width) / 2, (newHeight - height) / 2, 
            width, height, angle);
        
        return rotatedImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents a scaled version 
        of this image. The hotspot is scaled accordingly.
    */
    public CoreImage scale(double scale) {
        return scale((int)Math.round(scale * width), (int)Math.round(scale * height));
    }
    
    /**
        Returns a new CoreImage whose raster data represents a scaled version 
        of this image. The hotspot is scaled accordingly.
    */
    public CoreImage scale(int scaledWidth, int scaledHeight) {
        
        scaledWidth = Math.max(1, scaledWidth);
        scaledHeight = Math.max(1, scaledHeight);
        
        CoreImage scaledImage = new CoreImage(scaledWidth, scaledHeight, isOpaque);
        scaledImage.setHotspot(
            hotspotX * scaledWidth / width,
            hotspotY * scaledHeight / height);
        
        CoreGraphics g = scaledImage.createGraphics();
        g.drawScaledImage(this, 0, 0, scaledWidth, scaledHeight);
        return scaledImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents a 50% scaled 
        version of this image. This method uses a weighted average instead
        of bilinear interpolation. The hotspot is scaled accordingly.
    */
    public CoreImage halfSize() {
    
        CoreImage scaledImage = new CoreImage(width / 2, height / 2, isOpaque);
        scaledImage.setHotspot(hotspotX / 2, hotspotY / 2);
        
        int[] srcData = getData();
        int srcWidth = getWidth();
        int srcOffset = 0;
        
        int[] destData = scaledImage.getData();
        int destWidth = scaledImage.getWidth();
        int destHeight = scaledImage.getHeight();
        int destOffset = 0;
        
        for (int y = 0; y < destHeight; y++) {
            
            srcOffset = srcWidth * y * 2;
            
            for (int x = 0; x < destWidth; x++) {
                
                int p1 = srcData[srcOffset];
                int p2 = srcData[srcOffset + 1];
                int p3 = srcData[srcOffset + srcWidth];
                int p4 = srcData[srcOffset + srcWidth + 1];
                
                int a = ((p1 >>> 24) + (p2 >>> 24) + (p3 >>> 24) + (p4 >>> 24)) >> 2;
                int r = (((p1 >> 16) & 0xff) + ((p2 >> 16) & 0xff) + 
                        ((p3 >> 16) & 0xff) + ((p4 >> 16) & 0xff)) >> 2;
                int g = (((p1 >> 8) & 0xff) + ((p2 >> 8) & 0xff) + 
                        ((p3 >> 8) & 0xff) + ((p4 >> 8) & 0xff)) >> 2;
                int b = ((p1 & 0xff) + (p2 & 0xff) + (p3 & 0xff) + (p4 & 0xff)) >> 2;
                
                destData[destOffset] = (a << 24) | (r << 16) | (g << 8) | b;
                
                srcOffset += 2;
                destOffset++;
            }
        }
        
        return scaledImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents a mirrored version 
        of this image. The hotspot is mirrored accordingly.
    */
    public CoreImage mirror() {
        CoreImage mirroredImage = new CoreImage(width, height, isOpaque);
        
        mirroredImage.setHotspot((width - 1) - hotspotX, hotspotY);
            
        int[] srcData = data;
        int[] destData = mirroredImage.getData();
        int offset = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                destData[offset + i] = srcData[offset + width - 1 - i];
            }
            offset += width;
        }
        
        return mirroredImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents a flipped version 
        of this image. The hotspot is flipped accordingly.
    */
    public CoreImage flip() {
        CoreImage flippedImage = new CoreImage(width, height, isOpaque);
        flippedImage.setHotspot(hotspotX, (height - 1) - hotspotY);
            
        int[] srcData = data;
        int[] destData = flippedImage.getData();
        int srcOffset = width * (height - 1);
        int dstOffset = 0;
        for (int i = 0; i < height; i++) {
            System.arraycopy(srcData, srcOffset, destData, dstOffset, width);
            srcOffset -= width;
            dstOffset += width;
        }
        
        return flippedImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated to the left (counter-clockwise 90 degrees).
        The hotspot is rotated accordingly.
    */
    public CoreImage rotateLeft() {
        
        int newWidth = height;
        int newHeight = width;
        
        CoreImage rotImage = new CoreImage(newWidth, newHeight, isOpaque);
        rotImage.setHotspot(hotspotY, (newHeight - 1) - hotspotX);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < newHeight; j++) {
            int srcOffset = width - 1 - j;
            for (int i = 0; i < newWidth; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset += width;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated to the right (clockwise 90 degrees).
        The hotspot is rotated accordingly.
    */
    public CoreImage rotateRight() {
        
        int newWidth = height;
        int newHeight = width;
        
        CoreImage rotImage = new CoreImage(newWidth, newHeight, isOpaque);
        rotImage.setHotspot((newWidth - 1) - hotspotY, hotspotX);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < newHeight; j++) {
            int srcOffset = width * (height - 1) + j;
            for (int i = 0; i < newWidth; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset -= width;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    /**
        Returns a new CoreImage whose raster data represents this image 
        rotated 180 degrees.
        The hotspot is rotated accordingly.
    */
    public CoreImage rotate180() {
        
        CoreImage rotImage = new CoreImage(width, height, isOpaque);
        rotImage.setHotspot((width - 1) - hotspotX, (height - 1) - hotspotY);
            
        int[] srcData = data;
        int[] destData = rotImage.getData();
        int dstOffset = 0;
        for (int j = 0; j < height; j++) {
            int srcOffset = width * (height - j) - 1;
            for (int i = 0; i < width; i++) {
                destData[dstOffset] = srcData[srcOffset];
                srcOffset--;
                dstOffset++;
            }
        }
        
        return rotImage;
    }
    
    //
    // ARGB filters 
    //
    
    /**
        Returns a new CoreImage with all the opaque pixels of the specified color set to 
        transparent.
        @param rgbColor The color to convert to transparent. The alpha component is ignored.
        @return the new image.
    */
    public CoreImage setTransparentColor(int rgbColor) {
        // Only convert opaque pixels
        rgbColor |= 0xff000000;
        
        CoreImage newImage = new CoreImage(width, height, false);
        newImage.setHotspot(hotspotX, hotspotY);
        
        int[] srcData = data;
        int[] destData = newImage.getData();
        for(int i = 0; i < destData.length; i++) {
            int srcColor = srcData[i];
            destData[i] = (srcColor == rgbColor ? Colors.TRANSPARENT : srcColor);
        }
        return newImage;
    }
    
    /**
        Returns a new CoreImage with every color set to the specified color,
        without changing the alpha of each color. This method is useful for
        creating a variety of colored fonts or for creating a solid-color
        stencil of a sprite.
    */
    public CoreImage tint(int rgbColor) {
        
        CoreImage tintedImage = new CoreImage(width, height, isOpaque);
        tintedImage.setHotspot(hotspotX, hotspotY);
        
        int[] srcData = data;
        int[] destData = tintedImage.getData();
        
        for (int i = 0; i < srcData.length; i++) {
            int color = (srcData[i] & 0xff000000) | (rgbColor & 0x00ffffff);
            color = Colors.premultiply(color);
            destData[i] = color;
        }
        
        return tintedImage;
    }
    
    public CoreImage background(int argbColor) {
        CoreImage newImage = new CoreImage(width, height, 
            isOpaque || (argbColor >>> 24) == 0xff);
        newImage.setHotspot(hotspotX, hotspotY);
        
        argbColor = Colors.premultiply(argbColor);
        
        int[] destData = newImage.getData();
        for (int i = 0; i < destData.length; i++) {
            destData[i] = argbColor;
        }
        
        CoreGraphics g = newImage.createGraphics();
        g.drawImage(this);
        return newImage;
    }
    
    
    public CoreImage fade(int alpha) {
        CoreImage fadeImage = new CoreImage(width, height, 
            isOpaque && alpha >= 0xff);
        fadeImage.setHotspot(hotspotX, hotspotY);
        
        CoreGraphics g = fadeImage.createGraphics();
        g.setAlpha(alpha);
        g.drawImage(this);
        return fadeImage;
    }
}
