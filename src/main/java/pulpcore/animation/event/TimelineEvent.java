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

package pulpcore.animation.event;

import pulpcore.animation.Animation;

/**
    A TimelineEvent is an abstract class that can perform a certain action
    after a specific delay. TimelineEvents are added to and executed by a 
    {@link pulpcore.animation.Timeline}. Subclasses implement the {@link #run } 
    method.
    <p>
    An anonymous inner class can be used in a Scene2D to create a code block 
    that is executed after a delay:
    <pre>
    int delay = 1000; // milliseconds
    addEvent(new TimelineEvent(delay) {
        public void run() {
            // Code to execute after the delay
        }
    });  
    </pre>
    <p>
    A TimelineEvent only executes once - if it is in a looping Timeline, 
    it only executes the first time.
*/
public abstract class TimelineEvent extends Animation implements Runnable {

    private boolean hasExecuted;
    
    public TimelineEvent(int delay) {
        super(0, null, Math.max(1, delay));
        hasExecuted = false;
    }
    
    protected void updateState(int animTime) {
        if (!hasExecuted) {
            run();
            synchronized (this) {
                hasExecuted = true;
                this.notify();
            }
        }
    }
    
    public final boolean hasExecuted() {
        return hasExecuted;
    }
    
    public abstract void run();
}