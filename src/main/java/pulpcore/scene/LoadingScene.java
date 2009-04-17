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

package pulpcore.scene;

import java.io.IOException;
import pulpcore.animation.Timeline;
import pulpcore.Assets;
import pulpcore.Build;
import pulpcore.CoreSystem;
import pulpcore.image.Colors;
import pulpcore.image.CoreFont;
import pulpcore.net.Download;
import pulpcore.platform.ConsoleScene;
import pulpcore.sprite.Button;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.Label;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;
import pulpcore.util.StringUtil;

/**
    A scene that downloads the asset catalog file (zip file). It automatically handles download
    errors by allowing the user to retry a failed download.
    <p>
    The default implementation creates a solid-colored background with a progress bar in
    the middle of the stage. Subclasses can change this appearance by invoking 
    {@code getMainLayer().removeAll()} and adding other visual elements.
*/
public class LoadingScene extends Scene2D {
    
    private static final String ERROR_MESSAGE = "Oops! A download error occurred.";
    private static final String TRY_AGAIN = "Try Again";
    
    private static final int FADE_OUT_DURATION = 300;
    
    private static final int PROGRESS_BAR_WIDTH = 100 + 4;
    private static final int PROGRESS_BAR_HEIGHT = 10;
    
    private final String assetCatalogFile;
    private final int backgroundColor;
    private final CoreFont font;
    private final Sprite progressBar;
    private final Sprite progressBarBackground;
    private Button tryAgainButton;
    
    private Timeline onCompletion;
    
    private Group normalLayer;
    private Group errorLayer;
    
    private int totalTime;
    private boolean showProgress;
    private int state;
   
    private Download download;
    
    public LoadingScene(String assetCatalogFile) {
        this(assetCatalogFile, null, CoreSystem.getDefaultBackgroundColor(), null);
    }
    
    public LoadingScene(String assetCatalogFile, Scene nextScene) {
        this(assetCatalogFile, nextScene, CoreSystem.getDefaultBackgroundColor(), null);
    }
    
    public LoadingScene(String assetCatalogFile, Scene nextScene, int backgroundColor, 
        CoreFont font) 
    {
        this.assetCatalogFile = assetCatalogFile;
        this.backgroundColor = backgroundColor;
        
        if (font == null) {
            font = CoreFont.getSystemFont().tint(CoreSystem.getDefaultForegroundColor());
        }
        this.font = font;
        
        normalLayer = getMainLayer();
        errorLayer = new Group();
        errorLayer.visible.set(false);
        addLayer(errorLayer);
        
        showProgress = false;
        
        // Add the default view - plain background and a progress bar
        normalLayer.add(new FilledSprite(backgroundColor));
        int w = PROGRESS_BAR_WIDTH;
        int h = PROGRESS_BAR_HEIGHT;
        int x = Stage.getWidth() / 2 - w / 2;
        int y = Stage.getHeight() / 2;
        int progressBarColor;
        if (backgroundColor == CoreSystem.getDefaultBackgroundColor()) {
            progressBarColor = CoreSystem.getDefaultForegroundColor();
        }
        else {
            progressBarColor = 0x00ffffff ^ backgroundColor;
        }
        progressBarBackground = new FilledSprite(x, y, w, h, 
            Colors.rgba(progressBarColor, 56));
        progressBarBackground.visible.set(false);
        progressBar = new FilledSprite(x + 2, y + 2, w - 4, h - 4, progressBarColor);
        progressBar.visible.set(false);
        normalLayer.add(progressBarBackground);
        normalLayer.add(progressBar);
        
        // Default error message
        setErrorMessage(ERROR_MESSAGE, TRY_AGAIN);
        
        if (nextScene != null) {
            Timeline timeline = new Timeline();
            timeline.animate(progressBarBackground.alpha, 255, 0, FADE_OUT_DURATION);
            timeline.animate(progressBar.alpha, 255, 0, FADE_OUT_DURATION);
            timeline.setScene(nextScene, FADE_OUT_DURATION);
            onCompletion(timeline);
        }
    }
    
    /**
        Sets the timeline to start once the download is complete. Normally, the 
        timeline includes a SetScene event.
    */
    public void onCompletion(Timeline timeline) {
        if (onCompletion != null) {
            removeTimeline(onCompletion, true);
        }
        onCompletion = timeline;
        if (onCompletion != null) {
            onCompletion.stop();
            addTimeline(onCompletion);
        }
    }
    
    public void setErrorMessage(String message, String retryButtonText) {
        
        errorLayer.removeAll();
        errorLayer.add(new FilledSprite(backgroundColor));
        
        String[] strings = StringUtil.wordWrap(message, font, Stage.getWidth() * 4 / 5);
        
        int y = Stage.getHeight()/4;
        for (int i = 0; i < strings.length; i++) {
            Label label = new Label(font, strings[i], Stage.getWidth() / 2, y);
            label.setAnchor(0.5, 0);
            y += label.height.getAsInt() + 2;
            errorLayer.add(label);
        }
        
        tryAgainButton = Button.createLabeledButton(retryButtonText,  
            Stage.getWidth() / 2,
            Stage.getHeight() * 3 / 4);
        tryAgainButton.setAnchor(0.5, 1);
        errorLayer.add(tryAgainButton);
    }
    
    public void load() {
        startDownload();
    }
    
    public void unload() {
        super.unload();
        if (download != null) {
            download.cancel();
            download = null;
        }
    }
    
    private void startDownload() {
        normalLayer.visible.set(true);
        errorLayer.visible.set(false);
                
        totalTime = 0;
        if (Assets.containsCatalog(assetCatalogFile)) {
            state = Download.SUCCESS;
            download = null;
        }
        else if (download == null || 
            download.getState() == Download.ERROR ||
            download.getState() == Download.CANCELED) 
        {
            showProgress = false;
            download = Download.startDownload(assetCatalogFile);
            if (download == null) {
                state = Download.ERROR;
            }
        }
    }

    /**
        Subclasses should call super.update() to handing downloading success and error.
    */
    public void update(int elapsedTime) {
        
        if (download != null) {
            state = download.getState();
        }
        
        if (state == Download.DOWNLOADING) {
            totalTime += elapsedTime;
                
            if (!showProgress) {
                // Show progress bar if there is more than 1 second left to  
                // download after downloading for at least 200 milliseconds.
                if (totalTime >= 200 && download.getEstimatedTimeRemaining() >= 1000) {
                    showProgress = true;
                }
                // Show progress bar if the content size is unknown and 1KB
                // has been downloaded
                if (download.getBytesRead() >= 1024 && download.getSize() == -1) {
                    showProgress = true;
                }
            }
        }
        else if (state == Download.SUCCESS) {
            
            boolean success = (download == null || 
                Assets.addCatalog(assetCatalogFile, download.getData()));
            
            if (success) {
                totalTime += elapsedTime;
                CoreSystem.setTalkBackField("pulpcore.loadtime." + assetCatalogFile, 
                    Integer.toString(totalTime));
                totalTime = 0;
                
                if (onCompletion != null && !onCompletion.isPlaying()) {
                    onCompletion.play();
                }
                state = Download.IDLE;
            }
            else {
                state = Download.ERROR;
            }
            download = null;
        }
        
        if (state == Download.ERROR) {
            
            if (Build.DEBUG && download != null && 
                !(download.getLastException() instanceof IOException)) 
            {
                CoreSystem.print("Asset catalog error", download.getLastException());
                Stage.setScene(new ConsoleScene());
            }
            
            normalLayer.visible.set(false);
            errorLayer.visible.set(true);
            if (tryAgainButton.isClicked()) {
                startDownload();
            }
        }
        
        // Default progress bar view
        double p = getProgress();
        if (shouldProgressBeVisible() && download != null && p >= 0) {
            progressBar.visible.set((p > 0));
            if (p > 0) {
                progressBar.width.set(p * (progressBarBackground.width.get() - 4));    
            }
            progressBarBackground.visible.set(true);
        }
        else {
            progressBar.visible.set(false);
            progressBarBackground.visible.set(false);
        }
    }
    
    public boolean shouldProgressBeVisible() {
        return showProgress;
    }
    
    /**
        Returns a value from 0 to 1. If the download hasn't started,
        returns a value less than 0.
    */
    public double getProgress() {
        if (state == Download.SUCCESS) {
            return 1;
        }
        if (state == Download.IDLE && Assets.containsCatalog(assetCatalogFile)) {
            return 1;
        }
            
        if (download == null) {
            return showProgress ? 0 : -1;
        }
        double p = download.getPercentDownloaded();
        
        if (p < 0 && download.getSize() == -1 && totalTime > 0) {
            // Content length unknown. Use a 30 second half life
            p = 1 - Math.pow(0.5, totalTime / 30000.0);
        }
        
        if (p < 0) {
            return showProgress ? 0 : -1;
        }
        else {
            return p;
        }
    }
}
