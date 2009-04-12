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


import pulpcore.animation.Bool;
import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/*
 Inspired by Florent Dupont who was inspired by Romain Guy. Changes:
 * Fractional box size (1/8th pixel resolution)
 * No temporary buffers if quality == 1
 */
/**
    A blur filter.
    <p>
    For an opaque input image (for example, a photo), the output image has the same dimensions as
    the input. For a non-opaque input image, the output image is expanded to show the complete blur.
*/
public class Blur extends Filter {

    private static final int MAX_RADIUS = 255;

    private static final boolean INTEGER_ONLY = false;

    private static final boolean CPU_TEST = false;

    private static final int BORDER_COLOR = Colors.TRANSPARENT;

    // Work buffers. Total overhead is (w * 4 * 4 + 256 * 3 * 4) bytes
    // Where w = (maxImageWidth + (MAX_RADIUS+1)*2+1)
    private static int[] columnSumA = new int[1024];
    private static int[] columnSumR = new int[1024];
    private static int[] columnSumG = new int[1024];
    private static int[] columnSumB = new int[1024];
    private static final int[] colorTable = new int[256];
    private static final int[] colorTablef = new int[256];
    private static final int[] colorTable1f = new int[256];
    private static final Object lock = new Object();

    /**
        The blur radius. The radius can range from 0 (no blur) to 255. The default value is 4.
        Integer values are optimized to render slightly faster.
    */
    public final Fixed radius = new Fixed(4);

    /**
        The number of times to perform the blur. The default value is 3 
        (Gaussian approximation). A value of 1 renders faster.
     */
    public final Int quality = new Int(1);

    // This is a property only so it can be bound properly in copy()
    private final Bool autoExpand = new Bool(true);

    private int actualRadius = 0;
    private int actualQuality = 0;
    private int time;
    private int lastUpdateTime;
    // For quality > 1
    private CoreImage workBuffer;

    /**
        Creates a blur filter with a radius of 4 and a quality of 1.
     */
    public Blur() {
        this(4, 3);
    }

    /**
        Creates a blur filter with the specified radius, from 0 (no blur) to 255, and a quality of 1.
        Integer values are optimized to render slightly faster.
     */
    public Blur(float radius) {
        this(radius, 3);
    }

    /**
        Creates a blur filter with the specified radius, from 0 (no blur) to 255, and
        the specified quality, typically from 1 (default) to 3 (Guassian approximation).
     */
    public Blur(float radius, int quality) {
        this.radius.set(radius);
        this.quality.set(quality);
    }

    /**
        Copy constructor. Subclasses can use this to help implement {@link #copy() }.
    */
    public Blur(Blur filter) {
        autoExpand.bindWithInverse(filter.autoExpand);
        radius.bindWithInverse(filter.radius);
        quality.bindWithInverse(filter.quality);
    }

    /**
        Sets whether the edges of the output is clamped (sharp edges). The default is false
        (blurry edges).
        @see #getClampEdges()
     */
    public void setClampEdges(boolean clamp) {
        boolean newAutoExpand = !clamp;
        if (autoExpand.get() != newAutoExpand) {
            autoExpand.set(newAutoExpand);
            setDirty();
        }
    }

    /**
        Gets whether the edges of the output is clamped (sharp edges). The default is false
        (blurry edges).
        @see #setClampEdges(boolean)
     */
    public boolean getClampEdges() {
        return !autoExpand.get();
    }

    public Filter copy() {
        return new Blur(this);
    }

    public void update(int elapsedTime) {

        time += elapsedTime;
        radius.update(elapsedTime);
        quality.update(elapsedTime);

        int r = CoreMath.clamp(radius.getAsFixed(), 0, CoreMath.toFixed(MAX_RADIUS));
        if (INTEGER_ONLY) {
            r &= 0xffff0000;
        }
        else {
            // Round to nearest 1/8th a pixel (granularity of fmul() functions)
            r &= 0xffffe000;
        }

        int q = CoreMath.clamp(quality.get(), 0, 15);
        if (isDirty() || actualQuality != q || CPU_TEST) {
            actualQuality = q;
            actualRadius = r;
            setDirty();
        }
        else if (actualRadius != r) {
            // Update at a limited frame rate, depending on the radius
            int timeLimit = 0;
            if (r < CoreMath.toFixed(4)) {
                timeLimit = 16; // ~60 fps
            }
            else if (r < CoreMath.toFixed(8)) {
                timeLimit = 22; // ~45 fps
            }
            else if (r < CoreMath.toFixed(16)) {
                timeLimit = 32; // ~30 fps
            }
            else if (r < CoreMath.toFixed(32)) {
                timeLimit = 40; // ~25 fps
            }
            else if (r < CoreMath.toFixed(64)) {
                timeLimit = 50; // ~20 fps
            }
            else {
                timeLimit = 64; // ~15 fps
            }

            if (time - lastUpdateTime >= timeLimit) {

                // If the change crossed an integer boundary, use the int value.
                if (r < actualRadius) {
                    if (CoreMath.toIntCeil(r) != CoreMath.toIntCeil(actualRadius)) {
                        r = CoreMath.ceil(r);
                    }
                }
                else {
                    if (CoreMath.toIntFloor(r) != CoreMath.toIntFloor(actualRadius)) {
                        r = CoreMath.floor(r);
                    }
                }
                actualRadius = r;
                setDirty();
            }
        }
    }

    private boolean isInputOpaque() {
        return getInput() == null ? false : getInput().isOpaque();
    }

    private int autoExpandSize() {
        // Ceil to nearest 4px so that new buffers don't have to be created as often.
        // This formula works since 0 <= actualRadius <= 255 and the granularity is 1/8th px.
        return CoreMath.toIntCeil((actualRadius * actualQuality) >> 2) << 2;
    }

    public int getX() {
        return autoExpand.get() ? -autoExpandSize() : 0;
    }

    public int getY() {
        return autoExpand.get() ? -autoExpandSize() : 0;
    }

    public int getWidth() {
        return super.getWidth() + (autoExpand.get() ? autoExpandSize()*2 : 0);
    }

    public int getHeight() {
        return super.getHeight() + (autoExpand.get() ? autoExpandSize()*2 : 0);
    }
    
    public boolean isOpaque() {
        return (isInputOpaque() && !autoExpand.get());
    }

    protected void filter(CoreImage input, CoreImage output) {
        // If true, the outside of the image is the same pixels as the border.
        // If false, the outside of the image is considered BORDER_COLOR.
        boolean clamp = isOpaque();
        filter(input, output, 0, 0, clamp);
    }

    protected void filter(CoreImage input, CoreImage output, int shiftX, int shiftY, boolean clamp) {
        lastUpdateTime = time;
        if ((actualRadius <= 0 || actualQuality <= 0) && !CPU_TEST) {
            if (input.getWidth() == output.getWidth() &&
                    input.getHeight() == output.getHeight())
            {
                System.arraycopy(input.getData(), 0, output.getData(), 0,
                    input.getWidth() * input.getHeight());
            }
            else {
                CoreGraphics g = output.createGraphics();
                g.clear();
                g.drawImage(input, shiftX - getX(), shiftY - getY());
            }
        }
        else {
            if (actualQuality > 1) {
                if (workBuffer == null ||
                        workBuffer.getWidth() != output.getWidth() ||
                        workBuffer.getHeight() != output.getHeight() ||
                        workBuffer.isOpaque() != output.isOpaque())
                {
                    workBuffer = new CoreImage(output.getWidth(), output.getHeight(),
                            output.isOpaque());
                }
            }
            else {
                workBuffer = null;
            }

            // Synchronized because it uses shared static buffers.
            // A thread-safe implementation might store the buffers as a thread-local variable instead.
            synchronized (lock) {
                int w = output.getWidth() + (MAX_RADIUS+1)*2+1;
                if (w > columnSumA.length) {
                    columnSumA = new int[w];
                    columnSumR = new int[w];
                    columnSumG = new int[w];
                    columnSumB = new int[w];
                }
                CoreImage src = input;
                CoreImage dst = ((actualQuality & 1) == 1) ? output : workBuffer;
                CoreImage wrk = ((actualQuality & 1) == 1) ? workBuffer : output;
                int ox = shiftX - getX();
                int oy = shiftY - getY();
                for (int i = 0; i < actualQuality; i++) {
                    filter(src, dst, actualRadius, ox, oy, clamp);
                    src = dst;
                    dst = wrk;
                    wrk = src;
                    ox = 0;
                    oy = 0;
                }
            }
        }
    }

    private void filter(CoreImage src, CoreImage dst, final int r, 
            final int offsetX, final int offsetY, final boolean clamp)
    {
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();
        final int[] srcData = src.getData();
        final int[] dstData = dst.getData();

        final int rInt = CoreMath.toIntFloor(r);
        final int f = CoreMath.fracPart(r);
        final long windowLength = r * 2 + CoreMath.ONE;
        final long windowArea = CoreMath.mul(windowLength, windowLength);
        final int columnOffset = rInt + 1;

        for (int i = 0; i < 256; i++) {
            int c = CoreMath.clamp(i, 0, 255);
            int v = ((c << 16) | (c << 8) | c);

            colorTable[i] = (int)CoreMath.div(v, windowArea);
            colorTablef[i] = (int)CoreMath.mulDiv(v, f, windowArea);
            colorTable1f[i] = colorTable[i] - colorTablef[i];
        }

        // Setup column sums for first destination row

        int limit = (f == 0 || INTEGER_ONLY) ? rInt : rInt + 1;
        for (int x = -columnOffset; x < dstWidth + columnOffset; x++) {
            int index = x + columnOffset;
            int srcX = x - offsetX;
            if (clamp) {
                srcX = CoreMath.clamp(srcX, 0, srcWidth-1);
            }

            columnSumA[index] = 0;
            columnSumR[index] = 0;
            columnSumG[index] = 0;
            columnSumB[index] = 0;

            for (int i = -limit; i <= limit; i++) {
                int srcY = i - offsetY;
                if (clamp) {
                    srcY = CoreMath.clamp(srcY, 0, srcHeight-1);
                }

                int pixel;
                boolean isValidSrcX = (srcX >= 0 && srcX < srcWidth);
                boolean isValidSrcY = (srcY >= 0 && srcY < srcHeight);
                if (!clamp && (!isValidSrcX || !isValidSrcY)) {
                    pixel = BORDER_COLOR;
                }
                else {
                    pixel = srcData[srcX + srcY * srcWidth];
                }
                int[] table = (!INTEGER_ONLY && f != 0 && Math.abs(i) == limit) ? colorTablef : colorTable;
                columnSumA[index] += table[(pixel >>> 24)];
                columnSumR[index] += table[((pixel >> 16) & 0xff)];
                columnSumG[index] += table[((pixel >> 8) & 0xff)];
                columnSumB[index] += table[(pixel & 0xff)];
            }
        }

        int dstRowIndex = 0;
        for (int y = 0; y < dstHeight; y++) {

            if (f == 0 || INTEGER_ONLY) {
                // Setup pixel sum for (0, y)
                int sumA = 0;
                int sumR = 0;
                int sumG = 0;
                int sumB = 0;
                for (int i = -rInt; i <= rInt; i++) {
                    sumA += columnSumA[columnOffset + i];
                    sumR += columnSumR[columnOffset + i];
                    sumG += columnSumG[columnOffset + i];
                    sumB += columnSumB[columnOffset + i];
                }

                // Write this row
                int dstIndex = dstRowIndex;
                int prevX = columnOffset - rInt;
                int nextX = columnOffset + rInt + 1;
                for (int x = 0; x < dstWidth; x++) {
                    dstData[dstIndex++] =
                            ((sumA & 0xff0000) << 8) |
                            ((sumR & 0xff0000)) |
                            ((sumG & 0xff0000) >> 8) |
                            ((sumB & 0xff0000) >> 16);

                    // Sutract last column, add next column
                    sumA += columnSumA[nextX] - columnSumA[prevX];
                    sumR += columnSumR[nextX] - columnSumR[prevX];
                    sumG += columnSumG[nextX] - columnSumG[prevX];
                    sumB += columnSumB[nextX] - columnSumB[prevX];
                    prevX++;
                    nextX++;
                }
            }
            else {
                // Setup pixel sum for (0, y)
                int sumA = fmul(f, columnSumA[columnOffset - rInt - 1]) + fmul(f, columnSumA[columnOffset + rInt + 1]);
                int sumR = fmul(f, columnSumR[columnOffset - rInt - 1]) + fmul(f, columnSumR[columnOffset + rInt + 1]);
                int sumG = fmul(f, columnSumG[columnOffset - rInt - 1]) + fmul(f, columnSumG[columnOffset + rInt + 1]);
                int sumB = fmul(f, columnSumB[columnOffset - rInt - 1]) + fmul(f, columnSumB[columnOffset + rInt + 1]);
                for (int i = -rInt; i <= rInt; i++) {
                    sumA += columnSumA[columnOffset + i];
                    sumR += columnSumR[columnOffset + i];
                    sumG += columnSumG[columnOffset + i];
                    sumB += columnSumB[columnOffset + i];
                }

                // Write this row
                int dstIndex = dstRowIndex;
                int prevX = columnOffset - rInt;
                int nextX = columnOffset + rInt + 1;
                for (int x = 0; x < dstWidth; x++) {
                    dstData[dstIndex++] =
                            ((sumA & 0xff0000) << 8) |
                            ((sumR & 0xff0000)) |
                            ((sumG & 0xff0000) >> 8) |
                            ((sumB & 0xff0000) >> 16);

                    // Sutract last column, add next column
                    sumA += fmul(f, columnSumA[prevX], columnSumA[prevX-1], columnSumA[nextX], columnSumA[nextX+1]);
                    sumR += fmul(f, columnSumR[prevX], columnSumR[prevX-1], columnSumR[nextX], columnSumR[nextX+1]);
                    sumG += fmul(f, columnSumG[prevX], columnSumG[prevX-1], columnSumG[nextX], columnSumG[nextX+1]);
                    sumB += fmul(f, columnSumB[prevX], columnSumB[prevX-1], columnSumB[nextX], columnSumB[nextX+1]);
                    prevX++;
                    nextX++;
                }
            }

            // Prepare column sums for next row
            int numLoops = (f == 0 || INTEGER_ONLY) ? 1 : 2;
            for (int i = 0; i < numLoops; i++) {
                int[] table = (numLoops == 1) ? colorTable : (i == 0) ? colorTable1f : colorTablef;
                boolean isValidPrevIndex = true;
                boolean isValidNextIndex = true;
                int prevIndex = y - offsetY - rInt - i;
                int nextIndex = y - offsetY + rInt + i + 1;
                if (clamp) {
                    prevIndex = CoreMath.clamp(prevIndex, 0, srcHeight-1);
                    nextIndex = CoreMath.clamp(nextIndex, 0, srcHeight-1);
                }
                else {
                    isValidPrevIndex = prevIndex >= 0 && prevIndex < srcHeight;
                    isValidNextIndex = nextIndex >= 0 && nextIndex < srcHeight;
                }
                prevIndex *= srcWidth;
                nextIndex *= srcWidth;

                int x1 = -columnOffset;
                int x2 = offsetX;
                int x3 = srcWidth + offsetX;
                int x4 = dstWidth + columnOffset;
                int prevPixel;
                int nextPixel;

                // Left side
                if (clamp) {
                    prevPixel = srcData[prevIndex];
                    nextPixel = srcData[nextIndex];
                }
                else {
                    prevPixel = BORDER_COLOR;
                    nextPixel = BORDER_COLOR;
                }
                int pa = prevPixel >>> 24;
                int pr = (prevPixel >> 16) & 0xff;
                int pg = (prevPixel >> 8) & 0xff;
                int pb = prevPixel & 0xff;
                int na = nextPixel >>> 24;
                int nr = (nextPixel >> 16) & 0xff;
                int ng = (nextPixel >> 8) & 0xff;
                int nb = nextPixel & 0xff;
                int da = table[na] - table[pa];
                int dr = table[nr] - table[pr];
                int dg = table[ng] - table[pg];
                int db = table[nb] - table[pb];
                for (int x = x1; x < x2; x++) {
                    int columnIndex = x + columnOffset;

                    // Subtract last row, add next row
                    columnSumA[columnIndex] += da;
                    columnSumR[columnIndex] += dr;
                    columnSumG[columnIndex] += dg;
                    columnSumB[columnIndex] += db;
                }

                // Middle
                if (isValidPrevIndex && isValidNextIndex) {
                    for (int x = x2; x < x3; x++) {
                        int columnIndex = x + columnOffset;
                        int srcX = x - offsetX;
                        prevPixel = srcData[prevIndex + srcX];
                        nextPixel = srcData[nextIndex + srcX];

                        pa = prevPixel >>> 24;
                        pr = (prevPixel >> 16) & 0xff;
                        pg = (prevPixel >> 8) & 0xff;
                        pb = prevPixel & 0xff;
                        na = nextPixel >>> 24;
                        nr = (nextPixel >> 16) & 0xff;
                        ng = (nextPixel >> 8) & 0xff;
                        nb = nextPixel & 0xff;

                        // Subtract last row, add next row
                        columnSumA[columnIndex] += table[na] - table[pa];
                        columnSumR[columnIndex] += table[nr] - table[pr];
                        columnSumG[columnIndex] += table[ng] - table[pg];
                        columnSumB[columnIndex] += table[nb] - table[pb];
                    }
                }
                else {
                    for (int x = x2; x < x3; x++) {
                        int columnIndex = x + columnOffset;
                        int srcX = x - offsetX;
                        prevPixel = isValidPrevIndex ? srcData[prevIndex + srcX] : BORDER_COLOR;
                        nextPixel = isValidNextIndex ? srcData[nextIndex + srcX] : BORDER_COLOR;

                        pa = prevPixel >>> 24;
                        pr = (prevPixel >> 16) & 0xff;
                        pg = (prevPixel >> 8) & 0xff;
                        pb = prevPixel & 0xff;
                        na = nextPixel >>> 24;
                        nr = (nextPixel >> 16) & 0xff;
                        ng = (nextPixel >> 8) & 0xff;
                        nb = nextPixel & 0xff;

                        // Subtract last row, add next row
                        columnSumA[columnIndex] += table[na] - table[pa];
                        columnSumR[columnIndex] += table[nr] - table[pr];
                        columnSumG[columnIndex] += table[ng] - table[pg];
                        columnSumB[columnIndex] += table[nb] - table[pb];
                    }
                }

                // Right side
                if (clamp) {
                    prevPixel = srcData[prevIndex + srcWidth - 1];
                    nextPixel = srcData[nextIndex + srcWidth - 1];
                }
                else {
                    prevPixel = BORDER_COLOR;
                    nextPixel = BORDER_COLOR;
                }
                pa = prevPixel >>> 24;
                pr = (prevPixel >> 16) & 0xff;
                pg = (prevPixel >> 8) & 0xff;
                pb = prevPixel & 0xff;
                na = nextPixel >>> 24;
                nr = (nextPixel >> 16) & 0xff;
                ng = (nextPixel >> 8) & 0xff;
                nb = nextPixel & 0xff;
                da = table[na] - table[pa];
                dr = table[nr] - table[pr];
                dg = table[ng] - table[pg];
                db = table[nb] - table[pb];
                for (int x = x3; x < x4; x++) {
                    int columnIndex = x + columnOffset;

                    // Subtract last row, add next row
                    columnSumA[columnIndex] += da;
                    columnSumR[columnIndex] += dr;
                    columnSumG[columnIndex] += dg;
                    columnSumB[columnIndex] += db;
                }
            }
            dstRowIndex += dstWidth;
        }
    }

    // f is .000 to .111
    private int fmul(int f, int x) {
        int a = 0;
        if ((f & 0x8000) != 0) {
            a += x >> 1;
        }
        if ((f & 0x4000) != 0) {
            a += x >> 2;
        }
        if ((f & 0x2000) != 0) {
            a += x >> 3;
        }
        return a;
    }

    private int fmul(int f, int w, int x, int y, int z) {
        int a = y - w;
        if ((f & 0x8000) != 0) {
            a += (w >> 1) - (x >> 1) - (y >> 1) + (z >> 1);
        }
        if ((f & 0x4000) != 0) {
            a += (w >> 2) - (x >> 2) - (y >> 2) + (z >> 2);
        }
        if ((f & 0x2000) != 0) {
            a += (w >> 3) - (x >> 3) - (y >> 3) + (z >> 3);
        }
        return a;
    }
}
