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

/*
    Internal note: java.awt.Rectangle class may not be available on all 
    platforms. Also, the Rect class contains methods that the AWT rectangle
    does not have.
*/
    
/**
    The Rect class is used internally for dirty rectangles.
*/
public class Rect {
    
    /** The bitmask that indicates the top segment of a rectangle. */
    public static final int TOP = 1;
    
    /** The bitmask that indicates the left segment of a rectangle. */
    public static final int RIGHT = 2;
    
    /** The bitmask that indicates the right segment of a rectangle. */
    public static final int BOTTOM = 4;
    
    /** The bitmask that indicates the bottom segment of a rectangle. */
    public static final int LEFT = 8;
    
    public int x;
    public int y;
    public int width;
    public int height;
    
    public Rect() {
        setBounds(0, 0, 0, 0);
    }
    
    public Rect(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }
    
    public Rect(Rect r) {
        setBounds(r.x, r.y, r.width, r.height);
    }
    
    public void setBounds(Rect r) {
        setBounds(r.x, r.y, r.width, r.height);
    }
    
    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public int hashCode() {
        int result = 1;
        result = 37*result + x;
        result = 37*result + y;
        result = 37*result + width;
        result = 37*result + height;
        return result;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Rect) {
            Rect r = (Rect)obj;
            return equals(r.x, r.y, r.width, r.height);
        }
        else {
            return false;
        }
    }
    
    public boolean equals(int x, int y, int width, int height) {
        return (
            this.x == x && 
            this.y == y && 
            this.width == width && 
            this.height == height);
    }
    
    public int getArea() {
        return width * height;
    }
    
    public boolean contains(int x, int y) {
        return (
            x >= this.x && 
            y >= this.y && 
            x < this.x + this.width && 
            y < this.y + this.height);
    }
    
    public boolean contains(int x, int y, int width, int height) {
        return (
            x >= this.x && 
            y >= this.y && 
            x + width <= this.x + this.width && 
            y + height <= this.y + this.height);
    }
    
    public boolean contains(Rect r) {
        return contains(r.x, r.y, r.width, r.height);
    }
    
    public boolean intersects(Rect r) {
        return intersects(r.x, r.y, r.width, r.height);
    }
    
    public boolean intersects(int x, int y, int width, int height) {
        return 
            x + width > this.x && 
            x < this.x + this.width &&
            y + height > this.y && 
            y < this.y + this.height;
    }
    
    /**
        Sets this rectangle to the intersection of this rectangle and the specified
        rectangle. If the rectangles don't intersect, the width and height
        will be zero.
    */
    public void intersection(Rect r) {
        intersection(r.x, r.y, r.width, r.height);
    }
    
    /**
        Sets this rectangle to the intersection of this rectangle and the specified
        rectangle. If the rectangles don't intersect, the width and/or height
        will be zero.
    */
    public void intersection(int x, int y, int width, int height) {
        int x1 = Math.max(this.x, x);
        int y1 = Math.max(this.y, y);
        int x2 = Math.min(this.x + this.width - 1, x + width - 1);
        int y2 = Math.min(this.y + this.height - 1, y + height - 1);
        setBounds(x1, y1, Math.max(0, x2 - x1 + 1), Math.max(0, y2 - y1 + 1));
    }
    
    /**
        Sets this rectangle to the union of this rectangle and the specified
        rectangle.
    */
    public void union(Rect r) {
        union(r.x, r.y, r.width, r.height);
    }
    
    /**
        Sets this rectangle to the union of this rectangle and the specified
        rectangle.
    */
    public void union(int x, int y, int width, int height) {
        int x1 = Math.min(this.x, x);
        int y1 = Math.min(this.y, y);
        int x2 = Math.max(this.x + this.width - 1, x + width - 1);
        int y2 = Math.max(this.y + this.height - 1, y + height - 1);
        setBounds(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    // TOP to BOTTOM,  LEFT to RIGHT, TOP|LEFT to BOTTOM|RIGHT, TOP|BOTTOM to TOP|BOTTOM
    public static int getOppositeSide(int side) {
        return ((side << 2) & 0xc) | ((side >> 2) & 0x3);
    }
    
    /**
        Returns the {@link #LEFT} (x), {@link #TOP} (y), 
        {@link #RIGHT} (x + width - 1), or {@link #BOTTOM} (y + height - 1) 
        boundary of this rectangle.
    */
    public int getBoundary(int side) {
        switch (side) {
            case LEFT: return x; 
            case TOP: return y;
            case RIGHT: return x + width - 1;
            case BOTTOM: return y + height - 1;
            default: return x;
        }
    }
    
    public void setBoundary(int side, int boundary) {
        switch (side) {
            case LEFT: 
                width += x - boundary;
                x = boundary;
                break;
                
            case TOP:
                height += y - boundary;
                y = boundary;
                break;
                
            case RIGHT: 
                width = boundary - x + 1;
                break;
                
            case BOTTOM: 
                height = boundary - y + 1;
                break;
                
            default:
                // Do nothing
                break;
        }
    }
    
    public void setOutsideBoundary(int side, int boundary) {
        switch (side) {
            case LEFT: 
                width += x - boundary - 1;
                x = boundary + 1;
                break;
                
            case TOP:
                height += y - boundary - 1;
                y = boundary + 1;
                break;
                
            case RIGHT: 
                width = boundary - x;
                break;
                
            case BOTTOM: 
                height = boundary - y;
                break;
                
            default:
                // Do nothing
                break;
        }
    }    
    
    /**
        Determines which segments of the specified rectangle are completely or
        partially inside this rectangle. The result is a binary OR of the codes
        {@link #TOP}, {@link #LEFT}, {@link #BOTTOM}, {@link #RIGHT}, 
        corresponding to each segment.
    */
    public int getIntersectionCode(Rect r) {
        return getIntersectionCode(r.x, r.y, r.width, r.height);
    }
    
    /**
        Determines which segments of the specified rectangle are completely or
        partially inside this rectangle. The result is a binary OR of the codes
        {@link #TOP}, {@link #LEFT}, {@link #BOTTOM}, {@link #RIGHT}, 
        corresponding to each segment.
    */
    public int getIntersectionCode(int x, int y, int width, int height) {
        
        int ax1 = this.x;
        int ay1 = this.y;
        int ax2 = this.x + this.width - 1;
        int ay2 = this.y + this.height - 1;
        
        int bx1 = x;
        int by1 = y;
        int bx2 = x + width - 1;
        int by2 = y + height - 1;
        
        int code = 0;
        
        if (bx2 >= ax1 && bx1 <= ax2) {
            if (by1 > ay1 && by1 <= ay2) {
                code |= TOP;
            }
            if (by2 >=  ay1 && by2 < ay2) {
                code |= BOTTOM;
            }
        }
        
        if (by2 >= ay1 && by1 <= ay2) {
            if (bx1 > ax1 && bx1 <= ax2) {
                code |= LEFT;
            }
            if (bx2 >= ax1 && bx2 < ax2) {
                code |= RIGHT;
            }
        }
        
        return code;
    }
    
    public String toString() {
        return "Rectangle: " + x + "," + y + " " + width + "x" + height;
    }
}