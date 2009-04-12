/*
    Copyright (c) 2009, Interactive Pulp, LLC
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
package pulpcore.image.filter;

import pulpcore.animation.Color;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.image.BlendMode;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
    The ColorOverlay filter fills visible pixels of an input image with a color, optionally drawing
     the color over the existing input pixels if alpha is less than 255.
 */
public final class ColorOverlay extends Filter {

    private final PropertyListener updater = new PropertyListener() {
        public void propertyChange(Property property) {
            colorDirty = true;
            setDirty();
        }
    };

    /**
        The overlay color.
    */
    public final Color color = new Color(updater);

    /**
        The alpha of the overlay, from 0 to 255. For values less than 255, the original
        input image is visible underneath the overlay color. The default is 255.
    */
    public final Int alpha = new Int(updater, 255);

    private boolean colorDirty = true;
    private int[] colorTable = new int[256];

    /**
        Creates a ColorOverlay filter with the specified color.
    */
    public ColorOverlay(int argbColor) {
        this(argbColor, 255);
    }

    /**
        Creates a ColorOverlay filter with the specified color and alpha.
    */
    public ColorOverlay(int argbColor, int alpha) {
        this.color.set(argbColor);
        this.alpha.set(alpha);
    }

    public Filter copy() {
        ColorOverlay copy = new ColorOverlay(0);
        copy.color.bindWithInverse(color);
        copy.alpha.bindWithInverse(alpha);
        return copy;
    }

    public boolean isOpaque() {
        return (super.isOpaque() && (color.get() >>> 24) == 255);
    }

    public void update(int elapsedTime) {
        color.update(elapsedTime);
        alpha.update(elapsedTime);
    }

    protected void filter(CoreImage src, CoreImage dst) {
        int alphaFilter = CoreMath.clamp(alpha.get(), 0, 255);
        if (colorDirty) {
            int rgbColor = Colors.rgb(color.get());
            int alphaColor = (Colors.getAlpha(color.get()) * alphaFilter) / 255;
            for (int i = 0; i < 256; i++) {
                colorTable[i] = Colors.premultiply(rgbColor, (alphaColor * i) / 255);
            }
            colorDirty = false;
        }

        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();
        
        if (alphaFilter == 0) {
            System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
        }
        else {
            for (int i = 0; i < srcPixels.length; i++) {
                dstPixels[i] = colorTable[srcPixels[i] >>> 24];
            }
            if (alphaFilter < 255) {
                // Use DstOver: The color overlay (dst) is over the src
                CoreGraphics g = dst.createGraphics();
                g.setBlendMode(BlendMode.DstOver());
                g.setAlpha(255-alphaFilter);
                g.drawImage(src);
            }
        }
    }
}
