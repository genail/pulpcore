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
package pulpcore.animation;

/**
    The Easing class provides functions to ease property animation in a non-linear way.
    Most apps will not need to create an instance of this class.
*/
public class Easing {
    
    private static final int TYPE_IN = 0;
    private static final int TYPE_OUT = 1;
    private static final int TYPE_IN_OUT = 2;
    
    private static final int FUNCTION_LINEAR = 0;
    private static final int FUNCTION_QUADRADIC = 1;
    private static final int FUNCTION_CUBIC = 2;
    private static final int FUNCTION_QUARTIC = 3;
    private static final int FUNCTION_QUINTIC = 4;
    private static final int FUNCTION_BACK = 5;
    private static final int FUNCTION_ELASTIC = 6;
    
    public static final Easing NONE = new Easing(TYPE_IN, FUNCTION_LINEAR);
    
    public static final Easing REGULAR_IN = new Easing(TYPE_IN, FUNCTION_QUADRADIC);
    public static final Easing REGULAR_OUT = new Easing(TYPE_OUT, FUNCTION_QUADRADIC);
    public static final Easing REGULAR_IN_OUT = new Easing(TYPE_IN_OUT, FUNCTION_QUADRADIC);
    
    public static final Easing STRONG_IN = new Easing(TYPE_IN, FUNCTION_QUINTIC);
    public static final Easing STRONG_OUT = new Easing(TYPE_OUT, FUNCTION_QUINTIC);
    public static final Easing STRONG_IN_OUT = new Easing(TYPE_IN_OUT, FUNCTION_QUINTIC);
    
    public static final Easing BACK_IN = new Easing(TYPE_IN, FUNCTION_BACK);
    public static final Easing BACK_OUT = new Easing(TYPE_OUT, FUNCTION_BACK);
    public static final Easing BACK_IN_OUT = new Easing(TYPE_IN_OUT, FUNCTION_BACK);
    
    public static final Easing ELASTIC_IN = new Easing(TYPE_IN, FUNCTION_ELASTIC);
    public static final Easing ELASTIC_OUT = new Easing(TYPE_OUT, FUNCTION_ELASTIC);
    public static final Easing ELASTIC_IN_OUT = new Easing(TYPE_IN_OUT, FUNCTION_ELASTIC);
    
    private final int type;
    private final int function;
    private final float strength;
    
    protected Easing() {
        this(TYPE_IN, FUNCTION_LINEAR);
    }
    
    private Easing(int type) {
        this(type, FUNCTION_LINEAR);
    }
    
    private Easing(int type, int function) {
        this(type, function, 1);
    }
    
    private Easing(int type, int function, double stength) {
        this.type = type;
        this.function = function;
        this.strength = (float)stength;
    }
    
    public Easing(Easing easing, double strength) {
        this(easing.type, easing.function, strength);
    }
    
    public final int ease(int time, int duration) { 
        if (time <= 0 || duration <= 0) {
            return 0;
        }
        else if (time >= duration) {
            return duration;
        }
        
        final double t = (double)time / duration;
        double easedT;
        
        switch (type) {
            
            default:
                easedT = t;
                break;
            
            case TYPE_IN: 
                easedT = ease(t);
                break;
                
            case TYPE_OUT: 
                easedT = 1-ease(1-t);
                break;
            
            case TYPE_IN_OUT: 
                if (t < 0.5) {
                    easedT = ease(2*t)/2;
                }
                else {
                    easedT = 1-ease(2 - 2*t)/2;
                }
                break;
        }
        
        if (strength != 1) {
            easedT = strength * easedT + (1 - strength) * t;
        }
        
        return (int)Math.round(easedT * duration);
    }
    
    protected double ease(double t) {
        
        double t2;
        double t3;
        
        switch (function) {
            
            default: case FUNCTION_LINEAR: 
                return t;
            
            case FUNCTION_QUADRADIC:
                return t * t;
            
            case FUNCTION_CUBIC: 
                return t * t * t;
            
            case FUNCTION_QUARTIC: 
                t2 = t * t;
                return t2 * t2;
                
            case FUNCTION_QUINTIC: 
                t2 = t * t;
                return t2 * t2 * t;
                
            case FUNCTION_BACK:
                t2 = t * t;
                t3 = t2 * t;
                return t3 + t2 - t;
                
            case FUNCTION_ELASTIC:
                t2 = t * t;
                t3 = t2 * t;
                
                double scale = t2 * (2*t3 + t2 - 4*t + 2);
                double wave = (float)-Math.sin(t * 3.5 * Math.PI);
                
                return scale * wave;
        }
    }
}

