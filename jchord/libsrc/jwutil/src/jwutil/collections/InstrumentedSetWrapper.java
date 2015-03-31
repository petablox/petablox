// InstrumentedSetWrapper.java, created Wed Mar  5  0:26:26 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import jwutil.strings.Strings;
import jwutil.util.Assert;

/**
 * Allows you to profile set operations.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: InstrumentedSetWrapper.java,v 1.2 2004/10/03 10:54:35 joewhaley Exp $
 */
public class InstrumentedSetWrapper implements Set {

    private final Set wrappedSet;
    private final InstrumentationResults results;

    private abstract static class GlobalStats {
        
        private static final Map m = new HashMap();
        private static final ReferenceQueue q = new ReferenceQueue();
        private static final Map results = new HashMap();
        
        private static volatile Thread cleanupThread;
        static {
            Runnable cleanUp = new Runnable() {
                public void run() {
                    Thread thisThread = Thread.currentThread();
                    WeakReference wr;
                    while (thisThread == cleanupThread) {
                        try {
                            wr = (WeakReference)q.remove();
                            synchronized (m) {
                                InstrumentationResults i = (InstrumentationResults) m.get(wr);
                                if (i != null) {
                                    m.remove(wr);
                                    finish(i);
                                }
                            }
                            wr = null;
                        } catch (InterruptedException e) { }
                    }
                }
            };
            cleanupThread = new Thread(cleanUp);
            cleanupThread.setDaemon(true);
            cleanupThread.start();
        }

        public static void register(InstrumentedSetWrapper i) {
            synchronized (m) {
                Object o = m.put(new WeakReference(i, q), i.results);
                Assert._assert(o == null);
            }
        }
        
        static {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Thread t = cleanupThread;
                    cleanupThread = null;
                    t.interrupt();
                    synchronized (m) {
                        for (Iterator it=m.values().iterator(); it.hasNext(); ) {
                            InstrumentationResults i = (InstrumentationResults) it.next();
                            it.remove();
                            finish(i);
                        }
                    }
                    FileWriter out = null;
                    try {
                        out = new FileWriter("set_profile_data");
                        for (Iterator it=results.values().iterator(); it.hasNext(); ) {
                            InstrumentationResults i = (InstrumentationResults) it.next();
                            out.write(i.dump());
                            out.write(Strings.lineSep);
                        }
                    } catch (IOException x) {
                        System.err.println("IO Exception occurred while writing set profile data.");
                        x.printStackTrace();
                    } finally {
                        try {
                            if (out != null) out.close();
                        } catch (IOException x) { }
                    }
                }
            });
        }
        
        private static void finish(InstrumentationResults i) {
            InstrumentationResults i2 = (InstrumentationResults)results.get(i.identifier);
            //System.out.println("Finishing up results for "+i.identifier);
            if (i2 == null) results.put(i.identifier, i2 = i);
            else i2.mergeResults(i);
            //System.out.println("Results = "+i2.dump());
        }
    }

    public InstrumentedSetWrapper(Set s) {
        this.wrappedSet = s;
        //Object identifier = new Throwable().getStackTrace()[1];
        Object identifier = Arrays.asList(new Throwable().getStackTrace()).subList(1,4);
        this.results = new InstrumentationResults(identifier);
        GlobalStats.register(this);
    }

    public Set getWrappedSet() { return this.wrappedSet; }

    public String toString() {
        results._toString(this.wrappedSet);
        return wrappedSet.toString();
    }

    public int hashCode() {
        results._hashCode(this.wrappedSet);
        return wrappedSet.hashCode();
    }
    
    public boolean equals(Object o) {
        results._equals(this.wrappedSet, o);
        return wrappedSet.equals(o);
    }
    
    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        results._size(this.wrappedSet);
        return wrappedSet.size();
    }

    /**
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        results._isEmpty(this.wrappedSet);
        return wrappedSet.isEmpty();
    }

    /**
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object arg0) {
        results._contains(this.wrappedSet, arg0);
        return wrappedSet.contains(arg0);
    }

    /**
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        results._iterator(this.wrappedSet);
        return wrappedSet.iterator();
    }

    /**
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        results._toArray(this.wrappedSet);
        return wrappedSet.toArray();
    }

    /**
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] arg0) {
        results._toArray(this.wrappedSet, arg0);
        return wrappedSet.toArray(arg0);
    }

    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object arg0) {
        results._add(this.wrappedSet, arg0);
        return wrappedSet.add(arg0);
    }

    /**
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object arg0) {
        results._remove(this.wrappedSet, arg0);
        return wrappedSet.remove(arg0);
    }

    /**
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection arg0) {
        results._containsAll(this.wrappedSet, arg0);
        return wrappedSet.containsAll(arg0);
    }

    /**
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection arg0) {
        results._addAll(this.wrappedSet, arg0);
        return wrappedSet.addAll(arg0);
    }

    /**
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection arg0) {
        results._retainAll(this.wrappedSet, arg0);
        return wrappedSet.retainAll(arg0);
    }

    /**
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection arg0) {
        results._removeAll(this.wrappedSet, arg0);
        return wrappedSet.removeAll(arg0);
    }

    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        results._clear();
        wrappedSet.clear();
    }

    public static class InstrumentationResults {
        
        public final Object identifier;
        
        public InstrumentationResults(Object identifier) {
            this.identifier = identifier;
        }
        
        public void mergeResults(InstrumentationResults that) {
            Field[] fs = this.getClass().getDeclaredFields();
            for (int i=0; i<fs.length; ++i) {
                Field f = fs[i];
                if (f.getType() == int.class) {
                    try {
                        f.setInt(this, f.getInt(this)+f.getInt(that));
                    } catch (IllegalAccessException x) {
                        // cannot occur.
                        Assert.UNREACHABLE();
                    }
                }
            }
        }
        
        public boolean equals(Object o) {
            return equals((InstrumentationResults)o);
        }
        public boolean equals(InstrumentationResults that) {
            if (this.identifier == that.identifier) return true;
            if (this.identifier == null) return false;
            return this.identifier.equals(that.identifier);
        }
        public int hashCode() {
            return this.identifier == null ? 0 : this.identifier.hashCode();
        }
        
        public String dump() {
            StringBuffer sb = new StringBuffer();
            sb.append("Results");
            if (identifier != null) {
                sb.append(" for ");
                sb.append(identifier.toString());
            }
            sb.append(':');
            sb.append(Strings.lineSep);
            Field[] fs = this.getClass().getDeclaredFields();
            for (int i=0; i<fs.length; ++i) {
                Field f = fs[i];
                if (f.getType() == int.class) {
                    try {
                        int v = f.getInt(this);
                        if (v > 0)
                        {
                            sb.append(f.getName());
                            sb.append('=');
                            sb.append(v);
                            sb.append(Strings.lineSep);
                        }
                    } catch (IllegalAccessException x) {
                        // cannot occur.
                        Assert.UNREACHABLE();
                    }
                }
            }
            return sb.toString();
        }
        
        private int clear_count;
        private int removeAll_count;
        private int retainAll_count;
        private int addAll_count;
        private int containsAll_count;
        private int remove_count;
        private int add_count;
        private int toArray2_count;
        private int toArray_count;
        private int iterator_count;
        private int contains_count;
        private int isEmpty_count;
        private int size_count;
        private int equals_count;
        private int hashCode_count;
        private int toString_count;
        public void _clear() {
            //System.out.println(this+"clear");
            clear_count++;
        }
        public void _removeAll(Set set, Collection arg0) {
            //System.out.println(this+"removeAll");
            removeAll_count++;
        }
        public void _retainAll(Set set, Collection arg0) {
            //System.out.println(this+"retainAll");
            retainAll_count++;
        }
        public void _addAll(Set set, Collection arg0) {
            addAll_count++;
        }
        public void _containsAll(Set set, Collection arg0) {
            //System.out.println(this+"containsAll");
            containsAll_count++;
        }
        public void _remove(Set set, Object arg0) {
            remove_count++;
        }
        public void _add(Set set, Object arg0) {
            add_count++;
        }
        public void _toArray(Set set) {
            //System.out.println(this+"toArray");
            toArray_count++;
        }
        public void _toArray(Set set, Object[] arg0) {
            //System.out.println(this+"toArray2");
            toArray2_count++;
        }
        public void _iterator(Set set) {
            iterator_count++;
        }
        public void _contains(Set set, Object arg0) {
            //System.out.println(this+"contains");
            contains_count++;
        }
        public void _isEmpty(Set set) {
            isEmpty_count++;
        }
        public void _size(Set set) {
            size_count++;
        }
        public void _equals(Set set, Object o) {
            equals_count++;
        }
        public void _hashCode(Set set) {
            //System.out.println(this+"hashCode");
            hashCode_count++;
        }
        public void _toString(Set set) {
            //System.out.println(this+"toString");
            toString_count++;
        }
    }

}
