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

/**
    Attempts to compensate for the 55ms granularity on Windows 95/98 and 15ms
    on Windows XP/NT/2000.
*/
public class Win32Timer extends SystemTimer implements Runnable {
    
    private static final int NUM_SAMPLES_BITS = 6; // 64 samples
    private static final int NUM_SAMPLES = 1 << NUM_SAMPLES_BITS;
    private static final int NUM_SAMPLES_MASK = NUM_SAMPLES - 1;
    
    /*
        5 ms is the minimum value that the MS VM will use to sleep. Values
        less than 5 ms will be ignored.
    */
    private static final int SLEEP_TIME = 5;

    private long[] samples;
    private int numSamples;
    private int firstIndex;
    
    private long estTime;
    private long lastTime;
    
    private Thread timerThread;
    
    public String getName() {
        return "Win32Timer";
    }
    
    public void start() {
        super.start();
        timerThread = null;
        
        numSamples = 0;
        firstIndex = 0;
        samples = new long[NUM_SAMPLES];
        estTime = System.currentTimeMillis() * 1000;
        lastTime = estTime;
        
        timerThread = new Thread(this, "PulpCore-Win32Timer");
        timerThread.setDaemon(true);
        timerThread.setPriority(Thread.MAX_PRIORITY);
        timerThread.start();
    }
    
    public void stop() {
        super.stop();
        timerThread = null;
    }
    
    public long getTimeMillis() {
        return getTimeMicros() / 1000;
    }
    
    public long getTimeMicros() {
        return Math.max(estTime, System.currentTimeMillis() * 1000);
    }
    
    public long sleepUntilTimeMicros(long timeMicros) {
        while (true) {
            // Typical granularity on Windows seems to be about 10ms,
            // so return if the time is within 5ms.
            long currentTimeMicros = getTimeMicros();
            if (currentTimeMicros >= timeMicros - 5000) {
                return currentTimeMicros;
            }
            
            synchronized (this) {
                try {
                     wait(100);
                }
                catch (InterruptedException ex) { }
            }
        }
    }
    
    public void run() {
        
        Thread thisThread = Thread.currentThread();
        
        while (timerThread == thisThread) {
            try {
                Thread.sleep(SLEEP_TIME);
            }
            catch (InterruptedException ex) { }
            
            update();
        }
    }
    
    private void update() {
        synchronized (this) {
            long time = System.currentTimeMillis();
            
            if (time > lastTime) {
                // Use the newly updated system time
                estTime = Math.max(estTime, time * 1000L);
            }
            else if (numSamples > 0) {
                // Make an estimate on how much time has passed since the last update.
                estTime += 1000L * (time - getFirstSample()) / numSamples;
            }
            else {
                estTime = time * 1000L;
            }
            
            addSample(time);
            lastTime = time;
        
            notifyAll();
        }
    }
    
    private void addSample(long sample) {
        
        samples[(firstIndex + numSamples) & NUM_SAMPLES_MASK] = sample;
        
        if (numSamples == NUM_SAMPLES) {
            firstIndex = (firstIndex + 1) & NUM_SAMPLES_MASK;
        }
        else {
            numSamples++;
        }
    }
    
    private long getFirstSample() {
        return samples[firstIndex];
    }
}