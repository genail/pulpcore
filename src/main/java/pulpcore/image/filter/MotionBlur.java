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
import pulpcore.image.Colors;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;

/**
    The MotionBlur filter simulates the streaking of rapidly moving objects.
 */
public final class MotionBlur extends Filter {

    private static final int MAX_DISTANCE = 255;

    private static int[] x1Offsets = new int[MAX_DISTANCE + 2];
    private static int[] y1Offsets = new int[MAX_DISTANCE + 2];
    private static int[][] accum;
    private static final Object bufferLock = new Object();

    /**
    The motion distance in pixels. 
    The maximum value is 255. The default value is 4.
     */
    public final Fixed distance = new Fixed(4);

    /**
    The motion angle in radians, typically from -Math.PI/2 to Math.PI/2.
     */
    public final Fixed angle = new Fixed(0);

    // This is a property only so it can be bound properly in copy()
    private final Bool autoExpand = new Bool(true);

    private int actualDistance;

    // Invalid value so cos/sin will be computed
    private int actualAngle = Integer.MAX_VALUE;

    private int actualAngleCos;

    private int actualAngleSin;

    private Rect bounds = new Rect();

    /**
    Create a MotionBlur filter with an angle of 0 and a distance of 4.
     */
    public MotionBlur() {
        this(0, 4);
    }

    /**
    Create a MotionBlur filter with the specified angle and a distance of 4.
     */
    public MotionBlur(float angle) {
        this(angle, 4);
    }

    /**
    Create a MotionBlur filter with the specified angle and the specified distance.
     */
    public MotionBlur(float angle, float distance) {
        this.angle.set(angle);
        this.distance.set(distance);
    }

    private MotionBlur(MotionBlur filter) {
        autoExpand.bindWithInverse(filter.autoExpand);
        distance.bindWithInverse(filter.distance);
        angle.bindWithInverse(filter.angle);
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
        return new MotionBlur(this);
    }

    public void update(int elapsedTime) {
        distance.update(elapsedTime);
        angle.update(elapsedTime);

        int d = CoreMath.clamp(distance.getAsFixed(), 0, CoreMath.toFixed(MAX_DISTANCE));
        // Granularity: 1/8th pixel
        d &= 0xffffe000;
        int a = getAngle();
        if (actualDistance != d) {
            actualDistance = d;
            setDirty();
        }
        if (actualAngle != a) {
            actualAngle = a;
            actualAngleCos = CoreMath.cos(a);
            actualAngleSin = CoreMath.sin(a);
            setDirty();
        }
        if (isDirty()) {
            setBounds();
        }
    }

    private void setBounds() {
        int w = super.getWidth();
        int h = super.getHeight();
        bounds.setBounds(0, 0, w, h);
        if (autoExpand.get()) {
            int iterations = getIterations();
            int x = actualAngleCos * (iterations - 1) / 2;
            int y = actualAngleSin * (iterations - 1) / 2;
            if (x < 0) {
                x = CoreMath.toIntFloor(x);
            }
            else {
                x = CoreMath.toIntCeil(x);
            }
            if (y < 0) {
                y = CoreMath.toIntFloor(y);
            }
            else {
                y = CoreMath.toIntCeil(y);
            }
            bounds.union(x, y, w, h);
            bounds.union(-x, -y, w, h);
        }
    }
    
    private int getAngle() {
        int a = angle.getAsFixed();

        // Reduce range to -2*pi and 2*pi
        int s = a / CoreMath.TWO_PI;
        a -= s * CoreMath.TWO_PI;

        // Reduce range to -pi and pi
        if (a > CoreMath.PI) {
            a = a - CoreMath.TWO_PI;
        }
        else if (a < -CoreMath.PI) {
            a = a + CoreMath.TWO_PI;
        }

        if (a > CoreMath.ONE_HALF_PI) {
            a = a - CoreMath.PI;
        }
        else if (a < -CoreMath.ONE_HALF_PI) {
            a = a + CoreMath.PI;
        }
        return a;
    }

    private boolean isInputOpaque() {
        return getInput() == null ? false : getInput().isOpaque();
    }

    public int getX() {
        return bounds.x;
    }

    public int getY() {
        return bounds.y;
    }

    public int getWidth() {
        return bounds.width;
    }

    public int getHeight() {
        return bounds.height;
    }

    public boolean isOpaque() {
        return (isInputOpaque() && !autoExpand.get());
    }

    private int getIterations() {
        // Iterations must be odd
        return (CoreMath.toIntCeil(actualDistance) + 1) | 1;
    }

    protected void filter(CoreImage input, CoreImage output) {
        synchronized (bufferLock) {
            int[] srcData = input.getData();
            int[] dstData = output.getData();
            int dstWidth = output.getWidth();
            int dstHeight = output.getHeight();
            int srcWidth = input.getWidth();
            int srcHeight = input.getHeight();
            int actualDistancePlusOne = actualDistance + CoreMath.ONE;
            int iterations = getIterations();
            int outerAlpha = 255-CoreMath.toIntRound(127 * (CoreMath.toFixed(iterations) - actualDistancePlusOne));
            int divisor = actualDistancePlusOne;
            int iDivisor = CoreMath.toIntFloor(divisor);
            int iDivisor2 = CoreMath.log2(iDivisor);
            int sx = actualAngleCos * (iterations - 1) / 2;
            int sy = actualAngleSin * (iterations - 1) / 2;
            for (int i = 0; i < iterations; i++) {
                x1Offsets[i] = (CoreMath.toIntRound(sx)+getX());
                y1Offsets[i] = (CoreMath.toIntRound(sy)+getY());
                sx -= actualAngleCos;
                sy -= actualAngleSin;
            }

            if (accum == null || accum[0].length < dstWidth) {
                accum = new int[4][dstWidth];
            }

            int[] a = accum[0];
            int[] r = accum[1];
            int[] g = accum[2];
            int[] b = accum[3];

            int dstOffset = 0;
            for (int y = 0; y < dstHeight; y++) {
                // Reset this row
                int rowX1 = dstWidth - 1;
                int rowX2 = 0;
                for (int i = 0; i < iterations; i++) {
                    int sourceY = y + y1Offsets[i];
                    if (sourceY >= 0 && sourceY < srcHeight) {
                        rowX1 = Math.min(rowX1, -x1Offsets[i]);
                        rowX2 = Math.max(rowX2, -x1Offsets[i] + srcWidth-1);
                    }
                }
                rowX1 = Math.max(rowX1, 0);
                rowX2 = Math.min(rowX2, dstWidth - 1);
                if (rowX1 < rowX2) {
                    for (int x = rowX1; x <= rowX2; x++) {
                        a[x] = 0;
                        r[x] = 0;
                        g[x] = 0;
                        b[x] = 0;
                    }
                    // Write the iteration (accumulate)
                    for (int i = 0; i < iterations; i++) {
                        int sourceY = y + y1Offsets[i];

                        if (sourceY >= 0 && sourceY < srcHeight) {
                            int x1 = Math.max(-x1Offsets[i], 0);
                            int x2 = Math.min(-x1Offsets[i] + srcWidth-1, dstWidth - 1);
                            int sourceX = x1+x1Offsets[i];
                            int srcOffset = sourceX + sourceY*srcWidth;

                            if (outerAlpha < 255 && (i ==0 || i == iterations - 1)) {
                                for (int x = x1; x <= x2; x++) {
                                    int argb = srcData[srcOffset++];
                                    a[x] += (Colors.getAlpha(argb) * outerAlpha) >> 8;
                                    r[x] += (Colors.getRed(argb) * outerAlpha) >> 8;
                                    g[x] += (Colors.getGreen(argb) * outerAlpha) >> 8;
                                    b[x] += (Colors.getBlue(argb) * outerAlpha) >> 8;
                                }
                            }
                            else {
                                for (int x = x1; x <= x2; x++) {
                                    int argb = srcData[srcOffset++];
                                    a[x] += Colors.getAlpha(argb);
                                    r[x] += Colors.getRed(argb);
                                    g[x] += Colors.getGreen(argb);
                                    b[x] += Colors.getBlue(argb);
                                }
                            }
                        }
                    }

                    // Convert the row
                    for (int x = 0; x < rowX1; x++) {
                        dstData[dstOffset++] = 0;
                    }
                    if (CoreMath.toFixed(iDivisor) == divisor) {
                        if ((1 << iDivisor2) == iDivisor) {
                            for (int x = rowX1; x <= rowX2; x++) {
                                dstData[dstOffset++] = Colors.rgba(r[x] >> iDivisor2, g[x] >> iDivisor2, b[x] >> iDivisor2, a[x] >> iDivisor2);
                            }
                        }
                        else {
                            for (int x = rowX1; x <= rowX2; x++) {
                                dstData[dstOffset++] = Colors.rgba(r[x] / iDivisor, g[x] / iDivisor, b[x] / iDivisor, a[x] / iDivisor);
                            }
                        }
                    }
                    else {
                        for (int x = rowX1; x <= rowX2; x++) {
                            dstData[dstOffset++] = Colors.rgba(
                                    (int)((((long)r[x]) << 16) / divisor),
                                    (int)((((long)g[x]) << 16) / divisor),
                                    (int)((((long)b[x]) << 16) / divisor),
                                    (int)((((long)a[x]) << 16) / divisor));
                        }
                    }
                    for (int x = rowX2+1; x < dstWidth; x++) {
                        dstData[dstOffset++] = 0;
                    }
                }
                else {
                    for (int x = 0; x < dstWidth; x++) {
                        dstData[dstOffset++] = 0;
                    }
                }
            }
        }
    }
}
