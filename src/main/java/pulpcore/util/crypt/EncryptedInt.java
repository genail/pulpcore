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

package pulpcore.util.crypt;

import java.io.EOFException;
import pulpcore.animation.Int;
import pulpcore.animation.Property;
import pulpcore.animation.PropertyListener;
import pulpcore.util.ByteArray;

/**
    An EncryptedInt is an {@link pulpcore.animation.Int } whose value is internally encrypted. This
    class is usefull for protecting against runtime memory modifications from tools like 
    Cheat Engine.
    For a game with global high scores, typical encrypted values might be the score and the
    game level.
*/
public class EncryptedInt extends Int implements PropertyListener {
    
    private ARC4 cipher;
    private ByteArray buffer = new ByteArray(4);
    private int cryptedValue;
    
    /**
        Constructs a new Int object with no listener, the value of zero, and a cipher
        with a randomly generated encryption key.
    */
    public EncryptedInt() {
        this(null, 0, new ARC4());
    }
    
    /**
        Constructs a new Int object with no listener, the value of zero, and the specified
        cipher.
    */
    public EncryptedInt(ARC4 cipher) {
        this(null, 0, cipher);
    }

    /**
        Constructs a new Int object with the specified listener, the value of zero, and a cipher
        with a randomly generated encryption key.
    */
    public EncryptedInt(PropertyListener listener) {
        this(listener, 0, new ARC4());
    }
    
    /**
        Constructs a new Int object with the specified listener, the value of zero, and the 
        specified cipher.
    */
    public EncryptedInt(PropertyListener listener, ARC4 cipher) {
        this(listener, 0, cipher);
    }
    
    /**
        Constructs a new Int object with the specified value, no listener, and a cipher
        with a randomly generated encryption key.
    */
    public EncryptedInt(int value) {
        this(null, value, new ARC4());
    }
    
    /**
        Constructs a new Int object with the specified value, no listener, and the 
        specified cipher.
    */
    public EncryptedInt(int value, ARC4 cipher) {
        this(null, value, cipher);
    }
    
    /**
        Constructs a new Int object with the specified value, the specified listener, and a cipher
        with a randomly generated encryption key.
    */
    public EncryptedInt(PropertyListener listener, int value) {
        this(listener, value, new ARC4());
    }
    
    /**
        Constructs a new Int object with the specified value, the specified listener, and the 
        specified cipher.
    */
    public EncryptedInt(PropertyListener listener, int value, ARC4 cipher) {
        super(listener, value);
        this.cipher = cipher;
        propertyChange(this);
        addListener(this);
    }
    
    public void propertyChange(Property property) {
        cryptedValue = crypt(getValue());
    }
    
    public int get() {
        return crypt(cryptedValue);
    }
    
    private int crypt(int d) {
        buffer.reset();
        buffer.writeInt(d);
        buffer.crypt(cipher);
        
        buffer.reset();
        return buffer.readInt();
    }
    
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    
    public int hashCode() {
        return super.hashCode();
    }
}
