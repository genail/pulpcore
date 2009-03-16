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

import pulpcore.Build;

/**
    An Animation changes state over a specific duration. It is up to subclasses to
    implement the {@link #updateState(int)} method to change the state as the Animation is updated
    over time. An Animation is updated over time with its {@link #update(int)} method, which is
    typically called by a {@link Timeline} or, in the case of a {@link Tween}, by the 
    {@link Property} it is attached to.
    <p>
    A simple Animation starts immediately:
    <pre>
    |===================|
    0     duration     end
    
    ------- time ------->
    </pre>
    
    <p>Animations can delay before they start: 
    <pre>
    |--------------|===================|
    0  startDelay        duration     end
    
    ---------------- time --------------->
    </pre>
    
    <p>Animations can loop:
    <pre>
    |--------------|===================|===================|===================|
    0  startDelay        duration            duration           duration      end
    
    ----------------------------------- time ------------------------------------>
    </pre>
    
    <p>Animations can have a delay between loops:
    <pre>
    |--------------|================|------|================|------|================|
    0  startDelay       duration      loop      duration      loop      duration   end
                                      delay                   delay
    ------------------------------------- time ------------------------------------->
    </pre>
    <p>Also, Animations can have an {@link Easing} to make the Animation look more smooth.
*/
public abstract class Animation {
    
    /**
        Value indicating that the Animation loops forever.
        @see #loopForever()
        @see #getTotalDuration()
    */
    public static final int LOOP_FOREVER = -1;
    
    /* package-private */ static final int SECTION_START_DELAY = 0;
    /* package-private */ static final int SECTION_ANIMATION = 1;
    /* package-private */ static final int SECTION_LOOP_DELAY = 2;
    
    private final Easing easing;
    private final int startDelay;
    private int duration;
    private int numLoops;
    private int loopDelay;
    
    private int elapsedTime;
    
    public Animation(int duration) {
        this(duration, null, 0);
    }
    
    public Animation(int duration, Easing easing) {
        this(duration, easing, 0);
    }
    
    public Animation(int duration, Easing easing, int startDelay) {
        if (Build.DEBUG) {
            if (duration < 0) {
                throw new IllegalArgumentException("Duration cannot be < 0");
            }
            if (startDelay < 0) {
                throw new IllegalArgumentException("Start delay cannot be < 0");
            }
        }
        this.duration = duration;
        this.easing = easing;
        this.startDelay = startDelay;
        loop(1, 0);
    }
    
    /**
        Causes this animation to loop indefinitely. Same as {@code loop(LOOP_FOREVER, 0)}.
        <p>
        If the duration is 0 this call is ignored.
        @throws IllegalArgumentException if duration is 0
    */
    public final void loopForever() {
        loop(LOOP_FOREVER, 0);
    }
    
    /**
        Causes this animation to loop indefinitely. Same as {@code loop(LOOP_FOREVER, loopDelay)}.
        <p>
        If the duration is 0 and loopDelay is 0, this call is ignored.
        @param loopDelay The delay between the end of a duration and the start of a new one.
        @throws IllegalArgumentException if loopDelay is negative.
        @throws IllegalArgumentException if duration is 0 and loopDelay is 0.
    */
    public final void loopForever(int loopDelay) {
        loop(LOOP_FOREVER, loopDelay);
    }
    
    /**
        Sets the number of loops to play. A value of {@link #LOOP_FOREVER} causes this timeline
        to play indefinitely.
        <p>
        If looping (numLoops != 1), and the duration is 0, this call is ignored.
        @param numLoops The number of times to loop the duration. A value of 1 means the duration
        is played exactly once.
        @throws IllegalArgumentException if numLoops is not LOOP_FOREVER but is < 1.
    */
    public final void loop(int numLoops) {
        loop(numLoops, 0);
    }
    
    /**
        Sets the number of loops to play. A value of {@link #LOOP_FOREVER} causes this timeline
        to play indefinitely.
        <p>
        If looping (numLoops != 1), and the duration is 0, and loopDelay is 0, this call is ignored.
        @param numLoops The number of times to loop the duration. A value of 1 means the duration
        is played exactly once.
        @param loopDelay The delay between the end of one duration and the start of the next one.
        @throws IllegalArgumentException if numLoops is not LOOP_FOREVER but is < 1.
        @throws IllegalArgumentException if loopDelay is negative.
        @throws IllegalArgumentException if looping (numLoops != 1), duration is 0, and loopDelay is 0.
    */
    public final void loop(int numLoops, int loopDelay) {
        if (Build.DEBUG) {
            if (!(numLoops == LOOP_FOREVER || numLoops > 0)) {
                throw new IllegalArgumentException("numLoops must be > 0 or LOOP_FOREVER");
            }
            if (loopDelay < 0) {
                throw new IllegalArgumentException("Loop delay cannot be < 0");
            }
        }
        if (duration == 0 && numLoops != 1 && loopDelay == 0) {
            // Can't loop!
            this.numLoops = 1;
            //throw new IllegalArgumentException("Loop delay cannot be 0 if duration is 0 and " +
            //    "numLoops != 1");
        }
        else {
            this.numLoops = numLoops;
            this.loopDelay = loopDelay;
        }
    }
    
    public final int getStartDelay() {
        return startDelay;
    }
    
    public final int getDuration() {
        return duration;
    }
    
    public final Easing getEasing() {
        return easing;
    }
    
    /* package-private */ final void setDuration(int duration) {
        this.duration = duration;
    }
    
    public final int getNumLoops() {
        return numLoops;
    }
    
    public final int getLoopDelay() {
        return loopDelay;
    }
    
    /**
        Returns the total duration including the start delay and loops.
    */
    public final int getTotalDuration() {
        if (numLoops == LOOP_FOREVER) {
            return LOOP_FOREVER;
        }
        else {
            return startDelay + duration * numLoops + loopDelay * (numLoops - 1);
        }
    }
    
    public final int getTime() {
        return elapsedTime;
    }
    
    private final int getAnimTime() {
        return getAnimTime(elapsedTime);
    }
    
    private final int getAnimTime(int elapsedTime) {
        int animTime = elapsedTime - startDelay;
        if (animTime >= 0 && numLoops != 1) {
            animTime %= (duration + loopDelay);
        }
        return animTime;
    }
    
    /* package-private */ final int getSection() {
        return getSection(elapsedTime);
    }
    
    /* package-private */ final int getSection(int elapsedTime) {
        int animTime = getAnimTime(elapsedTime);
        if (animTime < 0) {
            return SECTION_START_DELAY;
        }
        else if (animTime < duration) {
            return SECTION_ANIMATION;
        }
        else {
            return SECTION_LOOP_DELAY;
        }
    }

    public final boolean isFinished() {
        if (numLoops == LOOP_FOREVER) {
            return false;
        }
        else {
            return elapsedTime >= getTotalDuration();
        }
    }
    
    /**
        Sets the current time to the end of this animation. If this animation loop forever,
        the time is set to the end of the current loop.
    */
    public final void fastForward() {
        if (numLoops == LOOP_FOREVER) {
            int loop = 0;
            int animTime = elapsedTime - startDelay;
            if (animTime >= 0) {
                loop = animTime / (duration + loopDelay);
            }
            this.numLoops = loop;
        }
        setTime(getTotalDuration());
    }
    
    public final void rewind() {
        setTime(0);
    }
    
    public boolean update(int elapsedTime) {
        return setTime(this.elapsedTime + elapsedTime);
    }
    
    private final boolean setTime(int newTime) {
        // Takes care of special case where elapsedTime, startDelay and duration are 0
        int oldState = (elapsedTime <= 0) ? SECTION_START_DELAY : getSection();
        this.elapsedTime = newTime;
        int newState = getSection();
        
        if (newState == SECTION_ANIMATION) {
            int animTime = getAnimTime();
            if (easing != null) {
                animTime = easing.ease(animTime, duration);
            }
            updateState(animTime);
            return true;
        }
        else if ((newState == SECTION_LOOP_DELAY && oldState != SECTION_LOOP_DELAY) ||
            (newState == SECTION_START_DELAY && oldState == SECTION_ANIMATION))
        {
            updateState(duration);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
        Updates the state based on the animation time, typically from 0 to {@link #getDuration()}.
        Note that the duration can be zero.
        @param animTime The animation time, typically from 0 to {@link #getDuration()}, although
        an {@link Easing} can cause the value to be outside those bounds.
    */
    protected abstract void updateState(int animTime);

}
