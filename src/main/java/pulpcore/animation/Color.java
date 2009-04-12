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

package pulpcore.animation;

import pulpcore.math.CoreMath;

/**
    An Color is a 32-bit ARGB value that can be animated over time.
*/
public class Color extends Property {
    
    public Color() {
        this(null, 0);
    }
    
    public Color(PropertyListener listener) {
        this(listener, 0);
    }
    
    public Color(int argbColor) {
        this(null, argbColor);
    }
    
    public Color(PropertyListener listener, int argbColor) {
        super(listener, argbColor);
    }
    
    //
    // Set
    //
    
    protected void setValue(Number value) {
        setValue(value.intValue());
    }
    
    public void set(int argbColor) {
        setValue(argbColor);
        setBehavior(null);
    }
    
    public void set(int argbColor, int delay) {
        animateTo(argbColor, 0, null, delay);
    }
    
    /**
        Gets the packed, 32-bit ARGB value of this color.
    */
    public int get() {
        return super.getValue();
    }
    
    public String toString() {
        String s = Integer.toHexString(get());
        while (s.length() < 8) {
            s = '0' + s;
        }
        return "0x" + s;
    }
    
    /**
        Returns true if the specified object is a
        {@code Color} or {@link java.lang.Integer} and its value is equal to this value.
    */
    public boolean equals(Object obj) {
        if (obj instanceof Color) {
            return get() == ((Color)obj).get();
        }
        else if (obj instanceof Integer) {
            return get() == ((Integer)obj).intValue();
        }
        else {
            return false;
        }
    }
    
    public int hashCode() {
        // Same as java.lang.Integer
        return get();
    }

    //
    // Convenience methods
    //

    /**
        Binds this property to the specified property. If this property is given a new behavior,
        the binding is broken.
    */
    public void bindTo(Color property) {
        setBehavior(new Binding(this, property, false));
    }

    /**
        Bi-directionally binds this property to the specified property.
        If this property is given a new behavior, the specified property is then bi-directionally
        bound to this property. The binding is permanent, until a new bi-directional binding
        is specified.
    */
    public void bindWithInverse(Color property) {
        setBehavior(new Binding(this, property, true));
    }
    
    /**
        Binds this property to the specified function.
    */
    public void bindTo(BindFunction function) {
        setBehavior(new Binding(this, function));
    }
    
    /**
        Animates this Int from the one value (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromARGB, int toARGB, int duration) {
        setBehavior(new ColorTween(fromARGB, toARGB, duration));
    }
    
    /**
        Animates this Int from the one value (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromARGB, int toARGB, int duration, Easing easing) {
        setBehavior(new ColorTween(fromARGB, toARGB, duration, easing));
    }
    
    /**
        Animates this Int from the one value (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromARGB, int toARGB, int duration, Easing easing, int startDelay) {
        setBehavior(new ColorTween(fromARGB, toARGB, duration, easing, startDelay));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toARGB, int duration) {
        setBehavior(new ColorTween(get(), toARGB, duration));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toARGB, int duration, Easing easing) {
        setBehavior(new ColorTween(get(), toARGB, duration, easing));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toARGB, int duration, Easing easing, int startDelay) {
        setBehavior(new ColorTween(get(), toARGB, duration, easing, startDelay));
    }
    
    
    /* package-private */ static class ColorTween extends Tween {
        
        public ColorTween(int fromARGB, int toARGB, int duration) {
            this(fromARGB, toARGB, duration, null, 0);
        }
        
        public ColorTween(int fromARGB, int toARGB, int duration, Easing easing) {
            this(fromARGB, toARGB, duration, easing, 0);
        }
        
        public ColorTween(int fromARGB, int toARGB, int duration, Easing easing, int startDelay) { 
            super(fromARGB, toARGB, duration, easing, startDelay);
        }
        
        protected void updateState(int animTime) {
            int dur = getDuration();
            if (dur == 0) {
                super.updateState(animTime);
            }
            else if (animTime == 0) {
                setValue(getFromValue());
            }
            else if (animTime == dur) {
                setValue(getToValue());
            }
            else {
                int v1 = getFromValue();
                int v2 = getToValue();
                
                int a1 = v1 >>> 24;
                int b1 = (v1 >> 16) & 0xff;
                int c1 = (v1 >> 8) & 0xff;
                int d1 = v1 & 0xff;
                
                int a2 = v2 >>> 24;
                int b2 = (v2 >> 16) & 0xff;
                int c2 = (v2 >> 8) & 0xff;
                int d2 = v2 & 0xff;
                
                int a = a1 + CoreMath.mulDiv(a2 - a1, animTime, dur);
                int b = b1 + CoreMath.mulDiv(b2 - b1, animTime, dur);
                int c = c1 + CoreMath.mulDiv(c2 - c1, animTime, dur);
                int d = d1 + CoreMath.mulDiv(d2 - d1, animTime, dur);
                
                if (a < 0) a = 0;
                if (b < 0) b = 0;
                if (c < 0) c = 0;
                if (d < 0) d = 0;
                
                if (a > 255) a = 255;
                if (b > 255) b = 255;
                if (c > 255) c = 255;
                if (d > 255) d = 255;
                
                setValue((a << 24) | (b << 16) | (c << 8) | d);
            }
        }
    }
}
