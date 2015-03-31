// LightMap.java, created Sun Mar 19 15:46:56 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * <code>LightMap</code> is a very lightweight implementation of
 the <code>java.util.Map</code> interface.
 * 
 * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: LightMap.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class LightMap implements Map, Cloneable, java.io.Serializable {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3258407344043144246L;

    public static class Factory extends MapFactory {
        private Factory() { }
        public static final Factory INSTANCE = new Factory();
        
        /** Generates a new <code>Map</code>, using the entries of
            <code>map</code> as a template for its initial mappings. 
        */
        public final Map makeMap(Map map) {
            Map newMap = new LightMap();
            newMap.putAll(map);
            return newMap;
        }
    }
    
    // the number of mappings in this map
    private int size = 0;
    // the root of the binary tree used to store the mapping
    private BinTreeNode root = null;

    /** Creates a <code>LightMap</code>. */
    public LightMap() {}

    public final int size(){
        return size;
    }

    public final boolean isEmpty(){
        return size != 0;
    }

    /** Returns <code>true</code> if this map contains a mapping
        for the specified key. */
    public final boolean containsKey(Object key){
        return get(key) != null;
    }

    /** Unsupported yet. */
    public final boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /** Returns the value to which this map maps the specified key. */
    public final Object get(Object key){
        BinTreeNode p = root;
        int key_hash_code = key.hashCode();

        while(p != null)
            if(key_hash_code < p.keyHashCode)
                p = p.left;
            else
                if(key_hash_code > p.keyHashCode)
                    p = p.right;
                else
                    if(p.key.equals(key))
                        return p.value;
                    else p = p.right;

        return null;
    }


    /** Associates the specified value with the specified key in this map. */
    public final Object put(Object key, Object value){
        BinTreeNode prev = null;
        BinTreeNode node = root;
        int key_hash_code = key.hashCode();

        while(node != null) {
            prev = node;
            if(key_hash_code < node.keyHashCode)
                node = node.left;
            else
                if((key_hash_code > node.keyHashCode) || !node.key.equals(key))
                    node = node.right;
                else {
                    Object temp = node.value;
                    node.value = value;
                    return temp;
                }
        }

        size++;
        hash_code = -1; // invalidate the cached hash code

        BinTreeNode new_node = new BinTreeNode(key, value);
        if(prev == null){
            root = new_node;
            return null;
        }

        if(key_hash_code < prev.keyHashCode)
            prev.left = new_node;
        else
            prev.right = new_node;
        return null;
    }


    /** Removes the mapping previously attached to <code>key</code>.
        Returns the old mapping if any, or <code>null</code> otherwise. */
    public final Object remove(Object key) {
        
        if(key == null) return null;

        int key_hash_code = key.hashCode();
        BinTreeNode prev = null;
        int son = 0;
        BinTreeNode node = root;

        while(node != null)
            if(key_hash_code < node.keyHashCode) {
                prev = node;
                node = node.left;
                son  = 0;
            }
            else
                if((key_hash_code > node.keyHashCode) ||
                   !node.key.equals(key)) {
                    prev = node;
                    node = node.right;
                    son  = 1;
                }
                else {
                    size--;
                    hash_code = -1; // invalidate the cached hash code
                    return remove_node(node, prev, son);
                }
        return null;
    }

    // Remove the BinTreeNode pointed to by node. prev is supposed to be the
    // parent node, pointing to node through his left (son == 0) respectively
    // right (son == 1) link. Returns the old value attached to node.
    private final Object remove_node(BinTreeNode node, BinTreeNode prev,
                                     int son) {
        if(node.left == null)
            return remove_semi_leaf(node, prev, son, node.right);
        if(node.right == null)
            return remove_semi_leaf(node, prev, son, node.left);

        // The BinTreeNode to replace node in the tree. This is either the
        // next or the precedend node (in the order of the hashCode's.
        // We decide a bit randomly, to gain some balanceness.
        BinTreeNode m = 
            (node.keyHashCode % 2 == 0) ?
            extract_next(node) : extract_prev(node);

        return finish_removal(node, prev, son, m);
    }


    // Remove a [semi]-leaf (a node with at least one of its sons absent.
    // In this case, we simply "bypass" the link from the predecessor (if any)
    // to the only predecessor of node that could exist.
    private final Object remove_semi_leaf(BinTreeNode node, BinTreeNode prev,
                                           int son, BinTreeNode m) {
        if(prev == null)
            root = m;
        else
            if(son == 0)
                prev.left = m;
            else
                prev.right = m;

        return node.value;
    }


    // Terminal phase of the node removal. Returns the old value attached to
    // node. node, prev and son are as for remove_node; m points to the
    // BinTreeNode that should replace node in the tree.
    private final Object finish_removal(BinTreeNode node, BinTreeNode prev,
                                       int son, BinTreeNode m) {
        if(m != null) { // set up the links for m
            m.left  = node.left;
            m.right = node.right;
        }
        if(prev == null)
            root = m;
        else
            if(son == 0)
                prev.left = m;
            else
                prev.right = m;

        return node.value;
    }

    // Finds the leftmost BinTreeNode from the right subtree of node;
    // removes it from that subtree and returns it.
    private final BinTreeNode extract_next(BinTreeNode node) {
        BinTreeNode prev = node.right;
        BinTreeNode curr = prev.left;

        if(curr == null) {
            node.right = node.right.right;
            return prev;
        }

        while(curr.left != null) {
            prev = curr;
            curr = curr.left;
        }

        prev.left = curr.right;
        return curr;
    }

    // Finds the rightmost BinTreeNode from the left subtree of node;
    // removes it from that subtree and returns it.
    private final BinTreeNode extract_prev(BinTreeNode node) {
        BinTreeNode prev = node.left;
        BinTreeNode curr = prev.right;

        if(curr == null) {
            node.left = node.left.left;
            return prev;
        }

        while(curr.right != null) {
            prev = curr;
            curr = curr.right;
        }

        prev.right = curr.left;
        return curr;
    }


    /** Copies all of the mappings from the specified map to this map. */
    public final void putAll(Map map) throws UnsupportedOperationException{
        for(Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }


    /** Removes all mappings from this map. */
    public final void clear() throws UnsupportedOperationException{
        size = 0;
        hash_code = -1;
        root = null;
    }

    /** Returns a collection view of the values contained in this map. */
    public final Collection values(){
        final Collection vals = new LinkedList();
        get_values(root, vals);
        return vals;
    }

    // recursively explore the tree of mappings and gather all the
    // values in the set vset
    private static final void get_values(final BinTreeNode node,
                                         final Collection vals){
        if(node == null) return;
        vals.add(node.value);
        get_values(node.left,  vals);
        get_values(node.right, vals);
    }

    /** Returns the set of entries of this map. The result is a 
        <code>Set</code> of <code>Map.Entry</code>. */
    public final Set entrySet() throws UnsupportedOperationException {
        final Set eset = new HashSet();
        get_entries(root, eset);
        return eset;
    }

    // recursively explore the tree of mappings and gather all the
    // entries in the set eset
    private static final void get_entries(final BinTreeNode node,
                                          final Set eset) {
        if(node == null) return;
        eset.add(new Entry(node));
        get_entries(node.left,  eset);
        get_entries(node.right, eset);
    }

    private static class Entry implements Map.Entry {
        Object key;
        Object value;
        BinTreeNode node; // the node behind this entry

        Entry(BinTreeNode node){
            this.node  = node;
            this.key   = node.key;
            this.value = node.value;
        }

        public Object getKey() { return node.key; }
        public Object getValue() { return node.value; }

        public Object setValue(Object value) {
            Object old_value = node.value;
            node.value = value;
            this.value = value;
            return old_value;
        }

        public boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof Map.Entry)) return false;

            Map.Entry e2 = (Map.Entry) o;
            return 
                key.equals(e2.getKey()) && 
                value.equals(e2.getValue());
        }

        public int hashCode(){
            return key.hashCode() + value.hashCode();
        }

        public String toString() {
            return "<" + key + "," + value + ">";
        }
    }


    /** Returns a set view of the keys contained in this map. Unlike the
        <code>java.util</code> maps, this set is NOT backed by the map
        (<i>eg</i> removing a key from the returned set has no effect on
        the map). */
    public final Set keySet() {
        final Set kset = new HashSet();
        get_keys(root, kset);
        return kset;
    }

    // recursively explore the tree of mappings and gather all the
    // keys in the set kset
    private static final void get_keys(final BinTreeNode node,
                                       final Set kset) {
        if(node == null) return;
        kset.add(node.key);
        get_keys(node.left,  kset);
        get_keys(node.right, kset);
    }


    private BinTreeNode copy_tree(BinTreeNode node) {
        if(node == null) return null;
        BinTreeNode newnode = new BinTreeNode(node.key, node.value);

        newnode.left  = copy_tree(node.left);
        newnode.right = copy_tree(node.right);
        
        return newnode;
    }


    public Object clone() {
        try {
            LightMap newmap = (LightMap) super.clone();
            newmap.root = copy_tree(root);
            return newmap;
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }
    }


    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Map))
            return false;
        
        Map m2 = (Map) o;

        Set set1 = this.entrySet();
        Set set2 = m2.entrySet();

        // two maps are equal if they have the same set of entries
        return set1.equals(set2);
    }


    public int hashCode() {
        if(hash_code == -1)
            hash_code = compute_hash_code(root);
        return hash_code;
    }

    private int hash_code = -1;

    private int compute_hash_code(BinTreeNode node) {
        if(node == null) return 0;
        
        return 
            node.keyHashCode + 
            compute_hash_code(node.left) +
            compute_hash_code(node.right);
    }

    private static class BinTreeNode implements java.io.Serializable {
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3256442503912765744L;
        
        final Object key;
        Object value;
        final int keyHashCode;

        BinTreeNode left;
        BinTreeNode right;

        BinTreeNode(final Object key, final Object value) {
            this.key    = key;
            this.value  = value;
            keyHashCode = key.hashCode();
            left  = null;
            right = null;
        }

        public String toString() {
            return "<" + key + "," + value + ">";
        }
    }


    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        build_str(root, buffer);
        buffer.append(" ]");
        return buffer.toString();
    }

    private void build_str(final BinTreeNode node, final StringBuffer buffer) {
        if(node == null) return;
        build_str(node.left,  buffer);
        buffer.append(" <" + node.key + "," + node.value + ">");
        build_str(node.right, buffer);
    }

}
