// IterationFlowGraph.java, created Jun 29, 2004
// Copyright (C) 2004 Michael Carbin
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.HashWorklist;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.graphs.SCComponent;

/**
 * IterationFlowGraph
 * 
 * @author mcarbin
 * @version $Id: IterationFlowGraph.java 339 2004-10-18 04:17:51Z joewhaley $
 */
public class IterationFlowGraph {
    IterationList iterationElements;
    MultiMap innerSCCs;
    List firstSCCs, rules, loops;
    MultiMap containedBy;
    MultiMap dependencies;

    public IterationFlowGraph(List rules, Stratify strat) {
        this(rules, strat.firstSCCs, strat.innerSCCs);
    }
    
    public IterationFlowGraph(List rules, List firstSCCs, MultiMap innerSCCs) {
        this.firstSCCs = firstSCCs;
        this.innerSCCs = innerSCCs;
        this.rules = rules;
        dependencies = new GenericMultiMap();
        loops = new LinkedList();
        iterationElements = new IterationList(false);
        containedBy = new GenericMultiMap();
        constructStrataLoops();
        constructDependencies();
    }

    private void constructStrataLoops() {
        for (Iterator it = firstSCCs.iterator(); it.hasNext();) {
            SCComponent scc = (SCComponent) it.next();
            IterationList loop = buildIterationList(scc, scc.isLoop());
            iterationElements.addElement(loop);
        }
    }

    private void constructDependencies() {
        constructRuleDependencies();
        constructListDependencies(iterationElements);
    }

    private void constructRuleDependencies() {
        InferenceRule.DependenceNavigator depNav = new InferenceRule.DependenceNavigator(rules);
        HashWorklist w = new HashWorklist(true);
        for (Iterator it = rules.iterator(); it.hasNext();) {
            Object rule = it.next();
            w.add(new Pair(rule, rule));
        }
        //transitive closure
        while (!w.isEmpty()) {
            Pair pair = (Pair) w.pull();
            Object link = pair.get(0);
            Object initRule = pair.get(1);
            Collection usedRelations = depNav.prev(link);
            for (Iterator it2 = usedRelations.iterator(); it2.hasNext();) {
                Collection definingRules = depNav.prev(it2.next());
                for (Iterator it3 = definingRules.iterator(); it3.hasNext();) {
                    Object o = it3.next();
                    dependencies.add(initRule, o);
                    w.add(new Pair(o, initRule));
                }
            }
        }
    }

    private void constructListDependencies(IterationList list) {
        for (Iterator it = list.iterator(); it.hasNext();) {
            IterationElement elem = (IterationElement) it.next();
            if (elem instanceof InferenceRule) {
                Collection ruleDepends = dependencies.getValues(elem);
                for (Iterator it2 = ruleDepends.iterator(); it2.hasNext();) {
                    IterationElement elem2 = (IterationElement) it2.next();
                    //nothing
                }
            } else if (elem instanceof IterationList) {
                constructListDependencies((IterationList) elem);
                Collection listDepends = dependencies.getValues(elem);
                for (Iterator it2 = listDepends.iterator(); it2.hasNext();) {
                    IterationElement elem2 = (IterationElement) it2.next();
                    //nothing
                }
            }
        }
    }

    IterationList buildIterationList(SCComponent scc, boolean isLoop) {
        IterationList list = new IterationList(isLoop);
        while (scc != null) {
            Collection c = innerSCCs.getValues(scc);
            if (c.isEmpty()) {
                for (Iterator it = scc.nodeSet().iterator(); it.hasNext();) {
                    Object o = it.next();
                    if (o instanceof InferenceRule) {
                        InferenceRule rule = (InferenceRule) o;
                        list.addElement(rule);
                        containedBy.add(rule, list);
                    }
                }
            } else {
                for (Iterator it = c.iterator(); it.hasNext();) {
                    SCComponent scc2 = (SCComponent) it.next();
                    IterationList childLoop = buildIterationList(scc2, scc.isLoop());
                    list.addElement(childLoop);
                    Collection childElems = childLoop.getAllNestedElements();
                    for (Iterator it2 = childElems.iterator(); it2.hasNext();) {
                        containedBy.add(it2.next(), list);
                    }
                }
            }
            scc = scc.nextTopSort();
        }
        if (list.isLoop()) {
            loops.add(list);
        }
        return list;
    }

    public String toString() {
        return iterationElements.toString();
    }

    public boolean dependsOn(IterationElement e1, IterationElement e2) {
        return dependencies.getValues(e1).contains(e2);
    }

    public IterationList getIterationList() {
        return iterationElements;
    }

    public IterationList expand() {
        if (iterationElements.isLoop()) {
            IterationList unrolled = iterationElements.unroll();
            iterationElements.expandInLoop();
            unrolled.elements.add(iterationElements);
            iterationElements = unrolled;
        } else {
            iterationElements.expand(true);
        }
        return iterationElements;
    }

    /**
     * @return Returns the loops.
     */
    public List getLoops() {
        return loops;
    }
    
    public IterationElement getIterationElement(String s) {
        return iterationElements.getElement(s);
    }
}
