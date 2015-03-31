// BinHeapPriorityQueue.java, created Tue Jun  1 15:08:45 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Arrays;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * <code>BinHeapPriorityQueue</code> is an implementation of the
 * <code>PriorityQueue</code> interface. It supports O(1) time
 * <code>peekMax</code> and O(lg n) time <code>insert</code> and 
 * <code>removeMax</code> operations, assuming that
 * <code>ArrayList</code> is implemented in a reasonable manner.  The
 * <code>remove</code> operation is probably slow however.
 *
 * Look into implementing a FibonacciHeap-based representation if
 * speed becomes an issue. 
 * 
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: BinHeapPriorityQueue.java,v 1.2 2005/04/19 08:58:41 joewhaley Exp $
 */
public class BinHeapPriorityQueue extends AbstractCollection implements MaxPriorityQueue {
    private HashMap item2entry;
    private Entry[] heap;
    private int size;

    private final static int DEFAULT_SIZE=16;
    
    public BinHeapPriorityQueue() {
        this(DEFAULT_SIZE);
    }

    public BinHeapPriorityQueue(int size) {
        item2entry = new HashMap(size);
        heap = new Entry[size];
        size = 0;
    }


    public boolean insert(Object item, int priority) {

        // already exists? then simply set priority
        if (item2entry.containsKey(item)) {
            return setPriority(item, priority) != priority;
        }

        ensureCapacity(size+1);
        
        Entry entry = new Entry();
        entry.item = item;
        entry.priority = priority;

        // insert at last position in heap
        entry.heapIndex = size;
        heap[size] = entry;
        size++;
        
        item2entry.put(item, entry);
        
        // now percolate to go up the hierarchy
        percolate(entry);

        // the collection has changed
        return true;
    }

    public int getPriority(Object item) {
        Entry entry = (Entry) item2entry.get(item);

        if (entry == null) {
            throw new NoSuchElementException(item.toString());
        }

        return entry.priority;
    }
    

    public int setPriority(Object item, int priority) {
        Entry entry = (Entry) item2entry.get(item);

        if (entry == null) {
            throw new NoSuchElementException(item.toString());
        }

        int oldPriority = entry.priority;
        entry.priority = priority;
        
        priorityChanged(entry, priority - oldPriority);

        return oldPriority;
    }

    public void changePriority(Object item, int delta) {
        Entry entry = (Entry) item2entry.get(item);

        if (entry == null) {
            throw new NoSuchElementException(item.toString());
        }

        entry.priority += delta;
        priorityChanged(entry, delta);
    }

    public Object peekMax() {
        return heap[0].item;
    }
    
    public Object deleteMax() {
        if (size == 0) {
            throw new NoSuchElementException("Heap is empty");
        }
        
        Object item = heap[0].item;

        removeEntry(heap[0]);

        return item;
    }
    
    
    public boolean remove(Object item) {
        Entry entry = (Entry) item2entry.get(item);

        if (entry == null) return false;

        removeEntry(entry);

        return true;
    }

    public boolean contains(Object item) {
        return item2entry.containsKey(item);
    }

    public Iterator iterator() {
        return new HeapIterator();
    }

    public void clear() {
        item2entry.clear();
        Arrays.fill(heap, null);
        size = 0;
    }

    public int size() {
        return size;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("[");
        
        for (int i = 0; i<size; i++) {
            sb.append(" (").append(heap[i].item).append(", ")
                .append(heap[i].priority).append(")");
        }

        return sb.append(" ]").toString();
    }

    private void removeEntry(Entry entry) {
        Entry last = heap[size - 1];
        
        swap(entry, last);

        heap[size - 1] = null;
        size--;

        priorityChanged(last, last.priority - entry.priority);
        
        item2entry.remove(entry.item);
    }

    private void ensureCapacity(int size) {
        if (heap.length >= size) return;

        Entry[] newHeap = new Entry[ Math.max(size, heap.length*2) ];
        
        System.arraycopy(heap, 0, newHeap, 0, heap.length);

        heap = newHeap;
    }
        
    private void priorityChanged(Entry entry, int delta) {
        if (delta > 0) {
            // priority has grown, move up
            percolate(entry);
        } else if (delta < 0) {
            // priority has decreased, move down
            siftDown(entry);
        }
    }

    private void percolate(Entry entry) {
        while (entry.heapIndex > 0) {
            Entry parent = heap[ (entry.heapIndex-1) / 2 ];

            // are we better than parent? if not, we're in the right place
            if (entry.priority <=  parent.priority) {
                break;
            }

            swap(parent, entry);
        }
    }

    private void siftDown(Entry entry) {
        while (entry.heapIndex*2 + 1 < size) {
            Entry leftSon = heap[entry.heapIndex*2 + 1];
            Entry rightSon = entry.heapIndex*2 + 2 < size ?
                heap[entry.heapIndex*2 + 2] : null;

            Entry maxSon =
                (rightSon == null || leftSon.priority > rightSon.priority) ?
                leftSon : rightSon;

            // are we better than our best son? if so, we're in the right place
            if (entry.priority >= maxSon.priority) {
                break;
            }

            swap(entry, maxSon);
        }
    }

    
    private void swap(Entry e1, Entry e2) {
        int e1_index = e1.heapIndex;
        int e2_index = e2.heapIndex;
                    
        e1.heapIndex = e2_index;
        heap[e2_index] = e1;

        e2.heapIndex = e1_index;
        heap[e1_index] = e2;
    }

    

    class Entry {
        int priority;
        int heapIndex;
        Object item;
    }

    // wrapper around the hashMap's entry iterator
    class HeapIterator implements Iterator {
        Iterator hashIterator;
        Entry current;
        
        HeapIterator() {
            hashIterator = item2entry.entrySet().iterator();
        }

        public boolean hasNext() {
            return hashIterator.hasNext();
        }

        public Object next() {
            Map.Entry hashEntry = ((Map.Entry) hashIterator.next());
            current = (Entry) hashEntry.getValue();
            return hashEntry.getKey();
        }

        public void remove() {
            hashIterator.remove();
            removeEntry(current);
        }
    }
}
