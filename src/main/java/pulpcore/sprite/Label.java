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

import java.lang.ref.WeakReference;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.CoreSystem;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.sprite.Group;
import pulpcore.util.StringUtil;

/**
    The Label is a Sprite that displays text. The text can be formatted
    using printf-style parameters 
    @see pulpcore.util.StringUtil
*/
public class Label extends Sprite {
    
    private String formatText;
    private String displayText;
    private Object[] formatArgs;
    private WeakListener formatArgListener;
    
    private int stringWidth;
    private CoreFont font;
    private boolean autoWidth;
    private boolean autoHeight;
    
    public final Int numDisplayChars = new Int(this);
    
    public Label(String text, int x, int y) {
        this(null, text, x, y, -1, -1);
    }
    
    public Label(String text, double x, double y) {
        this(null, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(String text, int x, int y, int w, int h) {
        this(null, text, x, y, w, h);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(String text, double x, double y, double w, double h) {
        this(null, text, x, y, w, h);
    }
    
    public Label(CoreFont font, String text, int x, int y) {
        this(font, text, x, y, -1, -1);
    }
    
    public Label(CoreFont font, String text, double x, double y) {
        this(font, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(CoreFont font, String text, int x, int y, int w, int h) {
        super(x, y, w, h);
        
        init(font, text, w < 0, h < 0);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public Label(CoreFont font, String text, double x, double y, double w, double h) {
        super(x, y, w, h);
        
        init(font, text, w < 0, h < 0); 
    }
    
    private void init(CoreFont font, String text, boolean autoWidth, boolean autoHeight) {
        if (font == null) {
            this.font = CoreFont.getSystemFont();
        }
        else {
            this.font = font;
        }
        
        this.autoWidth = autoWidth;
        this.autoHeight = autoHeight;
        
        setText(text);
    }
    
    public void propertyChange(Property p) {
        super.propertyChange(p);
        
        if (p == numDisplayChars || 
            (p == super.width && !autoWidth) || 
            (p == super.height && !autoHeight))
        {
            pack();
            return;
        }
        
        if (formatArgs != null) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (p == formatArgs[i]) {
                    format();
                    return;
                }
            }
        }
    }
    
    protected int getNaturalWidth() {
        int w = stringWidth;
        if (numDisplayChars.get() < displayText.length()) {
            w = font.getStringWidth(displayText, 0, numDisplayChars.get());
        }
        return CoreMath.toFixed(w);
    }
    
    protected int getNaturalHeight() {
        return CoreMath.toFixed(font.getHeight());
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        numDisplayChars.update(elapsedTime);
        
        CoreImage fontImage = font.getImage();
        fontImage.update(elapsedTime);
    }
    
    private void pack() {
        if (autoWidth) {
            setDirty(true);
            if (autoHeight) {
                width.setAsFixed(getNaturalWidth());
            }
            else {
                width.setAsFixed(CoreMath.mulDiv(getNaturalWidth(), height.getAsFixed(), 
                    getNaturalHeight()));
            }
        }
        if (autoHeight) {
            setDirty(true);
            if (autoWidth) {
                height.setAsFixed(getNaturalHeight());
            }
            else {
                height.setAsFixed(CoreMath.mulDiv(getNaturalHeight(), width.getAsFixed(), 
                    getNaturalWidth()));
            }
        }
    }
    
    /**
        @return this Label's font.
    */
    public CoreFont getFont() {
        return font;
    }
    
    /**
        Set this Label's font and resizes it if necessary.
    */
    public void setFont(CoreFont font) {
        this.font = font;
        format();
    }
    
    /**
        @return the formatted display text.
    */
    public String getText() {
        return displayText;
    }
    
    public void setText(String text) {
        this.formatText = text;
        format();
    }
    
    public void setFormatArg(Object arg) {
        setFormatArgs(new Object[] { arg });
    }
    
    public void setFormatArgs(Object[] args) {
        removeFormatArgListeners();
        formatArgs = CoreSystem.arraycopy(args);
        if (formatArgs != null) {
            if (formatArgListener == null) {
                formatArgListener = new WeakListener(this);
            }
            for (int i = 0; i < formatArgs.length; i++) {
                if (formatArgs[i] instanceof Property) {
                    Property p = (Property)formatArgs[i];
                    checkFormatArgListeners(p);
                    p.addListener(formatArgListener);
                }
            }
        }
        format();
    }
    
    private void checkFormatArgListeners(Property p) {
        PropertyListener[] listeners = p.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof WeakListener) {
                WeakListener l = (WeakListener)listeners[i];
                if (l.getListener() == null) {
                    p.removeListener(l);
                }
            }
        }
    }
    
    private void removeFormatArgListeners() {
        if (formatArgs != null && formatArgListener != null) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (formatArgs[i] instanceof Property) {
                    ((Property)formatArgs[i]).removeListener(formatArgListener);
                }
            }
        }
    }
    
    private void format() {
        displayText = StringUtil.format(formatText, formatArgs);
        if (displayText == null) {
            displayText = "null";
        }
        stringWidth = font.getStringWidth(displayText);
        numDisplayChars.set(displayText.length());
        setDirty(true);
        pack();
    }
    
    protected void drawSprite(CoreGraphics g) {
        String currDisplayText = displayText;
        if (numDisplayChars.get() < displayText.length()) {
            currDisplayText = displayText.substring(0, numDisplayChars.get());
        }
        
        drawText(g, currDisplayText);
    }
    
    protected void drawText(CoreGraphics g, String text) {
        g.setFont(getFont());
        g.drawString(text);
    }
    
    /**
        Weak listener to prevent memory leaks. (Many throw-away Labels listening to one Property)
    */
    private static class WeakListener implements PropertyListener {
        
        private WeakReference reference;
        
        public WeakListener(PropertyListener l) {
            reference = new WeakReference(l);
        }
        
        public PropertyListener getListener() {
            return (PropertyListener)reference.get();
        }
        
        public void propertyChange(Property p) {
            PropertyListener l = getListener();
            if (l == null) {
                // The Label was garbage collected.
                p.removeListener(this);
            }
            else {
                l.propertyChange(p);
            }
        }
    }
    
    /**
        Creates a Group of Labels with the system font. The newline ('\n') character can be used 
        to specify explicit line breaks.
    */
    public static Group createMultilineLabel(String text, int x, int y) {
        return createMultilineLabel(new Group(x, y), CoreFont.getSystemFont(),
            StringUtil.split(text, '\n'));
    }
    
    /**
        Creates a Group of Labels with the system font. Word-wrapping is used to confine the
        text to the specified viewWidth. The newline ('\n') character can be used to specify 
        explicit line breaks.
    */
    public static Group createMultilineLabel(String text, int x, int y, int viewWidth) {
        return createMultilineLabel(new Group(x, y), CoreFont.getSystemFont(),
            StringUtil.wordWrap(text, CoreFont.getSystemFont(), viewWidth));
    }         
    
    /**
        Creates a Group of Labels with the system font. The newline ('\n') character can be used 
        to specify explicit line breaks.
    */
    public static Group createMultilineLabel(String text, double x, double y) {
        return createMultilineLabel(new Group(x, y), CoreFont.getSystemFont(),
            StringUtil.split(text, '\n'));
    }                                 
    
    /**
        Creates a Group of Labels with the system font. Word-wrapping is used to confine the
        text to the specified viewWidth. The newline ('\n') character can be used to specify 
        explicit line breaks.
    */
    public static Group createMultilineLabel(String text, double x, double y, int viewWidth) {
        return createMultilineLabel(new Group(x, y), CoreFont.getSystemFont(),
            StringUtil.wordWrap(text, CoreFont.getSystemFont(), viewWidth));
    }
    
    /**
        Creates a Group of Labels with the specified Font. The newline ('\n') character can be used 
        to specify explicit line breaks.
    */
    public static Group createMultilineLabel(CoreFont font, String text, int x, int y) {
        return createMultilineLabel(new Group(x, y), font, StringUtil.split(text, '\n'));
    }
    
    /**
        Creates a Group of Labels with the specified Font. Word-wrapping is used to confine the
        text to the specified viewWidth. The newline ('\n') character can be used to specify 
        explicit line breaks.
    */
    public static Group createMultilineLabel(CoreFont font, String text, int x, int y, 
        int viewWidth) 
    {
        return createMultilineLabel(new Group(x, y), font,
            StringUtil.wordWrap(text, font, viewWidth));
    }
    
    /**
        Creates a Group of Labels with the specified Font. The newline ('\n') character can be used 
        to specify explicit line breaks.
    */
    public static Group createMultilineLabel(CoreFont font, String text, double x, double y) {
        return createMultilineLabel(new Group(x, y), font, StringUtil.split(text, '\n'));
    }
    
    /**
        Creates a Group of Labels with the specified Font. Word-wrapping is used to confine the
        text to the specified viewWidth. The newline ('\n') character can be used to specify 
        explicit line breaks.
    */
    public static Group createMultilineLabel(CoreFont font, String text, double x, double y, 
        int viewWidth) 
    {
        return createMultilineLabel(new Group(x, y), font,
            StringUtil.wordWrap(text, font, viewWidth));
    }
    
    private static Group createMultilineLabel(Group group, CoreFont font, String[] lines) {
        int lineSpacing = font.getHeight() + (font.getHeight() / 8);
        for (int i = 0; i < lines.length; i++) {
            group.add(new Label(font, lines[i], 0, i * lineSpacing));
        }
        group.pack();
        return group;
    }
}