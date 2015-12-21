/*
 * Created on Feb 6, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import jwutil.collections.Pair;

/**
 * @author mcarbin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class UnorderedPair extends Pair {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3258135756147798328L;
    
    /**
     * @param left
     * @param right
     */
    public UnorderedPair(Object left, Object right) {
        super(left, right);
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
       if(o instanceof UnorderedPair){
           UnorderedPair that = (UnorderedPair) o;
           return (this.left.equals(that.left) && this.right.equals(that.right)) ||
                  (this.left.equals(that.right) & this.right.equals(that.left));
       }
       return false;
    }
    /**
     * Returns the hash code value for this set.  The hash code of a set is
     * defined to be the sum of the hash codes of the elements in the set.
     * This ensures that <tt>s1.equals(s2)</tt> implies that
     * <tt>s1.hashCode()==s2.hashCode()</tt> for any two sets <tt>s1</tt>
     * and <tt>s2</tt>, as required by the general contract of
     * Object.hashCode.<p>
     *
     * This implementation enumerates over the set, calling the
     * <tt>hashCode</tt> method on each element in the collection, and
     * adding up the results.
     *
     * @return the hash code value for this set.
     */
    public int hashCode() {
        return left.hashCode() + right.hashCode();
    }

}
