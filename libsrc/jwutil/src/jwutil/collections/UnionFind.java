// UnionFind.java, created Mar 25, 2004 8:11:25 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

/**
 * Union-Find data structure.  Works with objects or int's.
 * 
 * @author jwhaley
 * @version $Id: UnionFind.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class UnionFind {
    
    private int[] array;
    private IndexMap map;
    
    /**
     * Construct a union-find data structure that will contain at most
     * <code>numElements</code> elements.
     * 
     * @param numElements  maximum number of elements
     */
    public UnionFind(int numElements) {
        array = new int[numElements];
        for (int i = 0; i < array.length; i++)
            array[i] = -1;
        map = new IndexMap("UnionFindMap");
    }
    
    /**
     * Add an object if it doesn't already exist. New elements are not unioned
     * with anyone. They are in their own equivalence class.
     * 
     * @param x
     *            object to add
     * @return the index of the object
     */
    public int add(Object x) {
        return map.get(x);
    }
    
    /**
     * Returns true if this union-find contains the given object, false otherwise.
     * 
     * @param x  object to check
     * @return  true iff this union-find contains the given object
     */
    public boolean contains(Object x) {
        return map.contains(x);
    }
    
    /**
     * Unions two elements.
     * 
     * @param e1  first element
     * @param e2  second element
     */
    public void union(Object e1, Object e2) {
        union(map.get(e1), map.get(e2));
    }
    
    /**
     * Finds the representative for the equivalence class of the given object.
     * 
     * @param x  object
     * @return  representative
     */
    public Object find(Object x) {
        return map.get(find(map.get(x)));
    }
    
    /**
     * Checks whether the two elements are part of the same equivalence class.
     * 
     * @param e1  first element
     * @param e2  second element
     * @return  whether the two elements are unioned
     */
    public boolean differ(Object e1, Object e2) {
        return differ(map.get(e1), map.get(e2));
    }
    
    /**
     * Union two disjoint sets using the height heuristic.
     * 
     * @param e1
     *                first element
     * @param e2
     *                second element
     */
    public void union(int e1, int e2) {
        int root1 = find(e1);
        int root2 = find(e2);
        if (root1 == root2) return;
        if (array[root2] < array[root1]) /* root2 is deeper */
            array[root1] = root2; /* Make root2 new root */
        else {
            if (array[root1] == array[root2])
                array[root1]--; /* Update height if same */
            array[root2] = root1; /* Make root1 new root */
        }
    }
    
    /**
     * Perform a find with path compression.  Error checks omitted for
     * simplicity.
     * 
     * @param x
     *                the element being searched for.
     * @return the set containing x.
     */
    public int find(int x) {
        if (array[x] < 0)
            return x;
        else
            return array[x] = find(array[x]);
    }
    
    /**
     * Checks whether the two elements are part of the same equivalence class.
     * 
     * @param e1  first element
     * @param e2  second element
     * @return  whether the two elements are unioned
     */
    public boolean differ(int e1, int e2) {
        return find(e1) != find(e2);
    }
}
