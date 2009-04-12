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

import pulpcore.image.Colors;
import pulpcore.image.CoreImage;

/**
    A false-color filter that mocks a Thermal view filter.
    <p>
    This filter is done by grayscaling the image and applying false colors.
    Colors originally from white->black are changed into a full RGB spectrum color.

    @author Florent Dupont
 */
public final class Thermal extends Filter {

    private final static int[] PALETTE = new int[256];

    static {
        createPalette(PALETTE);
    }

    public Filter copy() {
        return new Thermal();
    }

    protected void filter(CoreImage src, CoreImage dst) {

        int[] srcPixels = src.getData();
        int[] dstPixels = dst.getData();

        for (int i = 0; i < srcPixels.length; i++) {
            int srcRGB = Colors.unpremultiply(srcPixels[i]);

            int srcA = srcRGB >>> 24;
            int srcR = (srcRGB >> 16) & 0xff;
            int srcG = (srcRGB >> 8) & 0xff;
            int srcB = srcRGB & 0xff;

            // Add together 30% of red value, 59% of green, 11% of blue
            int dstGray = (srcR * 77 + srcG * 151 + srcB * 28) >> 8;

            // Rearrange the grayscale colors to obtain different values from white to red to
            // green to blue to black. Real thermal filter have 10 values. See examples here:
            // http://en.wikipedia.org/wiki/Thermal_imaging
            // but this "fake thermal filter" have only 8 ranges to facilitate calculation.
            dstPixels[i] = Colors.premultiply(PALETTE[dstGray], srcA);
        }
    }

    // RGB spectrum palette
    private static void createPalette(int[] palette) {

        for (int i = 0; i < 255; i++) {

            if (i >= 0 && i <= 31) {
                // form black to dark blue
                // from 0xff000000 -> 0xff000080
                int val = (i << 2);
                palette[i] = 0xff000000 | val;
            }
            else if (i >= 32 && i <= 63) {
                // from dark blue to blue 0xff000080 -> 0xff0000ff
                int val = 0x80 + ((i - 32) << 2);
                palette[i] = 0xff000000 | val;
            }
            else if (i >= 64 && i <= 95) {
                // from blue to cyan 0xff0000ff -> 0xff00ffff
                int val = ((i - 64) << 3);
                palette[i] = 0xff0000ff | (val << 8);
            }
            else if (i >= 96 && i <= 127) {
                // from cyan to green 0xff00ffff -> 0xff00ff00
                int val1 = 0xff - ((i - 96) << 3);
                palette[i] = 0xff00ff00 | val1;
            }
            else if (i >= 128 && i <= 159) {
                // from green to yellow 0xff00ff00 -> 0xffffff00
                int val1 = ((i - 128) << 3);
                palette[i] = 0xff00ff00 | (val1 << 16);
            }
            else if (i >= 160 && i <= 191) {
                // from yellow to red 0xffffff00 -> 0xffff0000
                int val1 = 0xff - ((i - 160) << 3);
                palette[i] = 0xffff0000 | (val1 << 8);
            }
            else if (i >= 192 && i <= 223) {
                // from red to purple 0xffff0000 -> 0xff0000ff
                int val1 = ((i - 192) << 3);
                palette[i] = 0xffff0000 | (val1);
            }
            else if (i >= 224 && i <= 255) {
                // 0xffff00ff to 0xffffffff
                int val1 = ((i - 224) << 3);
                palette[i] = 0xffff00ff | (val1 << 8);
            }
        }
    }
}
