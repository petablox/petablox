// IterationList.java, created Jun 30, 2004
// Copyright (C) 2004 Michael Carbin
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import net.sf.bddbddb.ir.dynamic.IRBoolean;
import net.sf.bddbddb.ir.Operation;

/**
 * IterationList
 * 
 * @author mcarbin
 * @version $Id: IterationList.java 339 2004-10-18 04:17:51Z joewhaley $
 */
public class IterationList implements IterationElement {
    boolean TRACE = false;
    List /*IterationElement*/ elements;
    List allNestedElems = null;
    //    boolean isLoop = false;
    IRBoolean loopBool;
    IterationList loopEdge;
    int index;
    static int blockNumber;

    public IterationList(boolean isLoop) {
        this(isLoop, new LinkedList());
    }

    public IterationList(boolean isLoop, List elems) {
        //       this.isLoop = isLoop;
        this.elements = elems;
        this.index = ++blockNumber;
        if (isLoop) {
            loopBool = new IRBoolean("loop" + Integer.toString(this.index) + "_bool", false);
            loopEdge = new IterationList(false);
        }
    }

    public IterationList getLoopEdge() {
        return loopEdge;
    }

    // Return a list that has the IR for all of the loops.
    IterationList unroll() {
        List newElements = new LinkedList();
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IterationList) {
                IterationList list = (IterationList) o;
                newElements.add(list.unroll());
            } else if (isLoop()) {
                InferenceRule rule = (InferenceRule) o;
                List ir = rule.generateIR();
                newElements.addAll(ir);
            }
        }
        return new IterationList(false, newElements);
    }

    void expandInLoop() {
        List newElements = new LinkedList();
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IterationList) {
                IterationList list = (IterationList) o;
                list.expandInLoop();
                newElements.add(list);
            } else {
                InferenceRule rule = (InferenceRule) o;
                List ir = rule.generateIR_incremental();
                newElements.addAll(ir);
            }
        }
        elements = newElements;
        allNestedElems = null;
    }

    public void expand(boolean unroll) {
        List newElements = new LinkedList();
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IterationList) {
                IterationList list = (IterationList) o;
                if (list.isLoop()) {
                    if (unroll) newElements.add(list.unroll());
                    list.expandInLoop();
                } else {
                    list.expand(unroll);
                }
                newElements.add(list);
            } else {
                InferenceRule rule = (InferenceRule) o;
                List ir = rule.generateIR();
                newElements.addAll(ir);
            }
        }
        elements = newElements;
        allNestedElems = null;
    }
    
    public void addElement(IterationElement elem) {
        elements.add(elem);
        allNestedElems = null;
    }

    public void addElement(int j, IterationElement elem) {
        elements.add(j, elem);
        allNestedElems = null;
    }

    public void removeElement(int i) {
        elements.remove(i);
    }
    
    public void removeElement(IterationElement elem) {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (elem.equals(o)) {
                i.remove();
                return;
            }
            if (o instanceof IterationList) {
                ((IterationList) o).removeElement(elem);
            }
        }
        allNestedElems = null;
    }

    public void removeElements(Collection elems) {
        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (elems.contains(o)) {
                i.remove();
                continue;
            }
            if (o instanceof IterationList) {
                ((IterationList) o).removeElements(elems);
            }
        }
        allNestedElems = null;
    }

    public String toString() {
        return (isLoop() ? "loop" : "list") + index;
    }

    public String toString_full() {
        return (isLoop() ? "(loop) " : "") + elements.toString();
    }

    public void print() {
        print(this, "");
    }

    private static void print(IterationList list, String space) {
        System.out.println(space + list + ":");
        for (Object o : list.elements) {
            if (o instanceof IterationList)
                print((IterationList) o, space + "  ");
            else
                System.out.println(space + "  " + o);
        }
    }

    public boolean contains(IterationElement elem) {
        return getAllNestedElements().contains(elem);
    }

    public boolean isLoop() {
        return loopBool != null;
    }

    public ListIterator iterator() {
        return elements.listIterator();
    }

    public ListIterator reverseIterator() {
        return new ReverseIterator(elements.listIterator(elements.size()));
    }
    static class ReverseIterator implements ListIterator {
        ListIterator it;

        public ReverseIterator(ListIterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasPrevious();
        }

        public Object next() {
            return it.previous();
        }

        public int nextIndex() {
            return it.previousIndex();
        }

        public boolean hasPrevious() {
            return it.hasNext();
        }

        public Object previous() {
            return it.next();
        }

        public int previousIndex() {
            return it.nextIndex();
        }

        public void remove() {
            it.remove();
        }

        public void add(Object o) {
            throw new UnsupportedOperationException();
            //it.add(o);
        }

        public void set(Object o) {
            it.set(o);
        }
    }

    public List getAllNestedElements() {
        if (allNestedElems == null) {
            List list = new LinkedList();
            for (Iterator it = elements.iterator(); it.hasNext();) {
                Object elem = it.next();
                if (elem instanceof IterationList) {
                    list.addAll(((IterationList) elem).getAllNestedElements());
                } else {
                    list.add(elem);
                }
            }
            allNestedElems = list;
        }
        return allNestedElems;
    }

    /**
     * @return Returns the loopBool.
     */
    public IRBoolean getLoopBool() {
        return loopBool;
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.size() == 0;
    }
    
    public IterationElement[] getElements() {
        return (IterationElement[]) elements.toArray(new IterationElement[elements.size()]);
    }
    
    public IterationElement getElement(String s) {
        for (Iterator i = elements.iterator(); i.hasNext(); ) {
            IterationElement e2 = (IterationElement) i.next();
            if (s.equals(e2.toString())) return e2;
            if (e2 instanceof IterationList) {
                IterationElement result = ((IterationList) e2).getElement(s);
                if (result != null) return result;
            }
        }
        return null;
    }
    
    public IterationList getContainingList(IterationElement e) {
        if (elements.contains(e)) return this;
        for (Iterator i = elements.iterator(); i.hasNext(); ) {
            IterationElement e2 = (IterationElement) i.next();
            if (e2 instanceof IterationList) {
                IterationList result = ((IterationList) e2).getContainingList(e);
                if (result != null) return result;
            }
        }
        return null;
    }
    
    public int indexOf(IterationElement e) {
        return elements.indexOf(e);
    }
}
