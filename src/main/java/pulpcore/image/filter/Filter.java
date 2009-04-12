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
package pulpcore.image.filter;

import pulpcore.image.CoreImage;

/**
    Base class for image filters. Subclasses override the
    {@link #filter(pulpcore.image.CoreImage, pulpcore.image.CoreImage) } method.
    @see pulpcore.sprite.Sprite#setFilter(pulpcore.image.filter.Filter)
*/
public abstract class Filter {

    private CoreImage input = null;
    private CoreImage output = null;
    private boolean isDirty = true;

    /**
        Gets the x offset the output image should display relative to the input.
     */
    public int getX() {
        return 0;
    }

    /**
        Gets the y offset the output image should display relative to the input.
     */
    public int getY() {
        return 0;
    }

    /**
        Gets the width of the output of this filter. By default, the width
        is the same as the input's width.
     */
    public int getWidth() {
        if (input != null) {
            return input.getWidth();
        }
        else {
            return 0;
        }
    }

    /**
        Gets the height of the output of this filter. By default, the height
        is the same as the input's height.
     */
    public int getHeight() {
        if (input != null) {
            return input.getHeight();
        }
        else {
            return 0;
        }
    }

    /**
        Returns true if the output of this filter is opaque. By default, the output is
        opaque if the input is opaque.
     */
    public boolean isOpaque() {
        if (input != null) {
            return input.isOpaque();
        }
        else {
            return false;
        }
    }

    /**
        Updates the filter. The default method does nothing.
    */
    public void update(int elapsedTime) {

    }

    /**
        Performs this filter on the input image onto the specified output image. 
     <p>
        This method is called from {@link #getOutput()} if {@link #isDirty()} returns true.
        The output image will be the same dimensions as
        ({@link #getWidth()} x {@link #getHeight() }. Implementors must ensure
        that every pixel in {@code output} is drawn.
     */
    protected abstract void filter(CoreImage input, CoreImage output);

    // Unfortunately clone() can't be used because the reference to properties would be
    // created (instead of clones of the properties). This would normally be desired,
    // except this means property.update() would be called multiple times per frame.
    // Plus, output, dirty, etc. would be cloned.

    /**
        Creates a copy of the Filter for another Sprite to use. The input filter is also copied.
        The properties of the copy, if any, will be bound to the original filter's properties.
        <p>
        This method is used by the Sprite class. Most apps will not need to call this method.
        <p>
        Subclasses should bind all properties of the cloned
        object using bindWithInverse(). For example, for the HSBAdjust filter:
        <pre>
        public Filter copy() {
            HSBAdjust copy = new HSBAdjust();
            copy.hue.bindWithInverse(hue);
            copy.brightness.bindWithInverse(brightness);
            copy.saturation.bindWithInverse(saturation);
            return copy;
        }
        </pre>
    */
    public abstract Filter copy();

    //
    // Final methods
    //

    /* package private */ void notifyInputChanged() {
    }

    /**
        Performs this filter on the input image onto a newly created output image. The current
        input image, if any, is ignored.
     */
    public final CoreImage filter(CoreImage input) {
        CoreImage oldInput = getInput();
        // Set input so that getWidth(), etc. is correct.
        setInput(input);
        CoreImage newOutput = new CoreImage(getWidth(), getHeight(), isOpaque());
        filter(input, newOutput);
        setInput(oldInput);
        return newOutput;
    }

    /**
        Sets the filter input.
     */
    public final void setInput(CoreImage input) {
        if (this.input != input) {
            this.input = input;
            setDirty();
            notifyInputChanged();
            update(0);
        }
    }

    public final CoreImage getInput() {
        return input;
    }

    /* package private */ CoreImage getUnfilteredOutput() {
        int w = getWidth();
        int h = getHeight();
        if (output == null ||
            output.getWidth() != w ||
            output.getHeight() != h ||
            output.isOpaque() != isOpaque())
        {
            output = new CoreImage(w, h, isOpaque());
            //pulpcore.CoreSystem.print("New output for " + getClass().getName() + ": " + this.output.getWidth() + "x" + this.output.getHeight());
            setDirty();
        }
        return output;
    }

    /**
        Gets the filtered output image.
    */
    public final CoreImage getOutput() {
        CoreImage out = getUnfilteredOutput();
        if (isDirty()) {
            filter(input, out);
            //pulpcore.CoreSystem.print("Filtered: " + getClass().getName());
            setDirty(false);
        }
        return out;
    }

    /* package private */ boolean isChildDirty() {
        return false;
    }

    /* package private */ boolean isDirtyFlagSet() {
        return isDirty;
    }

    public final boolean isDirty() {
        return isDirtyFlagSet() || isChildDirty();
    }

    public final void setDirty() {
        setDirty(true);
    }

    private void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }
}