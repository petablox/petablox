// MyStringTokenizer.java, created Apr 21, 2004 7:06:28 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.strings;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * MyStringTokenizer is like StringTokenizer, but gives you access to the string
 * and position, and also ignores tokens inbetween quotation marks.
 */
public class MyStringTokenizer implements Enumeration {
    
    /**
     * Returns the string that is being tokenized.
     * 
     * @return  string that is being tokenized
     */
    public String getString() {
        return str;
    }

    /**
     * Returns the current position of the tokenizer in the string.
     * 
     * @return  current position of the tokenizer in the string
     */
    public int getPosition() {
        return currentPosition;
    }
    
    private int currentPosition;
    private int newPosition;
    private int maxPosition;
    private String str;
    private String delimiters;
    private boolean retDelims;
    private boolean delimsChanged;
    private char maxDelimChar;

    /**
     * Set maxDelimChar to the highest char in the delimiter set.
     * 
     * see java.util.StringTokenizer#setMaxDelimChar()
     */
    private void setMaxDelimChar() {
        if (delimiters == null) {
            maxDelimChar = 0;
            return;
        }
        char m = 0;
        for (int i = 0; i < delimiters.length(); i++) {
            char c = delimiters.charAt(i);
            if (m < c) m = c;
        }
        maxDelimChar = m;
    }

    /**
     * Constructs a string tokenizer for the specified string. All  
     * characters in the <code>delim</code> argument are the delimiters 
     * for separating tokens. 
     * <p>
     * If the <code>returnDelims</code> flag is <code>true</code>, then 
     * the delimiter characters are also returned as tokens. Each 
     * delimiter is returned as a string of length one. If the flag is 
     * <code>false</code>, the delimiter characters are skipped and only 
     * serve as separators between tokens. 
     * <p>
     * Note that if <tt>delim</tt> is <tt>null</tt>, this constructor does
     * not throw an exception. However, trying to invoke other methods on the
     * resulting <tt>StringTokenizer</tt> may result in a 
     * <tt>NullPointerException</tt>.
     *
     * @param   str            a string to be parsed.
     * @param   delim          the delimiters.
     * @param   returnDelims   flag indicating whether to return the delimiters
     *                         as tokens.
     * @exception NullPointerException if str is <CODE>null</CODE>
     * 
     * @see java.util.StringTokenizer#StringTokenizer(java.lang.String,java.lang.String,boolean)
     */
    public MyStringTokenizer(String str, String delim, boolean returnDelims) {
        currentPosition = 0;
        newPosition = -1;
        delimsChanged = false;
        this.str = str;
        maxPosition = str.length();
        delimiters = delim;
        retDelims = returnDelims;
        setMaxDelimChar();
    }

    /**
     * Constructs a string tokenizer for the specified string. The 
     * characters in the <code>delim</code> argument are the delimiters 
     * for separating tokens. Delimiter characters themselves will not 
     * be treated as tokens.
     * <p>
     * Note that if <tt>delim</tt> is <tt>null</tt>, this constructor does
     * not throw an exception. However, trying to invoke other methods on the
     * resulting <tt>StringTokenizer</tt> may result in a
     * <tt>NullPointerException</tt>.
     *
     * @param   str     a string to be parsed.
     * @param   delim   the delimiters.
     * @exception NullPointerException if str is <CODE>null</CODE>
     * 
     * @see java.util.StringTokenizer#StringTokenizer(java.lang.String,java.lang.String)
     */
    public MyStringTokenizer(String str, String delim) {
        this(str, delim, false);
    }

    /**
     * Constructs a string tokenizer for the specified string. The 
     * tokenizer uses the default delimiter set, which is 
     * <code>"&nbsp;&#92;t&#92;n&#92;r&#92;f"</code>: the space character, 
     * the tab character, the newline character, the carriage-return character,
     * and the form-feed character. Delimiter characters themselves will 
     * not be treated as tokens.
     *
     * @param   str   a string to be parsed.
     * @exception NullPointerException if str is <CODE>null</CODE> 
     * 
     * @see java.util.StringTokenizer#StringTokenizer(java.lang.String)
     */
    public MyStringTokenizer(String str) {
        this(str, " \t\n\r\f");
    }

    /**
     * Skips delimiters starting from the specified position. If retDelims
     * is false, returns the index of the first non-delimiter character at or
     * after startPos. If retDelims is true, startPos is returned.
     * 
     * see java.util.StringTokenizer#skipDelimiters(int)
     */
    private int skipDelimiters(int startPos) {
        if (delimiters == null) throw new NullPointerException();
        int position = startPos;
        while (!retDelims && position < maxPosition) {
            char c = str.charAt(position);
            if ((c > maxDelimChar) || (delimiters.indexOf(c) < 0)) break;
            position++;
        }
        return position;
    }

    /**
     * Skips ahead from startPos and returns the index of the next delimiter
     * character encountered, or maxPosition if no such delimiter is found.
     * 
     * see java.util.StringTokenizer#scanToken(int)
     */
    private int scanToken(int startPos) {
        int position = startPos;
        boolean inString = false;
        while (position < maxPosition) {
            char c = str.charAt(position);
            if (c == '"') {
                inString = !inString;
            } else if (!inString
                && ((c <= maxDelimChar) && (delimiters.indexOf(c) >= 0))) break;
            position++;
        }
        if (retDelims && (startPos == position)) {
            char c = str.charAt(position);
            if ((c <= maxDelimChar) && (delimiters.indexOf(c) >= 0)) position++;
        }
        return position;
    }

    /**
     * Tests if there are more tokens available from this tokenizer's string. 
     * If this method returns <tt>true</tt>, then a subsequent call to 
     * <tt>nextToken</tt> with no argument will successfully return a token.
     *
     * @return  <code>true</code> if and only if there is at least one token 
     *          in the string after the current position; <code>false</code> 
     *          otherwise.
     * 
     * @see java.util.StringTokenizer#hasMoreTokens()
     */
    public boolean hasMoreTokens() {
        newPosition = skipDelimiters(currentPosition);
        return (newPosition < maxPosition);
    }

    /**
     * Returns the next token from this string tokenizer.
     *
     * @return     the next token from this string tokenizer.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     * 
     * @see java.util.StringTokenizer#nextToken()
     */
    public String nextToken() {
        currentPosition = (newPosition >= 0 && !delimsChanged)
            ? newPosition
            : skipDelimiters(currentPosition);
        delimsChanged = false;
        newPosition = -1;
        if (currentPosition >= maxPosition) throw new NoSuchElementException();
        int start = currentPosition;
        currentPosition = scanToken(currentPosition);
        return str.substring(start, currentPosition);
    }

    /**
     * Returns the next token in this string tokenizer's string. First, 
     * the set of characters considered to be delimiters by this 
     * <tt>StringTokenizer</tt> object is changed to be the characters in 
     * the string <tt>delim</tt>. Then the next token in the string
     * after the current position is returned. The current position is 
     * advanced beyond the recognized token.  The new delimiter set 
     * remains the default after this call. 
     *
     * @param      delim   the new delimiters.
     * @return     the next token, after switching to the new delimiter set.
     * @exception  NoSuchElementException  if there are no more tokens in this
     *               tokenizer's string.
     * @exception NullPointerException if delim is <CODE>null</CODE>
     * 
     * @see java.util.StringTokenizer#nextToken(String)
     */
    public String nextToken(String delim) {
        delimiters = delim;
        delimsChanged = true;
        setMaxDelimChar();
        return nextToken();
    }

    /* (non-Javadoc)
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    /* (non-Javadoc)
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement() {
        return nextToken();
    }

    /**
     * Calculates the number of times that this tokenizer's 
     * <code>nextToken</code> method can be called before it generates an 
     * exception. The current position is not advanced.
     *
     * @return  the number of tokens remaining in the string using the current
     *          delimiter set.
     * 
     * @see jwutil.strings.MyStringTokenizer#nextToken()
     * @see java.util.StringTokenizer#countTokens()
     */
    public int countTokens() {
        int count = 0;
        int currpos = currentPosition;
        while (currpos < maxPosition) {
            currpos = skipDelimiters(currpos);
            if (currpos >= maxPosition) break;
            currpos = scanToken(currpos);
            count++;
        }
        return count;
    }
}
