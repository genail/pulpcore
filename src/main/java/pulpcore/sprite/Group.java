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

package pulpcore.sprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import pulpcore.image.BlendMode;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Tuple2i;
import pulpcore.math.Transform;
import pulpcore.Stage;
import pulpcore.animation.Property;
import pulpcore.image.filter.Filter;

/**
    A container of Sprites.
*/
public class Group extends Sprite {
    
    /** Immutable list of sprites. A new array is created when the list changes. */
    private Sprite[] sprites = new Sprite[0];
    /** The list of sprites at the last call to getRemovedSprites() */
    private Sprite[] previousSprites = null;
    private boolean hadFilterLastUpdate = false;
    
    /** Used for children to check if this Group's transform has changed since the last update */
    private int transformModCount = 0;
    
    private int fNaturalWidth;
    private int fNaturalHeight;
    
    private CoreImage backBuffer;
    private boolean backBufferCoversStage;
    private BlendMode backBufferBlendMode = BlendMode.SrcOver();
    private Transform backBufferTransform = new Transform();
    
    public Group() {
        this(0, 0, 0, 0);
    }
    
    public Group(int x, int y) {
        this(x, y, 0, 0);
    }
    
    public Group(int x, int y, int width, int height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    public Group(double x, double y) {
        this(x, y, 0, 0);
    }
    
    public Group(double x, double y, double width, double height) {
        super(x, y, width, height);
        fNaturalWidth = CoreMath.toFixed(width);
        fNaturalHeight = CoreMath.toFixed(height);
    }
    
    /* package-private */ int getTransformModCount() {
        return transformModCount;
    }
    
    /* package-private */ void updateTransformModCount() {
        transformModCount++;
    }

    private Object getTreeLock() {
        Object lock = getScene2D();
        if (lock == null) {
            lock = this;
        }
        return lock;
    }


    /**
        Returns {@code true} if sprites inside this Group are not visible outside the
        natural bounds of this Group.

        The default implementation returns {@code true} if the Group has a back buffer.
        @see #getNaturalWidth()
        @see #getNaturalHeight()
    */
    public boolean isOverflowClipped() {
        return hasBackBuffer();
    }
    
    //
    // Sprite list queries
    //
    
    /**
        Returns an Iterator of the Sprites in this Group (in proper sequence). The iterator 
        provides a snapshot of the state of the list when the iterator was constructed. 
        No synchronization is needed while traversing the iterator. 
        The iterator does NOT support the {@code remove} method.
        @return The iterator.
    */
    public Iterator iterator() {
        return Collections.unmodifiableList(Arrays.asList(sprites)).iterator();
    }
    
    /**
        Returns the number of sprites in this group. This includes child groups but not
        the children of those groups.
    */
    public int size() {
        return sprites.length;
    }
    
    /**
        Returns the sprite at the specified position in this group. Returns {@code null } if
        the index is out of range ({@code index < 0 || index >= size()}).
    */
    public Sprite get(int index) {
        Sprite[] snapshot = sprites;
        if (index < 0 || index >= snapshot.length) {
            return null;
        }
        return snapshot[index];
    }
    
    /**
        Returns {@code true} if this Group contains the specified Sprite. 
    */
    public boolean contains(Sprite sprite) {
        return indexOf(sprites, sprite) != -1;
    }
    
    /**
        Returns {@code true} if this Group is an ancestor of the specified Sprite.
    */
    public boolean isAncestorOf(Sprite sprite) {
        Group parent = (sprite == null) ? null : sprite.getParent();
        while (parent != null) {
            if (parent == this) {
                return true;
            }
            else {
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
        Finds the Sprite whose tag is equal to the specified tag (using
        {@code tag.equals(sprite.getTag())}. Returns null if the specified tag is null, or
        if no Sprite with the specified tag is found.
    */
    public Sprite findWithTag(Object tag) {
        if (tag == null) {
            return null;
        }
        if (tag.equals(this.getTag())) {
            return this;
        }
        Sprite[] snapshot = sprites;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Sprite child = snapshot[i];
            if (child instanceof Group) {
                child = ((Group)child).findWithTag(tag);
                if (child != null) {
                    return child;
                }
            }
            else if (tag.equals(child.getTag())) {
                return child;
            }
        }
        return null;
    }
    
    /**
        Finds the top-most sprite at the specified location, or null if none is found.
        All sprites in this Group and any child Groups are searched until a sprite is found.
        This method never returns a Group.
        @param viewX x-coordinate in view space.
        @param viewY y-coordinate in view space.
        @return The top-most sprite at the specified location, or null if none is found.
    */
    public Sprite pick(int viewX, int viewY) {
        if (isOverflowClipped()) {
            double lx = getLocalX(viewX, viewY);
            double ly = getLocalY(viewX, viewY);
            double lw = CoreMath.toFloat(getNaturalWidth());
            double lh = CoreMath.toFloat(getNaturalHeight());
            if (lx < 0 || ly < 0 || lx >= lw || ly >= lh) {
                return null;
            }
        }
        Sprite[] snapshot = sprites;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Sprite child = snapshot[i];
            if (child instanceof Group) {
                child = ((Group)child).pick(viewX, viewY);
                if (child != null) {
                    return child;
                }
            }
            else if (child.contains(viewX, viewY)) {
                return child;
            }
        }
        return null;
    }
    
    /**
        Finds the top-most sprite that is enabled and visible at the specified location, or null 
        if none is found.
        All sprites in this Group and any child Groups are searched until a sprite is found.
        This method never returns a Group.
        <p>
        This Group or it's ancestors (if any) are not checked if they are enabled or visible.
        <p>
        This method is useful for finding a sprite to use to set the cursor or take mouse input
        from.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
        @return The top-most sprite that is enabled and visible at the specified location, or null 
        if none is found.
    */
    public Sprite pickEnabledAndVisible(int viewX, int viewY) {
        if (isOverflowClipped()) {
            double lx = getLocalX(viewX, viewY);
            double ly = getLocalY(viewX, viewY);
            double lw = CoreMath.toFloat(getNaturalWidth());
            double lh = CoreMath.toFloat(getNaturalHeight());
            if (lx < 0 || ly < 0 || lx >= lw || ly >= lh) {
                return null;
            }
        }
        Sprite[] snapshot = sprites;
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Sprite child = snapshot[i];
            if (child.enabled.get() == true && child.visible.get() == true && 
                child.alpha.get() > 0) 
            {
                if (child instanceof Group) {
                    child = ((Group)child).pickEnabledAndVisible(viewX, viewY);
                    if (child != null) {
                        return child;
                    }
                }
                else if (child.contains(viewX, viewY)) {
                    return child;
                }
            }
        }
        return null;
    }
    
    /**
        Returns the number of sprites in this group and all child groups (not counting child
        Groups themselves).
    */
    public int getNumSprites() {
        Sprite[] snapshot = sprites;
        int count = 0;
        for (int i = 0; i < snapshot.length; i++) {
            Sprite s = snapshot[i];
            if (s instanceof Group) {
                count += ((Group)s).getNumSprites();
            }
            else {
                count++;
            }
        }
        return count;
    }
    
    /**
        Returns the number of visible sprites in this group and all child groups (not counting child
        Groups themselves).
    */
    public int getNumVisibleSprites() {
        Sprite[] snapshot = sprites;
        if (visible.get() == false || alpha.get() == 0) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < snapshot.length; i++) {
            Sprite s = snapshot[i];
            if (s instanceof Group) {
                count += ((Group)s).getNumVisibleSprites();
            }
            else if (s.visible.get() == true && s.alpha.get() > 0) {
                count++;
            }
        }
        return count;
    }
    
    //
    // Sprite list modifications
    // NOTE: if adding another modification method, also add it to Viewport and ScrollPane
    //
    
    /**
        Adds a Sprite to this Group. The Sprite is added so it appears above all other sprites in
        this Group. If this Sprite already belongs to a Group, it is first removed from that 
        Group before added to this one.
    */
    public void add(Sprite sprite) {
        if (sprite != null) {
            synchronized (getTreeLock()) {
                Group parent = sprite.getParent();
                if (parent != null) {
                    parent.remove(sprite);
                }
                Sprite[] snapshot = sprites;
                sprites = add(snapshot, sprite, snapshot.length);
                sprite.setParent(this);
            }
        }
    }
    
    /**
        Inserts a Sprite to this Group at the specified position. The Sprite at the current
        position (if any) and any subsequent Sprites are moved up in the z-order
        (adds one to their indices).
        <p>
        If the index is less than zero, the sprite is inserted at position zero (the bottom in the 
        z-order).
        If the index is greater than or equal to {@link #size()}, the sprite is inserted at 
        position {@link #size()} (the top in the z-order).
    */
    public void add(int index, Sprite sprite) {
        if (sprite != null) {
            synchronized (getTreeLock()) {
                Group parent = sprite.getParent();
                if (parent != null) {
                    parent.remove(sprite);
                }
                Sprite[] snapshot = sprites;
                sprites = add(snapshot, sprite, index);
                sprite.setParent(this);
            }
        }
    }
    
    /**
        Removes a Sprite from this Group.
    */
    public void remove(Sprite sprite) {
        if (sprite != null) {
            synchronized (getTreeLock()) {
                Sprite[] snapshot = sprites;
                int index = indexOf(snapshot, sprite);
                if (index != -1) {
                    sprites = remove(snapshot, index);
                    sprite.setParent(null);
                }
            }
        }
    }
    
    /**
        Removes all Sprites from this Group.
    */
    public void removeAll() {
        synchronized (getTreeLock()) {
            Sprite[] snapshot = sprites;
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].setParent(null);
            }
            sprites = new Sprite[0];
        }
    }
    
    private void move(Sprite sprite, int position, boolean relative) {
        synchronized (getTreeLock()) {
            Sprite[] snapshot = sprites;
            int oldPosition = indexOf(snapshot, sprite);
            if (oldPosition != -1) {
                if (relative) {
                    position += oldPosition;
                }
                if (position < 0) {
                    position = 0;
                }
                else if (position > snapshot.length - 1) {
                    position = snapshot.length - 1;
                }
                if (oldPosition != position) {
                    snapshot = remove(snapshot, oldPosition);
                    sprites = add(snapshot, sprite, position);
                    sprite.setDirty(true);
                }
            }
        }
    }
    
    /**
        Moves the specified Sprite to the top of the z-order, so that all the other Sprites 
        currently in this Group appear underneath it. If the specified Sprite is not in this Group,
        or the Sprite is already at the top, this method does nothing.
    */
    public void moveToTop(Sprite sprite) {
        move(sprite, Integer.MAX_VALUE, false);
    }
    
    /**
        Moves the specified Sprite to the bottom of the z-order, so that all the other Sprites 
        currently in this Group appear above it. If the specified Sprite is not in this Group,
        or the Sprite is already at the bottom, this method does nothing.
    */
    public void moveToBottom(Sprite sprite) {
        move(sprite, 0, false);
    }
    
    /**
        Moves the specified Sprite up in z-order, swapping places with the first Sprite that 
        appears above it. If the specified Sprite is not in this Group, or the Sprite is already
        at the top, this method does nothing.
    */
    public void moveUp(Sprite sprite) {
        move(sprite, +1, true);
    }
    
    /**
        Moves the specified Sprite down in z-order, swapping places with the first Sprite that 
        appears below it. If the specified Sprite is not in this Group, or the Sprite is already
        at the bottom, this method does nothing.
    */
    public void moveDown(Sprite sprite) {
        move(sprite, -1, true);
    }
    
    /**
        Gets a list of all of the Sprites in this Group that were
        removed since the last call to this method.
        <p>
        This method is used by Scene2D to implement dirty rectangles.
    */
    public ArrayList getRemovedSprites() {
        ArrayList removedSprites = null;
        Filter f = getWorkingFilter();
        if (hadFilterLastUpdate && f == null) {
            // Special case: filter removed
            removedSprites = new ArrayList();
            removedSprites.add(this);
        }
        if (previousSprites == null) {
            // First call from Scene2D - no remove notifications needed
            previousSprites = sprites;
        }
        else if (previousSprites != sprites) {
            // Modifications occurred - get list of all removed sprites.
            // NOTE: we make the list here, rather than in remove(), because if the list was
            // creating in remove() and this method was never called (non-Scene2D implementation)
            // the removedSprites list would continue to grow, resulting in a memory leak.
            for (int i = 0; i < previousSprites.length; i++) {
                if (previousSprites[i].getParent() != this) {
                    if (removedSprites == null) {
                        removedSprites = new ArrayList();
                    }
                    removedSprites.add(previousSprites[i]);
                }
            }
            previousSprites = sprites;
        }
        hadFilterLastUpdate = (f != null);
        return removedSprites;
    }
    
    /**
        Packs this group so that its dimensions match the area covered by its children. 
        If this Group has a back buffer, the back buffer is resized if necessary.
        <p>
        The Group is not re-located. If child Sprites are have a negative x or y dimension,
        (are positions to the top or left of the Group bounds) a portion of the Sprite may be
        cropped.
    */
    public void pack() {
        Sprite[] snapshot = sprites;
        
        if (snapshot.length > 0) {
            // Integers
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            Rect bounds = new Rect();
            
            for (int i = 0; i < snapshot.length; i++) {
                Sprite sprite = snapshot[i];
                if (sprite instanceof Group) {
                    ((Group)sprite).pack();
                }
                sprite.getRelativeBounds(bounds);
                maxX = Math.max(maxX, bounds.x + bounds.width);
                maxY = Math.max(maxY, bounds.y + bounds.height);
            }
            fNaturalWidth = CoreMath.toFixed(maxX);
            fNaturalHeight = CoreMath.toFixed(maxY);
        }
        else {
            fNaturalWidth = 0;
            fNaturalHeight = 0;
        }
        width.setAsFixed(fNaturalWidth);
        height.setAsFixed(fNaturalHeight);
        if (hasBackBuffer()) {
            createBackBuffer(backBufferBlendMode);
        }
        setDirty(true);
    }
    
    //
    // Back buffers
    //

    /* package-private */ Transform getBackBufferTransform() {
        return (getFilter() != null) ? Sprite.IDENTITY : backBufferTransform;
    }
    
    /**
        Creates a back buffer for this Group.
        <p>
        If this Group was created with a dimension (constructors {@link #Group(int,int,int,int) } 
        or {@link #Group(double,double,double,double) } or has a dimension after calling
        {@link #pack() }, then the back buffer has the same dimensions of this Group. Otherwise,
        the back buffer has the same dimensions of the Stage.
    */
    public void createBackBuffer() {
        createBackBuffer(BlendMode.SrcOver());
    }
    
    /**
        Creates a back buffer for this Group, and sets the blend mode for rendering onto 
        the back buffer.
        <p>
        If this Group was created with a dimension (constructors {@link #Group(int,int,int,int) } 
        or {@link #Group(double,double,double,double) } or has a dimension after calling
        {@link #pack() }, then the back buffer has the same dimensions of this Group. Otherwise,
        the back buffer has the same dimensions of the Stage.
    */
    public void createBackBuffer(BlendMode blendMode) {
        setBackBufferBlendMode(blendMode);
        Transform t = new Transform();
        int w = getNaturalWidth();
        int h = getNaturalHeight();
        int backBufferWidth;
        int backBufferHeight;
        if (w == 0 || h == 0) {
            backBufferWidth = Stage.getWidth();
            backBufferHeight = Stage.getHeight();
            backBufferCoversStage = true;
            t.translate(x.getAsFixed(), y.getAsFixed());
        }
        else {
            backBufferWidth = CoreMath.toIntCeil(w);
            backBufferHeight = CoreMath.toIntCeil(h);
            backBufferCoversStage = false;
        }
        if (backBuffer == null || 
            backBuffer.getWidth() != backBufferWidth ||
            backBuffer.getHeight() != backBufferHeight)
        {
            backBuffer = new CoreImage(backBufferWidth, backBufferHeight, false);
            backBufferChanged();
        }
        if (!backBufferTransform.equals(t)) {
            backBufferTransform = t;
            backBufferChanged();
        }
    }
    
    private void backBufferChanged() {
        setDirty(true);
    }
    
    /**
        Checks if this Group has a back buffer.
        @return true if this Group has a back buffer.
    */
    public boolean hasBackBuffer() {
        return (backBuffer != null);
    }

    public CoreImage getBackBuffer() {
        return backBuffer;
    }
    
    /**
        Removes this Group's back buffer.
    */
    public void removeBackBuffer() {
        if (backBuffer != null) {
            backBuffer = null;
            backBufferChanged();
        }
    }
    
    /**
        Sets this Group's blend mode for rendering onto its back buffer.
        @param backBufferBlendMode the blend mode.
    */
    public void setBackBufferBlendMode(BlendMode backBufferBlendMode) {
        if (backBufferBlendMode == null) {
            backBufferBlendMode = BlendMode.SrcOver();
        }
        if (this.backBufferBlendMode != backBufferBlendMode) {
            this.backBufferBlendMode = backBufferBlendMode;
            backBufferChanged();
        }
    }
    
    /**
        Gets this Group's blend mode for rendering onto its back buffer.
        @return the blend mode.
    */
    public BlendMode getBackBufferBlendMode() {
        return backBufferBlendMode;
    }
    
    //
    // Sprite class implementation
    // 
    
    protected int getNaturalWidth() {
        if (fNaturalWidth > 0) {
            return fNaturalWidth;
        }
        else {
            return width.getAsFixed();
        }
    }
    
    protected int getNaturalHeight() {
        if (fNaturalHeight > 0) {
            return fNaturalHeight;
        }
        else {
            return height.getAsFixed();
        }
    }
    
    public void propertyChange(Property p) {
        super.propertyChange(p);
        if ((p == width || p == height) && hasBackBuffer()) {
            setDirty(true);
        }
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);

        Filter f = getWorkingFilter();
        
        Sprite[] snapshot = sprites;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].update(elapsedTime);
            if (f != null && snapshot[i].isDirty()) {
                setDirty(true);
            }
        }
    }
    
    protected final void drawSprite(CoreGraphics g) {
        Sprite[] snapshot = sprites;
        
        if (backBuffer == null) {
            Rect oldClip = null;
            boolean setClip = isOverflowClipped();

            if (setClip) {
                Transform t = g.getTransform();
                oldClip = g.getClip();
                Rect newClip = new Rect();
                t.getBounds(width.getAsFixed(), height.getAsFixed(), newClip);
                g.clipRect(newClip);
            }

            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].draw(g);
            }

            if (setClip) {
                g.setClip(oldClip);
            }
        }
        else {
            int clipX;
            int clipY; 
            int clipW;
            int clipH;
            Transform clipTransform;
            CoreGraphics g2 = backBuffer.createGraphics();
            g2.setBlendMode(backBufferBlendMode);
            
            if (g == null) {
                clipX = 0;
                clipY = 0; 
                clipW = backBuffer.getWidth();
                clipH = backBuffer.getHeight();
                clipTransform = Sprite.IDENTITY;
            }
            else {
                clipX = g.getClipX();
                clipY = g.getClipY(); 
                clipW = g.getClipWidth();
                clipH = g.getClipHeight();
                if (backBufferCoversStage) {
                    clipTransform = g.getTransform();
                }
                else {
                    clipTransform = getDrawTransform();
                }
            }

            if (clipTransform.getType() != Transform.TYPE_IDENTITY) {
                Tuple2i p1 = new Tuple2i(CoreMath.toFixed(clipX), CoreMath.toFixed(clipY));
                Tuple2i p2 = new Tuple2i(
                    CoreMath.toFixed(clipX + clipW), 
                    CoreMath.toFixed(clipY + clipH));
                
                boolean success = true;
                success &= clipTransform.inverseTransform(p1);
                success &= clipTransform.inverseTransform(p2);
                if (!success) {
                    return;
                }
                if (p2.x < p1.x) {
                    int t = p1.x;
                    p1.x = p2.x;
                    p2.x = t;
                }
                if (p2.y < p1.y) {
                    int t = p1.y;
                    p1.y = p2.y;
                    p2.y = t;
                }
                clipX = CoreMath.toIntFloor(p1.x) - 1;
                clipY = CoreMath.toIntFloor(p1.y) - 1;
                clipW = CoreMath.toIntCeil(p2.x) - clipX + 2;
                clipH = CoreMath.toIntCeil(p2.y) - clipY + 2;
            }
            g2.setClip(clipX, clipY, clipW, clipH);
            g2.clear();
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i].draw(g2);
            }

            // g will be null if called from a filtered sprite
            if (g != null) {
                if (backBufferCoversStage) {
                    // Note: setting the transform is ok;
                    // the transform is popped upon returning from drawSprite()
                    g.setTransform(Stage.getDefaultTransform());
                }

                g.drawImage(backBuffer);
            }
        }
    }
    
    //
    // Static convenience methods for working with immutable Sprite arrays
    //
    
    private static int indexOf(Sprite[] snapshot, Sprite s) {
        for (int i = 0; i < snapshot.length; i++) {
            if (s == snapshot[i]) {
                return i;
            }
        }
        return -1;
    }
    
    private static Sprite[] remove(Sprite[] snapshot, int index) {
        if (index >= 0 && index < snapshot.length) {
            Sprite[] newSprites = new Sprite[snapshot.length - 1];
            System.arraycopy(snapshot, 0, newSprites, 0, index);
            System.arraycopy(snapshot, index + 1, newSprites, index, 
                newSprites.length - index);
            snapshot = newSprites;
        }
        return snapshot;
    }
    
    private static Sprite[] add(Sprite[] snapshot, Sprite sprite, int index) {
        if (index < 0) {
            index = 0;
        }
        else if (index > snapshot.length) {
            index = snapshot.length;
        }
        Sprite[] newSprites = new Sprite[snapshot.length + 1];
        System.arraycopy(snapshot, 0, newSprites, 0, index);
        newSprites[index] = sprite;
        System.arraycopy(snapshot, index, newSprites, index + 1, snapshot.length - index);
        return newSprites;
    }
}