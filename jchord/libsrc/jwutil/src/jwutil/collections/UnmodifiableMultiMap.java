// UnmodifiableMultiMap.java, created Fri Mar 28 23:58:36 2003 by joewhaley
// Copyright (C) 2001-3 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.AbstractMap;

import jwutil.strings.Strings;

/** <code>UnmodifiableMultiMap</code> is an abstract superclass to
    save developers the trouble of implementing the various mutator
    methds of the <code>MultiMap</code> interface.

    @author  Felix S. Klock II <pnkfelix@mit.edu>
    @version $Id: UnmodifiableMultiMap.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
*/
public abstract class UnmodifiableMultiMap/*<K,V>*/ 
    extends AbstractMap/*<K,V>*/ implements MultiMap/*<K,V>*/ {

    /** Constructs and returns an unmodifiable <code>MultiMap</code>
    backed by <code>mmap</code>.
    */
    public static /*<K,V>*/ MultiMap/*<K,V>*/ proxy(final MultiMap/*<K,V>*/ mmap) {
        return new UnmodifiableMultiMap/*<K,V>*/() {
            public Object/*V*/ get(Object key) { 
                return mmap.get(key); 
            }
            public Collection/*<V>*/ getValues(Object/*K*/ key) { 
                return mmap.getValues(key);
            }
            public boolean contains(Object a, Object b) { 
                return mmap.contains(a, b);
            }
            public Set/*<Map.Entry<K,V>>*/ entrySet() { return mmap.entrySet(); }
        };
    }
    /** Returns a <code>Set</code> view that allows you to recapture
     *  the <code>MultiMap</code> view. */
    public abstract Set/*<Map.Entry<K,V>>*/ entrySet();
    
    protected Set/*<Map.Entry<K,V>>*/ entrySetHelper(final Set/*<K>*/ keys) {
        Map m = new AbstractMap() {
            public Set entrySet() {
                return new AbstractSet() {
                    public int size() { return keys.size(); }
                    public Iterator iterator() {
                        final Iterator i = keys.iterator();
                        return new UnmodifiableIterator() {
                            public boolean hasNext() { return i.hasNext(); }
                            public Object next() {
                                final Object o = i.next();
                                return new Map.Entry() {
                                    public Object getKey() { return o; }
                                    public Object getValue() {
                                        return getValues(o);
                                    }
                                    public Object setValue(Object value) {
                                        throw new UnsupportedOperationException();
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
        return new GenericMultiMap.GenericMultiMapEntrySet(m);
    }

    /** Throws UnsupportedOperationException. */
    public Object/*V*/ put(Object/*K*/ key, Object/*V*/ value) { die(); return null; }
    /** Throws UnsupportedOperationException. */
    public Object/*V*/ remove(Object key) { die(); return null; }
    /** Throws UnsupportedOperationException. */
    public boolean remove(Object key, Object value) { return die(); }
    /** Throws UnsupportedOperationException. */
    public /*<K2 extends K, V2 extends V>*/ void putAll(Map/*<K2,V2>*/ t) { die(); }
    /** Throws UnsupportedOperationException. */
    public void clear() { die(); }
    /** Throws UnsupportedOperationException. */
    public boolean add(Object/*K*/ key, Object/*V*/ value) { return die(); }
    /** Throws UnsupportedOperationException. */
    public /*<V2 extends V>*/ boolean addAll(Object/*K*/ key, Collection/*<V2>*/ values) { return die(); }
    /** Throws UnsupportedOperationException. */
    public /*<K2 extends K, V2 extends V>*/ boolean addAll(MultiMap/*<K2,V2>*/ mm) { return die(); }
    /** Throws UnsupportedOperationException. */
    public /*<T>*/ boolean retainAll(Object/*K*/ key, Collection/*<T>*/ values) { return die(); }
    /** Throws UnsupportedOperationException. */
    public /*<T>*/ boolean removeAll(Object/*K*/ key, Collection/*<T>*/ values) { return die(); }
    private boolean die() {
        throw new UnsupportedOperationException();
    }
    
    public String computeHistogram(String keyName, String valueName) {
        return computeHistogram(this, DEFAULT_HISTOGRAM_SIZE, keyName, valueName);
    }
    
    public static final int DEFAULT_HISTOGRAM_SIZE = 100;

    public static String computeHistogram(MultiMap dis, int size, String keyName, String valueName) {
        int[] histogram = new int[size];
        int keys = 0;
        long total = 0;
        for (Iterator i = dis.keySet().iterator(); i.hasNext(); ) {
            Object key = i.next();
            Collection values = dis.getValues(key);
            int x = values.size();
            if (x >= size) x = size-1;
            histogram[x]++;
            keys++;
            total += x;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(" Total # of edges: ");
        sb.append(total);
        sb.append(Strings.lineSep);
        for (int i=0; i<size; ++i) {
            if (histogram[i] > 0) {
                if (i == size-1) sb.append(">=");
                sb.append(i);
                sb.append(' ');
                sb.append(keyName);
                if (i != 1) sb.append('s');
                sb.append(":\t");
                sb.append(histogram[i]);
                sb.append(' ');
                sb.append(valueName);
                if (histogram[i] > 1) sb.append('s');
                sb.append(Strings.lineSep);
            }
        }
        sb.append("Average # of ");
        sb.append(valueName);
        sb.append("s: ");
        sb.append((double)total/(double)keys);
        return sb.toString();
    }
    
}
