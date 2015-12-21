// CharSequenceWrapper.java, created Mon Mar 17 16:02:44 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.strings;

/**
 * CharSequenceWrapper is a wrapper for JDK 1.3 Strings so that they
 * can implement the JDK 1.4 "CharSequence" interface.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: CharSequenceWrapper.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class CharSequenceWrapper implements CharSequence {

    private final String s;
    
    /**
     * Make a new wrapper for the given String. 
     * @param s String to wrap
     */
    public CharSequenceWrapper(String s) { this.s = s; }
    
    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return s.length(); 
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        return s.charAt(index);
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int start, int end) {
        return new CharSequenceWrapper(s.substring(start, end));
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return s; }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() { return s.hashCode(); }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) { return s.equals(o); }

}
