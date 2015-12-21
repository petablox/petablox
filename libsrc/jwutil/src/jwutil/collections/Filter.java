// Filter.java, created Tue Feb 23 06:17:30 1999 by cananian
// Copyright (C) 1998 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

/**
 * Filter. Default is an identity mapping.
 * 
 * @author C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: Filter.java,v 1.2 2004/10/04 14:37:07 joewhaley Exp $
 */
public class Filter {
    /**
     * Return <code>true</code> if the specified element should be included in
     * the filtered enumeration.
     * 
     * <BR>
     * Default implementation returns true for all <code>Object</code> s (no
     * filter).
     */
    public boolean isElement(Object o) {
        return true;
    }

    /**
     * Perform a mapping on elements from the source enumeration.
     * 
     * <BR>
     * Default implementation returns <code>o</code> (identity mapping).
     */
    public Object map(Object o) {
        return o;
    }
}
