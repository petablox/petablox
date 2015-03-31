// Triple.java, created Mon Apr 21  2:48:51 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

/**
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Triple.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class Triple extends java.util.AbstractList
    implements java.io.Serializable {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3258128068173706036L;
    
    public Object left, middle, right;
    public Triple(Object left, Object middle, Object right) {
        this.left = left; this.middle = middle; this.right = right;
    }
    public int size() { return 3; }
    public Object get(int index) {
        switch(index) {
        case 0: return this.left;
        case 1: return this.middle;
        case 2: return this.right;
        default: throw new IndexOutOfBoundsException();
        }
    }
    public Object set(int index, Object element) {
        Object prev;
        switch(index) {
        case 0: prev=this.left; this.left=element; return prev;
        case 1: prev=this.middle; this.middle=element; return prev;
        case 2: prev=this.right; this.right=element; return prev;
        default: throw new IndexOutOfBoundsException();
        }
    }
}
