// Pair.java, created Wed Mar  5  0:26:26 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractList;

import jwutil.io.Textualizable;
import jwutil.io.Textualizer;

/**
 * List of two elements.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Pair.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class Pair extends AbstractList implements Serializable, Textualizable {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3545236912383078965L;
    
    /**
     * The elements of the pair.
     */
    public Object left, right;
    
    /**
     * Construct a new Pair.
     * 
     * @param left  first element
     * @param right  second element
     */
    public Pair(Object left, Object right) {
        this.left = left; this.right = right;
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() { return 2; }
    
    /* (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        switch(index) {
        case 0: return this.left;
        case 1: return this.right;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object element) {
        Object prev;
        switch(index) {
        case 0: prev=this.left; this.left=element; return prev;
        case 1: prev=this.right; this.right=element; return prev;
        default: throw new IndexOutOfBoundsException();
        }
    }
    
    /* (non-Javadoc)
     * @see jwutil.io.Textualizable#write(jwutil.io.Textualizer)
     */
    public void write(Textualizer t) throws IOException {
    }
    
    /* (non-Javadoc)
     * @see jwutil.io.Textualizable#writeEdges(jwutil.io.Textualizer)
     */
    public void writeEdges(Textualizer t) throws IOException {
        t.writeEdge("left", (Textualizable) left);
        t.writeEdge("right", (Textualizable) right);
    }
    
    /* (non-Javadoc)
     * @see jwutil.io.Textualizable#addEdge(java.lang.String, jwutil.io.Textualizable)
     */
    public void addEdge(String edge, Textualizable t) {
        if (edge.equals("left"))
            this.left = t;
        else if (edge.equals("right"))
            this.right = t;
        else
            throw new InternalError();
    }
}
