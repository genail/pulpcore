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

package pulpcore.image;

import pulpcore.CoreSystem;
import pulpcore.math.CoreMath;

/**
    An AnimatedImage is a CoreImage that contains multiple frames of animation.
    Frames are either physical or virtual.
*/
public class AnimatedImage extends CoreImage {
    
    /** Physical frames */
    private final CoreImage[] frames;
    
    /** Virtual frames */
    private int[] frameSequence;
    
    /** Duration of each frame (virtual is it exists; otherwise, physical) */
    private int[] frameDuration;
    private int totalDuration;
    private boolean loop;
    
    // Fields used during animation
    private boolean playing;
    /** Elapsed time in the current frame */
    private int animTime;
    private int currentFrame;
    
    /** Frame at the end of the last update */
    private int lastFrame;
    
    private final boolean differentHotSpotPerFrame;
    
    // For Image Manipulation
    private AnimatedImage(AnimatedImage image, CoreImage[] frames) {
        super(frames[0].getWidth(), frames[0].getHeight(), frames[0].isOpaque(), null);
        
        setHotspot(image.getHotspotX(), image.getHotspotY()); 
        
        this.frames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            this.frames[i] = new CoreImage(frames[i]);
        }
        
        setSequence(image.frameSequence, image.frameDuration, image.loop);
        differentHotSpotPerFrame = image.differentHotSpotPerFrame;
        animTime = image.animTime;
        currentFrame = image.currentFrame;
        lastFrame = image.lastFrame;
        playing = image.playing;
    }
    
    /**
        Creates an AnimatedImage from a list of images. The internal raster data array is shared.
        @throws IllegalArgumentException if not all the images have the same dimensions.
    */
    public AnimatedImage(CoreImage[] frames) {
        super(frames[0].getWidth(), frames[0].getHeight(), frames[0].isOpaque(), null);
        
        this.frames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            if (frames[i].getWidth() != getWidth() || frames[i].getHeight() != getHeight()) {
                throw new IllegalArgumentException();
            }
            this.frames[i] = new CoreImage(frames[i]);
        }
        
        totalDuration = 0;
        differentHotSpotPerFrame = true;
        setFrame(0);
        playing = true;
    }
    
    /**
        Creates a copy of the specified AnimatedImage. The internal raster data array is shared.
    */
    public AnimatedImage(AnimatedImage image) {
        super(image);
        
        frames = new CoreImage[image.frames.length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new CoreImage(image.frames[i]);
        }
        setSequence(image.frameSequence, image.frameDuration, image.loop);
        differentHotSpotPerFrame = image.differentHotSpotPerFrame;
        setFrame(0);
        playing = true;
    }
    
    /**
        Creates an AnimatedImage by spliting a image into frames on a grid.
    */
    public AnimatedImage(CoreImage image, int numFramesAcross, int numFramesDown) { 
        super(image.getWidth() / numFramesAcross, image.getHeight() / numFramesDown,
            image.isOpaque(), null);
        
        setHotspot(image.getHotspotX(), image.getHotspotY()); 
            
        frames = image.split(numFramesAcross, numFramesDown);
        differentHotSpotPerFrame = false;
        totalDuration = 0;
        setFrame(0);
        playing = true;
    }
    
    /**
        Sets the duration for each physical frame, and optionally sets the animation to loop.
        Virtual frames are not used.
    */
    public void setFrameDuration(int duration, boolean loop) {
        if (frameDuration == null || frameDuration.length != frames.length) {
            frameDuration = new int[frames.length];
        }
        
        this.loop = loop;
        this.frameSequence = null;
        this.totalDuration = duration * frames.length;
        
        for (int i = 0; i < frameDuration.length; i++) {
            frameDuration[i] = duration;
        }
        
        setFrame(0);
    }
    
    /**
        Sets the frame sequence (virtual frames), the duration for each virtual frame, and 
        optionally sets the animation to loop.
        @param frameSequence an array where each element points to an index in the list of
        physical frames. Can be null.
        @param frameDuration the duration of each virtual frame. If frameSequence is not null, 
        frameDuration must be the same length as frameSequence. If frameSequence is null, 
        frameDuration must be the same length as the number of physical frames.
    */
    public void setSequence(int[] frameSequence, int[] frameDuration, boolean loop) {
        this.frameSequence = CoreSystem.arraycopy(frameSequence);
        this.frameDuration = CoreSystem.arraycopy(frameDuration);
        this.loop = loop;
        this.totalDuration = 0;
        if (frameDuration != null) {
            for (int i = 0; i < frameDuration.length; i++) {
                this.totalDuration += frameDuration[i];
            }
        }
        setFrame(0);
    }

    /**
        Returns true if this AnimatedImage loops.
    */
    public boolean isLooping() {
        return loop;
    }

    /**
        Gets the number of frames in this AnimatedImage. If this AnimatedImage has virtual
        frames, this method returns the number of virtual frames. Otherwise, the number of 
        physical frames is returned.
    */
    public int getNumFrames() {
        if (frameSequence == null) {
            return frames.length;
        }
        else {
            return frameSequence.length;
        }
    }
    
    /**
        Gets the total duration of this AnimatedImage.
    */
    public int getDuration() {
        return totalDuration;
    }
    
    /**
        Gets the duration of the specified virtual frame.
    */
    public int getDuration(int frame) {
        if (frameDuration == null || frame < 0 || frame >= frameDuration.length) {
            return 0;
        }
        else {
            return frameDuration[frame];
        }
    }
    
    /**
        Gets the image for the specified virtual frame.
    */
    public CoreImage getImage(int frame) {
        if (frameSequence != null) {
            return frames[frameSequence[frame]];
        }
        else {
            return frames[frame];
        }
    }
    
    /**
        Sets the current frame. 
    */
    public void setFrame(int frame) {
        setFrame(frame, true);
    }
    
    private void setFrame(int frame, boolean resetAnimTime) {
        currentFrame = frame;
        if (resetAnimTime) {
            animTime = 0;
        }
        
        CoreImage image = getImage(frame);
        setData(image.getData());
        setOpaque(image.isOpaque());
        if (differentHotSpotPerFrame) {
            setHotspot(image.getHotspotX(), image.getHotspotY()); 
        }
    }
    
    /**
        Gets the current frame.
    */
    public int getFrame() {
        if (frameSequence != null) {
            return frameSequence[currentFrame];
        }
        else {
            return currentFrame;
        }
    }

    public boolean update(int elapsedTime) {
        if (!playing || frameDuration == null || totalDuration <= 0) {
            return false;
        }
        
        animTime += elapsedTime;
        
        while (animTime >= frameDuration[currentFrame]) {
            animTime -= frameDuration[currentFrame];
            if (currentFrame == frameDuration.length - 1) {
                if (loop) {
                    setFrame(0, false);
                }
                else {
                    playing = false;
                    break;
                }
            }
            else {
                setFrame(currentFrame + 1, false);
            }
        }
        
        int frame = getFrame();
        if (frame != lastFrame) {
            lastFrame = frame;
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
        Returns true if this AnimatedImage is currently playing.
        @see #start()
        @see #stop()
        @see #pause()
    */
    public boolean isPlaying() {
        return playing;
    }
    
    /**
        Starts playing the animation. By defauly, an AnimatedImage is started on creation; this
        method is useful after calling {@link #stop()} or {@link #pause()}.
        @see #isPlaying()
        @see #stop()
        @see #pause()
    */
    public void start() {
        playing = true;
    }
    
    /**
        Stops the animation without changing the current frame.
        @see #isPlaying()
        @see #start()
        @see #stop()
    */
    public void pause() {
        playing = false;
    }
    
    /**
        Stops the animation and sets the frame to zero.
        @see #isPlaying()
        @see #start()
        @see #pause()
    */
    public void stop() {
        playing = false;
        setFrame(0);
    }
    
    //
    // Image Manipulation - overrides CoreImage's methods
    //
    
    public CoreImage crop(int x, int y, int w, int h) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].crop(x, y, w, h);
        }
        return new AnimatedImage(this, newFrames);
    }
    
    public CoreImage rotate(int angle, boolean sizeAsNeeded) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].rotate(angle, sizeAsNeeded);
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        int x = getHotspotX() - getWidth()/2;
        int y = getHotspotY() - getHeight()/2;
        int cos = CoreMath.cos(angle);
        int sin = CoreMath.sin(angle);
        newImage.setHotspot(
            CoreMath.toIntRound(x * cos - y * sin) + newImage.getWidth() / 2,
            CoreMath.toIntRound(x * sin + y * cos) + newImage.getHeight() / 2);
        return newImage;
    }
    
    public CoreImage halfSize() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].halfSize();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot(
            getHotspotX() / 2,
            getHotspotY() / 2);
        return newImage;
    }
    
    public CoreImage scale(int scaledFrameWidth, int scaledFrameHeight) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].scale(scaledFrameWidth, scaledFrameHeight);
        }
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot(
            getHotspotX() * newImage.getWidth() / getWidth(),
            getHotspotY() * newImage.getHeight() / getHeight());
        return newImage;
    }
    
    public CoreImage mirror() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].mirror();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot(getWidth() - 1 - getHotspotX(), getHotspotY());
        return newImage;
    }
    
    public CoreImage flip() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].flip();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot(getHotspotX(), getHeight() - 1 - getHotspotY());
        return newImage;
    }
    
    public CoreImage rotateLeft() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].rotateLeft();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot(getHotspotY(), (getWidth() - 1) - getHotspotX());
        return newImage;
    }
    
    public CoreImage rotateRight() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].rotateRight();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot((getHeight() - 1) - getHotspotX(), getHotspotY());
        return newImage;
    }
    
    public CoreImage rotate180() {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].rotate180();
        }
        
        AnimatedImage newImage = new AnimatedImage(this, newFrames);
        newImage.setHotspot((getWidth() - 1) - getHotspotX(), (getHeight() - 1) - getHotspotY());
        return newImage;
    }
    
    //
    // ARGB filters 
    //
    
    public CoreImage setTransparentColor(int rgbColor) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].setTransparentColor(rgbColor);
        }
        return new AnimatedImage(this, newFrames);
    }
    
    public CoreImage tint(int rgbColor) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].tint(rgbColor);
        }
        return new AnimatedImage(this, newFrames);   
    }
    
    public CoreImage background(int argbColor) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].background(argbColor);
        }
        return new AnimatedImage(this, newFrames);
    }
    
    public CoreImage fade(int alpha) {
        CoreImage[] newFrames = new CoreImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            newFrames[i] = frames[i].fade(alpha);
        }
        return new AnimatedImage(this, newFrames);
    }
}
