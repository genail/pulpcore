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

import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.Hashtable;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.math.Rect;
import pulpcore.platform.Surface;

/**
    The BufferStrategySurface class is a Surface implementation for Java 1.4 or 
    newer. It provides about the same performace as BufferedImageSurface, but does not
    result in dropped frames on Mac OS X.
*/
public class BufferStrategySurface extends Surface {

    protected static final DirectColorModel COLOR_MODEL = 
        new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
        
    private BufferStrategy bufferStrategy;
    private BufferedImage bufferedImage;
    private Container container;
    private Canvas canvas;
    private boolean osRepaint;
    private boolean useDirtyRects;
    
    public BufferStrategySurface(Container container) {
        this.container = container;
        this.canvas = new Canvas() {
            public void paint(Graphics g) {
                notifyOSRepaint();
            }
            
            public void update(Graphics g) {
                notifyOSRepaint();
            }
        };
        container.removeAll();
        container.setLayout(null);
        canvas.setSize(1, 1);
        container.add(canvas);
        canvas.setLocation(0, 0);
        // Try to create the surface for the sake of toString(), below
        isReady();
    }
    
    public Canvas getCanvas() {
        return canvas;
    }
    
    public synchronized void notifyOSRepaint() {
        osRepaint = true;
    }
    
    private synchronized void checkOSRepaint() {
        if (osRepaint) {
            osRepaint = false;
            contentsLost = true;
        }
    }
    
    public boolean isReady() {
        int w = container.getWidth();
        int h = container.getHeight();
        if (w <= 0 || h <= 0) {
            return false;
        }
        else if (bufferedImage == null || getWidth() != w || getHeight() != h) {
            setSize(w, h);
        }
        
        if (bufferStrategy == null) {
            try {
                useDirtyRects = false;
                createBufferStrategy();
                bufferStrategy = canvas.getBufferStrategy();
                if (bufferStrategy != null) {
                    BufferCapabilities caps = bufferStrategy.getCapabilities();
                    useDirtyRects = !caps.isPageFlipping();
                }
            }
            catch (Exception ex) {
                // There is a ClassCastException when running under Eclipse 3.3 for Mac OS X Java 5.
                // My guess is there is a SWT conflict. This won't affect end users, but developers 
                // will have to use appletviewer or a web browser to run PulpCore apps.
                if (Build.DEBUG) CoreSystem.print("Couldn't create surface", ex);
            }
        }
        else if (!contentsLost) {
            contentsLost = bufferStrategy.contentsLost() | bufferStrategy.contentsRestored();
        }
        
        checkOSRepaint();
        
        return (bufferStrategy != null);
    }
    
    protected void notifyResized() {
        int w = getWidth();
        int h = getHeight();
        contentsLost = true;
        bufferStrategy = null;
        canvas.setSize(w, h);

        // Create the AWT image
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, w, h, new int[] { 0xff0000, 0x00ff00, 0x0000ff }); 
        DataBuffer dataBuffer = new DataBufferInt(getData(), w * h);
        WritableRaster raster = Raster.createWritableRaster(
            sampleModel, dataBuffer, new Point(0,0));
        bufferedImage = new BufferedImage(COLOR_MODEL, raster, true, new Hashtable());
    }
    
    public long show(Rect[] dirtyRectangles, int numDirtyRectangles) {
        if (bufferStrategy == null || bufferedImage == null) {
            return 0;
        }
        
        if (numDirtyRectangles == 0 && !contentsLost) {
            return 0;
        }
        
        long sleepTime = 0;
        Graphics g = null;
        try {
            while (true) {
                while (true) {
                    g = bufferStrategy.getDrawGraphics();
            
                    if (g != null) {
                        if (contentsLost || !useDirtyRects || numDirtyRectangles < 0) {
                            g.drawImage(bufferedImage, 0, 0, null);
                        }
                        else {
                            for (int i = 0; i < numDirtyRectangles; i++) {
                                Rect r = dirtyRectangles[i];
                                g.setClip(r.x, r.y, r.width, r.height);
                                g.drawImage(bufferedImage, 0, 0, null);
                            }
                        }
                        g.dispose();
                        g = null;
                    }
                    
                    if (bufferStrategy.contentsRestored()) {
                        contentsLost = true;
                    }
                    else {
                        break;
                    }
                }

                bufferStrategy.show();
                
                if (bufferStrategy.contentsLost()) {
                    contentsLost = true;
                }
                else {
                    contentsLost = false;
                    break;
                }
            }
        }
        catch (Exception ex) {
            // Ignore
        }
        finally {
            if (g != null) {
                g.dispose();
            }
        }
        return sleepTime;
    }
    
    private void createBufferStrategy() {
        // First, try Copied method (double buffering)
        //try {
        //    canvas.createBufferStrategy(2, new BufferCapabilities(
        //        new ImageCapabilities(true),
        //        new ImageCapabilities(true),
        //        BufferCapabilities.FlipContents.COPIED));
        //    return;
        //} 
        //catch (AWTException e) {
        //    // Ignore
        //}
        
        // Try whatever the system wants
        canvas.createBufferStrategy(2);
        
        /* Try to enable vsync (Java 6u10)
        
            Note, "separate_jvm" property must be specified for applets that v-sync. From the bug
            description:
                Note that since the D3D pipeline uses single thread rendering - meainng
                that all D3D-related activity happens on a single thread only
                one BufferStrategy in per vm instance can be made v-synced without
                undesireable effects. 
                
                If there's more than one (say N) v-synced BSs then
                since their Present() calls will effectively be serialized (since they're
                running from a single thread) each BS will be able
                to flip only on every Nth vsync, resulting in decrease in
                perceived responsiveness.

            Unfortunately this doesn't work because sandboxed applets can't access sun.* packages.
            This is left here (commented out) in case the API gets moved to com.sun. 
        */
        /*
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();
        if (bufferStrategy != null) {
            BufferCapabilities caps = bufferStrategy.getCapabilities();
            try {
                Class ebcClass = Class.forName(
                    "sun.java2d.pipe.hw.ExtendedBufferCapabilities");
                Class vstClass = Class.forName(
                    "sun.java2d.pipe.hw.ExtendedBufferCapabilities$VSyncType");
                
                Constructor ebcConstructor = ebcClass.getConstructor(
                    new Class[] { BufferCapabilities.class, vstClass });
                Object vSyncType = vstClass.getField("VSYNC_ON").get(null);
                
                BufferCapabilities newCaps = (BufferCapabilities)ebcConstructor.newInstance(
                    new Object[] { caps, vSyncType });
                
                canvas.createBufferStrategy(2, newCaps);
                
                // TODO: if success, setCanChangeRefreshRate(false) and setRefreshRate(60). 
                // Possibly override refreshRateSync()?
            }
            catch (Throwable t) {
                // Ignore
                t.printStackTrace();
            }
        }
        */
    }
    
    public String toString() {
        String s = "BufferStrategy";
        if (bufferStrategy != null) {
            BufferCapabilities caps = bufferStrategy.getCapabilities();
            s += " (" + 
            "isPageFlipping=" + caps.isPageFlipping() + 
            ", " + 
            "useDirtyRects=" + useDirtyRects + 
            ")";
        }
        return s;
    }
}
