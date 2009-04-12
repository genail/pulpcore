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
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
 * A Glow filter.
 * 
 * This Glow filter uses 3 parameters : 
 * - amount : the amount of glow to be used. float value that could go from 0 to up to 2 depending on the effect
 *   you're expecting to have. Default value is 0.5;
 * - radius : the radius of glow to be used. Default value is 3.
 * - quality : the rendering quality of the filter. A better quality needs more time to render. Default value is 3.
 *  In some cases values below 3 should be OK (quality 1 is very poor though). Quality value more than 3 won't 
 *  really affect the rendering since this 3 fast blur are somehow equivalent to a real Gaussian Blur.
 *      
 * 
 * Example of use below that mimic the PS3 menu selection :
 * <code>
 *  public void load() {
 *	
 *		add(new FilledSprite(Colors.BLACK));
 *		
 *		Glow glow = new Glow(0.8, 5);
 *		
 *		Timeline timeline = new Timeline();
 *		timeline.animate(glow.amount, 0.2, 0.8, 500, null, 0);
 *		timeline.animate(glow.amount, 0.8, 0.2, 500, null, 500);
 *		timeline.loopForever();
 *	    addTimeline(timeline);
 *	    CoreFont font = CoreFont.getSystemFont().tint(0xff80ff80);
 *	    Label lb = new Label(font, "An example of glowing text...", 20, 20);
 *	    lb.setFilter(glow);
 *		
 *		add(lb);
 *  }
 *		
 * </code>
 * 
 * @author Florent Dupont
 */
public final class Glow extends Blur {

    private static final int MAX_ACTUAL_AMOUNT = Integer.MAX_VALUE / 255;

	/**
    	The amount of glow to be used, typically from 0 to 8. The default value is 0.5.
    */
	public final Fixed amount = new Fixed(0.5f);
	
	private int actualAmount = 0;
	
	/**
    	Creates a Glow filter with a radius of 3, a quality of 3 and amount of 0,5.
	*/
	public Glow() {
		this(0.5f);
	}

	/**
    	Creates a Glow filter with the specified amount, a radius of 3 and a quality of 3.
    */
	public Glow(double amount) {
		this(amount, 3);
	}

	/**
		Creates a Glow filter with the specified amount and radius. Default quality of 3.
	*/
	public Glow(double amount, int radius) {
		this(amount, radius, 3);
	}

	/**
		Creates a Glow filter with the specified amount, radius and quality.
    */
	public Glow(double amount, int radius, int quality) {
		super(radius, quality);
		this.amount.set(amount);
	}

    /**
        Copy constructor. Subclasses can use this to help implement {@link #copy() }.
    */
    private Glow(Glow filter) {
        super(filter);
        amount.bindWithInverse(filter.amount);
    }

	public Filter copy() {
        return new Glow(this);
	}
	
	public void update(int elapsedTime) {
		
		amount.update(elapsedTime);


        float newAmount = 0;
        if (radius.get() > 0) {
            // Add a bit of the radius since a high radius diminishes the glow.
            newAmount += (float)radius.get() / 64;
        }
        if (amount.get() > 0) {
            newAmount += (float)amount.get();
        }
        int newActualAmount = CoreMath.clamp((int)(32768 * newAmount), 0, MAX_ACTUAL_AMOUNT);
		if (actualAmount != newActualAmount) {
			actualAmount = newActualAmount;
			setDirty();
		}

        // Call super.update() afterwards because actualRadius isn't set unless the filter is dirty
        super.update(elapsedTime);
	}

	protected void filter(CoreImage src, CoreImage dst) {

		// call the parent blur filter.
		super.filter(src, dst);

		int a = actualAmount;

		int[] dstData = dst.getData();
		int[] srcData = src.getData();
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int dstWidth = dst.getWidth();
		int dstHeight = dst.getHeight();
		
		int xOffset = getX();
		int yOffset = getY();
		
		for (int y = 0; y < dstHeight; y++) {
            int dstIndex = (y * dstWidth);
            if (y + yOffset < 0 || y + yOffset >= srcHeight) {
                filterOnBlankArea(dstData, dstIndex, dstWidth);
            }
            else {
                int srcIndex = ((y + yOffset) * srcWidth);
                int w = -xOffset;
                if (w > 0) {
                    filterOnBlankArea(dstData, dstIndex, w);
                    dstIndex += w;
                }
                for (int x = 0; x < srcWidth; x++) {
                    int dstPixel = dstData[dstIndex];
                    int srcPixel = srcData[srcIndex];

					int da = dstPixel >>> 24;
					int dr = (dstPixel >> 16) & 0xff;
					int dg = (dstPixel >> 8) & 0xff;
					int db = dstPixel & 0xff;

					int sa = srcPixel >>> 24;
					int sr = (srcPixel >> 16) & 0xff;
					int sg = (srcPixel >> 8) & 0xff;
					int sb = srcPixel & 0xff;

					int na = sa + ((a * da) >> 13);
					na = na > 255 ? 255 : na;
                    int nr = sr + ((a * dr) >> 13);
					nr = nr > na ? na : nr;
					int ng = sg + ((a * dg) >> 13);
					ng = ng > na ? na : ng;
					int nb = sb + ((a * db) >> 13);
					nb = nb > na ? na : nb;

					dstData[dstIndex] = (na << 24) | (nr << 16) | (ng << 8) | nb;
                    dstIndex++;
                    srcIndex++;
                }
                w = dstWidth - srcWidth + xOffset;
                if (w > 0) {
                    filterOnBlankArea(dstData, dstIndex, w);
                    dstIndex += w;
                }
            }
		}
	}
    
    private void filterOnBlankArea(int[] dstData, int dstIndex, int length) {

        int a = actualAmount;
        
        for (int j = 0; j < length; j++) {
            int dstPixel = dstData[dstIndex];
            int da = dstPixel >>> 24;
            int dr = (dstPixel >> 16) & 0xff;
            int dg = (dstPixel >> 8) & 0xff;
            int db = dstPixel & 0xff;

            int na = (a * da) >> 13;
            na = na > 255 ? 255 : na;
            int nr = (a * dr) >> 13;
            nr = nr > na ? na : nr;
            int ng = (a * dg) >> 13;
            ng = ng > na ? na : ng;
            int nb = (a * db) >> 13;
            nb = nb > na ? na : nb;

            dstData[dstIndex] = (na << 24) | (nr << 16) | (ng << 8) | nb;
            dstIndex++;
        }
    }
}
