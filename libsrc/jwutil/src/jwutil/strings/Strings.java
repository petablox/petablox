// Strings.java, created Fri Jan 11 17:14:14 2002 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.strings;

/**
 * A bunch of utility functions for strings.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Strings.java,v 1.2 2005/05/28 10:23:15 joewhaley Exp $
 */
public abstract class Strings {

    /**
     * Line separator (newline character)
     */
    public static final String lineSep = System.getProperty("line.separator");

    /**
     * Return the number as a hex string, prepended by "0x".
     *
     * @param i number
     * @return hex string
     */
    public static String hex(int i) {
        return "0x" + Integer.toHexString(i);
    }

    /**
     * Return a string representation of the address of an object.
     * If we are not running native, then it uses the identity hash code.
     *
     * @param o object
     * @return hex string of address of object
     */
    public static String hex(Object o) {
        return hex(System.identityHashCode(o));
    }

    /**
     * Return the number as a hex string, padded to eight digits and prepended by "0x".
     *
     * @param i number
     * @return hex string
     */
    public static String hex8(int i) {
        String t = Integer.toHexString(i);
        return "0x00000000".substring(0, 10 - t.length()) + t;
    }

    /**
     * Return the number as a hex string, padded to sixteen digits and prepended by "0x".
     *
     * @param i number
     * @return hex string
     */
    public static String hex16(long i) {
        String t = Long.toHexString(i);
        return "0x0000000000000000".substring(0, 18 - t.length()) + t;
    }

    /**
     * Return the number as a signed hex string, prepended by "0x".
     *
     * @param i number
     * @return hex string
     */
    public static String shex(int i) {
        if (i < 0)
            return "-" + hex(-i);
        else
            return hex(i);
    }

    /**
     * Return the w leftmost characters of the string, padding with spaces if necessary.
     *
     * @param s string
     * @param w number of characters
     * @return truncated/padded string
     */
    public static String left(String s, int w) {
        int n = s.length();
        if (w < n) return s.substring(0, w);
        StringBuffer b = new StringBuffer(w);
        b.append(s);
        for (int i = n; i < w; ++i) {
            b.append(' ');
        }
        return b.toString();
    }

    /**
     * Return the w rightmost characters of the string, padding with spaces if necessary.
     *
     * @param s string
     * @param w number of characters
     * @return truncated/padded string
     */
    public static String right(String s, int w) {
        int n = s.length();
        if (w < n) return s.substring(n - w);
        StringBuffer b = new StringBuffer(w);
        for (int i = n; i < w; ++i) {
            b.append(' ');
        }
        b.append(s);
        return b.toString();
    }
    
    /**
     * Replace all occurrences of <em>old</em> in <em>str</em> with <em>new_</em>.
     *
     * @param str String to permute
     * @param old String to be replaced
     * @param new_ Replacement string
     * @return new String object
     */
    public static final String replace(String str, String old, String new_) {
        int index, old_index;
        StringBuffer buf = new StringBuffer();
        
        try {
            if((index = str.indexOf(old)) != -1) { // `old' found in str
                old_index = 0;                     // String start offset
                
                // While we have something to replace
                while((index = str.indexOf(old, old_index)) != -1) {
                    buf.append(str.substring(old_index, index)); // append prefix
                    buf.append(new_);                            // append replacement
                    
                    old_index = index + old.length(); // Skip `old'.length chars
                }
                
                buf.append(str.substring(old_index)); // append rest of string
                str = buf.toString();
            }
        } catch(StringIndexOutOfBoundsException e) { // Should not occur
            System.err.println(e);
        }
        
        return str;
    }

    /**
     * Return a string for an integer justified left or right and filled up with
     * `fill' characters if necessary.
     *
     * @param i integer to format
     * @param length length of desired string
     * @param left_justify format left or right
     * @param fill fill character
     * @return formatted int
     */
    public static final String format(int i, int length, boolean left_justify, char fill) {
        return fillup(Integer.toString(i), length, left_justify, fill);
    }

    /**
     * Fillup char with up to length characters with char `fill' and justify it left or right.
     *
     * @param str string to format
     * @param length length of desired string
     * @param left_justify format left or right
     * @param fill fill character
     * @return formatted string
     */
    public static final String fillup(String str, int length, boolean left_justify, char fill) {
        int    len = length - str.length();
        char[] buf = new char[(len < 0)? 0 : len];

        for(int j=0; j < buf.length; j++)
            buf[j] = fill;

        if(left_justify)
            return str + new String(buf);    
        else
            return new String(buf) + str;
    }

}
