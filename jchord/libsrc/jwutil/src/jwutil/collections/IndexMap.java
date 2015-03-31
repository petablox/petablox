// IndexMap.java, created Jun 15, 2003 2:04:05 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import jwutil.io.Textualizable;
import jwutil.io.Textualizer;
import jwutil.util.Assert;

/**
 * An IndexMap provides a fast mapping between elements and (integer)
 * indices.
 * 
 * @author John Whaley
 * @version $Id: IndexMap.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class IndexMap implements IndexedMap {
    private final String name;
    private final HashMap hash;
    private final ArrayList list;
    private final boolean trace;
    
    public IndexMap(String name) {
        this.name = name;
        hash = new HashMap();
        list = new ArrayList();
        trace = false;
    }
    
    public IndexMap(String name, int size) {
        this.name = name;
        hash = new HashMap(size);
        list = new ArrayList(size);
        trace = false;
    }
    
    public IndexMap(String name, int size, boolean t) {
        this.name = name;
        hash = new HashMap(size);
        list = new ArrayList(size);
        trace = t;
    }
    
    public int get(Object o) {
        Integer i = (Integer) hash.get(o);
        if (i == null) {
            hash.put(o, i = new Integer(list.size()));
            list.add(o);
            if (trace) System.out.println(this+"["+i+"] = "+o);
        }
        return i.intValue();
    }
        
    public Object get(int i) {
        return list.get(i);
    }
        
    public boolean contains(Object o) {
        return hash.containsKey(o);
    }
        
    public int size() {
        return list.size();
    }
        
    public String toString() {
        return name;
    }
    
    public Iterator iterator() {
        return list.iterator();
    }
    
    public void clear() {
        hash.clear();
        list.clear();
    }
    
    public boolean addAll(Collection c) {
        int before = size();
        for (Iterator i=c.iterator(); i.hasNext(); ) {
            get(i.next());
        }
        return before != size();
    }
    
    public boolean addAll(IndexedMap that) {
        for (Iterator i = that.iterator(); i.hasNext();) {
            Object o = i.next();
            this.hash.put(o, new Integer(this.list.size()));
            this.list.add(o);
        }
        return that.size() > 0;
    }
    
    public void dump(final BufferedWriter out) throws IOException {
        Textualizer t = new Textualizer.Map(out, this);
        t.writeString(size()+"\n");
        for (int j = 0; j < size(); ++j) {
            Textualizable o = (Textualizable) get(j);
            t.writeTypeOf(o);
            t.writeObject(o);
            t.writeString("\n");
        }
    }
    
    public void dumpStrings(final BufferedWriter out) throws IOException {
        for (int j = 0; j < size(); ++j) {
            Object o = get(j);
            out.write(o+"\n");
        }
    }
    
    public static IndexMap load(String name, BufferedReader in) throws IOException {
        String s = in.readLine();
        int size = Integer.parseInt(s);
        IndexMap dis = new IndexMap(name, size);
        Textualizer t = new Textualizer.Map(in, dis);
        for (int i = 0; i < size; ++i) {
            t.nextLine();
            Textualizable n = t.readObject();
            int j = dis.get(n);
            Assert._assert(i == j);
        }
        return dis;
    }
    
    public static IndexMap loadStringMap(String name, BufferedReader in) throws IOException {
        IndexMap dis = new IndexMap(name);
        for (;;) {
            String o = in.readLine();
            if (o == null) break;
            dis.hash.put(o, new Integer(dis.list.size()));
            dis.list.add(o);
        }
        return dis;
    }
}
