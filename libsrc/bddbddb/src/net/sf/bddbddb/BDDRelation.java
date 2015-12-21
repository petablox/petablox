// BDDRelation.java, created Mar 16, 2004 12:40:26 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.BDD.BDDIterator;

/**
 * An implementation of Relation that uses BDDs.
 * 
 * @author jwhaley
 * @version $Id: BDDRelation.java 556 2005-05-21 10:15:40Z joewhaley $
 */
public class BDDRelation extends Relation {
    
    public static String BDD_INPUT_SUFFIX = SystemProperties.getProperty("bddinputsuffix", ".bdd");
    public static String BDD_OUTPUT_SUFFIX = SystemProperties.getProperty("bddoutputsuffix", ".bdd");
    public static String TUPLES_INPUT_SUFFIX = SystemProperties.getProperty("tuplesinputsuffix", ".tuples");
    public static String TUPLES_OUTPUT_SUFFIX = SystemProperties.getProperty("tuplesoutputsuffix", ".tuples");
    
    /**
     * Link to solver.
     */
    protected BDDSolver solver;
    
    /**
     * Value of relation.
     */
    protected BDD relation;
    
    /**
     * List of BDDDomains that are used in this relation.
     * The indices coincide with those of the attributes.
     */
    protected List/*<BDDDomain>*/ domains;
    
    /**
     * Cache of the BDD set.
     */
    private BDD domainSet;

    static final byte EQ = 1;
    static final byte LT = 2;
    static final byte GT = 3;
    static final byte MAP = 4;
    protected byte special_type;
    
    /**
     * Construct a new BDDRelation.
     * This is only to be called internally.
     * 
     * @param solver  solver
     * @param name  name of relation
     * @param attributes  list of attributes for relation
     */
    BDDRelation(BDDSolver solver, String name, List attributes) {
        super(solver, name, attributes);
        this.solver = solver;
        if (solver.TRACE) solver.out.println("Created BDDRelation " + this);
    }

    /*
     * (non-Javadoc)
     * Called before variable order is set.
     * 
     * @see net.sf.bddbddb.Relation#initialize()
     */
    public void initialize() {
        if (!isInitialized) {
            if (negated != null && name.startsWith("!")) {
                if (solver.TRACE) solver.out.println("Skipping initialization of negated BDDRelation " + name);
                if (solver.TRACE) solver.out.println(" because normal " + negated.name + " is/will be initialized.");
                return;
            }
            this.relation = solver.bdd.zero();
            this.domains = new LinkedList();
            if (solver.TRACE) solver.out.println("Initializing BDDRelation " + name + " with attributes " + attributes);
            this.domainSet = solver.bdd.one();
            for (Iterator i = attributes.iterator(); i.hasNext();) {
                Attribute a = (Attribute) i.next();
                Domain fd = a.attributeDomain;
                Collection doms = solver.getBDDDomains(fd);
                BDDDomain d = null;
                String option = a.attributeOptions;
                if (option != null && option.length() > 0) {
                    // use the given domain.
                    if (!option.startsWith(fd.name)) throw new IllegalArgumentException("Attribute " + a + " has domain " + fd + ", but tried to assign "
                        + option);
                    //int index =
                    // Integer.parseInt(option.substring(fd.name.length()));
                    for (Iterator j = doms.iterator(); j.hasNext();) {
                        BDDDomain dom = (BDDDomain) j.next();
                        if (dom.getName().equals(option)) {
                            if (domains.contains(dom)) {
                                solver.out.println("Cannot assign " + dom + " to attribute " + a + ": " + dom + " is already assigned");
                                option = "";
                                break;
                            } else {
                                d = dom;
                                break;
                            }
                        }
                    }
                    if (option.length() > 0) {
                        while (d == null) {
                            BDDDomain dom = solver.allocateBDDDomain(fd);
                            if (dom.getName().equals(option)) {
                                d = dom;
                                break;
                            }
                        }
                    }
                }
                if (d == null) {
                    // find an applicable domain.
                    for (Iterator j = doms.iterator(); j.hasNext();) {
                        BDDDomain dom = (BDDDomain) j.next();
                        if (!domains.contains(dom)) {
                            d = dom;
                            break;
                        }
                    }
                    if (d == null) {
                        d = solver.allocateBDDDomain(fd);
                    }
                }
                if (solver.TRACE) solver.out.println("Attribute " + a + " (" + a.attributeDomain + ") assigned to BDDDomain " + d);
                domains.add(d);
                domainSet.andWith(d.set());
            }
            isInitialized = true;
        }
        if (negated != null && !negated.isInitialized) {
            BDDRelation bddn = (BDDRelation) negated;
            bddn.relation = solver.bdd.one();
            bddn.domains = this.domains;
            bddn.domainSet = this.domainSet.id();
            bddn.isInitialized = true;
        }
    }

    /**
     * (Re-)calculate the domain set. 
     * 
     * @return  the domain set
     */
    BDD calculateDomainSet() {
        if (domainSet != null) {
            domainSet.free();
        }
        this.domainSet = solver.bdd.one();
        for (Iterator i = domains.iterator(); i.hasNext();) {
            BDDDomain d = (BDDDomain) i.next();
            domainSet.andWith(d.set());
        }
        return domainSet;
    }

    public BDD getDomainSet() {
        return domainSet;
    }
    
    /**
     * Do more initialization.  This initializes the values of equivalence relations.
     * Called after variable order is set, so the computation is faster.
     */
    public void initialize2() {
        Assert._assert(isInitialized);
        if (special_type != 0) {
            BDDDomain d1 = (BDDDomain) domains.get(0);
            BDDDomain d2 = (BDDDomain) domains.get(1);
            if (solver.TRACE)
                solver.out.println("Initializing value of special relation "+this+" "+d1+","+d2);
            //Assert._assert(relation.isZero());
            relation.free();
            BDD b;
            switch (special_type) {
                case EQ:
                    b = d1.buildEquals(d2);
                    break;
                case LT:
                    b = buildLessThan(d1, d2);
                    break;
                case GT:
                    b = buildLessThan(d2, d1);
                    break;
                case MAP:
                    Domain a1 = ((Attribute)attributes.get(0)).attributeDomain;
                    Domain a2 = ((Attribute)attributes.get(1)).attributeDomain;
                    b = buildMap(a1, d1, a2, d2);
                    break;
                default:
                    throw new InternalError();
            }
            
            relation = b;
            updateNegated();
        }
    }

    /**
     * Build a BDD representing d1 < d2.
     * 
     * @param d1 first domain
     * @param d2 second domain
     * @return BDD that is true iff d1 < d2.
     */
    private BDD buildLessThan(BDDDomain d1, BDDDomain d2) {
        BDD leftwardBitsEqual = solver.bdd.one();
        BDD result = solver.bdd.zero();
        for (int i=d1.varNum()-1; i>=0; i--) {
            BDD v1 = d1.getFactory().ithVar(d1.vars()[i]);
            BDD v2 = d2.getFactory().ithVar(d2.vars()[i]);
            result.orWith(v2.and(v1.not()).and(leftwardBitsEqual));
            leftwardBitsEqual.andWith(v1.biimp(v2));
        }
        return result;
    }

    /**
     * Helper function for building a map.
     */
    private BDD buildMap(Domain a1, BDDDomain d1, Domain a2, BDDDomain d2) {
        if (solver.NOISY) solver.out.print("Building "+this+": ");
        BigInteger index, size;
        index = (a2.map != null) ? BigInteger.valueOf(a2.map.size()) : a2.size;
        size = (a1.map != null) ? BigInteger.valueOf(a1.map.size()) : a1.size;
        BigInteger total = index.add(size);
        if (total.compareTo(d2.size()) > 0) {
            throw new IllegalArgumentException("Domain "+a2+" (current size="+index+", max size="+d2.size()+") is not large enough to contain mapping from "+a1+" (size "+size+")");
        }
        int bits = Math.min(d1.varNum(), d2.varNum());
        BDD b = d1.buildAdd(d2, bits, index.longValue());
        b.andWith(d1.varRange(BigInteger.ZERO, size.subtract(BigInteger.ONE)));
        if (a2.map != null) {
            if (a1.map != null) {
                a2.map.addAll(a1.map);
            } else {
                int v = size.intValue();
                for (int i = 0; i < v; ++i) {
                    a2.map.get(a1+"_"+i);
                }
            }
            Assert._assert(a2.map.size() == total.intValue(), a1.map.size()+" != "+total);
        } else {
            a2.size = a2.size.add(size);
        }
        if (solver.NOISY) solver.out.println(a1+" ("+d1+") 0.."+(size.subtract(BigInteger.ONE))+" maps to "+a2+" ("+d2+") "+index+".."+(total.subtract(BigInteger.ONE)));
        return b;
    }
    
    /**
     * Updated the negated form of this relation.
     */
    void updateNegated() {
        if (negated != null) {
            BDDRelation bddn = (BDDRelation) negated;
            bddn.relation.free();
            bddn.relation = relation.not();
        }
    }

    /**
     * Verify that the domains for this BDD are correct.
     * 
     * @return  whether the domains are correct
     */
    public boolean verify() {
        return verify(relation);
    }
    
    /**
     * Verify that the domains for the given BDD match this relation.
     * 
     * @param r  the given BDD
     * @return  whether the domains match
     */
    public boolean verify(BDD r) {
        if(r == null) return true; /* trivially true? */
        BDD s = r.support();
        calculateDomainSet();
        BDD t = domainSet.and(s);
        s.free();
        
        boolean result = t.equals(domainSet);
        if (!result) {
            solver.out.println("Warning, domains for " + this + " don't match BDD: " + activeDomains(r) + " vs " + domains);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#load()
     */
    public void load() throws IOException {
        if (solver.NOISY) solver.out.print("Loading BDD from file: " + name + ".bdd ");
        load(solver.basedir + name + ".bdd");
        if (solver.NOISY) solver.out.println(relation.nodeCount() + " nodes, " + dsize() + " elements.");
        if (solver.TRACE) solver.out.println("Domains of loaded relation:" + activeDomains(relation));
    }

    public static boolean SMART_LOAD = true;
    
    /**
     * Load this relation from the given file.
     * 
     * @param filename  the file to load
     * @throws IOException
     */
    public void load(String filename) throws IOException {
        Assert._assert(isInitialized);
        BDD r2;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            String s = in.readLine();
            if (s != null && s.startsWith("# ")) {
                // Parse BDD information.
                List fileDomains = checkInfoLine(filename, s, false, true);
                in.mark(4096);
                List fileDomainList = new ArrayList(fileDomains.size());
                BDD mask = null;
				int[] map = null; 
                for (Iterator i = fileDomains.iterator(); i.hasNext(); ) {
                    BDDDomain d = (BDDDomain) i.next();
                    String s2 = in.readLine();
                    if (!s2.startsWith("# ")) {
                        solver.err.println("BDD file \""+filename+"\" has no variable assignment line for "+d);
                        in.reset();
                        break;
                    }
                    StringTokenizer st = new StringTokenizer(s2.substring(2));
                    if (!st.hasMoreTokens()) {
                        String msg = "BDD file \""+filename+"\" has an invalid BDD information line";
                        throw new IOException(msg);
                    }
                    int[] vars = d.vars();
                    int j;
                    for (j = 0; j < vars.length; ++j) {
                        if (!st.hasMoreTokens()) {
                            if (!SMART_LOAD) {
                                String msg = "in file \""+filename+"\", not enough bits for domain "+d;
                                throw new IOException(msg);
                            }
                            if (mask == null) mask = solver.bdd.nithVar(vars[j]);
                            else mask.andWith(solver.bdd.nithVar(vars[j]));
                            continue;
                        }
                        int k = Integer.parseInt(st.nextToken());
                        if (vars[j] != k) {
                            if (!SMART_LOAD) {
                                String msg = "in file \""+filename+"\", bit "+j+" for domain "+d+" ("+k+") does not match expected ("+vars[j]+")";
                                throw new IOException(msg);
                            }
                            if (k >= solver.bdd.varNum())
                                solver.bdd.setVarNum(k+1);
                            if (solver.TRACE) solver.out.println("Rename "+k+" to "+vars[j]);
							if (map == null || map.length < solver.bdd.varNum()) {
								int[] t = new int[solver.bdd.varNum()];
								for (int x = 0; x < t.length; x++)
									t[x] = x;
								if (map != null)
									System.arraycopy(map, 0, t, 0, map.length);
								map = t;
							}
							map[k] = vars[j];
                        }
                    }
                    if (st.hasMoreTokens()) {
						String msg = "in file \""+filename+"\", too many bits for domain "+d;
						throw new IOException(msg);
                    }
                }
				// MAYUR: replaced argument 'in' below by 'filename' and also created map
                r2 = solver.bdd.load(in, map);
                if (mask != null) {
                    r2.andWith(mask);
                }
            } else {
                solver.err.println("BDD file \""+filename+"\" has no header line.");
                r2 = solver.bdd.load(filename);
            }
        } finally {
            if (in != null) try { in.close(); } catch (IOException _) { }
        }
        if (r2 != null) {
            if (r2.isZero()) {
                if (solver.VERBOSE >= 2) solver.out.println("Warning: " + filename + " is zero.");
            } else if (r2.isOne()) {
                if (solver.VERBOSE >= 2) solver.out.println("Warning: " + filename + " is one.");
            } else {
                if (!verify(r2)) {
                    throw new IOException("Expected domains for loaded BDD " + filename + " to be " + domains + ", but found " + activeDomains(r2)
                        + " instead");
                }
            }
            relation.free();
            relation = r2;
        }
        updateNegated();
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#loadTuples()
     */
    public void loadTuples() throws IOException {
        loadTuples(solver.basedir + name + ".tuples");
        if (solver.NOISY) solver.out.println("Loaded tuples from file: " + name + ".tuples");
        if (solver.NOISY) solver.out.println("Domains of loaded relation:" + activeDomains(relation));
    }

    /**
     * Makes a domain info line for this relation.
     * 
     * @return  domain info line
     */
    String makeInfoLine() {
        StringBuffer sb = new StringBuffer();
        sb.append("#");
        for (Iterator i = domains.iterator(); i.hasNext();) {
            BDDDomain d = (BDDDomain) i.next();
            sb.append(' ');
            sb.append(d.toString());
            sb.append(':');
            sb.append(d.varNum());
        }
        return sb.toString();
    }
    
    /**
     * Makes a BDD variable info line for this relation and domain.
     * 
     * @param d  domain
     * @return  BDD variable info line
     */
    String makeBDDVarInfoLine(BDDDomain d) {
        StringBuffer sb = new StringBuffer();
        sb.append("#");
        int[] vars = d.vars();
        for (int i = 0; i < vars.length; ++i) {
            sb.append(' ');
            sb.append(vars[i]);
        }
        return sb.toString();
    }
    
    /**
     * Checks that the given domain info line matches this relation.
     * 
     * @param filename  filename to use in error message
     * @param s  domain info line
     * @param order  true if we want to check the order, false otherwise
     * @param ex  true if we want to throw an exception, false if we just want to print to stderr
     * @throws IOException
     */
    List checkInfoLine(String filename, String s, boolean order, boolean ex) throws IOException {
        StringTokenizer st = new StringTokenizer(s.substring(2));
        List domainList = new ArrayList(domains.size());
        Iterator i = domains.iterator();
        while (st.hasMoreTokens()) {
            String msg = null;
            String dname = st.nextToken(": ");
            int dbits = Integer.parseInt(st.nextToken());
            if (domainList.size() >= domains.size()) {
                msg = "extra domain "+dname;
            } else {
                BDDDomain d = solver.getBDDDomain(dname);
                if (d == null) {
                    msg = "unknown domain "+dname;
                } else if (order) {
                    BDDDomain d2 = (BDDDomain) domains.get(domainList.size());
                    if (d != d2) {
                        msg = "domain "+dname+" does not match expected domain "+d2;
                    }
                } else if (!domains.contains(d)) {
                    msg = "domain "+dname+" is not in domain set "+domains;
                }
                if (msg == null && !SMART_LOAD && d.varNum() != dbits) {
                    msg = "number of bits for domain "+dname+" ("+dbits +") does not match expected ("+d.varNum()+")";
                }
                if (d != null) domainList.add(d);
            }
            if (msg != null) {
                if (ex) throw new IOException("in file \""+filename+"\", "+msg);
                else solver.err.println("WARNING: in file \""+filename+"\", "+msg);
            }
        }
        if (domainList.size() != domains.size()) {
            Collection c = new ArrayList(domains);
            c.removeAll(domainList);
            StringBuffer sb = new StringBuffer();
            sb.append("file \""+filename+"\" is missing domains:");
            for (Iterator j = c.iterator(); j.hasNext(); ) {
                sb.append(" "+j.next());
            }
            String msg = sb.toString();
            if (ex) throw new IOException(msg);
            else solver.err.println("WARNING: "+msg); 
        }
        return domainList;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#loadTuples(java.lang.String)
     */
    public void loadTuples(String filename) throws IOException {
        Assert._assert(isInitialized);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            // Load the header line.
            String s = in.readLine();
            if (s == null) return;
            if (!s.startsWith("# ")) {
                solver.err.println("Tuple file \""+filename+"\" is missing header line, using default.");
                BDD b = parseTuple(s);
                relation.orWith(b);
            } else {
                checkInfoLine(filename, s, true, true);
            }
            for (;;) {
                s = in.readLine();
                if (s == null) break;
                if (s.length() == 0) continue;
                if (s.startsWith("#")) continue;
                BDD b = parseTuple(s);
                relation.orWith(b);
            }
        } finally {
            if (in != null) in.close();
        }
        updateNegated();
    }

    /**
     * Parse the given tuple string and return a BDD corresponding to it.
     * 
     * @param s  tuple string
     * @return  BDD form of tuple
     */
    BDD parseTuple(String s) {
        StringTokenizer st = new StringTokenizer(s);
        BDD b = solver.bdd.one();
        for (int i = 0; i < domains.size(); ++i) {
            BDDDomain d = (BDDDomain) domains.get(i);
            String v = st.nextToken();
            if (v.equals("*")) {
                b.andWith(d.domain());
            } else {
                int x = v.indexOf('-');
                if (x < 0) {
                    BigInteger l = new BigInteger(v);
                    Domain dd = getAttribute(i).getDomain();
                    solver.ensureCapacity(dd, l);
                    b.andWith(d.ithVar(l));
                    if (solver.TRACE_FULL) solver.out.print(attributes.get(i) + ": " + l + ", ");
                } else {
                    BigInteger l = new BigInteger(v.substring(0, x));
                    BigInteger m = new BigInteger(v.substring(x + 1));
                    Domain dd = getAttribute(i).getDomain();
                    solver.ensureCapacity(dd, m);
                    b.andWith(d.varRange(l, m));
                    if (solver.TRACE_FULL) solver.out.print(attributes.get(i) + ": " + l + "-" + m + ", ");
                }
            }
        }
        if (solver.TRACE_FULL) solver.out.println();
        return b;
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#save()
     */
    public void save() throws IOException {
        save(solver.basedir + name + ".bdd");
    }

    /**
     * Save the value of this relation to the given file.
     * 
     * @param filename  name of file to save
     * @throws IOException
     */
    public void save(String filename) throws IOException {
        Assert._assert(isInitialized);
        if (solver.VERBOSE >= 1)
			solver.out.println("Relation " + this + ": " + relation.nodeCount() + " nodes, " + dsize() + " elements ("+activeDomains(relation)+")");
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename));
            out.write(makeInfoLine());
            out.write('\n');
            for (Iterator i = domains.iterator(); i.hasNext(); ) {
                BDDDomain d = (BDDDomain) i.next();
                out.write(makeBDDVarInfoLine(d));
                out.write('\n');
            }
            solver.bdd.save(out, relation);
        } finally {
            if (out != null) try { out.close(); } catch (IOException x) { }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#saveTuples()
     */
    public void saveTuples() throws IOException {
        saveTuples(solver.basedir + name + ".tuples");
    }

    /**
     * Save the value of this relation in tuple form to the given file.
     * 
     * @param filename  name of file to save
     * @throws IOException
     */
    public void saveTuples(String filename) throws IOException {
        solver.out.println("Relation " + this + ": " + relation.nodeCount() + " nodes, " + dsize() + " elements ("+activeDomains(relation)+")");
        saveTuples(solver.basedir + name + ".tuples", relation);
    }

    /**
     * Save the given relation in tuple form to the given file.
     * 
     * @param fileName  name of file to save
     * @param relation  value to save
     * @throws IOException
     */
    public void saveTuples(String fileName, BDD relation) throws IOException {
        Assert._assert(isInitialized);
        BufferedWriter dos = null;
        try {
            dos = new BufferedWriter(new FileWriter(fileName));
            if (relation.isZero()) {
                return;
            }
            BDD allDomains = solver.bdd.one();
            dos.write("#");
            solver.out.print(fileName + " domains {");
            int[] domIndices = new int[domains.size()];
            int k = -1;
            for (Iterator i = domains.iterator(); i.hasNext();) {
                BDDDomain d = (BDDDomain) i.next();
                solver.out.print(" " + d.toString());
                dos.write(" " + d.toString() + ":" + d.varNum());
                domIndices[++k] = d.getIndex();
            }
            dos.write("\n");
            solver.out.println(" } = " + relation.nodeCount() + " nodes, " + dsize() + " elements");
            if (relation.isOne()) {
                for (k = 0; k < domIndices.length; ++k) {
                    dos.write("* ");
                }
                dos.write("\n");
                return;
            }
            
            calculateDomainSet();
            int lines = 0;
            BDDIterator i = relation.iterator(domainSet);
            while (i.hasNext()) {
                BigInteger[] v = i.nextTuple();
                for (k = 0; k < domIndices.length; ++k) {
                    BigInteger val = v[domIndices[k]];
                    if (val.equals(BigInteger.ZERO)) {
                        // Check if this is the universal set.
                        BDDDomain d = solver.bdd.getDomain(domIndices[k]);
                        if (i.isDontCare(d)) {
                            i.skipDontCare(d);
                            dos.write("* ");
                            continue;
                        }
                    }
                    dos.write(val + " ");
                }
                dos.write("\n");
                ++lines;
            }
            solver.out.println("Done writing " + lines + " lines.");
        } finally {
            if (dos != null) dos.close();
        }
    }

    /**
     * Return a string representation of the active domains of the given relation.
     * 
     * @param r  relation to check
     * @return  string representation of the active domains
     */
    public static String activeDomains(BDD r) {
        BDDFactory bdd = r.getFactory();
        BDD s = r.support();
        int[] a = s.scanSetDomains();
        s.free();
        if (a == null) return "(none)";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; ++i) {
            sb.append(bdd.getDomain(a[i]));
            if (i < a.length - 1) sb.append(',');
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#dsize()
     */
    public double dsize() {
        calculateDomainSet();
        return relation.satCount(domainSet);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator()
     */
    public TupleIterator iterator() {
        calculateDomainSet();
        final BDDIterator i = relation.iterator(domainSet);
        return new MyTupleIterator(i, domains);
    }
    
    /**
     * Implementation of TupleIterator for BDDs.
     */
    static class MyTupleIterator extends TupleIterator {
        protected BDDIterator i;
        protected List domains;

        protected MyTupleIterator(BDDIterator i, List domains) {
            this.i = i;
            this.domains = domains;
        }

        public BigInteger[] nextTuple() {
            BigInteger[] q = i.nextTuple();
            BigInteger[] r = new BigInteger[domains.size()];
            int j = 0;
            for (Iterator k = domains.iterator(); k.hasNext(); ++j) {
                BDDDomain d = (BDDDomain) k.next();
                r[j] = q[d.getIndex()];
            }
            return r;
        }

        public boolean hasNext() {
            return i.hasNext();
        }

        public void remove() {
            i.remove();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(int)
     */
    public TupleIterator iterator(int k) {
        final BDDDomain d = (BDDDomain) domains.get(k);
        BDD s = d.set();
        final BDDIterator i = relation.iterator(s);
        return new TupleIterator() {
            public BigInteger[] nextTuple() {
                BigInteger v = i.nextValue(d);
                return new BigInteger[]{v};
            }

            public boolean hasNext() {
                return i.hasNext();
            }

            public void remove() {
                i.remove();
            }
        };
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(int, java.math.BigInteger)
     */
    public TupleIterator iterator(int k, BigInteger j) {
        final BDDDomain d = (BDDDomain) domains.get(k);
        BDD val = d.ithVar(j);
        val.andWith(relation.id());
        calculateDomainSet();
        final BDDIterator i = val.iterator(domainSet);
        return new MyTupleIterator(i, domains);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(java.math.BigInteger[])
     */
    public TupleIterator iterator(BigInteger[] j) {
        BDD val = relation.id();
        for (int i = 0; i < j.length; ++i) {
            if (j[i].signum() < 0) continue;
            final BDDDomain d = (BDDDomain) domains.get(i);
            val.andWith(d.ithVar(j[i]));
        }
        calculateDomainSet();
        final BDDIterator i = val.iterator(domainSet);
        return new MyTupleIterator(i, domains);
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#contains(int, java.math.BigInteger)
     */
    public boolean contains(int k, BigInteger j) {
        final BDDDomain d = (BDDDomain) domains.get(k);
        BDD b = relation.id();
        b.restrictWith(d.ithVar(j));
        boolean result = !b.isZero();
        b.free();
        return result;
    }

    boolean add(BDD val) {
        BDD old = relation.id();
        relation.orWith(val);
        boolean result = !old.equals(relation);
        old.free();
        return result;
    }
    
    /**
     * Add a single to this relation.
     * 
     * @param a  first attribute
     * @return  whether this relation changed
     */
    public boolean add(int a) {
        BDDDomain d0 = (BDDDomain) domains.get(0);
        Domain dd0 = getAttribute(0).getDomain();
        solver.ensureCapacity(dd0, a);
        BDD val = d0.ithVar(a);
        return add(val);
    }
    
    /**
     * Add a double to this relation.
     * 
     * @param a  first attribute
     * @param b  second attribute
     * @return  whether this relation changed
     */
    public boolean add(int a, int b) {
        BDDDomain d0 = (BDDDomain) domains.get(0);
        Domain dd0 = getAttribute(0).getDomain();
        solver.ensureCapacity(dd0, a);
        BDD val = d0.ithVar(a);
        BDDDomain d1 = (BDDDomain) domains.get(1);
        Domain dd1 = getAttribute(1).getDomain();
        solver.ensureCapacity(dd1, b);
        val.andWith(d1.ithVar(b));
        return add(val);
    }
    
    /**
     * Add a triple to this relation.
     * 
     * @param a  first attribute
     * @param b  second attribute
     * @param c  third attribute
     * @return  whether this relation changed
     */
    public boolean add(int a, int b, int c) {
        BDDDomain d0 = (BDDDomain) domains.get(0);
        Domain dd0 = getAttribute(0).getDomain();
        solver.ensureCapacity(dd0, a);
        BDD val = d0.ithVar(a);
        BDDDomain d1 = (BDDDomain) domains.get(1);
        Domain dd1 = getAttribute(1).getDomain();
        solver.ensureCapacity(dd1, b);
        val.andWith(d1.ithVar(b));
        BDDDomain d2 = (BDDDomain) domains.get(2);
        Domain dd2 = getAttribute(2).getDomain();
        solver.ensureCapacity(dd2, c);
        val.andWith(d2.ithVar(c));
        return add(val);
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#add(java.math.BigInteger[])
     */
    public boolean add(BigInteger[] tuple) {
        BDD val = solver.bdd.one();
        for (int i = 0; i < tuple.length; ++i) {
            final BDDDomain d = (BDDDomain) domains.get(i);
            Domain dd = getAttribute(i).getDomain();
            solver.ensureCapacity(dd, tuple[i]);
            val.andWith(d.ithVar(tuple[i]));
        }
        return add(val);
    }
    
    /**
     * Return the value of this relation in BDD form.
     * 
     * @return BDD form of this relation
     */
    public BDD getBDD() {
        return relation;
    }

    /**
     * Set the value of this relation from the given BDD.
     * 
     * @param b  BDD value to set from
     */
    public void setBDD(BDD b) {
        if (relation != null) relation.free();
        relation = b;
    }

    /**
     * Get the BDDDomain with the given index.
     * 
     * @param i  index
     * @return  BDDDomain at that index
     */
    public BDDDomain getBDDDomain(int i) {
        return (BDDDomain) domains.get(i);
    }

    /**
     * Get the BDDDomain that matches the given attribute, or
     * null if the attribute hasn't been assigned one yet.
     * 
     * @param a  attribute
     * @return  BDDDomain that matches that attribute
     */
    public BDDDomain getBDDDomain(Attribute a) {
        int i = attributes.indexOf(a);
        if (i == -1 || domains == null) return null;
        return (BDDDomain) domains.get(i);
    }
    
    /**
     * Get the attribute that is assigned to the given BDDDomain.
     * 
     * @param d  BDD domain
     * @return attribute
     */
    public Attribute getAttribute(BDDDomain d) {
       int i = domains.indexOf(d);
       if (i == -1) return null;
       return (Attribute) attributes.get(i);
    }

    /**
     * Returns the list of BDD domains this relation is using.
     * 
     * @return  the list of BDDDomains this relation is using
     */
    public List getBDDDomains() {
        return domains;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#copy()
     */
    public Relation copy() {
        List a = new LinkedList(attributes);
        Relation that = solver.createRelation(name + '\'', a);
        return that;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#free()
     */
    public void free() {
        if (relation != null) {
            relation.free();
            relation = null;
        }
        /*
        if (domainSet != null) {
            domainSet.free();
            domainSet = null;
        }*/
    }

    /**
     * Do any onUpdate actions.
     * Called just after an update occurs.
     * 
     * @param oldValue  old value of relation
     */
    void doUpdate(BDD oldValue) {
        if (onUpdate != null) {
            for (Iterator i = onUpdate.iterator(); i.hasNext(); ) {
                CodeFragment f = (CodeFragment) i.next();
                f.invoke(this, oldValue);
            }
        }
    }
    
    /**
     * Get the solver object.
     * 
     * @return  solver object
     */
    public BDDSolver getSolver() {
        return solver;
    }
    
    /**
     * Set the BDD domain assignment of this relation to the given one.
     * 
     * @param newdom  new BDD domain assignment
     */
    public void setDomainAssignment(List newdom) {
        Assert._assert(newdom.size() == attributes.size());
        Assert._assert(new HashSet(newdom).size() == newdom.size(), newdom.toString());
        for (int i = 0; i < newdom.size(); ++i) {
            Domain d = ((Attribute) attributes.get(i)).getDomain();
            Assert._assert(solver.getBDDDomains(d).contains(newdom.get(i)));
        }
        this.domains = newdom;
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#verboseToString()
     */
    public String verboseToString(){
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append('[');
        boolean any = false;
        for (Iterator it = getAttributes().iterator(); it.hasNext(); ){
            any = true;
            Attribute a = (Attribute) it.next();
            sb.append(a + ":");
            if (domains != null)
                sb.append(getBDDDomain(a));
            else
                sb.append(a.attributeDomain.toString());
            sb.append(',');
        }
        if (any)
            sb.deleteCharAt(sb.length() - 1);
        sb.append(']');
       
        return sb.toString();
    }
}
