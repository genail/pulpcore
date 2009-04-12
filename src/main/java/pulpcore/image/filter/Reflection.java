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

import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
 *	A Reflection filter.
 *	Fake a mirrored image on brilliant surface.
 *
 *	@author Florent Dupont
 *
*/
public final class Reflection extends Filter {
	
	/**
		The vertical gap, in pixels, between the bottom of the image and the top of the
        reflection. The default value is 1.
	*/
    // TODO: allow for negative gap?
	public final Int gap = new Int(1);

    /**
    	The reflection height as a fraction of the input image, from 0 to 1.
        The default value is 0.75.
    */
	public final Fixed fraction = new Fixed(0.75f);

    /**
        The alpha of the top of the reflection (nearest the bottom of the image), from 0 to 255.
        The default value is 128.
     */
    public final Int topAlpha = new Int(128);
	
    /**
        The alpha of the bottom of the reflection (farthest from from the bottom of the image),
        from 0 to 255. The default value is 0.
     */
    public final Int bottomAlpha = new Int(0);

	
	private float actualFraction;
	private int actualGap;
	private int actualTopAlpha;
	private int actualBottomAlpha;
		
	/**
		Creates a Reflection filter with the default parameters.
	 */
	public Reflection() {
		this(1, 0.75f, 128, 0);
	}

    /**
		Creates a Reflection filter with the specified gap.
	 */
	public Reflection(int gap) {
		this(gap, 0.75f, 128, 0);
	}
	
	/**
		Creates a Reflection filter with the specified gap and reflection height (as a fraction of
        the input image).
	*/
	public Reflection(int gap, float fraction) {
		this(1, fraction, 128, 0);
	}
	
	/**
		Creates a Reflection filter with the specified gap, (as a fraction of
        the input image), and alpha values.
	 */
	public Reflection(int gap, float fraction, int topAlpha, int bottomAlpha) {
		this.gap.set(gap);
        this.fraction.set(fraction);
        this.topAlpha.set(topAlpha);
        this.bottomAlpha.set(bottomAlpha);
	}
	
    public Filter copy() {
        Reflection copy = new Reflection();
        copy.gap.bindWithInverse(gap);
        copy.fraction.bindWithInverse(fraction);
        copy.topAlpha.bindWithInverse(topAlpha);
        copy.bottomAlpha.bindWithInverse(bottomAlpha);
        return copy;
    }
    
    public int getHeight() {
        int h = super.getHeight();
        int reflectionHeight = Math.round(actualFraction * h);
        if (reflectionHeight <= 0) {
            return h;
        }
        else {
            return h + actualGap + reflectionHeight;
        }
    }

    public boolean isOpaque() {
    	return false;
    }
    
    public void update(int elapsedTime) {
        	
    	gap.update(elapsedTime);
        fraction.update(elapsedTime);
    	topAlpha.update(elapsedTime);
        bottomAlpha.update(elapsedTime);

        float newFraction = CoreMath.clamp((float)fraction.get(), 0, 1);
    	if (actualFraction != newFraction) {
    		actualFraction = newFraction;
    		setDirty();
    	}

        int newGap = CoreMath.clamp(gap.get(), 0, 32768);
    	if (actualGap != newGap) {
    		actualGap = newGap;
    		setDirty();
    	}

        int newTopAlpha = CoreMath.clamp(topAlpha.get(), 0, 255);
    	if (actualTopAlpha != newTopAlpha) {
    		actualTopAlpha = newTopAlpha;
    		setDirty();
    	}

        int newBottomAlpha = CoreMath.clamp(bottomAlpha.get(), 0, 255);
    	if (actualBottomAlpha != newBottomAlpha) {
    		actualBottomAlpha = newBottomAlpha;
    		setDirty();
    	}
    }

    protected void filter(CoreImage src, CoreImage dst) {
    	
        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();
        
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        
        // Copy the source to the destination
        int srcOffset = 0;
        for (int i = 0; i < srcHeight; i++) {
        	System.arraycopy(srcPixels, srcOffset, dstPixels, srcOffset, srcWidth);
        	srcOffset += srcWidth;
        }

        int reflectionHeight = Math.round(actualFraction * srcHeight);
        if (reflectionHeight <= 0) {
            return;
        }

        // Make sure the gap is clear
        for (int y = 0; y < actualGap; y++) {
            int dstIndex = srcWidth * (srcHeight + y);
            for (int x = 0; x < srcWidth; x++) {
                dstPixels[dstIndex++] = 0;
            }
        }

        // Create the reflection
        for (int y = 0; y < reflectionHeight; y++) {
            // Interpolate the alpha for this row
            int da = actualTopAlpha - actualBottomAlpha;
            float f = (float)(reflectionHeight - y) / reflectionHeight;
            // Ease the curve
            f = f * f;
        	int maskA = actualBottomAlpha + Math.round(f * da);

            int srcIndex = (srcHeight - y - 1) * srcWidth;
            int dstIndex = (y + actualGap + srcHeight) * srcWidth;
            
        	for (int x = 0; x < srcWidth; x++) {

        		int srcARGB = srcPixels[srcIndex];
        		int srcR = (srcARGB >> 16) & 0xff;
        		int srcG = (srcARGB >> 8) & 0xff;
        		int srcB = srcARGB & 0xff;
        		int srcA = srcARGB >>> 24;

        		srcR = (srcR * maskA) >> 8; 
        		srcG = (srcG * maskA) >> 8;
        		srcB = (srcB * maskA) >> 8;
        		srcA = (srcA * maskA) >> 8;
        		 
        		dstPixels[dstIndex] = (srcA << 24) | (srcR << 16) | (srcG << 8) | srcB;
                srcIndex++;
                dstIndex++;
        	}
        }
	}
}
