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

package pulpcore;

import java.util.LinkedList;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Rect;
import pulpcore.math.Transform;
import pulpcore.math.Tuple2i;
import pulpcore.platform.AppContext;
import pulpcore.platform.ConsoleScene;
import pulpcore.platform.PolledInput;
import pulpcore.platform.SceneSelector;
import pulpcore.platform.Surface;
import pulpcore.scene.Scene;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;

/**
    The Stage class manages Scenes and drawing to the Surface. The Stage class is a 
    singleton that Scenes can access using its static methods.
    <p>
    Stage runs the animation loop. The main class (e.g. CoreApplet) creates, 
    starts, and stops the Stage.
*/
public class Stage implements Runnable {
    
    private static final float SLOW_MOTION_SPEED = 1/4f;
    private static final float FAST_MOTION_SPEED = 4f;
    
    private static final int DEBUG_COMMAND_SHOW_CONSOLE = 1;
    private static final int DEBUG_COMMAND_SHOW_SCENE_SELECTOR = 2;
    private static final int DEBUG_COMMAND_SHOW_SCENE_INFO = 4;
    private static final int DEBUG_COMMAND_SPEED_SLOW = 8;
    private static final int DEBUG_COMMAND_SPEED_NORMAL = 16;
    private static final int DEBUG_COMMAND_SPEED_FAST = 32;
    
    /** No limit to the frame rate (not recommended) */
    public static final int MAX_FPS = -1;
    /** 60 fps */
    public static final int HIGH_FPS = 60;
    /** 30 fps */
    public static final int MEDIUM_FPS = 30;
    /** 15 fps */
    public static final int LOW_FPS = 15;
    /** 60 fps, or the screen's refresh rate if it is between 55hz and 65hz. */
    public static final int DEFAULT_FPS = 0;

    private static final int DEFAULT_DEFAULT_FPS = HIGH_FPS;
    
    /** Perform no auto scaling (default) */
    public static final int AUTO_OFF = 0;
    
    /** Automatically center the Scene in the Stage. The Scene is not scaled. */
    public static final int AUTO_CENTER = 1;
    
    /** Automatically stretch the Scene to the Stage dimensions. */
    public static final int AUTO_STRETCH = 2;
    
    /** 
        Automatically scale the Scene to the Stage dimensions, preserving the Scene's 
        aspect ratio. 
    */
    public static final int AUTO_FIT = 3;
    
    private static final int NO_NEXT_SCENE = 0;
    private static final int PUSH_SCENE = 1;
    private static final int POP_SCENE = 2;
    private static final int SET_SCENE = 3;
    private static final int REPLACE_SCENE = 4;
    
    private AppContext appContext;
    
    // Dirty rectangles
    private Rect[] dirtyRectangles;
    private int numDirtyRectangles;
    private boolean renderingErrorOccurred = false;
    
    // Stage info
    private int desiredFPS = DEFAULT_FPS;
    private long frameRateDelay = 1000000L / DEFAULT_DEFAULT_FPS;
    private double actualFPS = -1;
    private long remainderMicros;
    private Thread animationThread;
    private final Surface surface;
    private boolean destroyed = false;
    
    // Scene management
    private Scene currentScene;
    private Scene nextScene;
    private int nextSceneType = NO_NEXT_SCENE;
    private LinkedList sceneStack = new LinkedList();
    private Scene uncaughtExceptionScene;
    private Runnable shutdownCode;
    
    // Auto scaling
    private int naturalWidth = 1;
    private int naturalHeight = 1;
    private int autoScaleType = AUTO_OFF;
    private Transform defaultTransform = new Transform();
    
    // Frame rate display (debug only)
    private boolean showInfoOverlay;
    private ImageSprite infoOverlay;
    private long overlayCreationTime;
    private int overlayFrames;
    private long overlaySleepTime;
    private Activity memActivity;
    private Activity cpuActivity;
    
    private int debugCommands;
    
    // Slow motion mode (debug only)
    private float speed = 1;
    private float elapsedTimeRemainder;
    
    // Called from the AWT thread
    public Stage(Surface surface, AppContext appContext) {
        this.surface = surface;
        this.appContext = appContext;
        if (Build.DEBUG) {
            this.uncaughtExceptionScene = new ConsoleScene();
        }
        else {
            this.uncaughtExceptionScene = null;
        }
    }
    
    //
    // Static convenience methods
    //
    
    private static Stage getThisStage() {
        return CoreSystem.getThisAppContext().getStage();
    }
    
    /**
        @return The width of the surface.
    */
    public static int getWidth() {
        Stage instance = getThisStage();
        if (instance.autoScaleType == AUTO_OFF) {
            return instance.surface.getWidth();
        }
        else {
            return instance.naturalWidth;
        }
    }
    
    /**
        @return The height of the surface.
    */
    public static int getHeight() {
        Stage instance = getThisStage();
        if (instance.autoScaleType == AUTO_OFF) {
            return instance.surface.getHeight();
        }
        else {
            return instance.naturalHeight;
        }
    }
    
    /**
        @return The transform used to draw onto the surface.
    */
    public static Transform getDefaultTransform() {
        Stage instance = getThisStage();
        return instance.defaultTransform;
    }
    
    public static void setAutoScale(int naturalWidth, int naturalHeight) {
        setAutoScale(naturalWidth, naturalHeight, AUTO_FIT);
    }
    
    public static void setAutoScale(int naturalWidth, int naturalHeight, int autoScaleType) {
        Stage instance = getThisStage();
        instance.naturalWidth = naturalWidth;
        instance.naturalHeight = naturalHeight;
        instance.autoScaleType = autoScaleType;
        instance.setTransform();
    }
    
    /**
        Sets the desired frame rate in frames per second. The Stage will attempt
        to get as close to the desired frame rate as possible, but the actual 
        frame rate may vary.
        <p>
        To run at the highest frame rate possible (no pauses between frames),
        invoke {@code setFrameRate(Stage.MAX_FPS)}. Note, however, 
        running at the highest frame rate possible usually means
        as many processor cycles are used as possible.
        
        @see #getFrameRate()
        @see #getActualFrameRate()
    */
    public static void setFrameRate(int desiredFPS) {
        if (DEFAULT_FPS == -1) {
            return;
        }
        
        if (desiredFPS < 0) {
            desiredFPS = MAX_FPS;
        }
        
        Stage instance = getThisStage();
        if (instance.desiredFPS != desiredFPS) {
            instance.desiredFPS = desiredFPS;
            instance.remainderMicros = 0;
        }
        if (desiredFPS == MAX_FPS) {
            instance.frameRateDelay = 0;
        }
        else {
            int fps = desiredFPS;
            if (fps == 0) {
                fps = getDefaultFrameRate();
            }
            instance.frameRateDelay = 1000000L / fps;
        }
    }
    
    /**
        Gets the current desired frame rate in frames per second. This is the same value passed
        in to {@link #setFrameRate(int)}.
        @see #setFrameRate(int)
        @see #getActualFrameRate()
    */
    public static int getFrameRate() {
        int fps = getThisStage().desiredFPS;
        if (fps == 0) {
            fps = getDefaultFrameRate();
        }
        return fps;
    }

    private static int getDefaultFrameRate() {
        int fps = CoreSystem.getThisAppContext().getRefreshRate();
        if (fps >= DEFAULT_DEFAULT_FPS - 5 && fps <= DEFAULT_DEFAULT_FPS + 5) {
            return fps;
        }
        else {
            return DEFAULT_DEFAULT_FPS;
        }
    }
    
    /**
        Gets the actual frame rate the Stage is displaying in frames per second. 
        The actual frame rate
        may vary from the desired frame rate. This method returns -1 if the frame rate has not
        yet been calculated. The actual frame rate is calculated periodically.
        @see #setFrameRate(int)
        @see #getFrameRate()
    */
    public static double getActualFrameRate() {
        return getThisStage().actualFPS;
    }
    
    public static void setDirtyRectangles(Rect[] dirtyRectangles) {
        if (dirtyRectangles == null) {
            setDirtyRectangles(null, 0);
        }
        else {
            setDirtyRectangles(dirtyRectangles, dirtyRectangles.length);
        }
    }
        
    public static void setDirtyRectangles(Rect[] dirtyRectangles, int numDirtyRectangles) {
        Stage instance = getThisStage();
        
        if (dirtyRectangles == null) {
            numDirtyRectangles = 0;
        }
        
        instance.dirtyRectangles = dirtyRectangles;
        instance.numDirtyRectangles = numDirtyRectangles;
    }
    
    /**
        Gets the current active Scene.
    */
    public static Scene getScene() {
        return getThisStage().currentScene;
    }
    
    /**
        Returns true if the there are Scenes on the scene stack.
    */
    public static boolean canPopScene() {
        Stage instance = getThisStage();
        return (!instance.sceneStack.isEmpty());
    }

    /**
        Unloads the current scene, empties the scene stack, and
        sets the next scene to display.
        <p>
        Any scenes on the scene stack are unloaded and the scene stack is emptied.
        <p>
        The new scene isn't activated until the next frame. Multiple calls to the
        {@link #setScene(Scene)}, {@link #replaceScene(Scene)}, {@link #pushScene(Scene)},
        and {@link #popScene()} 
        methods within a single frame will cause the 
        first calls to be ignored - only the last call during
        a single frame is recognized. 
    */
    public static void setScene(Scene scene) {
        Stage instance = getThisStage();
        instance.nextScene = scene;
        instance.nextSceneType = SET_SCENE;
    }
    
    /**
        Unloads the current scene and sets the next scene to display.
        <p>
        The scene stack is left untouched.
        <p>
        The new scene isn't activated until the next frame. Multiple calls to the
        {@link #setScene(Scene)}, {@link #replaceScene(Scene)}, {@link #pushScene(Scene)},
        and {@link #popScene()} 
        methods within a single frame will cause the 
        first calls to be ignored - only the last call during
        a single frame is recognized. 
    */
    public static void replaceScene(Scene scene) {
        Stage instance = getThisStage();
        instance.nextScene = scene;
        instance.nextSceneType = REPLACE_SCENE;
    }
    
    /**
        Pushes the current scene onto the scene stack and sets the current scene.
        The pushed scene is activated again when {@link #popScene()} is invoked. 
        <p>
        The new scene isn't activated until the next frame. Multiple calls to the
        {@link #setScene(Scene)}, {@link #replaceScene(Scene)}, {@link #pushScene(Scene)},
        and {@link #popScene()} 
        methods within a single frame will cause the 
        first calls to be ignored - only the last call during
        a single frame is recognized. 
    */
    public static void pushScene(Scene scene) {
        Stage instance = getThisStage();
        instance.nextScene = scene;
        instance.nextSceneType = PUSH_SCENE;
    }
    
    /**
        Sets the current scene to the scene at the top of the scene stack. If
        the scene stack is empty, this method does nothing. 
        <p>
        The popped scene isn't activated until the next frame. Multiple calls to the
        {@link #setScene(Scene)}, {@link #replaceScene(Scene)}, {@link #pushScene(Scene)},
        and {@link #popScene()} 
        methods within a single frame will cause the 
        first calls to be ignored - only the last call during
        a single frame is recognized. 
    */
    public static void popScene() {
        Stage instance = getThisStage();
        if (!instance.sceneStack.isEmpty()) {
            instance.nextScene = null;
            instance.nextSceneType = POP_SCENE;
        }
    }
    
    /**
        Sets the scene to use when an uncaught exception occurs. If null, the app "reboots" itself
        after a short delay. By default, debug builds use a ConsoleScene, and release builds use
        null.
        <p>
        When an uncaught exception occurs, the talkback field "pulpcore.uncaught-exception" is
        set to the exception's stack trace.
        <p>
        If the uncaught exception scene itself throws an exception, the app is stopped.
    */
    public static void setUncaughtExceptionScene(Scene scene) {
        Stage instance = getThisStage();
        instance.uncaughtExceptionScene = scene;
    }
    
    /**
        Sets the runnable to execute at application shutdown. Single-scene apps can overload the
        {@link Scene2D#unload()} method, but multi-scene apps that require shutdown code to be
        run (for example, logging off from a server) can use this method for convenience.
        <P>
        The {@code runnable} is executed in the animation thread after all scenes are
        unloaded.
    */
    public static void invokeOnShutdown(final Runnable runnable) {
        final Stage instance = getThisStage();
        instance.shutdownCode = runnable;
    }
    
    /**
        Gets a screenshot of the current appearance of the stage.
        @return a new image that contains the screenshot.
    */
    public static CoreImage getScreenshot() {
        Stage instance = getThisStage();
        CoreImage image = new CoreImage(instance.surface.getWidth(), instance.surface.getHeight());
        getScreenshot(image, 0, 0);
        return image;
    }
    
    /**
        Gets a screenshot at the specified location on the screen and copies
        it to the specified image.
    */
    public static void getScreenshot(CoreImage image, int x, int y) {
        Stage instance = getThisStage();
        instance.surface.getScreenshot(image, x, y);
    }
    
    /**
        Returns true if the current thread is the animation thread.
    */
    public static boolean isAnimationThread() {
        // This method can be called from any thread, even after an app has quit
        AppContext context = CoreSystem.getThisAppContext();
        if (context != null) {
            Stage instance = context.getStage();
            if (instance != null) {
                return (instance.animationThread == Thread.currentThread());
            }
        }
        return false;
    }
    
    //
    // Methods called from the main platform class
    //
    
    // Called from the AWT event thread
    public synchronized void start() {
        if (destroyed) {
            throw new RuntimeException();
        }
        
        if (animationThread == null) {
            surface.notifyStart();
            // Run animation at norm priority because the AWT Event thread 
            // needs to run at a higher priority. 
            animationThread = appContext.createThread("PulpCore-Stage", this);
            appContext.setAnimationThread(animationThread);
            animationThread.start();
        }
    }
    
    // Called from the AWT event thread
    public synchronized void stop() {
        if (animationThread != null) {
            surface.notifyStop();
            try {
                animationThreadStop().join(500);
            }
            catch (InterruptedException ex) { }
        }
    }
    
    // Called from the AWT event thread
    // After destroying, the Stage must never be used again
    public synchronized void destroy() {
        destroyed = true;
        if (animationThread == null) {
            doDestroy();
        }
        else {
            stop();
        }
    }
    
    //
    // Animation thread
    //
    
    public void run() {
        
        Thread currentThread = Thread.currentThread();
        
        // Run in a loop - if animationLoop() throws an exception or returns, 
        // the app is "rebooted".
        while (animationThread == currentThread) {
            
            try {
                animationLoop();
            }
            catch (Throwable t) {
                
                boolean uncaughtExceptionSceneBroke = 
                    (currentScene != null && currentScene == uncaughtExceptionScene);
                
                currentScene = null;
                nextScene = null;
                nextSceneType = NO_NEXT_SCENE;
                sceneStack = new LinkedList();
                
                if (t instanceof ThreadDeath || uncaughtExceptionSceneBroke) {
                    // Don't reboot 
                    animationThreadStop();
                }
                else {
                    try {
                        CoreSystem.setTalkBackField("pulpcore.uncaught-exception", t);
                    }
                    catch (Exception ex) {
                        t.printStackTrace();
                    }
                    if (uncaughtExceptionScene == null) {
                        // Delay and reboot
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ex) { }
                    }
                    else {
                        // Show error scene
                        currentScene = uncaughtExceptionScene;
                        currentScene.load();
                        currentScene.showNotify();
                    }
                }
            }
        }
    }
    
    private synchronized Thread animationThreadStop() {
        Thread t = animationThread;
        appContext.setAnimationThread(null);
        animationThread = null;
        return t;
    }
        
    /**
        This method is called repeatedly from the run() method.
    */
    private void animationLoop() {
        
        Thread currentThread = Thread.currentThread();
        
        long lastTimeMicros = CoreSystem.getTimeMicros();
        long nextTimeMicros = lastTimeMicros + frameRateDelay;
        int elapsedTime = 0;
        
        while (animationThread == currentThread) {
            
            // Check if the surface is ready to be drawn to
            if (!surface.isReady()) {
                if (Build.DEBUG) CoreSystem.print("Surface not ready.");
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException ex) { }
                lastTimeMicros = CoreSystem.getTimeMicros();
                nextTimeMicros = lastTimeMicros + frameRateDelay;
                elapsedTime = 0;
                remainderMicros = 0;
                continue;
            }
            
            // Take in debug keys
            if (Build.DEBUG) {
                if (Input.isControlDown() && Input.isPressed(Input.KEY_C)) {
                    debugCommands |= DEBUG_COMMAND_SHOW_CONSOLE;
                }
                if (Input.isControlDown() && Input.isPressed(Input.KEY_X)) {
                    debugCommands |= DEBUG_COMMAND_SHOW_SCENE_SELECTOR;
                }
                if (Input.isControlDown() && Input.isPressed(Input.KEY_I)) {
                    debugCommands |= DEBUG_COMMAND_SHOW_SCENE_INFO;
                }
                if (Input.isControlDown() && Input.isPressed(Input.KEY_1)) {
                    debugCommands |= DEBUG_COMMAND_SPEED_SLOW;
                }
                if (Input.isControlDown() && Input.isPressed(Input.KEY_2)) {
                    debugCommands |= DEBUG_COMMAND_SPEED_NORMAL;
                }
                if (Input.isControlDown() && Input.isPressed(Input.KEY_3)) {
                    debugCommands |= DEBUG_COMMAND_SPEED_FAST;
                }
                
                if (currentScene instanceof ConsoleScene) {
                    debugCommands &= ~DEBUG_COMMAND_SHOW_CONSOLE;
                }
                if (currentScene instanceof SceneSelector) {
                    debugCommands &= ~DEBUG_COMMAND_SHOW_SCENE_SELECTOR;
                }
            }
    
            // Check if a new Scene should be shown
            if (Build.DEBUG) {
                if ((debugCommands & DEBUG_COMMAND_SHOW_CONSOLE) != 0) {
                    pushScene(new ConsoleScene());
                }
                if ((debugCommands & DEBUG_COMMAND_SHOW_SCENE_SELECTOR) != 0) {
                    pushScene(new SceneSelector());
                }
            }
            if (hasNewScene()) {
                lastTimeMicros = CoreSystem.getTimeMicros();
                nextTimeMicros = lastTimeMicros + frameRateDelay;
                elapsedTime = 0;
                remainderMicros = 0;
            }
            if (currentScene == null) {
                animationThreadStop();
                break;
            }
            
            boolean oldFocus = Input.hasKeyboardFocus();
            
            // Capture input
            pollInput();
            
            // Redraw if the focus changed
            boolean focusedChanged = Input.hasKeyboardFocus() != oldFocus;
            boolean needsFullRedraw = focusedChanged | renderingErrorOccurred;
            
            if (Build.DEBUG) {
                if ((debugCommands & DEBUG_COMMAND_SHOW_SCENE_INFO) != 0) {
                    showInfoOverlay = !showInfoOverlay;
                    needsFullRedraw = true;
                }
                if ((debugCommands & DEBUG_COMMAND_SPEED_SLOW) != 0) {
                    speed = SLOW_MOTION_SPEED;
                    elapsedTimeRemainder = 0;
                }
                if ((debugCommands & DEBUG_COMMAND_SPEED_NORMAL) != 0) {
                    speed = 1;
                    elapsedTimeRemainder = 0;
                }
                if ((debugCommands & DEBUG_COMMAND_SPEED_FAST) != 0) {
                    speed = FAST_MOTION_SPEED;
                    elapsedTimeRemainder = 0;
                }
            }
            
            // Finished processing debug commands
            debugCommands = 0;
            
            // Reset dirty rectangles. Scene can set dirty rectangles in updateScene() or 
            // drawScene()
            numDirtyRectangles = -1;
            
            if (surface.contentsLost()) {
                needsFullRedraw = true;
                setTransform();
            }
            if (needsFullRedraw) {
                currentScene.redrawNotify();
            }
            
            CoreGraphics g = surface.getGraphics();
            
            // Update and draw scene
            final Scene scene = currentScene;
            synchronized (scene) {
                appContext.runEvents();
                
                scene.updateScene(elapsedTime);
                
                // Set the transform
                // (Don't set the default transform for a Scene2D - it already handles it)
                g.reset();
                if (!(scene instanceof Scene2D)) {
                    g.getTransform().concatenate(defaultTransform);
                }
                
                try {
                    scene.drawScene(g);
                    renderingErrorOccurred = false;
                }
                catch (ArrayIndexOutOfBoundsException ex) {
                    // The CoreGraphics system can still throw some ArrayIndexOutOfBoundsExceptions 
                    // in some rare cases. It may be from scaling a sprite to 
                    // have a width and height < 1.
                    appContext.setTalkBackField("pulpcore.platform.graphics.error", ex);
                    renderingErrorOccurred = true;
                }
            }
            
            // Draw frame rate and memory info (DEBUG only)
            if (Build.DEBUG) {
                if (showInfoOverlay && infoOverlay != null &&
                    !(currentScene instanceof Scene2D))
                {
                    g.reset();
                    g.getTransform().concatenate(defaultTransform);
                    infoOverlay.draw(g);
                }
            }
                
            // Send pending sound data to sound system
            // (Don't create the sound engine if it's not already created)
            if (CoreSystem.getPlatform().isSoundEngineCreated()) {
                double fps = actualFPS > 0 ? actualFPS : desiredFPS > 0 ? desiredFPS : getDefaultFrameRate();
                int estimatedTimeUntilNextUpdate = Math.max(elapsedTime, 
                    (int)Math.round(1000 / fps));
                CoreSystem.getPlatform().updateSoundEngine(estimatedTimeUntilNextUpdate);
            }

            // Show surface (blocks until surface is updated)
            long surfaceSleepTimeMicros;
            if (surface.contentsLost() || numDirtyRectangles < 0) {
                surfaceSleepTimeMicros = surface.show();
            }
            else {
                surfaceSleepTimeMicros = surface.show(dirtyRectangles, numDirtyRectangles);
            }

            appContext.notifyFrameComplete();

            // Sleep to create correct frame rate
            long currTimeMicros;
            if (frameRateDelay == 0) {
                if (Build.DEBUG) {
                    overlaySleepTime += surfaceSleepTimeMicros;
                }
                currTimeMicros = CoreSystem.getTimeMicros();
            }
            else {
                long priorToSleepTime = CoreSystem.getTimeMicros();
                currTimeMicros = CoreSystem.getPlatform().sleepUntilTimeMicros(nextTimeMicros);

                // Get next sleep time
                nextTimeMicros += frameRateDelay;
                if (currTimeMicros > nextTimeMicros) {
                    nextTimeMicros = currTimeMicros + frameRateDelay;
                }          

                if (Build.DEBUG) {
                    long sleepTimeMicros = currTimeMicros - priorToSleepTime;
                    overlaySleepTime += surfaceSleepTimeMicros + sleepTimeMicros;
                }
            }
            doInfoSample();

            // Update elapsed time
            long elapsedTimeMicros = currTimeMicros - lastTimeMicros + remainderMicros;
            elapsedTime = (int)(elapsedTimeMicros / 1000);
            remainderMicros = elapsedTimeMicros - elapsedTime * 1000;
            lastTimeMicros = currTimeMicros;
          
            if (Build.DEBUG && speed != 1) {
                float e = elapsedTime * speed + elapsedTimeRemainder;
                elapsedTime = (int)e;
                elapsedTimeRemainder = e - elapsedTime;
            }
        }
        
        if (destroyed) {
            doDestroy();
        }
    }
    
    private void doDestroy() {
        animationThread = Thread.currentThread();
        appContext.setAnimationThread(animationThread);
        if (currentScene != null) {
            currentScene.hideNotify();
            currentScene.unload();
        }
        clearSceneStack();

        if (shutdownCode != null) {
            try {
                shutdownCode.run();
            }
            catch (Throwable ex) {
                if (Build.DEBUG) ex.printStackTrace();
            }
        }
        appContext.setAnimationThread(null);
        animationThread = null;
    }
    
    /**
        Removes and unloads all scenes from the stack. 
    */
    private void clearSceneStack() {
        while (!sceneStack.isEmpty()) {
            Scene scene = (Scene)sceneStack.removeLast();
            scene.unload();
        }
    }
    
    private boolean hasNewScene() {
        
        boolean nextSceneLoaded = false;
        
        if (currentScene == null) {
            try {
                currentScene = CoreSystem.getThisAppContext().createFirstScene();
            }
            catch (Throwable ex) {
                if (Build.DEBUG) {
                    CoreSystem.print("Couldn't create first scene", ex);
                    if ((ex instanceof NoClassDefFoundError) &&
                            ex.getMessage().indexOf("ScalaObject") != -1)
                    {
                        CoreSystem.print("To run Scala PulpCore apps in a browser, " +
                                "compile the app in release mode.");
                    }
                }
            }
            if (currentScene == null) {
                if (Build.DEBUG) {
                    CoreSystem.print("Couldn't create first scene");
                    currentScene = new ConsoleScene();
                }
                else {
                    return false;
                }
            }
        }
        else if (nextSceneType == NO_NEXT_SCENE) {
            return false;
        }
        else if (nextSceneType == PUSH_SCENE) {
            currentScene.hideNotify();
            sceneStack.addLast(currentScene);
            currentScene = nextScene;
        }
        else if (nextSceneType == POP_SCENE) {
            currentScene.hideNotify();
            currentScene.unload();
            if (sceneStack.isEmpty()) {
                currentScene = null;
            }
            else {
                currentScene = (Scene)sceneStack.removeLast();
            }
            nextSceneLoaded = true;
        }
        else if (nextSceneType == SET_SCENE) {
            currentScene.hideNotify();
            currentScene.unload();
            clearSceneStack();
            currentScene = nextScene;
        }
        else if (nextSceneType == REPLACE_SCENE) {
            currentScene.hideNotify();
            currentScene.unload();
            currentScene = nextScene;
        }
        
        // Set defaults
        nextScene = null;
        nextSceneType = NO_NEXT_SCENE;
        setFrameRate(DEFAULT_FPS);
        
        // Perform a garbage collection if no sounds are playing
        // (A GC causes sound distortion on some systems)
        // NOTE: this is disabled because it could interfere with WeakReference caching of
        // images, sounds, and fonts inbetween scenes.
        //if (CoreSystem.getNumSoundsPlaying() == 0) {
        //    System.gc();
        //}
        
        if (currentScene != null) {
            if (Build.DEBUG) {
                String sceneName = currentScene.getClass().getName();
                CoreSystem.printMemory("Stage: scene set to " + sceneName); 
            }
            if (!nextSceneLoaded) {
                currentScene.load();
            }
            currentScene.showNotify();
        }
        
        // Do an extra input poll clear any keypresses during the scene switch process.
        // (Note, the input is polled again after returning from this method)
        pollInput();
        
        return true;
    }
    
    private void setTransform() {
        defaultTransform.clear();
        
        if (surface.getWidth() != naturalWidth || surface.getHeight() != naturalHeight) {
            
            float w = naturalWidth;
            float h = naturalHeight;
            
            switch (autoScaleType) {
                default: case AUTO_OFF:
                    // Do nothing
                    break;
                    
                case AUTO_CENTER:
                    defaultTransform.translate(
                        CoreMath.toFixed((surface.getWidth() - w) / 2),
                        CoreMath.toFixed((surface.getHeight() - h) / 2));
                    break;
                    
                case AUTO_STRETCH:
                    defaultTransform.scale(
                        CoreMath.toFixed(surface.getWidth() / w),
                        CoreMath.toFixed(surface.getHeight() / h));
                    break;
                    
                case AUTO_FIT:
                    float a = h * surface.getWidth();
                    float b = w * surface.getHeight();
                    float newWidth;
                    float newHeight;
                    
                    if (a > b) {
                        newHeight = surface.getHeight();
                        newWidth = (float)Math.floor(newHeight * w / h);
                    }
                    else if (a < b) {
                        newWidth = surface.getWidth();
                        newHeight = (float)Math.floor(newWidth * h / w);
                    }
                    else {
                        newWidth = surface.getWidth();
                        newHeight = surface.getHeight();
                    }
                    
                    defaultTransform.translate(
                        CoreMath.floor(CoreMath.toFixed((surface.getWidth() - newWidth) / 2)),
                        CoreMath.floor(CoreMath.toFixed((surface.getHeight() - newHeight) / 2)));
                    // Kind of weird - looks good with the Flashlight example at various sizes
                    int scaleX = (int)Math.floor(CoreMath.ONE * newWidth / w - 1);
                    int scaleY = (int)Math.floor(CoreMath.ONE * newHeight / h - 1);
                    // Scale both identically
                    int scale = (scaleX + scaleY)/2;
                    defaultTransform.scale(scale, scale);
                    break;
            }
        }
    }
    
    public void pollInput() {
        appContext.pollInput();
        
        if (defaultTransform.getType() != Transform.TYPE_IDENTITY) {
            PolledInput p = appContext.getPolledInput();
            transform(p.mouse);
            transform(p.mousePress);
            transform(p.mouseRelease);
            transform(p.mouseWheel);
        }
    }
    
    private final void transform(Tuple2i point) {
        int fx = CoreMath.toFixed(point.x);
        int fy = CoreMath.toFixed(point.y);
        int tx = defaultTransform.inverseTransformX(fx, fy);
        int ty = defaultTransform.inverseTransformY(fx, fy);
        point.x = (tx == Integer.MAX_VALUE) ? tx : CoreMath.toInt(tx);
        point.y = (ty == Integer.MAX_VALUE) ? ty : CoreMath.toInt(ty);
    }
        
    //
    // Called via reflection via PulpCore Player
    //
    
    private static void toggleInfoOverlay() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SHOW_SCENE_INFO;
    }
    
    private static void showConsole() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SHOW_CONSOLE;
    }
    
    private static void showSceneSelector() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SHOW_SCENE_SELECTOR;
    }
    
    private static void setSpeedSlow() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SPEED_SLOW;
    }
    
    private static void setSpeedNormal() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SPEED_NORMAL;
    }
    
    private static void setSpeedFast() {
        Stage instance = getThisStage();
        instance.debugCommands |= DEBUG_COMMAND_SPEED_FAST;
    }
    
    //
    // Debug mode only
    //
    
    public static Sprite getInfoOverlay() {
        Stage instance = getThisStage();
        if (!instance.showInfoOverlay) {
            return null;
        }
        else {
            return instance.infoOverlay;
        }
    }
    
    private void doInfoSample() {
        
        // Sample every 500 milliseconds
        
        if (overlayCreationTime == 0) {
            overlayFrames = 0;
            overlayCreationTime = CoreSystem.getTimeMicros();
            return;
        }
        
        overlayFrames++;
        
        long time = CoreSystem.getTimeMicros() - overlayCreationTime;
        if (time < 500000) {
            return;
        }
        
        // For release mode, just calculate the frame rate and return;
        actualFPS = overlayFrames * 1000000.0 / time;
        if (!Build.DEBUG) {
            overlayFrames = 0;
            overlayCreationTime = CoreSystem.getTimeMicros();
            return;
        }
        
        // Take a sample of CPU, memory, and Scene info
        int fixedFPS = CoreMath.toFixed(actualFPS);
        int sleepTime = CoreMath.toFixed((float)overlaySleepTime / (1000*overlayFrames));
        int fixedCPU = CoreMath.toFixed(1-(float)overlaySleepTime / time);
        
        overlayFrames = 0;
        overlaySleepTime = 0;
        overlayCreationTime = CoreSystem.getTimeMicros();
        
        String fps = CoreMath.toString(fixedFPS, 1) + " fps";
        if (sleepTime > 0) {
            fps += " (" + CoreMath.toString(sleepTime, 1) + "ms sleep)";
        }
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long currentMemory = totalMemory - runtime.freeMemory();
        
        if (memActivity == null) {
            memActivity = new Activity();
            cpuActivity = new Activity();
            cpuActivity.setMax(CoreMath.ONE);
        }
        memActivity.setMax((int)(totalMemory / 1024));
        memActivity.addSample((int)(currentMemory / 1024));
        cpuActivity.addSample(fixedCPU);
        
        String memoryUsage = 
            ((float)((currentMemory * 10) >> 20) / 10) + " of " +
            ((float)((totalMemory * 10) >> 20) / 10) + " MB";
        
        String currentSceneName;
        if (currentScene == null) {
            currentSceneName = "null";
        }
        else {
            currentSceneName = currentScene.getClass().getName();
            
            String info = "";
            
            if (currentScene instanceof Scene2D) {
                Scene2D scene2D = (Scene2D)currentScene;
                info += scene2D.getNumVisibleSprites() + " of ";
                info += scene2D.getNumSprites() + " sprites";
                info += ", " + scene2D.getNumTimelines() + " timelines";
            }
            if (numDirtyRectangles >= 0) {
                if (info.length() > 0) {
                    info += ", ";
                }
                info += numDirtyRectangles + " dirty rects";
            }
            
            // Removed - this prevents ProGuard from removing sound code
            //int numSounds = CoreSystem.getNumSoundsPlaying();
            //if (numSounds > 0) {
            //    if (info.length() > 0) {
            //        info += ", ";
            //    }
            //    info += numSounds + " sounds";
            //}
            
            if (info.length() > 0) {
                currentSceneName += " (" + info + ")";
            }
        }
        
        if (!showInfoOverlay) {
            return;
        }
        
        // Draw it
        int lineHeight = CoreFont.getSystemFont().getHeight() + 2;
        int height = 32 + 5 + lineHeight;
        int activityX = 3;
        int activityY = lineHeight + 2;
        int activityWidth = 32;
        int activityTextX = activityWidth + 3;
        int activityTextY = activityY + 9;
        
        if (infoOverlay == null || infoOverlay.width.get() != getWidth()) {
            CoreImage image = new CoreImage(getWidth(), height, false); 
            infoOverlay = new ImageSprite(image, 0, 0);
        }
        CoreGraphics g = infoOverlay.getImage().createGraphics();
        g.clear();
        g.setColor(0xccffffff);
        g.fillRect(0, 0, getWidth(), height);
        g.drawString(currentSceneName, activityX, 2);
        
        cpuActivity.draw(g, activityX, activityY, 32);
        g.drawString(fps, activityX + activityTextX, activityTextY);
        
        activityX = getWidth() / 2;
        memActivity.draw(g, activityX, activityY, 32); 
        g.drawString(memoryUsage, activityX + activityTextX, activityTextY);
        
        infoOverlay.setDirty(true);
    }
    
    
    static class Activity {
        
        private static final int NUM_SAMPLES = 32;
        
        private int[] samples = new int[NUM_SAMPLES];
        private int index = 0;
        private int max = -1;
        
        public void addSample(int value) {
            samples[index] = value;
            index = (index + 1) % NUM_SAMPLES;
        }
        
        public int getMax() {
            if (this.max != -1) {
                return this.max;
            }
            int calculatedMax = Integer.MIN_VALUE;
            for (int i = 0; i < NUM_SAMPLES; i++) {
                calculatedMax = Math.max(calculatedMax, samples[(index + i) % NUM_SAMPLES]);
            }
            return calculatedMax;
        }
        
        public void setMax(int max) {
            this.max = max;
        }
        
        public void draw(CoreGraphics g, int x, int y, int height) {
            int calculatedMax = getMax();
            
            g.setColor(Colors.BLACK);
            g.fillRect(x, y, NUM_SAMPLES, height);
            g.setColor(0xff25f816);
            
            // Newest sample on the right
            for (int i = 0; i < NUM_SAMPLES; i++) {
                int j = NUM_SAMPLES - i - 1;
                int sample = samples[(index + j) % NUM_SAMPLES];
                int sampleHeight = height * sample / calculatedMax;
                g.fillRect(x + j, y + height - sampleHeight, 1, sampleHeight); 
            }
        }
    }
}
