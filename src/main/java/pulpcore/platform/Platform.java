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


/**
    The Platform interface contains various platform-specific methods used 
    internally by PulpCore. 
    Most applications should not call any of the methods in this class directly. 
*/
public interface Platform {
    
    public AppContext getThisAppContext();
    
    /**
        Returns the current value of the system timer in milliseconds. 
    */
    public long getTimeMillis();
    
    /**
        Returns the current value of the system timer in microseconds. 
    */
    public long getTimeMicros();
    
    /**
        Sleeps until the specified time, in microseconds, occurs.
        This is an absolute version of Thread.sleep().
        @return the current time in microseconds (as near timeMicros as possible).
    */
    public long sleepUntilTimeMicros(long timeMicros);    
    
    /**
        Checks if the platform has access to the native operating system
        clipboard. If not, an application-only clipboard is used.
    */
    public boolean isNativeClipboard();
    
    /**
        Returns the text currently in the clipboard. Returns an empty string
        if there is no text in the clipboard.
    */
    public String getClipboardText();
    
    /**
        Sets the text in the clipboard.
    */
    public void setClipboardText(String text);
    
    public boolean isSoundEngineCreated();
    
    public SoundEngine getSoundEngine();
    
    public void updateSoundEngine(int timeUntilNextUpdate);
    
    /**
        Returns true if this platform is hosted in a browser (Applets)
    */
    public boolean isBrowserHosted();
    
    /**
        Returns the name of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserName();
    
    /**
        Returns the version of the web browser, or null if the browser name could not be determined.
    */
    public String getBrowserVersion();
}