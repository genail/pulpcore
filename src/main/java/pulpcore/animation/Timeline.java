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

import java.util.ArrayList;
import pulpcore.animation.event.SceneChangeEvent;
import pulpcore.animation.event.SoundEvent;
import pulpcore.animation.event.TimelineEvent;
import pulpcore.Build;
import pulpcore.math.CoreMath;
import pulpcore.scene.Scene;
import pulpcore.sound.Sound;
import pulpcore.sprite.Sprite;

/**
    A Timeline is a list of Animations.
*/
public final class Timeline extends Animation {
    
    private Timeline parent;
    
    // Parallel arrays - behaviors have a property
    private ArrayList animationList; // Animations. Must implement Behavior if property is non-null.
    private ArrayList propertyList; // Property if Behavior; false otherwise
    
    private boolean playing;
    private double playSpeed = 1;
    
    // Remainder, in microseconds, of the play time. Used when playSpeed != 1.
    private int remainderMicros;
    
    private int lastAnimTime = 0;
    
    public Timeline() {
        this(null, 0);
    }
    
    public Timeline(int startDelay) {
        this(null, startDelay);
    }
    
    public Timeline(Easing easing) {
        this(easing, 0);
    }
    
    public Timeline(Easing easing, int startDelay) {
        super(0, easing, startDelay);
        animationList = new ArrayList();
        propertyList = new ArrayList();
        playing = true;
    }
    
    private void setParent(Timeline parent) {
        this.parent = parent;
    }
    
    private void calcDuration() {
        // TODO: sort children by their getTotalDuration() ?
        int duration = 0;
        for (int i = 0; i < animationList.size(); i++) {
            Animation anim = (Animation)animationList.get(i);
            int childDuration = anim.getTotalDuration();
            if (childDuration == LOOP_FOREVER) {
                duration = LOOP_FOREVER;
                break;
            }
            else if (childDuration > duration) {
                duration = childDuration;
            }
        }
        super.setDuration(duration);
        if (parent != null) {
            parent.calcDuration();
        }
    }
    
    //
    // Movie controls
    //
    
    /**
        Sets the play speed. A speed of '1' is normal, '.5' is half speed,
        '2' is twice normal speed, and '-1' is reverse speed.
        Note: play speed only affects
        top-level Timelines - child Timelines play at their parent's 
        speed.
    */
    public void setPlaySpeed(double speed) {
        playSpeed = speed;
    }
    
    public double getPlaySpeed() {
        return playSpeed;
    }
    
    public void pause() {
        playing = false;
    }
    
    public void play() {
        playing = true;
    }
    
    public void stop() {
        playing = false;
        rewind();
    }
    
    public boolean isPlaying() {
        return playing;
    }
    
    //@Override
    public boolean update(int elapsedTime) {
        if (!playing || playSpeed == 0) {
            elapsedTime = 0;
        }
        else if (playSpeed == -1) {
            elapsedTime = -elapsedTime;
        }
        else if (playSpeed != 1) {
            long timeMicros = Math.round(elapsedTime * 1000L * playSpeed) + remainderMicros;
            elapsedTime = (int)(timeMicros / 1000);
            remainderMicros = (int)(timeMicros % 1000);
        }
        return super.update(elapsedTime);
    }
        
    protected void updateState(int animTime) {
        // First, update those animations that were previously in SECTION_ANIMATION
        for (int i = 0; i < animationList.size(); i++) {
            Animation anim = (Animation)animationList.get(i);
            if (anim.getSection(lastAnimTime) == SECTION_ANIMATION) {
                boolean active = anim.update(animTime - anim.getTime());
                if (active && anim instanceof Behavior) {
                    ((Property)propertyList.get(i)).setValue(((Behavior)anim).getValue());
                }
            }
        }
        
        // Next, update all other animations
        for (int i = 0; i < animationList.size(); i++) {
            Animation anim = (Animation)animationList.get(i);
            if (anim.getSection(lastAnimTime) != SECTION_ANIMATION) {
                boolean active = anim.update(animTime - anim.getTime());
                if (active && anim instanceof Behavior) {
                    ((Property)propertyList.get(i)).setValue(((Behavior)anim).getValue());
                }
            }
        }
        
        lastAnimTime = animTime;        
    }
    
    //
    // Children
    //
    
    /**
        Creates a child timeline that starts at the specified time relative to the start of this
        timeline.
        <p>
        This method provides an alternative syntax for delayed animations:
        <pre>
        timeline.at(500).animate(sprite.alpha, 0, 255, 500);
        timeline.at(1000).set(sprite.enabled, true);
        </pre>
        @param time Time in milliseconds.
        @return the child timeline.
    */
    public Timeline at(int time) {
        Timeline child = new Timeline(Easing.NONE, time);
        add(child);
        return child;
    }
    
    /**
        Adds an event to the timeline.
    */
    public void addEvent(TimelineEvent event) {
        add(event);
    }
    
    /**
        @deprecated Replaced by {@link #add(Property, Animation) } 
    */
    public void animate(Property property, Animation animation) {
        add(property, animation);
    }
    
    public void add(Animation animation) {
        if (Build.DEBUG) {
            if (animation instanceof Behavior) {
                throw new IllegalArgumentException("Behavior must have a property attached.");
            }
        }
        if (animation instanceof Timeline) {
            ((Timeline)animation).setParent(this);
        }
        animationList.add(animation);
        propertyList.add(null);
        calcDuration();
    }
    
    public void add(Property property, Animation animation) {
        if (Build.DEBUG) {
            if (property == null || animation == null) {
                throw new IllegalArgumentException("Both property and behavior must be non-null.");
            }
            if (!(animation instanceof Behavior)) {
                 throw new IllegalArgumentException("Animation must implement Behavior");
            }
        }
        animationList.add(animation);
        propertyList.add(property);
        calcDuration();
    }
    
    /**
        Calls notifyAll() on all child TimelineEvents, waking any threads that are waiting for
        them to execute.
    */
    public void notifyChildren() {
        for (int i = 0; i < animationList.size(); i++) {
            Object anim = animationList.get(i);
            if (anim instanceof Timeline) {
                ((Timeline)anim).notifyChildren();
            }
            else if (anim instanceof TimelineEvent) {
                synchronized (anim) {
                    ((TimelineEvent)anim).notifyAll();
                }
            }
        }
    }

    //
    // Event convenience methods
    //
    
    public void setScene(Scene scene, int delay) {
        add(new SceneChangeEvent(scene, delay, false));
    }
    
    public void interruptScene(Scene scene, int delay) {
        add(new SceneChangeEvent(scene, delay, true));
    }
    
    public void playSound(Sound sound, int delay) {
        add(new SoundEvent(sound, delay));
    }
    
    //
    // Set convenience methods (immediate)
    //
    
    public void set(Bool property, boolean value) {
        set(property, value, 0);
    }
    
    public void set(Int property, int value) {
        set(property, value, 0);
    }
    
    public void set(Color property, int value) {
        set(property, value, 0);
    }
    
    public void setAsFixed(Fixed property, int value) {
        set(property, value, 0);
    }
    
    public void set(Fixed property, int value) {
        set(property, value, 0);
    }
    
    public void set(Fixed property, double value) {
        set(property, value, 0);
    }
    
    public void setLocation(Sprite sprite, int x, int y) { 
        set(sprite.x, x, 0);
        set(sprite.y, y, 0);
    }
    
    public void setLocation(Sprite sprite, double x, double y) { 
        set(sprite.x, x, 0);
        set(sprite.y, y, 0);
    }
    
    public void setSize(Sprite sprite, int width, int height) { 
        set(sprite.width, width, 0);
        set(sprite.height, height, 0);
    }
    
    public void setSize(Sprite sprite, double width, double height) { 
        set(sprite.width, width, 0);
        set(sprite.height, height, 0);
    }
    
    //
    // Set convenience methods (delayed)
    //
    
    public void set(Bool property, boolean value, int delay) {
        add(property, new Tween(property.get()?1:0, value?1:0, 0, null, delay));
    }
    
    public void set(Int property, int value, int delay) {
        add(property, new Tween(property.get(), value, 0, null, delay));
    }
    
    public void set(Color property, int value, int delay) {
        add(property, new Tween(property.get(), value, 0, null, delay));
    }
    
    public void setAsFixed(Fixed property, int fValue, int delay) {
        add(property, new Tween(property.getAsFixed(), fValue, 0, null, delay));
    }

    public void set(Fixed property, int value, int delay) {
        add(property, new Tween(property.getAsFixed(), CoreMath.toFixed(value), 0, null, delay));
    }
    
    public void set(Fixed property, double value, int delay) {
        add(property, new Tween(property.getAsFixed(), CoreMath.toFixed(value), 0, null, delay));
    }
    
    public void setLocation(Sprite sprite, int x, int y, int delay) { 
        set(sprite.x, x, delay);
        set(sprite.y, y, delay);
    }
    
    public void setLocation(Sprite sprite, double x, double y, int delay) { 
        set(sprite.x, x, delay);
        set(sprite.y, y, delay);
    }
    
    public void setSize(Sprite sprite, int width, int height, int delay) { 
        set(sprite.width, width, delay);
        set(sprite.height, height, delay);
    }
    
    public void setSize(Sprite sprite, double width, double height, int delay) { 
        set(sprite.width, width, delay);
        set(sprite.height, height, delay);
    }
    
    //
    // Int convenience methods
    // 
    
    public void animate(Int property, int fromValue, int toValue, int duration) {
        add(property, new Tween(fromValue, toValue, duration));
    }
    
    public void animate(Int property, int fromValue, int toValue, int duration, Easing easing) {
        add(property, new Tween(fromValue, toValue, duration, easing));
    }
    
    public void animate(Int property, int fromValue, int toValue, int duration, Easing easing, 
        int startDelay)
    {
        add(property, new Tween(fromValue, toValue, duration, easing, startDelay));
    }
    
    public void animateTo(Int property, int toValue, int duration) {
        add(property, new Tween(property.get(), toValue, duration));
    }
    
    public void animateTo(Int property, int toValue, int duration, Easing easing) {
        add(property, new Tween(property.get(), toValue, duration, easing));
    }
    
    public void animateTo(Int property, int toValue, int duration, Easing easing, int startDelay) {
        add(property, new Tween(property.get(), toValue, duration, easing, startDelay));
    }

    //
    // Color convenience methods
    //
    
    public void animate(Color property, int fromARGB, int toARGB, int duration) {
        add(property, new Color.ColorTween(fromARGB, toARGB, duration));
    }
    
    public void animate(Color property, int fromARGB, int toARGB, int duration, Easing easing) {
        add(property, new Color.ColorTween(fromARGB, toARGB, duration, easing));
    }
    
    public void animate(Color property, int fromARGB, int toARGB, int duration, Easing easing, 
        int startDelay)
    {
        add(property, new Color.ColorTween(fromARGB, toARGB, duration, easing, startDelay));
    }
    
    public void animateTo(Color property, int toARGB, int duration) {
        add(property, new Color.ColorTween(property.get(), toARGB, duration));
    }
    
    public void animateTo(Color property, int toARGB, int duration, Easing easing) {
        add(property, new Color.ColorTween(property.get(), toARGB, duration, easing));
    }
    
    public void animateTo(Color property, int toARGB, int duration, Easing easing, int startDelay) {
        add(property, new Color.ColorTween(property.get(), toARGB, duration, easing, startDelay));
    }
    
    //
    // Fixed convenience methods
    //
    
    public void animateAsFixed(Fixed property, int fFromValue, int fToValue, int duration) {
        add(property, new Tween(fFromValue, fToValue, duration));
    }
    
    public void animateAsFixed(Fixed property, int fFromValue, int fToValue, int duration,
        Easing easing)
    {
        add(property, new Tween(fFromValue, fToValue, duration, easing));
    }
    
    public void animateAsFixed(Fixed property, int fFromValue, int fToValue, int duration,
        Easing easing, int startDelay)
    {
        add(property, new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    public void animateToFixed(Fixed property, int fToValue, int duration) {
        add(property, new Tween(property.getAsFixed(), fToValue, duration));
    }
    
    
    public void animateToFixed(Fixed property, int fToValue, int duration, Easing easing) {
        add(property, new Tween(property.getAsFixed(), fToValue, duration, easing));
    }
    
    public void animateToFixed(Fixed property, int fToValue, int duration, Easing easing, 
        int startDelay)
    {
        add(property, new Tween(property.getAsFixed(), fToValue, duration, easing, 
            startDelay));
    }    

    //
    // Fixed as int convenience methods
    //
    
    public void animate(Fixed property, int fromValue, int toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration));
    }
    
    public void animate(Fixed property, int fromValue, int toValue, int duration, Easing easing) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing));
    }
    
    public void animate(Fixed property, int fromValue, int toValue, int duration, Easing easing,
        int startDelay)
    {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }
    
    public void animateTo(Fixed property, int toValue, int duration) {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration));
    }
    
    public void animateTo(Fixed property, int toValue, int duration, Easing easing) {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing));
    }
    
    public void animateTo(Fixed property, int toValue, int duration, Easing easing, 
        int startDelay)
    {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }    
    
    //
    // Fixed as double convenience methods
    //
    
    public void animate(Fixed property, double fromValue, double toValue, int duration) {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration));
    }
    
    public void animate(Fixed property, double fromValue, double toValue, int duration, 
        Easing easing) 
    {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing));
    }
    
    public void animate(Fixed property, double fromValue, double toValue, int duration, 
        Easing easing, int startDelay)
    {
        int fFromValue = CoreMath.toFixed(fromValue);
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }

    public void animateTo(Fixed property, double toValue, int duration) {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration));
    }
    
    public void animateTo(Fixed property, double toValue, int duration, Easing easing) {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing));
    }
    
    public void animateTo(Fixed property,double toValue, int duration, Easing easing, 
        int startDelay)
    {
        int fFromValue = property.getAsFixed();
        int fToValue = CoreMath.toFixed(toValue);
        add(property, new Tween(fFromValue, fToValue, duration, easing, startDelay));
    }    

    //
    // Move as int convenience methods
    //
    
    public void move(Sprite sprite, int x1, int y1, int x2, int y2, int duration) {
        animate(sprite.x, x1, x2, duration);
        animate(sprite.y, y1, y2, duration);
    }
    
    
    public void move(Sprite sprite, int x1, int y1, int x2, int y2, int duration, Easing easing) {
        animate(sprite.x, x1, x2, duration, easing);
        animate(sprite.y, y1, y2, duration, easing);
    }
    
    public void move(Sprite sprite, int x1, int y1, int x2, int y2, int duration, Easing easing,
        int startDelay)
    {
        animate(sprite.x, x1, x2, duration, easing, startDelay);
        animate(sprite.y, y1, y2, duration, easing, startDelay);
    }
    
    public void moveTo(Sprite sprite, int x, int y, int duration) {
        animateTo(sprite.x, x, duration);
        animateTo(sprite.y, y, duration);
    }
    
    public void moveTo(Sprite sprite, int x, int y, int duration, Easing easing) {
        animateTo(sprite.x, x, duration, easing);
        animateTo(sprite.y, y, duration, easing);
    }    
    
    public void moveTo(Sprite sprite, int x, int y, int duration, Easing easing, int startDelay) {
        animateTo(sprite.x, x, duration, easing, startDelay);
        animateTo(sprite.y, y, duration, easing, startDelay);
    }    
    
    //
    // Move as double convenience methods
    //
    
    public void move(Sprite sprite, double x1, double y1, double x2, double y2, int duration) {
        animate(sprite.x, x1, x2, duration);
        animate(sprite.y, y1, y2, duration);
    }
    
    public void move(Sprite sprite, double x1, double y1, double x2, double y2, int duration,
        Easing easing) 
    {
        animate(sprite.x, x1, x2, duration, easing);
        animate(sprite.y, y1, y2, duration, easing);
    }
    
    public void move(Sprite sprite, double x1, double y1, double x2, double y2, int duration, 
        Easing easing, int startDelay)
    {
        animate(sprite.x, x1, x2, duration, easing, startDelay);
        animate(sprite.y, y1, y2, duration, easing, startDelay);
    }
    
    public void moveTo(Sprite sprite, double x, double y, int duration) {
        animateTo(sprite.x, x, duration);
        animateTo(sprite.y, y, duration);
    }
    
    public void moveTo(Sprite sprite, double x, double y, int duration, Easing easing) {
        animateTo(sprite.x, x, duration, easing);
        animateTo(sprite.y, y, duration, easing);
    }    
    
    public void moveTo(Sprite sprite, double x, double y, int duration, Easing easing, 
        int startDelay) 
    {
        animateTo(sprite.x, x, duration, easing, startDelay);
        animateTo(sprite.y, y, duration, easing, startDelay);
    }        
    
    //
    // Scale as int convenience methods
    //
    
    public void scale(Sprite sprite, int width1, int height1, int width2, int height2, 
        int duration) 
    {
        animate(sprite.width, width1, width2, duration);
        animate(sprite.height, height1, height2, duration);
    }
    
    public void scale(Sprite sprite, int width1, int height1, int width2, int height2, 
        int duration, Easing easing) 
    {
        animate(sprite.width, width1, width2, duration, easing);
        animate(sprite.height, height1, height2, duration, easing);
    }
    
    public void scale(Sprite sprite, int width1, int height1, int width2, int height2, 
        int duration, Easing easing, int startDelay)
    {
        animate(sprite.width, width1, width2, duration, easing, startDelay);
        animate(sprite.height, height1, height2, duration, easing, startDelay);
    }
    
    public void scaleTo(Sprite sprite, int width, int height, int duration) {
        animateTo(sprite.width, width, duration);
        animateTo(sprite.height, height, duration);
    }
    
    public void scaleTo(Sprite sprite, int width, int height, int duration, Easing easing) {
        animateTo(sprite.width, width, duration, easing);
        animateTo(sprite.height, height, duration, easing);
    }    
    
    public void scaleTo(Sprite sprite, int width, int height, int duration, Easing easing, 
        int startDelay) 
    {
        animateTo(sprite.width, width, duration, easing, startDelay);
        animateTo(sprite.height, height, duration, easing, startDelay);
    }    
    
    //
    // Scale as double convenience methods
    //
    
    public void scale(Sprite sprite, double width1, double height1, double width2, double height2, 
        int duration) 
    {
        animate(sprite.width, width1, width2, duration);
        animate(sprite.height, height1, height2, duration);
    }
    
    public void scale(Sprite sprite, double width1, double height1, double width2, double height2, 
        int duration, Easing easing) 
    {
        animate(sprite.width, width1, width2, duration, easing);
        animate(sprite.height, height1, height2, duration, easing);
    }
    
    public void scale(Sprite sprite, double width1, double height1, double width2, double height2, 
        int duration, Easing easing, int startDelay)
    {
        animate(sprite.width, width1, width2, duration, easing, startDelay);
        animate(sprite.height, height1, height2, duration, easing, startDelay);
    }
    
    public void scaleTo(Sprite sprite, double width, double height, int duration) {
        animateTo(sprite.width, width, duration);
        animateTo(sprite.height, height, duration);
    }
    
    public void scaleTo(Sprite sprite, double width, double height, int duration, Easing easing) {
        animateTo(sprite.width, width, duration, easing);
        animateTo(sprite.height, height, duration, easing);
    }    
    
    public void scaleTo(Sprite sprite, double width, double height, int duration, Easing easing, 
        int startDelay) 
    {
        animateTo(sprite.width, width, duration, easing, startDelay);
        animateTo(sprite.height, height, duration, easing, startDelay);
    }            
}
