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
    A Fixed is an fixed-point value (16 bits integer, 16 bits fraction)
    that can be animated over time. See {@link pulpcore.math.CoreMath} for
    methods to convert between integers and fixed-point numbers.
*/
public final class Fixed extends Property {

    public Fixed() {
        this(null, 0);
    }

    public Fixed(PropertyListener listener) {
        this(listener, 0);
    }
    
    //
    // Constructors with setters - 2 methods
    //
    
    public Fixed(int value) {
        this(null, value);
    }
    
    public Fixed(double value) {
        this(null, value);
    }
    
    //
    // Constructors with setters and listeners - 2 methods
    //
    
    public Fixed(PropertyListener listener, int value) {
        super(listener, CoreMath.toFixed(value));
    }
    
    public Fixed(PropertyListener listener, double value) {
        super(listener, CoreMath.toFixed(value));
    }
    
    //
    // Getters
    //

    public int getAsFixed() {
        return super.getValue();
    }
    
    public int getAsInt() {
        return CoreMath.toInt(super.getValue());
    }
    
    public int getAsIntFloor() {
        return CoreMath.toIntFloor(super.getValue());
    }
    
    public int getAsIntCeil() {
        return CoreMath.toIntCeil(super.getValue());
    }
    
    public int getAsIntRound() {
        return CoreMath.toIntRound(super.getValue());
    }
    
    public double get() {
        return CoreMath.toDouble(super.getValue());
    }
    
    public String toString() {
        return CoreMath.toString(super.getValue(), 7);
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
        if (obj instanceof Fixed) {
            return getAsFixed() == ((Fixed)obj).getAsFixed();
        }
        else if (obj instanceof Int) {
            long objValue = ((Int)obj).get();
            return getAsFixed() == (objValue << CoreMath.FRACTION_BITS);
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
            long objValue = ((Number)obj).longValue();
            return getAsFixed() == (objValue << CoreMath.FRACTION_BITS);
        }
        else {
            return false;
        }
    }
    
    public int hashCode() {
        // Same as java.lang.Float
        return Float.floatToIntBits(CoreMath.toFloat(super.getValue()));
    }
    
    //
    // Setters - 3 methods
    //
    
    protected void setValue(Number value) {
        setValue(CoreMath.toFixed(value.doubleValue()));
    }

    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void setAsFixed(int fValue) {
        setValue(fValue);
        setBehavior(null);
    }    
    
    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void set(int value) {
        setAsFixed(CoreMath.toFixed(value));
    }
    
    /**
        Sets the value of this property. 
        Any previous animations are stopped.
    */
    public void set(double value) {
        setAsFixed(CoreMath.toFixed(value));
    }
    
    //
    // Setters (with a delay) - 3 methods
    //
    
    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void setAsFixed(int fValue, int delay) {
        animateToFixed(fValue, 0, null, delay);
    }
    
    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void set(int value, int delay) {
        animateTo(value, 0, null, delay);
    }
    
    /**
        Sets the value of this property after a specific delay. 
        Any previous animations are stopped.
    */
    public void set(double value, int delay) {
        animateTo(value, 0, null, delay);
    }
    
    /**
        Binds this property to the specified property. If this property is given a new behavior,
        the binding is broken.
    */
    public void bindTo(Int property) {
        setBehavior(new Binding(this, property, false));
    }

    /**
        Bi-directionally binds this property to the specified property.
        If this property is given a new behavior, the specified property is then bi-directionally
        bound to this property. The binding is permanent, until a new bi-directional binding
        is specified.
    */
    public void bindWithInverse(Int property) {
        setBehavior(new Binding(this, property, true));
    }

    /**
        Binds this property to the specified property. If this property is given a new behavior,
        the binding is broken.
    */
    public void bindTo(Fixed property) {
        setBehavior(new Binding(this, property, false));
    }

    /**
        Bi-directionally binds this property to the specified property.
        If this property is given a new behavior, the specified property is then bi-directionally
        bound to this property.
    */
    public void bindWithInverse(Fixed property) {
        setBehavior(new Binding(this, property, true));
    }
    
    /**
        Binds this property to the specified function.
    */
    public void bindTo(BindFunction function) {
        setBehavior(new Binding(this, function));
    }
    
    //
    // Animation convenience methods - fixed-point
    //
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration) {
        setBehavior(new Tween(fFromValue, fToValue, duration));
    }
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration, Easing easing) {
        setBehavior(new Tween(fFromValue, fToValue, duration, easing));
    }
    
    /**
        Animates this property from the one fixed-point value (fFromValue) to another (fToValue).
        Any previous animations are stopped.
    */
    public void animateAsFixed(int fFromValue, int fToValue, int duration, Easing easing,
        int startDelay) 
    {
        setBehavior(new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration) {
        setBehavior(new Tween(getAsFixed(), fToValue, duration));
    }
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration, Easing easing) {
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing));
    }
    
    /**
        Animates this property from the current value to the specified fixed-point value.
        Any previous animations are stopped.
    */
    public void animateToFixed(int fToValue, int duration, Easing easing, int startDelay) {
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing, startDelay));
    }    

    //
    // Animation convenience methods - integer
    //
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        setBehavior(new Tween(fFromValue, fToValue, duration));
    }
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        setBehavior(new Tween(fFromValue, fToValue, duration, easing));
    }
    
    /**
        Animates this property from the one integer (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(int fromValue, int toValue, int duration, Easing easing, int startDelay) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration));
    }
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing));
    }
    
    /**
        Animates this property from the current value to the specified integer.
        Any previous animations are stopped.
    */
    public void animateTo(int toValue, int duration, Easing easing, int startDelay) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing, startDelay));
    }    

    //
    // Animation convenience methods - double
    //
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        setBehavior(new Tween(fFromValue, fToValue, duration));
    }
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration, Easing easing) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        setBehavior(new Tween(fFromValue, fToValue, duration, easing));
    }
    
    /**
        Animates this property from the one double (fromValue) to another (toValue).
        Any previous animations are stopped.
    */
    public void animate(double fromValue, double toValue, int duration, Easing easing, 
        int startDelay) 
    {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration));
    }
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration, Easing easing) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing));
    }
    
    /**
        Animates this property from the current value to the specified double.
        Any previous animations are stopped.
    */
    public void animateTo(double toValue, int duration, Easing easing, int startDelay) {
        int fToValue = CoreMath.toFixed(toValue); 
        setBehavior(new Tween(getAsFixed(), fToValue, duration, easing, startDelay));
    }
}