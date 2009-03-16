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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.platform.AppContext;
import pulpcore.platform.applet.opt.NanoTimer;
import pulpcore.platform.Platform;
import pulpcore.platform.SoundEngine;

/*
TODO: 
Metion "accessClipboard" permission somewhere
My .java.policy file: 

grant codeBase "file:/Users/brackeen/Pulp/projects/-" {
     permission java.awt.AWTPermission "accessClipboard";
};

*/
public final class AppletPlatform implements Platform {
    
    private static final AppletPlatform INSTANCE = new AppletPlatform();
    
    private String clipboardText = "";
    private SystemTimer timer;
    
    private AppletAppContext mainContext = null;
    private List allContexts = null;
    private AppletAppContext initContext = null;
    private Thread initContextThread = null;
    
    private SoundEngine soundEngine;
    
    public static AppletPlatform getInstance() {
        return INSTANCE;
    }
    
    private AppletPlatform() {
        
        // Check for Java 1.5 highRes timer
        if (CoreSystem.isJava15orNewer()) {
            try {
                 Class c = Class.forName("java.lang.System");
                 c.getMethod("nanoTime", new Class[0]);
                 timer = new NanoTimer();
            }
            catch (Throwable t) {
                // ignore
            }
        }
        
        // Use an estimating timer on Windows
        if (timer == null) {
            if (CoreSystem.isWindows()) {
                timer = new Win32Timer();
            }
        }
        
        // Use System.currentTimeMillis()
        if (timer == null) {
            timer = new SystemTimer();
        }

        CoreSystem.init(this);
    }
    
    //
    // App management
    //
    
    public AppContext getThisAppContext() {
        // In most cases, there will be only one registered App. In that case, this method
        // returns as quickly as possible.
        if (allContexts == null) {
            return mainContext;
        }
        
        synchronized (this) {
            // Double check inside the lock
            if (allContexts == null) {
                return mainContext;
            }
            
            // Look through all registered apps and find the context for this ThreadGroup
            // TODO: implement as ThreadLocal instead? (This implementation was Java 1.1 compatible)
            ThreadGroup currentThreadGroup = Thread.currentThread().getThreadGroup();
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.get(i);
                ThreadGroup contextThreadGroup = context.getThreadGroup();
                if (contextThreadGroup == currentThreadGroup ||
                    contextThreadGroup.parentOf(currentThreadGroup))
                {
                    return context;
                }
            }

            if (initContext != null && Thread.currentThread() == initContextThread) {
                // We're initializing the context from the system thread
                return initContext;
            }
            
            throw new Error("No context found for thread");
        }
    }
    
    private synchronized AppContext getAppContext(CoreApplet app) {
        if (mainContext != null && mainContext.getApplet() == app) {
            return mainContext;
        }
        
        if (allContexts != null) {
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.get(i);
                if (context.getApplet() == app) {
                    return context;
                }
            }
        }
        
        return null;
    }
    
    private synchronized boolean isRegistered(CoreApplet app) {
        return (getAppContext(app) != null);
    }
    
    private synchronized int getNumRegisteredApps() {
        if (allContexts == null) {
            if (mainContext == null) {
                return 0;
            }
            else {
                return 1;
            }
        }
        else {
            return allContexts.size();
        }
    }
    
    public synchronized AppContext registerApp(CoreApplet app) {
        
        if (app == null) {
            return null;
        }
        
        AppContext context = getAppContext(app);
        if (context != null) {
            return context;
        }
        
        boolean wasEmpty = (getNumRegisteredApps() == 0);
        
        AppletAppContext newContext = new AppletAppContext(app, timer);
        if (mainContext == null) {
            mainContext = newContext;
        }
        else {
            if (allContexts == null) {
                allContexts = new ArrayList();
                allContexts.add(mainContext);
            }
            allContexts.add(newContext);
        }
        // Setup a temporary context for init
        initContext = newContext;
        initContextThread = Thread.currentThread();
        newContext.init();
        initContext = null;
        initContextThread = null;
               
        if (wasEmpty) {
            timer.start();
        }
        
        return newContext;
    }
    
    public synchronized void unregisterApp(CoreApplet app) {
        
        if (app == null || !isRegistered(app)) {
            return;
        }
        
        if (mainContext != null && mainContext.getApplet() == app) {
            mainContext.destroy();
            mainContext = null;
        }
        
        if (allContexts != null) { 
            for (int i = 0; i < allContexts.size(); i++) {
                AppletAppContext context = (AppletAppContext)allContexts.get(i);
                if (context.getApplet() == app) {
                    context.destroy();
                    allContexts.remove(i);
                    break;
                }
            }
            
            if (mainContext == null) {
                mainContext = (AppletAppContext)allContexts.get(0);
            }
            
            if (allContexts.size() == 1) {
                allContexts = null;
            }
        }
        
        if (getNumRegisteredApps() == 0) {
            timer.stop();
            if (soundEngine != null) {
                soundEngine.destroy();
                soundEngine = null;
            }
        }
    }
    
    //
    // System time
    //
    
    public long getTimeMillis() {
        return timer.getTimeMillis();
    }
    
    public long getTimeMicros() {
        return timer.getTimeMicros();
    }
    
    public long sleepUntilTimeMicros(long time) {
        return timer.sleepUntilTimeMicros(time);
    }
    
    //
    // Clipboard
    //
    
    public boolean isNativeClipboard() {
        if (!Build.DEBUG) {
            // The applet doesn't have permission
            return false;
        }
        else {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard();
                return true;
            }
            catch (SecurityException ex) {
                return false;
            }
            
            // TODO: test is JNLP clipboard is available
        }
    }
    
    public String getClipboardText() {
        if (Build.DEBUG) {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                
                int attemptsLeft = 10; // 10*100ms = 1 second
                while (attemptsLeft > 0) {
                    try {
                        Transferable contents = clipboard.getContents(null);
                        Object data = contents.getTransferData(DataFlavor.stringFlavor);
                        if (data instanceof String) {
                            return (String)data;
                        }
                        else {
                            // Shouldn't happen
                            return "";
                        }
                    }
                    catch (UnsupportedFlavorException ex) {
                        // String text not available
                        return "";
                    }
                    catch (IOException ex) {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException ie) {
                            // Ignore
                        }
                        attemptsLeft--;
                    }
                    catch (IllegalStateException ex) {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException ie) {
                            // Ignore
                        }
                        attemptsLeft--;
                    }
                }
            }
            catch (SecurityException ex) {
                // The applet doesn't have permission
            }
        }
        
        // Try JNLP API
        /*
        Object clipboard = getJNLPService("javax.jnlp.ClipboardService");
        if (clipboard != null) {
            try {
                Method m = clipboard.getClass().getMethod("getContents", new Class[0]);
                Transferable t = (Transferable)m.invoke(clipboard, new Object[0]);
                
                if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) { 
                    try { 
                        return (String)t.getTransferData(DataFlavor.stringFlavor); 
                    } 
                    catch (Exception e) { 
                        return ""; 
                    } 
                }
                else {
                    return "";
                }
            }
            catch (Throwable t) {
                // Ignore, fall through
                if (Build.DEBUG) CoreSystem.print("JNLP fail", t);
            }
        }
        */
        
        // Internal clipboard
        return clipboardText;
    }
    
    public void setClipboardText(String text) {
        if (text == null) {
            text = "";
        }
        
        if (Build.DEBUG) {
            // Debug mode - the developer might have put permission in the policy file.
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection data = new StringSelection(text);
                int attemptsLeft = 10; // 10*100ms = 1 second
                
                while (attemptsLeft > 0) {
                    try {
                        clipboard.setContents(data, data);
                        // Success
                        return;
                    }
                    catch (IllegalStateException ex) {
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException ie) {
                            // Ignore
                        }
                        attemptsLeft--;
                    }
                }
            }
            catch (SecurityException ex) {
                // The applet doesn't have permission - ignore and try again
            }
        }
        
        // Try JNLP API
        /*
        Object clipboard = getJNLPService("javax.jnlp.ClipboardService");
        if (clipboard != null) {
            StringSelection data = new StringSelection(text);
            try {
                Method m = clipboard.getClass().getMethod("setContents", 
                    new Class[] { Class.forName("java.awt.datatransfer.Transferable") });
                m.invoke(clipboard, new Object[] { data });
                if (Build.DEBUG) CoreSystem.print("JNLP success");
                // Success
                return;
            }
            catch (Throwable t) {
                // Ignore, fall through
                if (Build.DEBUG) CoreSystem.print("JNLP fail", t);
            }
        }
        */
        
        // Internal clipboard
        clipboardText = text;
    }
    
    public Object getJNLPService(String service) {
        try {
             Class c = Class.forName("javax.jnlp.ServiceManager");
             Method m = c.getMethod("lookup", new Class[] { String.class });
             return m.invoke(null, new Object[] { service });
        }
        catch (Throwable t) {
            return null;
        }
    }
    
    //
    // Sound
    //
    
    public boolean isSoundEngineCreated() {
        return (soundEngine != null);
    }
    
    public SoundEngine getSoundEngine() {
        if (soundEngine == null) {
            soundEngine = JavaSound.create();
            CoreSystem.setTalkBackField("pulpcore.platform.sound", "javax.sound");
        }
        return soundEngine;
    }
    
    public void updateSoundEngine(int timeUntilNextUpdate) {
        if (soundEngine != null) {
            soundEngine.update(timeUntilNextUpdate);
        }
    }
    
    //
    // Browser
    //
        
    public boolean isBrowserHosted() {
        return true;
    }
    
    /**
        Returns the name of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserName() {
        AppletAppContext context = (AppletAppContext)getThisAppContext();
        return context.getAppProperty("pulpcore_browser_name");
    }
    
    /**
        Returns the version of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserVersion() {
        AppletAppContext context = (AppletAppContext)getThisAppContext();
        return context.getAppProperty("pulpcore_browser_version");
    }
}
