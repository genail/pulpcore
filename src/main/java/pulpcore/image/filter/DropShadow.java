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
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
    A simple drop shadow.

    @author David Brackeen
    @author Florent Dupont
 */
public class DropShadow extends Blur {

    private final static int DEFAULT_COLOR = Colors.rgba(0, 0, 0, 128);

    private final PropertyListener updater = new PropertyListener() {
        public void propertyChange(Property property) {
            if (property == color) {
                colorDirty = true;
            }
            setDirty();
        }
    };

    /**
        The shadow x offset. The default value is 3.
    */
    public final Int shadowOffsetX = new Int(updater, 3);

    /**
        The shadow y offset. The default value is 3.
    */
    public final Int shadowOffsetY = new Int(updater, 3);
    
    /**
        The shadow color. The default value is black with 50% alpha.
    */
    public final Color color = new Color(updater, DEFAULT_COLOR);

    private int[] shadowColorTable = new int[256];
    private boolean colorDirty = true;
    
    /**
        Creates a Shadow filter with an offset of (3,3), the default shadow color
        (black with 50% alpha) and the default {@link Blur } values.
    */
    public DropShadow() {
        this(3, 3, DEFAULT_COLOR, 4, 3);
    }
    
    /**
        Creates a Shadow filter with the specified X and Y offset, the default shadow color 
        (black with 50% alpha) and the default {@link Blur } values.
     */
    public DropShadow(int offsetX, int offsetY) {
        this(offsetX, offsetY, DEFAULT_COLOR, 4, 3);
    }
    
    /**
        Creates a Shadow filter with the specified X and Y offset and shadow color,
        and the default {@link Blur } values.
     */
    public DropShadow(int offsetX, int offsetY, int shadowColor) {
        this(offsetX, offsetY, shadowColor, 4, 3);
    }
    
    /**
        Creates a Shadow filter with the specified X and Y offset, shadow color, and shadow radius.
     */
    public DropShadow(int offsetX, int offsetY, int shadowColor, float radius) {
        this(offsetX, offsetY, shadowColor, radius, 3);
    }
    
    /**
        Creates a Shadow filter with the specified X and Y offset, shadow color, shadow radius 
        and shadow quality, from 1 (fastes) to 3 (Guassian approximation).
    */
    public DropShadow(int offsetX, int offsetY, int shadowColor, float radius, int quality) {
        super(radius, quality);
        this.shadowOffsetX.set(offsetX);
        this.shadowOffsetY.set(offsetY);
        this.color.set(shadowColor);
    }

    /**
        Copy constructor. Subclasses can use this to help implement {@link #copy() }.
    */
    public DropShadow(DropShadow filter) {
        super(filter);
        shadowOffsetX.bindWithInverse(filter.shadowOffsetX);
        shadowOffsetY.bindWithInverse(filter.shadowOffsetY);
        color.bindWithInverse(filter.color);
    }
    
    public Filter copy() {
        return new DropShadow(this);
    }

    public void update(int elapsedTime) {
        shadowOffsetX.update(elapsedTime);
        shadowOffsetY.update(elapsedTime);
        color.update(elapsedTime);

        // Call super.update() afterwards because actualRadius isn't set unless the filter is dirty
        super.update(elapsedTime);
    }
    
    private void createShadowColorTable() {
        int rgb = Colors.rgb(color.get());
        int alpha = Colors.getAlpha(color.get());
        for (int i = 0; i < 256; i++) {
            shadowColorTable[i] = Colors.premultiply(rgb, (alpha * i) >> 8);
        }
    }
    
    public boolean isOpaque() {
        return false;
    }

    // TODO: The bounds could use some optimizations for large shadow offsets
    
    public int getX() {
        if (shadowOffsetX.get() > 0) {
            return super.getX();
        }
        else {
            return super.getX() + shadowOffsetX.get();
        }
    }

    public int getY() {
        if (shadowOffsetY.get() > 0) {
            return super.getY();
        }
        else {
            return super.getY() + shadowOffsetY.get();
        }
    }

    public int getWidth() {
        return super.getWidth() + CoreMath.abs(shadowOffsetX.get());
    }

    public int getHeight() {
        return super.getHeight() + CoreMath.abs(shadowOffsetY.get());
    }
    
    protected void filter(CoreImage src, CoreImage dst) {

        // NOTE: This isn't optimal because each pixel is drawn three times.
        // Also, (x,y) offset is an integer, but fractional might be better.

        // Create the blur
        super.filter(src, dst, shadowOffsetX.get(), shadowOffsetY.get(), false);

        // Convert the blur to the shadow color
        if (colorDirty) {
            createShadowColorTable();
            colorDirty = false;
        }
        int[] dstData = dst.getData();
        for (int i = 0; i < dstData.length; i++) {
            dstData[i] = shadowColorTable[dstData[i] >>> 24];
        }

        // Draw the input image on top of the shadow
        CoreGraphics g = dst.createGraphics();
        g.drawImage(src, -getX(), -getY());
    }
}
