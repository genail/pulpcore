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

public class Tween extends Animation implements Behavior {

    private final int fromValue;
    private final int toValue;
    private int value;
    
    public Tween(int fromValue, int toValue, int duration) {
        this(fromValue, toValue, duration, null, 0);
    }
    
    public Tween(int fromValue, int toValue, int duration, Easing easing) {
        this(fromValue, toValue, duration, easing, 0);
    }
    
    public Tween(int fromValue, int toValue, int duration, Easing easing, int startDelay) {
        super(duration, easing, startDelay);
        this.fromValue = fromValue;
        this.toValue = toValue;
    }
    
    protected void updateState(int animTime) {
        if (getDuration() == 0) {
            if (animTime < 0) { 
                value = fromValue;
            }
            else {
                value = toValue;
            }
        }
        else {
            value = fromValue + CoreMath.mulDiv(toValue - fromValue, animTime, getDuration());
        }
    }
    
    public final int getFromValue() {
        return fromValue;
    }
    
    public final int getToValue() {
        return toValue;
    }
    
    protected final void setValue(int value) {
        this.value = value;
    }
    
    public final int getValue() {
        return value;
    }
}
