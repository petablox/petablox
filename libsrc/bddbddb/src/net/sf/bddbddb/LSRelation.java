// LSRelation.java, created Feb 8, 2005 4:29:57 AM by joewhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import jwutil.collections.SortedArraySet;
import jwutil.collections.SortedIntArraySet;
import jwutil.util.Assert;

/**
 * LSRelation
 * 
 * @author jwhaley
 * @version $Id: LSRelation.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class LSRelation extends Relation {

    /**
     * Reference to solver.
     */
    LSSolver solver;
    
    /**
     * Holds the actual data.  Only one of these is non-null.
     */
    SortedIntArraySet intSet;
    SortedArraySet objSet;
    
    /**
     * Number of bits used for each attribute.
     */
    int[] bits;
    
    /**
     * Add tuple to this relation.
     * 
     * @param t  tuple to add
     * @return  whether relation has changed
     */
    public boolean add(BigInteger[] t) {
        if (objSet != null) return objSet.add(t);
        else return intSet.add(compress(t));
    }
    
    /**
     * Extract packed value x into given array.
     * 
     * @param arr  array to extract into
     * @param x  packed value
     */
    protected void extract(BigInteger[] arr, int x) {
        for (int k = 0; k < bits.length; ++k) {
            arr[k] = BigInteger.valueOf(x & ((1 << bits[k]) - 1));
            x >>>= bits[k];
        }
    }
    
    /**
     * Compress the given array into a packed value.
     * 
     * @param arr  array to compress
     * @return  packed value
     */
    protected int compress(BigInteger[] arr) {
        int result = 0;
        for (int k = 0; k < bits.length; ++k) {
            if (arr[k].bitLength() > bits[k])
                throw new InternalError(arr[k]+" too big for "+bits[k]+" bits");
            result <<= bits[k];
            result |= arr[k].intValue();
        }
        return result;
    }
    
    /**
     * Construct a new LSRelation.
     * 
     * @param solver  solver object
     * @param name  relation name
     * @param attributes  relation attributes
     */
    LSRelation(LSSolver solver, String name, List attributes) {
        super(solver, name, attributes);
        this.solver = solver;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#initialize()
     */
    public void initialize() {
        int totalBits = 0;
        bits = new int[attributes.size()];
        int k = 0;
        for (Iterator i = attributes.iterator(); i.hasNext(); ++k) {
            Attribute a = (Attribute) i.next();
            bits[k] = a.attributeDomain.size.bitLength();
            totalBits += bits[k];
        }
        if (totalBits < 32) intSet = new SortedIntArraySet();
        else objSet = (SortedArraySet) SortedArraySet.FACTORY.makeSet(TUPLE_COMPARATOR);
    }

    public static final TupleComparator TUPLE_COMPARATOR = new TupleComparator();
    
    public static class TupleComparator implements Comparator {

        private TupleComparator() { }
        
        public int compare(Object arg0, Object arg1) {
            BigInteger[] a = (BigInteger[]) arg0;
            BigInteger[] b = (BigInteger[]) arg1;
            for (int i = 0; i < a.length; ++i) {
                int v = a[i].compareTo(b[i]);
                if (v != 0) return v;
            }
            return 0;
        }
        
    }
    
    BDDSolver temp;
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#load()
     */
    public void load() throws IOException {
        if (temp == null) {
            temp = new BDDSolver();
            try {
                temp.load(solver.inputFilename);
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        Relation r = temp.getRelation(name);
        r.load();
        TupleIterator i = r.iterator();
        while (i.hasNext()) {
            this.add(i.nextTuple());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#loadTuples()
     */
    public void loadTuples() throws IOException {
        loadTuples(solver.basedir + name + ".tuples");
        if (solver.NOISY) solver.out.println("Loaded tuples from file: " + name + ".tuples");
    }

    List checkInfoLine(String filename, String s, boolean order, boolean ex) throws IOException {
        // todo.
        return null;
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
            } else {
                checkInfoLine(filename, s, true, true);
            }
            for (;;) {
                s = in.readLine();
                if (s == null) break;
                if (s.length() == 0) continue;
                if (s.startsWith("#")) continue;
                parseTuple(s);
            }
        } finally {
            if (in != null) in.close();
        }
        updateNegated();
    }

    /**
     * Updated the negated form of this relation.
     */
    void updateNegated() {
        if (negated != null) {
            // TODO.
        }
    }
    
    void parseTuple(BigInteger[] t, int i, String s) {
        if (i == t.length) {
            add(t);
            return;
        }
        int z = s.indexOf(' ');
        String v = (z < 0) ? s : s.substring(0, z);
        if (z <= 0) s = "";
        else s = s.substring(z+1);
        BigInteger l, m;
        if (v.equals("*")) {
            Attribute a = (Attribute) attributes.get(i);
            l = BigInteger.ZERO;
            m = a.attributeDomain.size.subtract(BigInteger.ONE);
        } else {
            int x = v.indexOf('-');
            if (x < 0) {
                t[i] = new BigInteger(v);
                parseTuple(t, i+1, s);
                return;
            } else {
                l = new BigInteger(v.substring(0, x));
                m = new BigInteger(v.substring(x + 1));
            }
        }
        while (l.compareTo(m) <= 0) {
            t[i] = l;
            parseTuple(t, i+1, s);
            l = l.add(BigInteger.ONE);
        }
    }
    
    /**
     * Parse the given tuple string and add it to the relation.
     * 
     * @param s  tuple string
     */
    void parseTuple(String s) {
        BigInteger[] t = new BigInteger[attributes.size()];
        if (s.indexOf('-') >= 0 || s.indexOf('*') >= 0) {
            parseTuple(t, 0, s);
            return;
        }
        StringTokenizer st = new StringTokenizer(s);
        for (int i = 0; i < t.length; ++i) {
            String v = st.nextToken();
            t[i] = new BigInteger(v);
        }
        add(t);
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#save()
     */
    public void save() throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#saveTuples()
     */
    public void saveTuples() throws IOException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#saveTuples(java.lang.String)
     */
    public void saveTuples(String filename) throws IOException {
        // TODO Auto-generated method stub
        
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
        intSet = null; objSet = null;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#dsize()
     */
    public double dsize() {
        if (intSet != null) return intSet.size();
        else return objSet.size();
    }

    BigInteger[] getTuple(BigInteger[] arr, int k) {
        if (intSet != null) {
            int x = intSet.get(k);
            extract(arr, x);
            return arr;
        } else {
            BigInteger[] x = (BigInteger[]) objSet.get(k++);
            return x;
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator()
     */
    public TupleIterator iterator() {
        return new TupleIterator() {
            int k = 0;
            BigInteger[] arr;
            { if (intSet != null) arr = new BigInteger[attributes.size()]; }
            public BigInteger[] nextTuple() {
                if (k == size()) throw new NoSuchElementException();
                return getTuple(arr, k++);
            }
            public boolean hasNext() {
                return k < size();
            }
        };
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(int)
     */
    public TupleIterator iterator(final int k) {
        return new TupleIterator() {
            int n = 0;
            BigInteger[] arr;
            { if (intSet != null) arr = new BigInteger[attributes.size()]; }
            void gotoNext(BigInteger currVal) {
                while (n < arr.length) {
                    arr = getTuple(arr, n);
                    if (!arr[k].equals(currVal)) return;
                    ++n;
                }
            }
            public BigInteger[] nextTuple() {
                if (n == size()) throw new NoSuchElementException();
                arr = getTuple(arr, n);
                gotoNext(arr[k]);
                return new BigInteger[] { arr[k] };
            }
            public boolean hasNext() {
                return n < size();
            }
        };
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(int, java.math.BigInteger)
     */
    public TupleIterator iterator(final int k, final BigInteger j) {
        return new TupleIterator() {
            int n = 0;
            BigInteger[] arr;
            { if (intSet != null) arr = new BigInteger[attributes.size()]; }
            void gotoNext() {
                while (n < arr.length) {
                    arr = getTuple(arr, n);
                    if (arr[k].equals(j)) return;
                    ++n;
                }
                throw new NoSuchElementException();
            }
            public BigInteger[] nextTuple() {
                gotoNext();
                return arr;
            }
            public boolean hasNext() {
                return n < size();
            }
        };
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#iterator(java.math.BigInteger[])
     */
    public TupleIterator iterator(final BigInteger[] j) {
        return new TupleIterator() {
            int n = 0;
            BigInteger[] arr;
            { if (intSet != null) arr = new BigInteger[attributes.size()]; }
            void gotoNext() {
outer:
                while (n < arr.length) {
                    arr = getTuple(arr, n);
                    for (int k = 0; k < j.length; ++k) {
                        if (j[k].signum() >= 0 && !arr[k].equals(j)) {
                            ++n;
                            continue outer;
                        }
                    }
                    return;
                }
                throw new NoSuchElementException();
            }
            public BigInteger[] nextTuple() {
                gotoNext();
                return arr;
            }
            public boolean hasNext() {
                return n < size();
            }
        };
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.Relation#contains(int, java.math.BigInteger)
     */
    public boolean contains(int k, BigInteger j) {
        for (TupleIterator i = iterator(); i.hasNext(); ) {
            BigInteger[] t = i.nextTuple();
            if (t[k].equals(j)) return true;
        }
        return false;
    }

    public String verboseToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("[");
        boolean any = false;
        for(Iterator it = getAttributes().iterator(); it.hasNext(); ){
            any = true;
            Attribute a = (Attribute) it.next();
            sb.append(a + ",");
        }
        if(any)
            sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
       
        return sb.toString();
    }

}
