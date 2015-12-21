// TuplesArray.java, created Jan 23, 2005 5:52:03 PM by joewhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

/**
 * TuplesArray
 * 
 * @author jwhaley
 * @version $Id: TuplesArray.java,v 1.1 2005/01/24 23:53:38 joewhaley Exp $
 */
public class TuplesArray extends Tuples {
    
    public static class S1 extends TuplesArray implements Tuples.S1 {
        public S1(int estSize) { super(1, estSize); }
        public int add(int a) {
            if (num == tuples.length) grow(tuples.length*2);
            tuples[num++] = a;
            return num;
        }
    }
    
    public static class S2 extends TuplesArray implements Tuples.S2 {
        public S2(int estSize) { super(2, estSize); }
        public int add(int a, int b) {
            if (num*2 == tuples.length) grow(tuples.length*2);
            tuples[num*2] = a;
            tuples[num*2+1] = b;
            return num++;
        }
    }
    
    public static class S3 extends TuplesArray implements Tuples.S3 {
        public S3(int estSize) { super(3, estSize); }
        public int add(int a, int b, int c) {
            if (num*3 == tuples.length) grow(tuples.length*2);
            tuples[num*3] = a;
            tuples[num*3+1] = b;
            tuples[num*3+2] = c;
            return num++;
        }
    }
    
    public static class S4 extends TuplesArray implements Tuples.S4 {
        public S4(int estSize) { super(4, estSize); }
        public int add(int a, int b, int c, int d) {
            if (num*4 == tuples.length) grow(tuples.length*2);
            tuples[num*4] = a;
            tuples[num*4+1] = b;
            tuples[num*4+2] = c;
            tuples[num*4+3] = d;
            return num++;
        }
    }
    
    protected final int tupleSize;
    protected int[] tuples;
    protected int num;
    
    public TuplesArray(int tupleSize, int estSize) {
        this.tupleSize = tupleSize;
        this.tuples = new int[estSize * tupleSize];
        this.num = 0;
    }
    
    protected void grow(int newSize) {
        int[] a2 = new int[newSize];
        System.arraycopy(tuples, 0, a2, 0, tuples.length);
        tuples = a2;
    }
    
    /* (non-Javadoc)
     * @see jwutil.collections.Tuples#add(int[])
     */
    public int add(int[] a) {
        int ts = tupleSize;
        if (a.length != ts)
            throw new IllegalArgumentException("wrong tuple size: "+a.length+" instead of "+ts);
        if (num*ts == tuples.length)
            grow(tuples.length*2);
        System.arraycopy(a, 0, tuples, num*ts, ts);
        return ++num;
    }

    public TupleIterator tupleIterator() {
        return new TupleIterator() {
            int k;
            public boolean hasNext() {
                return k < num;
            }
            public int[] nextTuple(int[] t) {
                int ts = t.length;
                System.arraycopy(tuples, k*ts, t, 0, ts);
                ++k;
                return t;
            }
            public int[] nextTuple() {
                int ts = tupleSize;
                return nextTuple(new int[ts]);
            }
        };
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        return num;
    }
    
    /* (non-Javadoc)
     * @see jwutil.collections.Tuples#tupleSize()
     */
    public int tupleSize() {
        return tupleSize;
    }
}
