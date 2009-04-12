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

import pulpcore.animation.Color;
import pulpcore.animation.Int;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
 * A edge Stroke filter. <p>
 * An example can be found here: 
 * http://www.webdesign.org/web/photoshop/photoshop-basics/stroke-a-stroke-in-photoshop.14342.html
 * 
 * Example of use (to obtain the same effect as the above PSD example):
 * <pre>
 * ImageSprite sprite = new ImageSprite("star.png", 50, 50);
 * FilterChain chain = new FilterChain(new Stroke(0xff289acd, 7), new Stroke(0xFF00679A, 7));
 * sprite.setFilter(chain);
 * add(sprite);
 * </pre>
 * @author Florent Dupont
 */
public final class Stroke extends Filter {

    private final static int DEFAULT_COLOR = 0xff000000;
    private final static boolean ANTI_ALIAS = true;

    /**
        The stroke color. The default color is black.
    */
    public final Color color = new Color(DEFAULT_COLOR);

    /**
        The stroke size. The minimum value is 1. The default value is 5.
    */
    public final Int size = new Int(5);

    private int actualColor = 0;
    private int actualRadius = -1;
    private int fillIntensity;
    private int[] precalculatedIntensities;
    private int[] colorTable;

    private Stroke(Stroke filter) {
        size.bindWithInverse(filter.size);
        color.bindWithInverse(filter.color);
    }

    /**
    Creates a Stroke filter with the default color (black) and a size of 5.
     */
    public Stroke() {
        this(DEFAULT_COLOR);
    }

    /**
    Creates a Stroke filter with the specified color and a size of 5.
     */
    public Stroke(int color) {
        this(color, 5);
    }

    /**
    Creates a Stroke filter with the specified color and size.
     */
    public Stroke(int color, int radius) {
        this.color.set(color);
        this.size.set(radius);
    }

    public Filter copy() {
        return new Stroke(this);
    }

    public void update(int elapsedTime) {

        super.update(elapsedTime);

        color.update(elapsedTime);
        size.update(elapsedTime);

        if (color.get() != actualColor || colorTable == null) {
            if (colorTable == null) {
                colorTable = new int[256];
            }
            actualColor = color.get();
            setDirty();
            int rgb = Colors.rgb(color.get());
            int alpha = Colors.getAlpha(color.get());
            for (int i = 0; i < 256; i++) {
                colorTable[i] = Colors.premultiply(rgb, i);
            }
        }

        // Clamp: Getting error for size > 180 or so.
        int r = CoreMath.clamp(size.get() - 1, 0, 180);
        if (actualRadius != r) {
            actualRadius = r;
            setDirty();
            precalculatedIntensities = precalculateIntensities(actualRadius);
        }
    }

    public boolean isOpaque() {
        return false;
    }

    public int getX() {
        return -(actualRadius + 1);
    }

    public int getY() {
        return -(actualRadius + 1);
    }

    public int getWidth() {
        return super.getWidth() + (actualRadius + 1) * 2;
    }

    public int getHeight() {
        return super.getHeight() + (actualRadius + 1) * 2;
    }

    protected void filter(CoreImage src, CoreImage dst) {

        int xOffset = getX();
        int yOffset = getY();

        int[] srcData = src.getData();
        int[] dstData = dst.getData();
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int srcSize = srcWidth * srcHeight;
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();

        CoreGraphics g = dst.createGraphics();
        g.clear();
        
        if (srcSize == 0) {
            return;
        }

        int colorAlpha = actualColor >>> 24;
        int colorPremultiplied = Colors.premultiply(actualColor);
        boolean antiAlias = ANTI_ALIAS && actualRadius <= 16 && srcWidth >= 2 && srcHeight >= 2;
        if (colorAlpha > 0) {

            int srcOffset = 0;
            int dstOffset = -xOffset - yOffset * dstWidth;
            if (antiAlias) {
                for (int j = 0; j < srcWidth; j++) {
                    // Top edge anti-alias
                    int a = srcData[srcOffset + j] >>> 24;
                    if (a > 0) {
                        fillIntensity = (a * colorAlpha) >> 8;
                        wuAntialiasedCircle(dstData, dstWidth, dstHeight,
                                j - xOffset, -1 - yOffset, actualRadius);
                    }

                    // Bottom edge anti-alias
                    a = srcData[srcOffset + j + srcSize - srcWidth] >>> 24;
                    if (a > 0) {
                        fillIntensity = (a * colorAlpha) >> 8;
                        wuAntialiasedCircle(dstData, dstWidth, dstHeight,
                                j - xOffset, srcHeight - yOffset, actualRadius);
                    }
                }
            }
            for (int i = 0; i < srcHeight; i++) {
                if (antiAlias) {
                    // Left edge anti-alias
                    int a = srcData[srcOffset] >>> 24;
                    if (a > 0) {
                        fillIntensity = (a * colorAlpha) >> 8;
                        wuAntialiasedCircle(dstData, dstWidth, dstHeight,
                                -1 - xOffset, i - yOffset, actualRadius);
                    }

                    // Right edge anti-alias
                    a = srcData[srcOffset + srcWidth - 1] >>> 24;
                    if (a > 0) {
                        fillIntensity = (a * colorAlpha) >> 8;
                        wuAntialiasedCircle(dstData, dstWidth, dstHeight,
                                srcWidth - xOffset, i - yOffset, actualRadius);
                    }
                }
                for (int j = 0; j < srcWidth; j++) {
                    int p = srcData[srcOffset];

                    if (p == 0) {
                        if (antiAlias) {
                            int a = 0;
                            // Antialiased edge: Fill circle with the highest intensity of neighbor pixels
                            if (j > 0) {
                                a = Math.max(a, srcData[srcOffset - 1] >>> 24);
                            }
                            if (j < srcWidth - 1) {
                                a = Math.max(a, srcData[srcOffset + 1] >>> 24);
                            }
                            if (i > 0) {
                                a = Math.max(a, srcData[srcOffset - srcWidth] >>> 24);
                            }
                            if (i < srcHeight - 1) {
                                a = Math.max(a, srcData[srcOffset + srcWidth] >>> 24);
                            }
                            if (a > 0) {
                                fillIntensity = (a * colorAlpha) >> 8;
                                wuAntialiasedCircle(dstData, dstWidth, dstHeight,
                                        j - xOffset, i - yOffset, actualRadius);
                            }
                        }
                    }
                    else {
                        // Non-transparent: fill underneath
                        dstData[dstOffset] = colorPremultiplied;

                        // If next to a transparent pixel, fill the circle
                        boolean isBorder = false;
                        if (i == 0 || j == 0 || i == srcHeight - 1 || j == srcWidth - 1) {
                            isBorder = true;
                        }
                        else {
                            isBorder = srcData[srcOffset - 1] == 0 ||
                                srcData[srcOffset + 1] == 0 ||
                                srcData[srcOffset - srcWidth] == 0 ||
                                srcData[srcOffset + srcWidth] == 0;
                        }
                        if (isBorder) {
                            fillIntensity = colorAlpha;
                            wuAntialiasedFilledCircle(dstData, dstWidth, dstHeight,
                                    j - xOffset, i - yOffset, actualRadius);
                        }
                    }

                    srcOffset++;
                    dstOffset++;
                }
                dstOffset += dstWidth - srcWidth;
            }
        }

        // Draw the input image on top of the stroke
        g.drawImage(src, -xOffset, -yOffset);
    }

    /**
     * precalculate the D(r, j) intensity values.
     * precalculation stays the same for a given radius.
     * Make sure to re-precalculate if the radius changes.
     * int this precalculation j goes from 1 to  r / sqrt(2)
     * @param radius
     * @return an int array containing precalcs.
     */
    private final int[] precalculateIntensities(int radius) {

        // radius stays the same.
        // make sure to re-precalculate if the radius changes
        // j from 1 to  r / sqrt(2)
        //int maxJ = (int)Math.ceil(radius * 1.0d / 1.4142135623d);
        int[] result = new int[radius + 1];

        for (int j = 1; j <= radius; j++) {
            result[j] = calculateIntensity(radius, j);
        }
        return result;
    }

    /**
     * used for debug purpose.
     * May be useful to place this in CoreGraphic ...
     */
    private final void wuAntialiasedCircle(int[] pixels, int width, int height, int xCenter, int yCenter, int radius) {

        int i = radius;
        int t = 0;

        drawOctants(fillIntensity, pixels, width, height, radius, xCenter, yCenter);

        for (int j = 1; j < i; j++) {
            int d = precalculatedIntensities[j]; //precalculated D(r, j)
            if (d < t) {
                i--;
            }
            if (i < j) {
                break;
            }

            int fd = (d * fillIntensity) >> 8;
            drawOctants(fillIntensity - fd, pixels, width, height, i, j, xCenter, yCenter);
            if (i != j) {
                drawOctants(fd, pixels, width, height, i - 1, j, xCenter, yCenter);
            }
            t = d;
        }

    }

    /**
     *  May be useful to place this in CoreGraphic ...
     */
    private final void wuAntialiasedFilledCircle(int[] pixels, int width, int height, int xCenter, int yCenter, int radius) {

        int i = radius;
        int t = 0;

        // fills the inside lines.
        fillOctants(fillIntensity, pixels, width, height, 0, 0, xCenter, yCenter, i + 1);

        for (int j = 1; j < i; j++) {
            int d = precalculatedIntensities[j]; //precalculated D(r, j)
            if (d < t) {
                i--;
            }
            if (i < j) {
                break;
            }

            int intensity = ((255 - d) * fillIntensity) >> 8;
            drawOctants(intensity, pixels, width, height, i, j, xCenter, yCenter);
            t = d;

            // filling the inside lines
            fillOctants(fillIntensity, pixels, width, height, j, j, xCenter, yCenter, i - j);
        }
    }

    /**
     *  Wu's intensity calculation.
     *  Equivalent of D(r, j) in Wu's algorithm
     *  Math formula is given in Graphics Gem II (p. 446)
     *
     *  Each values is precalculater performances issues and available with {@link #precalculateIntensities(int)}.
     *  Fixed point values are used to increase calculation.
     *  The equivalent in using double :
     *    double r2j2 = (r * r)  - (j * j);
     *	  double pr = Math.ceil(Math.sqrt(r2j2)) - Math.sqrt(r2j2);
     *	  double result = Math.floor(255 * pr + 0.5);
     *
     */
    private int calculateIntensity(int r, int j) {
        int r2j2_fixed = CoreMath.toFixed(r * r) - CoreMath.toFixed(j * j);
        int r2j2sqrt_fixed = CoreMath.sqrt(r2j2_fixed);
        int pr_fixed = CoreMath.ceil(r2j2sqrt_fixed) - r2j2sqrt_fixed;
        int result_fixed = (255 * pr_fixed + (CoreMath.ONE / 2));
        return CoreMath.toIntFloor(result_fixed);
    }

    private final void drawOctants(int intensity, int[] pixels, int width, int height, int y, int xCenter, int yCenter) {
        setPixel(intensity, pixels, width, height, xCenter, yCenter + y);
        setPixel(intensity, pixels, width, height, xCenter, yCenter - y);
        if (y != 0) {
            setPixel(intensity, pixels, width, height, xCenter + y, yCenter);
            setPixel(intensity, pixels, width, height, xCenter - y, yCenter);
        }
    }

    /**
     * Draws every circle octants with a specified intensity.
     * Equivalent of I(i, j) in Wu's algorithm
     */
    private final void drawOctants(int intensity, int[] pixels, int width, int height, int x, int y, int xCenter, int yCenter) {
        setPixel(intensity, pixels, width, height, xCenter + x, yCenter + y);
        setPixel(intensity, pixels, width, height, xCenter - x, yCenter + y);
        setPixel(intensity, pixels, width, height, xCenter + x, yCenter - y);
        setPixel(intensity, pixels, width, height, xCenter - x, yCenter - y);
        if (x != y) {
            setPixel(intensity, pixels, width, height, xCenter + y, yCenter + x);
            setPixel(intensity, pixels, width, height, xCenter - y, yCenter + x);
            setPixel(intensity, pixels, width, height, xCenter + y, yCenter - x);
            setPixel(intensity, pixels, width, height, xCenter - y, yCenter - x);
        }
    }

    private final void fillOctants(int intensity, int[] pixels, int width, int height, int x, int y, int xCenter, int yCenter, int length) {
        setPixelRow(intensity, pixels, width, height, xCenter + x, yCenter + y, length, 1);
        setPixelRow(intensity, pixels, width, height, xCenter - x, yCenter + y, length, -1);
        setPixelRow(intensity, pixels, width, height, xCenter + x, yCenter - y, length, 1);
        setPixelRow(intensity, pixels, width, height, xCenter - x, yCenter - y, length, -1);
        setPixelRow(intensity, pixels, width, height, xCenter + y, yCenter + x, length, width);
        setPixelRow(intensity, pixels, width, height, xCenter - y, yCenter + x, length, width);
        setPixelRow(intensity, pixels, width, height, xCenter + y, yCenter - x, length, -width);
        setPixelRow(intensity, pixels, width, height, xCenter - y, yCenter - x, length, -width);
    }

    private final void setPixel(int intensity, int[] pixels, int width, int height, int x, int y) {

        int offset = (y * width) + x;
        int ARGB = pixels[offset];

        if ((ARGB >>> 24) < intensity) {
            pixels[offset] = colorTable[intensity];
        }
    }

    private final void setPixelRow(int intensity, int[] pixels, int width, int height, int x, int y, int length, int dOffset) {

        int offset = (y * width) + x;
        for (int i = 0; i < length; i++) {
            int ARGB = pixels[offset];
            if ((ARGB >>> 24) < intensity) {
                pixels[offset] = colorTable[intensity];
            }
            offset += dOffset;
        }
    }
}
