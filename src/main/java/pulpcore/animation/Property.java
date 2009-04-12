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

/**
    The Property class is the base class for animating values. Properties have a value,
    a behavior to control how the value changes, and listeners to alert when the value changes.
    <p>
    Properties have an abstract 32-bit value, and it's up to subclasses to interpret that value
    with get and set methods.
*/
public abstract class Property {
    
    private Behavior behavior;
    private PropertyListener listener;
    private int value;
    
    /**
        Creates a property with the specified listener and initial value. The listener may be
        {@code null}. The behavior is {@code null}.
    */
    public Property(PropertyListener listener, int value) {
        this.listener = listener;
        this.value = value;
    }
    
    /**
        Sets the value for this property. If the new value is different from the old value,
        any listeners are alerted. The behavior, if any, is not changed.
        @param value the new value.
    */
    protected abstract void setValue(Number value);
    
    public abstract boolean equals(Object obj);

    public abstract int hashCode();
    
    /**
        Sets the value for this property. If the new value is different from the old value,
        any listeners are alerted. The behavior, if any, is not changed.
        @param value the new value.
    */
    protected final void setValue(int value) {
        if (this.value != value) {
            this.value = value;
            if (listener != null) {
                listener.propertyChange(this);
            }
        }
    }
    
    /**
        Gets the value for this property.
        @return the value.
    */
    protected final int getValue() {
        return value;
    }
    
    /**
        Sets the behavior for this property, which may be null. The value of this property is
        immediately set if {@code behavior.update(0)} returns {@code true}.
        @param behavior The new behavior.
    */
    public final void setBehavior(Behavior behavior) {
        Binding bidirectionalBinding = null;
        if (isBehaviorBidirectionalBinding()) {
            bidirectionalBinding = (Binding)this.behavior;
        }

        // Set behavior and update value immediately
        this.behavior = behavior;
        update(0);

        // Inverse the bi-directional binding, if any. 
        if (bidirectionalBinding != null) {
            Property source = bidirectionalBinding.getSource();
            Property target;
            if (isBehaviorBidirectionalBinding()) {
                target = ((Binding)this.behavior).getTarget();
            }
            else {
                target = bidirectionalBinding.getTarget();
            }
            source.setBehavior(new Binding(source, target, true));
        }
    }

    private boolean isBehaviorBidirectionalBinding() {
        return this.behavior != null && this.behavior instanceof Binding &&
                ((Binding)this.behavior).isBidirectional();
    }
    
    /**
        Gets the behavior for this property, or null if this property currently does not have
        a behavior.
        @return the behavior.
    */
    public final Behavior getBehavior() {
        return behavior;
    }
    
    /**
        Returns a newly allocated array of all the listeners registered on this Property.
        @return all of this Property's {@link PropertyListener}s or an empty array if no 
        listeners are registered.
    */
    public final PropertyListener[] getListeners() {
        if (listener == null) {
            return new PropertyListener[0];
        }
        else if (listener instanceof MultiListener) {
            return ((MultiListener)listener).getListeners();
        }
        else {
            return new PropertyListener[] { listener };
        }
    }
    
    /**
        Adds the specified listener to receive events from this Property. If the listener is 
        {@code null}, no exception is thrown and no action is performed.
        @param listener The listener to add.
    */
    public final void addListener(PropertyListener listener) {
        if (listener == null || this.listener == listener) {
            // Do nothing
        }
        else if (this.listener == null) {
            this.listener = listener;
        }
        else if (this.listener instanceof MultiListener) {
            ((MultiListener)this.listener).addListener(listener);
        }
        else {
            this.listener = new MultiListener(this.listener, listener);
        }
    }
    
    /**
        Removes the specified listener so that it no longer receives events from this Property.
        This method performs no function, nor does it throw an exception, if the listener specified 
        by the argument was not previously added to this Property. If the listener is {@code null}, 
        no exception is thrown and no action is performed.
        @param listener The listener to remove.
    */
    public final void removeListener(PropertyListener listener) {
        if (this.listener == listener) {
            this.listener = null;
        }
        else if (this.listener instanceof MultiListener) {
            MultiListener ml = ((MultiListener)this.listener);
            ml.removeListener(listener);
            if (ml.size() == 1) {
                this.listener = ml.get(0);
            }
        }
    }
    
    /**
        Updates this Property, possibly modifying its value if it has a {@link Behavior}.
        This method should be called once per frame, and a {@link pulpcore.sprite.Sprite}
        typically handles property updating.
        @param elapsedTime Elapsed time since the last update, in milliseconds.
    */
    public final void update(int elapsedTime) {
        if (behavior != null) {
            // Make a copy in case the behavior reference is changed in update() or setValue()
            Behavior b = behavior;
            boolean isActive = b.update(elapsedTime);
            if (isActive) {
                setValue(b.getValue());
            }
            if (behavior == b && b.isFinished()) {
                behavior = null;
            }
        }
    }
    
    /**
        Checks if this property has a behavior and it is not finished animating.
        @return true if this property has a behavior and it is not finished animating.
    */
    public final boolean isAnimating() {
        return (behavior != null && !behavior.isFinished());
    }
    
    /**
        Stops the behavior, if any.
        @param gracefully if true, the behavior is fast-forwarded to it's end and the property's 
        value is immediately set.
    */
    public final void stopAnimation(boolean gracefully) {
        if (!isBehaviorBidirectionalBinding()) {
            if (behavior != null && gracefully) {
                // Make a copy in case the behavior reference is changed in fastForward()
                Behavior b = behavior;
                b.fastForward();
                setValue(b.getValue());
            }
            behavior = null;
        }
    }
}