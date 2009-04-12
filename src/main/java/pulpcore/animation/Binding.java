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
    Helper class to facilitate property.bindTo() methods.
*/
/* package-private */ final class Binding implements Behavior, PropertyListener {

    private static final int FUNCTION_NONE = 0;
    private static final int FUNCTION_TO_INT = 1;
    private static final int FUNCTION_TO_FIXED = 2;
    private static final int FUNCTION_CUSTOM = 3;
    
    private final Property target;
    private final Property source;
    private final boolean bidirectional;
    private final BindFunction customFunction;
    private final int function;
    
    /* package-private */ Binding(Property target, BindFunction function) {
        this.target = target;
        this.source = null;
        this.bidirectional = false;
        this.customFunction = function;
        this.function = FUNCTION_CUSTOM;
    }
    
    /* package-private */ Binding(Property target, Property source, boolean bidirectional) {
        this.target = target;
        this.source = source;
        this.bidirectional = bidirectional;
        this.customFunction = null;
        if (target instanceof Int && source instanceof Fixed) {
            this.function = FUNCTION_TO_INT;
        }
        else if (target instanceof Fixed && source instanceof Int) {
            this.function = FUNCTION_TO_FIXED;
        }
        else {
            this.function = FUNCTION_NONE;
        }
        if (source != target) {
            source.addListener(this);
        }
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public Property getSource() {
        return source;
    }

    public Property getTarget() {
        return target;
    }
    
    public void propertyChange(Property property) {
        if (target.getBehavior() != this) {
            source.removeListener(this);
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
