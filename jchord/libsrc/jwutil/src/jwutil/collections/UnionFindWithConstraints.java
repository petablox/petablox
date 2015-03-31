// UnionFind.java, created Mar 25, 2004 8:11:25 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Arrays;
import java.util.Random;
import jwutil.util.Assert;

/**
 * Union-Find data structure with not-equal constraints.
 * 
 * @author jwhaley
 * @version $Id: UnionFindWithConstraints.java,v 1.1 2005/08/30 11:05:42 joewhaley Exp $
 */
public class UnionFindWithConstraints {
    
    static final boolean SORT_ON_CHECK = false;
    static final boolean SORT_ON_COMBINE = true;
    static final boolean TRACE = false;
    
    private int[] array;
    private int[][] neq_constraints;
    private int[] neq_constraints_length;
    private IndexMap map;
    
    /**
     * Construct a union-find data structure that will contain at most
     * <code>numElements</code> elements.
     * 
     * @param numElements  maximum number of elements
     */
    public UnionFindWithConstraints(int numElements) {
        array = new int[numElements];
        neq_constraints = new int[numElements][];
        neq_constraints_length = new int[numElements];
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
    public boolean union(Object e1, Object e2) {
        return union(e1, e2, true);
    }
    public boolean union(Object e1, Object e2, boolean doit) {
        return union(map.get(e1), map.get(e2), doit);
    }
    
    private boolean try_combine_neq_constraints(int r1, int r2, boolean doit) {
        Assert._assert(array[r1] < 0);
        Assert._assert(array[r2] < 0);
        int[] a1 = neq_constraints[r1];
        int[] a2 = neq_constraints[r2];
        int len1 = neq_constraints_length[r1];
        int len2 = neq_constraints_length[r2];
        if (a2 == null || len2 == 0) {
            if (doit) {
                neq_constraints[r2] = null;
                neq_constraints_length[r2] = 999999;
            }
            return true;
        }
        if (len1 == 0) {
            if (doit) {
                neq_constraints[r1] = a2;
                neq_constraints_length[r1] = len2;
                neq_constraints[r2] = null;
                neq_constraints_length[r2] = 999999;
            }
            return true;
        }
        int[] result = new int[Math.max(len1 + len2, 4)];
        int i1 = 0, i2 = 0, len = 0, t;
        boolean a1c = false, a2c = false;
        t = find(a1[i1]);
        if (t != a1[i1]) { a1c = true; a1[i1] = t; }
        if (a1[i1] == r1 || a1[i1] == r2) return false;
        t = find(a2[i2]);
        if (t != a2[i2]) { a2c = true; a2[i2] = t; }
        if (a2[i2] == r1 || a2[i2] == r2) return false;
        while (i1 < len1 && i2 < len2) {
            boolean d1 = a1[i1] <= a2[i2];
            boolean d2 = a1[i1] >= a2[i2];
            boolean failed = false;
            if (d1) {
                t = a1[i1++];
                if (i1 < len1) {
                    int t2 = find(a1[i1]);
                    if (t2 != a1[i1]) { a1c = true; a1[i1] = t2; }
                    if (a1[i1] == r1 || a1[i1] == r2) failed = true;
                }
            }
            if (d2) {
                t = a2[i2++];
                if (i2 < len2) {
                    int t2 = find(a2[i2]);
                    if (t2 != a2[i2]) { a2c = true; a2[i2] = t2; }
                    if (a2[i2] == r1 || a2[i2] == r2) failed = true;
                }
            }
            if (failed) {
                if (SORT_ON_COMBINE && a1c) {
                    if (TRACE) System.out.print("Collapsing neq for "+r1+":");
                    neq_constraints_length[r1] = sort_and_collapse(a1, len1);
                }
                if (SORT_ON_COMBINE && a2c) {
                    if (TRACE) System.out.print("Collapsing neq for "+r2+":");
                    neq_constraints_length[r2] = sort_and_collapse(a2, len2);
                }
                return false;
            }
            result[len++] = t;
        }
        if (!doit) return true;
        while (i1 < len1) {
            a1[i1] = find(a1[i1]);
            result[len++] = a1[i1++];
        }
        while (i2 < len2) {
            a2[i2] = find(a2[i2]);
            result[len++] = a2[i2++];
        }
        // keep the array best-effort sorted and collapsed.
        if (TRACE) System.out.print("Collapsing neq for "+r1+" and "+r2+":");
        if (SORT_ON_COMBINE) len = sort_and_collapse(result, len);
        neq_constraints[r1] = result;
        neq_constraints_length[r1] = len;
        neq_constraints[r2] = null;
        neq_constraints_length[r2] = 999999;
        return true;
    }
    
    private static int sort_and_collapse(int[] result, int len) {
        Arrays.sort(result, 0, len);
        int orig_len = len;
        if (TRACE) System.out.print(" "+result[0]);
        for (int i = 1, j = 0; i < orig_len; ++i) {
            if (result[j] != result[i]) {
                if (TRACE) System.out.print(" "+result[i]);
                result[++j] = result[i];
            } else {
                if (TRACE) System.out.print(" dup");
                --len;
            }
        }
        if (TRACE) System.out.println(" length "+len);
        return len;
    }
    
    private boolean check_neq_constraints(int r1, int r2) {
        Assert._assert(array[r1] < 0);
        Assert._assert(array[r2] < 0);
        int[] a = neq_constraints[r1];
        if (a == null) return true;
        boolean result = true;
        if (TRACE) System.out.print("Reconstructing neq for "+r1+":");
        for (int i = 0; i < neq_constraints_length[r1]; ++i) {
            int t = find(a[i]);
            if (TRACE) System.out.print(" "+i+"="+t);
            a[i] = t;
            if (t == r2) {
                result = false;
                if (!SORT_ON_CHECK) break;
            }
            if (SORT_ON_CHECK) {
                // this code below is to keep the array best-effort sorted and collapsed.
                // it may be faster to just sort and remove dups instead.
                int j = i;
                while (j > 0 && a[j] <= a[j-1]) {
                    if (a[j] == a[j-1]) {
                        if (TRACE) System.out.print(" (dup)");
                        --neq_constraints_length[r1];
                        --i;
                        if (neq_constraints_length[r1] > j)
                            System.arraycopy(a, j+1, a, j, neq_constraints_length[r1]-j);
                    } else {
                        int t2 = a[j-1];
                        a[j-1] = a[j];
                        if (TRACE) System.out.print(" (swap "+(j-1)+"="+t2+"<->"+j+"="+a[j]+")");
                        a[j] = t2;
                    }
                    --j;
                }
            }
        }
        if (TRACE) System.out.println();
        return result;
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
    
    public boolean disjoint(Object e1, Object e2) {
        return disjoint(map.get(e1), map.get(e2));
    }
    
    public boolean disjoint(int e1, int e2) {
        int root1 = find(e1);
        int root2 = find(e2);
        if (root1 == root2) return false;
        int[] a = neq_constraints[root1];
        int len = neq_constraints_length[root1];
        int k = -1;
        if (len > 0 && (k = binarySearch(a, 0, len-1, root2)) >= 0)
            return true;
        if (a == null)
            a = neq_constraints[root1] = new int[4];
        if (len >= a.length) {
            int[] b = new int[a.length * 2];
            System.arraycopy(a, 0, b, 0, len);
            a = neq_constraints[root1] = b;
        }
        // insert it into the right place.
        k = -k-1;
        if (k < len)
            System.arraycopy(a, k, a, k+1, len-k);
        a[k] = root2;
        neq_constraints_length[root1]++;
        return true;
    }
    
    static int binarySearch(int[] a, int low, int high, int key) {
        while (low <= high) {
            int mid = (low + high) >> 1;
            int midVal = a[mid];
            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    
    /**
     * Union two disjoint sets using the height heuristic.
     * 
     * @param e1
     *                first element
     * @param e2
     *                second element
     */
    public boolean union(int e1, int e2) {
        return union(e1, e2, true);
    }
    public boolean union(int e1, int e2, boolean doit) {
        int root1 = find(e1);
        int root2 = find(e2);
        if (root1 == root2) return true;
        if (!check_neq_constraints(root1, root2) ||
            !check_neq_constraints(root2, root1)) return false;
        if (array[root2] < array[root1]) {/* root2 is deeper */
            if (!try_combine_neq_constraints(root2, root1, doit)) return false;
            if (doit) {
                if (TRACE) System.out.println("\t"+root1+"->"+root2);
                array[root1] = root2; /* Make root2 new root */
            }
        } else {
            if (!try_combine_neq_constraints(root1, root2, doit)) return false;
            if (array[root1] == array[root2])
                array[root1]--; /* Update height if same */
            if (doit) {
                if (TRACE) System.out.println("\t"+root2+"->"+root1);
                array[root2] = root1; /* Make root1 new root */
            }
        }
        return true;
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
        if (array[x] < 0) {
            return x;
        } else {
            int t = find(array[x]);
            if (TRACE && t != array[x]) {
                System.out.println("\t"+x+"-c>"+t);
            }
            array[x] = t;
            return t;
        }
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
    
    private void rep_ok() {
        for (int i = 0; i < neq_constraints.length; ++i) {
            int[] list = neq_constraints[i];
            int len = neq_constraints_length[i];
            if (len == 999999) {
                Assert._assert(array[i] >= 0);
                continue;
            }
            if (SORT_ON_CHECK && SORT_ON_COMBINE) {
                for (int j = 0; j < len - 1; ++j) {
                    Assert._assert(list[j] < list[j+1]);
                }
            }
            for (int j = 0; j < len; ++j) {
                Assert._assert(differ(i, list[j]));
            }
        }
    }
    
    static void randomTest() {
        Random r = new Random(12346);
        final int size = 10000;
        final int nops = 10000;
        final boolean CHECK = true;
        UnionFindWithConstraints u = new UnionFindWithConstraints(size);
        UnionFind uf = null;
        int[][] neq = null;
        int nneq = 0;
        long total_time = 0L;
        if (CHECK) {
            uf = new UnionFind(size);
            neq = new int[nops][2];
        }
        for (int i = 0; i < nops; ++i) {
            int a, b;
            a = r.nextInt(size);
            b = r.nextInt(size);
            if (r.nextBoolean()) {
                if (TRACE) System.out.println(nneq+": disjoint("+a+","+b+") = ");
                long time = System.currentTimeMillis();
                boolean res = u.disjoint(a, b);
                total_time += System.currentTimeMillis() - time;
                if (TRACE || CHECK) System.out.println(nneq+": disjoint("+a+","+b+") = "+res);
                
                if (CHECK) {
                    boolean res2 = uf.differ(a, b);
                    if (res2) {
                        int[] pair = neq[nneq++];
                        pair[0] = a;
                        pair[1] = b;
                    }
                    Assert._assert(res == res2);
                }
            } else {
                if (TRACE) System.out.println(nneq+": union("+a+","+b+") = ");
                long time = System.currentTimeMillis();
                boolean res = u.union(a, b);
                total_time += System.currentTimeMillis() - time;
                if (TRACE || CHECK) System.out.println(nneq+": union("+a+","+b+") = "+res);
                
                if (CHECK) {
                    boolean res2 = true;
                    int aa = uf.find(a);
                    int bb = uf.find(b);
                    for (int j = 0; j < nneq; ++j) {
                        int[] pair = neq[j];
                        pair[0] = uf.find(pair[0]);
                        pair[1] = uf.find(pair[1]);
                        if (pair[0] == aa && pair[1] == bb ||
                            pair[1] == aa && pair[0] == bb) {
                            res2 = false;
                            break;
                        }
                    }
                    if (res2) {
                        uf.union(a, b);
                    }
                    Assert._assert(res == res2);
                }
            }
            if (CHECK) u.rep_ok();
        }
        System.out.println("Time spent for "+nops+" operations: "+total_time+" ms");
    }
    
    public static void main(String[] args) {
        randomTest();
        
        UnionFindWithConstraints u = new UnionFindWithConstraints(100);
        boolean b;
        b = u.disjoint(1, 2);
        Assert._assert(b);
        b = u.disjoint(1, 4);
        Assert._assert(b);
        b = u.disjoint(2, 3);
        Assert._assert(b);
        b = u.disjoint(3, 4);
        Assert._assert(b);
        b = u.union(1, 4);
        Assert._assert(!b);
        b = u.union(1, 3);
        Assert._assert(b);
        b = u.union(1, 3);
        Assert._assert(b);
        b = u.union(4, 1);
        Assert._assert(!b);
        b = u.union(2, 4);
        Assert._assert(b);
        b = u.union(2, 3);
        Assert._assert(!b);
        b = u.disjoint(1, 3);
        Assert._assert(!b);
        b = u.disjoint(4, 3);
        Assert._assert(b);
        b = u.disjoint(3, 4);
        Assert._assert(b);
        b = u.union(2, 3);
        Assert._assert(!b);
    }
}
