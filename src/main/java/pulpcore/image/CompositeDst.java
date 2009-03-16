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
    DST (for premultiplied images)

    Porter-Duff's Dst rule:
        Ar = Ad
        Cr = Cd

    Porter-Duff's Dst rule with extra alpha:
        Ar = Ad
        Cr = Cd

    Destination is left untouched.

 */
final class CompositeDst extends Composite {

	CompositeDst() {}

    // Different from the other Composites since it's a no-op

	void blend(int[] destData, int destOffset, int srcARGB) {
	}

	void blend(int[] destData, int destOffset, int srcARGB, int extraAlpha) {
	}

	void blendRow(int[] destData, int destOffset, int srcARGB, int numPixels) {
	}

	void blend(int[] srcData, int srcScanSize, boolean srcOpaque, int edgeClamp,
			int srcX, int srcY, int srcWidth, int srcHeight, 
			int u, int v, int du, int dv, 
			boolean rotation,
			boolean renderBilinear, int renderAlpha,
			int[] destData, int destScanSize, int destOffset, int numPixels, int numRows)
	{
		
	}        
}

