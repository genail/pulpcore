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


import pulpcore.animation.Int;
import pulpcore.image.CoreImage;

/**
 *  The Superimpose filter allows to superimpose an image over a sprite.
 *	
 *	This filter uses 3 parameters: 
 *  - An image that will be superimposed over the filtered sprite. This image can be a different size of the Sprite.
 *  - the position (through x and y attributes) of the image. Coordinate are relative to the upper-left pixel 
 *    position of the filtered sprite. Default values are 0,0
 *  - an alpha value applied on the superimposed image. Default value is 255 (no fading).
 *  Each of this value can move from -255 to 255. Higher values will be clamped.
 *  The alpha channel of the original image is not affected by the filter.
 * 
 * @author Florent Dupont
 */
public final class Superimpose extends Filter {
	
	/**
	 * Image superimposed to the filter.
	 * this attribute is accessible through accessors.
	 */
	private CoreImage image;
	
	/**
	 * Fading applied to the superimposed image, from 0 to 255. The default value is 255.
	 */
	public final Int alpha = new Int(255);
	
	/**
	 *  The x offset of the superimposed image. This coordinates are relative to the left
	 *  pixel position of the filtered sprite.
	 */
	public final Int x = new Int(0);

    /**
	 *  The y offset of the superimposed image. This coordinates are relative to the upper
	 *  pixel position of the filtered sprite.
	 */
	public final Int y = new Int(0);
	
	private int actualAlpha = 255;
	private int actualImgXOffset = 0;
	private int actualImgYOffset = 0;
	
	/**
	 * Creates the Superimpose filter with no image.
	 */
	public Superimpose() {
		this(null);
	}
	
	/**
	 * Creates the superimpose filter with the specified image.
	 * @param image Image to superimpose to the filtered Sprite.
	 */
	public Superimpose(CoreImage image) {
		this(image, 0, 0);
	}
	
	/**
	 * Creates a Superimpose filter with the specified parameters.
	 * 
	 * @param image Image to superimpose to the filtered Sprite.
	 * @param x position X relative to the sprite.
	 * @param y position Y relative to the sprite.
	 */
	public Superimpose(CoreImage image, int x, int y) {
		this(image, x, y, 255);
	}
	
	/**
	 * Creates a Superimpose filter with the specified parameters.
	 * 
	 * @param image Image to superimpose to the filtered Sprite.
	 * @param x X offset (relative to the upper-left point of the filtered sprite).
	 * @param y Y offset (relative to the upper-left point of the filtered sprite).
	 * @param alpha amount of fading applied to the superimposed image, from 0 to 255.
	 */
	public Superimpose(CoreImage image, int x, int y, int alpha) {
		this.image = image;
		this.x.set(x);
		this.y.set(y);
		this.alpha.set(alpha);
	}

	/**
	 * Image accessors.
	 * @return the superimposed image
	 */
	public CoreImage getImage() {
		return this.image;
	}
	
	/**
	 * Image accessors
	 * @param image The superimposed image to apply to this filter.
	 */
	public void setImage(CoreImage image) {
        if (this.image != image) {
            this.image = image;
            setDirty();
        }
	}
	
    public Filter copy() {
        Superimpose copy = new Superimpose();
        // TODO: somehow bind image, too
        copy.image = image;
        copy.alpha.bindWithInverse(alpha);
        copy.x.bindWithInverse(x);
        copy.y.bindWithInverse(y);
        return copy;
    }
    
    public void update(int elapsedTime) {    	
    	alpha.update(elapsedTime);
    	
    	if (alpha.get() != actualAlpha) {
    		actualAlpha = alpha.get();
    		setDirty();
    	}
    	
    	if (x.get() != actualImgXOffset) {
    		actualImgXOffset = y.get();
    		setDirty();
    	}
    	
    	if (x.get() != actualImgYOffset) {
    		actualImgYOffset = y.get();
    		setDirty();
    	}
    }
    
    public int getHeight() {
    	int imgHeight = image.getHeight();
    	int srcHeight = super.getHeight();

    	if (actualImgYOffset < 0) {
    		return (imgHeight > srcHeight) ? -actualImgYOffset + imgHeight : -actualImgYOffset + srcHeight;
    	}
        else {
    		return (actualImgYOffset + imgHeight > srcHeight) ? actualImgYOffset + imgHeight : srcHeight; 
    	}
    }
    
    public int getWidth() {
    	int imgWidth = image.getWidth();
		int srcWidth = super.getWidth();
		
    	if (actualImgXOffset < 0) {
    		return (imgWidth > srcWidth) ? -actualImgXOffset + imgWidth : -actualImgXOffset + srcWidth;
    	}
        else {
    		return (actualImgXOffset + imgWidth > srcWidth) ? actualImgXOffset + imgWidth : srcWidth; 
    	}
    }
    
    public int getX() {
    	if (actualImgXOffset < 0) {
    		return super.getX() + actualImgXOffset;
        }
    	else {
    		return super.getX();
    	}
    }
    
    public int getY() {
    	if(actualImgYOffset < 0) {
    		return super.getY() + actualImgYOffset;
    	}
    	else {
    		return super.getY();
    	}
    }
    
    /**
     *  perform a SRC_OVER Porter & Duff operation.
     *  +-------+
     *  |  SRC  | 
     *  |  +-------+
     *  |  |  DST  |
     *  |  +-------+
     *  +-------+
     * @param src the destination pixel
     * @param dst the source pixel 
     * @return the resulting color of DST OVER SRC
     */
    
    private final int srcOver(int src, int dst) {
    	int da = src >>> 24;
		int dr = (src >> 16) & 0xff;
		int dg = (src >> 8) & 0xff;
		int db = src & 0xff;
		
		int sa = dst >>> 24;
		int sr = (dst >> 16) & 0xff;
		int sg = (dst >> 8) & 0xff;
		int sb = dst & 0xff;
		
		sa *= actualAlpha;
        sr *= actualAlpha;
        sg *= actualAlpha;
        sb *= actualAlpha;
        
        int oneMinusSA = 0xff - (sa>>8);
		
		da = (sa + da * oneMinusSA) >> 8;
        dr = (sr + dr * oneMinusSA) >> 8;
        dg = (sg + dg * oneMinusSA) >> 8;
        db = (sb + db * oneMinusSA) >> 8;
         
        return (da << 24) | (dr << 16) | (dg << 8) | db;
    }

    protected void filter(CoreImage src, CoreImage dst) {
			
    	int[] imgPixels = image.getData();

    	int[] srcPixels = src.getData();
    	int[] dstPixels = dst.getData();
    	int srcHeight = src.getHeight();
    	int srcWidth = src.getWidth();
    	int dstWidth = dst.getWidth();

    	int imgWidth = image.getWidth();
    	int imgHeight = image.getHeight();

    	int xOffset = getX();
    	int yOffset = getY();

    	// first copies the src into the dst
    	for (int i = 0; i < srcHeight; i++) {
            int srcIndex = (i * srcWidth);
    		int dstIndex = ((i - yOffset) * dstWidth) - xOffset;
    		for (int j = 0; j < srcWidth; j++) {
    			dstPixels[dstIndex++] = srcPixels[srcIndex++];
    		}
    	}

    	// then copies the superimposed image over the existing dst value.
    	for (int i = 0; i < imgHeight; i++) {
            int imgIndex = (i * imgWidth);
  			int dstIndex = ((i + actualImgYOffset - yOffset) * dstWidth) + actualImgXOffset - xOffset;
    		for (int j = 0; j < imgWidth; j++) {

    			int dstARGB = dstPixels[dstIndex];
    			int imgARGB = imgPixels[imgIndex];
    			
    			if (imgARGB != 0) {
    				// if the image pixel is fully transparent, no need to perform a SRC_OVER operation
    				if (dstARGB == 0) {
    					dstPixels[dstIndex] = imgARGB;
    				}
    				else {
    					dstPixels[dstIndex] = srcOver(dstARGB, imgARGB);
    				}
    			}
    			// else .... 
    			// if the dst pixel is fully transparent, no need to perform a SRC_OVER, just copies the image pixel into DST

                imgIndex++;
                dstIndex++;
    		}
    	}
    }
}

