// OrderConstraintSet.java, created Oct 27, 2004 11:51:27 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.math.CombinationGenerator;
import jwutil.math.Distributions;
import jwutil.util.Assert;
import net.sf.bddbddb.order.OrderConstraint.AfterConstraint;
import net.sf.bddbddb.order.OrderConstraint.BeforeConstraint;
import net.sf.bddbddb.order.OrderConstraint.InterleaveConstraint;

/**
 * OrderConstraintSet
 * 
 * @author jwhaley
 * @version $Id: OrderConstraintSet.java 502 2005-04-13 00:26:23Z joewhaley $
 */
public class OrderConstraintSet {
    
    LinkedHashSet set;
    MultiMap objToConstraints;
    
    /**
     * 
     */
    public OrderConstraintSet() {
        set = new LinkedHashSet();
        objToConstraints = new GenericMultiMap();
    }
    
    public OrderConstraintSet copy() {
        return new OrderConstraintSet(this);
    }
    
    private OrderConstraintSet(LinkedHashSet set, MultiMap objToConstraints){
        this.set = set;
        this.objToConstraints = objToConstraints;
    }
    
    public OrderConstraintSet(OrderConstraintSet that) {
        set = new LinkedHashSet(that.set);
        objToConstraints = new GenericMultiMap();
        for (Iterator i = set.iterator(); i.hasNext(); ) {
            OrderConstraint c = (OrderConstraint) i.next();
            objToConstraints.add(c.a, c);
            objToConstraints.add(c.b, c);
        }
    }
    public OrderConstraintSet translate(Map translationMap){
        OrderConstraintSet newOcs = new OrderConstraintSet();
        for(Iterator it = set.iterator(); it.hasNext(); ){
            OrderConstraint oc = (OrderConstraint) it.next();
            Object newA = translationMap.get(oc.a);
            Object newB = translationMap.get(oc.b);
            if(newA == null || newB == null) continue; /* skip or assert */
            OrderConstraint newOc = OrderConstraint.makeConstraint(oc.getType(), newA, newB);
            newOcs.constrain(newOc);            
        }
        return newOcs;
    }
    
    public int size(){ return set.size(); }
    public boolean constrain(OrderConstraintSet ocs, Collection invalidConstraints){
        return constrain(ocs.set, invalidConstraints);
        
    }
    public boolean constrain(Order c, Collection /*OrderConstraint*/ invalidConstraints) {
        return constrain(c.getConstraints(), invalidConstraints);
    }
    
    public boolean constrain(Collection c, Collection /*OrderConstraint*/ invalidConstraints) {
        boolean worked = true;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            OrderConstraint oc = (OrderConstraint) i.next();
            if(!constrain(oc)){
                worked = false;
                if(invalidConstraints != null) invalidConstraints.add(oc);
            }
        }
        return worked;
    }
    
    public boolean constrain(OrderConstraint c) {
        if (set.contains(c)) return true;
        if (set.contains(c.getOpposite1())) return false;
        if (set.contains(c.getOpposite2())) return false;
        if (c instanceof InterleaveConstraint){
         /*   if(c.getFirst().equals(c.getSecond()))
                Assert._assert(false);
           */
            addInterleaveConstraint((InterleaveConstraint) c);
        }else {
            if (c.a.equals(c.b)) return false;
            addPrecedenceConstraint(c);
        }
        return true;
    }
    
    void checkTransitivePrecedence(Object a, Object b, Object c, Object d) {
        if (b.equals(c)) {
            addPrecedenceConstraint(a, d);
        } else if (a.equals(d)) {
            addPrecedenceConstraint(c, b);
        }
    }
    
    void addInterleaveConstraint(Object a, Object b) {
        if (a.equals(b)) return;
        InterleaveConstraint o = (InterleaveConstraint) OrderConstraint.makeInterleaveConstraint(a, b);
        addInterleaveConstraint(o);
    }
    void addInterleaveConstraint(InterleaveConstraint c) {
        if (set.contains(c)) return;
        set.add(c);
        Collection c1 = objToConstraints.getValues(c.a);
        Collection c2 = objToConstraints.getValues(c.b);
        objToConstraints.add(c.a, c);
        objToConstraints.add(c.b, c);
        addInterleaveConstraints(c.a, c.b, c1);
        addInterleaveConstraints(c.a, c.b, c2);
    }
    void addInterleaveConstraints(Object a, Object b, Collection ocs) {
        // Make a backup to avoid ConcurrentModificationException
        ocs = new ArrayList(ocs);
        for (Iterator i = ocs.iterator(); i.hasNext(); ) {
            OrderConstraint oc = (OrderConstraint) i.next();
            if (oc instanceof InterleaveConstraint) {
                addInterleaveConstraint(a, oc.a);
                addInterleaveConstraint(a, oc.b);
                addInterleaveConstraint(b, oc.a);
                addInterleaveConstraint(b, oc.b);
            } else {
                Object c, d;
                if (oc instanceof BeforeConstraint) {
                    c = oc.a; d = oc.b;
                } else {
                    c = oc.b; d = oc.a;
                }
                checkTransitivePrecedence(a, b, c, d);
                checkTransitivePrecedence(b, a, c, d);
            }
        }
    }
    
    void addPrecedenceConstraint(Object a, Object b) {
        Assert._assert(!a.equals(b));
        OrderConstraint o = OrderConstraint.makePrecedenceConstraint(a, b);
        addPrecedenceConstraint(o);
    }
    void addPrecedenceConstraint(OrderConstraint c) {
        Assert._assert(c instanceof BeforeConstraint || c instanceof AfterConstraint);
        Assert._assert(!c.a.equals(c.b));
        if (set.contains(c)) return;
        set.add(c);
        Collection c1 = objToConstraints.getValues(c.a);
        Collection c2 = objToConstraints.getValues(c.b);
        objToConstraints.add(c.a, c);
        objToConstraints.add(c.b, c);
        if (c instanceof BeforeConstraint) {
            addPrecedenceConstraints(c.a, c.b, c1);
            addPrecedenceConstraints(c.a, c.b, c2);
        } else {
            addPrecedenceConstraints(c.b, c.a, c1);
            addPrecedenceConstraints(c.b, c.a, c2);
        }
    }
    void addPrecedenceConstraints(Object a, Object b, Collection ocs) {
        Assert._assert(!a.equals(b));
        // Make a backup to avoid ConcurrentModificationException
        ocs = new ArrayList(ocs);
        for (Iterator i = ocs.iterator(); i.hasNext(); ) {
            OrderConstraint oc = (OrderConstraint) i.next();
            Object c, d;
            if (oc instanceof AfterConstraint) {
                c = oc.b; d = oc.a;
            } else {
                c = oc.a; d = oc.b;
                if (oc instanceof InterleaveConstraint)
                    checkTransitivePrecedence(a, b, d, c);
            }
            checkTransitivePrecedence(a, b, c, d);
        }
    }
    
    /**
     * Find the earliest element in the order.
     */
    public Object findEarliest(Object o) {
        return findEarliest(o, null);
    }
    /**
     * Find the earliest element in the order, other than the orders in the skip set.
     */
    public Object findEarliest(Object o, Set skip) {
        Collection c = objToConstraints.getValues(o);
        if (c != null) {
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                OrderConstraint oc = (OrderConstraint) i.next();
                if (oc instanceof BeforeConstraint) {
                    if (o.equals(oc.b) &&
                        (skip == null || !skip.contains(oc.a))) {
                        return findEarliest(oc.a, skip);
                    }
                } else if (oc instanceof AfterConstraint) {
                    if (o.equals(oc.a) &&
                        (skip == null || !skip.contains(oc.b))) {
                        return findEarliest(oc.b, skip);
                    }
                }
            }
        }
        return o;
    }
    
    /**
     * Get the elements that must be interleaved with this element.
     * 
     * @param o  element
     * @return  collection of interleaved elements, including o
     */
    Collection getInterleaved(Object o) {
        Collection result = new LinkedList();
        Set visited = new HashSet();
        result.add(o);
        visited.add(o);
        Collection c = objToConstraints.getValues(o);
        if (c != null) {
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                OrderConstraint oc = (OrderConstraint) i.next();
                if (oc instanceof InterleaveConstraint) {
                    if (o.equals(oc.a)) {
                        if (visited.add(oc.b))
                            result.add(oc.b);
                    } else {
                        if (visited.add(oc.a))
                            result.add(oc.a);
                    }
                }
            }
        }
        return result;
    }
    
    public boolean hasPredecessor(Object o, Collection skip) {
        Collection cons = objToConstraints.getValues(o);
        for (Iterator i = cons.iterator(); i.hasNext(); ) {
            OrderConstraint oc = (OrderConstraint) i.next();
            if (oc instanceof BeforeConstraint) {
                if (o.equals(oc.b) &&
                    (skip == null || !skip.contains(oc.a))) {
                    return true;
                }
            } else if (oc instanceof AfterConstraint) {
                if (o.equals(oc.a) &&
                    (skip == null || !skip.contains(oc.b))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    BigInteger countAllWays(Collection vars, List earliest, Set skip) {
        BigInteger result = BigInteger.ZERO;
        if (earliest.size() == 0) {
            return BigInteger.ONE;
        }
        Iterator i = earliest.iterator();
        while (i.hasNext()) {
            // Choose c to be first.
            Collection c = (Collection) i.next();
            //Assert._assert(!skip.containsAny(c));
            skip.addAll(c);
            List newEarliest = findAllEarliest(vars, skip);
            BigInteger r = countAllWays(vars, newEarliest, skip);
            skip.removeAll(c);
            result = result.add(r);
        }
        int num = earliest.size();
        for (int k = 2; k <= num; ++k) {
            // Do all interleaves combining k elements.
            CombinationGenerator cg = new CombinationGenerator(num, k);
            while (cg.hasMore()) {
                int[] p = cg.getNext();
                Collection combo = new LinkedList();
                for (int x = 0; x < p.length; ++x) {
                    Collection c = (Collection) earliest.get(p[x]);
                    combo.addAll(c);
                }
                //Assert._assert(!skip.containsAny(combo));
                skip.addAll(combo);
                List newEarliest = findAllEarliest(vars, skip);
                BigInteger r = countAllWays(vars, newEarliest, skip);
                skip.removeAll(combo);
                result = result.add(r);
            }
        }
        return result;
    }
    
    List combineAllWays(Collection vars, List earliest, Set skip) {
        List result = new LinkedList();
        if (earliest.size() == 0) {
            result.add(Collections.EMPTY_LIST);
            return result;
        }
        Iterator i = earliest.iterator();
        while (i.hasNext()) {
            // Choose c to be first.
            Collection c = (Collection) i.next();
            //Assert._assert(!skip.containsAny(c));
            skip.addAll(c);
            List newEarliest = findAllEarliest(vars, skip);
            //System.out.println(" > combineAllWays("+newEarliest+", "+skip+")");
            List r = combineAllWays(vars, newEarliest, skip);
            //System.out.println(" < combineAllWays("+newEarliest+", "+skip+") = "+r);
            skip.removeAll(c);
            for (Iterator j = r.iterator(); j.hasNext(); ) {
                List list = (List) j.next();
                List list2 = new ArrayList(list);
                list2.add(0, c);
                //System.out.println("    Adding "+list2+" to result");
                result.add(list2);
            }
        }
        int num = earliest.size();
        for (int k = 2; k <= num; ++k) {
            // Do all interleaves combining k elements.
            CombinationGenerator cg = new CombinationGenerator(num, k);
            while (cg.hasMore()) {
                Collection combo = new LinkedList();
                int[] p = cg.getNext();
                for (int x = 0; x < p.length; ++x) {
                    Collection c = (Collection) earliest.get(p[x]);
                    combo.addAll(c);
                }
                //Assert._assert(!skip.containsAny(combo));
                skip.addAll(combo);
                List newEarliest = findAllEarliest(vars, skip);
                //System.out.println("  > combineAllWays("+newEarliest+", "+skip+")");
                List r = combineAllWays(vars, newEarliest, skip);
                //System.out.println("  < combineAllWays("+newEarliest+", "+skip+") = "+r);
                skip.removeAll(combo);
                for (Iterator j = r.iterator(); j.hasNext(); ) {
                    List list = (List) j.next();
                    List list2 = new ArrayList(list);
                    list2.add(0, combo);
                    //System.out.println("     Adding "+list2+" to result");
                    result.add(list2);
                }
            }
        }
        return result;
    }
    
    List findAllEarliest(Collection vars, Collection skip) {
        List earliest = new LinkedList();
        Set mySkip = new HashSet();
        if (skip != null) mySkip.addAll(skip);
        for (Iterator i = vars.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (mySkip != null && mySkip.contains(o)) continue;
            if (hasPredecessor(o, skip)) continue;
            Collection c = getInterleaved(o);
            mySkip.addAll(c);
            earliest.add(c);
        }
        return earliest;
    }
    
    public boolean onlyOneOrder(int n) {
        if (n == 0 || n == 1) return true;
        int maxc = n * (n-1) / 2;
        return set.size() == maxc;
    }
    
    static final double LOG2 = Math.log(2);
    
    public int approxNumOrders(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        double result = Distributions.recurrenceA000670(n).doubleValue();
        double nc = Math.log(set.size()) / LOG2;
        if (nc < 0) nc = 0;
        double maxc = Math.log(n * (n-1) / 2) / LOG2;
        if (maxc < 1) maxc = 1;
        double result2 = result - ((result - 1) * nc / maxc);
        double max = (double) Integer.MAX_VALUE;
        if (result2 > max) return Integer.MAX_VALUE;
        else return (int) result2;
    }
    
    public BigInteger countAllOrders(Collection vars) {
        return countAllOrders(vars, null);
    }
    
    public BigInteger countAllOrders(Collection vars, Set skip) {
        List earliest = findAllEarliest(vars, skip);
        if (skip == null) skip = new HashSet();
        BigInteger result = countAllWays(vars, earliest, skip);
        return result;
    }
    
    public List generateAllOrders(Collection vars) {
        return generateAllOrders(vars, null);
    }
    
    public List generateAllOrders(Collection vars, Set skip) {
        
        //System.out.println("Generating all orders for "+this);
        
        // Find the set of earliest elements (no predecessors)
        List earliest = findAllEarliest(vars, skip);
        //System.out.println("findAllEarliest("+vars+","+skip+") = "+earliest);
        
        if (skip == null) skip = new HashSet();
        
        // Combine those earliest elements in all possible ways.
        List result = combineAllWays(vars, earliest, skip);
        
        // Convert Lists to Orders.
        for (ListIterator i = result.listIterator(); i.hasNext(); ) {
            List a = (List) i.next();
            for (ListIterator j = a.listIterator(); j.hasNext(); ) {
                Collection c = (Collection) j.next();
                if (c.size() == 1)
                    j.set(c.iterator().next());
            }
            Order o = new Order(a);
            i.set(o);
        }
        
        return result;
    }
    
    static Random random = new Random(System.currentTimeMillis());
    public Order generateRandomOrder(Collection vars) {
        Set done = new HashSet(vars.size());
        ArrayList input = new ArrayList(vars);
        List result = new ArrayList(vars.size());
        while (!input.isEmpty()) {
            int r = random.nextInt(input.size());
            Object o = input.get(r);
            o = findEarliest(o, done);
            Collection c = getInterleaved(o);
            boolean interleave = false;
            if (!result.isEmpty()) {
                Object prev = result.get(result.size()-1);
                if (prev instanceof Collection) prev = ((Collection) prev).iterator().next();
                if (!set.contains(OrderConstraint.makePrecedenceConstraint(prev, o))) {
                    // It is possible to interleave with previous.
                    interleave = random.nextBoolean();
                }
            }
            if (interleave) {
                Object prev = result.get(result.size()-1);
                if (prev instanceof Collection) ((Collection) prev).addAll(c);
                else {
                    c.add(prev);
                    result.set(result.size()-1, c);
                }
            } else {
                if (c.size() == 1) result.add(c.iterator().next());
                else result.add(c);
            }
            done.addAll(c);
            input.removeAll(c);
        }
        Order o = new Order(result);
        return o;
    }
    
    public String toString() {
        return set.toString();
    }
    
    public static void main(String[] args) {
        OrderConstraintSet dis = new OrderConstraintSet();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Set allStrings = new HashSet();
        try {
            for (;;) {
                System.out.print("Enter constraint: ");
                String s = in.readLine();
                if (s == null) break;
                if (s.equals("done")) break;
                if (s.length() == 0) continue;
                StringTokenizer st = new StringTokenizer(s, "<>~", true);
                String a = st.nextToken();
                if (!st.hasMoreTokens()) {
                    allStrings.add(a);
                    continue;
                }
                String op = st.nextToken();
                String b = st.nextToken();
                OrderConstraint c;
                if (op.equals("<")) c = OrderConstraint.makePrecedenceConstraint(a, b);
                else if (op.equals(">")) c = OrderConstraint.makePrecedenceConstraint(b, a);
                else if (op.equals("~")) c = OrderConstraint.makeInterleaveConstraint(a, b);
                else {
                    continue;
                }
                if (dis.constrain(c)) {
                    System.out.println("Constraints now: "+dis);
                } else {
                    System.out.println("Cannot add constraint!");
                }
                allStrings.add(a);
                allStrings.add(b);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = 0; i < 10; ++i) {
            Order o = dis.generateRandomOrder(allStrings);
            System.out.println("One random order is: "+o);
        }
        BigInteger c = dis.countAllOrders(allStrings);
        System.out.println("Total number of orders: "+c);
        List allOrders = dis.generateAllOrders(allStrings);
        System.out.println("All orders ("+allOrders.size()+"): "+allOrders);
    }
}
