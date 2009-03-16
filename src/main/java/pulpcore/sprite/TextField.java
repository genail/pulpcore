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

import pulpcore.animation.Color;
import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.Input;
import pulpcore.math.CoreMath;
import pulpcore.math.Transform;

/**
    The TextField is a Sprite that behaves like a common UI text input
    field. It handles selection, cut, copy, paste, keyboard traversal and mouse
    input. By default, a TextField's cursor is {@link pulpcore.Input#CURSOR_TEXT}.
    
    <!--
    <table>
        <tr><td><b>Action</b></td>
        <td><b>Mouse</b></td>
        <td><b>Keyboard (Windows)</b></td>
        <td><b>Keyboard (Mac)</b></td></tr>
        
        <tr><td>Move cursor</td>
        <td>click</td>
        <td>left, right, home, end</td>
        <td>left, right, command+left, command+right</td></tr>
        
        <tr><td>Select</td>
        <td>click and drag</td>
        <td>shift+left, shift+right, shift+home, shift+end</td></tr>
        <td>shift+left, shift+right, command+shift+left, command+shift+right</td></tr>
        
        <tr><td>Traverse word</td>
        <td></td>
        <td>ctrl+left, ctrl+right</td>
        <td>alt+left, alt+right</td></tr>
        
        <tr><td>Select word</td>
        <td>double-click</td>
        <td>ctrl+shift+left, ctrl+shift+right</td>
        <td>alt+shift+left, alt+shift+right</td></tr>
        
        <tr><td>Cut, copy, paste</td>
        <td></td>
        <td>ctrl+x, ctrl+c, ctrl+v</td>
        <td>command+x, command+c, command+v</td></tr>
        </table>
        Note: using the command key doesn't work on FireFox 2.0. Safari 2.0 is ok.
    -->
*/
public class TextField extends Sprite {
    
    private static final int DRAG_SCROLL_DELAY = 100;
    private static final int BLINK_TIME = 500;
    private static final int EXTRA_CARET_HEIGHT = 0;
    
    private static CoreFont systemSelectionFont;
    
    /** 
        The highlight color to use when text is selected.
        By default, the selection color is black. 
    */
    public final Color selectionColor = new Color(this, Colors.BLACK);
    
    /**
        The caret color. By default, the caret color is black.
    */
    public final Color caretColor = new Color(this, Colors.BLACK);

    private CoreFont font;
    private String text;
    private int timeUntilBlinkOff;
    private boolean hasFocus;
    private int caretPosition;
    private int scrollPosition;
    private int maxNumChars;
    
    // Note: selectionLength is negative for selections to the left of the cursor
    private int selectionLength;
    private CoreFont selectionFont;
    
    private boolean passwordMode;
    
    // For mouse dragging
    private boolean isMouseDragging;
    private int dragScrollDelay;
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(int x, int y, int w, int h) {
        this(null, null, "", x, y, w, h);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(double x, double y, double w, double h) {
        this(null, null, "", x, y, w, h);
    }
    
    public TextField(String text, int x, int y) {
        this(null, null, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(String text, int x, int y, int w, int h) {
        this(null, null, text, x, y, w, h);
    }
    
    public TextField(String text, double x, double y) {
        this(null, null, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(String text, double x, double y, double w, double h) {
        this(null, null, text, x, y, w, h);
    }
    
    public TextField(CoreFont font, CoreFont selectionFont, String text, int x, int y) {
        this(font, selectionFont, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(CoreFont font, CoreFont selectionFont, 
        String text, int x, int y, int w, int h) 
    {
        super(x, y, w, h);
        this.text = text;
        init(font, selectionFont, w < 0, h < 0);
    }
    
    public TextField(CoreFont font, CoreFont selectionFont, String text, double x, double y) {
        this(font, selectionFont, text, x, y, -1, -1);
    }
    
    /**
        If height < 0, the height is automatically set to fit the font
        height.
    */
    public TextField(CoreFont font, CoreFont selectionFont, 
        String text, double x, double y, double w, double h) 
    {
        super(x, y, w, h);
        this.text = text;
        init(font, selectionFont, w < 0, h < 0);
    }
    
    private void init(CoreFont font, CoreFont selectionFont, 
        boolean autoWidth, boolean autoHeight) 
    {
        if (font == null) {
            this.font = CoreFont.getSystemFont();
        }
        else {
            this.font = font;
        }
        
        if (selectionFont == null) {
            if (this.font == CoreFont.getSystemFont()) {
                if (systemSelectionFont == null) {
                    systemSelectionFont = this.font.tint(0xffffff);
                }
                this.selectionFont = systemSelectionFont;
            }
            else {
                this.selectionFont = this.font.tint(0xffffff);
            }
        }
        else {
            this.selectionFont = selectionFont;
        }
        
        if (autoWidth) {
            // Set the width to the width of the text plus an em-space.
            width.set(this.font.getStringWidth(this.text + "M"));
        }
        
        if (autoHeight) {
            height.set(this.font.getHeight() + EXTRA_CARET_HEIGHT * 2);
        }
        
        setMaxNumChars(256);
        
        caretPosition = text.length();
        timeUntilBlinkOff = BLINK_TIME;
        
        setCursor(Input.CURSOR_TEXT);
    }
    
    protected int getNaturalWidth() {
        return CoreMath.mulDiv(width.getAsFixed(), getNaturalHeight(), height.getAsFixed());
    }
    
    protected int getNaturalHeight() {
        return CoreMath.toFixed(font.getHeight() + EXTRA_CARET_HEIGHT * 2);
    }
    
    public boolean isPasswordMode() {
        return passwordMode;
    }
    
    public void setPasswordMode(boolean passwordMode) {
        if (this.passwordMode != passwordMode) {
            this.passwordMode = passwordMode;
            setDirty(true);
        }
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        if (text == null) {
            this.text = "";
        }
        else if (text.length() > maxNumChars) {
            this.text = text.substring(0, maxNumChars);
        }
        else {
            this.text = text;
        }
        
        if (caretPosition > this.text.length()) {
            setCaretPosition(this.text.length());
        }
        setSelectionLength(0);
    }
    
    private void setSelectionLength(int length) {
        if (selectionLength != length) {
            selectionLength = length;
            setDirty(true);
        }
    }
    
    public void setMaxNumChars(int maxNumChars) {
        this.maxNumChars = maxNumChars;
        // Make sure text isn't longer than maxNumChars
        setText(text);
    }
    
    public int getMaxNumChars() {
        return maxNumChars;
    }
    
    public boolean hasFocus() {
        return hasFocus;
    }
    
    public void setFocus(boolean hasFocus) {
        if (this.hasFocus != hasFocus) {
            this.hasFocus = hasFocus;
            setDirty(true);
        }
        timeUntilBlinkOff = BLINK_TIME;
    }
    
    public void setCaretPosition(int position) {
        setCaretPosition(position, false);
    }
    
    public void setCaretPosition(int position, boolean selectMode) {
        if (position < 0) {
            position = 0;
        }
        if (position > text.length()) {
            position = text.length();
        }
        int positionChange = position - caretPosition;
        caretPosition = position;
        
        if (selectMode) {
            setSelectionLength(selectionLength - positionChange);
        }
        else {
            setSelectionLength(0);
        }
        
        if (positionChange != 0 || timeUntilBlinkOff < 0) {
            setDirty(true);
        }
        timeUntilBlinkOff = BLINK_TIME;
        
        // update scroll position if needed
        if (caretPosition < scrollPosition) {
            // set scrollPosition so that cursor appears at 25% of width or less
            setScrollPositionPercent(25);
        }
        else if (caretPosition > 0) {
            String displayText = convertToDisplayText(text);
            int caretX = font.getStringWidth(displayText, scrollPosition, caretPosition);
            if (caretX >= CoreMath.toInt(getNaturalWidth())) {
                // set scrollPosition so that cursor appears at 75% of width
                setScrollPositionPercent(75);
            }
        }
    }
    
    private boolean isPositionVisible(int position) {
        if (position < scrollPosition) {
            return false;
        }
        else if (position > 0) {
            String displayText = convertToDisplayText(text);
            int x = font.getStringWidth(displayText, scrollPosition, position);
            if (x >= CoreMath.toInt(getNaturalWidth())) {
                return false;
            }
        }
        return true;
    }
    
    private void setScrollPositionPercent(int percent) {
        int goalWidth = CoreMath.toInt(getNaturalWidth()) * percent / 100;
        for (int i = caretPosition - 1; i >= 0; i--) {
            if (font.getStringWidth(text, i, caretPosition) >= goalWidth) {
                scrollPosition = i + 1;
                return;
            }
        }
        scrollPosition = 0;
    }
    
    public int getCaretPosition() {
        return caretPosition;
    }
    
    private int getCharPositionFromMouse() {
        double mouseClickX = getLocalX(Input.getMouseX(), Input.getMouseY());
        
        if (mouseClickX == Double.MAX_VALUE) {
            return 0;
        }
        
        String displayText = convertToDisplayText(text);
        
        if (mouseClickX < 0) {
            mouseClickX = -mouseClickX;
            int len = 0;
            for (int i = scrollPosition - 1; i >= 0; i--) {
                char ch = displayText.charAt(i);
                int charWidth = font.getCharWidth(ch);
                if (mouseClickX < len + charWidth/2) {
                    return i;
                }
                len = font.getStringWidth(text, i, scrollPosition);
            }
            return 0;
        }
        else {
            int len = 0;
            for (int i = scrollPosition; i < displayText.length(); i++) {
                char ch = displayText.charAt(i);
                int charWidth = font.getCharWidth(ch);
                if (mouseClickX < len + charWidth/2) {
                    return i;
                }
                len = font.getStringWidth(text, scrollPosition, i + 1);
            }
            return displayText.length();
        }
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        
        selectionColor.update(elapsedTime);
        caretColor.update(elapsedTime);
        
        if (isEnabledAndVisible()) {
            handleInput(elapsedTime);
            
            timeUntilBlinkOff -= elapsedTime;
            if (timeUntilBlinkOff < -BLINK_TIME * 2/3) {
                timeUntilBlinkOff = BLINK_TIME;
                if (hasFocus && selectionLength == 0) {
                    setDirty(true);
                }
            }
            else if (timeUntilBlinkOff <= 0 && timeUntilBlinkOff + elapsedTime > 0) {
                if (hasFocus && selectionLength == 0) {
                    setDirty(true);
                }
            }
        }
        else {
            timeUntilBlinkOff = BLINK_TIME;
        }
    }
    
    private int findPreviousWordStart(int position) {
        if (passwordMode) {
            return 0;
        }
        
        boolean wordFound = false;
        
        while (true) {
            position--;
            
            if (position <= 0) {
                return 0;
            }
            
            char ch = text.charAt(position);
            if (Character.isLetterOrDigit(ch)) {
                wordFound = true;
            }
            else if (wordFound) {
                return position + 1;
            }
        }
    }
    
    private int findNextWordStart(int position) {
        if (passwordMode) {
            return text.length();
        }
        
        boolean nonWordFound = false;
        
        while (true) {
            if (position >= text.length()) {
                return text.length();
            }
            
            char ch = text.charAt(position);
            if (!Character.isLetterOrDigit(ch)) {
                nonWordFound = true;
            }
            else if (nonWordFound) {
                return position;
            }
            
            position++;
        }
    }
    
    private void insertTextAtCaret(String newText) {
        // Only allow characters that can be displayed by the current font
        for (int i = 0; i < newText.length(); i++) {
            if (!font.canDisplay(newText.charAt(i))) {
                newText = newText.substring(0, i) + newText.substring(i + 1);
                i--;
            }
        }
        
        int textLength = text.length() - Math.abs(selectionLength);
        int lenToInsert = Math.min(newText.length(), maxNumChars - textLength);
        if (lenToInsert > 0) {
            deleteSelectedText();
            
            text = text.substring(0, caretPosition) + 
                newText.substring(0, lenToInsert) + 
                text.substring(caretPosition);
            setCaretPosition(caretPosition + lenToInsert);
        }
        timeUntilBlinkOff = BLINK_TIME;
        setDirty(true);
    }
    
    private void deleteSelectedText() {
        int selectionStart;
        int selectionEnd;
        
        if (selectionLength == 0) {
            return;
        }
        else if (selectionLength < 0) {
            selectionStart = caretPosition + selectionLength;
            selectionEnd = caretPosition;
        }
        else {
            selectionStart = caretPosition;
            selectionEnd = caretPosition + selectionLength;
        }
        
        text = text.substring(0, selectionStart) + text.substring(selectionEnd);
        setCaretPosition(selectionStart);
        setDirty(true);
    }
    
    private void copySelectionToClipboard() {
        if (selectionLength == 0) {
            return;
        }
        
        String text = convertToDisplayText(getSelectedText());
        CoreSystem.setClipboardText(text);
    }
    
    // Coverts text to stars ("*******") if password mode is enabled
    private String convertToDisplayText(String text) {
        if (!passwordMode) {
            return text;
        }
        
        StringBuffer buffer = new StringBuffer(text.length()); 
        for (int i = 0; i < text.length(); i++) {
            buffer.append('*');
        }
        return buffer.toString();
    }
    
    private String getSelectedText() {
        int selectionStart;
        int selectionEnd;
        
        if (selectionLength == 0) {
            return "";
        }
        else if (selectionLength < 0) {
            selectionStart = caretPosition + selectionLength;
            selectionEnd = caretPosition;
        }
        else {
            selectionStart = caretPosition;
            selectionEnd = caretPosition + selectionLength;
        }
        return text.substring(selectionStart, selectionEnd);
    }
    
    public void selectAll() {
        caretPosition = 0;
        setCaretPosition(text.length(), true);
    }
    
    private void selectWord(int position) {
        if (passwordMode) {
            selectAll();
            return;
        }
        
        if (text.length() == 0) {
            return;
        }
        
        if (position < 0) {
            position = 0;
        }
        if (position >= text.length()) {
            position = text.length() - 1;
        }
        
        boolean wordType = Character.isLetterOrDigit(text.charAt(position));
        
        // Find start
        int start = position - 1;
        while (start >= 0) {
            boolean charType = Character.isLetterOrDigit(text.charAt(start));
            if (wordType != charType) {
                break;
            }
            start--;
        }
        start++;
        
        // Find end
        int end = position + 1;
        while (end < text.length()) {
            boolean charType = Character.isLetterOrDigit(text.charAt(end));
            if (wordType != charType) {
                break;
            }
            end++;
        }
        
        setCaretPosition(start, false);
        setCaretPosition(end, true);
    }
    
    private void handleInput(int elapsedTime) {
        
        if (isMouseTripleClicked()) {
            selectAll();
            isMouseDragging = false;
        }
        else if (isMouseDoubleClicked()) {
            selectWord(getCharPositionFromMouse());
            isMouseDragging = false;
        }
        else if (isMousePressed()) {
            isMouseDragging = true;
            dragScrollDelay = DRAG_SCROLL_DELAY;
            setFocus(true);
            setCaretPosition(getCharPositionFromMouse());
        }
        else if (Input.isMousePressed()) {
            setFocus(false);
            isMouseDragging = false;
        }
        else if (Input.isMouseDown()) {
            if (isMouseDragging) {
                int pos = getCharPositionFromMouse();
                
                if (isPositionVisible(pos)) {
                    setCaretPosition(pos, true);
                }
                else {
                    dragScrollDelay -= elapsedTime;
                    if (dragScrollDelay <= 0) {
                        setCaretPosition(pos, true);
                        dragScrollDelay = DRAG_SCROLL_DELAY;
                    }
                }
            }
        }
        else {
            isMouseDragging = false;
        }
        
        if (!hasFocus) {
            return;
        }
        
        boolean traversalModifier = false;
        boolean clipboardModifier = false;
        boolean homeEndModifier = false;
        if (CoreSystem.isMacOSX()) {
            traversalModifier = Input.isAltDown();
            clipboardModifier = Input.isMetaDown();
            homeEndModifier = Input.isMetaDown();
        }
        else {
            traversalModifier = Input.isControlDown();
            clipboardModifier = Input.isControlDown();
            homeEndModifier = false;
        }
        
        if (Input.isTyped(Input.KEY_LEFT)) {
            int newPosition;
            if (homeEndModifier) {
                newPosition = 0;
            }
            else if (traversalModifier) {
                newPosition = findPreviousWordStart(caretPosition);
            }
            else {
                newPosition = caretPosition - 1;
            }
            setCaretPosition(newPosition, Input.isShiftDown());
        }
        if (Input.isTyped(Input.KEY_RIGHT)) {
            int newPosition;
            if (homeEndModifier) {
                newPosition = text.length();
            }
            else if (traversalModifier) {
                newPosition = findNextWordStart(caretPosition);
            }
            else {
                newPosition = caretPosition + 1;
            }
            setCaretPosition(newPosition, Input.isShiftDown());
        }
        if (Input.isPressed(Input.KEY_HOME)) {
            setCaretPosition(0, Input.isShiftDown());
        }
        if (Input.isPressed(Input.KEY_END)) {
            setCaretPosition(text.length(), Input.isShiftDown());
        }
        
        String textInput = Input.getTypedChars();
        
        if (textInput.length() > 0) {
            insertTextAtCaret(textInput);
        }
        else {
            if (Input.isTyped(Input.KEY_BACK_SPACE)) {
                if (selectionLength != 0) {
                    deleteSelectedText();
                }
                else if (caretPosition > 0) {
                    int newPosition;
                    if (traversalModifier) {
                        newPosition = findPreviousWordStart(caretPosition);
                    }
                    else {
                        newPosition = caretPosition - 1;
                    }
                    text = text.substring(0, newPosition) + text.substring(caretPosition);
                    setCaretPosition(caretPosition - 1);
                }
                timeUntilBlinkOff = BLINK_TIME;
                setDirty(true);
            }
            else if (Input.isTyped(Input.KEY_DELETE)) {
                if (selectionLength != 0) {
                    deleteSelectedText();
                }
                else if (caretPosition < text.length()) {
                    int newPosition;
                    if (traversalModifier) {
                        newPosition = findNextWordStart(caretPosition);
                    }
                    else {
                        newPosition = caretPosition + 1;
                    }
                    text = text.substring(0, caretPosition) + text.substring(newPosition);
                }
                timeUntilBlinkOff = BLINK_TIME;
                setDirty(true);
            }
            else if (clipboardModifier && Input.isPressed(Input.KEY_C)) {
                copySelectionToClipboard();
            }
            else if (clipboardModifier && Input.isPressed(Input.KEY_X)) {
                copySelectionToClipboard();
                deleteSelectedText();
            }
            else if (clipboardModifier && Input.isPressed(Input.KEY_V)) {
                insertTextAtCaret(CoreSystem.getClipboardText());
            }
        }
    }
    
    protected void drawSprite(CoreGraphics g) {

        boolean focusVisible = isEnabledAndVisible() && hasFocus && Input.hasKeyboardFocus();
        
        String text = convertToDisplayText(this.text);
        String displayText = text;
        
        if (scrollPosition > 0) {
            displayText = text.substring(scrollPosition);
        }
        
        Transform t = g.getTransform();
        int maxWidth = CoreMath.toInt(getNaturalWidth());
        int cx = 0;
        
        if (!focusVisible || selectionLength == 0) {
            // Render text with no selection
            g.setFont(font);
            //g.drawString(displayText, 0, EXTRA_CARET_HEIGHT);
            
            // Draw String with a local clip
            g.pushTransform();
            for (int i = 0; i < displayText.length(); i++) {
                int newCx = font.getCharPosition(displayText, 0, i);
                if (newCx >= maxWidth) {
                    break;
                }
                t.translate(CoreMath.toFixed(newCx - cx), 0);
                cx = newCx;
                    
                g.drawChar(displayText.charAt(i), maxWidth - cx);
            }
            g.popTransform();
            
            // Show Caret
            if (focusVisible && selectionLength == 0 && 
                timeUntilBlinkOff > 0 && caretPosition >= scrollPosition) 
            {
                int caretX = font.getStringWidth(text, scrollPosition, caretPosition);
                if (caretX >= 0 && caretX < maxWidth) {
                    g.setColor(caretColor.get());
                    g.fillRect(caretX, 0, 1, font.getHeight() + EXTRA_CARET_HEIGHT*2);
                }
            }
        }
        else {
            int selectionStart;
            int selectionEnd;
            if (selectionLength < 0) {
                selectionStart = caretPosition - scrollPosition + selectionLength;
                selectionEnd = caretPosition - scrollPosition;
            }
            else {
                selectionStart = caretPosition - scrollPosition;
                selectionEnd = caretPosition - scrollPosition + selectionLength;
            }
            
            // Render selection highlight
            selectionStart = Math.max(selectionStart, 0);
            int selX = font.getCharPosition(displayText, 0, selectionStart);
            int selWidth = selectionFont.getStringWidth(displayText, selectionStart, selectionEnd);
            g.setColor(selectionColor.get());
            g.fillRect(selX, 0, Math.min(selWidth, maxWidth-selX),
                selectionFont.getHeight() + EXTRA_CARET_HEIGHT*2);
            
            // Render text
            for (int i = 0; i < displayText.length(); i++) {
                char ch = displayText.charAt(i);
                
                // Assume the two fonts have the same string widths...
                int newCx = font.getCharPosition(displayText, 0, i);
                if (newCx >= maxWidth) {
                    break;
                }
                t.translate(CoreMath.toFixed(newCx - cx), 0);
                cx = newCx;
                    
                if (i >= selectionStart && i < selectionEnd) {
                    g.setFont(selectionFont);
                }
                else {
                    g.setFont(font);
                }
                
                g.drawChar(ch, maxWidth - cx);
            }
        }
    }
}