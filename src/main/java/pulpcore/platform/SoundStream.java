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

import java.lang.ref.WeakReference;
import pulpcore.animation.Fixed;
import pulpcore.math.CoreMath;
import pulpcore.sound.Playback;
import pulpcore.sound.Sound;


public class SoundStream {
    
    // The number of milliseconds to fade from a mute/unmute
    public static final int MUTE_TIME = 5;
    
    // Number of frames to render while animating. Should represent at least 1 ms at 44100Hz
    // (that is, greater than 44 frames)
    private static final int MAX_FRAMES_TO_RENDER_WHILE_ANIMATING = 64;
    
    private static final int STATE_PLAYING = 0;
    private static final int STATE_PAUSED = 1;
    private static final int STATE_TRACKING = 2;
    private static final int STATE_TRACKING_WHILE_PAUSED = 3;
    private static final int STATE_STOPPING = 4;
    private static final int STATE_STOPPED = 5;
    
    private final Fixed level;
    private final Fixed pan;
    private final AppContext context;
    private final Sound sound;
    private final int startFrame;
    private final int numLoopFrames;
    private final int stopFrame;
    
    private final Fixed outputLevel = new Fixed();
    
    private int frame;
    private int animationFrame;
    private boolean loop;
    private int state;
    
    private boolean lastMute;
    private double lastMasterVolume;
    
    private int trackingFrame;
    
    private Playback playback;
    private WeakReference playbackRef;
    
    public SoundStream(AppContext context, Sound sound, Fixed level, Fixed pan, 
        int startFrame, int numLoopFrames, int stopFrame)
    {
        this.level = level;
        this.pan = pan;
        this.context = context;
        this.sound = sound;
        this.startFrame = startFrame;
        this.numLoopFrames = numLoopFrames;
        this.stopFrame = stopFrame;
        
        this.loop = (numLoopFrames > 0);
        this.frame = 0;
        this.animationFrame = 0;
        
        this.lastMute = isMute();
        this.lastMasterVolume = getMasterVolume();
        this.outputLevel.set(lastMute ? 0 : lastMasterVolume);
        
        this.state = STATE_PLAYING;
        
        this.playback = new SoundStreamPlayback(level, pan);
        this.playbackRef = new WeakReference(playback);
    }
    
    // Called right after creation of SoundStream
    public Playback getPlayback() {
        return this.playback;
    }
    
    public boolean isFinished() {
        return (context == null || frame >= sound.getNumFrames());
    }
    
    private boolean isPaused() {
        return state == STATE_PAUSED || state == STATE_TRACKING_WHILE_PAUSED;
    }
        
    public void stop() {
        if (state != STATE_STOPPED) {
            state = STATE_STOPPING;
        }
    }
    
    private void checkPlayback() {
        if (playback == null) {
            Playback thisPlayback = (Playback)playbackRef.get();
            if (thisPlayback != null) {
                if (!isPaused()) {
                    // App code called setPause(false).
                    playback = thisPlayback;
                }
            }
            else if (isPaused()) {
                // Playback is paused, but no references to the Playback object exist, so
                // stop the sound
                stop();
            }
        }
        else if (playback.isPaused()) {
            // App code called setPause(true).
            playback = null;
        }
    }
    
    private void setFramePositionNow(int framePosition) {
        animationFrame = startFrame + framePosition;
        if (inLoop()) {
            frame = startFrame + (framePosition % numLoopFrames);
        }
        else if (framePosition > sound.getNumFrames()) {
            frame = sound.getNumFrames();
        }
        else {
            frame = framePosition;
        }
    }
        
    private boolean isMute() {
        return (context == null || context.isMute() || state != STATE_PLAYING);
    }
    
    private double getMasterVolume() {
        if (context == null) {
            return 0;
        }
        else {
            return context.getSoundVolume();
        }
    }
    
    private void advanceFramePosition(int numFrames) {
        if (state == STATE_STOPPED) {
            // Advance the frame so that the stream will reached the isFinished() state
            frame += numFrames;
        }
        else if (state != STATE_PLAYING && outputLevel.getAsFixed() == 0) {
            // Do nothing
        }
        else {
            int oldAnimationTime = getAnimationTime();
            int oldFrame = frame;
            int newFrame = frame + numFrames;
            animationFrame += numFrames;
            
            if (inLoop()) {
                frame = startFrame + ((newFrame - startFrame) % numLoopFrames);
            }
            else if (newFrame > sound.getNumFrames()) {
                frame = sound.getNumFrames();
            }
            else {
                frame = newFrame;
            }
            
            int elapsedTime = getAnimationTime() - oldAnimationTime;
            // TODO: possible thread issue if the PulpCore thread sets the animation of these?
            // Currently JavaSound runs in the animation thread, so it should be okay.
            level.update(elapsedTime);
            pan.update(elapsedTime);
            outputLevel.update(elapsedTime);
        }
    }
    
    private int getAnimationTime() {
        if (animationFrame < startFrame) {
            return 0;
        }
        else {
            return 1000 * (animationFrame - startFrame) / sound.getSampleRate();
        }
    }
    
    private boolean inLoop() {
        return (loop && frame >= startFrame && frame < startFrame + numLoopFrames);
    }
    
    public void render(byte[] dest, int destOffset, int destChannels, int numFrames) {
        boolean mute = isMute();
        double masterVolume = getMasterVolume();
        if (context == null || context.getStage() == null) {
            // Context destroyed!
            mute = true;
            loop = false;
        }
        
        // Gradually change sound volume over time to reduce popping
        if (lastMute != mute || lastMasterVolume != masterVolume) {
            double currLevel = outputLevel.get();
            double goalLevel = mute ? 0 : masterVolume;
            outputLevel.animateTo(goalLevel, MUTE_TIME);
            lastMute = mute;
            lastMasterVolume = masterVolume;
        }
        
        // Last render when stopping
        if (state == STATE_STOPPING && outputLevel.getAsFixed() == 0) {
            loop = false;
            mute = true;
            frame = stopFrame;
            state = STATE_STOPPED;
        }
        else if (state == STATE_TRACKING && outputLevel.getAsFixed() == 0) {
            state = STATE_PLAYING;
            setFramePositionNow(trackingFrame);
        }
        
        checkPlayback();
        
        int destFrameSize = destChannels * 2;
        
        while (numFrames > 0) {
            
            boolean isAnimating = level.isAnimating() || outputLevel.isAnimating() || 
                pan.isAnimating();
            int currLevel = getCurrLevel();
            int currPan = getCurrPan();
            
            int framesToRender = numFrames;
            if (isAnimating) {
                // Only render a few frames, then recalcuate animation parameters
                framesToRender = Math.min(MAX_FRAMES_TO_RENDER_WHILE_ANIMATING, framesToRender);
            }
            if (inLoop()) {
                // Don't render past loop boundary
                framesToRender = Math.min(framesToRender, startFrame + numLoopFrames - frame); 
            }
            
            // Figure out the next level and pan (for interpolation)
            int startFrame = frame;
            advanceFramePosition(framesToRender);
            int nextLevel = getCurrLevel();
            int nextPan = getCurrPan();
            
            // Render
            if (currLevel > 0 || nextLevel > 0) {
                sound.getSamples(dest, destOffset, destChannels, startFrame, framesToRender);
            }
            render(dest, destOffset, destChannels, framesToRender, 
                currLevel, nextLevel, currPan, nextPan);
            
            // Inc offsets
            numFrames -= framesToRender;
            destOffset += framesToRender * destFrameSize;
        }
    }
    
    private int getCurrLevel() {
        int currLevel = level.getAsFixed();
        if (frame >= sound.getNumFrames()) {
            currLevel = 0;
        }
        if (currLevel <= 0) {
            currLevel = 0;
            loop = false;
        }
        else {
            currLevel = CoreMath.mul(currLevel, outputLevel.getAsFixed());
        }
        return currLevel;
    }
    
    private int getCurrPan() {
        int currPan = pan.getAsFixed();
        if (currPan < -CoreMath.ONE) {
            currPan = -CoreMath.ONE;
        }
        else if (currPan > CoreMath.ONE) {
            currPan = CoreMath.ONE;
        }
        return currPan;
    }
    
    private static void render(byte[] data, int offset, int channels,
        int numFrames, int startLevel, int endLevel, int startPan, int endPan)
    {
        int frameSize = channels * 2;
        
        if (startLevel <= 0 && endLevel <= 0) {
            // Mute
            int length = numFrames * frameSize;
            for (int i = 0; i < length; i++) {
                data[offset++] = 0;
            }
        }
        else if (channels == 1 || (startPan == 0 && endPan == 0)) {
            // No panning (both stereo and mono rendering)
            if (startLevel != CoreMath.ONE || endLevel != CoreMath.ONE) {
                int numSamples = numFrames*channels;
                int level = startLevel;
                int levelInc = (endLevel - startLevel) / numSamples;
                for (int i = 0; i < numSamples; i++) {
                    int input = getSample(data, offset); 
                    int output = (input * level) >> CoreMath.FRACTION_BITS;
                    setSample(data, offset, output);
                    
                    offset += 2;
                    level += levelInc;
                }
            }
        }
        else {
            // Stereo sound with panning
            int startLeftLevel4LeftInput;
            int startLeftLevel4RightInput;
            int startRightLevel4LeftInput;
            int startRightLevel4RightInput;
            int endLeftLevel4LeftInput;
            int endLeftLevel4RightInput;
            int endRightLevel4LeftInput;
            int endRightLevel4RightInput;
            if (startPan < 0) {
                startLeftLevel4LeftInput = CoreMath.ONE + startPan / 2;
                startLeftLevel4RightInput = -startPan / 2;
                startRightLevel4LeftInput = 0;
                startRightLevel4RightInput = CoreMath.ONE + startPan;
            }
            else {
                startLeftLevel4LeftInput = CoreMath.ONE - startPan;
                startLeftLevel4RightInput = 0;
                startRightLevel4LeftInput = startPan / 2;
                startRightLevel4RightInput = CoreMath.ONE - startPan / 2;
            }
            if (endPan < 0) {
                endLeftLevel4LeftInput = CoreMath.ONE + endPan / 2;
                endLeftLevel4RightInput = -endPan / 2;
                endRightLevel4LeftInput = 0;
                endRightLevel4RightInput = CoreMath.ONE + endPan;
            }
            else {
                endLeftLevel4LeftInput = CoreMath.ONE - endPan;
                endLeftLevel4RightInput = 0;
                endRightLevel4LeftInput = endPan / 2;
                endRightLevel4RightInput = CoreMath.ONE - endPan / 2;
            }
            if (startLevel != CoreMath.ONE) {
                startLeftLevel4LeftInput = CoreMath.mul(startLevel, startLeftLevel4LeftInput);
                startLeftLevel4RightInput = CoreMath.mul(startLevel, startLeftLevel4RightInput);
                startRightLevel4LeftInput = CoreMath.mul(startLevel, startRightLevel4LeftInput);
                startRightLevel4RightInput = CoreMath.mul(startLevel, startRightLevel4RightInput);
            }
            if (endLevel != CoreMath.ONE) {
                endLeftLevel4LeftInput = CoreMath.mul(endLevel, endLeftLevel4LeftInput);
                endLeftLevel4RightInput = CoreMath.mul(endLevel, endLeftLevel4RightInput);
                endRightLevel4LeftInput = CoreMath.mul(endLevel, endRightLevel4LeftInput);
                endRightLevel4RightInput = CoreMath.mul(endLevel, endRightLevel4RightInput);
            }
            
            int leftLevel4LeftInput = startLeftLevel4LeftInput;
            int leftLevel4RightInput = startLeftLevel4RightInput;
            int rightLevel4LeftInput = startRightLevel4LeftInput;
            int rightLevel4RightInput = startRightLevel4RightInput;
            int leftLevel4LeftInputInc = 
                (endLeftLevel4LeftInput - startLeftLevel4LeftInput) / numFrames;
            int leftLevel4RightInputInc = 
                (endLeftLevel4RightInput - startLeftLevel4RightInput) / numFrames;
            int rightLevel4LeftInputInc = 
                (endRightLevel4LeftInput - startRightLevel4LeftInput) / numFrames;
            int rightLevel4RightInputInc = 
                (endRightLevel4RightInput - startRightLevel4RightInput) / numFrames;
            for (int i = 0; i < numFrames; i++) {
                int leftInput = getSample(data, offset);
                int rightInput = getSample(data, offset + 2);
                int leftOutput = 
                    (leftInput * leftLevel4LeftInput + rightInput * leftLevel4RightInput) >>
                    CoreMath.FRACTION_BITS;
                int rightOutput = 
                    (leftInput * rightLevel4LeftInput + rightInput * rightLevel4RightInput) >>
                    CoreMath.FRACTION_BITS;                        
                setSample(data, offset, leftOutput);
                setSample(data, offset + 2, rightOutput);
                
                offset += 4;
                leftLevel4LeftInput += leftLevel4LeftInputInc;
                leftLevel4RightInput += leftLevel4RightInputInc;
                rightLevel4LeftInput += rightLevel4LeftInputInc;
                rightLevel4RightInput += rightLevel4RightInputInc;
            }
        }
    }
    
    public static int getSample(byte[] data, int offset) {
        // Signed little endian
        return (data[offset + 1] << 8) | (data[offset] & 0xff);
    }
    
    public static void setSample(byte[] data, int offset, int sample) {
        // Signed little endian
        data[offset] = (byte)sample;
        data[offset + 1] = (byte)(sample >> 8);
    }
    
    public class SoundStreamPlayback extends Playback {
        
        public SoundStreamPlayback(Fixed level, Fixed pan) {
            super(level, pan);
        }
        
        public int getSampleRate() {
            return sound.getSampleRate();
        }
        
        public int getFramePosition() {
            if (animationFrame < startFrame) {
                return 0;
            }
            else {
                return animationFrame - startFrame;
            }
        }
        
        public void setFramePosition(int framePosition) {
            if (getFramePosition() != framePosition) {
                trackingFrame = framePosition;
                if (state == STATE_PAUSED) {
                    //state = STATE_TRACKING_WHILE_PAUSED;
                    setFramePositionNow(trackingFrame);
                }
                else if (state == STATE_PLAYING) {
                    state = STATE_TRACKING;
                }
            }
        }
        
        public void setPaused(boolean paused) {
            if (paused && state == STATE_PLAYING) {
                state = STATE_PAUSED;
            }
            else if (paused && state == STATE_TRACKING) {
                state = STATE_TRACKING_WHILE_PAUSED;
            }
            else if (!paused && state == STATE_PAUSED) {
                state = STATE_PLAYING;
            }
            else if (!paused && state == STATE_TRACKING_WHILE_PAUSED) {
                state = STATE_PLAYING;
                setFramePositionNow(trackingFrame);
            }
        }
        
        public boolean isPaused() {
            return SoundStream.this.isPaused();
        }
            
        public void stop() {
            SoundStream.this.stop();
        }
        
        public boolean isFinished() {
            return SoundStream.this.isFinished();
        }        
    }
}
