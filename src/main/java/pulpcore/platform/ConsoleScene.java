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

import java.util.Iterator;
import java.util.LinkedList;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.ScrollPane;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import pulpcore.util.StringUtil;

public class ConsoleScene extends Scene2D {
    
    private String lastLine;
    private Button backButton;
    private Button clearButton;
    private Button copyButton;
    private ScrollPane textbox;
    private int lineSpacing;
    
    public void load() {

        // Load lazily so Stage doesn't have to load the system font on <init>
        lineSpacing = CoreFont.getSystemFont().getHeight() + 2;
        
        add(new FilledSprite(Colors.WHITE));
        
        backButton = Button.createLabeledButton("OK", Stage.getWidth() - 5, Stage.getHeight() - 5);
        backButton.setAnchor(Sprite.SOUTH_EAST);
        backButton.setKeyBinding(new int[] { Input.KEY_ESCAPE, Input.KEY_ENTER });
        
        clearButton = Button.createLabeledButton("Clear", 5, Stage.getHeight() - 5);
        clearButton.setAnchor(Sprite.SOUTH_WEST);
        
        copyButton = Button.createLabeledButton("Copy to Clipboard", 
            clearButton.x.getAsInt() + clearButton.width.getAsInt() + 5, Stage.getHeight() - 5);
        copyButton.setAnchor(Sprite.SOUTH_WEST);
        
        textbox = new ScrollPane(5, 5, Stage.getWidth() - 10, 
            Stage.getHeight() - 15 - clearButton.height.getAsInt());
        textbox.setScrollUnitSize(lineSpacing);
        textbox.setAnimationDuration(60, 250);
        refresh();
        textbox.scrollEnd();
        addLayer(textbox);
        
        add(clearButton);
        if (CoreSystem.isNativeClipboard()) {
            add(copyButton);
        }
        if (Stage.canPopScene()) {
            add(backButton);
        }
    }
    
    
    public void update(int elapsedTime) {
        
        if (needsRefresh()) {
            refresh();
        }
            
        if (clearButton.isClicked()) {
            CoreSystem.clearLog();
        }
        
        // Check button presses
        if (backButton.isClicked()) {
            if (Stage.canPopScene()) {
                Stage.popScene();
            }
        }
        if (copyButton.isClicked()) {
            CoreSystem.setClipboardText(CoreSystem.getLogText());
        }
    }
    
    private boolean needsRefresh() {
        LinkedList logLines = CoreSystem.getThisAppContext().getLogLines();
        Object line = null;
        if (logLines.size() > 0) {
            line = logLines.getLast();
        }
        return (lastLine != line);
    }
    
    private void refresh() {
        LinkedList logLines = CoreSystem.getThisAppContext().getLogLines();
        if (logLines.size() > 0) {
            lastLine = (String)logLines.getLast();
        }
        else {
            lastLine = null;
        }
        
        textbox.removeAll();
        int numLines = 0;
        int y = 0;
        int w = textbox.width.getAsInt() - ScrollPane.SCROLLBAR_WIDTH;
        Iterator i = logLines.iterator();
        while (i.hasNext()) {
            String[] text = StringUtil.wordWrap((String)i.next(), null, w);
            
            if (text.length == 0) {
                text = new String[] { " " };
            }
            for (int j = 0; j < text.length; j++) {
                String line = StringUtil.replace(text[j], "\t", "    ");
                textbox.add(new Label(line, 0, y));
                y += lineSpacing;
                numLines++;
            }
        }
    }
}
