// BDDOperationInterpreter.java, created Jun 29, 2004 12:54:42 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.BDDSolver;
import net.sf.bddbddb.Domain;
import net.sf.bddbddb.ir.dynamic.If;
import net.sf.bddbddb.ir.dynamic.Nop;
import net.sf.bddbddb.ir.highlevel.Copy;
import net.sf.bddbddb.ir.highlevel.Difference;
import net.sf.bddbddb.ir.highlevel.Free;
import net.sf.bddbddb.ir.highlevel.GenConstant;
import net.sf.bddbddb.ir.highlevel.Invert;
import net.sf.bddbddb.ir.highlevel.Join;
import net.sf.bddbddb.ir.highlevel.JoinConstant;
import net.sf.bddbddb.ir.highlevel.Load;
import net.sf.bddbddb.ir.highlevel.Project;
import net.sf.bddbddb.ir.highlevel.Rename;
import net.sf.bddbddb.ir.highlevel.Save;
import net.sf.bddbddb.ir.highlevel.Union;
import net.sf.bddbddb.ir.highlevel.Universe;
import net.sf.bddbddb.ir.highlevel.Zero;
import net.sf.bddbddb.ir.lowlevel.ApplyEx;
import net.sf.bddbddb.ir.lowlevel.BDDProject;
import net.sf.bddbddb.ir.lowlevel.Replace;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * BDDOperationInterpreter
 * 
 * @author jwhaley
 * @version $Id: BDDOperationInterpreter.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class BDDOperationInterpreter implements OperationInterpreter {
    boolean TRACE = SystemProperties.getProperty("traceinterpreter") != null;
    BDDFactory factory;
    String varorder;
    BDDSolver solver;
    public boolean needsDomainMatch;

    /**
     * @param factory
     */
    public BDDOperationInterpreter(BDDSolver solver, BDDFactory factory) {
        this.solver = solver;
        this.factory = factory;
        this.varorder = solver.VARORDER;
        this.needsDomainMatch = true;
    }
    public static boolean CHECK = !SystemProperties.getProperty("checkir", "no").equals("no");

    protected BDD makeDomainsMatch(BDD b, BDDRelation r1, BDDRelation r2) {
        if (CHECK) {
            r1.verify();
            r2.verify();
        }
        if (!needsDomainMatch) return b;
        boolean any = false;
 
        BDDPairing pair = factory.makePair();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d1 = r1.getBDDDomain(a);
            BDDDomain d2 = r2.getBDDDomain(a);
            if (d2 == null || d1 == d2) continue;
            any = true;
            pair.set(d1, d2);
            if (TRACE) solver.out.println("   " + a + " Renaming " + d1 + " to " + d2);
            if (CHECK && varorder != null) {
                int index1 = varorder.indexOf(d1.toString());
                int index2 = varorder.indexOf(d2.toString());
                for (Iterator j = r2.getAttributes().iterator(); j.hasNext();) {
                    Attribute a2 = (Attribute) j.next();
                    if (a2 == a) continue;
                    BDDDomain d3 = r2.getBDDDomain(a2);
                    int index3 = varorder.indexOf(d3.toString());
                    boolean bad;
                    if (index1 < index2) bad = (index3 >= index1 && index3 <= index2);
                    else bad = (index3 >= index2 && index3 <= index1);
                    if (bad) {
                        if (TRACE) solver.out.println("Expensive rename! " + r1 + "->" + r2 + ": " + d1 + " to " + d2 + " across " + d3);
                    }
                }
            }
        }
        if (any) {
            if (TRACE) solver.out.println("      Rename to make " + r1 + " match " + r2);
            b.replaceWith(pair);
            if (TRACE) solver.out.println("      Domains of result: " + BDDRelation.activeDomains(b));
        }
        pair.reset();
        return b;
    }

    BDDDomain getUnusedDomain(Domain d, Collection dontuse) {
        for (Iterator i = solver.getBDDDomains(d).iterator(); i.hasNext(); ) {
            BDDDomain dd = (BDDDomain) i.next();
            if (!dontuse.contains(dd)) return dd;
        }
        BDDDomain dd = solver.allocateBDDDomain(d);
        return dd;
    }
    
    BDD getProjectSet(Map m, BDDRelation r1, BDDRelation r2, BDDRelation r3) {
        BDD b = factory.one();
        for (Iterator i = r2.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (r1.getAttributes().contains(a)) continue;
            BDDDomain d = (m != null)?(BDDDomain)m.get(a):r2.getBDDDomain(a);
            b.andWith(d.set());
        }
        for (Iterator i = r3.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            if (r1.getAttributes().contains(a)) continue;
            BDDDomain d = (m != null)?(BDDDomain)m.get(a):r3.getBDDDomain(a);
            b.andWith(d.set());
        }
        return b;
    }
    
    protected BDD makeDomainsMatch(BDD b2, BDD b3, BDDRelation r1, BDDRelation r2, BDDRelation r3) {
        if (CHECK) {
            r1.verify();
            r2.verify();
            r3.verify();
        }
        if (!needsDomainMatch) {
            return getProjectSet(null, r1, r2, r3);
        }
        boolean any2 = false, any3 = false;
        BDDPairing pair2 = factory.makePair();
        BDDPairing pair3 = factory.makePair();
        Map renameMap = new HashMap();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d1 = r1.getBDDDomain(a);
            renameMap.put(a, d1);
        }
        for (Iterator i = r2.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d2 = r2.getBDDDomain(a);
            BDDDomain d1 = (BDDDomain) renameMap.get(a);
            if (d1 == null) {
                if (!renameMap.values().contains(d2)) d1 = d2;
                else d1 = getUnusedDomain(a.getDomain(), renameMap.values());
                renameMap.put(a, d1);
            }
            if (d1 != d2) {
                pair2.set(d2, d1);
                any2 = true;
                if (TRACE) solver.out.println("   "+r2+": " + a + " Renaming " + d2 + " to " + d1);
            }
        }
        for (Iterator i = r3.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d3 = r3.getBDDDomain(a);
            BDDDomain d1 = (BDDDomain) renameMap.get(a);
            if (d1 == null) {
                if (!renameMap.values().contains(d3)) d1 = d3;
                else d1 = getUnusedDomain(a.getDomain(), renameMap.values());
                renameMap.put(a, d1);
            }
            if (d1 != d3) {
                pair3.set(d3, d1);
                any3 = true;
                if (TRACE) solver.out.println("   "+r3+": " + a + " Renaming " + d3 + " to " + d1);
            }
        }
        if (any2) {
            if (TRACE) solver.out.println("      Rename to make " + r2 + " match " + r1);
            b2.replaceWith(pair2);
            if (TRACE) solver.out.println("      Domains of result: " + BDDRelation.activeDomains(b2));
        }
        if (any3) {
            if (TRACE) solver.out.println("      Rename to make " + r3 + " match " + r1);
            b3.replaceWith(pair3);
            if (TRACE) solver.out.println("      Domains of result: " + BDDRelation.activeDomains(b3));
        }
        pair2.reset();
        pair3.reset();
        return getProjectSet(renameMap, r1, r2, r3);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Join)
     */
    public Object visit(Join op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc1();
        BDDRelation r2 = (BDDRelation) op.getSrc2();
        BDD b1 = makeDomainsMatch(r1.getBDD().id(), r1, r0);
        BDD b2 = makeDomainsMatch(r2.getBDD().id(), r2, r0);
        if (TRACE) solver.out.println("   And " + r1 + "," + r2);
        b1.andWith(b2);
        r0.setBDD(b1);
        if (TRACE) solver.out.println("   ---> Nodes: " + b1.nodeCount() + " Domains: " + BDDRelation.activeDomains(b1));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Project)
     */
    public Object visit(Project op) {
        if(!needsDomainMatch) 
            Assert.UNREACHABLE(); /* if we ran domain assignment we should only see BDDProjects */
        
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        List attributes = op.getAttributes();
        BDD b = factory.one();
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d = r1.getBDDDomain(a);
            b.andWith(d.set());
            if (TRACE) solver.out.println("   Projecting " + d);
        }
        if (TRACE) solver.out.println("   Exist " + r1);
        BDD r = r1.getBDD().exist(b);
        b.free();
        BDD b1 = makeDomainsMatch(r, r1, r0);
        r0.setBDD(b1);
        if (TRACE) solver.out.println("   ---> Nodes: " + b1.nodeCount() + " Domains: " + BDDRelation.activeDomains(b1));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }
    
    public Object visit(BDDProject op) {
        if(needsDomainMatch)
            Assert.UNREACHABLE(); /* without domain assignment we can't see these */
            
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        List domains = op.getDomains();
        BDD b = factory.one();
        for (Iterator i = domains.iterator(); i.hasNext();) {
            BDDDomain d = (BDDDomain) i.next();
            b.andWith(d.set());
            if (TRACE) solver.out.println("   Projecting " + d);
        }
        if (TRACE) solver.out.println("   Exist " + r1);
        BDD r = r1.getBDD().exist(b);
        b.free();
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount() + " Domains: " + BDDRelation.activeDomains(r));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Rename)
     */
    public Object visit(Rename op) {
        if(!needsDomainMatch)
            Assert.UNREACHABLE(); /* all renames should be removed after domains assignment */
        
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        if (CHECK) {
            r1.verify();
        }
        Map renames = op.getRenameMap();
        boolean any = false;
        BDDPairing pair = factory.makePair();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a1 = (Attribute) i.next();
            BDDDomain d1 = r1.getBDDDomain(a1);
            Attribute a0 = (Attribute) renames.get(a1);
            if (a0 == null) a0 = a1;
            BDDDomain d0 = r0.getBDDDomain(a0);
            Assert._assert(d0 != null);
            if (d1.equals(d0)) continue;
            any = true;
            pair.set(d1, d0);
            if (TRACE) solver.out.println("   Renaming " + d1 + " to " + d0);
        }
        BDD b = r1.getBDD().id();
        if (any) {
            if (TRACE) solver.out.println("   " + r0 + " = Replace " + r1);
            b.replaceWith(pair);
        }
        pair.reset();
        r0.setBDD(b);
        if (TRACE) solver.out.println("   ---> Nodes: " + b.nodeCount() + " Domains: " + BDDRelation.activeDomains(b));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        if (CHECK) {
            r0.verify();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Union)
     */
    public Object visit(Union op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc1();
        BDDRelation r2 = (BDDRelation) op.getSrc2();
        BDD b1 = makeDomainsMatch(r1.getBDD().id(), r1, r0);
        BDD b2 = makeDomainsMatch(r2.getBDD().id(), r2, r0);
        if (TRACE) solver.out.println("   Or " + r1 + "," + r2);
        b1.orWith(b2);
        r0.setBDD(b1);
        if (TRACE) solver.out.println("   ---> Nodes: " + b1.nodeCount() + " Domains: " + BDDRelation.activeDomains(b1));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Difference)
     */
    public Object visit(Difference op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc1();
        BDDRelation r2 = (BDDRelation) op.getSrc2();
        BDD b1 = makeDomainsMatch(r1.getBDD().id(), r1, r0);
        BDD b2 = makeDomainsMatch(r2.getBDD().id(), r2, r0);
        if (TRACE) solver.out.println("   " + r0 + " = Diff " + r1 + "," + r2);
        b1.applyWith(b2, BDDFactory.diff);
        r0.setBDD(b1);
        if (TRACE) solver.out.println("   ---> Nodes: " + b1.nodeCount() + " Domains: " + BDDRelation.activeDomains(b1));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.JoinConstant)
     */
    public Object visit(JoinConstant op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        Attribute a = op.getAttribute();
        if(TRACE) solver.out.println(r0 + ": " + r0.getAttributes() + " " + r0.getBDDDomains());
        if(TRACE) solver.out.println(r1 + ": " + r1.getAttributes() + " " + r1.getBDDDomains());
        long value = op.getValue();
        BDD r = makeDomainsMatch(r1.getBDD().id(), r1, r0);
        if (TRACE) solver.out.println("   " + r0 + " = And " + r1 + "," + r0.getBDDDomain(a) + ":" + value);
        r.andWith(r0.getBDDDomain(a).ithVar(value));
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount() + " Domains: " + BDDRelation.activeDomains(r));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.GenConstant)
     */
    public Object visit(GenConstant op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        Attribute a = op.getAttribute();
        long value = op.getValue();
        if (TRACE) solver.out.println("   " + r0 + " = Ithvar " + r0.getBDDDomain(a) + ":" + value);
        BDD r = r0.getBDDDomain(a).ithVar(value);
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount());
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Free)
     */
    public Object visit(Free op) {
        BDDRelation r = (BDDRelation) op.getSrc();
        if (TRACE) solver.out.println("   Free " + r);
        r.free();
        //BDD b = factory.zero();
        //r.setBDD(b);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Zero)
     */
    public Object visit(Zero op) {
        BDDRelation r = (BDDRelation) op.getRelationDest();
        BDD b = factory.zero();
        if (TRACE) solver.out.println("   Zero " + r);
        r.setBDD(b);
        if (TRACE) solver.out.println("   ---> Nodes: " + b.nodeCount());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Universe)
     */
    public Object visit(Universe op) {
        BDDRelation r = (BDDRelation) op.getRelationDest();
        BDD b = factory.one();
        for (Iterator i = r.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d = r.getBDDDomain(a);
            b.andWith(d.domain());
        }
        if (TRACE) solver.out.println("   Domain " + r);
        r.setBDD(b);
        if (TRACE) solver.out.println("   ---> Nodes: " + b.nodeCount());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Invert)
     */
    public Object visit(Invert op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        if (TRACE) solver.out.println("   " + r0 + " = Not " + r1);
        BDD r = makeDomainsMatch(r1.getBDD().not(), r1, r0);
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount() + " Domains: " + BDDRelation.activeDomains(r));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Copy)
     */
    public Object visit(Copy op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        if (TRACE) solver.out.println("   " + r0 + " = Id " + r1);
        BDD r = makeDomainsMatch(r1.getBDD().id(), r1, r0);
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount() + " Domains: " + BDDRelation.activeDomains(r));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.Load)
     */
    public Object visit(Load op) {
        BDDRelation r = (BDDRelation) op.getRelationDest();
        try {
            if (op.isTuples()) {
                r.loadTuples(op.getFileName());
            } else {
                r.load(op.getFileName());
            }
        } catch (IOException x) {
        }
        solver.startTime = System.currentTimeMillis();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.HighLevelInterpreter#visit(net.sf.bddbddb.ir.Save)
     */
    public Object visit(Save op) {
        long time = System.currentTimeMillis();
        BDDRelation r = (BDDRelation) op.getSrc();
        try {
            if (op.isTuples()) {
                r.saveTuples(op.getFileName());
            } else {
                r.save(op.getFileName());
            }
        } catch (IOException x) {
        }
        solver.startTime += (System.currentTimeMillis() - time);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.ApplyEx)
     */
    public Object visit(ApplyEx op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc1();
        BDDRelation r2 = (BDDRelation) op.getSrc2();
        BDDOp bddop = op.getOp();
        //solver.out.println("Dest: " + r0 + " attrs: " + r0.getAttributes() + " domains: " + r0.getBDDDomains());
        //solver.out.println("Src1: " + r1 + " attrs: " + r1.getAttributes() + " domains: " + r1.getBDDDomains());
        //solver.out.println("Src2: " + r2 + " attrs: " + r2.getAttributes() + " domains: " + r2.getBDDDomains());
     
        BDD b1 = r1.getBDD().id();
        BDD b2 = r2.getBDD().id();
        BDD b3 = needsDomainMatch ? makeDomainsMatch(b1, b2, r0, r1, r2) : op.getProjectSet();
        //if (TRACE) solver.out.println("   " + op.toString());
        BDD b = b1.applyEx(b2, bddop, b3);
        b1.free();
        b2.free();
        b3.free();
        r0.setBDD(b);
        if (TRACE) solver.out.println("   ---> Nodes: " + b.nodeCount() + " Domains: " + BDDRelation.activeDomains(b));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        if (CHECK) {
            r0.verify();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.If)
     */
    public Object visit(If op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.Nop)
     */
    public Object visit(Nop op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.Replace)
     */
    public Object visit(Replace op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        BDDRelation r1 = (BDDRelation) op.getSrc();
        BDDPairing pair = op.getPairing();
        BDD r = pair != null ? r1.getBDD().replace(pair) : r1.getBDD().id();
        r0.setBDD(r);
        if (TRACE) solver.out.println("   ---> Nodes: " + r.nodeCount() + " Domains: " + BDDRelation.activeDomains(r));
        if (TRACE) solver.out.println("   ---> " + r0 + "+ elements: "+r0.dsize());
        return null;
    }
}
