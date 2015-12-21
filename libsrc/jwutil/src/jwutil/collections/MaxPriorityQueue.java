// PriorityQueue.java, created Tue Jun  1 13:43:17 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;

/**
 * <code>MaxPriorityQueue</code> maintains a <code>Collection</code> of
 * <code>Object</code>s, each with an associated priority.
 * Implementations should make the <code>peekMax</code> and
 * <code>removeMax</code> operations efficient.  Implementations
 * need not implement the Object-addition operations of the
 * <code>Collection</code> interface, since they do not associate each
 * added <code>Object</code> with a priority.
 * 
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: MaxPriorityQueue.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface MaxPriorityQueue extends Collection {

    /** Inserts <code>item</code> into this, assigning it priority
    <code>priority</code>. 
    @param item <code>Object</code> being inserted
    @param priority Priority of <code>item</code>
    */
    boolean insert(Object item, int priority);

    /** Returns the <code>Object</code> in <code>this</code> with the
    highest priority.
    */
    Object peekMax();
    
    /** Returns and removes the <code>Object</code> in
    <code>this</code> with the highest priority.
    */
    Object deleteMax();


    /** Returns the priority currently associated with this item.
     */
    int getPriority(Object item);

    /** Changes the priority of this <code>Object</code>
        to the new, specified value, and returns the old value
    */
    int setPriority(Object item, int newPriority);

    /** Change the priority of this element by the specified delta.
        This is equivalent to <code>setPriority(item, getPriority(item)+delta).
        It might save time in some implementations.
    */
    void changePriority(Object item, int delta);
    
}
