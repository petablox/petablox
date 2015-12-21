// FibonacciHeap.java, created Jun 15, 2003 12:16:03 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

/**
 * FibonacciHeap
 * 
 * @author John Whaley
 * @version $Id: FibonacciHeap.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class FibonacciHeap {

    static class Node {
        int key;
        Object entry;
        
        Node next;
        Node previous;
        
        boolean mark;
        int degree;

        Node parent;
        List childlist;

        Node(int key, Object entry) {
            this.key = key;
            this.entry = entry;
            childlist = new List();
            parent = null;
            mark = false;
            degree = 0;
        }

        Node(int key) {
            this.key = key;
            this.entry = null;
            childlist = new List();
            parent = null;
            mark = false;
            degree = 0;
        }

        Node(Node p) {
            this.key = p.key;
            this.entry = p.entry;
            childlist = new List();
            parent = null;
            mark = false;
            degree = 0;
        }

        public void print() {
            System.out.print(key);
            if (mark)
                System.out.print("*");
            System.out.print(" ");
            if (childlist.head != null) {
                childlist.print();
            }
        }
    }

    static class List {

        Node    head;
        boolean empty;

        List() {
          head  = null;
          empty = true;
        }
  
        void insert(Node newNode) {
          if (empty) {
            newNode.next     = newNode;
            newNode.previous = newNode;
            head = newNode;
            empty = false;
          } else {
            newNode.next       = head;
            newNode.previous   = head.previous;    
            head.previous.next = newNode;
            head.previous      = newNode;
            head = newNode;
          }
        }

        void remove(Node element) {
          if (element != null) {
            element.previous.next = element.next; 
            element.next.previous = element.previous;

            if (element == head)
              head = head.next;

            if (element == head) {
                head  = null;
                empty = true;
            }
          }
        }

        void merge(List L) {
          if (L == null || L.head == null) return;
          if (head == null) {
            head = L.head;
            empty = false;
            return;
          }
          Node previous = head.previous;
          L.head.previous.next = head;
          head.previous = L.head.previous;
          previous.next = L.head;
          L.head.previous = previous;
        }

        void printTree() {
            if (!empty) {
                Node x = (Node) head;
                x.print();
                x = (Node) x.next;
                int i = 2;
                while (x != head) {
                    System.out.print("   ");
                    x.print();
                    x = (Node) x.next;
                    i++;
                }
                System.out.println();
            } else
                System.out.println("Tree is empty.");
        }

        void print() {
            if (!empty) {
                Node x = (Node) head;
                x.print();
                x = (Node) x.next;
                while (x != head) {
                    x.print();
                    x = (Node) x.next;
                }
            } else
                System.out.println("List is empty.");
        }

    }

    protected List rootlist;
    protected Node min;
    protected int size;

    public FibonacciHeap() {
        size = 0;
        rootlist = new List();
        min = null;
    }

    public boolean empty() {
        return rootlist.empty;
    }

    public void meld(FibonacciHeap Q) {
        if (Q == null)
            return;
        FibonacciHeap F = (FibonacciHeap) Q;
        rootlist.merge(F.rootlist);
        size = size + F.size;
        if (min == null)
            min = F.min;
        else if (F.min != null && F.min.key < min.key)
            min = F.min;
    }

    public Node insert(int k, Object p) {
        Node q = new Node(k, p);
        FibonacciHeap Q = new FibonacciHeap();
        Q.rootlist.insert(q);
        Q.min = q;
        Q.size = 1;
        meld(Q);
        return q;
    }

    private Node link(Node x, Node y) {
        if (x.key > y.key)
            return link(y, x);

        x.childlist.insert(y);
        y.parent = x;
        x.degree++;
        return x;
    }

    private void tableInsert(Node[] A, Node x) {
        if (A[x.degree] == null) {
            A[x.degree] = x;
            return;
        }
        Node y = A[x.degree];
        A[x.degree] = null;
        x = link(x, y);
        tableInsert(A, x);
    }

    private int log(int n) {
        int i = 1;
        int logn = 0;
        while (i < n) {
            i = 2 * i;
            logn++;
        }
        return logn;
    }

    private void consolidate() {
        int maxDegree = 2 * log(size);
        Node[] A = new Node[maxDegree];

        for (int i = 0; i < maxDegree; i++)
            A[i] = null;

        while (!rootlist.empty) {
            Node x = (Node) rootlist.head;
            rootlist.remove(x);
            tableInsert(A, x);
        }

        min = null;
        for (int i = 0; i < maxDegree; i++) {
            if (A[i] != null) {
                A[i].mark = false;
                A[i].parent = null;
                rootlist.insert(A[i]);
                if (min == null || A[i].key < min.key)
                    min = A[i];
            }
        }
    }

    public Node deleteMin() {

        if (min == null)
            return new Node(Integer.MIN_VALUE);

        rootlist.merge(min.childlist);

        rootlist.remove(min);

        Node p = min;
        if (rootlist.empty)
            min = null;
        else
            consolidate();

        size = size - 1;
        return p;
    }

    private void cut2(Node v) {
        v.parent.childlist.remove(v);
        v.parent.degree = v.parent.degree - 1;
        rootlist.insert(v);
        v.parent = null;
        v.mark = false;
    }

    private void cascadingCut(Node v) {
        if (v.parent != null) {
            if (v.mark) {
                Node parent = v.parent;
                cut2(v);
                cascadingCut(parent);
            } else
                v.mark = true;
        }
    }

    public void decreaseKey2(int k, Node p) {
        Node v = (Node) p;
        if (k >= v.key)
            return;

        v.key = k;

        if (v.parent != null && v.parent.key > v.key) {
            Node parent = v.parent;
            cut2(v);
            cascadingCut(parent);
        }

        if (v.key < min.key)
            min = v;
    }

    private void cut(Node v) {
        Node parent = v.parent;
        if (parent != null) {
            v.parent.childlist.remove(v);
            v.parent.degree = v.parent.degree - 1;
            v.parent = null;
            v.mark = false;
            rootlist.insert(v);
        }
    }

    public void decreaseKey(int k, Node p) {
        Node v = (Node) p;
        if (k > v.key)
            return;
        v.key = k;
        if (v.key < min.key)
            min = v;
        if (v.parent == null || v.parent.key < v.key)
            return;

        v.mark = true;

        while (v.parent != null && v.mark) {
            Node parent = v.parent;

            cut(v);

            v = parent;
        }

        if (v.parent != null)
            v.mark = true;
    }

    public void delete(Node p) {
        decreaseKey(Integer.MIN_VALUE, p);
        deleteMin();
    }

    public Node min() {
        return min;
    }

    public void print() {
        rootlist.printTree();
    }

}

