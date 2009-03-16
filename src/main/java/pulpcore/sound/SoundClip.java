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

import pulpcore.platform.SoundStream;

/**
    A SoundClip represents a sampled sound clip. 
    Samples are in signed, little endian, 16-bit PCM format.
*/
/* package-private */ class SoundClip extends Sound {
    
    private final String assetName; 
    private final byte[] data;
    private final int dataOffset;
    private final int dataLength;
    private final int numChannels;
    private final int frameSize;
    private final int numFrames;
    
    /**
        Creates an sound clip with the specified samples 
        (signed, little endian, 16-bit PCM format). 
    */
    public SoundClip(String assetName, byte[] data, int sampleRate, boolean stereo) {
        this(assetName, data, 0, data.length, sampleRate, stereo);
    }
        
    
    /**
        Creates an sound clip with the specified samples 
        (signed, little endian, 16-bit PCM format). 
    */
    public SoundClip(String assetName, byte[] data, int dataOffset, int dataLength, 
        int sampleRate, boolean stereo) 
    {
        super(sampleRate);
        this.assetName = assetName;
        this.data = data;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
        this.numChannels = stereo ? 2 : 1;
        this.frameSize = getSampleSize() * numChannels;
        if ((dataLength % frameSize) != 0) {
            throw new IllegalArgumentException();
        }
        this.numFrames = dataLength / frameSize;
    }
    
    /**
        Returns the number of channels of this sound: 1 for mono or 2 for stereo.
    */
    public final int getNumChannels() {
        return numChannels;
    }
    
    public final int getNumFrames() {
        return numFrames;
    }
    
    public void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames)
    {
        if (srcFrame + numFrames > this.numFrames) {
            throw new IllegalArgumentException();
        }
        
        int srcOffset = srcFrame * frameSize + dataOffset;
        
        if (getNumChannels() == destChannels) {
            // Mono-to-mono or stereo-to-stereo
            int length = numFrames * frameSize;
            System.arraycopy(data, srcOffset, dest, destOffset, length);
        }
        else if (getNumChannels() == 1 && destChannels == 2) {
            // Mono-to-stereo
            for (int i = 0; i < numFrames; i++) {
                byte a = data[srcOffset++];
                byte b = data[srcOffset++];
                dest[destOffset++] = a;
                dest[destOffset++] = b;
                dest[destOffset++] = a;
                dest[destOffset++] = b;
            }
        }
        else {
            // Stereo-to-mono
            for (int i = 0; i < numFrames; i++) {
                int left = getSample(srcOffset);
                int right = getSample(srcOffset + 2);
                int sample = (left + right) >> 1;
                SoundStream.setSample(dest, destOffset, sample);
                destOffset += 2;
                srcOffset += frameSize;
            }
        }
    }
    
    private int getSample(int offset) {
        return SoundStream.getSample(data, offset);
    }
    
    private void setSample(int offset, int sample) {
        SoundStream.setSample(data, offset, sample);
    }
    
    public String toString() {
        return assetName;
    }
}
