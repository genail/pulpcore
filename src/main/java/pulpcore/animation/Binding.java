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
    Helper class to facilitate property.bindTo() methods.
*/
/* package-private */ final class Binding implements Behavior, PropertyListener {

    /* package-private */ static final int FUNCTION_NONE = 0;
    /* package-private */ static final int FUNCTION_TO_INT = 1;
    /* package-private */ static final int FUNCTION_TO_FIXED = 2;
    /* package-private */ static final int FUNCTION_CUSTOM = 3;
    
    private final Property target;
    private final Property source;
    private final BindFunction customFunction;
    private final int function;
    
    /* package-private */ Binding(Property target, BindFunction function) {
        this.target = target;
        this.source = null;
        this.customFunction = function;
        this.function = FUNCTION_CUSTOM;
    }
    
    /* package-private */ Binding(Property target, Property source) {
        this(target, source, FUNCTION_NONE);
    }
    
    /* package-private */ Binding(Property target, Property source, int function) {
        this.target = target;
        this.source = source;
        this.customFunction = null;
        this.function = function;
        if (source != target) {
            source.addListener(this);
        }
    }
    
    public void propertyChange(Property property) {
        if (target.getBehavior() != this) {
            target.removeListener(this);
        }
        else {
            target.setValue(getValue());
        }
    }
    
    public boolean update(int elapsedTime) {
        return true;
    }
    
    public void fastForward() {
        // Do nothing
    }

    public boolean isFinished() {
        return false;
    }
    
    public int getValue() {
        switch (function) {
            default: case FUNCTION_NONE: return source.getValue();
            case FUNCTION_TO_INT: return CoreMath.toInt(source.getValue());
            case FUNCTION_TO_FIXED: return CoreMath.toFixed(source.getValue());
            case FUNCTION_CUSTOM: 
                target.setValue(customFunction.f());
                return target.getValue();
        }
    }
    
}
