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

package pulpcore.sprite;

import pulpcore.animation.Color;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.Stage;
import pulpcore.animation.Property;

/**
    Solid-colored rectangluar shaped sprite.
*/
public class FilledSprite extends Sprite {
    
    /** Fill color */
    public final Color fillColor = new Color(this);
    
    /** Border color */
    public final Color borderColor = new Color(this);
    
    // Fixed-point
    private int borderTop;
    private int borderLeft;
    private int borderBottom;
    private int borderRight;
    
    public FilledSprite(int fillColor) {
        this(0, 0, Stage.getWidth(), Stage.getHeight(), fillColor);
    }
    
    /**
        @param fillColor Color of the FilledSprite in the ARGB format.
    */
    public FilledSprite(int x, int y, int w, int h, int fillColor) {
        super(x, y, w, h);
        this.fillColor.set(fillColor);
    }
    
    public FilledSprite(double x, double y, double w, double h, int fillColor) {
        super(x, y, w, h);
        this.fillColor.set(fillColor);
    }

    public void propertyChange(Property p) {
        super.propertyChange(p);
        if (p == width || p == height || p == fillColor || p == borderColor) {
            setDirty(true);
        }
    }
    
    public final void setBorderSize(int borderSize) {
        setBorderSize(borderSize, borderSize, borderSize, borderSize);
    }
    
    public void setBorderSize(int top, int left, int bottom, int right) {
        top = CoreMath.toFixed(Math.max(0, top));
        left = CoreMath.toFixed(Math.max(0, left));
        bottom = CoreMath.toFixed(Math.max(0, bottom));
        right = CoreMath.toFixed(Math.max(0, right));
        
        if (this.borderTop != top) {
            this.borderTop = top;
            setDirty(true);
        }
        if (this.borderLeft != left) {
            this.borderLeft = left;
            setDirty(true);
        }
        if (this.borderBottom != bottom) {
            this.borderBottom = bottom;
            setDirty(true);
        }
        if (this.borderRight != right) {
            this.borderRight = right;
            setDirty(true);
        }
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        fillColor.update(elapsedTime);
        borderColor.update(elapsedTime);
    }
    
    // Override the center anchor since FilledRect is a vector object
    // (Raster objects need the center to be floor() so that centered images don't
    // look blurry)
    
    protected int getAnchorX() {
        if ((getAnchor() & HCENTER) != 0) {
            return getNaturalWidth() / 2;
        }
        else {
            return super.getAnchorX();
        }
    }
    
    protected int getAnchorY() {
        if ((getAnchor() & VCENTER) != 0) {
            return getNaturalHeight() / 2;
        }
        else {
            return super.getAnchorY();
        }
    }
    
    protected void drawSprite(CoreGraphics g) {
        
        int w = width.getAsFixed();
        int h = height.getAsFixed();
        int innerWidth = w - (borderLeft + borderRight);
        int innerHeight = h - (borderTop + borderBottom);
        
        // Inner fill
        if ((fillColor.get() >>> 24) != 0) {
            g.setColor(fillColor.get());
            g.fillRectFixedPoint(borderLeft, borderTop, innerWidth, innerHeight);
        }
        
        // Border fill
        if ((borderColor.get() >>> 24) != 0) {
            g.setColor(borderColor.get());
            if (borderTop > 0) {
                g.fillRectFixedPoint(0, 0, w, borderTop);
            }
            if (borderBottom > 0) {
                g.fillRectFixedPoint(0, h - borderBottom, w, borderBottom);
            }
            if (borderLeft > 0) {
                g.fillRectFixedPoint(0, borderTop, borderLeft, innerHeight);
            }
            if (borderRight > 0) {
                g.fillRectFixedPoint(w - borderRight, borderTop, borderRight, innerHeight);
            }
        }
    }
}