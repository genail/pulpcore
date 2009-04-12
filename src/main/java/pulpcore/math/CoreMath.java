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
    The CoreMath class contains fixed-point arithmetic functions and other 
    useful math functions.
    <p>
    Fixed-point numbers can be used in place of floating-point numbers. 
    Regarding fixed-point numbers:
    <p>
    Addition and subtraction of two fixed-point numbers is done normally, using
    + and - operators.
    <p>
    Multiplying a fixed-point number by an integer is done normally,
    using the * operator. The result is a fixed-point number.
    <p>
    Dividing a fixed-point number by an integer (the fixed-point number is the
    numerator, and the integer is the denominator) is done normally, using the / 
    operator. The result is a fixed-point number.
    
*/
public class CoreMath {
    
    public static final int FRACTION_BITS = 16;
    
    public static final int FRACTION_MASK = (1 << FRACTION_BITS) - 1;
    
    public static final int ONE = (1 << FRACTION_BITS);
    
    public static final int ONE_HALF = ONE >> 1;
    
    public static final int PI = (int)Math.round(Math.PI * ONE);
    
    public static final int TWO_PI = (int)Math.round(2 * Math.PI * ONE);
    
    public static final int ONE_HALF_PI = (int)Math.round(.5 * Math.PI * ONE);
    
    public static final int E = (int)Math.round(Math.E * ONE);
    
    public static final int MAX_VALUE = (1 << 31) - 1;
    
    public static final int MIN_VALUE = -(1 << 31);
    
    /** The maximum integer value that a 32-bit fixed-point value can represent. */
    public static final int MAX_INT_VALUE = (1 << (31 - FRACTION_BITS)) - 1;
    
    /** The minumum integer value that a 32-bit fixed-point value can represent. */
    public static final int MIN_INT_VALUE = -(1 << (31 - FRACTION_BITS));
    
    /** The maximum 32-bit floating-point value that a 32-bit fixed-point value can represent. */
    // Math.round(MAX_FLOAT_VALUE * ONE) = MAX_VALUE
    public static final float MAX_FLOAT_VALUE = 32768f; 
    
    /** The minumum 32-bit floating-point value that a 32-bit fixed-point value can represent. */
    public static final float MIN_FLOAT_VALUE = -32768f;
    
    /** The maximum 64-bit floating-point value that a 32-bit fixed-point value can represent. */
    // Found this by trial and error
    // Math.round(MAX_DOUBLE_VALUE * ONE) = MAX_VALUE
    public static final double MAX_DOUBLE_VALUE = 32767.999992370602;
    
    /** The minumum 64-bit floating-point value that a 32-bit fixed-point value can represent. */
    public static final double MIN_DOUBLE_VALUE = -32768.0;
    
    // For more accurate results for sine/cosine. This number was found by trial and error.
    private static final int TWO_PI_ERROR_FACTOR = 6;
    
    //private static final int TWO_PI_ERROR = -11;
    private static final int TWO_PI_ERROR =
        (int)Math.round((2*Math.PI*(1 << TWO_PI_ERROR_FACTOR) - 
        (double)(TWO_PI << TWO_PI_ERROR_FACTOR) / ONE) * ONE);
    
    /** Number of fractional bits used for some internal calculations */
    private static final int INTERNAL_BITS = 24;
    
    // Prevent instantiation
    private CoreMath() { }
    
    /**
        Converts an integer to a fixed-point value.
    */
    public static final int toFixed(int n) {
        if (n > MAX_INT_VALUE) {
            return MAX_VALUE;
        }
        else if (n < MIN_INT_VALUE) {
            return MIN_VALUE;
        }
        else {
            return n << FRACTION_BITS;
        }
    }

    /**
        Converts a float to a fixed-point value.
    */
    public static final int toFixed(float n) {
        if (n > MAX_FLOAT_VALUE) {
            return MAX_VALUE;
        }
        else if (n < MIN_FLOAT_VALUE) {
            return MIN_VALUE;
        }
        else {
            return Math.round(n * ONE);
        }
    }
    
    /**
        Converts a double-percsion float to a fixed-point value.
    */
    public static final int toFixed(double n) {
        if (n > MAX_DOUBLE_VALUE) {
            return MAX_VALUE;
        }
        else if (n < MIN_DOUBLE_VALUE) {
            return MIN_VALUE;
        }
        else {
            return (int)Math.round(n * ONE);
        }
    }
    
    /**
        Converts a String representing a double-percsion float into a fixed-point value.
        @throws NumberFormatException if the string does not contain a parsable number.
    */
    public static final int toFixed(String n) throws NumberFormatException {
        return toFixed(Double.valueOf(n).doubleValue());
    }
    
    /**
        Converts a fixed-point value to a float.
    */
    public static final float toFloat(int f) {
        return (float)f / ONE;
    }
    
    /**
        Converts a fixed-point value to a double.
    */
    public static final double toDouble(int f) {
        return (double)f / ONE;
    }

    /**
        Converts a fixed-point value to an integer. Same behavior as casting
        a float to an int.
    */
    public static final int toInt(int f) {
        if (f < 0) {
            return toIntCeil(f);
        }
        else {
            return toIntFloor(f);
        }
    }
    
    /**
        Converts a fixed-point value to an integer.
    */
    public static final int toIntFloor(int f) {
        return f >> FRACTION_BITS;
    }
    
    /**
        Converts a fixed-point value to an integer.
    */
    public static final int toIntRound(int f) {
        return toIntFloor(f + ONE_HALF);
    }
    
    /**
        Converts a fixed-point value to an integer.
    */
    public static final int toIntCeil(int f) {
        return -toIntFloor(-f);
    }

    /**
        Returns the fractional part of a fixed-point value (removes the
        integer part).
    */
    public static final int fracPart(int f) {
        return abs(f) & FRACTION_MASK;
    }
    
    /**
        Returns the floor of a fixed-point value (removes the
        fractional part).
    */
    public static final int floor(int f) {
        return f & ~FRACTION_MASK;
    }
    
    /**
        Returns the ceil of a fixed-point value.
    */
    public static final int ceil(int f) {
        return -floor(-f);
    }
    
    /**
        Returns the fixed-point value rounded to the nearest integer location.
    */
    public static final int round(int f) {
        return floor(f + ONE_HALF);
    }
    
    /**
        Converts a fixed-point number to a base-10 string representation.
    */
    public static final String toString(int f) {
        return formatNumber(
            abs(toInt(f)),
            fracPart(f) << (32 - FRACTION_BITS), (f < 0),
            1, 7, false);
    }
        
    /**
        Converts a fixed-point number to a base-10 string representation using 
        the specified number of fractional digits. 
    */
    public static final String toString(int f, int numFractionalDigits) {
        return formatNumber(
            abs(toInt(f)),
            fracPart(f) << (32 - FRACTION_BITS), (f < 0),
            numFractionalDigits, numFractionalDigits, false);
    }
    
    /**
        Converts a fixed-point number to a base-10 string representation.
        @param f the fixed-point number
        @param minFracDigits the minimum number of digits to show after
        the decimal point.
        @param maxFracDigits the maximum number of digits to show after
        the decimal point.
        @param grouping if (true, uses the grouping character (',') 
        to seperate groups in the integer portion of the number.
    */
    public static String toString(int f, 
        int minFracDigits, int maxFracDigits, boolean grouping)
    {
        return formatNumber(
            abs(toInt(f)),
            fracPart(f) << (32 - FRACTION_BITS), (f < 0),
            minFracDigits, maxFracDigits, grouping); 
    }
    
    /**
        Converts an integer to a base-10 string representation.
    */
    public static final String intToString(int n) {
        return formatNumber(abs(n), 0, (n < 0), 0, 0, false); 
    }
        
    /**
        Converts a integer to a base-10 string representation using 
        the specified number of fractional digits. 
    */
    public static final String intToString(int n, int numFractionalDigits) {
        return formatNumber(abs(n), 0, (n < 0), 
            numFractionalDigits, numFractionalDigits, false); 
    }
    
    /**
        Converts an integer to a base-10 string representation.
        @param n the integer
        @param minFracDigits the minimum number of digits to show after
        the decimal point.
        @param maxFracDigits the maximum number of digits to show after
        the decimal point.
        @param grouping if (true, uses the grouping character (',') 
        to seperate groups in the integer portion of the number.
    */
    public static String intToString(int n, 
        int minFracDigits, int maxFracDigits, boolean grouping)
    {
        return formatNumber(abs(n), 0, (n < 0), 
            minFracDigits, maxFracDigits, grouping); 
    }
    
    /**
        Converts a number to a base-10 string representation.
        @param intPart the integer part of the number
        @param fracPart the fractional part, a 32-bit fixed point value.
        @param minFracDigits the minimum number of digits to show after
        the decimal point.
        @param maxFracDigits the maximum number of digits to show after
        the decimal point.
        @param intPartGrouping if (true, uses the groupong character (',') 
        to seperate groups in the integer portion of the number.
    */
    private static String formatNumber(int intPart, int fracPart, boolean negative, 
        int minFracDigits, int maxFracDigits, boolean intPartGrouping)
    {
        StringBuffer buffer = new StringBuffer();
        long one = 1L << 32;
        long mask = one - 1;
        long frac = ((long)fracPart) & mask;
        
        // Round up if needed
        if (maxFracDigits < 10) {
            long place = 1;
            for (int i = 0; i < maxFracDigits; i++) {
                place *= 10;
            }
            frac += (1L << 31) / place;
            if (frac >= one) {
                intPart++;
            }
        }
        
        // Convert integer part
        if (!intPartGrouping || intPart == 0) {
            if (negative) {
                buffer.append('-');
            }
            buffer.append(intPart);
        }
        else {
            int i = 0;
            while (intPart > 0) {
                if (i == 3) {
                    buffer.insert(0, ',');
                    i = 0;
                }
                char ch = (char)((intPart % 10) + '0');
                buffer.insert(0, ch);
                intPart /= 10;
                i++;
            }
            
            if (negative) {
                buffer.insert(0, '-');
            }
        }
        
        if (maxFracDigits == 0 || (fracPart == 0 && minFracDigits == 0)) {
            return buffer.toString();
        }
        
        buffer.append('.');
        
        // Convert fractional part
        int numFracDigits = 0;
        while (true) {
            
            frac = (frac & mask) * 10L;
            
            if (frac == 0) {
                buffer.append('0');
            }
            else {
                buffer.append((char)('0' + ((frac >>> 32) % 10)));
            }
            
            numFracDigits++;
            if (numFracDigits == maxFracDigits || (frac == 0 && numFracDigits >= minFracDigits)) {
                break;
            }
        }
        
        // Remove uneeded trailing zeros
        if (numFracDigits > minFracDigits) {
            int len = numFracDigits - minFracDigits;
            for (int i = 0; i < len; i++) {
                if (buffer.charAt(buffer.length() - 1) == '0') {
                    buffer.setLength(buffer.length() - 1);
                }
                else {
                    break;
                }
            }
        }
        
        return buffer.toString();
    }
    
    //
    // Bit manipulation
    //
    
    /**
        Returns true if the number (greater than 1) is a power of two.
    */
    public static final boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }
  
    /**
        Counts the number of "on" bits in an integer.
    */
    public static final int countBits(int n) {
        /*
        int count = 0;
        while (n > 0) {
            count += (n & 1);
            n >>= 1;
        }
        return count;
        */
        int count = n;
        count = ((count >> 1)  & 0x55555555) + (count & 0x55555555);
        count = ((count >> 2)  & 0x33333333) + (count & 0x33333333);
        count = ((count >> 4)  & 0x0F0F0F0F) + (count & 0x0F0F0F0F);
        count = ((count >> 8)  & 0x00FF00FF) + (count & 0x00FF00FF);
        count = ((count >> 16) & 0x0000FFFF) + (count & 0x0000FFFF);
        return count;
    }
    
    /**
        Returns the log base 2 of an integer greater than 0. The returned value
        is equal to {@code Math.floor(Math.log(n) / Math.log(2))}.
    */
    public static final int log2(int n) {
        //if (n <= 1) {
        //    throw new ArithmeticException("NaN");
        //}
        int count = 0;
        while (true) {
            n >>= 1;
            if (n == 0) {
                return count;
            }
            count++;
        }
        
/*
        int count = 0;
        
        if ((n & 0xFFFF0000) != 0) {
            n >>= 16;
            count = 16;
        }
        if ((n & 0xFF00) != 0) {
            n >>= 8;
            count |= 8;
        } 
        if ((n & 0xF0) != 0) {
            n >>= 4;
            count |= 4;
        } 
        if ((n & 0xC) != 0) {
            n >>= 2;
            count |= 2;
        } 
        if ((n & 0x2) != 0) {
            //n >>= 1;
            count |= 1;
        } 
        
        return count;
*/
    }
    
    //
    // Integer math
    //
    
    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number. 
    */
    public static final int clamp(int n, int min, int max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }
    
    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number.
    */
    public static final float clamp(float n, float min, float max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }

    /**
        Clamps a number between two values. If the number <= min returns min; if the number >= max
        returns max; otherwise returns the number. 
    */
    public static final double clamp(double n, double min, double max) {
        if (n <= min) {
            return min;
        }
        else if (n >= max) {
            return max;
        }
        else {
            return n;
        }
    }
    
    /**
        Returns the sign of a number.
    */
    public static final int sign(int n) {
        return (n > 0)?1:((n < 0)?-1:0);
    }
    
    
    /**
        Returns the sign of a number.
    */
    public static final int sign(double n) {
        return (n > 0)?1:((n < 0)?-1:0);
    }
    
    /**
        Returns the absolute value of a number.
    */
    public static final int abs(int n) {
        return (n >= 0)?n:-n;
        //return (n ^ (n >> 31)) - (n >> 31);
    }
    
    /**
        Divides the number, n, by the divisor, d, rounding the result to the 
        nearest integer.
    */
    public static final int intDivRound(int n, int d) {
        if ((d > 0) ^ (n > 0)) {
            return (n - (d >> 1)) / d;
        }
        else {
            return (n + (d >> 1)) / d;
        }
    }
    
    /**
        Divides the number, n, by the divisor, d, returning the nearest integer 
        less than or equal to the result.
    */
    public static final int intDivFloor(int n, int d) {
        if (d > 0) {
            if (n < 0) {
                return (n - d + 1) / d;
            }
            else {
                return n / d;
            }
        }
        else if (d < 0) {
            if (n > 0) {
                return (n - d - 1) / d;
            }
            else {
                return n / d;
            }
        }
        else {
            // d == 0 throws ArithmeticException
            return n / d;
        }
    }
    
    /**
        Divides the number, n, by the divisor, d, returning the nearest integer 
        greater than or equal to the result.
    */
    public static final int intDivCeil(int n, int d) {
        return -intDivFloor(-n, d);
    }
    
    //
    // Fixed-point math
    //
    
    /**
        Multiplies two fixed-point numbers together.
    */
    public static final int mul(int f1, int f2) {
        return (int)(((long)f1 * f2) >> FRACTION_BITS);
    }
    
    /**
        Multiplies two fixed-point numbers together.
    */
    public static final long mul(long f1, long f2) {
        return (f1 * f2) >> FRACTION_BITS;
    }
    
    /**
        Divides the first fixed-point number by the second fixed-point number.
    */
    public static final int div(int f1, int f2) {
        return (int)(((long)f1 << FRACTION_BITS) / f2);
    }
    
    /**
        Divides the first fixed-point number by the second fixed-point number.
    */
    public static final long div(long f1, long f2) {
        return (f1 << FRACTION_BITS) / f2;
    }
    
    /**
        Multiplies the first two fixed-point numbers together, then divides by
        the third fixed-point number.
    */
    public static final int mulDiv(int f1, int f2, int f3) {
        return (int)((long)f1 * f2 / f3);
    }
    
    /**
        Multiplies the first two fixed-point numbers together, then divides by
        the third fixed-point number.
    */
    public static final long mulDiv(long f1, long f2, long f3) {
        return f1 * f2 / f3;
    }
    
    //
    // Logs and powers
    //
    
    public static final int sqrt(int fx) {
        if (fx < 0) {
            throw new ArithmeticException("NaN");
        }
        
        if (fx == 0 || fx == ONE) {
            return fx;
        }
        
        // invert numbers less than one (if they aren't too small)
        boolean invert = false;
        if (fx < ONE && fx > 6) {
            invert = true;
            fx = div(ONE, fx);
        }
        
        int iterations = 16;
        if (fx > ONE) {
            // number of iterations == (number of bits in number) / 2
            int s = fx;
            iterations = 0;
            while (s > 0) {
                s >>=2;
                iterations++;
            }            
        }
        
        // Newton's iteration
        int l = (fx >> 1) + 1;
        for (int i=1; i<iterations; i++) {
            l = (l + div(fx, l)) >> 1;
        }
        
        // undo the inversion
        if (invert) {
            return div(ONE, l);
        }

        return l;
    }
    
    public static final long sqrt(long fx) {
        if (fx < 0) {
            throw new ArithmeticException("NaN");
        }
        
        if (fx == 0 || fx == ONE) {
            return fx;
        }
        
        // Invert numbers less than one (if they aren't too small)
        boolean invert = false;
        if (fx < ONE && fx > 6) {
            invert = true;
            fx = div(ONE, fx);
        }
        
        int iterations = 16;
        if (fx > ONE) {
            // Number of iterations == (number of bits in number) / 2
            long s = fx;
            iterations = 0;
            while (s > 0) {
                s >>= 2;
                iterations++;
            }            
        }
        
        // Newton's iteration
        long l = (fx >> 1) + 1;
        for (int i=1; i<iterations; i++) {
            l = (l + div(fx, l)) >> 1;
        }
        
        // Undo the inversion
        if (invert) {
            return div(ONE, l);
        }

        return l;
    }
    
    public static long dist(int x1, int y1, int x2, int y2) {
        long dx = x1 - x2;
        long dy = y1 - y2;
        return sqrt(mul(dx, dx) + mul(dy, dy));
    }
    
    //
    // Fixed-point Trigonometry
    //
    
    /**
        Returns the sine of the specified fixed-point radian value.
    */
    public static final int sin(int fx) {
        
        if (fx == 0) {
            return 0;
        }

        // reduce range to -2*pi and 2*pi
        int s = fx / TWO_PI;
        if (abs(s) >= (1<<TWO_PI_ERROR_FACTOR)) {
            // fix any error for large values of fx
            fx -= s*TWO_PI + (s >> TWO_PI_ERROR_FACTOR) * TWO_PI_ERROR;
        }
        else {
            fx -= s*TWO_PI;
        }
        
        // reduce range to -pi/2 and pi/2
        // this allows us to limit the number of iterations in the maclaurin series
        if (fx > PI) {
            fx = fx - TWO_PI;
        }
        else if (fx < -PI) {
            fx = fx + TWO_PI;
        }
        if (fx > ONE_HALF_PI) {
            fx = PI - fx;
        }
        else if (fx < -ONE_HALF_PI) {
            fx = -PI - fx;
        }
        
        // Helps with rotation appearance near 90, 180, 270, 360, etc.
        if (abs(fx) < 32) {
            return 0;
        }
        else if (abs(fx - ONE_HALF_PI) < 32) {
            return ONE;
        }
        else if (abs(fx + ONE_HALF_PI) < 32) {
            return -ONE;
        }
        
        // Maclaurin power series
        int fxSquared = mul(fx, fx);
        int d = mul((1 << INTERNAL_BITS) / (2*3*4*5*6*7*8*9), fxSquared);
        int c = mul(d - (1 << INTERNAL_BITS) / (2*3*4*5*6*7), fxSquared);
        int b = mul(c + (1 << INTERNAL_BITS) / (2*3*4*5), fxSquared);
        int a = mul(b - (1 << INTERNAL_BITS) / (2*3), fxSquared);
        int sine = mul(a + (1 << INTERNAL_BITS), fx);
        return sine >> (INTERNAL_BITS - FRACTION_BITS);
    }
    
    /**
        Returns the cosine of the specified fixed-point radian value.
    */
    public static final int cos(int fx) {
        if (fx == 0) {
            return CoreMath.ONE;
        }
        else if (fx < 0) {
            // make up for potential overflow
            return sin(ONE_HALF_PI - TWO_PI - fx);  
        }
        else {
            return sin(ONE_HALF_PI - fx);  
        }
    }
    
    /**
        Returns the tangent of the specified fixed-point radian value.
    */
    public static final int tan(int fx) {
        int cos = cos(fx);
        if (cos == 0) {
            return Integer.MAX_VALUE;
        }
        else {
            return div(sin(fx), cos);
        }
    }
    
    /**
        Returns the cotangent of the specified fixed-point radian value.
    */
    public static final int cot(int fx) {
        int sin = sin(fx);
        if (sin == 0) {
            return Integer.MAX_VALUE;
        }
        else {
            return div(cos(fx), sin);
        }
    }
    
    /**
        Returns the arcsine of the specified fixed-point value.
    */
    public static final int asin(int fx) {
        if (abs(fx) > ONE) {
            throw new ArithmeticException("NaN");
        }
        else if (fx == ONE) {
            return ONE_HALF_PI;
        }
        else if (fx == -ONE) {
            return -ONE_HALF_PI;
        }
        else {
            return atan(div(fx, sqrt(ONE - mul(fx, fx))));
        }
    }
    
    /**
        Returns the arccosine of the specified fixed-point value.
    */
    public static final int acos(int fx) {
        return ONE_HALF_PI - asin(fx);
    }

    /**
        Returns the arctangent of the specified fixed-point value.
    */
    public static final int atan(int fx) {
        boolean negative = false;
        boolean invert = false;
        if (fx == 0) {
            return 0;
        }
        if (fx < 0) {
            negative = true;
            fx = -fx;
        }
        
        // Avoid overflow
        if (fx > ONE) {
            invert = true;
            fx = div(ONE, fx);
        }
        
        // Approximation from Ranko at http://www.lightsoft.co.uk/PD/stu/stuchat37.html
        // r(x) = (x + 0.43157974*x^3)/(1 + 0.76443945*x^2 + 0.05831938*x^4)
        int fxPow2 = mul(fx, fx);
        int fxPow3 = mul(fxPow2, fx);
        int fxPow4 = mul(fxPow3, fx);
        int numer = fx + mul(28284, fxPow3);
        int denom = ONE + mul(50098, fxPow2) + mul(3822, fxPow4);
        int answer = div(numer, denom);
        
        if (invert) {
            answer = ONE_HALF_PI - answer;
        }
        if (negative) {
            answer = -answer;
        }
        return answer;
    }
    
    /**
        Returns in the range from -pi to pi.
    */
    public static final int atan2(int fy, int fx) {
        if (fy == 0) {
            if (fx < 0) {
                return PI;
            }
            else {
                return 0;
            }
        }
        else if (fx == 0) {
            if (fy < 0) {
                return -ONE_HALF_PI;
            }
            else {
                return ONE_HALF_PI;
            }
        }
        else {
            int answer = atan(abs(div(fy, fx)));
            if (fy > 0 && fx < 0) {
                return PI - answer;
            }
            else if (fy < 0 && fx < 0) {
                return answer - PI;
            }
            else if (fy < 0 && fx > 0) {
                return -answer;
            }
            else {
                return answer;
            }
        }
    }
    
    //
    // Random number generation and Noise functions
    //
    
    /**
        Returns a random integer from 0 to max, inclusive
    */
    public static final int rand(int max) {
        return rand(0, max);
    }
    
    /**
        Returns a random integer from min to max, inclusive
    */
    public static final int rand(int min, int max) {
        // Prevent overflow
        long range = (long)max - (long)min + 1;
        int value = (int)(min + (long)(Math.random() * range));
        
        // The Java 1.1 doc is unclear -  Math.random() could return 1.0.
        // In later versions of the doc, Math.random() is stated to return 
        // 0 <= x < 1.0. For the sake of compatibility, assume the
        // return value is 0 <= x <= 1.0
        // (In any case, Math.random() returning 1.0 will be a very rare case.)
        if (value > max) {
            return max;
        }
        else {
            return value;
        }
    }
    
    /**
        Returns a random double from 0 to max, inclusive
    */
    public static final double rand(double max) {
        return rand(0, max);
    }
    
    /**
        Returns a random double from min to max, inclusive
    */
    public static final double rand(double min, double max) {
        return min + (Math.random()*(max-min));
    }
    
    /**
        Returns a random boolean.
    */
    public static final boolean rand() {
        return (rand(0, 1) == 0);
    }
    
    /**
        Returns true if a random event occurs. 
        @param percent The probability of the event occuring, from 0 (never) to 100 (always).
    */
    public static final boolean randChance(int percent) {
        return (rand(1, 100) <= percent);
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int noise(int n) {           

        // A common noise function.
        // Note: these numbers are all primes.
        n = (n << 13) ^ n;
        return  n * (n * n * 15731 + 789221) + 1376312589;
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int noise(int x, int y) {
        return noise(x + y * 57);
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int smoothNoise(int x) {
        return (int)(((long)noise(x) >> 1) + ((long)noise(x-1) >> 2) + ((long)noise(x+1) >> 2));
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int smoothNoise(int x, int y) {
    
        long corners = (
            (long)noise(x-1, y-1) + 
            (long)noise(x+1, y-1) + 
            (long)noise(x-1, y+1) + 
            (long)noise(x+1, y+1)) >> 4;
        long sides = (
            (long)noise(x-1, y) + 
            (long)noise(x+1, y) + 
            (long)noise(x, y-1) + 
            (long)noise(x, y+1)) >> 3;
        long center = noise(x, y) >> 2;
    
        return (int)(corners + sides + center);
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int interpolatedNoise(int fx) {
    
        int x = fx >> FRACTION_BITS;
        int f = fx & FRACTION_MASK;
        
        if (f == 0) {
            return smoothNoise(x);
        }
        else {
            int n1 = smoothNoise(x);
            int n2 = smoothNoise(x + 1);
            
            return cosineInterpolate(n1 , n2, f);
        }
    }
    
    /**
        @return a 32-bit signed integer
    */
    public static final int interpolatedNoise(int fx, int fy) {

        int x = fx >> FRACTION_BITS;
        int y = fy >> FRACTION_BITS;
        
        int v1 = smoothNoise(x, y);
        int v2 = smoothNoise(x + 1, y);
        int v3 = smoothNoise(x, y + 1);
        int v4 = smoothNoise(x + 1, y + 1);
        
        int n1 = cosineInterpolate(v1, v2, fx & FRACTION_MASK);
        int n2 = cosineInterpolate(v3, v4, fx & FRACTION_MASK);
        
        return cosineInterpolate(n1, n2, fy & FRACTION_MASK);
    }

    /**
        @param fx fixed-point value
        @param persistence fixed-point value <= 1.
        @return a 32-bit signed integer
        
        From http://freespace.virgin.net/hugo.elias/models/m_perlin.htm
    */
    public static final int perlinNoise(int fx, int persistence, int numOctaves) {
        long total = 0;
        
        int amplitude = ONE - persistence;
        
        for (int i = 0; i < numOctaves; i++) {
            
            total += mul(interpolatedNoise(fx << i), amplitude);
            
            amplitude = mul(amplitude, persistence);
        
        }
        
        return (int)total;
    }
    
    /**
        @param fx fixed-point value
        @param fy fixed-point value
        @param persistence fixed-point value <= 1.
        @return a 32-bit signed integer
        
        From http://freespace.virgin.net/hugo.elias/models/m_perlin.htm
    */
    public static final int perlinNoise(int fx, int fy, int persistence, int numOctaves) {
        long total = 0;
        
        int amplitude = ONE - persistence;
        
        for (int i = 0; i < numOctaves; i++) {
            
            total += mul(interpolatedNoise(fx << i, fy << i), amplitude);
            
            amplitude = mul(amplitude, persistence);
        
        }
        
        return (int)total;
    }
    
    //
    // Interpolation
    //
    
    /**
        Performs a 1-dimensional linear interpolation between values n1 and n2. The f 
        parameter is a fixed-point value from 0.0 to 1.0. 
        If f is less than 0 or greater than 1, extrapolation is calculated.
    */
    public static final int interpolate(int n1, int n2, int f) {
        
        return mul(n1, ONE - f) + mul(n2, f);
        
        // This could cause overflow errors:
        // return n1 + mul(n2 - n1, f);
    }
    
    /**
        Performs a 1-dimensional cosine interpolation between values n1 and n2. The f 
        parameter is a fixed-point value from 0.0 to 1.0.
        If f is less than 0 or greater than 1, extrapolation is calculated.
    */
    public static final int cosineInterpolate(int n1, int n2, int f) {
        
        f = mul(f, PI);
        f = (ONE - cos(f)) >> 1;
        
        return interpolate(n1, n2, f);
    }
    
    // This supposedly resemembles cosine interpolation by using the graph
    // y = 3x^2 - 2x^3
    public static final int quickCurveInterpolate(int n1, int n2, int f) {
        
        int fSquared = mul(f, f);
        int fCubed = mul(fSquared, f);
        
        int fi = ONE - f;
        int fiSquared = mul(fi, fi);
        int fiCubed = mul(fiSquared, fi);
        
        return mul(n1, 3*fiSquared - 2*fiCubed) + mul(n2, 3*fSquared - 2*fCubed);
    }
    
    /**
        Performs a 1-dimensional cubic interpolation between values n1 and n2. The f 
        parameter is a fixed-point value from 0.0 to 1.0.
        If f is less than 0 or greater than 1, extrapolation is calculated.
    */
    public static final int cubicInterpolate(int n0, int n1, int n2, int n3, int f) {
        int f2 = mul(f, f);
        int f3 = mul(f2, f);
        
        long p = ((long)n3 - n2) - ((long)n0 - n1);
        long q = ((long)n0 - n1) - p;
        long r = (long)n2 - n0;
        long s = n1;
    
        return (int)(mul(p, f3) + mul(q, f2) + mul(r, f) + s);
    }
}
