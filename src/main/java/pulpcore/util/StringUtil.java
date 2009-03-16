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

package pulpcore.util;

import java.util.ArrayList;
import java.util.List;
import pulpcore.animation.Fixed;
import pulpcore.animation.Int;
import pulpcore.image.CoreFont;
import pulpcore.math.CoreMath;

/**
    Provides utilities for manipulating and formatting text strings.
*/
public final class StringUtil {
    
    // Prevent instantiation
    private StringUtil() { }
    
    public static String rtrim(String s) {
        int off = s.length() - 1;
        while (off >= 0 && s.charAt(off) <= ' ') {
            off--;
        }
            
        return off < s.length() - 1 ? s.substring(0, off+1) : s;
    }
    
    public static String ltrim(String s) {
        int off = 0;
        while (off < s.length() && s.charAt(off) <= ' ') {
            off++;
        }
            
        return off > 0 ? s.substring(off) : s;
    }
    
    /**
        Calls {@code StringUtil.format(message, new Object[] { arg });}
    */
    public static String format(String message, Object arg) {
        return format(message, new Object[] { arg });
    }
    
    /**
        Formats a text string converting codes with string arguments. 
        <p>
        <table cellspacing="2">
        <tr>
            <td><b>Conversion</b></td>
            <td><b>Description</b></td>
        </tr>
        <tr>
            <td>%s</td>
            <td>Any object converted to a string. ("Hello World")</td>
        </tr>
        <tr>
            <td>%d</td>
            <td>An integer - Integer, Int, or Fixed (rounded). ("1000")</td>
        </tr>
        <tr>
            <td>%,d</td>
            <td>An integer with a grouping seperator. ("1,000")</td>
        </tr>
        <tr>
            <td>%f</td>
            <td>Fixed-point number. ("1000.12345")</td></tr>
        <tr>
            <td>%,f</td>
            <td>Fixed-point number with a grouping seperator. ("1,000.12345")</td>
        </tr>
        <tr>
            <td>%.2f</td>
            <td>Fixed-point number with a specific number of digits after the decimal 
            ("1000.12")</td>
        </tr>
        <tr>
            <td>%,.2f</td>
            <td>Fixed-point number with a grouping seperator and a specific number of digits 
            ("1,000.12")</td>
        </tr>
        <tr>
            <td>%%</td>
            <td>The percent character.</td>
        </tr>
        </table>
        <p>
        
        Example:
          format("Name: %s  Score: %,d", new Object[] {"Fred", new Integer(1000)});
        returns
          "Name: Fred  Score: 1,000"
        
    */
    public static String format(String message, Object[] args) {
        if (message == null) {
            return null;
        }
        else if (args == null || args.length == 0) {
            return message;
        }
        
        StringBuffer buffer = new StringBuffer();
        int lastIndex = 0;
        int currentArg = 0;
        
        while (true) {
            int index = message.indexOf('%', lastIndex);
            
            if (index == -1) {
                break;
            }
            
            buffer.append(message.substring(lastIndex, index));
            
            boolean grouping = false;
            boolean decimal = false;
            boolean found = false;
            int numFracDigits = -1;
            int codeIndex = index + 1;
            while (!found) {
                if (message.length() <= codeIndex) {
                    buffer.append(message.substring(index, codeIndex));
                    break;
                }
                
                char ch = message.charAt(codeIndex++);
                if (ch == '%') {
                    buffer.append('%');
                    break;
                }
                else if (ch == ',') {
                    grouping = true;
                }
                else if (ch == '.') {
                    numFracDigits = 0;
                    decimal = true;
                }
                else if (ch == 's') {
                    found = true;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    buffer.append((String)arg);
                }
                else if (ch == 'd') {
                    found = true;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    if (arg instanceof Integer) {
                        int value = ((Integer)arg).intValue();
                        buffer.append(CoreMath.intToString(value, 0, 0, grouping));
                    }
                    else if (arg instanceof Int) {
                        int value = ((Int)arg).get();
                        buffer.append(CoreMath.intToString(value, 0, 0, grouping));
                    }
                    else if (arg instanceof Fixed) {
                        int value = ((Fixed)arg).getAsIntRound();
                        buffer.append(CoreMath.intToString(value, 0, 0, grouping));
                    }
                    else {
                        buffer.append(arg.toString());
                    }
                }
                else if (ch == 'f') {
                    found = true;
                    int minDigits = (numFracDigits == -1) ? 1 : numFracDigits;
                    int maxDigits = (numFracDigits == -1) ? 7 : numFracDigits;
                    Object arg = (currentArg < args.length) ? args[currentArg] : "?";
                    if (arg instanceof Integer) {
                        int value = ((Integer)arg).intValue();
                        buffer.append(CoreMath.intToString(value, minDigits, maxDigits, 
                            grouping));
                    }
                    else if (arg instanceof Int) {
                        int value = ((Int)arg).get();
                        buffer.append(CoreMath.intToString(value, minDigits, maxDigits, 
                            grouping));
                    }
                    else if (arg instanceof Fixed) {
                        int f = ((Fixed)arg).getAsFixed();
                        buffer.append(CoreMath.toString(f, minDigits, maxDigits, grouping));
                    }
                    else {
                        buffer.append(arg.toString());
                    }
                }
                else if (decimal && ch >= '0' && ch <= '9') {
                    numFracDigits = (numFracDigits * 10) + (ch - '0');
                }
                else {
                    buffer.append(message.substring(index, codeIndex));
                    break;
                }
                
            }
            if (found) {
                currentArg++;
            }
            lastIndex = codeIndex;
        }
        
        if (lastIndex < message.length()) {
            buffer.append(message.substring(lastIndex, message.length()));
        }
        
        return buffer.toString();
    }
    
    /**
        Replace all instances of target in string str with the replacement string.
    */
    public static String replace(String str, String target, String replacement) {
        while (true) {
            int index = str.indexOf(target);
            if (index == -1) {
                return str;
            }
            
            str = str.substring(0, index) + replacement + str.substring(index + target.length());
        }
    }
    
    /**
        Word-wraps a line of text to multiple lines. Newline characters ('\n')
        can be used for explicit line breaks.
    */
    public static String[] wordWrap(String text, CoreFont font, int maxWidth) {
        if (text == null) {
            return null;
        }
        if (font == null) {
            font = CoreFont.getSystemFont();
        }
        List splitText = new ArrayList();
        int startIndex = 0;
        int lastGoodIndex = -1;
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ' || ch == '\t') {
                lastGoodIndex = i;
            }
            else if (ch == '\n') {
                splitText.add(rtrim(text.substring(startIndex, i)));
                startIndex = i + 1;
                lastGoodIndex = -1;
            }
            else if (font.getStringWidth(text, startIndex, i + 1) > maxWidth) {
                if (lastGoodIndex != -1) {
                    // wrap this word
                    i = lastGoodIndex;
                }
                else if (startIndex < i) {
                    // this word doesn't fit on the line... split it
                    i--;
                }
                else { // if (startIndex == i)
                    // this 1 character doesn't fit on the line... add it anyway
                }
                splitText.add(rtrim(text.substring(startIndex, i + 1)));
                startIndex = i + 1;
                lastGoodIndex = -1;
            }
        }
        
        // add remaining chars
        if (startIndex < text.length()) {
            splitText.add(rtrim(text.substring(startIndex)));
        }
        
        // convert to String array
        String[] retVal = new String[splitText.size()];
        splitText.toArray(retVal);
        return retVal;
    }
    
    /**
        Splits a string into an array of strings seperated by the specified delimiter.
        The delimiter is not included in the returned string. If the line is null or has
        a length of zero, an array of length zero is returned.
    */
    public static String[] split(String line, char delimiter)  {
        if (line == null || line.length() == 0) {
            return new String[0];
        }
        
        int count = 1;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delimiter) {
                count++;
            }
        }
        
        String[] results = new String[count];
        int index = 0;
        for (int i = 0; i < count; i++) {
            int nextIndex = line.indexOf(delimiter, index);
            if (nextIndex == -1) {
                results[i] = line.substring(index);
            }
            else {
                results[i] = line.substring(index, nextIndex);
                index = nextIndex + 1;
            }
        }
        return results;
    }
    
    /**
        Safe equality check for two strings. Returns true if two strings are null or if
        two non-null strings are equal. Returns false if only one string is null or if 
        two non-null strings are not equal.
    */
    public static boolean equals(String arg1, String arg2) {
        if (arg1 == null || arg2 == null) {
            return (arg1 == arg2);
        }
        else {
            return arg1.equals(arg2);
        }
    }
}
