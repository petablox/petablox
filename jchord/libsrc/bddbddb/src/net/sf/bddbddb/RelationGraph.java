// RelationGraph.java, created May 4, 2004 4:08:32 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.PrintStream;
import java.math.BigInteger;
import jwutil.collections.Pair;
import jwutil.graphs.Graph;
import jwutil.graphs.Navigator;
import jwutil.util.Assert;

/**
 * Allows relations to be treated as edges in a graph, so we can use
 * graph algorithms on them.
 * 
 * @author jwhaley
 * @version $Id: RelationGraph.java 350 2004-10-20 01:15:41Z joewhaley $
 */
public class RelationGraph implements Graph {
    
    /**
     * Trace flag.
     */
    protected boolean TRACE = false;
    
    /**
     * Trace output stream.
     */
    protected PrintStream out = System.out;
    
    /**
     * The rule term that represents the set of roots.
     */
    RuleTerm root;
    
    /**
     * The variable used as the root.
     */
    Variable rootVariable;
    
    /**
     * List of rule terms that represent edges in the graph.
     */
    List/*<RuleTerm>*/ edges;

    /**
     * Construct a new relation graph with the given root term, root variable, and list
     * of terms that represent edges in the graph.
     * 
     * @param root  root term
     * @param rootVar  root variable
     * @param edges  list of terms representing edges in the graph
     */
    RelationGraph(RuleTerm root, Variable rootVar, List/*<RuleTerm>*/ edges) {
        this.root = root;
        this.rootVariable = rootVar;
        this.edges = edges;
    }

    /**
     * Construct a new relation graph that uses a given relation as the set of
     * roots and another as the set of edges.  This is just an easier way of 
     * constructing a simple graph so that you don't have to build up rule terms.
     * 
     * @param roots  set of roots
     * @param edges  set of edges
     */
    RelationGraph(Relation roots, Relation edges) {
        Assert._assert(roots.attributes.size() == 1);
        Assert._assert(edges.attributes.size() == 2);
        Attribute a = (Attribute) roots.attributes.get(0);
        Domain fd = a.attributeDomain;
        this.rootVariable = new Variable(fd.toString(), fd);
        List varList = Collections.singletonList(rootVariable);
        this.root = new RuleTerm(roots, varList);
        Assert._assert(edges.getAttribute(0).attributeDomain == fd);
        Assert._assert(edges.getAttribute(1).attributeDomain == fd);
        List varList2 = new Pair(rootVariable, rootVariable);
        RuleTerm edge = new RuleTerm(edges, varList2);
        this.edges = Collections.singletonList(edge);
    }
    
    /**
     * A node in the relation graph.
     */
    static class GraphNode {
        Variable v;
        BigInteger number;

        /**
         * Make a new graph node with the given variable and value.
         * 
         * @param var  variable
         * @param num  value
         */
        GraphNode(Variable var, BigInteger num) {
            this.v = var;
            this.number = num;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return v.hashCode() ^ number.hashCode();
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(GraphNode that) {
            return this.v == that.v && this.number.equals(that.number);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            return equals((GraphNode) o);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return v.toString() + ":" + number;
        }
    }

    /**
     * Make a new graph node with the given variable and value.
     * 
     * @param v  variable
     * @param num  value
     * @return  new graph node
     */
    static GraphNode makeGraphNode(Variable v, BigInteger num) {
        return new GraphNode(v, num);
    }
    
    /**
     * Navigator object for this graph.
     */
    Nav navigator = new Nav();
    
    /**
     * Navigator for a relation graph.
     */
    class Nav implements Navigator {
        
        Map prevCache;
        Map nextCache;
        int cacheHit, cacheMiss;
        
        Nav() {
            prevCache = new HashMap();
            nextCache = new HashMap();
        }
        
        /**
         * Get the edges from a node where the from and to variables match the indices given.
         * 
         * @param node  graph node
         * @param fromIndex  index of from variable
         * @param toIndex  index of to variable
         * @return  collection of edges
         */
        Collection getEdges(Object node, int fromIndex, int toIndex) {
            GraphNode gn = (GraphNode) node;
            if (TRACE) out.println("Getting edges of " + gn + " indices (" + fromIndex + "," + toIndex + ")");
            Collection c = new LinkedList();
            for (Iterator a = edges.iterator(); a.hasNext();) {
                RuleTerm rt = (RuleTerm) a.next();
                if (rt.variables.get(fromIndex) == gn.v) {
                    if (TRACE) out.println("Rule term " + rt + " matches");
                    Variable v2 = (Variable) rt.variables.get(toIndex);
                    TupleIterator i = rt.relation.iterator(fromIndex, gn.number);
                    while (i.hasNext()) {
                        BigInteger[] j = i.nextTuple();
                        c.add(new GraphNode(v2, j[toIndex]));
                    }
                }
            }
            if (TRACE) out.println("Edges: " + c);
            return c;
        }

        void printCacheRatio() {
            System.out.print("Navigating relation graph: ");
            System.out.print(cacheHit+"/"+(cacheHit+cacheMiss)+": ");
            System.out.print(((double)cacheHit/(cacheHit+cacheMiss)));
            System.out.print("                \r");
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see joeq.Util.Graphs.Navigator#next(java.lang.Object)
         */
        public Collection next(Object node) {
            Collection result = (Collection) nextCache.get(node);
            if (result == null) {
                cacheMiss++;
                nextCache.put(node, result = getEdges(node, 0, 1));
            } else {
                cacheHit++;
            }
            if ((cacheMiss + cacheHit) % 256 == 0) printCacheRatio();
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see joeq.Util.Graphs.Navigator#prev(java.lang.Object)
         */
        public Collection prev(Object node) {
            Collection result = (Collection) prevCache.get(node);
            if (result == null) {
                cacheMiss++;
                prevCache.put(node, result = getEdges(node, 1, 0));
            } else {
                cacheHit++;
            }
            if ((cacheMiss + cacheHit) % 256 == 0) printCacheRatio();
            return result;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see joeq.Util.Graphs.Graph#getRoots()
     */
    public Collection getRoots() {
        Relation rootRelation = root.relation;
        int k = root.variables.indexOf(rootVariable);
        Collection roots = new LinkedList();
        TupleIterator i = rootRelation.iterator(k);
        while (i.hasNext()) {
            BigInteger j = i.nextTuple()[0];
            roots.add(new GraphNode(rootVariable, j));
        }
        if (TRACE) out.println("Roots of graph: " + roots);
        return roots;
    }

    /*
     * (non-Javadoc)
     * 
     * @see joeq.Util.Graphs.Graph#getNavigator()
     */
    public Navigator getNavigator() {
        return navigator;
    }
}
