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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyListener;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.CoreImage;
import pulpcore.platform.Surface;
import pulpcore.scene.LoadingScene;
import pulpcore.scene.Scene;
import pulpcore.Stage;

/**
    CoreApplet is a Java 1.4-compatible Platform implementation.
*/
// CoreApplet is final so that developers don't override CoreApplet constructor, and call Stage or 
// some other class that isn't ready until after init()
public final class CoreApplet extends Applet {
    
    static {
        // Send a message to the Java Console
        System.out.println(
            "PulpCore " + Build.VERSION + " (" + Build.BUILD_DATE + ")");
    }
    
    private AppletAppContext context;
    
    private Color getColorParameter(String param) {
        String color = getParameter(param);
        if (color != null) {
            try {
                return Color.decode(color);
            }
            catch (NumberFormatException ex) { }
        }
        return null;
    }

    public final void init() {
        Color bgColor = getColorParameter("boxbgcolor");
        if (bgColor != null) {
            setBackground(bgColor);
        }
        Color fgColor = getColorParameter("boxfgcolor");
        if (fgColor != null) {
            setForeground(fgColor);
        }
        
        if (context != null) {
            context.stop();
        }
        context = (AppletAppContext)AppletPlatform.getInstance().registerApp(this);
    }
    
    public final void start() {
        if (context != null) {
            context.start();
        }
    }
    
    public final void stop() {
        if (context != null) {
            context.stop();
        }
    }
    
    public final void destroy() {
        if (context != null) {
            // Calls context.destroy()
            AppletPlatform.getInstance().unregisterApp(this);
            context = null;
        }
    }
    
    public final void update(Graphics g) {
        // App-triggered painting
        if (context == null) {
            // Do nothing
        }
        else {
            Surface surface = context.getSurface();
            if (surface instanceof BufferedImageSurface) {
                ((BufferedImageSurface)surface).draw(g);
            }
        }
    }
    
    public final void paint(Graphics g) {
        // System-triggered painting
        if (context == null) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        else {
            Surface surface = context.getSurface();
            surface.notifyOSRepaint();
        }
    }
    
    /**
        Creates a Scene object from the named "scene" applet parameter.
    */
    public Scene createFirstScene() {
        
        Stage.setAutoScale(getWidth(), getHeight());
        
        Scene firstScene;
        
        // Create the first scene
        String sceneName = getParameter("scene");
        if (sceneName == null || sceneName.length() == 0) {
            if (Build.DEBUG) CoreSystem.print("No defined scene.");
            return null;
        }
        try {
            Class c = Class.forName(sceneName);
            firstScene = (Scene)c.newInstance();
        }
        catch (Exception ex) {
            if (Build.DEBUG) CoreSystem.print("Could not create Scene: " + sceneName, ex);
            return null;
        }
        
        // Auto-load assets
        String assetsName = getParameter("assets");
        if (assetsName == null || assetsName.length() == 0) {
            return firstScene;
        }
        else {
            return new LoadingScene(assetsName, firstScene);
        }
    }
    
    /**
        Gets the current active scene. This method is provided for calls from JavaScript.
        {@code pulpcore_object.getCurrentScene().callMyMethod()}.
    */
    public Scene getCurrentScene() {
        final Scene[] scene = new Scene[1];
        invokeAndWait(new Runnable() {
            public void run() {
                scene[0] = Stage.getScene();
            }
        });
        return scene[0];
    }
    
    /**
        Causes {@code runnable} to have its {@code run} method called in the animation thread.
        This will happen immediately before calling {@link pulpcore.scene.Scene.updateScene(int)}.
        The runnable is not guaranteed to execute if the app is exited by the user.
    */
    public void invokeLater(Runnable runnable) {
        AppletAppContext c = context;
        if (c != null) {
            c.invokeLater(runnable);
        }
    }
    
    /**
        Causes {@code runnable} to have its {@code run} method called in the animation thread.
        This will happen immediately before calling {@link pulpcore.scene.Scene.updateScene(int)}.
        The runnable is not guaranteed to execute if the app is exited by the user.
    */
    public void invokeAndWait(Runnable runnable) {
        AppletAppContext c = context;
        if (c != null) {
            c.invokeAndWait(runnable);
        }
    }
    
    // For PulpCore Player
    
    private Component getInputComponent() {
        AppletAppContext c = context;
        if (c != null) {
            Component comp = c.getInputComponent();
            if (comp != null) {
                return comp;
            }
        }
        return this;
    }
    
    public void requestFocus() {
        Component comp = getInputComponent();
        if (comp == this) {
            super.requestFocus();
        }
        else {
            comp.requestFocus();
        }
    }
    
    public void removeKeyListener(KeyListener l) {
        Component comp = getInputComponent();
        if (comp == this) {
            super.removeKeyListener(l);
        }
        else {
            comp.removeKeyListener(l);
        }
    }
    
    public void addKeyListener(KeyListener l) {
        Component comp = getInputComponent();
        if (comp == this) {
            super.addKeyListener(l);
        }
        else {
            comp.addKeyListener(l);
        }
    }
    
    public KeyListener[] getKeyListeners() {
        Component comp = getInputComponent();
        if (comp == this) {
            return super.getKeyListeners();
        }
        else {
            return comp.getKeyListeners();
        }
    }
    
    // Called via PulpCore Player via reflection
    // This method must be called in the animation thread.
    private BufferedImage getScreenshot() {
        CoreImage image = Stage.getScreenshot();
        
        if (image != null) {
            int w = image.getWidth();
            int h = image.getHeight();
            int[] d = image.getData();
            BufferedImage awtImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            awtImage.setRGB(0, 0, w, h, d, 0, w);
            return awtImage;
        }
        else {
            return null;
        }
    }

    // Called via PulpCore Player via reflection
    // For IDEs
    private void setOut(PrintStream out) {
        AppletAppContext c = context;
        if (c != null) {
            c.setOut(out);
        }
    }
}