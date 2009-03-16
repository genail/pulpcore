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

import java.util.ArrayList;
import pulpcore.Build;
import pulpcore.CoreSystem;

/**
    A SoundSequence is a immutable sequence of sampled sounds that can be played together
    smoothly with no skips or breaks between sounds. The sounds can be a mix of mono and
    stereo Sounds, or other SoundSequences. 
    <p>
    All sounds in a sequence must have the sample sample rate.
*/
public final class SoundSequence extends Sound {
    
    private ArrayList children = new ArrayList();
    private int numFrames;
    
    /**
        @throws IllegalArgumentException if not all sounds in the sequence have the same 
        sample rate.
    */
    public SoundSequence(Sound[] sounds) {
        super(sounds[0].getSampleRate());
        for (int i = 0; i < sounds.length; i++) {
            if (sounds[i].getSampleRate() != getSampleRate()) {
                if (Build.DEBUG) {
                    CoreSystem.print("The first sound (sound[0]) in SoundSequence is " + 
                        getSampleRate() + "Hz, but sound[" + i + "] is " + 
                        sounds[i].getSampleRate() + "Hz.");
                }
                throw new IllegalArgumentException();
            }
            if (sounds[i] instanceof SoundSequence) {
                children.addAll(((SoundSequence)sounds[i]).children);
            }
            else if (sounds[i] != null) {
                children.add(sounds[i]);
            }
        }
        numFrames = 0;
        for (int i = 0; i < children.size(); i++) {
            numFrames += ((Sound)children.get(i)).getNumFrames();
        }
    }
    
    public int getNumFrames() {
        return numFrames;
    }
    
    public void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames)
    {
        int destFrameSize = getSampleSize() * destChannels;
        
        int childIndex = 0;
        int childFrame = 0;
        while (childIndex < children.size() && numFrames > 0) {
            Sound child = (Sound)children.get(childIndex);
            int childNumFrames = child.getNumFrames();
            if (srcFrame >= childFrame + childNumFrames) {
                childIndex++;
                childFrame += childNumFrames;
            }
            else {
                int childSrcFrame = srcFrame - childFrame;
                int framesToGet = Math.min(numFrames, childNumFrames - childSrcFrame);
                child.getSamples(dest, destOffset, destChannels, childSrcFrame, framesToGet);
                destOffset += framesToGet * destFrameSize;
                srcFrame += framesToGet;
                numFrames -= framesToGet;
            }
        }
    
        if (numFrames > 0) {
            int length = numFrames * destFrameSize;
            for (int i = 0; i < length; i++) {
                dest[destOffset++] = 0;
            }
        }
    }
}
