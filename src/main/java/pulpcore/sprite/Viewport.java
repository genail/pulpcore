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

import pulpcore.animation.Bool;
import pulpcore.animation.Fixed;
import pulpcore.image.BlendMode;
import pulpcore.image.CoreGraphics;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.math.Tuple2i;

/**
    A {@code Viewport} is a {@code Group} whose contents can scroll, and the contents outside the 
    bounds of the {@code Group} are not visible.
*/
public class Viewport extends Group {
    
    /**
        The scroll x location, relative to the origin of this Viewport.
        Identical to {@code getContentPane().x}.
    */
    public final Fixed scrollX;
    
    /**
        The scroll y location, relative to the origin of this Viewport.
        Identical to {@code getContentPane().y}.
    */
    public final Fixed scrollY;
    
    /**
        The flag to indicate pixel snapping for scroll location. Initially set to {@code true}.
        Identical to {@code getContentPane().pixelSnapping}.
    */
    public final Bool scrollPixelSnapping;
    
    private final Group contents;
    
    public Viewport(int x, int y, int w, int h) {
        super(x, y, w, h);
        contents = new Group();
        scrollX = getContentPane().x;
        scrollY = getContentPane().y;
        scrollPixelSnapping = getContentPane().pixelSnapping;
        scrollPixelSnapping.set(true);
        super.add(contents);
    }
    
    public Group getContentPane() {
        return contents;
    }
    
    public int getContentWidth() {
        return Math.max(1, contents.width.getAsIntCeil());
    }
    
    public int getContentHeight() {
        return Math.max(1, contents.height.getAsIntCeil());
    }
    
    protected int getNaturalWidth() {
        return width.getAsFixed();
    }
    
    protected int getNaturalHeight() {
        return height.getAsFixed();
    }
    
    /**
        Returns {@code true}.
    */
    public boolean isOverflowClipped() {
        return true;
    }
    
    private void calcContentDimension() {
        Tuple2i contentDimension = getMaxLocation(contents);
        contents.width.set(contentDimension.x);
        contents.height.set(contentDimension.y);
    }
    
    private static Tuple2i getMaxLocation(Group group) {
        Tuple2i p = new Tuple2i(0, 0);
        Rect bounds = new Rect();
        for (int i = 0; i < group.size(); i++) {
            Sprite sprite = group.get(i);
            if (sprite instanceof Group) {
                Tuple2i subP = getMaxLocation((Group)sprite);
                Transform transform = new Transform();
                sprite.updateTransform(null, transform);
                transform.getBounds(CoreMath.toFixed(subP.x), CoreMath.toFixed(subP.y), bounds);
            }
            else if (sprite != null) {
                sprite.getRelativeBounds(bounds);
            }
            p.x = Math.max(p.x, bounds.x + bounds.width);
            p.y = Math.max(p.y, bounds.y + bounds.height);
        }
        return p;
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        // For now, recompute the content dimension every frame
        calcContentDimension();
        
        // If the conditions are right, set the clip. Otherwise use a back buffer.
        // This is because the software renderer can only clip rectangles in device space.
        Transform t = getDrawTransform();
        BlendMode b = getBackBufferBlendMode();
        if (b == BlendMode.SrcOver() && 
            (t.getType() == Transform.TYPE_IDENTITY ||
            t.getType() == Transform.TYPE_TRANSLATE))
        {
            removeBackBuffer();
        }
        else {
            createBackBuffer(b);
        }
    }
    
    /**
        Calls {@code getContentPane().add(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void add(Sprite sprite) {
        getContentPane().add(sprite);
        calcContentDimension();
    }

    /**
        Calls {@code getContentPane().add(index, sprite);}.
        <p>
        {@inheritDoc}
    */
    public void add(int index, Sprite sprite) {
        getContentPane().add(index, sprite);
        calcContentDimension();
    }
    
    /**
        Calls {@code getContentPane().remove(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void remove(Sprite sprite) {
        getContentPane().remove(sprite);
        calcContentDimension();
    }
    
    /**
        Calls {@code getContentPane().removeAll();}.
        <p>
        {@inheritDoc}
    */
    public void removeAll() {
        getContentPane().removeAll();
        calcContentDimension();
    }
    
    /**
        Calls {@code getContentPane().moveToTop(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void moveToTop(Sprite sprite) {
        getContentPane().moveToTop(sprite);
    }
    
    /**
        Calls {@code getContentPane().moveToBottom(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void moveToBottom(Sprite sprite) {
        getContentPane().moveToBottom(sprite);
    }
    
    /**
        Calls {@code getContentPane().moveUp(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void moveUp(Sprite sprite) {
        getContentPane().moveUp(sprite);
    }
    
    /**
        Calls {@code getContentPane().moveDown(sprite);}.
        <p>
        {@inheritDoc}
    */
    public void moveDown(Sprite sprite) {
        getContentPane().moveDown(sprite);
    }
}