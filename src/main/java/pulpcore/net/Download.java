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

package pulpcore.net;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import pulpcore.Build;
import pulpcore.CoreSystem;

/**
    Downloads a file into memory. Handles partial HTTP downloads if the 
    first attempt downloads only a portion of the file.
*/
public class Download implements Runnable {
    
    public static final int IDLE = 0;
    public static final int DOWNLOADING = 1;
    public static final int SUCCESS = 2;
    public static final int CANCELED = 3;
    public static final int ERROR = 4;
    
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_UNSUCCESSFUL_ATTEMPTS = 10;
    private static final int TIME_BETWEEN_ATTEMPTS = 3000;
    
    private final Object lock = new Object();
    
    private int state;
    private long startTime;
    private int unsuccessfulAttempts;
    private boolean resetUnsuccessfulAttempts;
    
    private URL url;
    private int size;
    
    private int startPosition;
    private int currentPosition;
    private int restartPosition;
    
    private Throwable lastException;
    
    private byte[] data;
    
    /**
        Starts downloading a file from the server. Returns immediatly.
        @return a Download object, or null if the path is invalid.
    */
    public static Download startDownload(String file) {
        URL url;
        try {
            url = new URL(CoreSystem.getBaseURL(), file);
        }
        catch (MalformedURLException ex) {
            if (Build.DEBUG) CoreSystem.print("Bad url: " + file, ex);
            return null;
        }
        
        Download download = new Download(url);
        download.start();
        return download;
    }
  
    public Download(URL url) {
        this.url = url;
        state = IDLE;
        size = -1;
        startTime = -1;
        unsuccessfulAttempts = 0;
    }
    
    public void start() {
        
        Thread downloadThread;
        
        synchronized (lock) {
            if (state != IDLE) {
                throw new IllegalStateException();
            }
            setState(DOWNLOADING);
            downloadThread = new Thread(this, "PulpCore-Download");
            downloadThread.start();
        }
    }
    
    public void cancel() {
        synchronized (lock) {
            if (state == DOWNLOADING) {
                setState(CANCELED);
            }
        }
    }
    
    /**
        Returns the size of the file currently downloading, or -1 if not known.
    */
    public int getSize() {
        return size;
    }
    
    /**
        Returns the number of bytes downloaded.
    */
    public int getBytesRead() {
        return currentPosition;
    }
    
    /**
        Returns a values less than zero if the percent not known
    */
    public double getPercentDownloaded() {
        synchronized (lock) {
            if (size <= 0) {
                return -1;
            }
            else {
                return (double)currentPosition / size;
            }
        }
    }
    
    public Throwable getLastException() {
        return lastException;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public int getState() {
        return state;
    }
    
    private final void setState(int state) {
        this.state = state;
        if (state == ERROR) {
            data = null;
        }
    }
    
    public long getEstimatedTimeRemaining() {
        
        synchronized (lock) {
            if (size <= 0 || startTime == -1) {
                return -1;
            }
            
            float p = (float)(currentPosition - startPosition) / (size - startPosition);
            
            if (p == 0) {
                return -1;
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            return Math.round((1 - p) * elapsedTime / p);
        }
    }
    
    public void run() {
        
        while (true) {
            try {
                attemptDownload();
            }
            catch (FileNotFoundException ex) {
                if (Build.DEBUG) CoreSystem.print("File not found: " + url);
                lastException = ex;
                setState(ERROR);
                return;
            }
            catch (IOException ex) {
                if (Build.DEBUG) CoreSystem.print("Download", ex);
                lastException = ex;
                unsuccessfulAttempts++;
            }
            catch (Throwable t) {
                lastException = t;
                setState(ERROR);
                return;
            }
            
            if (unsuccessfulAttempts >= MAX_UNSUCCESSFUL_ATTEMPTS) {
                setState(ERROR);
                return;
            }
            
            if (state != DOWNLOADING) {
                return;
            }
            
            try {
                Thread.sleep(TIME_BETWEEN_ATTEMPTS);
            }
            catch (InterruptedException ex) { }
        }
    }
    
    private void attemptDownload() throws IOException {
        
        long newStartTime = System.currentTimeMillis();
        startTime = -1;
        
        URLConnection connection = url.openConnection();
        
        int newStartPosition = 0;
        resetUnsuccessfulAttempts = false;
        if (connection instanceof HttpURLConnection) {
            connection.setRequestProperty("Cache-Control", "no-transform");
            if (restartPosition > 0) {
                connection.setRequestProperty("Range", "bytes=" + restartPosition + "-");
            }
            
            // Connect
            try {
                connection.connect();
            }
            catch (Exception ex) {
                // SocketPermission: access denied?
                IOException ioex = new IOException("Connection Error");
                ioex.initCause(ex);
                throw ioex;
            }
        
            int responseCode = ((HttpURLConnection)connection).getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                boolean parsedContentRange = false;
                
                // Example: "Content-Range: bytes 21010-47021/47022"
                String contentRange = connection.getHeaderField("Content-Range");
                if (contentRange != null && contentRange.startsWith("bytes ")) {
                    int index = contentRange.indexOf("-");
                    if (index != -1) {
                        try {
                            newStartPosition = Integer.parseInt(contentRange.substring(6, index));
                            parsedContentRange = true;
                            if (newStartPosition == restartPosition) {
                                // Reset the counter if we actually download something
                                resetUnsuccessfulAttempts = true;
                            }
                        }
                        catch (NumberFormatException ex) {
                            // Ignore
                        }
                    }
                }
                
                if (!parsedContentRange) {
                    restartPosition = 0;
                    data = null;
                    setErrorMessage("Couldn't parse Content-Range");
                    return;
                }
            }
            else if (responseCode != HttpURLConnection.HTTP_OK) {
                // Permanent Error - don't automatically retry
                setState(ERROR);
                setErrorMessage("HTTP response code " + responseCode);
                return;
            }
        }
        
        synchronized (lock) {
            startPosition = newStartPosition;
            currentPosition = newStartPosition;
            startTime = newStartTime;
            
            int contentLength = connection.getContentLength();
            if (contentLength > 0) {
                size = startPosition + contentLength;
            }
            else {
                size = -1;
            }
            
            markRestartPosition();
        }
        
        InputStream in = connection.getInputStream();
        boolean doubleCheck = false;
        
        try {
            doubleCheck = downloadData(in);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ex) {
                // ignore
            }
        }
        
        if (state == DOWNLOADING && data != null && doubleCheck) {
            // At this point, the connection is closed, but we don't know if it was closed 
            // because the file was downloaded successfully or because there was a connection error.
            // If a HEAD request returns successfully, assume the file was downloaded
            // successfully.
            connection = url.openConnection();
            
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection)connection;
                http.setRequestMethod("HEAD");
                http.setRequestProperty("Cache-Control", "no-transform");
                http.connect();
                if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    int contentLength = http.getContentLength();
                    if (data.length == contentLength) {
                        setState(SUCCESS);
                        return;
                    }
                    else if (contentLength <= 0) {
                        // If contentLength <= 0, we don't know if the download
                        // is complete. A proxy is mangling the server response, because
                        // the content-length should be the size of the file with a HEAD request.
                        // We're going to guess the download is complete.
                        setState(SUCCESS);
                        return;
                    }
                }
            }
            else {
                // Assume OK
                setState(SUCCESS);
                return;
            }
            
            // Permanent Error - don't automatically retry
            // (Automatically retrying can result in an infinite GET/HEAD loop for really 
            // fucked up proxies)
            setState(ERROR);
            setErrorMessage("HEAD request resulted in an unknown State");
        }
    }
    
    private final void setErrorMessage(String error) {
        lastException = new IOException(error);
        if (Build.DEBUG) {
            CoreSystem.print(url.toString());
            CoreSystem.print(error);
        }
    }
    
    private final int getCurrentPosition() {
        return currentPosition;
    }
    
    private final void increasePosition(int numBytes) {
        if (numBytes > 0) {
            currentPosition += numBytes;
            if (resetUnsuccessfulAttempts) {
                unsuccessfulAttempts = 0;
                resetUnsuccessfulAttempts = false;
            }
        }
    }
    
    private final void markRestartPosition() {
        restartPosition = currentPosition;
    }
    
    /**
        @return true if the data length should be double-checked with a head request.
    */
    private boolean downloadData(InputStream in) throws IOException {

        if (size == -1) {
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            
            if (startPosition > 0) {
                if (data == null || startPosition > data.length) {
                    // Permanent Error - don't automatically retry
                    setState(ERROR);
                    setErrorMessage("Bad Content-Range");
                    return false;
                }
                out.write(data, 0, startPosition);
            }
            
            data = null;
            byte[] buffer = new byte[BUFFER_SIZE];
            
            try {
                while (state == DOWNLOADING) {
                    int bytesRead = in.read(buffer);
                    
                    if (bytesRead == -1) {
                        // Either the download is finished, or there was a connection error
                        return true;
                    }
                    else if (bytesRead > 0) {
                        increasePosition(bytesRead);
                        markRestartPosition();
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
            finally {
                data = out.toByteArray();
            }
        }
        else {
            if (data == null) {
                data = new byte[size];
            }
            else if (data.length < size) {
                byte[] newData = new byte[size];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            }
            
            while (state == DOWNLOADING) {
                int pos = getCurrentPosition();
                
                int bytesRead = in.read(data, pos, Math.min(BUFFER_SIZE, size - pos));
                
                if (bytesRead > 0) {
                    increasePosition(bytesRead);
                    markRestartPosition();
                }
                
                if (getCurrentPosition() >= size) {
                    setState(SUCCESS);
                }
                else if (bytesRead == -1) {
                    throw new IOException("Closed Prematurely");
                }
            }
        }
        return false;
    }
}
