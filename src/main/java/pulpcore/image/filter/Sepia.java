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

import pulpcore.image.CoreImage;

/**
    A sepia tone filter.
    @author Florent Dupont
 */
public final class Sepia extends Filter {
		
    public Filter copy() {
        return new Sepia();
    }

    protected void filter(CoreImage src, CoreImage dst) {
			
        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();

        for(int i = 0; i < srcPixels.length; i++) {
            int srcRGB = srcPixels[i];

            int srcA = srcRGB >>> 24;
            int srcR = (srcRGB >> 16) & 0xff;
            int srcG = (srcRGB >> 8) & 0xff;
            int srcB = srcRGB & 0xff;

            // R*39.3% + G*76.9% + 18.9%B
            int dstR = (srcR * 100 + srcG * 196 + srcB * 48) >> 8;
            dstR = dstR > srcA ? srcA : dstR;
            // R*34.9% + G*68.6% + 16.8%B
            int dstG = (srcR * 89 + srcG * 175 + srcB * 43) >> 8;
            dstG = dstG > srcA ? srcA : dstG;
            // R*27.2% + G*53.4% + 13.1%B
            int dstB = (srcR * 69 + srcG * 136 + srcB * 33) >> 8;
            dstB = dstB > srcA ? srcA : dstB;

            // Values are already pre-multiplied
            dstPixels[i] = (srcA << 24) | (dstR << 16) | (dstG << 8) | dstB;
        }
    }
}
