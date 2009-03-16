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

import pulpcore.math.CoreMath;

/**
    An Int is an integer value that can be animated over time.
*/
public class Int extends Property {

    /**
        Constructs a new Int object with no listener and the value of zero.
    */
    public Int() {
        this(null, 0);
    }
    
    /**
        Constructs a new Int object with the specified listener and the value of zero.
        The listener is notified when the value is modified.
    */
    public Int(PropertyListener listener) {
        this(listener, 0);
    }
    
    /**
        Constructs a new Int object with the specified value and no listener.
    */
    public Int(int value) {
        this(null, value);
    }
    
    /**
        Constructs a new Int object with the specified listener and value.
        The listener is notified when the value is modified.
    */
    public Int(PropertyListener listener, int value) {
        super(listener, value);
    }
    
    public int get() {
        return super.getValue();
    }
    
    public String toString() {
        return Integer.toString(get());
    }
    
    /**
        Returns true if the specified object is an 
        {@code Int},
        {@link Fixed}, 
        {@link java.lang.Byte},
        {@link java.lang.Short},
        {@link java.lang.Integer},
        {@link java.lang.Long},
        {@link java.lang.Float}, or
        {@link java.lang.Double}, and
        its value is equal to this value.
    */
    public boolean equals(Object obj) {
        if (obj instanceof Int) {
            return get() == ((Int)obj).get();
        }
        else if (obj instanceof Fixed) {
            return (((long)get()) << CoreMath.FRACTION_BITS) == ((Fixed)obj).getAsFixed();
        }
        else if (obj instanceof Double) {
            return get() == ((Double)obj).doubleValue();
        }
        else if (obj instanceof Float) {
            return get() == ((Float)obj).floatValue();
        }
        else if (
            obj instanceof Byte || 
            obj instanceof Short || 
            obj instanceof Integer || 
            obj instanceof Long) 
        {
            return get() == ((Number)obj).longValue();
        }
        else {
            return false;
        }
    }
    
    public int hashCode() {
        // Same as java.lang.Integer
        return get();
    }
    
    protected void setValue(Number value) {
        setValue(value.intValue());
    }
    
    /**
        Sets the value of this Int. 
        Any previous animations are stopped.
    */
    public void set(int value) {
        setValue(value);
        setBehavior(null);
    }
    
    /**
        Sets the value of this Int after a specific delay. 
        Any previous animations are stopped.
    */
    public void set(int value, int delay) {
        animateTo(value, 0, null, delay);
    }
    
    //
    // Convenience methods
    //
    
    /**
        Binds this property to the specified property.
    */
    public void bindTo(Int property) {
        setBehavior(new Binding(this, property));
    }
    
    /**
        Binds this property to the specified property.
    */
    public void bindTo(Fixed property) {
        setBehavior(new Binding(this, property, Binding.FUNCTION_TO_INT));
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
    public void animate(int fromValue, int toValue, int duration) {
        setBehavior(new Tween(fromValue, toValue, duration));
    }
    
    /**
        Animates this Int from the one value (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing) {
        setBehavior(new Tween(fromValue, toValue, duration, easing));
    }
    
    /**
        Animates this Int from the one value (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing, int startDelay) {
        setBehavior(new Tween(fromValue, toValue, duration, easing, startDelay));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration) {
        setBehavior(new Tween(get(), toValue, duration));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing) {
        setBehavior(new Tween(get(), toValue, duration, easing));
    }
    
    /**
        Animates this Int from the current value to the specified value.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing, int startDelay) {
        setBehavior(new Tween(get(), toValue, duration, easing, startDelay));
    }
}