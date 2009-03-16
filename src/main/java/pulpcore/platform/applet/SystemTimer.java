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

import pulpcore.CoreSystem;

/**
    Uses System.currentTimeMillis() and compensates for backward jumps in
    time that can occur on some systems (notably, Java 5 on Windows 98).
*/
public class SystemTimer {
    
    // Number of frames in a row with no sleep before lowering sleep granularity
    private static final int FRAMES_BEFORE_SWITCH = 4;
    
    private Thread granularityThread;
    private long lastTime;
    private long virtualTime;
    private int framesInARowNoSleep = 0;
    private boolean sleptThisFrame = false;
    
    public void start() {
        lastTime = System.currentTimeMillis();
        virtualTime = 0;
    }
    
    public void stop() {
        // Do nothing
    }
    
    private final boolean getHighSleepGranularity() {
        return (granularityThread != null);
    }
    
    private void setHighSleepGranularity(boolean high) {
        if (high != getHighSleepGranularity()) {
            if (high) {
                startGranularityThread();
            }
            else {
                stopGranularityThread();
            }
        }
    }
    
    private final void startGranularityThread() {
        if (granularityThread == null) {
            // Improves the granularity of the sleep() function on Windows XP
            // Note: on some machines, time-of-day drift may occur if another thread hogs the
            // CPU
            // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6435126
            granularityThread = new Thread("PulpCore-Win32Granularity") {
                public void run() {
                    while (granularityThread == this) {
                        try {
                            Thread.sleep(Integer.MAX_VALUE);
                        }
                        catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                }
            };
            granularityThread.setDaemon(true);
            granularityThread.start();
        }
    }
    
    private final void stopGranularityThread() {
        if (granularityThread != null) {
            Thread t = granularityThread;
            granularityThread = null;
            t.interrupt();
        }
    }
    
    public long getTimeMillis() {
        long time = System.currentTimeMillis();
        if (time > lastTime) {
            virtualTime += time - lastTime;
        }
        lastTime = time;
        
        return virtualTime;
    }

    public long getTimeMicros() {
        return getTimeMillis() * 1000;
    }
    
    public String getName() {
        return "SystemTimer";
    }
    
    public void notifyFrameComplete() {
        if (CoreSystem.isWindowsXPorNewer()) {
            if (sleptThisFrame) {
                framesInARowNoSleep = 0;
            }
            else {
                framesInARowNoSleep++;
                // If we're maxing CPU, so disable high sleep granularity - it can cause
                // noticable time-of-day drift with max cpu.
                if (framesInARowNoSleep >= FRAMES_BEFORE_SWITCH && getHighSleepGranularity()) {
                    setHighSleepGranularity(false);
                }
            }
            sleptThisFrame = false;
        }
    }
    
    public long sleepUntilTimeMicros(long goalTimeMicros) {
        long yieldLimit = CoreSystem.isWindows() ? 2000 : 1000;
        while (true) {
            long currentTimeMicros = getTimeMicros();
            long diff = goalTimeMicros - currentTimeMicros;
            if (diff <= 0) {
                return currentTimeMicros;
            }
            else if (diff <= yieldLimit) {
                Thread.yield();
            }
            else {
                // We need to sleep, so make sure the sleep granularity is high
                if (CoreSystem.isWindowsXPorNewer()) {
                    setHighSleepGranularity(true);
                }
                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException ex) { }
            }
            // Only set if we didn't return immediately on the first check
            sleptThisFrame = true;
        }
    }
}