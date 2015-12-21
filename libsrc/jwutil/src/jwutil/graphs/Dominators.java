// Dominators.java, created Aug 18, 2003 12:42:07 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jwutil.collections.HashWorklist;
import jwutil.collections.IndexMap;

/**
 * Dominators
 * 
 * @author John Whaley
 * @version $Id: Dominators.java,v 1.3 2004/10/03 10:51:36 joewhaley Exp $
 */
public class Dominators {

    /**
     * Map from dominator node to domination information.
     */
    private Map domination;

    /**
     * Graph navigator.
     */
    private Navigator navigator;

    /**
     * For computation of dominators.
     */
    private Object[] vertex;

    /**
     * For computation of dominators.
     */
    private Object[] parent;

    /**
     * For computation of dominators.
     */
    private Object[] label;

    /**
     * For computation of dominators.
     */
    private Object[] ancestor;

    /**
     * For computation of dominators.
     */
    private List[] buckets;

    /**
     * For computation of dominators.
     */
    private int[] semi;

    /**
     * Map for labeling the nodes.
     */
    private Map map;

    /**
     * True if we are computing post-dominators, false otherwise.
     */
    private boolean post;

    /**
     * @param post false for dominators, true for post dominators to be determined
     * @param start the starting node in the graph (or end if post dominators)
     */
    public Dominators(boolean post, Object start, Navigator nav) {
        this.post = post;
        this.navigator = nav;

        int num = setLabels(start);

        map = new HashMap(1 + num / 4);
        vertex = new Object[num];
        semi = new int[num];
        parent = new Object[num];
        ancestor = new Object[num];
        label = new Object[num];
        buckets = new ArrayList[num];
        domination = new HashMap(1 + num / 4);

        for (int i = 0; i < num; i++) {
            semi[i] = -1;
            buckets[i] = new ArrayList();
        }

        int n = DFSFromArcs(post, start);

        // Make sure we record any nodes that are dead
        for (int j = 0; j < num; j++)
            if (vertex[j] != null)
                if (post)
                    n = processOutEdges(vertex[j], n);
                else
                    n = processInEdges(vertex[j], n);

        // Compute the dominators
        generate(post, start, num);

        semi = null;
        parent = null;
        ancestor = null;
        label = null;
        buckets = null;
        map = null;
    }

    private IndexMap labels;

    private int getLabel(Object o) {
        return labels.get(o);
    }
    
    private int setLabels(Object d) {
        HashWorklist wl = new HashWorklist(true);

        wl.add(d);
        labels = new IndexMap("Labels");

        while (!wl.isEmpty()) {
            Object v = wl.pull();
            getLabel(v);

            wl.addAll(navigator.prev(v));
            wl.addAll(navigator.next(v));
        }

        return labels.size();
    }

    private int DFSFromArcs(boolean post, Object d) {
        int n = 0;
        LinkedList wl = new LinkedList();

        wl.add(d);

        while (!wl.isEmpty()) {
            Object v = wl.removeLast();
            int vid = getLabel(v);
            if (semi[vid] >= 0)
                continue;

            semi[vid] = n;
            vertex[n] = v;
            label[vid] = v;
            n++;

            Collection c;
            if (post) {
                c = navigator.prev(v);
            } else {
                c = navigator.next(v);
            }
            for (Iterator i=c.iterator(); i.hasNext(); ) {
                Object w = i.next();
                int wid = getLabel(w);
                if (semi[wid] == -1) {
                    parent[wid] = v;
                    wl.add(w);
                }
            }
        }

        return n;
    }

    /**
     * Handle the cases where there is dead code.  That is, nodes for which there is no path
     * from the start node.
     */
    private int processInEdges(Object d, int n) {
        LinkedList wl = new LinkedList();

        wl.add(d);

        while (!wl.isEmpty()) {
            Object v = wl.removeLast();
            for (Iterator i=navigator.prev(v).iterator(); i.hasNext(); ) {
                Object w = i.next();
                int wid = getLabel(w);
                if (semi[wid] == -1) {
                    semi[wid] = n;
                    vertex[n] = w;
                    label[wid] = w;
                    n++;
                    wl.add(w);
                }
            }
        }

        return n;
    }

    /**
     * Handle the cases where there is dead code.  That is, nodes for which there is no path
     * to the end node.
     */
    private int processOutEdges(Object d, int n) {
        LinkedList wl = new LinkedList();

        wl.add(d);

        while (!wl.isEmpty()) {
            Object v = wl.removeLast();
            for (Iterator i=navigator.next(v).iterator(); i.hasNext(); ) {
                Object w = i.next();
                int wid = getLabel(w);
                if (semi[wid] == -1) {
                    semi[wid] = n;
                    vertex[n] = w;
                    label[wid] = w;
                    n++;
                    wl.add(w);
                }
            }
        }

        return n;
    }

    /**
     * Object calculation: dominator and post dominator
     * As described in A Fast Algorithm for Finding Dominators in a Flow Graph
     * by Lengauer and Tarjan, ACM. Tran. on Prog. Lang and System, July 1979
     */
    private void generate(boolean post, Object start, int n) {
        for (int i = n - 1; i >= 1; i--) {
            Object w = vertex[i];
            int wid = getLabel(w);

            // step 2
            Collection c;
            if (post) {
                c = navigator.next(w);
            } else {
                c = navigator.prev(w);
            }
            for (Iterator j=c.iterator(); j.hasNext(); ) {
                Object v = j.next();
                if (v == w)
                    continue;

                Object u = eval(v);
                int uid = getLabel(u);
                if (semi[uid] < semi[wid])
                    semi[wid] = semi[uid];
            }

            Object pw = parent[wid];
            ancestor[wid] = pw;

            buckets[semi[wid]].add(w);

            if (pw != null) {
                int pwid = getLabel(pw);

                // Step 3

                Iterator eb = buckets[semi[pwid]].iterator();
                while (eb.hasNext()) {
                    Object vd;
                    Object v = eb.next();
                    Object u = eval(v);
                    int vid = getLabel(v);
                    int uid = getLabel(u);

                    if (semi[uid] < semi[vid])
                        vd = u;
                    else
                        vd = parent[wid];
                    setNode(v, vd);
                }
                buckets[semi[pwid]].clear();
            }
        }

        // Step 4

        for (int j = 1; j < n; j++) {
            Object w = vertex[j];

            if (w == null)
                continue;

            int wid = getLabel(w);
            Object pd = getDominatorOf(w);
            if ((pd != null) && (pd != vertex[semi[wid]]))
                setNode(w, getDominatorOf(pd));
        }

        setNode(start, null);
    }

    private Object eval(Object v) {
        int vid = getLabel(v);

        if (ancestor[vid] == null)
            return v;
        else {
            compress(v);
            return label[vid];
        }
    }

    private void compress(Object v) {
        LinkedList wl = new LinkedList();

        // We have to start at the bottom of the dominator tree.
        for (;;) {
            int vid = getLabel(v);
            Object anv = ancestor[vid];
            int anvid = getLabel(anv);

            if (ancestor[anvid] == null)
                break;

            wl.add(v);
            v = anv;
        }

        while (!wl.isEmpty()) {
            v = wl.removeLast();

            int vid = getLabel(v);
            Object anv = ancestor[vid];
            int anvid = getLabel(anv);

            if (semi[getLabel(label[anvid])] < semi[getLabel(label[vid])])
                label[vid] = label[anvid];
            ancestor[vid] = ancestor[anvid];
        }

    }

    /**
     * Return the dominator of v.
     */
    public final Object getDominatorOf(Object v) {
        Object[] dom = (Object[]) domination.get(v);
        if (dom == null)
            return null;
        return dom[0];
    }

    private void setNode(Object me, Object d) {
        Object[] med = (Object[]) domination.get(me);
        if (med == null) {
            if (d == null)
                return;
            med = new Object[2];
            domination.put(me, med);
        }
        med[0] = d; // Make d the dominator of me.

        if (d == null)
            return;

        // Make me a dominatee of d.

        Object[] dd = (Object[]) domination.get(d);
        if (dd == null) {
            dd = new Object[2];
            domination.put(d, dd);
            dd[1] = me;
            return;
        }

        for (int i = 1; i < dd.length; i++) {
            Object c = dd[i];
            if (c == me)
                return;
            if (c == null) {
                dd[i] = me;
                return;
            }
        }

        Object nd[] = new Object[dd.length + 4];
        System.arraycopy(dd, 0, nd, 0, dd.length);
        nd[dd.length] = me;
        domination.put(d, nd);
    }

    /**
     * Return an iteration of the nodes that <code>n</code> strictly dominates.
     */
    public final Iterator getDominatees(Object n) {
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return Collections.EMPTY_SET.iterator();

        return new DomIterator(dom);
    }

    private static final class DomIterator implements Iterator {
        private Object dom[];
        private Object next;
        private int index;

        public DomIterator(Object dom[]) {
            this.dom = dom;
            this.next = dom[1];
            this.index = 2;
        }

        public boolean hasNext() {
            return (next != null);
        }

        public Object next() {
            Object x = next;
            next = null;
            if (index < dom.length)
                next = dom[index++];
            return x;
        }

        public void remove() {
            if (index < 2)
                return;
            for (int j = index; j < dom.length; j++)
                dom[j - 1] = dom[j];
            dom[dom.length - 1] = null;
            index--;
        }
    }

    /**
     * Push onto the stack all of the nodes that n strictly dominates.
     * The nodes are placed on the stack in ascending order by the node's label.
     */
    public final void pushDominatees(Object n, LinkedList wl) {
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return;

        for (int i = 1; i < dom.length - 1; i++) {
            Object f = dom[i];
            if (f == null)
                break;
            for (int j = i + 1; j < dom.length; j++) {
                Object s = dom[j];
                if (s == null)
                    break;
                if (getLabel(f) > getLabel(s)) {
                    dom[j] = f;
                    dom[i] = s;
                    f = s;
                }
            }
        }

        for (int i = 1; i < dom.length; i++) {
            Object d = dom[i];
            if (d == null)
                break;
            wl.add(d);
        }
    }

    /**
     * Return the number of the nodes that n dominates.
     */
    public final int numDominatees(Object n) {
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return 0;

        int num = 0;
        for (int i = 1; i < dom.length; i++) {
            Object d = dom[i];
            if (d == null)
                break;
            num++;
        }
        return num;
    }

    /**
     * Return the set of all nodes strictly dominated by node n and all 
     * nodes dominated by nodes dominated by n, and so on.
     * @param n is the dominator node
     */
    public final List getIterativeDomination(Object n) {
        ArrayList v = new ArrayList(20);
        getIterativeDomination(n, v);
        return v;
    }

    /**
     * Return the set of all nodes strictly dominated by node n and all 
     * nodes dominated by nodes dominated by n, and so on.
     * @param n is the dominator node
     * @param v dominated nodes are added to <code>List</code> v
     */
    public void getIterativeDomination(Object n, List v) {
        int start = v.size();
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return;

        for (int i = 1; i < dom.length; i++)
            v.add(dom[i]);

        for (int j = start; j < v.size(); j++) {
            Object c = v.get(j);
            if (c == null) {
                v.remove(j);
                j--;
                continue;
            }

            Object[] d = (Object[]) domination.get(c);
            if (d == null)
                continue;

            for (int i = 1; i < d.length; i++)
                v.add(d[i]);
        }
    }

    /**
     * Return true if CFG node n dominates node d.
     * @param n the node to test
     * @param d the node to test
     */
    public final boolean inDominatees(Object n, Object d) {
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return false;

        for (int i = 1; i < dom.length; i++) {
            if (d == dom[i])
                return true;
        }
        return false;
    }

    /**
     * Print out my dominance relations.
     * @param s the stream to write to
     */
    public final void displayDominance(java.io.PrintStream s, Object n) {
        Object[] dom = (Object[]) domination.get(n);
        if (dom == null)
            return;

        s.print(n);
        s.print(" is dominated by ");
        s.print(dom[0]);
        s.print(" and dominates ");
        for (int i = 1; i < dom.length; i++) {
            Object d = dom[i];
            if (d == null)
                continue;

            s.print(" ");
            s.print(d);
        }
        s.println();
    }

    /**
     * Return true if this domination is a post domination.
     */
    public final boolean isPostDominators() {
        return post;
    }

}
