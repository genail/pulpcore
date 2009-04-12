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

import java.util.ArrayList;
import java.util.List;
import pulpcore.image.CoreImage;


public class FilterChain extends Filter {

    private final List list = new ArrayList();
    private Identity identity = null;

    public FilterChain() {

    }

    public FilterChain(Filter filter) {
        list.add(filter);
        chain();
    }

    public FilterChain(Filter filter1, Filter filter2) {
        list.add(filter1);
        list.add(filter2);
        chain();
    }

    public FilterChain(Filter filter1, Filter filter2, Filter filter3) {
        list.add(filter1);
        list.add(filter2);
        list.add(filter3);
        chain();
    }

    public FilterChain(Filter filter1, Filter filter2, Filter filter3, Filter filter4) {
        list.add(filter1);
        list.add(filter2);
        list.add(filter3);
        list.add(filter4);
        chain();
    }

    public FilterChain(Filter[] filters) {
        for (int i = 0; i < filters.length; i++) {
            list.add(filters[i]);
        }
        chain();
    }

    public FilterChain(List filters) {
        list.addAll(filters);
        chain();
    }

    public Filter get(int i) {
        return (Filter)list.get(i);
    }

    public void add(Filter filter) {
        list.add(filter);
        chain();
    }

    public void add(int i, Filter filter) {
        list.add(i, filter);
        chain();
    }

    public void set(int i, Filter filter) {
        if (list.get(i) != filter) {
            list.set(i, filter);
            chain();
        }
    }

    public void remove(int i) {
        list.remove(i);
        chain();
    }

    public int size() {
        return list.size();
    }

    private void chain() {
        CoreImage input = getInput();
        for (int i = 0; i < list.size(); i++) {
            Filter f = (Filter)list.get(i);
            f.setInput(input);
            input = f.getUnfilteredOutput();
        }
    }

    /* package private */ void notifyInputChanged() {
        chain();
    }

    /* package private */ CoreImage getUnfilteredOutput() {
        return last().getUnfilteredOutput();
    }

    private Filter last() {
        if (list.size() == 0) {
            if (identity == null) {
                identity = new Identity();
            }
            identity.setInput(getInput());
            return identity;
        }
        else {
            return (Filter)list.get(list.size() - 1);
        }
    }

    public int getX() {
        chain();
        int x = 0;
        for (int i = 0; i < list.size(); i++) {
            x += ((Filter)list.get(i)).getX();
        }
        return x;
    }

    public int getY() {
        chain();
        int y = 0;
        for (int i = 0; i < list.size(); i++) {
            y += ((Filter)list.get(i)).getY();
        }
        return y;
    }

    public int getWidth() {
        chain();
        return last().getWidth();
    }

    public int getHeight() {
        chain();
        return last().getHeight();
    }

    public boolean isOpaque() {
        chain();
        return last().isOpaque();
    }

    public void update(int elapsedTime) {
        for (int i = 0; i < list.size(); i++) {
            ((Filter)list.get(i)).update(elapsedTime);
        }
    }

    /* package private */ boolean isChildDirty() {
        for (int i = 0; i < list.size(); i++) {
            if (((Filter)list.get(i)).isDirty()) {
                return true;
            }
        }
        return false;
    }

    protected void filter(CoreImage input, CoreImage output) {
        if (list.size() == 0) {
            last().filter(input, output);
        }
        else if (input == getInput() && output == getUnfilteredOutput()) {
            boolean inputDirty = isDirtyFlagSet();
            for (int i = 0; i < list.size(); i++) {
                Filter f = (Filter)list.get(i);
                if (inputDirty) {
                    f.setDirty();
                }
                f.setInput(input);
                if (!inputDirty && f.isDirty()) {
                    inputDirty = true;
                }
                input = f.getOutput();
            }
        }
        else {
            for (int i = 0; i < list.size(); i++) {
                Filter f = (Filter)list.get(i);
                CoreImage oldInput = f.getInput();
                f.setInput(input);
                CoreImage newOutput;
                if (i == list.size() - 1) {
                    newOutput = output;
                }
                else {
                    newOutput = new CoreImage(f.getWidth(), f.getHeight(), f.isOpaque());
                }
                f.filter(input, newOutput);
                f.setInput(oldInput);
                input = newOutput;
            }
        }
    }

    public Filter copy() {
        ArrayList listCopy = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            listCopy.add(((Filter)list.get(i)).copy());
        }
        return new FilterChain(listCopy);
    }

}
