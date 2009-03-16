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

import java.io.EOFException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import pulpcore.animation.Fixed;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.AppContext;
import pulpcore.platform.SoundEngine;
import pulpcore.platform.SoundStream;
import pulpcore.util.ByteArray;

/**
    The Sound class is a base class for sampled sound.
    @see SoundSequence
*/
public abstract class Sound {
    
    private static final SoundClip NO_SOUND = new SoundClip("null", new byte[0], 8000, false);
    
    // For u-law conversion. 132 * ((1 << i) - 1)
    private static final int[] EXP_TABLE = { 
        0, 132, 396, 924, 1980, 4092, 8316, 16764
    };
    
    private static final int SAMPLE_SIZE = 2;
    
    // HashMap<String, WeakReference<Sound>>
    private static HashMap loadedSounds = new HashMap();
    
    private final int sampleRate;
    
    /**
        Creates a new Sound with the specified sample rate.
        @param sampleRate the sample rate (samples per second, per channel). 
    */
    public Sound(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    /**
        Returns the sample rate - the number of samples played per second, per channel. 
    */
    public final int getSampleRate() {
        return sampleRate;
    }
    
    /**
        Returns the sample size - the number of bytes in each sample.
        This method always returns 2.
    */
    public final int getSampleSize() {
        return SAMPLE_SIZE;
    }
  
    /**
        Gets the duration of this clip in milliseconds.
        @return the duration of this clip in milliseconds.
    */
    public final long getDuration() {
        return 1000L * getNumFrames() / sampleRate;
    }
    
    /**
        Gets the length of this sound, expressed in the number of frames. For mono sounds,
        a frame is one sample, for stereo sounds, a frame consists of two samples - one for
        the left channel, and one for the right channel.
    */
    public abstract int getNumFrames();
    
    /**
        Copies a sequence of samples from this Sound to a byte array as 
        signed, little endian, 16-bit PCM format.
        @param dest the destination buffer.
        @param destOffset the offset, in bytes, in the destination buffer.
        @param destChannels The number of channels of the destination (1 or 2).
        @param srcFrame the frame position to start copying from.
        @param numFrames the number of frames to copy.
    */
    public abstract void getSamples(byte[] dest, int destOffset, int destChannels,
        int srcFrame, int numFrames);

    //
    // Play methods
    //
    
    /**
        Plays this sound clip. The Sound is played at full volume with no panning.
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback play() {
        return play(new Fixed(1.0), new Fixed(0), false);
    }
        
    /**
        Plays this sound clip with the specified colume level (0.0 to 1.0). 
        The level may have a property animation attached.
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback play(Fixed level) {
        return play(level, new Fixed(0), false);
    }
    
    /**
        Plays this sound clip with the specified level (0.0 to 1.0) and pan (-1.0 to 1.0).
        The level and pan may have a property animation attached. 
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback play(Fixed level, Fixed pan) {
        return play(level, pan, false);
    }
    
    /**
        Plays this sound clip with the specified level (0.0 to 1.0) and pan (-1.0 to 1.0),
        optionally looping. The level and pan may have a property animation attached.
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public Playback play(Fixed level, Fixed pan, boolean loop) {

        AppContext context = CoreSystem.getThisAppContext();
        SoundEngine soundEngine = CoreSystem.getPlatform().getSoundEngine();
        Playback playback = null;
        
        if (context != null && soundEngine != null && this.getNumFrames() > 0) {
            try {
                playback = soundEngine.play(context, this, level, pan, loop);
            }
            catch (Exception ex) {
                if (Build.DEBUG) CoreSystem.print("Sound play", ex);
            }
        }
        
        if (playback == null) {
            SoundStream stream = new SoundStream(null, this, level, pan, 0, 0, this.getNumFrames());
            playback = stream.getPlayback();
            playback.stop();
        }
        return playback;
    }

    /**
        Loops this sound clip. The Sound is played at full volume with no panning.
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback loop() {
        return play(new Fixed(1.0), new Fixed(0), true);
    }
    
    /**
        Loops this sound clip with the specified volume level (0.0 to 1.0). 
        The level may have a property animation attached.
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback loop(Fixed level) {
        return play(level, new Fixed(0), true);
    }
    
    /**
        Loops this sound clip with the specified volume level (0.0 to 1.0) and pan (-1.0 to 1.0).
        The level and pan may have a property animation attached. 
        @return a Playback object for this unique sound playback (one Sound can have many 
        simultaneous Playback objects) or {@code null} if the sound could not be played.
        @see pulpcore.animation.event.SoundEvent
        @see pulpcore.CoreSystem#setMute(boolean)
        @see pulpcore.CoreSystem#isMute()
    */
    public final Playback loop(Fixed level, Fixed pan) {
        return play(level, pan, true);
    }

    //
    // Loading
    //
    
    /**
        Creates an sound clip with the specified samples 
        (signed, little endian, 16-bit PCM format). 
        @return The sound, or a zero-length Sound on error.
    */
    public static Sound load(byte[] data, int sampleRate, boolean stereo) {
        if (!CoreSystem.isSoundEngineAvailable()) {
            return NO_SOUND;
        }
        
        // Check sample rate
        if (data == null) {
            return NO_SOUND;
        }
        try {
            return new SoundClip("raw", data, sampleRate, stereo);
        }
        catch (IllegalArgumentException ex) {
            if (Build.DEBUG) CoreSystem.print("Data length invalid");
            return NO_SOUND;
        }
    }
    
    /**
        Loads a sound from the the asset catalog.
        The sound can either be a .au file (8-bit u-law) or 
        a .wav file (16-bit, signed, PCM). Both mono and stereo is supported.
        <p>
        Ogg Vorbis is supported with an add-on. See 
        <a href="http://code.google.com/p/pulpcore/wiki/OggHowTo">
        http://code.google.com/p/pulpcore/wiki/OggHowTo</a>
        for details.
        <p>
        This method never returns {@code null}. If the sound cannot be loaded, or there is no
        sound engine available, a zero-length Sound is returned.
        <p>
        Sounds are internally cached (using a WeakReference), and if the sound was previously 
        loaded, this method may return the same reference.
        @param soundAsset The name of a AU, WAV, or OGG sound file.
        @return The sound, or a zero-length Sound on error.
    */
    public static Sound load(String soundAsset) {
        
        if (!CoreSystem.isSoundEngineAvailable()) {
            return NO_SOUND;
        }
        
        // Attempt to load from the cache
        // NOTE: we may need to disable caching if the sound engine can't play multiple copies
        // of the same sound simultaneously. Currently the JavaSound engine can.
        WeakReference soundRef = (WeakReference)loadedSounds.get(soundAsset);
        if (soundRef != null) {
            Sound sound = (Sound)soundRef.get();
            if (sound != null) {
                return sound;
            }
            else {
                loadedSounds.remove(soundAsset);
            }
        }
        
        ByteArray in = Assets.get(soundAsset);
        if (in == null) {
            return NO_SOUND;
        }
        
        Sound sound = null;
        try {
            if (soundAsset.toLowerCase().endsWith(".au")) {
                sound = loadAU(in, soundAsset);
            }
            else if (soundAsset.toLowerCase().endsWith(".wav")) {
                sound = loadWAV(in, soundAsset);
            }
            else if (soundAsset.toLowerCase().endsWith(".ogg")) {
                sound = loadOGG(in, soundAsset);
            }
            else {
                if (Build.DEBUG) CoreSystem.print("Unknown audio file: " + soundAsset);
            }
        }
        catch (EOFException ex) {
            if (Build.DEBUG) CoreSystem.print("Error loading sound: " + soundAsset);
        }
        
        if (sound != null) {
            loadedSounds.put(soundAsset, new WeakReference(sound));
            return sound;
        }
        else {
            return NO_SOUND;
        }
    }
    
    private static Sound loadAU(ByteArray in, String soundAsset) throws EOFException {
        // Make sure magic number is ".snd"
        int magicNumber = in.readInt();
        if (magicNumber != 0x2e736e64) {
            if (Build.DEBUG) CoreSystem.print("Not a sun audio file: " + soundAsset);
            return NO_SOUND;
        }
        int offset = in.readInt();
        int length = in.readInt();
        int encoding = in.readInt();
        int sampleRate = in.readInt();
        int numChannels = in.readInt();
        boolean stereo = (numChannels == 2);
        
        // Sometimes 8012Hz is used
        if (sampleRate >= 8000 && sampleRate <= 8100) {
            sampleRate = 8000;
        }
        
        // Check basic format
        if (encoding != 1 || numChannels < 1 || numChannels > 2) { 
            if (Build.DEBUG) {
                CoreSystem.print("Not an 8-bit u-law mono or stereo file: " + soundAsset);
            }
            return NO_SOUND;
        }
        
        // Skip to the data section
        in.setPosition(offset);
        
        // Convert u-law to linear
        byte[] data = new byte[length*2];
        offset = 0;
        for (int i = 0; i < length; i++) {
            int sample = ulaw2linear(in.readByte());
            SoundStream.setSample(data, offset, sample);
            offset+=2;
        }
    
        return new SoundClip(soundAsset, data, sampleRate, stereo);
    }
    
    private static Sound loadWAV(ByteArray in, String soundAsset) throws EOFException {
    
        final int RIFF = 0x52494646; // little endian
        final int RIFX = 0x52494658; // big endian (not used)
        final int WAVE = 0x57415645;
        
        final int FMT = 0x20746d66;
        final int DATA = 0x61746164;
        
        // Make sure magic number is "RIFF", followed by 4 bytes, and then "WAVE"
        int magic1 = in.readInt();
        in.readInt();
        int magic2 = in.readInt();
        if (magic1 != RIFF || magic2 != WAVE) {
            if (Build.DEBUG) CoreSystem.print("Not a WAV file: " + soundAsset);
            return NO_SOUND;
        }
        
        in.setByteOrder(ByteArray.LITTLE_ENDIAN);
        
        int sampleRate = -1;
        boolean stereo = false;
        
        // Read each chunk. Only process the FMT and DATA chunks; ignore others.
        // Return once the data chunk is found.
        while (true) {
            int chunkID = in.readInt();
            int chunkSize = in.readInt();
            
            if (chunkID == FMT) {
                int format = in.readShort();
                int numChannels = in.readShort();
                sampleRate = in.readInt();
                int avgByteRate = in.readInt();
                int blockAlign = in.readShort();
                int bitsPerSample = in.readShort();
                stereo = (numChannels == 2);
                
                // Sometimes 8012Hz is used
                if (sampleRate >= 8000 && sampleRate <= 8100) {
                    sampleRate = 8000;
                }
                
                // Check basic format
                if (format != 1 || bitsPerSample != 16 || 
                    numChannels < 1 || numChannels > 2) 
                {
                    if (Build.DEBUG) {
                        CoreSystem.print("Not an 16-bit PCM mono or stereo file: " + soundAsset);
                    }
                    return NO_SOUND;
                }
            }
            else if (chunkID == DATA) {
                if (sampleRate == -1) {
                    if (Build.DEBUG) {
                        CoreSystem.print("Bad WAV file: " + soundAsset);
                    }
                    return NO_SOUND;
                }
                byte[] data = in.getData();
                int dataOffset = in.position();
                int dataLength = chunkSize;
                return new SoundClip(soundAsset, data, dataOffset, dataLength, sampleRate, 
                    stereo);
            }
            else {
                // Skip this unknown chunk
                in.setPosition(in.position() + chunkSize);
            }
        }
    }
    
    private static Sound loadOGG(ByteArray in, String soundAsset) throws EOFException {
        /*
            JOrbis is LGPL and PulpCore is BSD, so we're keeping everything nice and
            separate. Assume:
            1. PulpCore wasn't compiled with the JOrbis jars
            2. PulpCore wasn't compiled with pulpcore.sound.JOrbisAdapter
            3. The JOrbis jars might be available at runtime.
        */
        Class jOrbisAdapterClass;
        try {
            jOrbisAdapterClass = Class.forName("pulpcore.sound.JOrbisAdapter");
        }
        catch (Exception ex) {
            if (Build.DEBUG) {
                CoreSystem.print("JOrbisAdapter, the jorbis jar, and/or " + 
                    "the jogg jar is not available: " + soundAsset);
            }
            return NO_SOUND;
        }
        
        // Meh.. this will work for now
        try {
            Method method = jOrbisAdapterClass.getMethod("decode", new Class[] {
                ByteArray.class, String.class });
            Sound sound = (Sound)method.invoke(null, new Object[] { in, soundAsset } );
            if (sound == null) {
                sound = NO_SOUND;
            }
            return sound;
        }
        catch (Exception ex) {
            if (Build.DEBUG) {
                CoreSystem.print("Error loading: " + soundAsset, ex);
            }
            return NO_SOUND;
        }
    }
        
    //
    // For u-law conversion
    //
    
    /** 
        Converts an 8-bit ulaw sample to a signed 16-bit linear sample.
        From http://www.speech.cs.cmu.edu/comp.speech/Section2/Q2.7.html
    */
    private static int ulaw2linear(byte ulawbyte) {
        // 1-bit sign, 3-bit exponent, 4-bit mantissa
        int ulaw = ~ulawbyte;
        int sign = ulaw & 0x80;
        int exponent = (ulaw >> 4) & 0x07;
        int mantissa = ulaw & 0x0f;
        int sample = EXP_TABLE[exponent] + (mantissa << (exponent + 3));

        if (sign != 0) {
            return -sample;
        }
        else { 
            return sample;
        }
    }            
}
