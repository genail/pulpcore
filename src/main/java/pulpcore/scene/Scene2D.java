/*
    Copyright (c) 2009, Interactive Pulp, LLC
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

package pulpcore.scene;

import java.util.ArrayList;
import pulpcore.animation.event.TimelineEvent;
import pulpcore.animation.Timeline;
import pulpcore.Build;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.sprite.Group;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

/**
    The Scene2D class is a Scene that provdes commonly used features like
    Sprite management, layer management, Timeline management,
    and dirty rectangle drawing. 
    <p>
    Note the updateScene() method cannot be overridden,
    and subclasses should override the {@link #update(int) } method instead.
    <p>
    Scene2D and {@link pulpcore.sprite.Group} are thread-safe, but in general, Sprites and 
    Properties are not thread-safe. For multi-threaded apps (for example, 
    network-enabled apps), use the {@link #addEvent(TimelineEvent)}, 
    {@link #addEventAndWait(TimelineEvent)}, {@link #invokeLater(Runnable)}, or
    {@link #invokeAndWait(Runnable)} methods to make sure Sprites and Properties are 
    modified on the animation thread. For example:
    <pre>
    // Called from the network thread.
    public void receiveNetworkMessage(String message) {
        // It is safe to add and remove sprites. 
        scene.add(myLabel);
        
        // Modify Sprites and properties in the animation thread.
        scene.invokeLater(new Runnable() {
            public void run() {
                myLabel.setText(message);
                myLabel.visible.set(true);
            }
        });
    }
    </pre>
*/
public class Scene2D extends Scene {
    
    // NOTE: experiment with these two values for screen sizes other than 550x400
    /** If the non-dirty area inbetween two dirty rects is less than this value, the two
    rects are union'ed. */
    private static final int MAX_NON_DIRTY_AREA = 2048;
    private static final int NUM_DIRTY_RECTANGLES = 64;
    //private static final int MAX_PERCENT_DIRTY_RECTANGLE_COVERAGE = 80;
    
    private static final int DEFAULT_MAX_ELAPSED_TIME = 100;
    
    // For debugging - same setting for all Scene2D instances
    
    private static boolean showDirtyRectangles;

    // Scene options
    
    private boolean dirtyRectanglesEnabled;
    private boolean paused;
    private int maxElapsedTime;
    private boolean isUnloading;
    
    // Layers
    
    private final Group root = new Group() {
        public Scene2D getScene2D() {
            return Scene2D.this;
        }
    };
    
    // Timelines
    
    private ArrayList timelines;
    
    // Dirty Rectangles
    
    private Rect drawBounds = new Rect();
    private boolean needsFullRedraw;
    private RectList dirtyRectangles;
    private RectList subRects;
    
    private Rect newRect = new Rect();
    private Rect workRect = new Rect();
    private Rect unionRect = new Rect();
    private Rect intersectionRect = new Rect();
    
    private int dirtyRectPadX = 1;
    private int dirtyRectPadY = 1;
    
    // Saved state: options automatically restored on showNotify()
    
    private boolean stateSaved;
    private int desiredFPS;
    
    // Called via reflection via PulpCore Player
    private static void toggleShowDirtyRectangles() {
        showDirtyRectangles = !showDirtyRectangles;
        Scene scene = Stage.getScene();
        if (scene instanceof Scene2D) {
            ((Scene2D)scene).needsFullRedraw = true;
        }
    }
    
    /**
        Creates a new Scene2D with one layer and with dirty rectangles enabled.
    */
    public Scene2D() {
        dirtyRectangles = new RectList(NUM_DIRTY_RECTANGLES);
        subRects = new RectList(NUM_DIRTY_RECTANGLES);
        
        // Initial settings 
        dirtyRectanglesEnabled = true;
        maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;
        desiredFPS = Stage.DEFAULT_FPS;
        stateSaved = false;
        paused = false;
        isUnloading = false;
        
        reset();
    }
    
    private void reset() {
        needsFullRedraw = true;
        timelines = new ArrayList();
        root.removeAll();
        addLayer(new Group()); 
    }
    
    /**
        Sets the paused state of this Scene2D. A paused Scene2D does not update sprites or 
        timelines, but update() is called as usual. By default, a Scene2D is not paused.
    */
    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    /**
        Gets the paused state of this Scene2D.
        @return true if this Scene2D is paused.
        @see #setPaused(boolean)
    */
    public synchronized boolean isPaused() {
        return paused;
    }
    
    /**
        Sets the default cursor for this Scene. By default, a Scene2D uses the default cursor.
        @see pulpcore.Input
        @see #getCursor()
    */
    public final synchronized void setCursor(int cursor) {
        root.setCursor(cursor);
    }
    
    /**
        Gets the cursor for this Scene. 
        @see pulpcore.Input
        @see #setCursor(int)
    */
    public final synchronized int getCursor() {
        int cursor = root.getCursor();
        if (cursor == -1) {
            return Input.CURSOR_DEFAULT;
        }
        else {
            return cursor;
        }
    }
    
    /**
        Sets the dirty rectangle mode on or off. By default, a Scene2D has dirty rectangles
        enabled, but some apps may have better performance with
        dirty rectangles disabled.
    */
    public final synchronized void setDirtyRectanglesEnabled(boolean dirtyRectanglesEnabled) {
        if (this.dirtyRectanglesEnabled != dirtyRectanglesEnabled) {
            this.dirtyRectanglesEnabled = dirtyRectanglesEnabled;
            needsFullRedraw = true;
            if (!this.dirtyRectanglesEnabled) {
                clearDirtyRects(root);
            }
        }
    }
    
    /**
        Checks the dirty rectangles are enabled for this Scene2D.
        @return true if dirty rectangles are enabled.
        @see #setDirtyRectanglesEnabled(boolean)
    */
    public final synchronized boolean isDirtyRectanglesEnabled() {
        return dirtyRectanglesEnabled;
    }
    
    /**
        Sets the maximum elapsed time used to update this Scene2D.
        If this value is zero, no maximum elapsed time is enforced: 
        the elapsed time always follows system time (while the Scene2D is active.)
        <p>
        If the maximum elapsed time is greater than zero, 
        long pauses between updates
        (caused by other processes or the garbage collector) effectively 
        slow down the animations rather than create a visible 
        skip in time.
        <p>
        By default, the maximum elapsed time is 100. 
    */
    public synchronized void setMaxElapsedTime(int maxElapsedTime) {
        this.maxElapsedTime = maxElapsedTime;
    }
    
    
    /**
        Gets the maximum elapsed time used to update this Scene2D.
        @see #setMaxElapsedTime(int)
    */
    public synchronized int getMaxElapsedTime() {
        return maxElapsedTime;
    }
    
    
    //
    // Timelines
    //
    
    
    /**
        Adds a Timeline to this Scene2D. The Timeline is automatically updated in the updateScene()
        method. The Timeline is removed when is is finished animating.
        <p>
        This method is safe to call from any thread.
    */
    public synchronized void addTimeline(Timeline timeline) {
        if (!timelines.contains(timeline)) {
            timelines.add(timeline);
        }
    }
    
    
    /**
        Removes a timeline from this Scene2D.
        @param gracefully if true and the timeline is not looping, the timeline is 
        fast-forwarded to its end before it is removed.
    */
    public synchronized void removeTimeline(Timeline timeline, boolean gracefully) {
        if (timeline == null) {
            return;
        }
        if (gracefully) {
            timeline.fastForward();
        }
        timelines.remove(timeline);
    }
    
    
    /**
        Removes all timelines from this Scene2D.
        @param gracefully if true, all non-looping timelines are 
        fastforwarded to their end before they are removed.
    */
    public synchronized void removeAllTimelines(boolean gracefully) {
        if (gracefully) {
            for (int i = 0; i < timelines.size(); i++) {
                Timeline t = (Timeline)timelines.get(i);
                t.fastForward();
            }
        }
        timelines.clear();
    }
    
    
    /**
        Gets the number of currently animating timelines. 
    */
    public synchronized int getNumTimelines() {
        return timelines.size();
    }

    //
    // Events
    //
    
    /**
        Adds a TimelineEvent to this Scene2D. The TimelineEvent is automatically triggered
        in the updateScene() method. The TimelineEvent is removed after triggering.
        <p>
        This method is safe to call from any thread.
    */
    public synchronized void addEvent(TimelineEvent event) {
        Timeline timeline = new Timeline();
        timeline.add(event);
        addTimeline(timeline);
    }
    
    /**
        Adds a TimelineEvent to this Scene2D and returns after the TimelineEvent executes or
        when this Scene2D is unloaded (whichever comes first).
        @throws Error if the current thread is the animation thread.
    */
    public void addEventAndWait(TimelineEvent event) {
        if (Stage.isAnimationThread()) {
            throw new Error("Cannot call addEventAndWait() or invokeAndWait() from the " + 
                "animation thread.");
        }
        
        synchronized (event) {
            addEvent(event);
            while (!event.hasExecuted() && !isUnloading) {
                try {
                    event.wait();
                }
                catch (InterruptedException ex) { }
            }
        }
    }
    
    /**
        Causes a runnable to have it's run() method called in the animation thread. This method
        is equivalent to:
        <pre>
        addEvent(new TimelineEvent(0) {
            public void run() {
                runnable.run();
            }
        });
        </pre>
        <p>
        This method is safe to call from any thread.
    */
    public synchronized void invokeLater(final Runnable runnable) {
        addEvent(new TimelineEvent(0) {
            public void run() {
                runnable.run();
            }
        });
    }
    
    /**
        Causes a runnable to have it's run() method called in the animation thread, and returns
        after the Runnable executes or
        when this Scene2D is unloaded (whichever comes first). This method is equivalent to:
        <pre>
        addEventAndWait(new TimelineEvent(0) {
            public void run() {
                runnable.run();
            }
        });
        </pre>
        @throws Error if the current thread is the animation thread.
    */
    public void invokeAndWait(final Runnable runnable) {
        addEventAndWait(new TimelineEvent(0) {
            public void run() {
                runnable.run();
            }
        });
    }
    
    //
    // Layers
    //
    
    /**
        Returns the main (bottom) layer. This layer cannot be removed.
    */
    public synchronized Group getMainLayer() {
        return (Group)root.get(0);
    }
    
    /**
        Adds the specified Group as the top-most layer.
    */
    public synchronized void addLayer(Group layer) {
        root.add(layer);
    }
    
    /**
        Removes the specified layer. If the specified layer is the main layer, this method
        does nothing.
    */
    public synchronized void removeLayer(Group layer) {
        if (layer != getMainLayer()) {
            root.remove(layer);
        }
    }
    
    /**
        Returns the total number of sprites in all layers.
    */
    public synchronized int getNumSprites() {
        return root.getNumSprites();
    }

    /**
        Returns the total number of visible sprites in all layers.
    */
    public synchronized int getNumVisibleSprites() {
        return root.getNumVisibleSprites();
    }
        
    //
    // Sprites
    //
    
    /**
        Adds a sprite to the main (bottom) layer.
    */
    public synchronized void add(Sprite sprite) {
        getMainLayer().add(sprite);
    }
    
    /**
        Removes a sprite from the main (bottom) layer.
    */
    public synchronized void remove(Sprite sprite) {
        getMainLayer().remove(sprite);
    }
    
    //
    // Dirty Rectangles
    //
    
    private void addDirtyRectangle(Rect parentClip, Rect r) {
        if (r == null) {
            return;
        }
        
        subRects.clear();
        
        // Increase bounds to correct off-by-one miscalculation in some rare rotated sprites.
        addDirtyRectangle(parentClip, r.x - dirtyRectPadX, r.y - dirtyRectPadY, 
            r.width + dirtyRectPadX*2, r.height + dirtyRectPadY*2, MAX_NON_DIRTY_AREA);
        
        int originalSize = subRects.size();
        for (int i = 0; i < subRects.size() && !dirtyRectangles.isOverflowed(); i++) {
            Rect r2 = subRects.get(i);
            if (i < originalSize) {
                addDirtyRectangle(parentClip, r2.x, r2.y, r2.width, r2.height, MAX_NON_DIRTY_AREA);
            }
            else {
                addDirtyRectangle(parentClip, r2.x, r2.y, r2.width, r2.height, 0);
            }
            if (subRects.isOverflowed()) {
                // Ah, crap.
                dirtyRectangles.overflow();
            }
        }
        
        // If covering too much area, don't use dirty rectangles
        // *** Disabled because I didn't see any improvement in the BubbleMark example
        //if (!dirtyRectangles.isOverflowed()) {
        //    int maxArea = drawBounds.getArea() * MAX_PERCENT_DIRTY_RECTANGLE_COVERAGE / 100;
        //    if (dirtyRectangles.getArea() >= maxArea) {
        //        dirtyRectangles.overflow();
        //    }
        //}
    }
    
    private void addDirtyRectangle(Rect parentClip, int x, int y, int w, int h, 
        int maxNonDirtyArea) 
    {
        if (w <= 0 || h <= 0 || dirtyRectangles.isOverflowed()) {
            return;
        }
        
        newRect.setBounds(x, y, w, h);
        newRect.intersection(drawBounds);
        if (parentClip != null) {
            newRect.intersection(parentClip);
        }
        if (newRect.width <= 0 || newRect.height <= 0) {
            return;
        }
        
        // The goal here is to have no overlapping dirty rectangles because
        // it would lead to problems with alpha blending.
        //
        // Performing a union on two overlapping rectangles would lead to
        // dirty rectangles that cover large portions of the scene that are
        // not dirty.
        // 
        // Instead: shrink, split, or remove existing dirty rectangles, or
        // shrink or remove the new dirty rectangle.
        for (int i = 0; i < dirtyRectangles.size(); i++) {
            
            Rect dirtyRect = dirtyRectangles.get(i);
            
            unionRect.setBounds(dirtyRect);
            unionRect.union(newRect);
            if (unionRect.equals(dirtyRect)) {
                return;
            }
            intersectionRect.setBounds(dirtyRect);
            intersectionRect.intersection(newRect);
            
            int newArea = unionRect.getArea() + intersectionRect.getArea() - 
                dirtyRect.getArea() - newRect.getArea();
            if (newArea < maxNonDirtyArea) {
                newRect.setBounds(unionRect);
                dirtyRectangles.remove(i);
                if (newArea > 0) {
                    // Start over - make sure there's no overlap
                    i = -1;
                }
                else {
                    i--;
                }
            }
            else if (dirtyRect.intersects(newRect)) {
                int code = dirtyRect.getIntersectionCode(newRect);
                int numSegments = CoreMath.countBits(code);
                if (numSegments == 0) {
                    // Remove the existing dirty rect in favor of the new one
                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 1) {
                    // Shrink the existing dirty rect
                    dirtyRect.setOutsideBoundary(Rect.getOppositeSide(code), 
                        newRect.getBoundary(code));
                    subRects.add(dirtyRect);
                    
                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 2) {
                    // Split the existing dirty rect into two
                    int side1 = 1 << CoreMath.log2(code);
                    int side2 = code - side1;
                    workRect.setBounds(dirtyRect);
                    
                    // First split
                    dirtyRect.setOutsideBoundary(Rect.getOppositeSide(side1), 
                        newRect.getBoundary(side1));
                    subRects.add(dirtyRect);
                    
                    // Second split
                    workRect.setOutsideBoundary(side1, 
                        dirtyRect.getBoundary(Rect.getOppositeSide(side1)));
                    workRect.setOutsideBoundary(Rect.getOppositeSide(side2), 
                        newRect.getBoundary(side2));
                    subRects.add(workRect);
                    
                    dirtyRectangles.remove(i);
                    i--;
                }
                else if (numSegments == 3) {
                    // Shrink the new dirty rect
                    int side = code ^ 0xf;
                    newRect.setOutsideBoundary(Rect.getOppositeSide(side), 
                        dirtyRect.getBoundary(side));
                    if (newRect.width <= 0 || newRect.height <= 0) {
                        return;
                    }
                }
                else if (numSegments == 4) {
                    // Exit - don't add this new rect
                    return;
                }
            }
        }
        
        dirtyRectangles.add(newRect);
    }
    
    //
    // Scene implementation
    //
    
    /**
        Forces all invokeAndWait() and addEventAndWait() calls to return, and 
        removes all layers, sprites, and timelines. 
    */
    public synchronized void unload() {
        isUnloading = true;
        
        for (int i = 0; i < timelines.size(); i++) {
            Timeline timeline = (Timeline)timelines.get(i);
            timeline.notifyChildren();
        }
        
        isUnloading = false;
        reset();
    }
    
    /**
        Notifies that this scene has been shown after another Scene is hidden
        or immediately after a call to start(). 
        <p>
        Subclasses that override this method should call {@code super.showNotify();}.
    */
    public void showNotify() {
        if (stateSaved) {
            Stage.setFrameRate(desiredFPS);
            stateSaved = false;
        }
        redrawNotify();
    }
    
    /**
        Notifies that this scene has been hidden by another Scene or 
        immediately before a call to stop(). 
        <p>
        Subclasses that override this method should call {@code super.hideNotify();}.
    */
    public void hideNotify() {
        desiredFPS = Stage.getFrameRate();
        stateSaved = true;
    }
    
    public final void redrawNotify() {
        Transform t = Stage.getDefaultTransform();
        
        if (t.getType() == Transform.TYPE_IDENTITY) {
            drawBounds.setBounds(0, 0, Stage.getWidth(), Stage.getHeight());
            dirtyRectPadX = 1;
            dirtyRectPadY = 1;
        }
        else {
            drawBounds.setBounds(
                CoreMath.toInt(t.getTranslateX()),
                CoreMath.toInt(t.getTranslateY()),
                CoreMath.toInt(Stage.getWidth() * t.getScaleX()),
                CoreMath.toInt(Stage.getHeight() * t.getScaleY()));
            // Based off of BackBufferTest scaled to full screen
            dirtyRectPadX = 1 + CoreMath.toIntCeil(t.getScaleX());
            dirtyRectPadY = 1 + CoreMath.toIntCeil(t.getScaleY());
        }
        needsFullRedraw = true;
    }
    
    public final void updateScene(int elapsedTime) {
        
        if (maxElapsedTime > 0 && elapsedTime > maxElapsedTime) {
            elapsedTime = maxElapsedTime;
        }
        
        if (Build.DEBUG && Input.isControlDown() && 
            Input.isPressed(Input.KEY_D))
        {
            showDirtyRectangles = !showDirtyRectangles;
            needsFullRedraw = true;
        }
        
        // Update timelines, layers, and sprites
        if (!paused) {
            root.update(elapsedTime);
            // Update timelines
            synchronized (this) {
                for (int i = 0; i < timelines.size(); i++) {
                    Timeline timeline = (Timeline)timelines.get(i);
                    timeline.update(elapsedTime);
                }
                // Remove finished timelines (seperate in case any timeline updates actually
                // modify the list of timelines)
                for (int i = 0; i < timelines.size(); i++) {
                    Timeline timeline = (Timeline)timelines.get(i);
                    if (timeline.isFinished()) {
                        timelines.remove(i);
                        i--;
                    }
                }
            }
        }
        // Allow subclasses to check input, change scenes, etc.
        update(elapsedTime);
        
        // Set cursor
        int cursor = Input.CURSOR_DEFAULT;
        if (Input.isMouseInside()) {
            Sprite pick = root.pickEnabledAndVisible(Input.getMouseX(), Input.getMouseY());
            if (pick != null) {
                cursor = pick.getCursor();
            }
        }
        Input.setCursor(cursor);
        
        if (!dirtyRectanglesEnabled || needsFullRedraw) {
            dirtyRectangles.overflow();
        }
        else {
            dirtyRectangles.clear();
        }
        
        if (needsFullRedraw) {
            root.setDirty(true);
            if (Build.DEBUG) {
                Sprite overlay = Stage.getInfoOverlay();
                if (overlay != null) {
                    overlay.setDirty(true);
                }
            }
        }
        
        if (dirtyRectanglesEnabled) {
            // Add dirty rectangles
            addDirtyRectangles(root, null, null, needsFullRedraw);
            root.setDirty(false);
            
            // Add dirty rectangle for the custom cursor
            // TODO: custom cursor may be broken for non-identity parent transforms
            /*
            CoreImage cursor = Input.getCustomCursor();
            if (cursor != null) {
                addDirtyRectangle(
                    Input.getMouseX() - cursor.getHotspotX(),
                    Input.getMouseY() - cursor.getHotspotY(),
                    cursor.getWidth(), cursor.getHeight());
            }
            */
                
            // Add dirty rectangle for debug overlay
            if (Build.DEBUG) {
                Sprite overlay = Stage.getInfoOverlay();
                if (overlay != null) {
                    if (needsFullRedraw || overlay.isDirty()) {
                        if (dirtyRectangles.isOverflowed()) {
                            overlay.updateDirtyRect();
                        }
                        else {
                            addDirtyRectangle(null, overlay.getDirtyRect());
                            boolean boundsChanged = overlay.updateDirtyRect();
                            if (boundsChanged) {
                                addDirtyRectangle(null, overlay.getDirtyRect());
                            }
                        }
                    }
                    overlay.setDirty(false);
                }
            }
        }
        else {
            updateTransforms(root);
        }
    }
    
    private void setDirty(Group group, boolean dirty) {
        group.setDirty(dirty);
        for (int i = 0; i < group.size(); i++) {
            Sprite sprite = group.get(i);
            if (sprite instanceof Group) {
                setDirty((Group)sprite, dirty);
            }
            else {
                sprite.setDirty(dirty);
            }
        }
    }
    
    private void clearDirtyRects(Group group) {
        group.clearDirtyRect();
        for (int i = 0; i < group.size(); i++) {
            Sprite sprite = group.get(i);
            if (sprite instanceof Group) {
                clearDirtyRects((Group)sprite);
            }
            else {
                sprite.clearDirtyRect();
            }
        }
    }
    
    private void updateTransforms(Group group) {
        // Hack: use getViewX() to force update of transform
        group.getViewX();
        for (int i = 0; i < group.size(); i++) {
            Sprite sprite = group.get(i);
            if (sprite instanceof Group) {
                updateTransforms((Group)sprite);
            }
            else {
                sprite.getViewX();
            }
        }
    }
    
    /**
        Recursive function to loop through all the child sprites of the 
        specified group.
    */
    private void addDirtyRectangles(Group group, Rect oldParentClip, Rect parentClip, 
        boolean parentDirty)
    {
        parentDirty |= group.isDirty();
        
        // Update the Group dirty rect.
        // Groups only have a dirty rect if isOverflowClipped() is true
        Rect oldClip = group.getDirtyRect();
        if (oldClip != null) {
            if (oldParentClip == null) {
                oldParentClip = new Rect(oldClip);
            }
            else {
                oldParentClip = new Rect(oldParentClip);
                oldParentClip.intersection(oldClip);
            }
        }
        boolean parentBoundsChanged = group.updateDirtyRect();
        Rect newClip = group.getDirtyRect();
        if (newClip != null) {
            if (parentClip == null) {
                parentClip = new Rect(newClip);
            }
            else {
                parentClip = new Rect(parentClip);
                parentClip.intersection(newClip);
            }
            // Special case: Group has a dirty filter
            if (group.hasBackBuffer() && group.getFilter() != null && group.isDirty()) {
                addDirtyRectangle(null, parentClip);
            }
        }

        if (!parentBoundsChanged) {
            if (oldParentClip != parentClip) {
                if (oldParentClip == null || parentClip == null) {
                    parentBoundsChanged = true;
                }
                else {
                    parentBoundsChanged = !oldParentClip.equals(parentClip);
                }
            }
        }
        
        // Add dirty rects for removed sprites
        ArrayList removedSprites = group.getRemovedSprites();
        if (removedSprites != null) {
            for (int i = 0; i < removedSprites.size(); i++) {
                if (removedSprites.get(i) == group) {
                    // Special case: Group had a filter that was removed
                    addDirtyRectangle(null, oldParentClip);
                }
                else {
                    notifyRemovedSprite(oldParentClip, (Sprite)removedSprites.get(i));
                }
            }
        }
        
        // Add dirty rects for the sprites
        for (int i = 0; i < group.size(); i++) {
            Sprite sprite = group.get(i);
            if (sprite instanceof Group) {
                addDirtyRectangles((Group)sprite, oldParentClip, parentClip, parentDirty);
            }
            else if (parentDirty || sprite.isDirty()) {
                if (dirtyRectangles.isOverflowed()) {
                    sprite.updateDirtyRect();
                }
                else {
                    addDirtyRectangle(oldParentClip, sprite.getDirtyRect());
                    boolean boundsChanged = sprite.updateDirtyRect();
                    if (parentBoundsChanged || boundsChanged) {
                        addDirtyRectangle(parentClip, sprite.getDirtyRect());
                    }
                }
            }
            
            sprite.setDirty(false);
        }
    }
        
    private final void notifyRemovedSprite(Rect parentClip, Sprite sprite) {
        if (dirtyRectangles.isOverflowed()) {
            return;
        }
            
        if (sprite instanceof Group) {
            Group group = (Group)sprite;
            for (int i = 0; i < group.size(); i++) {
                notifyRemovedSprite(parentClip, group.get(i));
            }
        }
        else if (sprite != null) {
            addDirtyRectangle(parentClip, sprite.getDirtyRect());
        }
    }
    
    /**
        Allows subclasses to check for input, change scenes, etc. By default, this method does
        nothing.
    */
    public void update(int elapsedTime) {
        // Do nothing
    }
    
    /**
        Draws all of the sprites in this scene. Most apps will not override this method.
    */
    public void drawScene(CoreGraphics g) {
        
        boolean drawOverlay = (Build.DEBUG && Stage.getInfoOverlay() != null);
        
        if (!dirtyRectanglesEnabled || needsFullRedraw || dirtyRectangles.isOverflowed()) {
            g.setClip(drawBounds);
            root.draw(g);
            if (Build.DEBUG && drawOverlay) {
                Stage.getInfoOverlay().draw(g);
            }
            needsFullRedraw = false;
        }
        else if (Build.DEBUG && showDirtyRectangles) {
            g.setClip(drawBounds);
            root.draw(g);
            if (Build.DEBUG && drawOverlay) {
                Stage.getInfoOverlay().draw(g);
            }
            
            for (int i = 0; i < dirtyRectangles.size(); i++) {
                Rect r = dirtyRectangles.get(i);
                g.setColor(Colors.GREEN);
                g.drawRect(r.x, r.y, r.width, r.height);
                g.setColor(Colors.rgba(Colors.GREEN, 128));
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        }
        else {
            // This might be a place to optimize. Currently every sprite is drawn for every
            // rectangle, and the clipping bounds makes sure we don't overdraw. 
            for (int i = 0; i < dirtyRectangles.size(); i++) {
                Rect r = dirtyRectangles.get(i);
                g.setClip(r.x, r.y, r.width, r.height);
                root.draw(g);
                if (Build.DEBUG && drawOverlay) {
                    Stage.getInfoOverlay().draw(g);
                }
            }
            Stage.setDirtyRectangles(dirtyRectangles.rects, dirtyRectangles.size);
        }
        
        dirtyRectangles.clear();
    }
    
    static class RectList {
        
        private Rect[] rects;
        private int size;
        
        public RectList(int capacity) {
            rects = new Rect[capacity];
            for (int i = 0; i < capacity; i++) {
                rects[i] = new Rect();
            }
            clear();
        }
        
        public int getArea() {
            int area = 0;
            for (int i = 0; i < size; i++) {
                area += rects[i].getArea();
            }
            return area;
        }
        
        public int size() {
            return size;
        }
        
        public void clear() {
            size = 0;
        }
        
        public boolean isOverflowed() {
            return (size < 0);
        }
        
        public void overflow() {
            size = -1;
        }
        
        public Rect get(int i) {
            return rects[i];
        }
        
        public void remove(int i) {
            if (size > 0) {
                if (i < size - 1) {
                    rects[i].setBounds(rects[size - 1]);
                }
                size--;
            }
        }
        
        public boolean add(Rect r) {
            if (size < 0 || size == rects.length) {
                size = -1;
                return false;
            }
            else {
                rects[size++].setBounds(r);
                return true;
            }
        }
    }
}