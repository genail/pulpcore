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

/**
    An enumeration of blend modes. 
    <p>
    The methods always returns the same object; that is, 
    {@code BlendMode.SrcOver() == BlendMode.SrcOver()} is always {@code true}.
    <p>
    The BlendMode class is designed for lazy-creation of blend modes so that 
    code shrinkers (ProGuard) can remove blend modes that an app does not use. 
    @see CoreGraphics#setBlendMode(BlendMode) 
    @see pulpcore.sprite.Sprite#setBlendMode(BlendMode)
    @author David Brackeen
    @author Florent Dupont
*/
public final class BlendMode {
    
	private static BlendMode CLEAR = null;
	private static BlendMode SRC = null;
	private static BlendMode DST = null;
    private static BlendMode SRC_OVER = null;
    private static BlendMode SRC_IN = null;
    private static BlendMode SRC_ATOP = null;
    private static BlendMode SRC_OUT = null;
    private static BlendMode DST_OVER = null;
    private static BlendMode DST_IN = null;
    private static BlendMode DST_OUT = null;
    private static BlendMode XOR = null;
    private static BlendMode DST_ATOP = null;

    private static BlendMode ADD = null;
    private static BlendMode MULT = null;
    
    /* package-private */ final Composite opaque;
    /* package-private */ final Composite alpha;
    
    /* package-private */ BlendMode(Composite opaque, Composite alpha) {
        this.opaque = opaque;
        this.alpha = alpha;
    }
    
    /** 
     * Gets the Clear blend mode (Porter-Duff).
     * Both the color and alpha of the destination are cleared.
     */
    public static BlendMode Clear() {
        if (CLEAR == null) {
            CLEAR = new BlendMode(new CompositeClear(), new CompositeClear());
        }
        return CLEAR;
    }

    /**
     * Gets the Xor blend mode (Porter-Duff).
     * The part of the source lying outside of the destination is combined with the
	 * part of the destination lying outside of the source.
     */
    public static BlendMode Xor() {
        if (XOR == null) {
            XOR = new BlendMode(new CompositeXor(true), new CompositeXor(false));
        }
        return XOR;
    }

    /**
     * Gets the Dst blend mode (Porter-Duff).
     * Destination is left untouched.
     */
    public static BlendMode Dst() {
        if (DST == null) {
            DST = new BlendMode(new CompositeDst(), new CompositeDst());
        }
        return DST;
    }

    /**
     * Gets the DstOver blend mode (Porter-Duff).
     * The destination is composed with the source and the result replaces
	 * the destination.
     */
    public static BlendMode DstOver() {
        if (DST_OVER == null) {
            DST_OVER = new BlendMode(new CompositeDstOver(true), new CompositeDstOver(false));
        }
        return DST_OVER;
    }

    /**
     * Gets the DstIn blend mode (Porter-Duff).
     * The part of the destination lying inside of the source replaces the destination.
     */
    public static BlendMode DstIn() {
        if (DST_IN == null) {
            DST_IN = new BlendMode(new CompositeDstIn(true), new CompositeDstIn(false));
        }
        return DST_IN;
    }

    /**
     * Gets the DstOut blend mode (Porter-Duff).
     * The part of the destination lying outside of the source replaces the destination.
     */
    public static BlendMode DstOut() {
        if (DST_OUT == null) {
            DST_OUT = new BlendMode(new CompositeDstOut(true), new CompositeDstOut(false));
        }
        return DST_OUT;
    }

    /**
     *  Gets the DstAtop blend mode (Porter-Duff).
     *  The part of the destination lying inside of the source is composed with the
     *  source and replace the destination.
     */
    public static BlendMode DstAtop() {
        if (DST_ATOP == null) {
            DST_ATOP = new BlendMode(new CompositeDstAtop(true), new CompositeDstAtop(false));
        }
        return DST_ATOP;
    }

    /**
     * Gets the SrcIn blend mode (Porter-Duff).
     * The source is copied to the destination.
     */
    public static BlendMode Src() {
        if (SRC == null) {
            SRC = new BlendMode(new CompositeSrc(), new CompositeSrc());
        }
        return SRC;
    }

    /**
     *  Gets the SrcIn blend mode (Porter-Duff).
     *  The part of the source lying inside of the destination replaces the destination.
     */
    public static BlendMode SrcIn() {
        if (SRC_IN == null) {
            SRC_IN = new BlendMode(new CompositeSrcIn(true), new CompositeSrcIn(false));
        }
        return SRC_IN;
    }

    /**
     * Gets the SrcOut blend mode (Porter-Duff).
     * The part of the source lying outside of the destination replaces the destination.
     */
    public static BlendMode SrcOut() {
        if (SRC_OUT == null) {
            SRC_OUT = new BlendMode(new CompositeSrcOut(true), new CompositeSrcOut(false));
        }
        return SRC_OUT;
    }

    /**
     * Gets the SrcAtop blend mode (Porter-Duff).
     * The part of the source lying inside of the destination is composed with
	 * the destination.
     */
    public static BlendMode SrcAtop() {
        if (SRC_ATOP == null) {
            SRC_ATOP = new BlendMode(new CompositeSrcAtop(true), new CompositeSrcAtop(false));
        }
        return SRC_ATOP;
    }

    /**
        Gets the SrcOver blend mode.
        The source is composited over the destination (Porter-Duff Source Over Destination rule).
        This is the default blend mode.
    */
    public static BlendMode SrcOver() {
        if (SRC_OVER == null) {
            SRC_OVER = new BlendMode(new CompositeSrcOver(true), new CompositeSrcOver(false));
        }
        return SRC_OVER;
    }
    
    /**
        Gets the Add blend mode. Color components from the source are added to those
        of the surface.
    */
    public static BlendMode Add() {
        if (ADD == null) {
            ADD = new BlendMode(new CompositeAdd(true), new CompositeAdd(false));
        }
        return ADD;
    }
    
    /**
        Gets the Multiply blend mode. Color components from the source are multiplied by those
        of the surface.
    */
    public static BlendMode Multiply() {
        if (MULT == null) {
            MULT = new BlendMode(new CompositeMult(), new CompositeMult());
        }
        return MULT;
    }
}
