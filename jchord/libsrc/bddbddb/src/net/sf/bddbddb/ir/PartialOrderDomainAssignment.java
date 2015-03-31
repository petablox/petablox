// PartialOrderDomainAssignment.java, created Jul 11, 2004 12:59:33 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.collections.UnionFind;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDSolver;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;
import net.sf.bddbddb.dataflow.PartialOrder.ConstraintGraph;
import net.sf.bddbddb.dataflow.PartialOrder.Constraints;

/**
 * PartialOrderDomainAssignment
 * 
 * @author John Whaley
 * @version $Id: PartialOrderDomainAssignment.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class PartialOrderDomainAssignment extends UFDomainAssignment {
    Collection beforeConstraints;
    List ileavedConstraints;

    /**
     * @param s
     */
    public PartialOrderDomainAssignment(Solver s, Constraints[] constraintMap) {
        super(s, constraintMap); // super() calls initialize() for us.
    }

    void initialize() {
        beforeConstraints = new LinkedList();
        ileavedConstraints = new LinkedList();
        super.initialize();
    }
    
    public void doAssignment() {
        super.doAssignment();
    }

    boolean wouldBeLegal(Object a1, Object a2) {
        if (!super.wouldBeLegal(a1, a2)) return false;
        List path = new LinkedList();
        ConstraintGraph graph = new ConstraintGraph(beforeConstraints);
        if (graph.isPath(a1, a2, path) || graph.isPath(a2, a1, path)) {
            if (TRACE) solver.out.println("Cannot,rep cycle detected: " + path);
            return false;
        }
        return true;
    }

    boolean forceBefore(Object o1, Object o2) {
        beforeConstraints = updateConstraints(beforeConstraints);
        Object rep1 = uf.find(o1);
        Object rep2 = uf.find(o2);
        Pair tempConstraint = new Pair(rep1, rep2);
        if (beforeConstraints.contains(tempConstraint)) {
            if (TRACE) solver.out.println(o1 + " already before " + o2);
            return true;
        }
        beforeConstraints.add(tempConstraint);
        List cycle = new LinkedList();
        if (!constraintsSatisfied(cycle)) {
            if (TRACE) solver.out.println("rep cycle detected: " + cycle);
            beforeConstraints.remove(tempConstraint);
            return false;
        }
        if (TRACE) solver.out.println("adding before constraint: " + tempConstraint);
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.ir.DomainAssignment#forceBefore(net.sf.bddbddb.Relation, net.sf.bddbddb.Attribute, net.sf.bddbddb.Relation, net.sf.bddbddb.Attribute)
     */
    boolean forceBefore(Relation r1, Attribute a1, Relation r2, Attribute a2) {
        Pair p1 = new Pair(r1, a1);
        Pair p2 = new Pair(r2, a2);
        return forceBefore(p1, p2);
    }

    boolean forceInterleaved(Object o1, Object o2) {
        if (TRACE) solver.out.println("Forcing " + o1 + " interleaved " + o2);
        //update constraint reps
        ileavedConstraints = updateConstraints(ileavedConstraints);
        Object rep1 = uf.find(o1);
        Object rep2 = uf.find(o2);
        Pair newConstraint = new Pair(rep1, rep2);
        if (ileavedConstraints.contains(newConstraint)) {
            if (TRACE) solver.out.println(o1 + " already interleaved with " + o2);
            return true;
        }
        ileavedConstraints.add(newConstraint);
        if (TRACE) solver.out.println("adding interleaved constraint: " + newConstraint);
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.ir.DomainAssignment#forceBefore(net.sf.bddbddb.Relation, net.sf.bddbddb.Attribute, net.sf.bddbddb.Relation, net.sf.bddbddb.Attribute)
     */
    boolean forceInterleaved(Relation r1, Attribute a1, Relation r2, Attribute a2) {
        Pair p1 = new Pair(r1, a1);
        Pair p2 = new Pair(r2, a2);
        return forceInterleaved(p1, p2);
    }

    boolean constraintsSatisfied(List cycle) {
        if (TRACE) solver.out.println("Before Constraints: " + beforeConstraints);
        ConstraintGraph graph = new ConstraintGraph(beforeConstraints);
        if (TRACE) solver.out.println("Before graph: " + graph);
        return !graph.isCycle(cycle);
    }

    private List updateConstraints(Collection constraints) {
        List newCons = new LinkedList(constraints);
        List adds = new LinkedList();
        for (Iterator it = newCons.iterator(); it.hasNext();) {
            Pair c = (Pair) it.next();
            Object crep1 = uf.find(c.left);
            Object crep2 = uf.find(c.right);
            if (crep1.equals(crep2)){
                it.remove();
                continue;
            }
            if (!crep1.equals(c.left) || !crep2.equals(c.right)) {
                it.remove();
                adds.add(new Pair(crep1, crep2));
            }
        }
        newCons.addAll(adds);
        return newCons;
    }

    public void setVariableOrdering() {
        TRACE = true;
        if (beforeConstraints.size() == 0 && ileavedConstraints.size() == 0) {
            if (TRACE) solver.out.println("No constraints specified using default ordering");
            super.setVariableOrdering();
        }
      //  solver.out.println("beforeConstraints: " + beforeConstraints);
        if (TRACE) solver.out.println("Interleaved constraints: " + ileavedConstraints);
        //solver.out.println("physical domains: " + physicalDomains);
        MultiMap ileavedDomains = new GenericMultiMap();
        Collection nodes = new LinkedHashSet(physicalDomains.keySet());
        
       
        for (Iterator it = ileavedConstraints.iterator(); it.hasNext();) {
            Pair c = (Pair) it.next();
            Object rep1 = uf.find(c.left);
            Object rep2 = uf.find(c.right);
            if (TRACE) solver.out.println("interleave constraint: " + c);
            if (TRACE) solver.out.println(c.left + " rep = " + rep1);
            if (TRACE) solver.out.println(c.right + " rep = " + rep2);
            if (rep1.equals(rep2)) continue;
            nodes.remove(rep1);
            nodes.remove(rep2);
            uf.union(rep1, rep2);
            Object newRep = uf.find(rep1);
            nodes.add(newRep);
            if (TRACE) solver.out.println("New rep: " + newRep);
            List domains = new LinkedList();
            Object d1 = ileavedDomains.get(rep1);
            if (d1 != null) {
                domains.addAll((Collection) d1);
            } else {
                Object mapdomain = physicalDomains.get(rep1);
                domains.add(mapdomain);
            }
            Object d2 = ileavedDomains.get(rep2);
            if (d2 != null) {
                domains.addAll((Collection) d2);
            } else {
                domains.add(physicalDomains.get(rep2));
            }
            if (TRACE) solver.out.println("interleaved: " + domains);
            ileavedDomains.addAll(newRep, domains);
        }
        beforeConstraints = updateConstraints(beforeConstraints);
      //  Constraints cons = new Constraints(new TreeSet(beforeConstraints));
       // cons.satisfy();
      //  beforeConstraints = (SortedSet) cons.getBeforeConstraints();
        ConstraintGraph graph = new ConstraintGraph(nodes, beforeConstraints);
        
        if (TRACE) solver.out.println("Nodes: " + nodes);
        if (TRACE) solver.out.println("Constraints: " + beforeConstraints);
        String order = graphToOrder(TRACE, graph, uf, ileavedDomains, physicalDomains);
        BDDSolver s = (BDDSolver) solver;
        s.VARORDER = order;
        s.setVariableOrdering();
    }

    public static String graphToOrder(boolean trace, ConstraintGraph graph, UnionFind uf, MultiMap ileavedDomains, Map domainMap){
        
        PrintStream out = System.out;
        Set visited = new HashSet();
        int i = 0;
        if (trace) out.println("Order graph: " + graph);
        StringBuffer sb = new StringBuffer();
        for (Collection roots = graph.getRoots(); !roots.isEmpty();) {
            if (trace) out.println("Nodes left: " + graph.getNodes());
            if (trace) out.println("Roots: " + roots);
            for (Iterator it = roots.iterator(); it.hasNext();) {
                Object root = it.next();
                Object rep = uf.find(root);
                if (!visited.contains(rep)) {
                    sb.append((i != 0) ? "_" : "");
                    i++;
                    visited.add(rep);
                    if (trace) out.println("root: " + rep);
                    Collection ileaved = ileavedDomains.getValues(rep);
                    if (ileaved != null && ileaved.size() != 0) {
                        
                        if (trace) out.println("interleaved");
                        Iterator jt = ileaved.iterator();
                        sb.append(jt.next());
                        while (jt.hasNext())
                            sb.append("x" + jt.next());
                    } else {
                        sb.append(domainMap.get(rep));
                    }
                }
                graph.removeEdgesFrom(root);
                graph.removeNode(root);
            }
            roots = graph.getRoots();
        }
        return sb.toString();
    }
    /* (non-Javadoc)
     * @see net.sf.bddbddb.ir.DomainAssignment#saveDomainAssignment(java.io.BufferedWriter)
     */
    public void saveDomainAssignment(BufferedWriter out) throws IOException {
        super.saveDomainAssignment(out);
        for (Iterator it = beforeConstraints.iterator(); it.hasNext();) {
            Pair c = (Pair) it.next();
            String left;
            if (c.left instanceof Pair) {
                Pair p = (Pair) c.left;
                left = p.left+" "+p.right;
            } else {
                left = c.left.toString();
            }
            String right;
            if (c.right instanceof Pair) {
                Pair p = (Pair) c.right;
                right = p.left+" "+p.right;
            } else {
                right = c.right.toString();
            }
            out.write(left+" < "+right+"\n");
        }
        for (Iterator it = ileavedConstraints.iterator(); it.hasNext();) {
            Pair c = (Pair) it.next();
            String left;
            if (c.left instanceof Pair) {
                Pair p = (Pair) c.left;
                left = p.left+" "+p.right;
            } else {
                left = c.left.toString();
            }
            String right;
            if (c.right instanceof Pair) {
                Pair p = (Pair) c.right;
                right = p.left+" "+p.right;
            } else {
                right = c.right.toString();
            }
            out.write(left+" ~ "+right+"\n");
        }
    }
}
