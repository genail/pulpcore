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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.scene.Scene;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.ScrollPane;
import pulpcore.Stage;
import pulpcore.math.CoreMath;
import pulpcore.sprite.Group;
import pulpcore.sprite.Sprite;

/**
    Provides an interface to switch to other Scenes. The Scene selector is activated by pressing 
    Ctrl-X in debug builds.
    <p>
    Other scenes can be switched to if its
    class is a public, non-abstract subclass of pulpcore.scene.Scene and it has
    a public no-argument constructor.
*/
public class SceneSelector extends Scene2D {
    
    private static List scenes;
    
    private Button cancel;
    private Button[] buttons;
    
    public void load() {
        
        if (scenes == null) {
            scenes = findSceneClasses();
        }
        
        add(new FilledSprite(Colors.WHITE));
        
        int spacing = 5;
        ScrollPane scrollPane = new ScrollPane(spacing, spacing, 
            Stage.getWidth() - spacing*2, Stage.getHeight() - spacing*2);
        
        if (Stage.canPopScene()) {
            cancel = Button.createLabeledButton("<< Back", 0, 0);
            scrollPane.add(cancel);
        }
        
        buttons = new Button[scenes.size()];
        for (int i = 0; i < scenes.size(); i++) {
            Class c = (Class)scenes.get(i);
            buttons[i] = Button.createLabeledButton(c.getName(), 0, 0);
            scrollPane.add(buttons[i]);
            
        }
        organizeInColumns(scrollPane.getContentPane(), 
                Stage.getWidth() - ScrollPane.SCROLLBAR_WIDTH, spacing);
        if (buttons.length > 0) {
            scrollPane.setScrollUnitSize(buttons[0].height.getAsInt() + spacing);
            scrollPane.setAnimationDuration(100, 250);
        }
        add(scrollPane);
    }
    
    public void update(int elapsedTime) {
        if (cancel != null && cancel.isClicked()) {
            Stage.popScene();
            return;
        }
        
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isClicked()) {
                
                Class c = (Class)scenes.get(i);
                
                try {
                    Scene scene = (Scene)c.newInstance();
                    Stage.setScene(scene);
                }
                catch (Throwable t) {
                    if (Build.DEBUG) CoreSystem.print("Couldn't create class " + c, t);
                }
            }
        }
    }

    private void organizeInColumns(Group group, int width, int spacing) {
        // Easy algorithm: all columns have the same width, assume all sprites have the
        // same height.
        int columnWidth = 0;
        for (int i = 0; i < group.size(); i++) {
            columnWidth = Math.max(columnWidth, group.get(i).width.getAsInt());
        }
        columnWidth += spacing;

        int numColumns = (width + spacing) / columnWidth;
        if (numColumns < 1) {
            numColumns = 1;
        }
        int spritesPerColumn = CoreMath.intDivCeil(group.size(), numColumns);
        int x = 0;
        int y = 0;
        for (int i = 0; i < group.size(); i++) {
            group.get(i).setLocation(x, y);
            if (((i + 1) % spritesPerColumn) == 0) {
                x += columnWidth;
                y = 0;
            }
            else {
                y += group.get(i).height.getAsInt() + spacing;
            }
        }
    }
    
    //
    // Private methods to find scenes
    //
    
    private List findSceneClasses() {
        List sceneClasses = new ArrayList();
        try {
            List jars = getJars();
            for (int i = 0; i < jars.size(); i++) {
                List classes = getClasses((String)jars.get(i));
                for (int j = 0; j < classes.size(); j++) {
                    String c = (String)classes.get(j);
                    
                    Class t = Class.forName(c);
                    if (isScene(t)) {
                        sceneClasses.add(t);
                    }
                }
            }
        }
        catch (Throwable t) {
            if (Build.DEBUG) CoreSystem.print("Couldn't get Scene list", t);
        }
        return sceneClasses;
    }
    
    /**
        Checks is a class is a public, non-abstract Scene with a public no-argument constructor.
    */
    private boolean isScene(Class c) {
        String name = c.getName();
        if (name.startsWith("pulpcore.")) {
            return false;
        }
        if (!Scene.class.isAssignableFrom(c)) {
            return false;
        }
        
        // Check if the class is public and not abstract
        int classModifiers = c.getModifiers();
        if ((classModifiers & Modifier.PUBLIC) == 0 ||
            (classModifiers & Modifier.ABSTRACT) != 0)
        {
            return false;
        }
        
        // Check if it has a public, no-argument constructor
        try {
            Constructor constructor = c.getDeclaredConstructor(new Class[] { });
            if ((constructor.getModifiers() & Modifier.PUBLIC) == 0) {
                return false;
            }
        }
        catch (Exception ex) {
            return false;
        }
        
        // All tests passed
        return true;
        
    }
    
    /**
        Converts a class filename into a Java class name.
    */
    private String getClassName(String filename) {
        if (filename.endsWith(".class")) {
            filename = filename.substring(0, filename.length() - 6);
        }
        if (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
        
        return filename.replace('/', '.');
    }
    
    /**
        Gets a list of all the classes inside a jar.
        (Assumes the jar is in the cache or local file system)
    */
    private List getClasses(String jar) throws Throwable {
        List classes = new ArrayList();
        
        URL url = new URL(CoreSystem.getBaseURL(), jar);
        ZipInputStream is = new ZipInputStream(url.openStream());
        
        while (true) {
            ZipEntry e = is.getNextEntry();
            if (e == null) {
                break;
            }
            String name = e.getName();
            if (name.endsWith(".class")) {
                classes.add(getClassName(name));
            }
        }
        
        is.close();
        
        return classes;
    }
    
    /**
        Gets a list of all the jars that are available to this class loader.
    */
    private List getJars() throws Throwable {
        String base = CoreSystem.getBaseURL().toString();
        
        ClassLoader classLoader = getClass().getClassLoader();
        
        if (classLoader instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader)classLoader).getURLs();
            
            List jars = new ArrayList();
            for (int i = 0; i < urls.length; i++) {
                String s = urls[i].toString();
                if (s.toLowerCase().endsWith(".jar") && s.startsWith(base)) {
                    jars.add(s.substring(base.length()));
                }
            }
            return jars;
        }
        else {
            if (Build.DEBUG) CoreSystem.print("Couldn't get Scene list");
            return new ArrayList();
        }
    }
}
