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

package pulpcore.scene;

import pulpcore.image.CoreGraphics;

/**
    A Scene is an object that updates the display and
    handles input from the user. All PulpCore apps will implement a Scene. 
    <p>A typical game will have several Scenes: for example, a title scene, 
    menu scene, main game scene, high score scene, help scene, etc.
    <p>For Applets, the first Scene is defined by the "scene" applet parameter:
    <pre>&lt;param name=&quot;scene&quot; value=&quot;MyFirstScene&quot; /&gt;</pre>
    <p>The {@link pulpcore.Stage} is responsible for invoking the 
    Scene's methods, with the exception of {@link #reload()}.
    
    @see pulpcore.Stage#setScene(Scene)
    @see pulpcore.Stage#replaceScene(Scene)
    @see pulpcore.Stage#pushScene(Scene)
    @see pulpcore.Stage#popScene()
*/
public abstract class Scene {

    /**
        Performs any actions needed to load this scene. By default, this 
        method does nothing. Typical implementations will load images and 
        sounds in this method.
    */
    public void load() { }

    /**
        Performs any actions needed to unload this scene. By default, this 
        method does nothing. This method should return as quickly as possible; if unloading 
        a scene requires a long computation, it should be done in a separate thread.
    */
    public void unload() { }
    
    /**
        Reloads the scene. This method calls {@code unload()} followed by {@code load()}.
    */
    public synchronized void reload() {
        unload();
        load();
    }
    
    /**
        Notifies that this scene has been shown after another Scene is hidden
        or immediately after a call to start(). Note, this method is not called 
        if the OS shows the app. By default, this method does nothing.
    */
    public void showNotify() { }
    
    /**
        Notifies that this scene has been hidden by another Scene or 
        immediately before a call to stop(). Note, this method is not called if 
        the OS hides the app. By default, this method does nothing.
    */
    public void hideNotify() { }
    
    /**
        Notifies that this scene that the Stage or the OS has requested a
        full redraw. By default, this method does nothing.
    */
    public void redrawNotify() { }
    
    /**
        Updates the scene. This method is periodically called by the
        {@link pulpcore.Stage} while this Scene is active. A scene will typically update
        sprites and handle input.
        <p>
        When a Scene is first shown (after a call to showNotify), the elapsedTime is zero.
        <p>
        The Stage starts a synchronized block on this Scene before calling this method and ends
        the block after {@link #drawScene(CoreGraphics) } returns.
        @param elapsedTime time, in milliseconds, since the last call to updateScene().
    */
    public abstract void updateScene(int elapsedTime);

    /**
        Draws to the surface's graphics context. The Stage calls this  
        method after calling {@link #updateScene(int) }.
        <p>
        The Stage starts a synchronized block on this Scene before calling 
        {@link #updateScene(int) } and ends the block after this method returns.
        @param g the CoreGraphics object to draw to. The CoreGraphics clip is 
        set to the entire display area.
    */
    public abstract void drawScene(CoreGraphics g);
    
}
