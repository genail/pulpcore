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

/**
    The Transform class represents a 2D affine transform. Most apps will not need to use this class
    - transformations happen automatically using sprite properties.
*/
public class Transform {
    
    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATE = 1 << 0;
    public static final int TYPE_SCALE = 1 << 1;
    public static final int TYPE_ROTATE = 1 << 2;
    
    private int type;
    private int m00, m01, m02;
    private int m10, m11, m12;
    
    public Transform() {
        clear();
    }
    
    public Transform(Transform transform) {
        set(transform);
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        else if (!(object instanceof Transform)) {
            return false;
        }
        else {
            Transform t = (Transform)object;
            return (
                m00 == t.m00 &&
                m01 == t.m01 &&
                m02 == t.m02 &&
                m10 == t.m10 &&
                m11 == t.m11 &&
                m12 == t.m12);
        }
    }
    
    public int hashCode() {
        int result = 1;
        result = 37*result + m00;
        result = 37*result + m01;
        result = 37*result + m02;
        result = 37*result + m10;
        result = 37*result + m11;
        result = 37*result + m12;
        return result;
    }
    
    public int transformX(int fx, int fy) {
        // [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
        // [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
        // [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
        
        return CoreMath.mul(m00, fx) + CoreMath.mul(m01, fy) + m02;
    }
    
    public int transformY(int fx, int fy) {
        // [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
        // [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
        // [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
        
        return CoreMath.mul(m10, fx) + CoreMath.mul(m11, fy) + m12;
    }
    
    public void transform(Tuple2i t) {
        t.set(transformX(t.x, t.y), transformY(t.x, t.y));
    }
    
    /**
        Returns Integer.MAX_VALUE if this transform can't be inverted.
    */
    public int inverseTransformX(int fx, int fy) {
        
        fx -= m02;
        fy -= m12;
        
        if ((type & Transform.TYPE_ROTATE) != 0) {
            int det = getDeterminant();
            if (det == 0) {
                return Integer.MAX_VALUE;
            }
            return (int)(((long)fx * m11 - (long)fy * m01) / det);
        }
        else if ((type & Transform.TYPE_SCALE) != 0) {
            if (m00 == 0 || m11 == 0) {
                return Integer.MAX_VALUE;
            }
            return CoreMath.div(fx, m00);
        }
        else {
            return fx;
        }
    }
    
    /**
        Returns Integer.MAX_VALUE if this transform can't be inverted.
    */
    public int inverseTransformY(int fx, int fy) {
        
        fx -= m02;
        fy -= m12;
        
        if ((type & Transform.TYPE_ROTATE) != 0) {
            int det = getDeterminant();
            if (det == 0) {
                return Integer.MAX_VALUE;
            }
            return (int)(((long)fy * m00 - (long)fx * m10) / det);
        }
        else if ((type & Transform.TYPE_SCALE) != 0) {
            if (m00 == 0 || m11 == 0) {
                return Integer.MAX_VALUE;
            }
            return CoreMath.div(fy, m11);
        }
        else {
            return fy;
        }
    }
    
    /**
        @return true on success; false if this transform can't be inverted.
    */
    public boolean inverseTransform(Tuple2i t) {
        int tx = inverseTransformX(t.x, t.y);
        int ty = inverseTransformY(t.x, t.y);
        if (tx == Integer.MAX_VALUE || ty == Integer.MAX_VALUE) {
            return false;
        }
        t.set(tx, ty);
        return true;
    }
    
    /**
        Gets the integer bounds.
        @return true if the bounds instance was changed
    */
    public boolean getBounds(int fw, int fh, Rect bounds) {
        int x1 = getTranslateX();
        int y1 = getTranslateY();
        int x2 = CoreMath.mul(getScaleX(), fw);
        int y2 = CoreMath.mul(getShearY(), fw);
        int x3 = CoreMath.mul(getShearX(), fh);
        int y3 = CoreMath.mul(getScaleY(), fh);
        int x4 = x1 + x2 + x3;
        int y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        int boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        int boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        int boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        int boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        int boundsX = CoreMath.toIntFloor(boundsX1);
        int boundsY = CoreMath.toIntFloor(boundsY1);
        int boundsW = CoreMath.toIntCeil(boundsX2) - boundsX;
        int boundsH = CoreMath.toIntCeil(boundsY2) - boundsY;
        
        if (!bounds.equals(boundsX, boundsY, boundsW, boundsH)) {
            bounds.setBounds(boundsX, boundsY, boundsW, boundsH);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
        Gets the fixed-point bounds.
    */
    public Rect getBounds(int fw, int fh) {
        int x1 = getTranslateX();
        int y1 = getTranslateY();
        int x2 = CoreMath.mul(getScaleX(), fw);
        int y2 = CoreMath.mul(getShearY(), fw);
        int x3 = CoreMath.mul(getShearX(), fh);
        int y3 = CoreMath.mul(getScaleY(), fh);
        int x4 = x1 + x2 + x3;
        int y4 = y1 + y2 + y3;
        x2 += x1;
        y2 += y1;
        x3 += x1;
        y3 += y1;
        
        int boundsX1 = Math.min( Math.min(x1, x2), Math.min(x3, x4) );
        int boundsY1 = Math.min( Math.min(y1, y2), Math.min(y3, y4) );
        int boundsX2 = Math.max( Math.max(x1, x2), Math.max(x3, x4) );
        int boundsY2 = Math.max( Math.max(y1, y2), Math.max(y3, y4) );
        
        int boundsW = boundsX2 - boundsX1;
        int boundsH = boundsY2 - boundsY1;
        
        return new Rect(boundsX1, boundsY1, boundsW, boundsH);
    }
    
    public int getType() {
        return type;
    }
    
    public int getTranslateX() {
        return m02;
    }
    
    public int getTranslateY() {
        return m12;
    }
    
    public int getScaleX() {
        return m00;
    }
    
    public int getScaleY() {
        return m11;
    }
    
    public int getShearX() {
        return m01;
    }
    
    public int getShearY() {
        return m10;
    }
    
    public int getDeterminant() {
        return CoreMath.mul(m00, m11) - CoreMath.mul(m01, m10);
    }
    
    //
    // Matrix modifications
    //
    
    /**
        Clears this transform, i.e., sets this transform to the identity matrix.  
    */
    public void clear() {
        m00 = CoreMath.ONE;
        m01 = 0;
        m02 = 0;
        
        m10 = 0;
        m11 = CoreMath.ONE;
        m12 = 0;
        
        type = TYPE_IDENTITY;
    }
    
    /**
        Sets this transform to a copy of specified transform.
    */
    public void set(Transform transform) {
        if (transform == this) {
            return;
        }
        else if (transform == null) {
            clear();
        }
        else {
            this.m00 = transform.m00;
            this.m01 = transform.m01;
            this.m02 = transform.m02;
            this.m10 = transform.m10;
            this.m11 = transform.m11;
            this.m12 = transform.m12;
            this.type = transform.type;
        }
    }
    
    public void concatenate(Transform transform) {
        mult(this, transform, this);
    }
    
    public void preConcatenate(Transform transform) {
        mult(transform, this, this);
    }
    
    private static void mult(Transform a, Transform b, Transform result) {
        
        // Assume either a or b could be the same instance as result
        
        if (a.type == TYPE_IDENTITY) {
            result.set(b);
        }
        else if (b.type == TYPE_IDENTITY) {
            result.set(a);
        }
        else if (b.type == TYPE_TRANSLATE) {
            result.set(a);
            result.translate(b.m02, b.m12);
        }
        else if (b.type == TYPE_SCALE) {
            result.set(a);
            result.scale(b.m00, b.m11);
        }
        else if (b.type == TYPE_ROTATE) {
            result.set(a);
            result.rotate(b.m00, b.m01);
        }
        else {
            long c00 = (long)a.m00*b.m00 + (long)a.m01*b.m10;
            long c01 = (long)a.m00*b.m01 + (long)a.m01*b.m11;
            long c02 = (long)a.m00*b.m02 + (long)a.m01*b.m12 + 
                (((long)a.m02) << CoreMath.FRACTION_BITS);
            
            long c10 = (long)a.m10*b.m00 + (long)a.m11*b.m10;
            long c11 = (long)a.m10*b.m01 + (long)a.m11*b.m11;
            long c12 = (long)a.m10*b.m02 + (long)a.m11*b.m12 + 
                (((long)a.m12) << CoreMath.FRACTION_BITS);
            
            result.m00 = (int)(c00 >> CoreMath.FRACTION_BITS);
            result.m01 = (int)(c01 >> CoreMath.FRACTION_BITS);
            result.m02 = (int)(c02 >> CoreMath.FRACTION_BITS);
            result.m10 = (int)(c10 >> CoreMath.FRACTION_BITS);
            result.m11 = (int)(c11 >> CoreMath.FRACTION_BITS);
            result.m12 = (int)(c12 >> CoreMath.FRACTION_BITS);
            result.type = a.type | b.type;
        }
    }
    
    public void translate(int fx, int fy) {
        // [   1   0   x   ]
        // [   0   1   y   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m02 += fx;
            m12 += fy;
        }
        else {
            m02 += CoreMath.mul(m00, fx);
            m12 += CoreMath.mul(m11, fy);
            
            if ((type & TYPE_ROTATE) != 0) {
                m02 += CoreMath.mul(m01, fy);
                m12 += CoreMath.mul(m10, fx);
            }
        }
        
        type |= TYPE_TRANSLATE;
    }
    
    public void roundTranslation() {
        m02 = CoreMath.round(m02);
        m12 = CoreMath.round(m12);
    }
    
    public void scale(int fx, int fy) {
        // [   x   0   0   ]
        // [   0   y   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m00 = fx;
            m11 = fy;
        }
        else {
            m00 = CoreMath.mul(m00, fx);
            m11 = CoreMath.mul(m11, fy);
            
            if ((type & TYPE_ROTATE) != 0) {
                m01 = CoreMath.mul(m01, fy);
                m10 = CoreMath.mul(m10, fx);
            }
        }
        
        type |= TYPE_SCALE;
    }
    
    //public void scaleByFraction(int fxNumerator, int fxDenominator, 
    //    int fyNumerator, int fyDenominator)
    //{
    //    // [   x   0   0   ]
    //    // [   0   y   0   ]
    //    // [   0   0   1   ]
    //    
    //    if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
    //        m00 = CoreMath.div(fxNumerator, fxDenominator);
    //        m11 = CoreMath.div(fyNumerator, fyDenominator);
    //    }
    //    else {
    //        m00 = CoreMath.mulDiv(m00, fxNumerator, fxDenominator);
    //        m11 = CoreMath.mulDiv(m11, fyNumerator, fyDenominator);
    //        
    //        if ((type & TYPE_ROTATE) != 0) {
    //            m01 = CoreMath.mulDiv(m01, fyNumerator, fyDenominator);
    //            m10 = CoreMath.mulDiv(m10, fxNumerator, fxDenominator);
    //        }
    //    }
    //    
    //    type |= TYPE_SCALE;
    //}
    
    public void rotate(int fAngle) {
        rotate(CoreMath.cos(fAngle), CoreMath.sin(fAngle));
    }
    
    public void rotate(int fCosAngle, int fSinAngle) {
        // [   x  -y   0   ]
        // [   y   x   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m00 = fCosAngle;
            m01 = -fSinAngle;
            m10 = fSinAngle;
            m11 = fCosAngle;
        }
        else {
            int c00 = CoreMath.mul(m00, fCosAngle);
            int c01 = -CoreMath.mul(m00, fSinAngle);
            int c10 = CoreMath.mul(m11, fSinAngle);
            int c11 = CoreMath.mul(m11, fCosAngle);
        
            if ((type & TYPE_ROTATE) != 0) {
                c00 += CoreMath.mul(m01, fSinAngle);
                c01 += CoreMath.mul(m01, fCosAngle);
                c10 += CoreMath.mul(m10, fCosAngle);
                c11 += -CoreMath.mul(m10, fSinAngle);
            }
            
            m00 = c00;
            m01 = c01;
            m10 = c10;
            m11 = c11;
        }
        
        type |= TYPE_ROTATE;
    }
    
    public void shear(int fx, int fy) {
        // [   1   x   0   ]
        // [   y   1   0   ]
        // [   0   0   1   ]
        
        if (type == TYPE_IDENTITY || type == TYPE_TRANSLATE) {
            m01 = fx;
            m10 = fy;
        }
        else {
            int c01 = m01 + CoreMath.mul(m00, fx);
            int c10 = m10 + CoreMath.mul(m11, fy);
                
            if ((type & TYPE_ROTATE) != 0) {
                m00 += CoreMath.mul(m01, fy);
                m11 += CoreMath.mul(m10, fx);
            }
            
            m01 = c01;
            m10 = c10;
        }
        
        type |= TYPE_ROTATE;
    }
}