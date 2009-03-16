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

package pulpcore.platform;

import pulpcore.animation.Fixed;
import pulpcore.platform.AppContext;
import pulpcore.sound.Sound;
import pulpcore.sound.Playback;

public interface SoundEngine {
    
    public static final int STATE_INIT = 0;
    public static final int STATE_READY = 1;
    public static final int STATE_FAILURE = 2;
    public static final int STATE_DESTROYED = 3;
    
    /**
        @return one of {@link STATE_INIT}, {@link STATE_READY}, {@link STATE_FAILURE}, or
        {@link STATE_DESTROYED}.
    */
    public int getState();
    
    /**
        @return the number of sounds playing.
    */
    public int getNumSoundsPlaying();
    
    /**
        @return an array of the supported sample rates, or an empty array if the state
        is not {@link STATE_READY}.
    */
    public int[] getSupportedSampleRates();
    
    /**
        @return the maximum number of simultaneous sounds, or 0 if the state
        is not {@link STATE_READY}.
    */
    public int getMaxSimultaneousSounds();
    
    /**
        Plays a sound if the state is {@link STATE_READY}.
    */
    public Playback play(AppContext context, Sound sound, Fixed level, Fixed pan, boolean loop);
    
    /**
        Updates the sounds system.
    */
    public void update(int timeUntilNextUpdate);
    
    /**
        Stops all playing sounds and sets the state to {@link STATE_DESTROYED}. 
    */
    public void destroy();
    
}
    
