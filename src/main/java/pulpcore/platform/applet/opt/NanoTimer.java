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

package pulpcore.platform.applet.opt;

import pulpcore.platform.applet.SystemTimer;

/**
    The NanoTimer class uses System.nanoTime() for Applets running on
    Java 1.5 or newer.
    <p>
    Some nanoTime implementations (Win XP, AMD Dual-core) return a different time depending on 
    which CPU the method is called on. This code attempts to guess which timer the nanoTime value 
    comes from.
*/
public class NanoTimer extends SystemTimer {
    
    private static final int NUM_TIMERS = 8;
    private static final long ONE_SEC = 1000000000L; // nanoseconds
    private static final long MAX_DIFF = ONE_SEC; 
    private static final long NEVER_USED = -1; 
    private static final long DEFAULT_FAIL_RESET_TIME = ONE_SEC;
    
    private long[] lastTimeStamps = new long[NUM_TIMERS];
    private long[] timeSinceLastUsed = new long[NUM_TIMERS];
    
    private long virtualNanoTime;
    private int timesInARowNewTimerChosen;
    private long lastDiff;
    private long failTime;
    private long failResetTime;
    
    public NanoTimer() {
        virtualNanoTime = 0;
        failResetTime = DEFAULT_FAIL_RESET_TIME;
        reset();
    }
    
    private void reset() {
        failTime = 0;
        lastDiff = 0;
        timesInARowNewTimerChosen = 0;
        for (int i = 0; i < NUM_TIMERS; i++) {
            timeSinceLastUsed[i] = NEVER_USED;
        }
    }
    
    public String getName() {
        return "NanoTimer";
    }
    
    private long nanoTime() {
        long diff;
        
        if (timesInARowNewTimerChosen >= NUM_TIMERS) {
            long nanoTime = System.currentTimeMillis() * 1000000;
            diff = nanoTime - lastTimeStamps[0];
            
            failTime += diff;
            if (failTime >= failResetTime) {
                // Maybe thrashing or system hibernation caused the problem - try again
                reset();
                // But, increase the reset time
                failResetTime *= 2;
            }
        }
        else {  
            long nanoTime = System.nanoTime();
    
            // Find which timer the nanoTime value came from 
            int bestTimer = -1;
            long bestDiff = 0;
            for (int i = 0; i < NUM_TIMERS; i++) {
                if (timeSinceLastUsed[i] != NEVER_USED) {
                    long t = lastTimeStamps[i] + timeSinceLastUsed[i];
                    long timerDiff = nanoTime - t;
                    if (timerDiff > 0 && timerDiff < MAX_DIFF) {
                        if (bestTimer == -1 || timerDiff < bestDiff) {
                            bestTimer = i;
                            bestDiff = timerDiff;
                        }
                    }
                }
            }
            
            // No best timer found
            if (bestTimer == -1) {
                // Use last good diff
                diff = lastDiff;
                
                // Find a new timer 
                bestTimer = 0;
                for (int i = 0; i < NUM_TIMERS; i++) {
                    if (timeSinceLastUsed[i] == NEVER_USED) {
                        // This timer never used - use it
                        bestTimer = i;
                        break;
                    }
                    else if (timeSinceLastUsed[i] > timeSinceLastUsed[bestTimer]) {
                        // Least used timer so far, but keep looking
                        bestTimer = i;
                    }
                }
                timesInARowNewTimerChosen++;
            }
            else {
                // Success!
                timesInARowNewTimerChosen = 0;
                failResetTime = DEFAULT_FAIL_RESET_TIME;
                diff = nanoTime - lastTimeStamps[bestTimer] - timeSinceLastUsed[bestTimer];
                
                // Set lastDiff if this same timer used twice in a row
                if (timeSinceLastUsed[bestTimer] == 0) {
                    lastDiff = diff;
                }
            }
            
            lastTimeStamps[bestTimer] = nanoTime;
            timeSinceLastUsed[bestTimer] = 0;
            
            // Increment usage of all other timers
            for (int i = 0; i < NUM_TIMERS; i++) {
                if (i != bestTimer && timeSinceLastUsed[i] != NEVER_USED) {
                    timeSinceLastUsed[i] += diff;
                }
            }
            
            // Check for total failure
            if (timesInARowNewTimerChosen >= NUM_TIMERS) {
                lastTimeStamps[0] = System.currentTimeMillis() * 1000000;
            }
        }
        
        virtualNanoTime += diff;
        
        return virtualNanoTime;
    }
    
    public long getTimeMillis() {
        return nanoTime() / 1000000;
    }
    
    public long getTimeMicros() {
        return nanoTime() / 1000;
    }
}