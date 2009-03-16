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

package pulpcore.sound;

import pulpcore.animation.Fixed;

/**
    The Playback class allows a Sound be modified while it is playing.
*/
public abstract class Playback {
    
    /**
        The level, from 0 to 1. The level animation is synchronized with the Sound's playback time,
        and can't be animated with a Timeline.
    */
    public final Fixed level;
    
    /**
        The pan, from -1 to 1. The pan animation is synchronized with the Sound's playback time,
        and can't be animated with a Timeline.
    */
    public final Fixed pan;
    
    public Playback(Fixed level, Fixed pan) {
        this.level = level;
        this.pan = pan;
    }
    
    /**
        Gets the sound's sample rate.
    */
    public abstract int getSampleRate();
    
    /**
        Gets the current playback position in frames.
    */
    public abstract int getFramePosition();
    
    /**
        Gets the current playback position in microseconds.
    */
    public final long getMicrosecondPosition() {
        return 1000000L * getFramePosition() / getSampleRate();
    }
    
    /**
        Sets the current plaback position to the first frame. This method returns immediately, 
        but there may be a slight delay (a few milliseconds) before the frame position is actually 
        set (to avoid clicks/pops).
    */
    public final void rewind() {
        setFramePosition(0);
    }
    
    /**
        Sets the current playback position. This method returns immediately, but there may be a 
        slight delay (a few milliseconds) before the frame position is actually set (to 
        avoid clicks/pops).
    */
    public abstract void setFramePosition(int framePosition);
    
    /**
        Sets the current playback position, in microseconds. This method returns immediately, 
        but there may be a slight delay (a few milliseconds) before the frame position is actually 
        set (to avoid clicks/pops).
    */
    public final void setMicrosecondPosition(long pos) {
        setFramePosition((int)(pos * getSampleRate() / 1000000));
    }
    
    /**
        Pauses this playback or continues playback after pausing. A paused sound may continue to
        send data (in the form of inaudible sound) to the sound engine until the playback is
        stopped.
        <p>
        If playback is paused and the garbage collector determines no references to this Playback 
        object exist (for example, when music is paused, but the Scene with the Playback object 
        no longer exists), the playback will automatically stop, and data will no longer be sent to 
        the sound engine. For example:
        <pre>
        Sound music = Sound.load("music.wav");
        Playback musicPlayback = music.play();
        ...
        // Playback is paused, but still active and ready to be unpaused.
        musicPlayback.setPaused(true); 
        ...
        // Paused playback will automatically become inactive at a future point in time.
        musicPlayback = null; 
        </pre>
    */
    public abstract void setPaused(boolean paused);
    
    /**
        Checks if this playback is currently paused and playback can continue.
    */
    public abstract boolean isPaused();
    
    /**
        Stops this playback as soon as possible. A stopped playback cannot be restarted.
    */
    public abstract void stop();
    
    /**
        Returns true if the playback is finished. Once finished, the playback cannot be restarted.
    */
    public abstract boolean isFinished();
    
}
