// Textualizer.java, created Oct 26, 2003 5:15:38 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.io;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jwutil.collections.IndexMap;
import jwutil.collections.IndexedMap;
import jwutil.collections.Pair;
import jwutil.util.Assert;

/**
 * Textualizer
 * 
 * @author John Whaley
 * @version $Id: Textualizer.java,v 1.1 2004/09/27 22:42:34 joewhaley Exp $
 */
public interface Textualizer {

    Textualizable readObject() throws IOException;
    Textualizable readReference() throws IOException;
    
    void writeTypeOf(Textualizable object) throws IOException;
    void writeObject(Textualizable object) throws IOException;
    void writeEdge(String edgeName, Textualizable object) throws IOException;
    void writeReference(Textualizable object) throws IOException;

    void writeString(String s) throws IOException;
    
    StringTokenizer nextLine() throws IOException;

    int getIndex(Textualizable object);
    boolean contains(Textualizable object);
    
    public static class Simple implements Textualizer {
        protected BufferedReader in;
        protected BufferedWriter out;
        protected StringTokenizer st;
        protected String currentLine;
        
        public Simple(BufferedReader in) {
            this.in = in;
        }
        
        public Simple(BufferedWriter out) {
            this.out = out;
        }
        
        public StringTokenizer nextLine() throws IOException {
            return st = new StringTokenizer(currentLine = in.readLine());
        }
        
        protected void updateTokenizer() throws IOException {
            if (st == null || !st.hasMoreElements())
                st = new StringTokenizer(currentLine = in.readLine());
        }
        
        public Textualizable readObject() throws IOException {
            updateTokenizer();
            String className = st.nextToken();
            if (className.equals("null")) return null;
            try {
                Class c = Class.forName(className);
                Method m = c.getMethod("read", new Class[] {StringTokenizer.class});
                return (Textualizable) m.invoke(null, new Object[] {st});
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        public Textualizable readReference() throws IOException {
            return readObject();
        }
        
        public void writeTypeOf(Textualizable object) throws IOException {
            if (object != null) {
                out.write(object.getClass().getName());
                out.write(' ');
            }
        }
        
        public void writeObject(Textualizable object) throws IOException {
            if (object != null) {
                object.write(this);
            } else {
                out.write("null");
            }
        }
        
        public void writeReference(Textualizable object) throws IOException {
            writeObject(object);
        }
        
        public void writeEdge(String edgeName, Textualizable object) throws IOException {
            throw new InternalError();
        }

        public void writeString(String s) throws IOException {
            out.write(s);
        }

        public int getIndex(Textualizable object) {
            throw new InternalError();
        }
        
        public boolean contains(Textualizable object) {
            throw new InternalError();
        }
    }
    
    public static class Map extends Simple {
        protected IndexedMap map;
        protected java.util.Map deferredEdges;
        
        public Map(BufferedReader in) {
            this(in, new IndexMap(""));
        }
        
        public Map(BufferedReader in, IndexedMap map) {
            super(in);
            this.map = map;
        }
        
        public Map(BufferedWriter out, IndexedMap map) {
            super(out);
            this.map = map;
        }
        
        public Textualizable readObject() throws IOException {
            Textualizable t = super.readObject();
            int s = map.size();
            int f = map.get(t);
            if (f != s) System.out.println("object " + t + " is already in table, line=" + currentLine);
            Assert._assert(f == s);
            if (false) readEdges(t);
            if (deferredEdges != null) {
                Collection d = (Collection) deferredEdges.get(new Integer(f));
                if (d != null) {
                    for (Iterator i = d.iterator(); i.hasNext(); ) {
                        Pair def = (Pair) i.next();
                        Textualizable source = (Textualizable) def.left;
                        String edgeName = (String) def.right;
                        source.addEdge(edgeName, t);
                    }
                    deferredEdges.remove(t);
                }
            }
            return t;
        }
        
        public void readEdges(Textualizable t) {
            while (st.hasMoreTokens()) {
                String edgeName = st.nextToken();
                int index = Integer.parseInt(st.nextToken());
                if (index >= map.size()) {
                    Integer i = new Integer(index);
                    Collection c = (Collection) deferredEdges.get(i);
                    if (c == null) deferredEdges.put(i, c = new LinkedList());
                    c.add(new Pair(t, edgeName));
                } else {
                    Textualizable t2 = (Textualizable) map.get(index);
                    t.addEdge(edgeName, t2);
                }
            }
        }
        
        public Textualizable readReference() throws IOException {
            updateTokenizer();
            int id = Integer.parseInt(st.nextToken());
            return (Textualizable) map.get(id);
        }
        
        public void writeObject(Textualizable object) throws IOException {
            //map.get(object);
            super.writeObject(object);
            if (object != null) object.writeEdges(this);
        }
        
        public void writeReference(Textualizable object) throws IOException {
            if (!map.contains(object)) {
                System.out.println("Not in map: "+object);
                writeObject(object);
            } else {
                int id = map.get(object);
                out.write(Integer.toString(id));
            }
        }

        public void writeEdge(String edgeName, Textualizable target) throws IOException {
            out.write(' ');
            out.write(edgeName);
            out.write(' ');
            map.get(target);
            writeReference(target);
        }
        
        public int getIndex(Textualizable object) {
            Assert._assert(map.contains(object));
            return map.get(object);
        }
        
        public boolean contains(Textualizable object) {
            return map.contains(object);
        }
        
        public IndexedMap getMap() {
            return map;
        }
    }
    
}
