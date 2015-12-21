// Tuples.java, created Jan 23, 2005 5:41:14 PM by joewhaley
// Copyright (C) 2005 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Tuples
 * 
 * @author jwhaley
 * @version $Id: Tuples.java,v 1.1 2005/01/24 23:53:38 joewhaley Exp $
 */
public abstract class Tuples extends AbstractCollection {
    
    public abstract int add(int[] a);
    public abstract TupleIterator tupleIterator();
    public abstract int size();
    public abstract int tupleSize();
    
    public void dump(BufferedWriter out) throws IOException {
        dump("", out);
    }
    
    public void dump(String prefix, BufferedWriter out) throws IOException {
        int[] t = new int[tupleSize()];
        for (TupleIterator ti = tupleIterator(); ti.hasNext(); ) {
            t = ti.nextTuple(t);
            out.write(prefix);
            for (int i = 0; i < t.length; ++i) {
                out.write(Integer.toString(t[i]));
                out.write(' ');
            }
            out.write('\n');
        }
    }
    
    public Iterator iterator() { return tupleIterator(); }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        int[] t = new int[tupleSize()];
        for (TupleIterator ti = tupleIterator(); ti.hasNext(); ) {
            t = ti.nextTuple(t);
            sb.append('(');
            for (int i = 0; i < t.length; ++i) {
                if (i > 0) sb.append(',');
                sb.append(t[i]);
            }
            sb.append(')');
            if (ti.hasNext()) sb.append(", ");
        }
        return sb.toString();
    }
    
    public interface Interface {
        TupleIterator tupleIterator();
        int size();
        boolean isEmpty();
        void dump(BufferedWriter out) throws IOException;
        void dump(String prefix, BufferedWriter out) throws IOException;
    }
    
    public interface S1 extends Interface {
        int add(int a);
    }
    
    public interface S2 extends Interface {
        int add(int a, int b);
    }
    
    public interface S3 extends Interface {
        int add(int a, int b, int c);
    }
    
    public interface S4 extends Interface {
        int add(int a, int b, int c, int d);
    }
    
}
