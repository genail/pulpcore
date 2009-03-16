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

package pulpcore.platform.applet;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Toolkit;
import pulpcore.Input;
import pulpcore.platform.PolledInput;

/**
    An input manager for Applets.
*/
public class AppletInput implements KeyListener, MouseListener,
    MouseMotionListener, MouseWheelListener, FocusListener
{
    private final Component comp;
    private final Cursor invisibleCursor;
    private final PolledInput polledInput = new PolledInput();
    
    private boolean[] keyPressed = new boolean[Input.NUM_KEY_CODES];
    private boolean[] keyDown = new boolean[Input.NUM_KEY_CODES];
    
    private int cursorCode;
    private int awtCursorCode;
    
    private int lastAppletMouseX = -1;
    private int lastAppletMouseY = -1;
    private int appletMouseX = -1;
    private int appletMouseY = -1;
    private int appletMousePressX = -1;
    private int appletMousePressY = -1;
    private int appletMouseReleaseX = -1;
    private int appletMouseReleaseY = -1;
    private int appletMouseWheelX = -1;
    private int appletMouseWheelY = -1;
    private int appletMouseWheel = 0;
    private boolean appletHasKeyboardFocus;
    private boolean appletIsMouseInside;
    private int focusCountdown;
    
    private StringBuffer typedCharsSinceLastPoll = new StringBuffer();
    
    public AppletInput(Component comp) {
        appletHasKeyboardFocus = false;
        cursorCode = Input.CURSOR_DEFAULT;
        
        this.comp = comp;
        comp.addKeyListener(this);
        comp.addMouseListener(this);
        comp.addMouseMotionListener(this);
        comp.addMouseWheelListener(this);
        comp.addFocusListener(this);
        comp.setFocusTraversalKeysEnabled(false);
        
        Dimension cursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(32, 32);
        if (cursorSize.width <= 0 || cursorSize.height <= 0) {
            // No cursor
            invisibleCursor = null;
        }
        else {
            BufferedImage cursorImage = new BufferedImage(
                cursorSize.width, cursorSize.height, BufferedImage.TYPE_INT_ARGB);
            
            invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImage, new Point(0, 0), "invisible");
        }
                  
        // This is a hack and probably won't work on all machines.
        // Firefox 2 + Windows XP + Java 5 + pulpcore.js appears to need a delay
        // before calling comp.requestFocus(). Calling it repeatedly does not focus
        // the component; there must be a delay.
        // A value of 10 tested fine; I've increased it here for slower machines.
        focusCountdown = 30;
    }
    
    /* package-private */ Component getComponent() {
        return comp;
    }
    
    /* package-private */ PolledInput getPolledInput() {
        return polledInput;
    }

    /* package-private */ void pollInput() {
        if (focusCountdown > 0) {
            if (appletHasKeyboardFocus) {
                focusCountdown = 0;
            }
            else {
                if (comp.getWidth() > 0 && comp.getHeight() > 0) {
                    focusCountdown--;
                    if (focusCountdown == 0) {
                        requestKeyboardFocus();
                    }
                }
            }
        }

        // Clear event queue
        // Backed-out for now since it caused deadlock with the PulpCore Player.
        // Chances are it could cause deadlock with Stencyl or another tool.
//        try {
//            EventQueue.invokeAndWait(this);
//        }
//        catch (Exception ex) { }

        synchronized (this) {

            for (int i = 0; i < Input.NUM_KEY_CODES; i++) {

                int oldState = polledInput.keyStates[i];

                if (keyPressed[i]) {
                    keyPressed[i] = false;
                    if (oldState == Input.RELEASED || oldState == Input.UP) {
                        polledInput.keyStates[i] = Input.PRESSED;
                    }
                    else {
                        polledInput.keyStates[i] = Input.REPEATED;
                    }
                }
                else if (keyDown[i]) {
                    if (oldState == Input.PRESSED || oldState == Input.REPEATED) {
                        polledInput.keyStates[i] = Input.DOWN;
                    }
                }
                else if (oldState == Input.PRESSED || oldState == Input.DOWN ||
                    oldState == Input.REPEATED)
                {
                    polledInput.keyStates[i] = Input.RELEASED;
                }
                else {
                    polledInput.keyStates[i] = Input.UP;
                }
            }

            polledInput.isMouseMoving = (appletMouseX != lastAppletMouseX ||
                appletMouseY != lastAppletMouseY);
            polledInput.mouse.x = appletMouseX;
            polledInput.mouse.y = appletMouseY;
            polledInput.mousePress.x = appletMousePressX;
            polledInput.mousePress.y = appletMousePressY;
            polledInput.mouseRelease.x = appletMouseReleaseX;
            polledInput.mouseRelease.y = appletMouseReleaseY;
            polledInput.mouseWheel.x = appletMouseWheelX;
            polledInput.mouseWheel.y = appletMouseWheelY;
            polledInput.mouseWheelRotation = appletMouseWheel;
            polledInput.hasKeyboardFocus = appletHasKeyboardFocus;
            polledInput.isMouseInside = appletIsMouseInside &&
                appletMouseX >= 0 && appletMouseY >= 0 &&
                appletMouseX < comp.getWidth() && appletMouseY < comp.getHeight();

            appletMouseWheel = 0;
            lastAppletMouseX = appletMouseX;
            lastAppletMouseY = appletMouseY;

            if (typedCharsSinceLastPoll.length() > 0) {
                polledInput.typedChars = typedCharsSinceLastPoll.toString();
                typedCharsSinceLastPoll = new StringBuffer();
            }
            else {
                polledInput.typedChars = "";
            }
        }
    }
    
    /* package-private */ void requestKeyboardFocus() {
        comp.requestFocus();
    }
    
    private synchronized void clear() {
        for (int i = 0; i < Input.NUM_KEY_CODES; i++) {
            keyPressed[i] = false;
            keyDown[i] = false;
        }
        
        typedCharsSinceLastPoll = new StringBuffer();
    }
    
    /* package-private */ void setCursor(int cursorCode) {
        
        int newAwtCursorCode = getAWTCursorCode(cursorCode);
        if (newAwtCursorCode == Cursor.CUSTOM_CURSOR && invisibleCursor == null) {
            newAwtCursorCode = Cursor.DEFAULT_CURSOR;
        }
        
        if (newAwtCursorCode == Cursor.DEFAULT_CURSOR) {
            cursorCode = Input.CURSOR_DEFAULT;
        }
        
        if (this.cursorCode != cursorCode || this.awtCursorCode != newAwtCursorCode) {
            if (newAwtCursorCode == Cursor.CUSTOM_CURSOR) {
                comp.setCursor(invisibleCursor);
            }
            else {
                comp.setCursor(Cursor.getPredefinedCursor(newAwtCursorCode));
            }
            this.awtCursorCode = newAwtCursorCode;
            this.cursorCode = cursorCode;
        }
    }
    
    /* package-private */ int getCursor() {
        return cursorCode;
    }
        
    private int getAWTCursorCode(int cursorCode) {
        switch (cursorCode) {
            default:               return Cursor.DEFAULT_CURSOR; 
            case Input.CURSOR_DEFAULT:   return Cursor.DEFAULT_CURSOR; 
            case Input.CURSOR_OFF:       return Cursor.CUSTOM_CURSOR; 
            case Input.CURSOR_HAND:      return Cursor.HAND_CURSOR; 
            case Input.CURSOR_CROSSHAIR: return Cursor.CROSSHAIR_CURSOR; 
            case Input.CURSOR_MOVE:      return Cursor.MOVE_CURSOR; 
            case Input.CURSOR_TEXT:      return Cursor.TEXT_CURSOR; 
            case Input.CURSOR_WAIT:      return Cursor.WAIT_CURSOR; 
            case Input.CURSOR_N_RESIZE:  return Cursor.N_RESIZE_CURSOR;
            case Input.CURSOR_S_RESIZE:  return Cursor.S_RESIZE_CURSOR;
            case Input.CURSOR_W_RESIZE:  return Cursor.W_RESIZE_CURSOR;
            case Input.CURSOR_E_RESIZE:  return Cursor.E_RESIZE_CURSOR;
            case Input.CURSOR_NW_RESIZE: return Cursor.NW_RESIZE_CURSOR;
            case Input.CURSOR_NE_RESIZE: return Cursor.NE_RESIZE_CURSOR;
            case Input.CURSOR_SW_RESIZE: return Cursor.SW_RESIZE_CURSOR;
            case Input.CURSOR_SE_RESIZE: return Cursor.SE_RESIZE_CURSOR;
        }
    }
    
    /**
        Translates the java.awt.event.KeyEvent to PulpCore's virtual key code.
    */
    private int getKeyCode(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE:   return Input.KEY_BACK_SPACE;
            case KeyEvent.VK_TAB:          return Input.KEY_TAB;
            case KeyEvent.VK_ENTER:        return Input.KEY_ENTER;
            case KeyEvent.VK_PAUSE:        return Input.KEY_PAUSE;
            case KeyEvent.VK_CAPS_LOCK:    return Input.KEY_CAPS_LOCK;
            case KeyEvent.VK_ESCAPE:       return Input.KEY_ESCAPE;
            case KeyEvent.VK_SPACE:        return Input.KEY_SPACE;
            case KeyEvent.VK_PAGE_UP:      return Input.KEY_PAGE_UP;
            case KeyEvent.VK_PAGE_DOWN:    return Input.KEY_PAGE_DOWN;
            case KeyEvent.VK_END:          return Input.KEY_END;
            case KeyEvent.VK_HOME:         return Input.KEY_HOME;
            case KeyEvent.VK_LEFT:         return Input.KEY_LEFT;
            case KeyEvent.VK_UP:           return Input.KEY_UP;
            case KeyEvent.VK_RIGHT:        return Input.KEY_RIGHT;
            case KeyEvent.VK_DOWN:         return Input.KEY_DOWN;
            case KeyEvent.VK_PRINTSCREEN:  return Input.KEY_PRINT_SCREEN;
            case KeyEvent.VK_INSERT:       return Input.KEY_INSERT;
            case KeyEvent.VK_DELETE:       return Input.KEY_DELETE;
            case KeyEvent.VK_SEMICOLON:    return Input.KEY_SEMICOLON;
            case KeyEvent.VK_EQUALS:       return Input.KEY_EQUALS;
            case KeyEvent.VK_COMMA:        return Input.KEY_COMMA;
            case KeyEvent.VK_MINUS:        return Input.KEY_MINUS;
            case KeyEvent.VK_PERIOD:       return Input.KEY_PERIOD;
            case KeyEvent.VK_SLASH:        return Input.KEY_SLASH;
            case KeyEvent.VK_BACK_QUOTE:   return Input.KEY_BACK_QUOTE;
            case KeyEvent.VK_OPEN_BRACKET: return Input.KEY_OPEN_BRACKET;
            case KeyEvent.VK_BACK_SLASH:   return Input.KEY_BACK_SLASH;
            case KeyEvent.VK_CLOSE_BRACKET:return Input.KEY_CLOSE_BRACKET;
            case KeyEvent.VK_QUOTE:        return Input.KEY_QUOTE;
            case KeyEvent.VK_0:            return Input.KEY_0;
            case KeyEvent.VK_1:            return Input.KEY_1;
            case KeyEvent.VK_2:            return Input.KEY_2;
            case KeyEvent.VK_3:            return Input.KEY_3;
            case KeyEvent.VK_4:            return Input.KEY_4;
            case KeyEvent.VK_5:            return Input.KEY_5;
            case KeyEvent.VK_6:            return Input.KEY_6;
            case KeyEvent.VK_7:            return Input.KEY_7;
            case KeyEvent.VK_8:            return Input.KEY_8;
            case KeyEvent.VK_9:            return Input.KEY_9;
            case KeyEvent.VK_A:            return Input.KEY_A;
            case KeyEvent.VK_B:            return Input.KEY_B;
            case KeyEvent.VK_C:            return Input.KEY_C;
            case KeyEvent.VK_D:            return Input.KEY_D;
            case KeyEvent.VK_E:            return Input.KEY_E;
            case KeyEvent.VK_F:            return Input.KEY_F;
            case KeyEvent.VK_G:            return Input.KEY_G;
            case KeyEvent.VK_H:            return Input.KEY_H;
            case KeyEvent.VK_I:            return Input.KEY_I;
            case KeyEvent.VK_J:            return Input.KEY_J;
            case KeyEvent.VK_K:            return Input.KEY_K;
            case KeyEvent.VK_L:            return Input.KEY_L;
            case KeyEvent.VK_M:            return Input.KEY_M;
            case KeyEvent.VK_N:            return Input.KEY_N;
            case KeyEvent.VK_O:            return Input.KEY_O;
            case KeyEvent.VK_P:            return Input.KEY_P;
            case KeyEvent.VK_Q:            return Input.KEY_Q;
            case KeyEvent.VK_R:            return Input.KEY_R;
            case KeyEvent.VK_S:            return Input.KEY_S;
            case KeyEvent.VK_T:            return Input.KEY_T;
            case KeyEvent.VK_U:            return Input.KEY_U;
            case KeyEvent.VK_V:            return Input.KEY_V;
            case KeyEvent.VK_W:            return Input.KEY_W;
            case KeyEvent.VK_X:            return Input.KEY_X;
            case KeyEvent.VK_Y:            return Input.KEY_Y;
            case KeyEvent.VK_Z:            return Input.KEY_Z;
            case KeyEvent.VK_NUMPAD0:      return Input.KEY_NUMPAD0;
            case KeyEvent.VK_NUMPAD1:      return Input.KEY_NUMPAD1;
            case KeyEvent.VK_NUMPAD2:      return Input.KEY_NUMPAD2;
            case KeyEvent.VK_NUMPAD3:      return Input.KEY_NUMPAD3;
            case KeyEvent.VK_NUMPAD4:      return Input.KEY_NUMPAD4;
            case KeyEvent.VK_NUMPAD5:      return Input.KEY_NUMPAD5;
            case KeyEvent.VK_NUMPAD6:      return Input.KEY_NUMPAD6;
            case KeyEvent.VK_NUMPAD7:      return Input.KEY_NUMPAD7;
            case KeyEvent.VK_NUMPAD8:      return Input.KEY_NUMPAD8;
            case KeyEvent.VK_NUMPAD9:      return Input.KEY_NUMPAD9;
            case KeyEvent.VK_MULTIPLY:     return Input.KEY_MULTIPLY;
            case KeyEvent.VK_ADD:          return Input.KEY_ADD;
            case KeyEvent.VK_SEPARATER:    return Input.KEY_SEPARATOR;
            case KeyEvent.VK_SUBTRACT:     return Input.KEY_SUBTRACT;
            case KeyEvent.VK_DECIMAL:      return Input.KEY_DECIMAL;
            case KeyEvent.VK_DIVIDE:       return Input.KEY_DIVIDE;
            case KeyEvent.VK_F1:           return Input.KEY_F1;
            case KeyEvent.VK_F2:           return Input.KEY_F2;
            case KeyEvent.VK_F3:           return Input.KEY_F3;
            case KeyEvent.VK_F4:           return Input.KEY_F4;
            case KeyEvent.VK_F5:           return Input.KEY_F5;
            case KeyEvent.VK_F6:           return Input.KEY_F6;
            case KeyEvent.VK_F7:           return Input.KEY_F7;
            case KeyEvent.VK_F8:           return Input.KEY_F8;
            case KeyEvent.VK_F9:           return Input.KEY_F9;
            case KeyEvent.VK_F10:          return Input.KEY_F10;
            case KeyEvent.VK_F11:          return Input.KEY_F11;
            case KeyEvent.VK_F12:          return Input.KEY_F12;
            case KeyEvent.VK_F13:          return Input.KEY_F13;
            case KeyEvent.VK_F14:          return Input.KEY_F14;
            case KeyEvent.VK_F15:          return Input.KEY_F15;
            case KeyEvent.VK_F16:          return Input.KEY_F16;
            case KeyEvent.VK_F17:          return Input.KEY_F17;
            case KeyEvent.VK_F18:          return Input.KEY_F18;
            case KeyEvent.VK_F19:          return Input.KEY_F19;
            case KeyEvent.VK_F20:          return Input.KEY_F20;
            case KeyEvent.VK_F21:          return Input.KEY_F21;
            case KeyEvent.VK_F22:          return Input.KEY_F22;
            case KeyEvent.VK_F23:          return Input.KEY_F23;
            case KeyEvent.VK_F24:          return Input.KEY_F24;
            case KeyEvent.VK_NUM_LOCK:     return Input.KEY_NUM_LOCK;
            case KeyEvent.VK_SCROLL_LOCK:  return Input.KEY_SCROLL_LOCK;
                
            case KeyEvent.VK_SHIFT:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_SHIFT;
                }
                else {
                    return Input.KEY_LEFT_SHIFT;
                }
                
            case KeyEvent.VK_CONTROL:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_CONTROL;
                }
                else {
                    return Input.KEY_LEFT_CONTROL;
                }
                
            case KeyEvent.VK_ALT:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_ALT;
                }
                else {
                    return Input.KEY_LEFT_ALT;
                }
            
            case KeyEvent.VK_META:
                if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                    return Input.KEY_RIGHT_META;
                }
                else {
                    return Input.KEY_LEFT_META;
                }
        }
        
        // Unknown key code
        return -1;
    }
    
    private int getKeyCode(MouseEvent e) {
        switch (e.getButton()) {
            default: case MouseEvent.BUTTON1:
                return Input.KEY_MOUSE_BUTTON_1;
            case MouseEvent.BUTTON2:
                return Input.KEY_MOUSE_BUTTON_2;
            case MouseEvent.BUTTON3:
                return Input.KEY_MOUSE_BUTTON_3;
        }
    }
    
    private synchronized void keyEvent(int keyCode, boolean pressed) {
        if (keyCode == -1) {
            return;
        }
        
        if (pressed) {
            keyPressed[keyCode] = true;
            keyDown[keyCode] = true;
        }
        else {
            keyDown[keyCode] = false;
        }
    }
    
    //
    // Event listener implementations
    //
    
    private void consume(KeyEvent e) {
        //int mask = KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK;
        //if ((e.getModifiersEx() & mask) == 0) {
        //    // Only consume if a no 'command' keys are down (ctrl, alt, meta)
        //    e.consume();
        //}
        e.consume();
    }
    
    public void keyPressed(KeyEvent e) {
        //pulpcore.CoreSystem.print("Press: " + e);
        int keyCode = getKeyCode(e);
        keyEvent(keyCode, true);
        
        consume(e);
        
        if (e.isMetaDown() && 
            !keyDown[Input.KEY_LEFT_META] && !keyDown[Input.KEY_RIGHT_META]) 
        {
            // Mac on Leopard won't send meta keys - do it here
            keyEvent(Input.KEY_LEFT_META, true);
            keyEvent(Input.KEY_LEFT_META, false);
        }
        
        if (keyCode == Input.KEY_LEFT_META || keyCode == Input.KEY_RIGHT_META) {
            // On Mac OS X, press and release events are not sent if the meta key is down
            // Release all keys
            for (int i = 0; i < Input.NUM_KEY_CODES; i++) {
                keyDown[i] = false;
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        //pulpcore.CoreSystem.print("Release: " + e);
        switch (e.getKeyCode()) {
            // Looks like a cross-platform AWT bug when both shift keys are down.
            // We need to release them both when either is released.
            case KeyEvent.VK_SHIFT:
                keyEvent(Input.KEY_LEFT_SHIFT, false);
                keyEvent(Input.KEY_RIGHT_SHIFT, false);
                break;
            case KeyEvent.VK_CONTROL:
                keyEvent(Input.KEY_LEFT_CONTROL, false);
                keyEvent(Input.KEY_RIGHT_CONTROL, false);
                break;
            case KeyEvent.VK_ALT:
                keyEvent(Input.KEY_LEFT_ALT, false);
                keyEvent(Input.KEY_RIGHT_ALT, false);
                break;
            default:
                keyEvent(getKeyCode(e), false);
                break;
        }
        consume(e);
    }

    public void keyTyped(KeyEvent e) {
        //pulpcore.CoreSystem.print("Type: " + e);
        if (e.isMetaDown()) {
            // On Mac OS X, press and release events are not sent if the meta key is down
            // Press and release this key
            int keyCode = getKeyCode(e);
            keyEvent(keyCode, true);
            keyEvent(keyCode, false);
        }
        else if (!e.isActionKey()) {
            synchronized (this) {
                // See this bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4435010
                char ch = e.getKeyChar();
                if (ch == KeyEvent.VK_BACK_SPACE) {
                    if (typedCharsSinceLastPoll.length() > 0) {
                        typedCharsSinceLastPoll.setLength(typedCharsSinceLastPoll.length() - 1);
                    }
                }
                else if (ch != KeyEvent.VK_DELETE && ch != KeyEvent.CHAR_UNDEFINED && 
                    !e.isActionKey() && ch >= ' ')
                { 
                    typedCharsSinceLastPoll.append(ch);
                }
            }
        }
        consume(e);
    }
    
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (appletHasKeyboardFocus == false) {
                comp.requestFocus();
            }
            keyEvent(getKeyCode(e), true);
        
            appletMouseX = e.getX();
            appletMouseY = e.getY();
            appletMousePressX = appletMouseX;
            appletMousePressY = appletMouseY;
            appletIsMouseInside = true;
        }
    }

    public void mouseReleased(MouseEvent e) {
        synchronized (this) {
            keyEvent(getKeyCode(e), false);
            
            appletMouseReleaseX = e.getX();
            appletMouseReleaseY = e.getY();
            appletIsMouseInside = true;
        } 
        
        // Attempt to fix mouse cursor bug. In Scared, the crosshair cursor was reverting to 
        // the default mouse cursor on mouse release.
        // Commented out because the bug is hard to duplicate. Perhaps it only occured on
        // Firefox on Mac OS X? And only "sometimes"?
        /*
        if (awtCursorCode != Cursor.DEFAULT_CURSOR) { 
            comp.setCursor(null);
            comp.setCursor(Cursor.getPredefinedCursor(awtCursorCode));
        }
        */
    }

    public void mouseClicked(MouseEvent e) {
        int clickCount = e.getClickCount();
        if (clickCount > 1) {
            // Detect double- and triple-clicks
            synchronized (this) {
                int keyCode;
                if (clickCount == 2) {
                     keyCode = Input.KEY_DOUBLE_MOUSE_BUTTON_1;
                }
                else {
                     keyCode = Input.KEY_TRIPLE_MOUSE_BUTTON_1;
                }
                
                switch (e.getButton()) {
                    case MouseEvent.BUTTON2: keyCode += 1; break;
                    case MouseEvent.BUTTON3: keyCode += 2; break;
                }
                
                keyPressed[keyCode] = true;
                keyDown[keyCode] = false;
                
                appletMouseReleaseX = e.getX();
                appletMouseReleaseY = e.getY();
                appletIsMouseInside = true;
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseExited(MouseEvent e) {
        synchronized (this) {
            mouseMoved(e);
            appletIsMouseInside = false;
        }
    }

    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseMoved(MouseEvent e) {
        synchronized (this) {
            appletMouseX = e.getX();
            appletMouseY = e.getY();
            appletIsMouseInside = true;
            /*
            boolean mouseInside = comp.contains(mouseX, mouseY);
            if (!mouseInside) {
                // Send Release events - the app doesn't receive release
                // events if the mouse is released outside the component.
                keyEvent(VK_MOUSE_BUTTON_1, false);
                keyEvent(VK_MOUSE_BUTTON_2, false);
                keyEvent(VK_MOUSE_BUTTON_3, false);
            }
            */
        }
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        synchronized (this) {
            appletMouseWheelX = e.getX();
            appletMouseWheelY = e.getY();
            
            int rotation = e.getWheelRotation();
            appletMouseWheel += (rotation < 0) ? -1 : (rotation > 0) ? 1 : 0;
            appletIsMouseInside = true;
        }
    }

    public void focusGained(FocusEvent e) {
        synchronized (this) {
            appletHasKeyboardFocus = true;
        }
    }
    
    public void focusLost(FocusEvent e) {
        synchronized (this) {
            appletHasKeyboardFocus = false;
            clear();
        }
    }
}