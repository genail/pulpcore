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

import java.util.WeakHashMap;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.animation.Bool;
import pulpcore.animation.Easing;
import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.image.CoreGraphics;
import pulpcore.image.BlendMode;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.math.Tuple2i;
import pulpcore.scene.Scene2D;
import pulpcore.Stage;
import pulpcore.image.CoreImage;
import pulpcore.image.filter.Filter;
import pulpcore.image.filter.FilterChain;

/**
    The superclass of all sprites. Contains location, dimension, alpha, 
    angle, visibility, and anchor information. 
    The Sprite does no drawing - subclasses implement the 
    {@link #drawSprite(CoreGraphics)}
    method to draw.
*/
public abstract class Sprite implements PropertyListener {

    // WeakHashMap<Filter, null>
    private static final WeakHashMap usedFilters = new WeakHashMap();
    
    static final Transform IDENTITY = new Transform();

    //
    // Text anchors
    //
    
    /** 
        Constant for positioning the anchor point at the "default" location
        of the Sprite, which is at the start its upper-left corner and can
        be changed by setting anchorX and anchorY property.  One exception is
        ImageSprite which uses the image's hotspot at the default anchor. This
        also can be modified any time by changing properties. 
        This is the default anchor.
    */
    public static final int DEFAULT = 0;
    
    /** Constant for positioning the anchor point on the left side of the sprite. */
    /* package-private */ static final int LEFT = 1;
    
    /** Constant for positioning the anchor point on the right side of the sprite. */
    /* package-private */ static final int RIGHT = 2;
    
    /** Constant for positioning the anchor point in the horizontal center of the sprite. */
    /* package-private */ static final int HCENTER = 4;
    
    /** Constant for positioning the anchor point on the upper side of the sprite. */
    /* package-private */ static final int TOP = 8;
    
    /** Constant for positioning the anchor point on the lower side of the sprite. */
    /* package-private */ static final int BOTTOM = 16;
    
    /** Constant for positioning the anchor point in the vertical center of the sprite. */
    /* package-private */ static final int VCENTER = 32;
    
    /** 
        Constant for positioning the anchor point in the upper center of the sprite.
    */
    public static final int NORTH = TOP | HCENTER;
    
    /** 
        Constant for positioning the anchor point in the lower center of the sprite.
    */
    public static final int SOUTH = BOTTOM | HCENTER;
    
    /** 
        Constant for positioning the anchor point in the left center of the sprite.
    */
    public static final int WEST = LEFT | VCENTER;
    
    /** 
        Constant for positioning the anchor point in the right center of the sprite.
        Equivalent to RIGHT | VCENTER.
    */
    public static final int EAST = RIGHT | VCENTER;
    
    /** 
        Constant for positioning the anchor point in the upper left corner of the sprite.
    */
    public static final int NORTH_WEST = TOP | LEFT;
    
    /** 
        Constant for positioning the anchor point in the upper right corner of the sprite.
        Equivalent to TOP | RIGHT.
    */
    public static final int NORTH_EAST = TOP | RIGHT;
    
    /** 
        Constant for positioning the anchor point in the lower left corner of the sprite.
    */
    public static final int SOUTH_WEST = BOTTOM | LEFT;
    
    /** 
        Constant for positioning the anchor point in the lower right corner of the sprite.
        Equivalent to BOTTOM | RIGHT.
    */
    public static final int SOUTH_EAST = BOTTOM | RIGHT;
    
    /** 
        Constant for positioning the anchor point in the center of the sprite.
    */
    public static final int CENTER = VCENTER | HCENTER;
    
    //
    // Properties
    //
    
    /**
        The flag indicating whether this Sprite is enabled. An enabled sprite can 
        respond to user input. Sprites are enabled by default.
    */
    public final Bool enabled = new Bool(this, true);
    
    /** The x location of this Sprite. */
    public final Fixed x = new Fixed(this);
    
    /** The y location of this Sprite. */
    public final Fixed y = new Fixed(this);
    
    /** The width of this Sprite. */
    public final Fixed width = new Fixed(this);
    
    /** The height of this Sprite. */
    public final Fixed height = new Fixed(this);
    
    /**
        The x anchor point of this Sprite, in range from 0.0 to 1.0. A value of
        0.0 is far left point of the Sprite and a value of 1.0 is far right point.
        The default is 0.0.
    */
    public final Fixed anchorX = new Fixed(this);
    
    /**
	    The y anchor point of this Sprite, in range from 0.0 to 1.0. A value of
	    0.0 is far top point of the Sprite and a value of 1.0 is far bottom point.
	    The default is 0.0.
	*/
    public final Fixed anchorY = new Fixed(this);
    
    /** 
        The angle of this Sprite, typically in range from 0 to 2*PI,
        although the angle can have any value. The Sprite is rotated around its anchor.
    */
    public final Fixed angle = new Fixed(this);

    /** 
        The alpha of this Sprite, in range from 0 to 255. A value of 0 is fully 
        transparent and a value of 255 is fully opaque. The default is 255.
    */
    public final Int alpha = new Int(this, 0xff);
    
    /** 
        The flag indicating whether or not this Sprite is visible.
    */
    public final Bool visible = new Bool(this, true);
    
    /**
        Sets whether pixel snapping enabled for rendering this Sprite. 
        If this value is true, only the integer portion of the x and y
        properties are used to draw this sprite.
        <p>
        Enabling pixel snapping may allow some type of images (e.g. pixel art) to look better.
        <p>
        This value is false by default.
    */
    public final Bool pixelSnapping = new Bool(this, false);
    
    private Group parent;
    private int anchor = DEFAULT;
    private int cursor = -1;
    private BlendMode blendMode = null;
    private SpriteFilter filter;
    private Object tag;

    // State information
    private int cosAngle = CoreMath.ONE;
    private int sinAngle = 0;
    private int parentTransformModCount;
    private boolean dirty = true;
    private boolean transformDirty = true;
    
    /** 
        The view transform, used for dirty rectangles and collision detection.
    */
    private final Transform viewTransform = new Transform();
    
    /**
        The draw transform, which will be different from the view transform if an ancestor 
        has a back buffer that does not cover the Stage. 
    */
    private final Transform drawTransform = new Transform();
    
    /** The draw bounding box used for dirty rectangles in Scene2D */
    private Rect dirtyRect;
    
    
    public Sprite(int x, int y, int width, int height) {
        this.x.set(x);
        this.y.set(y);
        this.width.set(width);
        this.height.set(height);
    }
    
    public Sprite(double x, double y, double width, double height) {
        this.x.set(x);
        this.y.set(y);
        this.width.set(width);
        this.height.set(height);
    }

    /**
        Gets this Sprite's tag.
        @see #setTag(Object)
        @see Group#findWithTag(Object)
    */
    public Object getTag() {
        return tag;
    }

    /**
        Sets this Sprite's tag. The tag can be used for marking the sprite or storing information
        with it. Different Sprites can share identical tags. By default, the tag is {@code null}.
        @see #getTag()
        @see Group#findWithTag(Object)
    */
    public void setTag(Object tag) {
        this.tag = tag;
    }

    /**
        Returns true if this Sprite is opaque. In other words, before applying transforms and alpha,
        all the pixels within it's bounds are drawn and are themselves opaque.
        <p>
        Returns false by default.
    */
    public boolean isOpaque() {
        return false;
    }
    
    /**
        Gets this Sprite's parent Group, or null if this Sprite does not have a parent.
    */
    public final Group getParent() {
        return parent;
    }
    
    /**
        Removes this Sprite from its parent Group. If this Sprite does not have a parent,
        this method does nothing.
    */
    public void removeFromParent() {
        Group p = parent;
        if (p != null) {
            p.remove(this);
        }
    }

    /**
        Gets this Sprite's oldest ancestor Group, or null if this Sprite does not have a parent.
    */
    public final Group getRoot() {
        Group currRoot = null;
        Group nextRoot = parent;
        while (true) {
            if (nextRoot == null) {
                return currRoot;
            }
            else {
                currRoot = nextRoot;
                nextRoot = nextRoot.getParent();
            }
        }
    }
    
    /**
        Gets the {@link pulpcore.scene.Scene2D } this Sprite belongs to, or {@code null} if 
        this Sprite is not in a Scene2D.
    */
    public Scene2D getScene2D() {
        Group root = getRoot();
        if (root != null) {
            // Scene2D's root overrides the getScene2D() method.
            return root.getScene2D();
        }
        else {
            return null;
        }
    }
    
    /* package-private */ final void setParent(Group parent) {
        if (this.parent != parent) {
            this.parent = parent;
            if (parent == null) {
                parentTransformModCount = -1;
            }
            else {
                parentTransformModCount = parent.getTransformModCount();
            }
            setDirty(true);
        }
    }

    /**
        Returns true if this Sprite's enabled property is set to true and its parent, if any, 
        is enabled.
    */
    public final boolean isEnabled() {
        return (enabled.get() == true && (parent == null || parent.isEnabled())); 
    }
    
    /**
        Returns true if this Sprite's enabled property is set to true, its visible property set
        to true, its alpha property is greater than zero, and its parent, if any, is enabled
        and visible.
    */
    public final boolean isEnabledAndVisible() {
        return (enabled.get() == true && visible.get() == true && 
            alpha.get() > 0 && (parent == null || parent.isEnabledAndVisible())); 
    }
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public final Rect getDirtyRect() {
        if (dirtyRect == null || dirtyRect.width <= 0) {
            return null;
        }
        else {
            return dirtyRect;
        }
    }
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public final boolean updateDirtyRect() {
        
        boolean changed = false;
        boolean isUnconstrainedGroup = false;
        if (this instanceof Group) {
            Group group = (Group)this;
            if (!group.isOverflowClipped()) {
                isUnconstrainedGroup = true;
            }
            else if ((group.getNaturalWidth() == 0 || group.getNaturalHeight() == 0) &&
                    getWorkingFilter() == null)
            {
                // Group has backbuffer that covers the stage, but no filter
                isUnconstrainedGroup = true;
            }
        }
        
        if (visible.get() == false || alpha.get() <= 0 || isUnconstrainedGroup) {
            changed = (getDirtyRect() != null);
            clearDirtyRect();
        }
        else {
            if (dirtyRect == null) {
                changed = true;
                dirtyRect = new Rect();
            }
            
            updateTransform();
            
            // Dirty rectangles are in device space. Convert view space to device space.
            Transform d = Stage.getDefaultTransform();
            Transform t = viewTransform;
            if (d.getType() != Transform.TYPE_IDENTITY) {
                t = new Transform(d);
                t.concatenate(viewTransform);
            }
            
            int w = getNaturalWidth();
            int h = getNaturalHeight();

            Filter f = getWorkingFilter();
            if (f != null && (w == 0 || h == 0)) {
                // Back buffer covers stage
                t = d;
                w = CoreMath.toFixed(Stage.getWidth());
                h = CoreMath.toFixed(Stage.getHeight());
            }
            else if (f != null) {
                int fx = f.getX();
                int fy = f.getY();
                if (fx != 0 || fy != 0) {
                    t = new Transform(t);
                    t.translate(CoreMath.toFixed(fx),CoreMath.toFixed(fy));
                }
                w = CoreMath.toFixed(f.getWidth());
                h = CoreMath.toFixed(f.getHeight());
            }
            
            changed |= t.getBounds(w, h, dirtyRect);
        }
        
        return changed;
    }
    
    /**
        For dirty rectangles - most apps will not need ot call this method directly.
    */
    public final void clearDirtyRect() {
        if (dirtyRect != null) {
            dirtyRect.width = -1;
        }
    }

    // NOTE:
    // We need differentiate between "contentsDirty" and "transformDirty".
    // At the moment, some classes (Group, Scene2D) are assuming setDirty(true) sets one
    // (or both) fields dirty.

    /**
        Marks this Sprite as dirty, which will force it to redraw on the next frame.
    */
    public final void setDirty(boolean dirty) {
        setDirty(dirty, dirty);
    }

    private final void setDirty(boolean dirty, boolean contentsChanged) {
        this.dirty = dirty;
        if (dirty) {
            transformDirty = true;
        }
        if (contentsChanged && filter != null) {
            filter.setDirty();
        }
    }

    /**
        Returns true if the Sprite's properties have changed since the last call to draw()
    */
    public final boolean isDirty() {
        if (!dirty) {
            Filter f = getWorkingFilter();
            if (f != null && f.isDirty()) {
                setDirty(true);
            }
        }
        return dirty;
    }

    /* package-private */ final boolean isTransformDirty() {
        if (transformDirty) {
            return true;
        }
        else if (parent == null) {
            return false;
        }
        else {
            return (parentTransformModCount != parent.getTransformModCount() ||
                parent.isTransformDirty());
        }
    }

    /* package-private */ final Transform getViewTransform() {
        updateTransform();
        return viewTransform;
    }

    /* package-private */ final Transform getDrawTransform() {
        updateTransform();
        return drawTransform;
    }
    
    /* package-private */ final Transform getParentViewTransform() {
        if (parent == null) {
            return IDENTITY;
        }
        else {
            return parent.getViewTransform();
        }
    }
    
    /* package-private */ final Transform getParentDrawTransform() {
        if (parent == null) {
            return Stage.getDefaultTransform();
        }
        else if (parent.hasBackBuffer()) {
            return parent.getBackBufferTransform();
        }
        else {
            return parent.getDrawTransform();
        }
    }
    
    /* package-private */ final void updateTransform(Transform parentTransform, 
        Transform transform) 
    {
        transform.set(parentTransform);
            
        // Translate
        transform.translate(x.getAsFixed(), y.getAsFixed());
        
        // Rotate
        if (cosAngle != CoreMath.ONE || sinAngle != 0) {
            transform.rotate(cosAngle, sinAngle);
        }
        
        // Scale
        int naturalWidth = getNaturalWidth();
        int naturalHeight = getNaturalHeight();
        if (naturalWidth > 0 && naturalHeight > 0 &&
            (naturalWidth != width.getAsFixed() || naturalHeight != height.getAsFixed())) 
        {
            int sx = CoreMath.div(width.getAsFixed(), naturalWidth);
            int sy = CoreMath.div(height.getAsFixed(), naturalHeight);
            transform.scale(sx, sy);
        }
        
        // Adjust for anchor
        transform.translate(-getAnchorX(), -getAnchorY());

        // Snap to the nearest integer location
        if (pixelSnapping.get()) {
            transform.roundTranslation();
        }
    }
    
    /**
        Gets the bounds relative to the parent.
    */
    /* package-private */ final void getRelativeBounds(Rect bounds) {
        Transform transform = new Transform();
        updateTransform(null, transform);
        transform.getBounds(getNaturalWidth(), getNaturalHeight(), bounds);
    }
    
    private final void updateTransform() {
        if (isTransformDirty()) {
            Transform pvt = getParentViewTransform();
            Transform pdt = getParentDrawTransform();
            updateTransform(pvt, viewTransform);
            if (pvt.equals(pdt)) {
                drawTransform.set(viewTransform);
            }
            else {
                updateTransform(pdt, drawTransform);
            }
            
            // Keep track of dirty state
            transformDirty = false;
            if (parent != null) {
                // Must happen after getParentTransform()
                parentTransformModCount = parent.getTransformModCount();
            }
            if (this instanceof Group) {
                ((Group)this).updateTransformModCount();
            }
        }
    }

    /**
        Gets the fixed-point value of the Sprite's natural width. Subclasses will override this
        method to specify the natural width. The natural width is the width of the Sprite if no
        scaling is applied - for an {@link ImageSprite}, the natural width is the
        width of the image.
    */
    protected int getNaturalWidth() {
        return width.getAsFixed();
    }
    
    /**
        Gets the fixed-point value of the Sprite's natural height. Subclasses will override this
        method to specify the natural height. The natural height is the height of the Sprite if no
        scaling is applied - for an {@link ImageSprite}, the natural height is the
        height of the image.
    */
    protected int getNaturalHeight() {
        return height.getAsFixed();
    }
    
    /**
        @return the fixed-point x anchor, typically from 0 to 
        {@code getNaturalWidth() - CoreMath.ONE}.
    */
    protected int getAnchorX() {
    	if (anchor == DEFAULT) {
            return CoreMath.round(CoreMath.mul(anchorX.getAsFixed(), getNaturalWidth()));
        }
    	else if ((anchor & HCENTER) != 0) {
            // Special case: make sure centered sprites are drawn on an integer boundary
            return CoreMath.floor(getNaturalWidth() / 2);
        }
        else if ((anchor & RIGHT) != 0) {
            return getNaturalWidth() - CoreMath.ONE;
        }
        else {
        	return 0;
        }
    }
    
    /**
        @return the fixed-point y anchor, typically from 0 to 
        {@code getNaturalHeight() - CoreMath.ONE}.
    */
    protected int getAnchorY() {
    	if (anchor == DEFAULT) {
    		return CoreMath.round(CoreMath.mul(anchorY.getAsFixed(), getNaturalHeight()));
    	}
        if ((anchor & VCENTER) != 0) {
            // Special case: make sure centered sprites are drawn on an integer boundary
            return CoreMath.floor(getNaturalHeight() / 2);
        }
        else if ((anchor & BOTTOM) != 0) {
            return getNaturalHeight() - CoreMath.ONE;
        }
        else {
        	return 0;
        }
    }
    
    /**
        Sets the anchor of this Sprite. The anchor affects where the Sprite is drawn in
        relation to its (x, y) location, and can be one of {@link #DEFAULT}, 
        {@link #NORTH}, {@link #SOUTH}, {@link #WEST}, {@link #EAST},
        {@link #NORTH_WEST}, {@link #SOUTH_WEST}, {@link #NORTH_EAST}, {@link #SOUTH_EAST}, or
        {@link #CENTER}.
        <p>
        <pre>
        NW     N     NE
          +----+----+
          |         |
        W +    *    + E
          |         |
          +----+----+
        SW     S     SE
        </pre>
        The {@link #DEFAULT} anchor uses {@link #anchorX} and {@link #anchorY} properties
        to determine anchor location. The default is {@link #NORTH_WEST} for most Sprites
        (except for ImageSprites, which set the CoreImage's hotspot as the anchor).  
    */
    public final void setAnchor(int anchor) {
        if (this.anchor != anchor) {
            this.anchor = anchor;
            setDirty(true);
        }
    }
    
    public final int getAnchor() {
        return anchor;
    }
    
    /**
        Sets the cursor for this Sprite. By default, a Sprite does not have a defined cursor.
        Note, the Sprite itself does not set the cursor -
        it is set by {@link pulpcore.scene.Scene2D}.
        @see pulpcore.Input
        @see #getCursor()
        @see #clearCursor()
    */
    public final void setCursor(int cursor) {
        this.cursor = cursor;
    }
    
    /**
        Clears the cursor for this Sprite, so that it's parent cursor is used.
        @see pulpcore.Input
        @see #getCursor()
        @see #setCursor(int)
    */
    public final void clearCursor() {
        this.cursor = -1;
    }
    
    /**
        Gets the cursor for this Sprite. If a cursor is not defined for this Sprite, the parent's
        cursor is used.
        @see pulpcore.Input
        @see #setCursor(int)
        @see #clearCursor()
    */
    public final int getCursor() {
        if (cursor == -1) {
            if (parent == null) {
                return Input.CURSOR_DEFAULT;
            }
            else {
                return parent.getCursor();
            }
        }
        else {
            return cursor;
        }
    }
    
    /**
        Sets the blend mode used to draw this Sprite. By default, the blend mode 
        method is null, which means the blend mode of this Sprite's parent is used. 
        @see pulpcore.image.BlendMode
    */
    public final void setBlendMode(BlendMode blendMode) {
        if (this.blendMode != blendMode) {
            this.blendMode = blendMode;
            setDirty(true);
        }
    }
    
    public final BlendMode getBlendMode() {
        return blendMode;
    }

    /**
        Sets the image filter for this Sprite.
        If this Sprite is a Group with no backbuffer, a backbuffer is created.
        The default filter is {@code null}.
        <p>
        If the specified filter is already attached to a Sprite, a clone of it is created.
        @see #getFilter()
    */
    public final void setFilter(Filter filter) {
        Filter currFilter = getFilter();
        Filter newFilter = filter;
        if (currFilter != newFilter) {
            setDirty(true);
            synchronized (usedFilters) {
                if (currFilter != null) {
                    currFilter.setInput(null);
                    markAsUnused(currFilter);
                    this.filter = null;
                }
                
                newFilter = copyIfUsed(newFilter);

                if (newFilter != null) {
                    markAsUsed(newFilter);
                    this.filter = new SpriteFilter(this, newFilter);
                    if ((this instanceof Group) && !((Group)this).hasBackBuffer()) {
                        ((Group)this).createBackBuffer();
                    }
                }
            }
        }
    }

    private Filter copyIfUsed(Filter filter) {
        if (filter != null && usedFilters.containsKey(filter)) {
            filter = filter.copy();
        }
        if (filter instanceof FilterChain) {
            FilterChain chain = (FilterChain)filter;
            for (int i = 0; i < chain.size(); i++) {
                chain.set(i, copyIfUsed(chain.get(i)));
            }
        }
        return filter;
    }

    private Filter markAsUsed(Filter filter) {
        if (filter != null) {
            usedFilters.put(filter, null);
        }
        if (filter instanceof FilterChain) {
            FilterChain chain = (FilterChain)filter;
            for (int i = 0; i < chain.size(); i++) {
                markAsUsed(chain.get(i));
            }
        }
        return filter;
    }

    private Filter markAsUnused(Filter filter) {
        if (filter != null) {
            usedFilters.remove(filter);
        }
        if (filter instanceof FilterChain) {
            FilterChain chain = (FilterChain)filter;
            for (int i = 0; i < chain.size(); i++) {
                markAsUnused(chain.get(i));
            }
        }
        return filter;
    }

    /**
        Gets the image filter for this Sprite, or null if there is no filter.
        @see #setFilter(pulpcore.image.filter.Filter) 
    */
    public final Filter getFilter() {
        if (filter != null) {
            return filter.getFilter();
        }
        else {
            return null;
        }
    }

    /* package-private */ Filter getWorkingFilter() {
        Filter f = getFilter();
        if (f != null) {
            if ((this instanceof Group) && !((Group)this).hasBackBuffer()) {
                // Ignore filters on groups with no back buffer
                return null;
            }
            else {
                // Make sure input is up to date
                f.setInput(filter.getCacheImage());
            }
        }
        return f;
    }

    private static class SpriteFilter {
        private final Sprite sprite;
        private final Filter filter;
        private CoreImage cache = null;
        private boolean cacheDirty = true;

        public SpriteFilter(Sprite sprite, Filter filter) {
            this.sprite = sprite;
            this.filter = filter;
            this.filter.setDirty();
            this.filter.setInput(getCacheImage());
        }

        public Filter getFilter() {
            return filter;
        }

        private int getCacheWidth() {
            if (sprite instanceof Group && ((Group)sprite).hasBackBuffer()) {
                return ((Group)sprite).getBackBuffer().getWidth();
            }
            else {
                return CoreMath.toIntCeil(sprite.getNaturalWidth());
            }
        }

        private int getCacheHeight() {
            if (sprite instanceof Group && ((Group)sprite).hasBackBuffer()) {
                return ((Group)sprite).getBackBuffer().getHeight();
            }
            else {
                return CoreMath.toIntCeil(sprite.getNaturalHeight());
            }
        }

        public void setDirty() {
            filter.setDirty();
            cacheDirty = true;
        }

        public CoreImage getCacheImage() {
            if (sprite instanceof ImageSprite) {
                return ((ImageSprite)sprite).getImage();
            }
            else if (sprite instanceof Group && ((Group)sprite).hasBackBuffer()) {
                // Update the back buffer
                cache = null;
                if (cacheDirty) {
                    sprite.drawSprite(null);
                    cacheDirty = false;
                }
                return ((Group)sprite).getBackBuffer();
            }
            else {
                int w = getCacheWidth();
                int h = getCacheHeight();
                boolean isOpaque = sprite.isOpaque();
                boolean needsClear = true;
                if (cache == null ||
                    cache.getWidth() != w ||
                    cache.getHeight() != h ||
                    cache.isOpaque() != isOpaque)
                {
                    cache = new CoreImage(w, h, isOpaque);
                    //pulpcore.CoreSystem.print("New Sprite cache: " + w + "x" + h);
                    cacheDirty = true;
                    needsClear = false;
                }
                if (cacheDirty) {
                    //pulpcore.CoreSystem.print("Sprite re-cached");
                    CoreGraphics g = cache.createGraphics();
                    if (needsClear) {
                        g.clear();
                    }
                    sprite.drawSprite(g);
                    cacheDirty = false;
                }
                return cache;
            }
        }
    }

    /**
        Updates all of this Sprite's properties. Subclasses that override this method should
        call super.update().
    */
    public void update(int elapsedTime) {
        x.update(elapsedTime);
        y.update(elapsedTime);
        width.update(elapsedTime);
        height.update(elapsedTime);
        anchorX.update(elapsedTime);
        anchorY.update(elapsedTime);
        alpha.update(elapsedTime);
        angle.update(elapsedTime);
        visible.update(elapsedTime);
        enabled.update(elapsedTime);
        pixelSnapping.update(elapsedTime);

        Filter f = getWorkingFilter();
        if (f != null) {
            f.update(elapsedTime);
        }
    }

    /**
        On a property change this Sprite is marked as dirty.
    */
    public void propertyChange(Property property) {
        // The following properties don't change the contents, by default:
        // x, y, width, height, alpha, angle, visible, enabled, pixelSnapping
        setDirty(true, false);
        if (property == angle) {
            cosAngle = CoreMath.cos(angle.getAsFixed());
            sinAngle = CoreMath.sin(angle.getAsFixed());
        }
    }

    /**
        Draws the Sprite. Subclasses override {@link #drawSprite(pulpcore.image.CoreGraphics) }.
     */
    public final void draw(CoreGraphics g) {
        if (!visible.get()) {
            return;
        }

        // Set alpha
        int newAlpha = alpha.get();
        int oldAlpha = g.getAlpha();
        if (oldAlpha != 255) {
            newAlpha = (newAlpha * oldAlpha) >> 8;
        }
        if (newAlpha <= 0) {
            return;
        }

        if (isDirty()) {
            updateTransform();
            setDirty(false);
        }
        
        g.setAlpha(newAlpha);
        
        // Set blend mode
        BlendMode oldBlendMode = g.getBlendMode();
        if (blendMode != null) {
            g.setBlendMode(blendMode);
        }
        
        // Set transform
        Transform t = drawTransform;
        Filter f = getWorkingFilter();
        if (f != null) {
            int fx = f.getX();
            int fy = f.getY();
            if (fx != 0 || fy != 0) {
                t = new Transform(t);
                t.translate(CoreMath.toFixed(fx), CoreMath.toFixed(fy));
            }
        }

        // Set transform
        g.pushTransform();
        g.setTransform(t);

        // Draw
        int oldEdgeClamp = g.getEdgeClamp();
        if (f != null) {
            if (this instanceof ImageSprite) {
                // Respect the antiAlias setting
                boolean antiAlias = ((ImageSprite)this).antiAlias.get();
                g.setEdgeClamp(antiAlias ? CoreGraphics.EDGE_CLAMP_NONE :
                    CoreGraphics.EDGE_CLAMP_ALL);
            }
            g.drawImage(f.getOutput());
        }
        else {
            drawSprite(g);
        }
        
        // Undo changes
        g.popTransform();
        g.setAlpha(oldAlpha);
        g.setBlendMode(oldBlendMode);
        g.setEdgeClamp(oldEdgeClamp);
    }
    
    /**
        Draws the sprite. The graphic context's alpha is set to this sprite's alpha,
        and it's translation is offset by this sprite's location.
        This method is not called if the sprite is not visible or it's alpha
        is less than or equal to zero.
        <p>
        This method may be called multiple times for each dirty rectangle. The clip of the
        graphics context will be set to the current dirty rectangle.
        <p>
        When the contents of this sprite change (in another words, the graphic output of this method
        will be different from the last time it is called), subclasses should call
        {@code setDirty(true)}.
        <p>
        Implementors should not save a reference to the graphics context as it can change
        between calls to this method.
    */
    protected abstract void drawSprite(CoreGraphics g);
    
    /**
        Gets the integer x-coordinate in Local Space of the specified location in
        View Space. Returns Double.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public final double getLocalX(double viewX, double viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localX = viewTransform.inverseTransformX(fx, fy);
        if (localX == Integer.MAX_VALUE) {
            return Double.MAX_VALUE;
        }
        return CoreMath.toDouble(localX);
    }
    
    /**
        Gets the integer y-coordinate in Local Space of the specified location in
        View Space. Returns Double.MAX_VALUE if the Local Space is invalid 
        (that is, it's Transform determinant is zero).
    */
    public final double getLocalY(double viewX, double viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localY = viewTransform.inverseTransformY(fx, fy);
        if (localY == Integer.MAX_VALUE) {
            return Double.MAX_VALUE;
        }
        return CoreMath.toDouble(localY);
    }
    
    /**
        Gets the x-coordinate of this sprite in View Space.
    */
    public final double getViewX() {
        updateTransform();
        return CoreMath.toDouble(viewTransform.getTranslateX());
    }
    
    /**
        Gets the y-coordinate of this sprite in View Space.
    */
    public final double getViewY() {
        updateTransform();
        return CoreMath.toDouble(viewTransform.getTranslateY());
    }
    
    /**
        Gets the x-coordinate in View Space of the specified location in
        Local Space.
    */
    public final double getViewX(double localX, double localY) {
        updateTransform();
        int fx = CoreMath.toFixed(localX);
        int fy = CoreMath.toFixed(localY);
        return CoreMath.toDouble(viewTransform.transformX(fx, fy));
    }
    
    /**
        Gets the y-coordinate in View Space of the specified location in
        Local Space.
    */
    public final double getViewY(double localX, double localY) {
        updateTransform();
        int fx = CoreMath.toFixed(localX);
        int fy = CoreMath.toFixed(localY);
        return CoreMath.toDouble(viewTransform.transformY(fx, fy));
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite. 
            
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
        @return true if the specified point is within the bounds of this 
        Sprite.
    */
    public final boolean contains(int viewX, int viewY) {
        updateTransform();
        int fx = CoreMath.toFixed(viewX);
        int fy = CoreMath.toFixed(viewY);
        int localX = viewTransform.inverseTransformX(fx, fy);
        int localY = viewTransform.inverseTransformY(fx, fy);
        
        if (localX == Integer.MAX_VALUE || localY == Integer.MAX_VALUE) {
            return false;
        }
        
        if (localX >= 0 && localX < getNaturalWidth() &&
            localY >= 0 && localY < getNaturalHeight())
        {
            if (getPixelLevelChecks()) {
                localX = CoreMath.toIntFloor(localX);
                localY = CoreMath.toIntFloor(localY);
                return !isTransparent(localX, localY);
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }
    
    /**
        Returns true if this sprite should use pixel-level checks for intersections and picking.
        <p>
        This method returns false. Subclasses of Sprite should override this method if they
        have pixel-level checks in their implementation of {@link #isTransparent(int, int) }.
    */
    public boolean getPixelLevelChecks() {
        return false;
    }
    
    /**
        Checks if the pixel at the specified integer location is transparent. This method does not
        check if this sprite is enabled or visible, nor does it check its alpha value. 
        <p>
        The default implementation always returns false. 
        Subclasses of this class may need to override this method to return accurate results.
        <p>
        This method is called from {@link #contains(int,int)}.
        @param localX integer x-coordinate in local space
        @param localY integer y-coordinate in local space
    */
    protected boolean isTransparent(int localX, int localY) {
        return false;
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite and this Sprite is the top-most Sprite at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public final boolean isPick(int viewX, int viewY) {
        if (contains(viewX, viewY)) {
            // Since the location is within the sprite, root.pick() won't search below this
            // sprite in the scene graph
            Group root = getRoot();
            return (root == null || root.pick(viewX, viewY) == this);
        }
        else {
            return false;
        }
    }
    
    /**
        Checks if the specified location is within the bounds of this 
        Sprite and this Sprite is the top-most visible and enabled Sprite at that location.
        @param viewX x-coordinate in view space
        @param viewY y-coordinate in view space
    */
    public final boolean isPickEnabledAndVisible(int viewX, int viewY) {
        if (contains(viewX, viewY)) {
            // Since the location is within the sprite, root.pick() won't search below this
            // sprite in the scene graph
            Group root = getRoot();
            return (root == null || root.pickEnabledAndVisible(viewX, viewY) == this);
        }
        else {
            return false;
        }
    }
    
    /**
        Checks if the specified sprite intersects this sprite. This method checks if this
        sprite's OBB (oriented bounding box) intersects with the specified sprite's OBB.
        The OBBs can be parallelograms in some cases.
        <p>
        The two sprites do no have to be in the same Group.
        @param sprite the sprite to test against.
        @return true if the two sprites' OBBs intersect.
    */
    public boolean intersects(Sprite sprite) {
        Sprite a = this;
        Sprite b = sprite;
        Transform at = a.getViewTransform();
        Transform bt = b.getViewTransform();
        int aw = a.getNaturalWidth();
        int ah = a.getNaturalHeight();
        int bw = b.getNaturalWidth();
        int bh = b.getNaturalHeight();
        boolean pixelLevel = a.getPixelLevelChecks() || b.getPixelLevelChecks();
        
        // First, test the bounding box of the two sprites
        Rect ab = at.getBounds(aw, ah);
        Rect bb = bt.getBounds(bw, bh);
        if (!ab.intersects(bb)) {
            return false;
        }
        
        // If the transforms aren't rotated, no further tests are needed
        if ((at.getType() & Transform.TYPE_ROTATE) == 0 &&
            (bt.getType() & Transform.TYPE_ROTATE) == 0)
        {
            if (pixelLevel) {
                ab.intersection(bb);
                return isPixelLevelCollision(b, ab);
            }
            else {
                return true;
            }
        }
        
        // One or both sprites are rotated. Use the separating axis theorem on the two
        // sprite's OBB (which is actually a parallelogram)
      
        // Step 1: Get sprite A's points
        Tuple2i[] ap = {
            new Tuple2i(0, 0),
            new Tuple2i(aw, 0),
            new Tuple2i(aw, ah),
            new Tuple2i(0, ah)
        };
            
        // Step 2: Get sprite B's points and convert them to sprite A's local space
        Tuple2i[] bp = {
            new Tuple2i(0, 0),
            new Tuple2i(bw, 0),
            new Tuple2i(bw, bh),
            new Tuple2i(0, bh)
        };
        for (int i = 0; i < 4; i++) {
            Tuple2i p = bp[i];
            bt.transform(p);
            boolean success = at.inverseTransform(p);
            if (!success) {
                return false;
            }
        }
        
        // Step 3: Get perpendiculars of each edge
        Tuple2i[] perps = { 
            new Tuple2i(ap[1].y - ap[0].y, ap[0].x - ap[1].x),
            new Tuple2i(ap[3].y - ap[0].y, ap[0].x - ap[3].x),
            new Tuple2i(bp[1].y - bp[0].y, bp[0].x - bp[1].x),
            new Tuple2i(bp[3].y - bp[0].y, bp[0].x - bp[3].x)
        };
        long[] perpLengths = {
            aw,
            ah,
            perps[2].length(),
            perps[3].length()
        };
        
        // Step 4: Project points onto each perpendicular.
        // For each perpendicular, the span of projected points from sprite A must intersect 
        // the span of projected points from sprite B
        for (int i = 0; i < perps.length; i++) {
            long amin = Long.MAX_VALUE;
            long amax = Long.MIN_VALUE;
            long bmin = Long.MAX_VALUE;
            long bmax = Long.MIN_VALUE;
            long len = perpLengths[i];
            Tuple2i p = perps[i];
            
            if (len <= 0) {
                return false;
            }
            for (int j = 0; j < ap.length; j++) {
                long v = CoreMath.div(p.dot(ap[j]), len);
                if (v < amin) {
                    amin = v;
                }
                if (v > amax) {
                    amax = v;
                }
            }
            for (int j = 0; j < bp.length; j++) {
                long v = CoreMath.div(p.dot(bp[j]), len);
                if (v < bmin) {
                    bmin = v;
                }
                if (v > bmax) {
                    bmax = v;
                }
            }
            if (amax < bmin || amin > bmax) {
                return false;
            }
        }
        
        if (pixelLevel) {
            // TODO: better intersection bounds for rotated sprites?
            ab.intersection(bb);
            return isPixelLevelCollision(b, ab);
        }
        else {
            return true;
        }
    }
    
    private boolean isPixelLevelCollision(Sprite sprite, Rect intersection) {
        Sprite a = this;
        Sprite b = sprite;
        int x1 = CoreMath.toIntFloor(intersection.x);
        int y1 = CoreMath.toIntFloor(intersection.y);
        int x2 = CoreMath.toIntCeil(intersection.x + intersection.width);
        int y2 = CoreMath.toIntCeil(intersection.y + intersection.height);
        
        for (int py = y1; py < y2; py++) {
            for (int px = x1; px < x2; px++) {
                if (a.contains(px, py) && b.contains(px, py)) {
                    return true;
                }
            }
        }
        return false;
    }
            
    /**
        Checks if this Sprite (and its parents) are enabled, and
        the mouse is currently within the bounds of this Sprite.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite.
    */
    public boolean isMouseOver() {
        return Input.isMouseInside() && isEnabled() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the mouse is within the bounds of this Sprite, and the primary mouse
        button is not pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is not pressed down.
    */
    public boolean isMouseHover() {
        return Input.isMouseInside() && !Input.isMouseDown() && isEnabled() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the mouse is currently within the bounds of this Sprite, and the primary mouse
        button is pressed down.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the mouse is currently within the bounds of this Sprite and 
        the primary mouse button is pressed down.
    */
    public boolean isMouseDown() {
        return Input.isMouseInside() && Input.isMouseDown() && isEnabled() && 
            contains(Input.getMouseX(), Input.getMouseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was pressed since the last update, and the press 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was pressed since the last update and the press 
        occurred within this Sprite's bounds.
    */
    public boolean isMousePressed() {
        return Input.isMouseInside() && Input.isMousePressed() && isEnabled() && 
            contains(Input.getMousePressX(), Input.getMousePressY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was released since the last update, and the release 
        occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was released since the last update and the release 
        occurred within this Sprite's bounds.
    */
    public boolean isMouseReleased() {
        return Input.isMouseInside() && Input.isMouseReleased() && isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was double-clicked since the last update, and the
        double-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was double-clicked since the last update and the 
        double-click occurred within this Sprite's bounds.
    */
    public boolean isMouseDoubleClicked() {
        return Input.isMouseInside() && Input.isPressed(Input.KEY_DOUBLE_MOUSE_BUTTON_1) && 
            isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled, 
        the primary mouse button was triple-clicked since the last update, and the
        triple-click occurred within this Sprite's bounds.
        <p>For a typical button UI button behavior, use {@link Button}.
        @return true if the primary mouse button was triple-clicked since the last update and the 
        triple-click occurred within this Sprite's bounds.
    */
    public boolean isMouseTripleClicked() {
        return Input.isMouseInside() && Input.isPressed(Input.KEY_TRIPLE_MOUSE_BUTTON_1) && 
            isEnabled() && 
            contains(Input.getMouseReleaseX(), Input.getMouseReleaseY());
    }
    
    /**
        Checks if this Sprite (and its parents) are enabled and
        the mouse wheel was rotated over this Sprite.
        @return true if the mouse wheel was rotated over this sprite since the  
        last rendering frame.
    */
    public boolean isMouseWheelRotated() {
        return Input.isMouseInside() && Input.getMouseWheelRotation() != 0 && isEnabled() && 
            contains(Input.getMouseWheelX(), Input.getMouseWheelY());
    }
    
// CONVENIENCE METHODS

    /**
        Sets the location of this Sprite.
    */
    public void setLocation(int x, int y) {
        this.x.set(x);
        this.y.set(y);
    }
    
    /**
        Sets the location of this Sprite.
    */
    public void setLocation(double x, double y) {
        this.x.set(x);
        this.y.set(y);
    }    
    
    /**
        Translates the location of this Sprite.
    */
    public void translate(int x, int y) {
        this.x.set(this.x.get() + x);
        this.y.set(this.y.get() + y);
    }
    
    /**
        Translates the location of this Sprite.
    */
    public void translate(double x, double y) {
        this.x.set(this.x.get() + x);
        this.y.set(this.y.get() + y);
    }    
    
    /**
        Sets the size of this Sprite.
        Changing the size is non-destructive - for example, an ImageSprite 
        doesn't internally scale it's image when this method is called. 
        Instead, an ImageSprite uses appropriate
        CoreGraphics methods to draw a scaled version of its image.
    */
    public void setSize(int width, int height) {
        this.width.set(width);
        this.height.set(height);
    }    
    
    /**
        Sets the size of this Sprite.
        Changing the size is non-destructive - for example, an ImageSprite 
        doesn't internally scale it's image when this method is called. 
        Instead, an ImageSprite uses appropriate
        CoreGraphics methods to draw a scaled version of its image.
    */
    public void setSize(double width, double height) {
        this.width.set(width);
        this.height.set(height);
    }
    
    //
    // Bind convenience methods
    //
    
    /**
        Binds this sprite's location to that of the specified sprite.
    */
    public void bindLocationTo(Sprite sprite) {
        x.bindTo(sprite.x);
        y.bindTo(sprite.y);
    }
    
    /**
        Binds this sprite's size to that of the specified sprite.
    */
    public void bindSizeTo(Sprite sprite) {
        width.bindTo(sprite.width);
        height.bindTo(sprite.height);
    }
    
    //
    // Move as int convenience methods
    //
    
    public void move(int startX, int startY, int endX, int endY, int duration) {
        x.animate(startX, endX, duration);
        y.animate(startY, endY, duration);
    }
    
    public void move(int startX, int startY, int endX, int endY, int duration, Easing easing) {
        x.animate(startX, endX, duration, easing);
        y.animate(startY, endY, duration, easing);
    }
    
    public void move(int startX, int startY, int endX, int endY, int duration, Easing easing,
        int startDelay)
    {
        x.animate(startX, endX, duration, easing, startDelay);
        y.animate(startY, endY, duration, easing, startDelay);
    }
    
    public void moveTo(int x, int y, int duration) {
        this.x.animateTo(x, duration);
        this.y.animateTo(y, duration);
    }
    
    public void moveTo(int x, int y, int duration, Easing easing) {
        this.x.animateTo(x, duration, easing);
        this.y.animateTo(y, duration, easing);
    }
    
    public void moveTo(int x, int y, int duration, Easing easing, int startDelay) {
        this.x.animateTo(x, duration, easing, startDelay);
        this.y.animateTo(y, duration, easing, startDelay);
    }
    
    //
    // Move as double convenience methods
    //
    
    public void move(double startX, double startY, double endX, double endY, int duration) {
        x.animate(startX, endX, duration);
        y.animate(startY, endY, duration);
    }
    
    public void move(double startX, double startY, double endX, double endY, int duration,
        Easing easing) 
    {
        x.animate(startX, endX, duration, easing);
        y.animate(startY, endY, duration, easing);
    }
    
    public void move(double startX, double startY, double endX, double endY, int duration,
        Easing easing, int startDelay)
    {
        x.animate(startX, endX, duration, easing, startDelay);
        y.animate(startY, endY, duration, easing, startDelay);
    }
    
    public void moveTo(double x, double y, int duration) {
        this.x.animateTo(x, duration);
        this.y.animateTo(y, duration);
    }
    
    public void moveTo(double x, double y, int duration, Easing easing) {
        this.x.animateTo(x, duration, easing);
        this.y.animateTo(y, duration, easing);
    }
    
    public void moveTo(double x, double y, int duration, Easing easing, int startDelay) {
        this.x.animateTo(x, duration, easing, startDelay);
        this.y.animateTo(y, duration, easing, startDelay);
    }
    
    //
    // Scale as int convenience methods
    //
    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration) 
    {
        width.animate(width1, width2, duration);
        height.animate(height1, height2, duration);
    }
    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration, Easing easing) 
    {
        width.animate(width1, width2, duration, easing);
        height.animate(height1, height2, duration, easing);
    }
    
    public void scale(int width1, int height1, int width2, int height2, 
        int duration, Easing easing, int startDelay)
    {
        width.animate(width1, width2, duration, easing, startDelay);
        height.animate(height1, height2, duration, easing, startDelay);
    }
    
    public void scaleTo(int width, int height, int duration) {
        this.width.animateTo(width, duration);
        this.height.animateTo(height, duration);
    }
    
    public void scaleTo(int width, int height, int duration, Easing easing) {
        this.width.animateTo(width, duration, easing);
        this.height.animateTo(height, duration, easing);
    }    
    
    public void scaleTo(int width, int height, int duration, Easing easing, 
        int startDelay) 
    {
        this.width.animateTo(width, duration, easing, startDelay);
        this.height.animateTo(height, duration, easing, startDelay);
    }    
    
    //
    // Scale as double convenience methods
    //
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration) 
    {
        width.animate(width1, width2, duration);
        height.animate(height1, height2, duration);
    }
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration, Easing easing) 
    {
        width.animate(width1, width2, duration, easing);
        height.animate(height1, height2, duration, easing);
    }
    
    public void scale(double width1, double height1, double width2, double height2, 
        int duration, Easing easing, int startDelay)
    {
        width.animate(width1, width2, duration, easing, startDelay);
        height.animate(height1, height2, duration, easing, startDelay);
    }
    
    public void scaleTo(double width, double height, int duration) {
        this.width.animateTo(width, duration);
        this.height.animateTo(height, duration);
    }
    
    public void scaleTo(double width, double height, int duration, Easing easing) {
        this.width.animateTo(width, duration, easing);
        this.height.animateTo(height, duration, easing);
    }    
    
    public void scaleTo(double width, double height, int duration, Easing easing, 
        int startDelay) 
    {
        this.width.animateTo(width, duration, easing, startDelay);
        this.height.animateTo(height, duration, easing, startDelay);
    }
}