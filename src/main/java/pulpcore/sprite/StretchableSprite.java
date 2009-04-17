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

package pulpcore.sprite;

import java.util.ArrayList;
import java.util.List;
import pulpcore.animation.BindFunction;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;

/**
    Stretchable sprites are sprites that, when scaled, draw the "static" sections normally
    and scale the "stretchable" sections. Typically, corners are drawn normally while the inside
    is scaled. The format used is identical to
    <a href="http://code.google.com/android/reference/available-resources.html#ninepatch">Android's 
    nine-patch format</a>. 
    <p>
    Example:
    <pre>
    import pulpcore.image.Colors;
    import pulpcore.scene.Scene2D;
    import pulpcore.sprite.FilledSprite;
    import pulpcore.sprite.StretchableSprite;
    
    public class StretchableSpriteTest extends Scene2D {
        public void load() {
            add(new FilledSprite(Colors.BLUE));
            add(new StretchableSprite("button.9.png", 5, 5, 200, 200));
        }
    }
    </pre>
*/
public class StretchableSprite extends ImageSprite {
    
    private static class Section {
        public int position;
        public int length;
        public boolean stretchable;
        
        public Section(int position, int length, boolean stretchable) {
            this.position = position;
            this.length = length;
            this.stretchable = stretchable;
        }
    }
    
    /** The border size defining section and padding sizes */
    private final int borderSize;
    private Section[] topSections;
    private Section[] leftSections;
    private int fStretchableWidth;
    private int fStretchableHeight;
    private int paddingTop, paddingRight, paddingBottom, paddingLeft;
    private boolean autoParse = false;

    /**
     Creates a StretchableSprite from an image with defined stretchable sections.
     The format used is identical to
     <a href="http://code.google.com/android/reference/available-resources.html#ninepatch">Android's
     nine-patch format</a>
     */
    public StretchableSprite(String imageName, double x, double y) {
        this(CoreImage.load(imageName), x, y);
    }
    
    /**
     Creates a StretchableSprite from an image with defined stretchable sections.
     The format used is identical to
     <a href="http://code.google.com/android/reference/available-resources.html#ninepatch">Android's
     nine-patch format</a>
     */
    public StretchableSprite(String imageName, double x, double y, double w, double h) {
        this(CoreImage.load(imageName), x, y, w, h);
    }

    /**
     Creates a StretchableSprite from an image with defined stretchable sections.
     The format used is identical to
     <a href="http://code.google.com/android/reference/available-resources.html#ninepatch">Android's
     nine-patch format</a>
     */
    public StretchableSprite(CoreImage image, double x, double y) {
        this(image, x, y, image.getWidth() - 2, image.getHeight() - 2);
    }
    
    /**
     Creates a StretchableSprite from an image with defined stretchable sections.
     The format used is identical to
     <a href="http://code.google.com/android/reference/available-resources.html#ninepatch">Android's
     nine-patch format</a>
     */
    public StretchableSprite(CoreImage image, double x, double y, double w, double h) {
        super(image, x, y, w, h);
        autoParse = true;
        borderSize = 1;
        parseImage();
    }

    /**
     Creates a StretchableSprite with the specified cap size. The caps (in the four corners) are
     not stretched.
     */
    public StretchableSprite(String imageName, int capWidth, int capHeight,
            double x, double y, double w, double h)
    {
        this(CoreImage.load(imageName), capWidth, capHeight, x, y, w, h);
    }

    /**
     Creates a StretchableSprite with the specified cap size. The caps (in the four corners) are
     not stretched.
     */
    public StretchableSprite(CoreImage image, int capWidth, int capHeight,
            double x, double y, double w, double h)
    {
        super(image, x, y, w, h);
        borderSize = 0;
        setCapSize(capWidth, capHeight);
    }
    
    public void setImage(CoreImage image) {
        super.setImage(image);
        if (autoParse) {
            parseImage();
        }
        else if (topSections != null && leftSections != null) {
            setCapSize(topSections[0].length, leftSections[0].length);
        }
    }

    private void setCapSize(int capWidth, int capHeight) {
        int w = getImage().getWidth();
        int h = getImage().getHeight();
        if (capWidth*2 >= w) {
            capWidth = (w-1)/2;
        }
        if (capHeight*2 >= h) {
            capHeight = (h-1)/2;
        }
        topSections = new Section[3];
        topSections[0] = new Section(0, capWidth, false);
        topSections[1] = new Section(capWidth, w - capWidth*2, true);
        topSections[2] = new Section(w - capWidth, capWidth, false);
        leftSections = new Section[3];
        leftSections[0] = new Section(0, capHeight, false);
        leftSections[1] = new Section(capHeight, h - capHeight*2, true);
        leftSections[2] = new Section(h - capHeight, capHeight, false);
        
        fStretchableWidth = CoreMath.toFixed(w - capWidth*2);
        fStretchableHeight = CoreMath.toFixed(h - capHeight*2);
        paddingTop = capHeight;
        paddingRight = capWidth;
        paddingBottom = capHeight;
        paddingLeft = capWidth;
    }
    
    /**
        Creates a rendered version of this StretchableSprite at the current dimensions.
        The angle is ignored.
    */
    public CoreImage render() {
        return render(width.getAsIntCeil(), height.getAsIntCeil()); 
    }
    
    /**
        Creates a rendered version of this StretchableSprite with the specified dimensions.
        The angle is ignored.
    */
    public CoreImage render(int width, int height) {
        int fOldWidth = this.width.getAsFixed();
        int fOldHeight = this.height.getAsFixed();
        this.width.set(width);
        this.height.set(height);
        
        CoreImage dest = new CoreImage(width, height, false);
        CoreGraphics g = dest.createGraphics();
        
        // Scale
        int naturalWidth = getNaturalWidth();
        int naturalHeight = getNaturalHeight();
        if (naturalWidth > 0 && naturalHeight > 0 &&
            (naturalWidth != this.width.getAsFixed() || naturalHeight != this.height.getAsFixed())) 
        {
            int sx = CoreMath.div(this.width.getAsFixed(), naturalWidth);
            int sy = CoreMath.div(this.height.getAsFixed(), naturalHeight);
            g.getTransform().scale(sx, sy);
        }
        
        // Draw
        drawSprite(g);
        
        // Cleanup
        this.width.setAsFixed(fOldWidth);
        this.height.setAsFixed(fOldHeight);
        return dest;
    }
    
    public int getPaddingTop() {
        return paddingTop;
    }
    
    public int getPaddingRight() {
        return paddingRight;
    }
    
    public int getPaddingBottom() {
        return paddingBottom;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }
    
    private void parseImage() {
        CoreImage image = getImage();
        if (image != null) {
            int w = image.getWidth();
            int h = image.getHeight();
            
            topSections = parse(1, 0, 1, 0, w - 2);
            leftSections = parse(0, 1, 0, 1, h - 2);
            
            Section rightPaddingSection = first(parse(w - 1, 1, 0, 1, h - 2), true);
            if (rightPaddingSection == null) {
                rightPaddingSection = first(leftSections, true);
                if (rightPaddingSection == null) {
                    rightPaddingSection = new Section(0, h - 2, true);
                }
            }
            
            Section bottomPaddingSection = first(parse(1, h - 1, 1, 0, w - 2), true);
            if (bottomPaddingSection == null) {
                bottomPaddingSection = first(topSections, true);
                if (bottomPaddingSection == null) {
                    bottomPaddingSection = new Section(0, w - 2, true);
                }
            }
            
            paddingTop = rightPaddingSection.position;
            paddingBottom = h - 2 - paddingTop - rightPaddingSection.length;
            paddingLeft = bottomPaddingSection.position;
            paddingRight = w - 2 - paddingLeft - bottomPaddingSection.length;
            
            fStretchableWidth = CoreMath.toFixed(totalLength(topSections, true));
            fStretchableHeight = CoreMath.toFixed(totalLength(leftSections, true));
        }
    }
    
    private Section first(Section[] sections, boolean stretchable) {
        for (int i = 0; i < sections.length; i++) {
            if (sections[i].stretchable == stretchable) {
                return sections[i];
            }
        }
        return null;
    }
    
    private int totalLength(Section[] sections, boolean stretchable) {
        int length = 0;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i].stretchable == stretchable) {
                length += sections[i].length;
            }
        }
        return length;
    }
    
    private Section[] parse(int x, int y, int dx, int dy, int length) {
        CoreImage image = getImage();
        List sections = new ArrayList();
        Section currSection = null;
        for (int i = 0; i < length; i++) {
            boolean stretchable = (image.getARGB(x, y) == Colors.BLACK);
            if (currSection == null || currSection.stretchable != stretchable) {
                if (currSection != null) {
                    sections.add(currSection);
                }
                currSection = new Section(i, 0, stretchable);
            }
            currSection.length++;
            x += dx;
            y += dy;
        }
        if (currSection != null) {
            sections.add(currSection);
        }
        
        Section[] array = new Section[sections.size()];
        sections.toArray(array);
        return array;
    }
    
    /*
        All conversions happen in fixed-point.
        
        Three coordinate spaces:
        * Local: PulpCore's Local space for the sprite.
        * Image: The coordinate space for the image.
        * Natural: The local space before scaling (the corners are the same as Image space).
          Natural space will be identical to either Local space or Image space.
    */
    
    private int convert(Section[] sections, int localLength, int imageLength,
            int pos, int imageSretchableLength, boolean localToImage) 
    {
        if (localLength == imageLength) {
            return pos;
        }

        int imagePos = 0;
        int localPos = 0;
        int imageConstantLength = imageLength - imageSretchableLength;
        int localStetchableLength = localLength - imageConstantLength;
                
        for (int i = 0; i < sections.length; i++) {
            Section section = sections[i];
            int nextLocalPos = localPos;
            int nextImagePos = imagePos + CoreMath.toFixed(section.length);
            if (section.stretchable) {
                nextLocalPos += CoreMath.div(section.length * localStetchableLength, 
                        imageSretchableLength);
            }
            else {
                nextLocalPos += CoreMath.toFixed(section.length);
            }
            if (localToImage) {
                if (pos <= nextLocalPos) {
                    return CoreMath.mulDiv(pos - localPos, CoreMath.toFixed(section.length),
                            nextLocalPos - localPos) + imagePos;
                }
            }
            else {
                // imageToLocal
                if (pos <= nextImagePos) {
                    return CoreMath.mulDiv(pos - imagePos, nextLocalPos - localPos,
                            CoreMath.toFixed(section.length)) + localPos;
                }
            }
            localPos = nextLocalPos;
            imagePos = nextImagePos;
        }
        
        return imageLength;
    }
    
    private int convertLocalXtoImageX(int lx) {
        int lw = getNaturalWidth();
        int iw = getImageWidth();
        
        return convert(topSections, lw, iw, lx, fStretchableWidth, true);
    }
    
    private int convertLocalYtoImageY(int ly) {
        int lh = getNaturalHeight();
        int ih = getImageHeight();

        return convert(leftSections, lh, ih, ly, fStretchableHeight, true);
    }
        
    private int convertImageXtoLocalX(int ix) {
        int lw = getNaturalWidth();
        int iw = getImageWidth();
        
        return convert(topSections, lw, iw, ix, fStretchableWidth, false);
    }
    
    private int convertImageYtoLocalY(int iy) {
        int lh = getNaturalHeight();
        int ih = getImageHeight();
        
        return convert(leftSections, lh, ih, iy, fStretchableHeight, false);
    }
    
    //
    // Sprite implementation
    //
   
    public void setAnchorToHotSpot() {
        anchorX.bindTo(new BindFunction() {
            public Number f() {
                CoreImage image = getImage();
                if (image != null && topSections != null) {
                    int ax = CoreMath.toFixed(borderSize) +
                        convertImageXtoLocalX(CoreMath.toFixed(image.getHotspotX() - borderSize));
                    return new Double(CoreMath.toDouble(ax) / image.getWidth());
                }
                else {
                    return new Double(0);
                }
            }
        });
        anchorY.bindTo(new BindFunction() {
            public Number f() {
                CoreImage image = getImage();
                if (image != null && leftSections != null) {
                    int ay = CoreMath.toFixed(borderSize) +
                        convertImageYtoLocalY(CoreMath.toFixed(image.getHotspotY() - borderSize));
                    return new Double(CoreMath.toDouble(ay) / image.getHeight());
                }
                else {
                    return new Double(0);
                }
            }
        });
    }
    
    protected boolean isTransparent(int localX, int localY) {
        CoreImage image = getImage();
        if (getPixelLevelChecks() && image != null) {
            int imageX = borderSize + 
                CoreMath.toInt(convertLocalXtoImageX(CoreMath.toFixed(localX)));
            int imageY = borderSize + 
                CoreMath.toInt(convertLocalYtoImageY(CoreMath.toFixed(localY)));
            if (imageX <= borderSize || 
                imageY <= borderSize ||
                imageX >= image.getWidth() - borderSize ||
                imageY >= image.getHeight() - borderSize)
            {
                return true;
            }
            else {
                return image.isTransparent(imageX, imageY);
            }
        }
        else {
            return super.isTransparent(localX, localY);
        }
    }
    
    protected int getNaturalWidth() {
        return Math.max(getImageWidth(), width.getAsFixed());
    }
    
    protected int getNaturalHeight() {
        return Math.max(getImageHeight(), height.getAsFixed());
    }
    
    private int getImageWidth() {
        CoreImage image = getImage();
        return (image == null) ? 0 : CoreMath.toFixed(image.getWidth() - borderSize*2);
    }
    
    private int getImageHeight() {
        CoreImage image = getImage();
        return (image == null) ? 0 : CoreMath.toFixed(image.getHeight() - borderSize*2);
    }
    
    protected void drawSprite(CoreGraphics g) {
        CoreImage image = getImage();
        if (image != null) {
            drawSections(g, image);
        }
    }
    
    private void drawSections(CoreGraphics g, CoreImage image) {
        int imageConstantWidth = getImageWidth() - fStretchableWidth;
        int localStetchableWidth = getNaturalWidth() - imageConstantWidth;
        int imageConstantHeight = getImageHeight() - fStretchableHeight;
        int localStetchableHeight = getNaturalHeight() - imageConstantHeight;
        
        int fy = 0;
        for (int i = 0; i < leftSections.length; i++) {
            Section ySection = leftSections[i];
            int srcY = ySection.position + borderSize;
            int srcHeight = ySection.length;
            int scaleY;
            if (ySection.stretchable) {
                scaleY = CoreMath.div(localStetchableHeight, fStretchableHeight);
            }
            else {
                scaleY = CoreMath.ONE;
            }
            
            int fx = 0;
            for (int j = 0; j < topSections.length; j++) {
                Section xSection = topSections[j];
                int srcX = xSection.position + borderSize;
                int srcWidth = xSection.length;
                int scaleX;
                if (xSection.stretchable) {
                    scaleX = CoreMath.div(localStetchableWidth, fStretchableWidth);
                }
                else {
                    scaleX = CoreMath.ONE;
                }

                // Set edge clamp
                int clamp = antiAlias.get() ? 0 : CoreGraphics.EDGE_CLAMP_ALL;
                if (i > 0) {
                    clamp |= CoreGraphics.EDGE_CLAMP_TOP;
                }
                if (i < leftSections.length-1) {
                    clamp |= CoreGraphics.EDGE_CLAMP_BOTTOM;
                }
                if (j > 0) {
                    clamp |= CoreGraphics.EDGE_CLAMP_LEFT;
                }
                if (j < topSections.length-1) {
                    clamp |= CoreGraphics.EDGE_CLAMP_RIGHT;
                }
                g.setEdgeClamp(clamp);
                
                // Draw
                g.pushTransform();
                g.getTransform().translate(fx, fy); 
                if (xSection.stretchable || ySection.stretchable) {
                    g.getTransform().scale(scaleX, scaleY);
                }
                g.drawImage(image, srcX, srcY, srcWidth, srcHeight);
                g.popTransform();

                fx += scaleX * srcWidth;
            }
            
            fy += scaleY * srcHeight;
        }
    }
}