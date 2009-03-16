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

package pulpcore.math;

import java.text.ParseException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import pulpcore.animation.Tween;
import pulpcore.animation.Easing;
import pulpcore.animation.Timeline;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.sprite.Sprite;

/*
    Internal notes:
    
    * Each bezier segment is converted to line segments so that 
    animation along the path can occur at a constant rate.
    
    * For now, MOVE_TO commands that don't occur at the first draw command
    are interpreted at LINE_TO commands.
    
    * For now, inputs are integers, but internally fixed point its used.
    
    * TODO: change conversion process from floating point to fixed-point.
*/


/**
    The Path class is a series of straight lines and curves that a
    Sprite can animate along. 
    <p>
    Paths points are immutable, but the path can be translated to another location.
    <p>
    Paths are created from a subset of the SVG path commands. Only M (absolute move-to), 
    L (absolute line-to) and C (absolute curve to) commands are supported.
    For example, a triangle path: 
    <pre>path = new Path("M 100 100 L 300 100 L 200 300 L 100 100");</pre>
    A simple curve: 
    <pre>path = new Path("M100,200 C100,100 400,100 400,200");</pre>
    
    Spaces are not required. Floating point values are accepted. 
    See http://www.w3.org/TR/SVG/paths.html#PathData
    
    <p>
    Note, the Path class is not used for rendering paths or shapes.
    Also, the Path class may change substantially in future iterations of PulpCore.
*/
public class Path {
    
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    /** 
        The move command. The move-to command requires one point: the location
        to move to.
    */
    private static final int MOVE_TO = 0;
    
    
    /** 
        The line segment command. The line-to command requires one point: 
        the location to draw a line segment to.
    */
    private static final int LINE_TO = 1;
    
    
    /** 
        The cubic bezier command. The curve-to command requires three points.
        The first point is the control point at the beginning of the curve,
        the second point is the control point at the end of the curve,
        and the third point is the final destination point. All points are
        absolute values.
    */
    private static final int CURVE_TO = 2;
    
    
    /** The number of points in the path. */
    private final int numPoints;
    
    /** The x-location of each point (fixed). */
    private final int[] xPoints;
    
    /** The y-location of each point (fixed). */
    private final int[] yPoints;
    
    /** The value, from 0 to 1, representing the portion of the total length at each point. */
    private final int[] pPoints;
    
    private int totalLength;
    
    private transient int lastCalcP = -1;
    private transient int[] lastCalcPoint = new int[2];
    
    
    /**
        Parse an SVG path data string. Only absolute move-to, line-to, and curve-to commands are
        supported. See http://www.w3.org/TR/SVG/paths.html#PathData
        @throws IllegalArgumentException If the path data string could not be parsed.
    */
    public Path(String svgPathData) throws IllegalArgumentException {
        svgPathData = svgPathData.trim();
        ArrayList commands = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(svgPathData, "MLC", true);
        
        while (tokenizer.hasMoreTokens()) {
            float[] command;
            String commandType = tokenizer.nextToken();
            if (commandType.equals("M")) {
                command = new float[3];
                command[0] = MOVE_TO;
            }
            else if (commandType.equals("L")) {
                command = new float[3];
                command[0] = LINE_TO;
            }
            else if (commandType.equals("C")) {
                command = new float[7];
                command[0] = CURVE_TO;
            }
            else {
                throw new IllegalArgumentException("Not a supported command: " + commandType);
            }
            
            parseSVGNumbers(tokenizer.nextToken(), command);
            commands.add(command);
        }
        
        int[][] points = toLineSegments(commands);
        
        this.xPoints = points[0];
        this.yPoints = points[1];
        this.numPoints = xPoints.length;
        this.pPoints = new int[numPoints];
        
        init();
    }
    
    
    private static void parseSVGNumbers(String s, float[] numbers) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(s, " ,", false);
        for (int i = 1; i < numbers.length; i++) {
            String n = tokenizer.nextToken();
            try {
                numbers[i] = Float.valueOf(n).floatValue();
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a number: " + n);
            }
        }
    }
    
    
    /*
    public Path(int[][] drawCommands) {
        int[][] points = toLineSegments(drawCommands);
        
        this.xPoints = points[0];
        this.yPoints = points[1];
        this.numPoints = xPoints.length;
        this.pPoints = new int[numPoints];
        
        init();
    }
    */
    
    
    public Path(int[] xPoints, int[] yPoints) {
        this.xPoints = toFixed(xPoints);
        this.yPoints = toFixed(yPoints);
        this.numPoints = xPoints.length;
        this.pPoints = new int[numPoints];
        
        init();
    }
    
    
    public double getLength() {
        return CoreMath.toDouble(totalLength);
    }
    
    
    // These two methods are commented out because they are untested
    //
    ///**
    //    Returns the area of the polygon defined by this closed path.
    //*/
    //public float getArea() {
    //    if (area == 0) {
    //        
    //        // Based on "Calculating the area and centroid of a polygon" by Paul Bourke
    //        // http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
    //        for (int i = 0; i < numPoints; i++) {
    //            int j = (i + 1) % numPoints;
    //            area += CoreMath.toFloat(xPoints[i]) * CoreMath.toFloat(yPoints[j]);
    //            area -= CoreMath.toFloat(xPoints[j]) * CoreMath.toFloat(yPoints[i]);
    //        }
    //        area /= 2;
    //    }
    //    return area;
    //}
    //
    //
    ///**
    //    Returns true if the specified point is contained inside
    //    the polygon defined by this closed path.
    //*/
    //public boolean contains(float x, float y) {
    //    int fx = CoreMath.toFixed(x);
    //    int fy = CoreMath.toFixed(y);
    //    
    //    // Based on the "Point Inclusion in Polygon Test" article by W. Randolph Franklin
    //    // http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    //    boolean isInside = false;
    //    int xpj = xPoints[numPoints - 1];
    //    int ypj = yPoints[numPoints - 1];
    //    for (int i = 0; i < numPoints; i++) {
    //        int xpi = xPoints[i];
    //        int ypi = yPoints[i];
    //        
    //        if (((ypi <= fy && fy < ypj) || (ypj <= fy && fy < ypi)) &&
    //            (fx < CoreMath.mulDiv(xpj - xpi, fy - ypi, ypj - ypi) + xpi))
    //        {
    //            // The ray crossed an edge
    //            isInside = !isInside;
    //        }
    //        xpj = xpi;
    //        ypj = ypi;
    //    }
    //    
    //    return isInside;
    //}
        
        
    private int[] toFixed(int[] n) {
        int[] f = new int[n.length];
        for (int i = 0; i < n.length; i++) {
            f[i] = CoreMath.toFixed(n[i]);
        }
        return f;
    }
    
    
    private void init() {
        
        // Find the lengths of each segment;
        totalLength = 0;
        int[] segmentLength = new int[numPoints - 1];
        for (int i = 0; i < numPoints - 1; i++) {
            int dist = (int)CoreMath.dist(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            segmentLength[i] = dist;
            totalLength += dist;
        }
        
        pPoints[0] = 0;
        pPoints[numPoints - 1] = CoreMath.ONE;
        int length = 0;
        for (int i = 1; i < numPoints - 1; i++) {
            length += segmentLength[i - 1];
            pPoints[i] = CoreMath.div(length, totalLength);
        }
    }
    
    
    public void translate(double x, double y) {
        if (x == 0 && y == 0) {
            return;
        }
        int fX = CoreMath.toFixed(x);
        int fY = CoreMath.toFixed(y);
        for (int i = 0; i < numPoints; i++) {
            xPoints[i] += fX;
            yPoints[i] += fY;
        }
    }
    
    
    public int getStartX() {
        return CoreMath.toInt(xPoints[0]);
    }
    
    
    public int getStartY() {
        return CoreMath.toInt(yPoints[0]);
    }
    
    
    public int getEndX() {
        return CoreMath.toInt(xPoints[numPoints - 1]);
    }
    
    
    public int getEndY() {
        return CoreMath.toInt(yPoints[numPoints - 1]);
    }
    
    //
    //
    //
    
    
    public double getX(double p) {
        return CoreMath.toDouble(get(0, CoreMath.toFixed(p)));
    }
    
    
    public double getY(double p) {
        return CoreMath.toDouble(get(1, CoreMath.toFixed(p)));
    }

    
    public double getAngle(double p) {
        int i = getLowerSegment(CoreMath.toFixed(p));
        
        //return CoreMath.atan2(yPoints[i + 1] - yPoints[i], xPoints[i + 1] - xPoints[i]);
        return Math.atan2(yPoints[i + 1] - yPoints[i], xPoints[i + 1] - xPoints[i]);
    }
    
    
    /**
        Places a Sprite at a position along the path.
        @param sprite The Sprite to place.
        @param p The position along the path to place the sprite, from 0 to 1. 
    */
    public void place(Sprite sprite, double p) {
        int P = CoreMath.toFixed(p);
        sprite.x.setAsFixed(get(0, P));
        sprite.y.setAsFixed(get(1, P));
    }    
    
    
    public void guide(Timeline timeline, Sprite sprite, int duration) {
        guideAsFixed(timeline, sprite, 0, CoreMath.ONE, duration, null, 0);
    }
    
    
    public void guide(Timeline timeline, Sprite sprite, int duration, Easing easing) {
        guideAsFixed(timeline, sprite, 0, CoreMath.ONE, duration, easing, 0);
    }
    
    
    public void guide(Timeline timeline, Sprite sprite, int duration, Easing easing, 
        int startDelay) 
    {
        guideAsFixed(timeline, sprite, 0, CoreMath.ONE, duration, easing, startDelay);
    }
    
    
    public void guideBackwards(Timeline timeline, Sprite sprite, int duration) {
        guideAsFixed(timeline, sprite, CoreMath.ONE, 0, duration, null, 0);
    }
    
    
    public void guideBackwards(Timeline timeline, Sprite sprite, int duration, Easing easing) {
        guideAsFixed(timeline, sprite, CoreMath.ONE, 0, duration, easing, 0);
    }
    
    
    public void guideBackwards(Timeline timeline, Sprite sprite, int duration, Easing easing, 
        int startDelay) 
    {
        guideAsFixed(timeline, sprite, CoreMath.ONE, 0, duration, easing, startDelay);
    }    
    
    
    public void guide(Timeline timeline, Sprite sprite, double startP, double endP, int duration) {
        guideAsFixed(timeline, sprite, CoreMath.toFixed(startP), CoreMath.toFixed(endP),
            duration, null, 0);
    }
    
    
    public void guide(Timeline timeline, Sprite sprite, double startP, double endP, int duration,
        Easing easing) 
    {
        guideAsFixed(timeline, sprite, CoreMath.toFixed(startP), CoreMath.toFixed(endP),
            duration, easing, 0);
    }
    
    
    public void guide(Timeline timeline, Sprite sprite, double startP, double endP, int duration,
        Easing easing, int startDelay) 
    {
        guideAsFixed(timeline, sprite, CoreMath.toFixed(startP), CoreMath.toFixed(endP),
            duration, easing, startDelay);
    }    


    private void guideAsFixed(Timeline timeline, Sprite sprite, int startP, int endP, 
        int duration, Easing easing, int startDelay) 
    {
        PathAnimation xAnimation = 
            new PathAnimation(X_AXIS, startP, endP, duration, easing, startDelay);
        PathAnimation yAnimation = 
            new PathAnimation(Y_AXIS, startP, endP, duration, easing, startDelay);
            
        if (timeline == null) {
            sprite.x.setBehavior(xAnimation);
            sprite.y.setBehavior(yAnimation);
        }
        else {
            timeline.add(sprite.x, xAnimation);
            timeline.add(sprite.y, yAnimation);
        }
    }
    
    
    //
    //
    //
    
    
    private int get(int axis, int p) {
        return getLocation(p)[axis];
    }
    
    
    private int getLowerSegment(int p) {
        p = Math.max(p, 0);
        p = Math.min(p, CoreMath.ONE);
        
        // Binary search for P
        int high = numPoints - 1;
        int low = 0;
        while (high - low > 1) {
            int index = (high + low) >>> 1;
          
            if (pPoints[index] > p) {
                high = index;
            }
            else {
                low = index;
            }
        }
        
        return low;
    }
    
    
    private int[] getLocation(int p) {
        p = Math.max(p, 0);
        p = Math.min(p, CoreMath.ONE);
        
        if (p == lastCalcP) {
            return lastCalcPoint;
        }
        
        lastCalcP = p;
        
        if (p == 0) {
            lastCalcPoint[0] = xPoints[0];
            lastCalcPoint[1] = yPoints[0];
            return lastCalcPoint;
        }
        else if (p == CoreMath.ONE) {
            lastCalcPoint[0] = xPoints[numPoints - 1];
            lastCalcPoint[1] = yPoints[numPoints - 1];
            return lastCalcPoint;
        }
        
        // Binary search for P
        int high = numPoints - 1;
        int low = 0;
        while (high - low > 1) {
            int index = (high + low) >>> 1;
          
            if (pPoints[index] > p) {
                high = index;
            }
            else {
                low = index;
            }
        }
        
        if (high == low) {
            lastCalcPoint[0] = xPoints[low];
            lastCalcPoint[1] = yPoints[low];
        }
        else {
            int q = p - pPoints[low];
            int r = pPoints[high] - pPoints[low];

            lastCalcPoint[0] = xPoints[low] + CoreMath.mulDiv(xPoints[high] - xPoints[low], q, r);
            lastCalcPoint[1] = yPoints[low] + CoreMath.mulDiv(yPoints[high] - yPoints[low], q, r);
        }
       
        return lastCalcPoint;
    }


    class PathAnimation extends Tween {
        
        private final int axis;
        private final int startP;
        private final int endP;
        
        
        PathAnimation(int axis, int startP, int endP, int duration, Easing easing, int startDelay) {
            super(
                get(axis, startP), 
                get(axis, endP),
                duration, easing, startDelay);
            this.axis = axis;
            this.startP = startP;
            this.endP = endP;
        }
        
        protected void updateState(int animTime) {
            if (getDuration() == 0) {
                super.updateState(animTime);
            } 
            else {
                super.setValue(
                    get(axis, startP + CoreMath.mulDiv(endP - startP, animTime, getDuration())));
            }
        }
    }
    
    
    /**
        Draws the segments of this path using the current color.
        @param drawJoints if true, draw rectangles at the joints between line segments
    */
    public void draw(CoreGraphics g, boolean drawJoints) {
        int x1 = xPoints[0];
        int y1 = yPoints[0];
        for (int i = 1; i < numPoints; i++) {
            if (drawJoints) {
                g.fillRectFixedPoint(x1-CoreMath.ONE, y1-CoreMath.ONE, 
                    CoreMath.toFixed(3), CoreMath.toFixed(3));
            }
            int x2 = xPoints[i];
            int y2 = yPoints[i];
            g.drawLineFixedPoint(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        
        if (drawJoints) {
            g.fillRectFixedPoint(x1-CoreMath.ONE, y1-CoreMath.ONE, 
                CoreMath.toFixed(3), CoreMath.toFixed(3));
        }
        
    }
        
    
    //
    // Convert draw commands to line segments
    //
    
    
    private static int[][] toLineSegments(ArrayList drawCommands) {
        
        int numDrawCommands = drawCommands.size();
        int[][][] allPoints = new int[numDrawCommands][2][];
        int numPoints = 0;
        float x = 0;
        float y = 0;
        
        for (int i = 0; i < numDrawCommands; i++) {
            float[] drawCommand = (float[])drawCommands.get(i);
            switch ((int)drawCommand[0]) {
                
                case MOVE_TO: case LINE_TO:
                    x = drawCommand[1];
                    y = drawCommand[2];
                    
                    allPoints[i] = new int[2][1];
                    allPoints[i][0][0] = CoreMath.toFixed(x);
                    allPoints[i][1][0] = CoreMath.toFixed(y);
                    numPoints++;
                    break;
                
                case CURVE_TO:
                    allPoints[i] = toLineSegments(x, y,
                         drawCommand[1],  drawCommand[2],
                         drawCommand[3],  drawCommand[4], 
                         drawCommand[5],  drawCommand[6]);
                 
                    x = drawCommand[5];
                    y = drawCommand[6];
                    numPoints += allPoints[i][0].length;
                    break;
            }
        }
        
        // Convert the allPoints array to xPoints, yPoints
        int[] xPoints = new int[numPoints];
        int[] yPoints = new int[numPoints];
        int offset = 0;
        for (int i = 0; i < numDrawCommands; i++) {
            int len = allPoints[i][0].length;
            System.arraycopy(allPoints[i][0], 0, xPoints, offset, len);
            System.arraycopy(allPoints[i][1], 0, yPoints, offset, len);
            
            offset += len;
            allPoints[i] = null;
        }
        
        int[][] returnValue = new int[2][];
        returnValue[0] = xPoints;
        returnValue[1] = yPoints;
        
        return returnValue;
    }
    
    
    //
    // Bezier conversion using forward differencing. 
    //
    
    
    private static int[][] toLineSegments(float x1, float y1, float x2, float y2, 
        float x3, float y3, float x4, float y4) 
    {
        // First division
        float x12   = (x1 + x2) / 2f;
        float y12   = (y1 + y2) / 2f;
        float x23   = (x2 + x3) / 2f;
        float y23   = (y2 + y3) / 2f;
        float x34   = (x3 + x4) / 2f;
        float y34   = (y3 + y4) / 2f;
        float x123  = (x12 + x23) / 2f;
        float y123  = (y12 + y23) / 2f;
        float x234  = (x23 + x34) / 2f;
        float y234  = (y23 + y34) / 2f;
        float x1234 = (x123 + x234) / 2f;
        float y1234 = (y123 + y234) / 2f;
        
        // Left division
        float lx12   = (x1 + x12) / 2f;
        float ly12   = (y1 + y12) / 2f;
        float lx23   = (x12 + x123) / 2f;
        float ly23   = (y12 + y123) / 2f;
        float lx34   = (x123 + x1234) / 2f;
        float ly34   = (y123 + y1234) / 2f;
        float lx123  = (lx12 + lx23) / 2f;
        float ly123  = (ly12 + ly23) / 2f;
        float lx234  = (lx23 + lx34) / 2f;
        float ly234  = (ly23 + ly34) / 2f;
        float lx1234 = (lx123 + lx234) / 2f;
        float ly1234 = (ly123 + ly234) / 2f;
        
        // Right division
        float rx12   = (x1234 + x234) / 2f;
        float ry12   = (y1234 + y234) / 2f;
        float rx23   = (x234 + x34) / 2f;
        float ry23   = (y234 + y34) / 2f;
        float rx34   = (x34 + x4) / 2f;
        float ry34   = (y34 + y4) / 2f;
        float rx123  = (rx12 + rx23) / 2f;
        float ry123  = (ry12 + ry23) / 2f;
        float rx234  = (rx23 + rx34) / 2f;
        float ry234  = (ry23 + ry34) / 2f;
        float rx1234 = (rx123 + rx234) / 2f;
        float ry1234 = (ry123 + ry234) / 2f;
        
        // Determine the number of segments for each division
        int numSegments1 = getNumSegments(x1, y1, lx12, ly12, lx123, ly123, lx1234, ly1234);
        int numSegments2 = getNumSegments(lx1234, ly1234, lx234, ly234, lx34, ly34, x1234, y1234);
        int numSegments3 = getNumSegments(x1234, y1234, rx12, ry12, rx123, ry123, rx1234, ry1234);
        int numSegments4 = getNumSegments(rx1234, ry1234, rx234, ry234, rx34, ry34, x4, y4);
        
        int totalPoints = numSegments1 + numSegments2 + numSegments3 + numSegments4 + 1;
        int[] xPoints = new int[totalPoints];
        int[] yPoints = new int[totalPoints];
        int offset = 0;
        
        // Convert to lines
        offset += toLineSegments(xPoints, yPoints, offset, numSegments1, 
            x1, y1, lx12, ly12, lx123, ly123, lx1234, ly1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments2, 
            lx1234, ly1234, lx234, ly234, lx34, ly34, x1234, y1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments3, 
            x1234, y1234, rx12, ry12, rx123, ry123, rx1234, ry1234);
        offset += toLineSegments(xPoints, yPoints, offset, numSegments4, 
            rx1234, ry1234, rx234, ry234, rx34, ry34, x4, y4);
        
        int[][] returnValue = new int[2][];
        returnValue[0] = xPoints;
        returnValue[1] = yPoints;
        return returnValue;
    }
    
    
    private static int getNumSegments(float x0, float y0, float x1, float y1, float x2, float y2, 
        float x3, float y3) 
    {
        int numSegments;
        
        // attempt to measure the "curvature" of the segment - more curvature, the more points.
        /*
        float dx = (x1 + x2 - x0 - x3) / 2;
        float dy = (y1 + y2 - y0 - y3) / 2;
        
        int numSegments;
        if (dx == 0 && dy == 0) {
            numSegments = 1;
        }
        else {
            float distSq = dx * dx + dy * dy;
        
            int log2Dist = FixedMath.log2(Math.round(distSq)) >> 1;
            
            numSegments = Math.max(1, 1 << (log2Dist - 1));
        }
        */
        
        float dist = Math.max(
            getDist(x1, y1, x0, y0, x3, y3),
            getDist(x2, y2, x0, y0, x3, y3));
        
        if (dist <= 0) {
            numSegments = 1;
        }
        else {
            numSegments = Math.max(1, 1 << (CoreMath.log2(Math.round(dist))));
            numSegments = Math.min(numSegments, 16);
        }
        
        return numSegments;
    }
    
    
    private static int toLineSegments(int[] xPoints, int[] yPoints, int offset, int numSegments, 
        float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) 
    {
        float t = 1f / numSegments;
        float tSquared = t * t;
        float tCubed = tSquared * t;
        
        float xf = x0;
        float xfd = 3 * (x1 - x0) * t;
        float xfdd2 = 3 * (x0 - 2 * x1 + x2) * tSquared;
        float xfddd6 = (3 * (x1 - x2) + x3 - x0) * tCubed;
        float xfddd2 = 3 * xfddd6;
        float xfdd = 2 * xfdd2;
        float xfddd = 2 * xfddd2;
        
        float yf = y0;
        float yfd = 3 * (y1 - y0) * t;
        float yfdd2 = 3 * (y0 - 2 * y1 + y2) * tSquared;
        float yfddd6 = (3 * (y1 - y2) + y3 - y0) * tCubed;
        float yfddd2 = 3 * yfddd6;
        float yfdd = 2 * yfdd2;
        float yfddd = 2 * yfddd2;
        
        xPoints[offset] = CoreMath.toFixed(x0);
        yPoints[offset] = CoreMath.toFixed(y0);
        offset++;
        
        for (int i = 1; i < numSegments; i++) {
            xf += xfd + xfdd2 + xfddd6;
            xfd += xfdd + xfddd2;
            xfdd += xfddd;
            xfdd2 += xfddd2;
            
            yf += yfd + yfdd2 + yfddd6;
            yfd += yfdd + yfddd2;
            yfdd += yfddd;
            yfdd2 += yfddd2;
            
            xPoints[offset] = CoreMath.toFixed(xf);
            yPoints[offset] = CoreMath.toFixed(yf);
            offset++;
        }
        
        xPoints[offset] = CoreMath.toFixed(x3);
        yPoints[offset] = CoreMath.toFixed(y3);
        offset++;
        
        return numSegments;
    }
    
    
    private static float getDist(float px, float py, float ax, float ay, float bx, float by) {
    
        // Distance from a point to a line approximation. From Graphics Gems II, page 11.
        
        float dx = Math.abs(bx - ax);
        float dy = Math.abs(by - ay);
        
        float div = dx + dy - Math.min(dx, dy)/2;
        
        if (div == 0) {
            return 0;
        }
        
        float a2 = (py - ay) * (bx - ax) - (px - ax) * (by - ay);
        
        return Math.abs(a2) / div;
    }
}