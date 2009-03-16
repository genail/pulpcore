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

import pulpcore.math.CoreMath;

/**
	SRC_IN (for premultiplied images)

	Porter-Duff's SRC_IN rule:
		Ar = As*Ad
		Cr = Cs*Ad

	Porter-Duff's SRC_IN rule with extra alpha:
		Ar = As*Ae*Ad
		Cr = Cs*Ae*Ad

	The part of the source lying inside of the destination replaces the destination.


@author Florent Dupont
 */
final class CompositeSrcIn extends Composite {

    // If true, the blend() functions send transparent pixels to blendPixel()
    private static final boolean BLEND_TRANSPARENT_PIXELS = true;

    private final boolean destOpaque;

	CompositeSrcIn(boolean destOpaque) {
		this.destOpaque = destOpaque;
	}

	// Ar = As*Ad
	// Cr = Cs*Ad
	private void blendOpaquePixel(int[] destData, int destOffset, int srcRGB) {
		int destRGB = destData[destOffset];
		int destR = (destRGB >> 16) & 0xff;
		int destG = (destRGB >> 8) & 0xff;
		int destB = destRGB & 0xff;
		int destA = destRGB >>> 24;
		int srcR = (srcRGB >> 16) & 0xff;
		int srcG = (srcRGB >> 8) & 0xff;
		int srcB = srcRGB & 0xff;

		destR = (srcR * destA)>>8;
		destG = (srcG * destA)>>8;
		destB = (srcB * destA)>>8;

		if (destOpaque) {
			destData[destOffset] = 0xff000000 | (destR << 16) | (destG << 8) | destB;
		}
		else {
			destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;    
		}

	}

	// Ar = As*Ae*Ad
	// Cr = Cs*Ae*Ad
	private void blendOpaquePixel(int[] destData, int destOffset, int srcRGB, int extraAlpha) {
		int destRGB = destData[destOffset];
		int destR = (destRGB >> 16) & 0xff;
		int destG = (destRGB >> 8) & 0xff;
		int destB = destRGB & 0xff;
		int destA = destRGB >>> 24;
			int srcR = (srcRGB >> 16) & 0xff;
			int srcG = (srcRGB >> 8) & 0xff;
			int srcB = srcRGB & 0xff;

			destR = (srcR * destA)>>8;
			destG = (srcG * destA)>>8;
			destB = (srcB * destA)>>8;

			if (destOpaque) {
				destData[destOffset] = (extraAlpha<<24) | (destR << 16) | (destG << 8) | destB;
			}
			else {
				destA = (extraAlpha * destA)>>8;
				destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;    
			}
	}

	//Ar = As*Ad
	//Cr = Cs*Ad
	private void blendPixel(int[] destData, int destOffset, int srcARGB) {
		int destRGB = destData[destOffset];
		int destR = (destRGB >> 16) & 0xff;
		int destG = (destRGB >> 8) & 0xff;
		int destB = destRGB & 0xff;
		int destA = destRGB >>> 24;
				int srcA = srcARGB >>> 24;
				int srcR = (srcARGB >> 16) & 0xff;
				int srcG = (srcARGB >> 8) & 0xff;
				int srcB = srcARGB & 0xff;

				if (destOpaque) {
					destR = srcR;
					destG = srcG;
					destB = srcB;
					destA = srcA;
					destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;
				}
				else {
					destR = (srcR * destA)>>8;
					destG = (srcG * destA)>>8;
					destB = (srcB * destA)>>8;
					destA = (srcA * destA)>>8;
					destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;    
				}

	}

	// Ar = As*Ae*Ad
	// Cr = Cs*Ae*Ad
	private void blendPixel(int[] destData, int destOffset, int srcARGB, int extraAlpha) {
		int destRGB = destData[destOffset];
		int destR = (destRGB >> 16) & 0xff;
		int destG = (destRGB >> 8) & 0xff;
		int destB = destRGB & 0xff;
		int destA = destRGB >>> 24;
					int srcA = srcARGB >>> 24;
					int srcR = (srcARGB >> 16) & 0xff;
					int srcG = (srcARGB >> 8) & 0xff;
					int srcB = srcARGB & 0xff;

					if (destOpaque) {
						destR = (srcR * extraAlpha)>>8;
						destG = (srcG * extraAlpha)>>8;
						destB = (srcB * extraAlpha)>>8;
						destA = (srcA * extraAlpha)>>8;
						destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;
					}
					else {
						destR = (srcR * extraAlpha * destA)>>16;
						destG = (srcG * extraAlpha * destA)>>16;
						destB = (srcB * extraAlpha * destA)>>16;
						destA = (srcA * extraAlpha * destA)>>16;
						destData[destOffset] = (destA << 24) | (destR << 16) | (destG << 8) | destB;
					}
	}

    /*
        DO NOT EDIT BELOW HERE
        The blend() methods need to be identical in all subclasses of Composite. Ideally, the
        blend() methods belong in the parent class Composite. However, if that were the case,
        calls to blendPixel() would result in an virtual method call - dramatically slowing
        down the rendering.

        The blend() code is cut-and-pasted in each subclass of Composite to get HotSpot to inline
        calls to blendPixel()
    */

    void blend(int[] destData, int destOffset, int srcARGB) {
        blendPixel(destData, destOffset, srcARGB);
    }

    void blend(int[] destData, int destOffset, int srcARGB, int extraAlpha) {
        blendPixel(destData, destOffset, srcARGB, extraAlpha);
    }

    void blendRow(int[] destData, int destOffset, int srcARGB, int numPixels) {
        if ((srcARGB >>> 24) == 0xff) {
            for (int i = 0; i < numPixels; i++) {
                blendOpaquePixel(destData, destOffset++, srcARGB);
            }
        }
        else {
            for (int i = 0; i < numPixels; i++) {
                blendPixel(destData, destOffset++, srcARGB);
            }
        }
    }

    void blend(int[] srcData, int srcScanSize, boolean srcOpaque, int edgeClamp,
        int srcX, int srcY, int srcWidth, int srcHeight,
        int u, int v, int du, int dv,
        boolean rotation,
        boolean renderBilinear, int renderAlpha,
        int[] destData, int destScanSize, int destOffset, int numPixels, int numRows)
    {
        if (renderAlpha <= 0) {
            return;
        }

        //
        // Pre-calc for bilinear scaling
        //

        int srcOffset = 0;
        int offsetTop = 0;
        int offsetBottom = 0;
        if (!rotation) {
            if (renderBilinear) {
                int imageY = v >> 16;
                if (srcOpaque) {
                    if (imageY >= srcHeight - 1) {
                        offsetTop = srcX + (srcY + srcHeight - 1) * srcScanSize;
                        offsetBottom = offsetTop;
                    }
                    else if (imageY < 0) {
                        offsetTop = srcX + srcY * srcScanSize;
                        offsetBottom = offsetTop;
                    }
                    else if ((v & 0xffff) == 0) {
                        offsetTop = srcX + (srcY + (v >> 16)) * srcScanSize;
                        offsetBottom = offsetTop;
                    }
                    else {
                        offsetTop = srcX + (srcY + (v >> 16)) * srcScanSize;
                        offsetBottom = offsetTop + srcScanSize;
                    }
                }
                else {
                    if (imageY >= 0) {
                        if (imageY < srcHeight - 1) {
                            offsetTop = srcX + (srcY + (v >> 16)) * srcScanSize;
                            offsetBottom = offsetTop + srcScanSize;
                        }
                        else if ((edgeClamp & CoreGraphics.EDGE_CLAMP_BOTTOM) != 0) {
                            offsetTop = srcX + (srcY + srcHeight - 1) * srcScanSize;
                            offsetBottom = offsetTop;
                        }
                        else if (v < (srcHeight << 16)) {
                            offsetTop = srcX + (srcY + (v >> 16)) * srcScanSize;
                            offsetBottom = -1;
                        }
                        else {
                            offsetTop = -1;
                            offsetBottom = -1;
                        }
                    }
                    else if ((edgeClamp & CoreGraphics.EDGE_CLAMP_TOP) != 0) {
                        offsetBottom = srcX + srcY * srcScanSize;
                        offsetTop = offsetBottom;
                    }
                    else if (v > -(1<<16)) {
                        offsetBottom = srcX + srcY * srcScanSize;
                        offsetTop = -1;
                    }
                    else {
                        offsetTop = -1;
                        offsetBottom = -1;
                    }
                }
            }
            else {
                srcOffset = srcX;
                if ((u >> 16) >= srcWidth - 1) {
                    srcOffset += srcWidth - 1;
                }
                else if (u > 0) {
                    srcOffset += (u >> 16);
                }

                if ((v >> 16) >= srcHeight - 1) {
                    srcOffset += (srcY + srcHeight - 1) * srcScanSize;
                }
                else if (v >= 0) {
                    srcOffset += (srcY + (v >> 16)) * srcScanSize;
                }
            }
        }

        //
        // Opaque blending
        //

        if (renderAlpha == 255) {
            if (rotation) {
                // TODO: handle opaque images seperately?
                if (renderBilinear) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData,
                                srcScanSize, srcX, srcY, srcWidth, srcHeight, edgeClamp, u, v);
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendOpaquePixel(destData, destOffset, srcARGB);
                        }
                        else if (BLEND_TRANSPARENT_PIXELS || srcAlpha != 0) {
                            blendPixel(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
                else {
                    srcOffset = srcX + srcY * srcScanSize;
                    for (int i = 0; i < numPixels; i++) {
                        int x = CoreMath.clamp(u >> 16, 0, srcWidth - 1);
                        int y = CoreMath.clamp(v >> 16, 0, srcHeight - 1);
                        int srcARGB = srcData[srcOffset + x + y * srcScanSize];
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendOpaquePixel(destData, destOffset, srcARGB);
                        }
                        else if (BLEND_TRANSPARENT_PIXELS || srcAlpha != 0) {
                            blendPixel(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
            }
            else if (renderBilinear) {
                if (srcOpaque) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearOpaque(srcData,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        blendOpaquePixel(destData, destOffset, srcARGB);
                        destOffset++;
                        u += du;
                    }
                }
                else {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData, edgeClamp,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        int srcAlpha = srcARGB >>> 24;
                        if (srcAlpha == 0xff) {
                            blendOpaquePixel(destData, destOffset, srcARGB);
                        }
                        else if (BLEND_TRANSPARENT_PIXELS || srcAlpha != 0) {
                            blendPixel(destData, destOffset, srcARGB);
                        }
                        destOffset++;
                        u += du;
                    }
                }
            }
            else {
                for (int y = 0; y < numRows; y++) {
                    if (du != (1 << 16)) {
                        int offset = u & 0xffff;
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                blendOpaquePixel(destData, destOffset + i, srcARGB);
                                offset += du;
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                int srcAlpha = srcARGB >>> 24;
                                if (srcAlpha == 0xff) {
                                    blendOpaquePixel(destData, destOffset + i, srcARGB);
                                }
                                else if (BLEND_TRANSPARENT_PIXELS || srcAlpha != 0) {
                                    blendPixel(destData, destOffset + i, srcARGB);
                                }
                                offset += du;
                            }
                        }
                    }
                    else {
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                blendOpaquePixel(destData, destOffset + i, srcARGB);
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                int srcAlpha = srcARGB >>> 24;
                                if (srcAlpha == 0xff) {
                                    blendOpaquePixel(destData, destOffset + i, srcARGB);
                                }
                                else if (BLEND_TRANSPARENT_PIXELS || srcAlpha != 0) {
                                    blendPixel(destData, destOffset + i, srcARGB);
                                }
                            }
                        }
                    }
                    destOffset += destScanSize;
                    srcOffset += srcScanSize;
                }
            }
        }

        //
        // Translucent blending
        //

        else {
            if (rotation) {
                // TODO: handle opaque images seperately?
                if (renderBilinear) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData,
                                srcScanSize, srcX, srcY, srcWidth, srcHeight, edgeClamp, u, v);
                        if (BLEND_TRANSPARENT_PIXELS || srcARGB != 0) {
                            blendPixel(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
                else {
                    srcOffset = srcX + srcY * srcScanSize;
                    for (int i = 0; i < numPixels; i++) {
                        int x = CoreMath.clamp(u >> 16, 0, srcWidth - 1);
                        int y = CoreMath.clamp(v >> 16, 0, srcHeight - 1);
                        int srcARGB = srcData[srcOffset + x + y * srcScanSize];
                        if (BLEND_TRANSPARENT_PIXELS || srcARGB != 0) {
                            blendPixel(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                        v += dv;
                    }
                }
            }
            else if (renderBilinear) {
                if (srcOpaque) {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearOpaque(srcData,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        blendOpaquePixel(destData, destOffset, srcARGB, renderAlpha);
                        destOffset++;
                        u += du;
                    }
                }
                else {
                    for (int i = 0; i < numPixels; i++) {
                        int srcARGB = getPixelBilinearTranslucent(srcData, edgeClamp,
                            offsetTop, offsetBottom, u, v, srcWidth);
                        if (BLEND_TRANSPARENT_PIXELS || srcARGB != 0) {
                            blendPixel(destData, destOffset, srcARGB, renderAlpha);
                        }
                        destOffset++;
                        u += du;
                    }
                }
            }
            else {
                for (int y = 0; y < numRows; y++) {
                    if (du != (1 << 16)) {
                        int offset = u & 0xffff;
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                blendOpaquePixel(destData, destOffset + i, srcARGB, renderAlpha);
                                offset += du;
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + (offset >> 16)];
                                if (BLEND_TRANSPARENT_PIXELS || srcARGB != 0) {
                                    blendPixel(destData, destOffset + i, srcARGB, renderAlpha);
                                }
                                offset += du;
                            }
                        }
                    }
                    else {
                        if (srcOpaque) {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                blendOpaquePixel(destData, destOffset + i, srcARGB, renderAlpha);
                            }
                        }
                        else {
                            for (int i = 0; i < numPixels; i++) {
                                int srcARGB = srcData[srcOffset + i];
                                if (BLEND_TRANSPARENT_PIXELS || srcARGB != 0) {
                                    blendPixel(destData, destOffset + i, srcARGB, renderAlpha);
                                }
                            }
                        }
                    }
                    destOffset += destScanSize;
                    srcOffset += srcScanSize;
                }
            }
        }
    }
}


