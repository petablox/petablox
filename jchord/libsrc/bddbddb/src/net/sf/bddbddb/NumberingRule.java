// NumberingRule.java, created May 4, 2004 8:57:36 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import jwutil.graphs.GlobalPathNumbering;
import jwutil.graphs.PathNumbering;
import jwutil.graphs.SCCPathNumbering;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;

/**
 * This class represents a special kind of rule used for numbering paths.
 * 
 * The form of the rule is as follows:
 * 
 * pathNum(a,b,x,y) :- A(a,b),B(b,c),C(c,d),D(d,e). number
 * 
 * The subgoal relations (A, B, C, and D) define the edges of the graph.
 * The first two variables of the head relation define the edge you want
 * to number, and the next two variables are filled in with the number
 * of the source and destination, respectively.
 * 
 * @author jwhaley
 * @version $Id: NumberingRule.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class NumberingRule extends InferenceRule {
    
    /**
     * Trace flag.
     */
    boolean TRACE = false;
    
    /**
     * Trace output stream.
     */
    PrintStream out;
    
    /**
     * Graph of relation.
     */
    RelationGraph rg;
    
    /**
     * Path numbering.
     */
    PathNumbering pn;
    
    /**
     * Time spent on computing this rule.
     */
    long totalTime;
    
    /**
     * Flag to control the dumping of the numbering of the graph in dot format.
     */
    static boolean DUMP_DOTGRAPH = !SystemProperties.getProperty("dumpnumberinggraph", "no").equals("no");

    String numberingType = SystemProperties.getProperty("numberingtype", "scc");
    
    /**
     * Construct a new NumberingRule.
     * Not to be called externally.
     * 
     * @param s
     * @param ir
     */
    NumberingRule(Solver s, InferenceRule ir) {
        super(s, ir.top, ir.bottom, ir.id);
        Assert._assert(ir.top.size() > 1);
        out = s.out;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.InferenceRule#initialize()
     */
    void initialize() {
        if (TRACE) out.println("Initializing numbering rule: " + this);
        RuleTerm root = (RuleTerm) top.get(0);
        Variable rootVar;
        if (root.variables.size() == 1) {
            rootVar = (Variable) root.variables.get(0);
        } else {
            List rootVars = new LinkedList(root.variables);
            calculateNecessaryVariables();
            rootVars.retainAll(necessaryVariables);
            Assert._assert(rootVars.size() == 1);
            rootVar = (Variable) rootVars.get(0);
        }
        if (TRACE) out.println("Root variable: " + rootVar);
        List edges = top.subList(1, top.size());
        rg = new RelationGraph(root, rootVar, edges);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.InferenceRule#split(int)
     */
    public Collection/*<InferenceRule>*/ split(int myIndex) {
        throw new InternalError("Cannot split a numbering rule!");
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.InferenceRule#update()
     */
    public boolean update() {
        if (pn != null) {
            Assert.UNREACHABLE("Can't put numbering in a cycle.");
            return false;
        }
        if (solver.NOISY) solver.out.println("Applying numbering rule:\n   " + this);
        long time = System.currentTimeMillis();
        if (numberingType.equals("scc"))
            pn = new SCCPathNumbering();
        else if (numberingType.equals("global"))
            pn = new GlobalPathNumbering();
        else
            Assert.UNREACHABLE("Unknown numbering type "+numberingType);
        BigInteger num = pn.countPaths(rg);
        if (solver.NOISY) solver.out.println("Done counting paths ("+(System.currentTimeMillis()-time)+" ms, number of paths = "+num+" ("+num.bitLength()+" bits)");
        Iterator i = bottom.variables.iterator();
        Variable v1, v2;
        v1 = (Variable) i.next();
        v2 = (Variable) i.next();
        if (TRACE) out.println("Finding relations with (" + v1 + "," + v2 + ")");
        // Which relation(s) are we talking about here?
        for (i = rg.edges.iterator(); i.hasNext();) {
            RuleTerm rt = (RuleTerm) i.next();
            if (rt.variables.get(0) == v1 && rt.variables.get(1) == v2) {
                if (TRACE) out.println("Match: " + rt);
                // TODO: generalize this to be not BDD-specific
                BDDRelation bddr = (BDDRelation) bottom.relation;
                Iterator k = bddr.domains.iterator();
                BDDDomain d0, d1, d2, d3;
                d0 = (BDDDomain) k.next();
                d1 = (BDDDomain) k.next();
                if (TRACE) out.println("Domains for edge: " + d0 + " -> " + d1);
                d2 = (BDDDomain) k.next();
                d3 = (BDDDomain) k.next();
                if (TRACE) out.println("Domains for numbering: " + d2 + " -> " + d3);
                Assert._assert(d0 != d1);
                Assert._assert(d2 != d3);
                for (TupleIterator j = rt.relation.iterator(); j.hasNext();) {
                    BigInteger[] t = j.nextTuple();
                    Object source = RelationGraph.makeGraphNode(v1, t[0]);
                    Object target = RelationGraph.makeGraphNode(v2, t[1]);
                    PathNumbering.Range r0 = pn.getRange(source);
                    PathNumbering.Range r1 = pn.getEdge(source, target);
                    if (TRACE) out.println("Edge: " + source + " -> " + target + "\t" + r0 + " -> " + r1);
                    if (r0 == null) {
                        if (TRACE) out.println("Unreachable edge!");
                        Assert._assert(r1 == null);
                        continue;
                    }
                    Assert._assert(r1 != null);
                    // TODO: generalize this to be not BDD-specific
                    BDD result = buildMap(d2, PathNumbering.toBigInt(r0.low), PathNumbering.toBigInt(r0.high), d3, PathNumbering.toBigInt(r1.low),
                        PathNumbering.toBigInt(r1.high));
                    result.andWith(d0.ithVar(t[0]));
                    result.andWith(d1.ithVar(t[1]));
                    bddr.relation.orWith(result);
                }
            }
        }
        time = System.currentTimeMillis() - time;
        if (solver.NOISY) out.println("Time spent: " + time + " ms");
        totalTime += time;
        if (DUMP_DOTGRAPH) {
            BufferedWriter dos = null;
            try {
                dos = new BufferedWriter(new FileWriter(solver.basedir + bottom.relation.name + ".dot"));
                pn.dotGraph(dos, rg.getRoots(), rg.getNavigator());
            } catch (IOException x) {
                solver.err.println("Error while dumping dot graph.");
                x.printStackTrace();
            } finally {
                if (dos != null) try {
                    dos.close();
                } catch (IOException x) {
                }
            }
        }
        ((BDDRelation) bottom.relation).updateNegated();
        return true;
    }

    /**
     * Utility function to build a map from a range in one BDD domain to a range in another
     * 
     * @param d1  first domain
     * @param startD1  start of range in d1, inclusive
     * @param endD1    end of range in d1, inclusive
     * @param d2  second domain
     * @param startD2  start of range in d2, inclusive
     * @param endD2    end of range in d2, inclusive
     * @return  BDD representation of map
     */
    static BDD buildMap(BDDDomain d1, BigInteger startD1, BigInteger endD1, BDDDomain d2, BigInteger startD2, BigInteger endD2) {
        BDD r;
        BigInteger sizeD1 = endD1.subtract(startD1);
        BigInteger sizeD2 = endD2.subtract(startD2);
        if (sizeD1.signum() == -1) {
            r = d2.varRange(startD2.longValue(), endD2.longValue());
            r.andWith(d1.ithVar(0));
        } else if (sizeD2.signum() == -1) {
            r = d1.varRange(startD1.longValue(), endD1.longValue());
            r.andWith(d2.ithVar(0));
        } else {
            int bits;
            if (endD1.compareTo(endD2) != -1) { // >=
                bits = endD1.bitLength();
            } else {
                bits = endD2.bitLength();
            }
            long val = startD2.subtract(startD1).longValue();
            r = d1.buildAdd(d2, bits, val);
            if (sizeD2.compareTo(sizeD1) != -1) { // >=
                // D2 is bigger, or they are equal.
                r.andWith(d1.varRange(startD1.longValue(), endD1.longValue()));
            } else {
                // D1 is bigger.
                r.andWith(d2.varRange(startD2.longValue(), endD2.longValue()));
            }
        }
        return r;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.InferenceRule#reportStats()
     */
    public void reportStats() {
        out.println("Rule " + this);
        out.println("   Time: " + totalTime + " ms");
    }
}
