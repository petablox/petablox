// BDDSolver.java, created Mar 16, 2004 12:49:19 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AccessControlException;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.ListFactory;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.ir.BDDInterpreter;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;

/**
 * An implementation of Solver that uses BDDs.
 * 
 * @author jwhaley
 * @version $Id: BDDSolver.java 544 2005-05-11 01:43:12Z joewhaley $
 */
public class BDDSolver extends Solver {
    
    /**
     * Filename for BDD domain info file.
     * The BDD domain info file contains the list of domains that are allocated
     */
    public static String bddDomainInfoFileName = SystemProperties.getProperty("bddinfo", "bddinfo");
    
    /**
     * Link to the BDD factory we use.
     */
    BDDFactory bdd;
    
    /**
     * Map from a field domain to the set of BDD domains we have allocated for that field domain.
     */
    MultiMap fielddomainsToBDDdomains;
    
    Map bddPairings;
    
    public BDDPairing getPairing(Map map) {
        if (bddPairings == null) bddPairings = new HashMap();
        BDDPairing p = (BDDPairing) bddPairings.get(map);
        if (p == null) {
            p = bdd.makePair();
            for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                p.set((BDDDomain) e.getKey(), (BDDDomain) e.getValue());
            }
            bddPairings.put(map, p);
        }
        return p;
    }
    
    public void ensureCapacity(Domain d, long v) {
        ensureCapacity(d, BigInteger.valueOf(v));
    }
    
    public void ensureCapacity(Domain d, BigInteger range) {
        if (d.getSize().compareTo(range) <= 0) {
            d.setSize(range);
            boolean any = false;
            for (Iterator i = this.getBDDDomains(d).iterator(); i.hasNext(); ) {
                if (ensureCapacity((BDDDomain) i.next(), range)) any = true;
            }
            if (any) {
                out.println("Growing domain "+d+" to "+d.getSize());
                for (Iterator i = this.getBDDDomains(d).iterator(); i.hasNext(); ) {
                    redoPairings((BDDDomain) i.next(), range);
                }
                for (Iterator i = this.getRelations().iterator(); i.hasNext(); ) {
                    BDDRelation r = (BDDRelation) i.next();
                    r.calculateDomainSet();
                    if (!r.relation.isZero())
                        out.println("Relation "+r+" domains "+BDDRelation.activeDomains(r.relation));
                }
                for (Iterator i = this.getRules().iterator(); i.hasNext(); ) {
                    BDDInferenceRule r = (BDDInferenceRule) i.next();
                    r.initializeQuantifySet();
                }
            }
        }
    }
    
    private boolean ensureCapacity(BDDDomain d, BigInteger range) {
        int oldSize = d.varNum();
        int newSize = d.ensureCapacity(range);
        if (oldSize != newSize) {
            if (TRACE) out.println("Growing BDD domain "+d+" to "+newSize+" bits.");
            return true;
        }
        return false;
    }
    
    private void redoPairings(BDDDomain d, BigInteger range) {
        for (Iterator i = bddPairings.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            Map map = (Map) e.getKey();
            BDDPairing p = (BDDPairing) e.getValue();
            boolean change = false;
            for (Iterator j = map.entrySet().iterator(); j.hasNext(); ) {
                Map.Entry e2 = (Map.Entry) j.next();
                if (d == e2.getKey()) {
                    ensureCapacity((BDDDomain) e2.getValue(), range);
                    change = true;
                }
                if (d == e2.getValue()) {
                    ensureCapacity((BDDDomain) e2.getKey(), range);
                    change = true;
                }
            }
            if (true) {
                //out.println("Pair "+map+" matches, rebuilding.");
                p.reset();
                for (Iterator j = map.entrySet().iterator(); j.hasNext(); ) {
                    Map.Entry e2 = (Map.Entry) j.next();
                    p.set((BDDDomain) e2.getKey(), (BDDDomain) e2.getValue());
                }
            }
        }
    }
    
    FindBestDomainOrder fbo;
    
    /**
     * Initial size of BDD node table.
     * You can set this with "-Dbddnodes=xxx"
     */
    int BDDNODES = Integer.parseInt(SystemProperties.getProperty("bddnodes", "500000"));
    
    /**
     * Initial size of BDD operation cache.
     * You can set this with "-Dbddcache=xxx"
     */
    int BDDCACHE = Integer.parseInt(SystemProperties.getProperty("bddcache", "0"));
    
    /**
     * BDD minimum free parameter.  This tells the BDD library when to grow the
     * node table.  You can set this with "-Dbddminfree=xxx"
     */
    double BDDMINFREE = Double.parseDouble(SystemProperties.getProperty("bddminfree", ".20"));
    
    /**
     * BDD variable ordering.
     */
    public String VARORDER = SystemProperties.getProperty("bddvarorder", null);

    public String TRIALFILE = SystemProperties.getProperty("trialfile", null);
    
    public String BDDREORDER = SystemProperties.getProperty("bddreorder", null);
    
    /**
     * Constructs a new BDD solver.  Also initializes the BDD library.
     */
    public BDDSolver() {
        super();
        if (BDDCACHE == 0) BDDCACHE = BDDNODES / 4;
		if (VERBOSE >= 2)
        	out.println("Initializing BDD library (" + BDDNODES + " nodes, cache size " + BDDCACHE + ", min free " + BDDMINFREE + "%)");
        bdd = BDDFactory.init(1000, BDDCACHE);
        if (VERBOSE >= 2) out.println("Using BDD library "+bdd.getVersion());
        fielddomainsToBDDdomains = new GenericMultiMap(ListFactory.linkedListFactory);
        bdd.setMinFreeNodes(BDDMINFREE);
        try {
            fbo = new FindBestDomainOrder(this);
        } catch (NoClassDefFoundError x) {
            if (VERBOSE >= 2) out.println("No machine learning library found, learning disabled.");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#initialize()
     */
    public void initialize() {
        if (!isInitialized)
            loadBDDDomainInfo();
        super.initialize();
        if (!isInitialized) {
            setVariableOrdering();
        }
        initialize2(); // Do some more initialization after variable ordering is set.
        isInitialized = true;
        
        if (TRIALFILE == null && inputFilename != null) {
            String sep = SystemProperties.getProperty("file.separator");
            int index1 = inputFilename.lastIndexOf(sep) + 1;
            if (index1 == 0) index1 = inputFilename.lastIndexOf('/') + 1;
            int index2 = inputFilename.lastIndexOf('.');
            if (index1 < index2)
                TRIALFILE = "trials_"+inputFilename.substring(index1, index2)+".xml";
        }
        if (TRIALFILE != null && fbo != null) fbo.loadTrials(TRIALFILE);
    }

    public String getBaseName() {
        if (inputFilename == null) return null;
        String sep = SystemProperties.getProperty("file.separator");
        int index1 = inputFilename.lastIndexOf(sep) + 1;
        if (index1 == 0) index1 = inputFilename.lastIndexOf('/') + 1;
        int index2 = inputFilename.lastIndexOf('.');
        if (index1 < index2)
            return inputFilename.substring(index1, index2);
        else
            return null;
    }
    
    /**
     * Load the BDD domain info, if it exists.
     * The domain info is the list of domains that are allocated.
     */
    void loadBDDDomainInfo() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(basedir + bddDomainInfoFileName));
            for (;;) {
                String s = in.readLine();
                if (s == null) break;
                if (s.length() == 0) continue;
                if (s.startsWith("#")) continue;
                StringTokenizer st = new StringTokenizer(s);
                String domain = st.nextToken();
                Domain fd = (Domain) nameToDomain.get(domain);
                allocateBDDDomain(fd);
            }
        } catch (IOException x) {
        } catch (AccessControlException x) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException _) {
            }
        }
    }

    /**
     * Initialize the values of equivalence relations.
     */
    public void initialize2() {
        for (Iterator i = nameToRelation.values().iterator(); i.hasNext();) {
            BDDRelation r = (BDDRelation) i.next();
            r.initialize2();
        }
    }

    /**
     * Set the BDD variable ordering based on VARORDER.
     */
    public void setVariableOrdering() {
        if (VARORDER != null) {
            VARORDER = fixVarOrder(VARORDER, VERBOSE >= 2);
            if (VERBOSE >= 2) out.print("Setting variable ordering to " + VARORDER + ", ");
            if (bdd instanceof net.sf.javabdd.JFactory && BDDREORDER != null) {
                System.out.println("Target var order:");
                int[] varOrder = bdd.makeVarOrdering(true, VARORDER);
                for (int i = 0; i < varOrder.length; ++i) System.out.print(varOrder[i]+",");
                System.out.println();
                
                net.sf.javabdd.JFactory jbdd = (net.sf.javabdd.JFactory) bdd;
                jbdd.reverseAllDomains();
                jbdd.setVarOrder(VARORDER);
            } else {
                int[] varOrder = bdd.makeVarOrdering(true, VARORDER);
                bdd.setVarOrder(varOrder);
                if (BDDREORDER != null) {
                    bdd.varBlockAll();
                }
            }
            if (VERBOSE >= 2) out.println("done.");
            int[] varOrder = bdd.getVarOrder();
			// for (int i = 0; i < varOrder.length; ++i) System.out.print(varOrder[i]+",");
            // System.out.println();
            // Grow variable table after setting var order.
            try {
                bdd.setNodeTableSize(BDDNODES);
            } catch (OutOfMemoryError x) {
                out.println("Not enough memory, cannot grow node table size.");
                bdd.setCacheSize(bdd.getNodeTableSize());
                bdd.setCacheRatio(0.25);
            }
            //bdd.setMaxIncrease(BDDNODES/2);
            bdd.setIncreaseFactor(2);
        }
        if (BDDREORDER != null) {
            try {
                BDDFactory.ReorderMethod m;
                java.lang.reflect.Field f = BDDFactory.class.getDeclaredField("REORDER_"+BDDREORDER);
                m = (BDDFactory.ReorderMethod) f.get(null);
                out.print("Setting dynamic reordering heuristic to " + BDDREORDER + ", ");
                bdd.autoReorder(m);
                //bdd.enableReorder();
                bdd.reorderVerbose(2);
                out.println("done.");
            } catch (NoSuchFieldException x) {
                err.println("Error: no such reordering method \""+BDDREORDER+"\"");
            } catch (IllegalArgumentException e) {
                err.println("Error: "+e+" on reordering method \""+BDDREORDER+"\"");
            } catch (IllegalAccessException e) {
                err.println("Error: "+e+" on reordering method \""+BDDREORDER+"\"");
            }
        }
    }

    /**
     * Verify that the variable order is sane: Missing BDD domains are added and extra
     * BDD domains are removed.
     */
    String fixVarOrder(String varOrder, boolean trace) {
        // Verify that variable order is sane.
        StringTokenizer st = new StringTokenizer(varOrder, "x_");
        List domains = new LinkedList();
        while (st.hasMoreTokens()) {
            domains.add(st.nextToken());
        }
        for (int i = 0; i < bdd.numberOfDomains(); ++i) {
            String dName = bdd.getDomain(i).getName();
            if (domains.contains(dName)) {
                domains.remove(dName);
                continue;
            }
            if (trace) out.println("Adding missing domain \"" + dName + "\" to bddvarorder.");
            String baseName = dName;
            for (;;) {
                char c = baseName.charAt(baseName.length() - 1);
                if (c < '0' || c > '9') break;
                baseName = baseName.substring(0, baseName.length() - 1);
            }
            int j = varOrder.lastIndexOf(baseName);
            if (j <= 0) {
                varOrder = dName + "_" + varOrder;
            } else {
                char c = varOrder.charAt(j - 1);
                varOrder = varOrder.substring(0, j) + dName + c + varOrder.substring(j);
            }
        }
        for (Iterator i = domains.iterator(); i.hasNext();) {
            String dName = (String) i.next();
            if (trace) out.println("Eliminating unused domain \"" + dName + "\" from bddvarorder.");
            int index = varOrder.indexOf(dName);
            if (index == 0) {
                if (varOrder.length() <= dName.length() + 1) {
                    varOrder = "";
                } else {
                    varOrder = varOrder.substring(dName.length() + 1);
                }
            } else {
                char before = varOrder.charAt(index - 1);
                int k = index + dName.length();
                if (before == '_' && k < varOrder.length() && varOrder.charAt(k) == 'x') {
                    // Case: H1_V1xV2 delete "V1x" substring
                    varOrder = varOrder.substring(0, index) + varOrder.substring(k + 1);
                } else {
                    varOrder = varOrder.substring(0, index - 1) + varOrder.substring(k);
                }
            }
        }
        return varOrder;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#solve()
     */
    public void solve() {
        if (USE_IR) {
            BDDInterpreter interpreter = new BDDInterpreter(ir);
            interpreter.interpret();
            //stratify.solve();
        } else {
            IterationList list = ifg.getIterationList();
            //out.println(list);
            BDDInterpreter interpreter = new BDDInterpreter(null);
            long time = System.currentTimeMillis();
            interpreter.interpret(list);
  /*          if (LEARN_BEST_ORDER) {
                time = System.currentTimeMillis() - time;
                out.println("SOLVE_TIME: " + time);
                reportStats();
                Learner learner = new IndividualRuleLearner(this, stratify);
                learner.learn();
            }
    */
        }
    }
    
    public List rulesToLearn(){
        List rulesToLearn = new LinkedList();
        for(Iterator it = rules.iterator(); it.hasNext(); ){
            BDDInferenceRule rule = (BDDInferenceRule) it.next();
            if(LEARN_ALL_RULES || rule.learn_best_order) rulesToLearn.add(rule);     
        }  
        return rulesToLearn;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#finish()
     */
    public void finish() {
        try {
            saveBDDDomainInfo();
        } catch (IOException x) {
        }
        //fbo.dump();
        //fbo.printTrialsDistro();
        //fbo.printBestTrials();
        //fbo.printBestBDDOrders();
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#cleanup()
     */
    public void cleanup() {
        BDDFactory.CacheStats s = bdd.getCacheStats();
        if (s.uniqueAccess > 0) {
            out.println(s);
        }
        bdd.done();
    }
    
    /**
     * Save the BDD domain info.
     * 
     * @throws IOException
     */
    void saveBDDDomainInfo() throws IOException {
        BufferedWriter dos = null;
        try {
            dos = new BufferedWriter(new FileWriter(basedir + "r" + bddDomainInfoFileName));
            for (int i = 0; i < bdd.numberOfDomains(); ++i) {
                BDDDomain d = bdd.getDomain(i);
                for (Iterator j = fielddomainsToBDDdomains.keySet().iterator(); j.hasNext();) {
                    Domain fd = (Domain) j.next();
                    if (fielddomainsToBDDdomains.getValues(fd).contains(d)) {
                        dos.write(fd.toString() + "\n");
                        break;
                    }
                }
            }
        } finally {
            if (dos != null) dos.close();
        }
    }

    /**
     * Make a BDD domain with the given name and number of bits.
     * 
     * @param name  name of BDD domain
     * @param bits  number of bits desired
     * @return  new BDD domain
     */
    BDDDomain makeDomain(String name, int bits) {
        Assert._assert(bits < 64);
        BDDDomain d = bdd.extDomain(new long[]{1L << bits})[0];
        d.setName(name);
        return d;
    }

    /**
     * Allocate a new BDD domain that matches the given domain.
     * 
     * @param dom  domain to match
     * @return  new BDD domain
     */
    public BDDDomain allocateBDDDomain(Domain dom) {
        int version = getBDDDomains(dom).size();
        int bits = dom.size.subtract(BigInteger.ONE).bitLength();
        BDDDomain d = makeDomain(dom.name + version, bits);
        if (TRACE) out.println("Allocated BDD domain " + d + ", size " + dom.size + ", " + bits + " bits.");
        fielddomainsToBDDdomains.add(dom, d);
        return d;
    }

    /**
     * Get the set of BDD domains allocated for a given domain.
     * 
     * @param dom  domain
     * @return  set of BDD domains
     */
    public Collection getBDDDomains(Domain dom) {
        return fielddomainsToBDDdomains.getValues(dom);
    }

    /**
     * Get the k-th BDD domain allocated for a given domain.
     * 
     * @param dom  domain
     * @param k  index
     * @return  k-th BDD domain allocated for the given domain
     */
    public BDDDomain getBDDDomain(Domain dom, int k) {
        List list = (List) fielddomainsToBDDdomains.getValues(dom);
        return (BDDDomain) list.get(k);
    }

    /**
     * Get the BDD domain with the given name.
     * 
     * @param s  name of BDD domain
     * @return  BDD domain with the given name
     */
    public BDDDomain getBDDDomain(String s) {
        for (int i = 0; i < bdd.numberOfDomains(); ++i) {
            BDDDomain d = bdd.getDomain(i);
            if (s.equals(d.getName())) return d;
        }
        return null;
    }

    /**
     * Return the map of field domains to sets of allocated BDD domains. 
     * 
     * @return  map between field domains and sets of allocated BDD domains
     */
    public MultiMap getBDDDomains() {
        return fielddomainsToBDDdomains;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#createInferenceRule(java.util.List,
     *      net.sf.bddbddb.RuleTerm)
     */
    public InferenceRule createInferenceRule(List top, RuleTerm bottom) {
        return new BDDInferenceRule(this, top, bottom);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#createRelation(java.lang.String,
     *      java.util.List, java.util.List, java.util.List)
     */
    public Relation createRelation(String name, List attributes) {
        while (nameToRelation.containsKey(name)) {
            name = mungeName(name);
        }
        return new BDDRelation(this, name, attributes);
    }

    /**
     * Munge the given name to be unique.
     * 
     * @param name  name of relation
     * @return  new unique name
     */
    String mungeName(String name) {
        return name + '#' + relations.size();
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createEquivalenceRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createEquivalenceRelation(Domain fd1, Domain fd2) {
        String name = fd1 + "_eq_" + fd2;
        Attribute a1 = new Attribute(fd1 + "_1", fd1, "");
        Attribute a2 = new Attribute(fd2 + "_2", fd2, "");
        BDDRelation r = new BDDRelation(this, name, new Pair(a1, a2));
        r.special_type = BDDRelation.EQ;
        return r;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createLessThanRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createLessThanRelation(Domain fd1, Domain fd2) {
        String name = fd1 + "_lt_" + fd2;
        Attribute a1 = new Attribute(fd1 + "_1", fd1, "");
        Attribute a2 = new Attribute(fd2 + "_2", fd2, "");
        BDDRelation r = new BDDRelation(this, name, new Pair(a1, a2));
        r.special_type = BDDRelation.LT;
        return r;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createGreaterThanRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createGreaterThanRelation(Domain fd1, Domain fd2) {
        String name = fd1 + "_gt_" + fd2;
        Attribute a1 = new Attribute(fd1 + "_1", fd1, "");
        Attribute a2 = new Attribute(fd2 + "_2", fd2, "");
        BDDRelation r = new BDDRelation(this, name, new Pair(a1, a2));
        r.special_type = BDDRelation.GT;
        return r;
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Solver#createMapRelation(net.sf.bddbddb.Domain, net.sf.bddbddb.Domain)
     */
    Relation createMapRelation(Domain fd1, Domain fd2) {
        String name = "map_" + fd1 + "_" + fd2;
        Attribute a1 = new Attribute(fd1.name, fd1, "");
        Attribute a2 = new Attribute(fd2.name, fd2, "");
        BDDRelation r = new BDDRelation(this, name, new Pair(a1, a2));
        r.special_type = BDDRelation.MAP;
        return r;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#saveResults()
     */
    void saveResults() throws IOException {
        super.saveResults();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.Solver#reportStats()
     */
    public void reportStats() {
        boolean find_best_order = !SystemProperties.getProperty("findbestorder", "no").equals("no");
        boolean print_best_order = !SystemProperties.getProperty("printbestorder", "no").equals("no");
        if(find_best_order || print_best_order){
            fbo.printBestBDDOrders();
            return;
        }
        int final_node_size = bdd.getNodeNum();
        int final_table_size = bdd.getNodeTableSize();
        out.println("MAX_NODES=" + final_table_size);
        out.println("FINAL_NODES=" + final_node_size);
        super.reportStats();
    }

    /**
     * Get the BDD factory used by this solver.
     * 
     * @return  BDD factory
     */
    public BDDFactory getBDDFactory() {
        return this.bdd;
    }

}
