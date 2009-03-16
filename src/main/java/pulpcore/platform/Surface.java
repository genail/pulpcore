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

package pulpcore.platform;

import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.Rect;

/**
    The Surface class contains the surface that Scenes draw onto. It handles
    copying the surface data to the screen.
*/
public abstract class Surface {
    
    private CoreGraphics g;
    protected CoreImage image;
    private int[] imageData;
    protected boolean contentsLost;
    
    protected int[] getData() {
        return imageData;
    }
    
    public boolean isReady() {
        return (image != null);
    }
    
    public boolean contentsLost() {
        return contentsLost;
    }
    
    protected final void setSize(int w, int h) {
        imageData = new int[w * h];
        image = new CoreImage(w, h, true, imageData);
        g = image.createGraphics();
        int bgColor = CoreSystem.getDefaultBackgroundColor();
        if (bgColor != Colors.BLACK) {
            g.setColor(bgColor);
            g.fill();
            g.reset();
        }
        notifyResized();
    }
    
    protected void notifyResized() {
        contentsLost = true;
    }
    
    public void notifyOSRepaint() {
        // Do nothing
    }
    
    public void notifyStop() {
        // Do nothing
    }
    
    public void notifyStart() {
        // Do nothing
    }
    
    public CoreGraphics getGraphics() {
        return g;
    }
    
    public int getWidth() {
        if (image == null) {
            return -1;
        }
        else {
            return image.getWidth();
        }
    }
    
    public int getHeight() {
        if (image == null) {
            return -1;
        }
        else {
            return image.getHeight();
        }
    }
    
    /**
        @return the number of microseconds slept before the frame was shown, if possible.
        For example, a Surface  may return the number of microseconds waiting for the 
        vertical blank interval. Returns zero if the surface did not sleep.
    */
    public final long show() {
        return show(null, -1);
    }
    
    /**
        @param dirtyRectangles list of dirty rectangles
        @param numDirtyRectangles If -1, the entire surface needs to be drawn. If 0, none of the
        surface needs to be drawn.
        @return the number of microseconds slept before the frame was shown, if possible.
        For example, a Surface  may return the number of microseconds waiting for the 
        vertical blank interval. Returns zero if the surface did not sleep.
    */
    public abstract long show(Rect[] dirtyRectangles, int numDirtyRectangles);
    
    public void getScreenshot(CoreImage image, int x, int y) {
        if (image != null) {
            CoreGraphics g2 = image.createGraphics();
            g2.drawImage(this.image, -x, -y);
        }
    }
}
