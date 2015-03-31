// SimpleHashSet.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import jwutil.util.Assert;

/**
 * A simple implementation of a hash map.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: SimpleHashSet.java,v 1.4 2005/05/05 19:38:49 joewhaley Exp $
 */
public class SimpleHashSet extends AbstractMap {

    public static final boolean TRACE = false;
    
    public static final int STARTING_TABLE_SIZE = 1024;
    public static final int STARTING_HASH_SIZE = 666;
    public static final int STARTING_CHAIN_SIZE = 4;
    
    public Object[] table = new Object[STARTING_TABLE_SIZE];
    public int size = -1;
    public int[][] chains = new int[STARTING_HASH_SIZE][]; // for hashing
    
    public Iterator iterator() {
        return Arrays.asList(table).subList(0, size+1).iterator();
    }
    
    public int size() { return size+1; }
    
    public boolean add(Object o) {
        int oldSize = this.size;
        o = getOrAdd(o);
        return size != oldSize;
    }
    
    public Set getMatchingHashcode(int hash) {
        return new ChainSet(this, hash);
    }
    
    public static class ChainSet extends AbstractSet {
        SimpleHashSet shs;
        int[] chain;
        int hash;
        public ChainSet(SimpleHashSet shs, int hash) {
            this.shs = shs;
            int chain_index = Math.abs(hash) % shs.chains.length;
            chain = shs.chains[chain_index];
            this.hash = hash;
            if (TRACE) System.out.println("Initialized chain set: hash="+hash+" index="+chain_index+" length="+(chain==null?-1:chain.length));
        }
        public Iterator iterator() {
            return new Itr();
        }
        public int size() {
            int s = 0, i = 0;
            for (;;) {
                if (i == chain.length)
                    return s;
                int loc = chain[i]-1;
                if (loc == -1)
                    return s;
                if (shs.table[loc].hashCode() == hash)
                    ++s;
                ++i;
            }
        }
        public class Itr extends UnmodifiableIterator {
            int i;
            public Itr() {
                i = -1;
                findNext();
            }
            private int findNext() {
                if (chain == null) return -1;
                for (;;) {
                    ++i;
                    if (i == chain.length)
                        return -1;
                    int loc = chain[i]-1;
                    if (loc == -1)
                        return -1;
                    if (shs.table[loc].hashCode() == hash) {
                        if (TRACE) System.out.println("next matching hash is chain["+i+"]="+loc);
                        return loc;
                    }
                }
            }
            public Object next() {
                if (chain == null || i == chain.length || chain[i]-1 == -1)
                    throw new NoSuchElementException();
                Object o = shs.table[chain[i]-1];
                findNext();
                return o;
            }
            public boolean hasNext() {
                return chain != null && i != chain.length && chain[i]-1 != -1;
            }
            public void addToEnd(Object o) {
                Assert._assert(o.hashCode() == hash);
                if (chain == null) {
                    int chain_index = Math.abs(hash) % shs.chains.length;
                    Assert._assert(shs.chains[chain_index] == null);
                    shs.chains[chain_index] = chain = new int[STARTING_CHAIN_SIZE];
                    i = 0;
                } else if (i == chain.length) {
                    int[] newchain = new int[chain.length<<1];
                    System.arraycopy(chain, 0, newchain, 0, chain.length);
                    int chain_index = Math.abs(hash) % shs.chains.length;
                    Assert._assert(shs.chains[chain_index] == (Object)chain);
                    shs.chains[chain_index] = newchain;
                    chain = newchain;
                } else if (chain[i]-1 != -1) {
                    throw new UnsupportedOperationException("not at end");
                }
                shs.addToTable_helper(o, chain, i);
            }
        }
        
    }
    
    public Object getOrAdd(Object o) {
        int id = getOrAddID(o);
        return table[id];
    }
    
    public Object get(Object o) {
        int id = getID(o);
        if (id == -1) return null;
        return table[id];
    }
    
    public int getID(Object b) {
        int hash = b.hashCode();
        int chain_index = Math.abs(hash) % chains.length;
        int[] chain = chains[chain_index];
        if (chain == null) return -1;
        for (int i=0; i<chain.length; ++i) {
            int id = chain[i]-1;
            if (id == -1) return -1;
            Object that = table[id];
            // ??? check hash before calling equals ???
            if (that.equals(b)) {
                // ??? swap first one and this one ???
                return id;
            }
        }
        return -1;
    }
    
    public int getOrAddID(Object b) {
        int hash = b.hashCode();
        int chain_index = Math.abs(hash) % chains.length;
        int[] chain = chains[chain_index];
        if (chain == null) {
            chains[chain_index] = chain = new int[STARTING_CHAIN_SIZE];
            return addToTable_helper(b, chain, 0);
        }
        for (int i=0; i<chain.length; ++i) {
            int id = chain[i]-1;
            if (id == -1) {
                return addToTable_helper(b, chain, i);
            }
            if (TRACE) System.out.println("Id="+id);
            Object that = table[id];
            // ??? check hash before calling equals ???
            if (that.equals(b)) {
                // ??? swap first one and this one ???
                return id;
            }
        }
        int[] newchain = new int[chain.length<<1];
        System.arraycopy(chain, 0, newchain, 0, chain.length);
        chains[chain_index] = newchain;
        return addToTable_helper(b, newchain, chain.length);
        // free(chain)
        
        // todo: rehash when the table gets too full...
    }
    
    // Helper function.
    private int addToTable_helper(Object b, int[] chain, int index) {
        if (++size == table.length) growTable_helper();
        table[size] = b;
        chain[index] = size+1;
        if (TRACE) System.out.println("added to table["+size+"]: "+table[size]);
        return size;
    }
    
    // Helper function.
    private void growTable_helper() {
        Object[] newtable = new Object[size<<1];
        System.arraycopy(table, 0, newtable, 0, size);
        table = newtable;
    }
    
    public boolean contains(Object arg0) {
        return getID(arg0) != -1;
    }
    
    /**
     * @see java.util.Map#containsKey(Object)
     */
    public boolean containsKey(Object arg0) {
        return contains(arg0);
    }

    /**
     * @see java.util.Map#containsValue(Object)
     */
    public boolean containsValue(Object arg0) {
        return contains(arg0);
    }

    /**
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        return new EntrySet();
    }
    
    public static class Entry extends AbstractMapEntry {
        Object o;
        public Entry(Object o) { this.o = o; }
        public Object getKey() { return o; }
        public Object getValue() { return o; }
        public Object setValue(Object o) { throw new UnsupportedOperationException(); }
    }
    
    public class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return new Itr();
        }
        public int size() {
            return SimpleHashSet.this.size();
        }
        public class Itr extends UnmodifiableIterator {
            Iterator itr = SimpleHashSet.this.iterator();
            public Itr() { }
            public Object next() { return new Entry(itr.next()); }
            public boolean hasNext() { return itr.hasNext(); }
        }
    }

    public class SetView implements Set {
        public Iterator iterator() {
            return SimpleHashSet.this.iterator();
        }
        public int size() {
            return SimpleHashSet.this.size();
        }
        
        /**
         * @see java.util.Collection#add(Object)
         */
        public boolean add(Object arg0) {
            return SimpleHashSet.this.add(arg0);
        }

        /**
         * @see java.util.Collection#addAll(Collection)
         */
        public boolean addAll(Collection arg0) {
            boolean change = false;
            for (Iterator i=arg0.iterator(); i.hasNext(); ) {
                if (add(i.next()))
                    change = true;
            }
            return change;
        }

        /**
         * @see java.util.Collection#clear()
         */
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#contains(Object)
         */
        public boolean contains(Object arg0) {
            return SimpleHashSet.this.contains(arg0);
        }

        /**
         * @see java.util.Collection#containsAll(Collection)
         */
        public boolean containsAll(Collection arg0) {
            return SimpleHashSet.this.containsAll(arg0);
        }

        /**
         * @see java.util.Collection#isEmpty()
         */
        public boolean isEmpty() {
            return SimpleHashSet.this.isEmpty();
        }

        /**
         * @see java.util.Collection#remove(Object)
         */
        public boolean remove(Object arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#removeAll(Collection)
         */
        public boolean removeAll(Collection arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#retainAll(Collection)
         */
        public boolean retainAll(Collection arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#toArray()
         */
        public Object[] toArray() {
            return SimpleHashSet.this.toArray();
        }

        /**
         * @see java.util.Collection#toArray(Object[])
         */
        public Object[] toArray(Object[] arg0) {
            return SimpleHashSet.this.toArray(arg0);
        }

    }
    
    public boolean containsAll(Collection c) {
        Iterator e = c.iterator();
        while (e.hasNext())
            if (!contains(e.next()))
                return false;
        return true;
    }
    
    public Object[] toArray() {
        Object[] o = new Object[size()];
        int j = -1;
        for (Iterator i=iterator(); i.hasNext(); ) {
            o[++j] = i.next();
        }
        Assert._assert(j+1 == o.length);
        return o;
    }
    
    public Object[] toArray(Object[] a) {
        int this_size = size();
        if (this_size > a.length) {
            a = (Object[])java.lang.reflect.Array.newInstance(
                                  a.getClass().getComponentType(), this_size);
        }
        int j = -1;
        for (Iterator i = iterator(); i.hasNext(); ) {
            a[++j] = i.next();
        }
        if (++j < a.length)
            a[j] = null;
        return a;
    }
    
    public Set getAsSet() {
        return new SetView();
    }
    
    /**
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return this.getAsSet();
    }

    /**
     * @see java.util.Map#put(Object, Object)
     */
    public Object put(Object arg0, Object arg1) {
        Assert._assert(arg0 == arg1);
        arg1 = getOrAdd(arg0);
        // we are supposed to return the old one...
        return null;
    }

    /**
     * @see java.util.Map#putAll(Map)
     */
    public void putAll(Map arg0) {
        Iterator i = arg0.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            Assert._assert(e.getKey() == e.getValue());
            add(e.getKey());
        }
    }

    /**
     * @see java.util.Map#values()
     */
    public Collection values() {
        return this.getAsSet();
    }

}
