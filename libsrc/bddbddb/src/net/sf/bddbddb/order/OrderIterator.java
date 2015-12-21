// OrderIterator.java, created Oct 24, 2004 12:20:11 AM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import jwutil.math.CombinationGenerator;
import jwutil.util.Assert;

/**
 * Iterate through all possible orders of a given list.
 * 
 * @author jwhaley
 * @version $Id: OrderIterator.java 435 2005-02-13 03:24:59Z cs343 $
 */
public class OrderIterator implements Iterator {
    
    List orig;
    List/*<CombinationGenerator>*/ combos;
    int comboCounter;
    
    public OrderIterator(List a) {
        orig = new ArrayList(a);
        combos = new ArrayList(a.size());
        comboCounter = 0;
        gotoNextCombo();
    }
    
    void gotoNextCombo() {
        combos.clear();
        int remaining = orig.size();
        int size = 1;
        int bits = comboCounter++;
        while (remaining > 0) {
            CombinationGenerator g;
            if (remaining == size) {
                g = new CombinationGenerator(remaining, size);
                if (!combos.isEmpty()) g.getNext();
                combos.add(g);
                break;
            }
            if ((bits&1)==0) {
                g = new CombinationGenerator(remaining, size);
                if (!combos.isEmpty()) g.getNext();
                combos.add(g);
                remaining -= size;
                size = 0;
            }
            size++;
            bits >>= 1;
        }
    }
    
    boolean hasNextCombo() {
        int elements = orig.size();
        return (comboCounter < (1 << (elements-1)));
    }
    
    boolean hasMore() {
        for (Iterator i = combos.iterator(); i.hasNext(); ) {
            CombinationGenerator g = (CombinationGenerator) i.next();
            if (g.hasMore()) return true;
        }
        return false;
    }
    
    public boolean hasNext() {
        return hasMore() || hasNextCombo();
    }
    
    public Object next() {
        return nextOrder();
    }
    
    public Order nextOrder() {
        if (!hasMore()) {
            if (!hasNextCombo()) {
                throw new NoSuchElementException();
            }
            gotoNextCombo();
        }
        List result = new LinkedList();
        List used = new ArrayList(orig);
        boolean carry = true;
        for (Iterator i = combos.iterator(); i.hasNext(); ) {
            CombinationGenerator g = (CombinationGenerator) i.next();
            int[] p;
            if (carry) {
                if (!g.hasMore()) g.reset();
                else carry = false;
                p = g.getNext();
            } else {
                p = g.getCurrent();
            }
            if (p.length == 1) {
                result.add(used.remove(p[0]));
            } else {
                LinkedList c = new LinkedList();
                for (int k = p.length-1; k >= 0; --k) {
                    c.addFirst(used.remove(p[k]));
                }
                result.add(c);
            }
        }
        Assert._assert(!carry);
        return new Order(result);
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
