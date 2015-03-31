// HashCodeComparator.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import jwutil.strings.Strings;
import jwutil.util.Assert;

/**
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: HashCodeComparator.java,v 1.2 2005/05/05 18:52:16 joewhaley Exp $
 */
public class HashCodeComparator implements Comparator {

    public static final boolean USE_IDENTITY_HASHCODE = false;
    public static final boolean USE_WEAK_REFERENCES = true;
    
    public static final HashCodeComparator INSTANCE = new HashCodeComparator();

    public static final boolean TRACE = false;
    public static final boolean TEST = false;

    private List duplicate_hashcode_objects;
    private final ReferenceQueue queue;
    private volatile Thread cleanupThread;
    
    public HashCodeComparator() {
        if (USE_WEAK_REFERENCES) {
            queue = new ReferenceQueue();
            Runnable cleanUp = new Runnable() {
                public void run() {
                    Thread thisThread = Thread.currentThread();
                    WeakReference wr;
                    while (thisThread == cleanupThread) {
                        try {
                            wr = (WeakReference)queue.remove();
                            duplicate_hashcode_objects.remove(wr);
                            wr = null;
                        } catch (InterruptedException e) { }
                    }
                }
            };
            cleanupThread = new Thread(cleanUp);
            cleanupThread.setDaemon(true);
            cleanupThread.start();
        } else {
            queue = null;
        }
    }

    protected void finalize() {
        Thread t = this.cleanupThread;
        this.cleanupThread = null;
        t.interrupt();
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object arg0, Object arg1) {
        boolean eq;
        if (USE_IDENTITY_HASHCODE) eq = arg0 == arg1;
        else eq = arg0.equals(arg1);
        if (eq) return 0;
        int a, b;
        if (USE_IDENTITY_HASHCODE) {
            a = System.identityHashCode(arg0);
            b = System.identityHashCode(arg1);
        } else {
            a = arg0.hashCode();
            b = arg1.hashCode();
        }
        if (TEST) {
            a %= 6835; b %= 6835;
        }
        if (a > b) return 1;
        if (a < b) return -1;
        // double-check locking idiom.
        if (duplicate_hashcode_objects == null) {
            synchronized (this) {
                if (duplicate_hashcode_objects == null) {
                    duplicate_hashcode_objects = Collections.synchronizedList(new LinkedList());
                }
            }
        }
        int i1 = indexOf(arg0);
        int i2 = indexOf(arg1);
        if (i1 == -1) {
            if (i2 == -1) {
                if (TRACE) {
                    i1 = duplicate_hashcode_objects.size();
                    System.out.println("Hash code conflict: "+Strings.hex8(a)+" "+arg0+" vs. "+arg1+", allocating ("+i1+")");
                }
                if (USE_WEAK_REFERENCES) arg0 = new WeakReference(arg0, queue);
                duplicate_hashcode_objects.add(arg0);
                return -1;
            } else {
                return 1;
            }
        } else if (i1 < i2 || i2 == -1) {
            return -1;
        } else {
            Assert._assert(i1 > i2);
            return 1;
        }
    }

    private int indexOf(Object o) {
        if (!USE_WEAK_REFERENCES) {
            if (USE_IDENTITY_HASHCODE) o = IdentityHashCodeWrapper.create(o);
            return duplicate_hashcode_objects.indexOf(o);
        }
        int index = 0;
        // lock the object just like a call to indexOf().
        synchronized (duplicate_hashcode_objects) {
            for (Iterator i=duplicate_hashcode_objects.iterator(); i.hasNext(); ++index) {
                WeakReference r = (WeakReference) i.next();
                if (USE_IDENTITY_HASHCODE) {
                    if (o == r.get()) return index;
                } else {
                    if (o.equals(r.get())) return index;
                }
            }
        }
        return -1;
    }

}
