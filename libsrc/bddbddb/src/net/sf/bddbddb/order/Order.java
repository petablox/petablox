// Order.java, created Oct 22, 2004 5:24:41 PM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;

import jwutil.util.Assert;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.order.OrderConstraint.InterleaveConstraint;

/**
 * Represents an order.  This is just a List with a few extra utility functions.
 * 
 * @author jwhaley
 * @version $Id: Order.java 435 2005-02-13 03:24:59Z cs343 $
 */
public class Order implements List, Comparable {
    
    /** Underlying list. */
    List list;
    
    /** Constraints of this order. */
    transient Collection constraints;
    
    /**
     * Construct a new Order that is a copy of the given Order.
     * 
     * @param o  order to copy
     */
    public Order(Order o) {
        this.list = new ArrayList(o.list);
        this.constraints = null;
    }
    
    /**
     * Construct a new Order from the given list.
     * 
     * @param l  list
     */
    public Order(List l) {
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof List) {
                Collections.sort((List) o, OrderConstraint.elementComparator);
            }
        }
        this.list = l;
        this.constraints = null;
    }
    
    /**
     * Returns true if this order obeys the given constraint.
     * 
     * @param c  constraint
     * @return  true if this order obeys the constraint, false otherwise
     */
    public boolean obeysConstraint(OrderConstraint c) {
        return c.obeyedBy(this);
    }
    
    /**
     * Return the collection of constraints in this order.
     * 
     * @return  collection of constraints
     */
    public Collection/*<OrderConstraint>*/ getConstraints() {
        if (constraints == null) {
            constraints = new LinkedList();
            
            // Precedence constraints.
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                Object o1 = i.next();
                Iterator j = list.iterator();
                while (j.hasNext() && j.next() != o1) ;
                while (j.hasNext()) {
                    Object o2 = j.next();
                    Iterator x, y;
                    if (o1 instanceof Collection) {
                        x = ((Collection) o1).iterator();
                    } else {
                        x = Collections.singleton(o1).iterator();
                    }
                    while (x.hasNext()) {
                        Object x1 = x.next();
                        if (o2 instanceof Collection) {
                            y = ((Collection) o2).iterator();
                        } else {
                            y = Collections.singleton(o2).iterator();
                        }
                        while (y.hasNext()) {
                            Object y1 = y.next();
                            //if (!x1.equals(y1))
                            constraints.add(OrderConstraint.makePrecedenceConstraint(x1, y1));
                        }
                    }
                }
            }
            
            // Interleave constraints.
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                Object o1 = i.next();
                if (o1 instanceof Collection) {
                    Collection c = (Collection) o1;
                    for (Iterator x = c.iterator(); x.hasNext(); ) {
                        Object a = x.next();
                        Iterator y = c.iterator();
                        while (y.hasNext() && y.next() != a) ;
                        while (y.hasNext()) {
                            Object b = y.next();
                            constraints.add(OrderConstraint.makeInterleaveConstraint(a, b));
                        }
                    }
                }
            }
        }
        return constraints;
    }
    
    /**
     * Returns the number of elements in this order.  This includes elements
     * within interleaves.
     * 
     * @return  number of elements in this order
     */
    public int numberOfElements() {
        int total = 0;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                total += ((Collection) o).size();
            } else {
                ++total;
            }
        }
        return total;
    }
    
    /**
     * Return the flattened version of this list.
     * 
     * @return  flattened version of this list
     */
    public List getFlattened() {
        List result = new LinkedList();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                result.addAll((Collection) o);
            } else {
                result.add(o);
            }
        }
        return result;
    }
    
    /**
     * Get all interleave constraints of this order.
     * 
     * @return  collection of interleave constraints
     */
    public Collection/*<InterleaveConstraint>*/ getAllInterleaveConstraints() {
        getConstraints();
        Collection s = new LinkedList();
        for (Iterator i = constraints.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof InterleaveConstraint)
                s.add(o);
        }
        return s;
    }
    
    /**
     * Get the number of interleave constraints in this order.
     * 
     * @return  number of interleave constraints
     */
    public int numInterleaveConstraints() {
        int n = 0;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                int k = ((Collection) o).size();
                n += k * (k-1) / 2;
            }
        }
        return n;
    }
    
    /**
     * Get all precedence constraints of this order.
     * 
     * @return  collection of precedence constraints
     */
    public Collection/*<OrderConstraint>*/ getAllPrecedenceConstraints() {
        getConstraints();
        Collection s = new LinkedList();
        for (Iterator i = constraints.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (!(o instanceof InterleaveConstraint))
                s.add(o);
        }
        return s;
    }
    
    /**
     * Get the number of precedence constraints in this order.
     * 
     * @return  number of precedence constraints
     */
    public int numPrecedenceConstraints() {
        int n = 0;
        int result = 0;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            int k;
            if (o instanceof Collection) {
                k = ((Collection) o).size();
            } else {
                k = 1;
            }
            result += n*k;
            n += k;
        }
        return result;
    }
    
    /**
     * Returns the similarity between two orders as a number between 0.0 and 1.0.
     * 1.0 means that the orders are exactly the same, and 0.0 means they have no
     * similarities.
     * 
     * Precedence constraints are weighted by the factor "PRECEDENCE_WEIGHT", and
     * interleave constraints are weighted by the factor "INTERLEAVE_WEIGHT".
     * 
     * @param that
     * @return  similarity measure between 0.0 and 1.0
     */
    public double similarity(Order that) {
        if (this.isEmpty() || that.isEmpty()) return 1.;
        if (this.numberOfElements() < that.numberOfElements())
            return this.similarity0(that);
        else
            return that.similarity0(this);
    }
    
    public static double PRECEDENCE_WEIGHT = 1.;
    public static double INTERLEAVE_WEIGHT = 3.;
    
    private double similarity0(Order that) {
        this.getConstraints();
        that.getConstraints();
        
        Collection dis_preds = this.getAllPrecedenceConstraints();
        Collection dis_inters = this.getAllInterleaveConstraints();
        Collection dat_preds = that.getAllPrecedenceConstraints();
        Collection dat_inters = that.getAllInterleaveConstraints();
        
        // Calculate the maximum number of similarities.
        int nPred = dis_preds.size();
        int nInter = dis_inters.size();
        
        // Find all similarities between the orders.
        dis_preds.removeAll(dat_preds);
        dis_inters.removeAll(dat_inters);
        int nPred2 = dis_preds.size();
        int nInter2 = dis_inters.size();

        double total = nPred * PRECEDENCE_WEIGHT + nInter * INTERLEAVE_WEIGHT;
        double unsimilar = nPred2 * PRECEDENCE_WEIGHT + nInter2 * INTERLEAVE_WEIGHT;
        double sim = (total - unsimilar) / total;
        if (FindBestDomainOrder.TRACE > 4) FindBestDomainOrder.out.println("Similarity ("+this+" and "+that+") = "+FindBestDomainOrder.format(sim));
        return sim;
    }
    
    public static double[] COMPLEXITY_SINGLE = 
    { 0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15. } ;
    public static double[] COMPLEXITY_MULTI = 
    { 0., 2., 4., 8., 16., 32., 64., 128., 256., 512., 1024., 2048., 4096., 8192., 16384., 32768. } ;
    
    /**
     * Returns a measure of the complexity of this order.  Higher numbers are
     * more complex (i.e. have more constraints)
     * 
     * @return a measure of the complexity of this order
     */
    public double complexity() {
        int n = Math.min(list.size(), COMPLEXITY_SINGLE.length-1);
        double total = COMPLEXITY_SINGLE[n];
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                n = Math.min(((Collection) o).size(), COMPLEXITY_MULTI.length-1);
                total += COMPLEXITY_MULTI[n];
            }
        }
        return total;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        return compareTo((Order) arg0);
    }
    /**
     * Compares orders lexigraphically. 
     * 
     * @param that  order to compare to
     * @return  -1, 0, or 1 if this order is less than, equal to, or greater than
     */
    public int compareTo(Order that) {
        if (this == that) return 0;
        return this.toString().compareTo(that.toString());
    }
    
    public boolean equals(Order that) {
        if (true) {
            return list.equals(that.list);
        } else {
            if (this.list.size() != that.list.size()) return false;
            Iterator i = this.list.iterator();
            Iterator j = that.list.iterator();
            while (i.hasNext()) {
                Object a = i.next();
                Object b = j.next();
                if (a instanceof Collection) {
                    if (b instanceof Collection) {
                        Collection ac = (Collection) a;
                        Collection bc = (Collection) b;
                        if (!ac.containsAll(bc)) return false;
                        if (!bc.containsAll(ac)) return false;
                    } else return false;
                } else if (!a.equals(b)) return false;
            }
            return true;
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Order)) return false;
        return equals((Order) obj);
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o) {
        return list.add(o);
    }
    /* (non-Javadoc)
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object element) {
        list.add(index, element);
    }
    /* (non-Javadoc)
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c) {
        return list.addAll(index,c);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c) {
        return list.addAll(c);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#clear()
     */
    public void clear() {
        list.clear();
    }
    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return list.contains(o);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }
    /* (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        return list.get(index);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return list.hashCode();
    }
    /* (non-Javadoc)
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        return list.indexOf(o);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator iterator() {
        return list.iterator();
    }
    /* (non-Javadoc)
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }
    /* (non-Javadoc)
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator() {
        return list.listIterator();
    }
    /* (non-Javadoc)
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index) {
        return list.listIterator(index);
    }
    /* (non-Javadoc)
     * @see java.util.List#remove(int)
     */
    public Object remove(int index) {
        return list.remove(index);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return list.remove(o);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c) {
        return list.removeAll(c);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c) {
        return list.retainAll(c);
    }
    /* (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object element) {
        return list.set(index, element);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        return list.size();
    }
    /* (non-Javadoc)
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex,toIndex);
    }
    /* (non-Javadoc)
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        return list.toArray();
    }
    /* (non-Javadoc)
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return list.toString();
    }

    /**
     * Generate a BDD order string from this variable order.
     * 
     * @param variableToBDDDomain  map from variable to BDD
     * @return  BDD order string
     */
    public String toVarOrderString(Map/*<Variable,BDDDomain>*/ variableToBDDDomain) {
        StringBuffer varOrder = new StringBuffer();
        for (Iterator i = iterator(); i.hasNext(); ) {
            Object p = i.next();
            if (p instanceof Collection) {
                Collection c = (Collection) p;
                int num = 0;
                for (Iterator j = c.iterator(); j.hasNext(); ) {
                    Object v = j.next();
                    Object d;
                    if (variableToBDDDomain != null) d = variableToBDDDomain.get(v);
                    else d = v;
                    if (d != null) {
                        if (varOrder.length() > 0) {
                            if (num == 0) {
                                varOrder.append('_');
                            } else {
                                varOrder.append('x');
                            }
                        }
                        varOrder.append(d);
                        ++num;
                    }
                }
            } else {
                Object d;
                if (variableToBDDDomain != null) d = variableToBDDDomain.get(p);
                else d = p;
                if (d != null) {
                    if (varOrder.length() > 0) varOrder.append('_');
                    varOrder.append(d);
                }
            }
        }
        String vOrder = varOrder.toString();
        return vOrder;
    }
    
    /**
     * Parse an order from a string.
     * 
     * @param s  string to parse
     * @param nameToObj  map from name to object (variable, etc.)
     * @return  order
     */
    public static Order parse(String s, Map nameToObj) {
        StringTokenizer st = new StringTokenizer(s, "[], ", true);
        String tok = st.nextToken();
        if (!tok.equals("[")) {
            throw new IllegalArgumentException("Unknown \""+tok+"\" in order \""+s+"\"");
        }
        List o = new LinkedList();
        List inner = null;
        while (st.hasMoreTokens()) {
            tok = st.nextToken();
            if (tok.equals(" ") || tok.equals(",")) continue;
            if (tok.equals("[")) {
                if (inner != null)
                    throw new IllegalArgumentException("Nested \""+tok+"\" in order \""+s+"\"");
                inner = new LinkedList();
                continue;
            }
            if (tok.equals("]")) {
                if (!st.hasMoreTokens()) break;
                if (inner == null)
                    throw new IllegalArgumentException("Unmatched \""+tok+"\" in order \""+s+"\"");
                o.add(inner);
                inner = null;
                continue;
            }
            Object obj = nameToObj.get(tok);
            if (obj == null) {
                throw new IllegalArgumentException("Unknown \""+tok+"\" in order \""+s+"\"");
            }
            if (inner != null) {
                if (!inner.contains(obj)) inner.add(obj);
            }
            else o.add(obj);
        }
        return new Order(o);
    }
    
    
    //////////////////// OLD CODE BELOW
    
    /**
     * Given a collection of orders, find its similarities and the
     * number of occurrences of each similarity.
     * 
     * @param c  collection of orders
     * @return  map from order similarities to frequencies
     */
    public static Map/*<Order,Integer>*/ calcLongSimilarities(Collection c) {
        Map m = new HashMap();
        if (FindBestDomainOrder.TRACE > 1) FindBestDomainOrder.out.println("Calculating similarities in the collection: "+c);
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Order a = (Order) i.next();
            Iterator j = c.iterator();
            while (j.hasNext() && j.next() != a) ;
            while (j.hasNext()) {
                Order b = (Order) j.next();
                Collection/*<Order>*/ sim = a.findLongSimilarities(b);
                // todo: expand sim to also include implied suborders.
                for (Iterator k = sim.iterator(); k.hasNext(); ) {
                    Order s = (Order) k.next();
                    Integer count = (Integer) m.get(s);
                    int newCount = (count==null) ? 1 : count.intValue()+1;
                    m.put(s, new Integer(newCount));
                }
            }
        }
        if (FindBestDomainOrder.TRACE > 1) FindBestDomainOrder.out.println("Similarities: "+m);
        return m;
    }
    
    /**
     * Utility function for intersecting elements and collections.
     * 
     * @param a  element or collection
     * @param b  element or collection
     * @return  element or collection which is the intersection
     */
    static Object intersect(Object a, Object b) {
        if (a instanceof Collection) {
            Collection ca = (Collection) a;
            if (b instanceof Collection) {
                Collection result = new LinkedList();
                result.addAll(ca);
                result.retainAll((Collection) b);
                if (result.isEmpty()) return null;
                else if (result.size() == 1) return result.iterator().next();
                else return result;
            }
            if (ca.contains(b)) return b;
        } else if (b instanceof Collection) {
            if (((Collection) b).contains(a)) return a;
        } else {
            if (a.equals(b)) return a;
        }
        return null;
    }
    
    /**
     * Utility function for adding new elements from one collection to another.
     * 
     * @param c  collection to add to
     * @param c2  collection to add from
     */
    static void addAllNew(Collection c, Collection c2) {
        outer:
        for (ListIterator c2i = ((List)c2).listIterator(); c2i.hasNext(); ) {
            List l2 = (List) c2i.next();
            for (ListIterator c1i = ((List)c).listIterator(); c1i.hasNext(); ) {
                List l1 = (List) c1i.next();
                if (l1.containsAll(l2)) continue outer;
                else if (l2.containsAll(l1)) {
                    c1i.set(l2);
                    continue outer;
                }
            }
            c.add(l2);
        }
    }
    
    // TODO: this should use a dynamic programming implementation instead
    // of recursive, because it is solving many repeated subproblems.
    static Collection findLongSimilarities(List o1, List o2) {
        if (o1.size() == 0 || o2.size() == 0) {
            return null;
        }
        Object x1 = o1.get(0);
        List r1 = o1.subList(1, o1.size());
        Object x2 = o2.get(0);
        List r2 = o2.subList(1, o2.size());
        Object x = intersect(x1, x2);
        Collection c = null;
        if (x != null) {
            c = findLongSimilarities(r1, r2);
            if (c == null) {
                c = new LinkedList();
                Collection c2 = new LinkedList();
                c2.add(x);
                c.add(c2);
            } else {
                for (Iterator i = c.iterator(); i.hasNext(); ) {
                    List l = (List) i.next();
                    l.add(0, x);
                }
            }
        }
        if (x == null || !x1.equals(x2)) {
            Collection c2 = findLongSimilarities(o1, r2);
            if (c == null) c = c2;
            else if (c2 != null) addAllNew(c, c2);
            Collection c3 = findLongSimilarities(r1, o2);
            if (c == null) c = c3;
            else if (c3 != null) addAllNew(c, c3);
        }
        return c;
    }
    
    /**
     * Return the collection of suborders that are similar between this order
     * and the given order.  Duplicates are eliminated.
     * 
     * @param that  other order
     * @return  collection of suborders that are similar
     */
    public Collection/*<Order>*/ findLongSimilarities(Order that) {
        if (false)
        {
            Collection f1 = this.getFlattened();
            Collection f2 = that.getFlattened();
            Assert._assert(f1.containsAll(f2));
            Assert._assert(f2.containsAll(f1));
        }
        
        Collection result = new LinkedList();
        Collection c = findLongSimilarities(this.list, that.list);
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            List l = (List) i.next();
            if (false && l.size() == 1) {
                Object elem = l.get(0);
                if (!(elem instanceof List)) continue;
                List l2 = (List) elem;
                if (l2.size() == 1) continue;
            }
            result.add(new Order(l));
        }
        return result;
    }
    
}
