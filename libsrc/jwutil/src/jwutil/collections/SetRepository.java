// SetRepository.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.io.PrintStream;
import jwutil.util.Assert;

/**
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: SetRepository.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class SetRepository extends SetFactory {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 4050480114695222065L;
    
    public static final boolean USE_HASHCODES = true;
    public static final boolean USE_SIZES     = false;
    
    public static final boolean VerifyAssertions = false;
    public static final boolean TRACE = false;
    public static final PrintStream out = System.out;

    public static class LinkedHashSetFactory extends SetFactory {
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3257854264024840501L;

        private LinkedHashSetFactory() { }
        public static final LinkedHashSetFactory INSTANCE = new LinkedHashSetFactory();
        
        /** Generates a new mutable <code>Set</code>, using the elements
            of <code>c</code> as a template for its initial contents. 
        */ 
        public final Set makeSet(Collection c) {
            return new LinkedHashSet(c);
        }
    }
    public static class SimpleHashSetFactory extends MapFactory {
        private SimpleHashSetFactory() { }
        public static final SimpleHashSetFactory INSTANCE = new SimpleHashSetFactory();
        
        /** Generates a new <code>Map</code>, using the entries of
            <code>map</code> as a template for its initial mappings. 
        */
        public final Map makeMap(Map map) {
            SimpleHashSet set = new SimpleHashSet();
            set.putAll(map);
            return set;
        }
    }

    Map cache;
    SetFactory setFactory;
    MapFactory entryFactory;
    
    public SetRepository() {
        if (USE_HASHCODES) {
            cache = new HashMap();
            setFactory = LinkedHashSetFactory.INSTANCE;
            entryFactory = LightMap.Factory.INSTANCE;
        } else if (USE_SIZES) {
            cache = new LightMap();
            setFactory = LinkedHashSetFactory.INSTANCE;
            entryFactory = SimpleHashSetFactory.INSTANCE;
        } else {
            Assert.UNREACHABLE();
        }
    }
    
    public final Set makeSet(Collection c) {
        Set s = SharedSet.make(this, c);
        if (VerifyAssertions) {
            if (!s.equals(c))
                Assert.UNREACHABLE(c+" != "+s);
        }
        return s;
    }
    
    public SharedSet getUnion(Collection sets, boolean disjoint) {
        Set check;
        if (VerifyAssertions) {
            check = new LinkedHashSet();
            for (Iterator i=sets.iterator(); i.hasNext(); )
                check.addAll((Collection)i.next());
            //if (TRACE) out.println("Looking for union set: "+check);
        }
        if (USE_HASHCODES) {
            SharedSet resultSet = SharedSet.makeUnion(this, sets);
            Object setIdentifier = new Integer(resultSet.hashCode());
            Map entry = (Map) cache.get(setIdentifier);
            if (entry == null) {
                if (TRACE) out.println("Adding new cache entry for "+setIdentifier);
                cache.put(setIdentifier, entry = entryFactory.makeMap());
            } else {
                if (TRACE) out.println("Cache entry for "+setIdentifier+" exists");
            }
            SharedSet resultSet2 = (SharedSet) entry.get(resultSet);
            if (resultSet2 != null) {
                if (TRACE) out.println("Set already exists in cache: "+System.identityHashCode(resultSet2));
                if (VerifyAssertions) {
                    if (!check.equals(resultSet2)) {
                        Assert.UNREACHABLE(check+" != "+resultSet2);
                    }
                }
                return resultSet2;
            }
            entry.put(resultSet, resultSet);
            if (TRACE) out.println("Set doesn't exist in cache, adding: "+System.identityHashCode(resultSet));
            if (VerifyAssertions) {
                if (!check.equals(resultSet)) {
                    Assert.UNREACHABLE(check+" != "+resultSet);
                }
            }
            return resultSet;
        } else if (USE_SIZES) {
            Object setIdentifier;
            if (disjoint)
                setIdentifier = calculateSetIdentifier_disjoint(sets);
            else
                setIdentifier = calculateSetIdentifier(sets);
            Map entry = (Map) cache.get(setIdentifier);
            if (entry == null) {
                if (TRACE) out.println("Adding new cache entry for "+setIdentifier);
                cache.put(setIdentifier, entry = entryFactory.makeMap());
            } else {
                if (TRACE) out.println("Cache entry for "+setIdentifier+" exists");
            }
            int newHashCode;
            if (disjoint)
                newHashCode = calculateHashcode_disjoint(sets);
            else
                newHashCode = calculateHashcode(sets);
            SimpleHashSet s = (SimpleHashSet) entry;
            Iterator i = s.getMatchingHashcode(newHashCode).iterator();
uphere:
            while (i.hasNext()) {
                SharedSet hs = (SharedSet) i.next();
                if (TRACE) out.println("Checking matching hashcode ("+newHashCode+") set: "+System.identityHashCode(hs));
                Iterator j = sets.iterator();
                while (j.hasNext()) {
                    Set s1 = (Set) j.next();
                    if (!hs.containsAll(s1)) {
                        if (TRACE) out.println("Missing something from set "+System.identityHashCode(s1));
                        continue uphere;
                    }
                }
                if (TRACE) out.println("Set already exists in cache: "+System.identityHashCode(hs));
                if (VerifyAssertions) {
                    if (!check.equals(hs)) {
                        Assert.UNREACHABLE(check+" != "+hs);
                    }
                }
                return hs;
            }
            SharedSet resultSet = SharedSet.makeUnion(this, sets);
            s.put(resultSet, resultSet);
            if (TRACE) out.println("Set doesn't exist in cache, adding: "+System.identityHashCode(resultSet));
            if (VerifyAssertions) {
                if (!check.equals(resultSet)) {
                    Assert.UNREACHABLE(check+" != "+resultSet);
                }
            }
            return resultSet;
        } else {
            Assert.UNREACHABLE(); return null;
        }
    }
    
    static boolean checkDisjoint(Collection sets) {
        Iterator i = sets.iterator();
        if (!i.hasNext()) return true;
        Set s1 = (Set) i.next();
        while (i.hasNext()) {
            s1 = (Set) i.next();
            for (Iterator j = s1.iterator(); j.hasNext(); ) {
                Object o = j.next();
                for (Iterator k = sets.iterator(); ; ) {
                    Set s2 = (Set) k.next();
                    if (s2 == s1) break;
                    if (s2.contains(o)) return false;
                }
            }
        }
        return true;
    }
    
    static int calculateHashcode_disjoint(Collection sets) {
        int newHashCode = 0;
        for (Iterator i = sets.iterator(); i.hasNext(); ) {
            Set s = (Set) i.next();
            newHashCode += s.hashCode();
        }
        if (VerifyAssertions) Assert._assert(checkDisjoint(sets) == true);
        return newHashCode;
    }
    
    static int calculateSize_disjoint(Collection sets) {
        int newSize = 0;
        for (Iterator i = sets.iterator(); i.hasNext(); ) {
            Set s = (Set) i.next();
            newSize += s.size();
        }
        if (VerifyAssertions) Assert._assert(checkDisjoint(sets) == true);
        return newSize;
    }
    
    static int calculateHashcode(Collection sets) {
        int newHashCode = 0;
        Iterator i = sets.iterator();
        if (!i.hasNext()) return newHashCode;
        Set s1 = (Set) i.next();
        newHashCode = s1.hashCode();
        while (i.hasNext()) {
            s1 = (Set) i.next();
uphere:
            for (Iterator j = s1.iterator(); j.hasNext(); ) {
                Object o = j.next();
                for (Iterator k = sets.iterator(); ; ) {
                    Set s2 = (Set) k.next();
                    if (s2 == s1) break;
                    if (s2.contains(o)) break uphere;
                }
                newHashCode += o.hashCode();
            }
        }
        return newHashCode;
    }
    
    static int calculateSize(Collection sets) {
        int newSize = 0;
        Iterator i = sets.iterator();
        if (!i.hasNext()) return newSize;
        Set s1 = (Set) i.next();
        newSize = s1.size();
        while (i.hasNext()) {
            s1 = (Set) i.next();
uphere:
            for (Iterator j = s1.iterator(); j.hasNext(); ) {
                Object o = j.next();
                for (Iterator k = sets.iterator(); ; ) {
                    Set s2 = (Set) k.next();
                    if (s2 == s1) break;
                    if (s2.contains(o)) break uphere;
                }
                ++newSize;
            }
        }
        return newSize;
    }
    
    public static Object calculateSetIdentifier_disjoint(Collection sets) {
        if (USE_HASHCODES) {
            int newHashCode = calculateHashcode_disjoint(sets);
            return new Integer(newHashCode);
        } else if (USE_SIZES) {
            int newSize = calculateSize_disjoint(sets);
            return new Integer(newSize);
        } else {
            Assert.UNREACHABLE();
            return null;
        }
    }
    
    public static Object calculateSetIdentifier(Collection sets) {
        if (USE_HASHCODES) {
            int newHashCode = calculateHashcode(sets);
            return new Integer(newHashCode);
        } else if (USE_SIZES) {
            int newSize = calculateSize(sets);
            return new Integer(newSize);
        } else {
            Assert.UNREACHABLE();
            return null;
        }
    }
    
    public static class SharedSet implements Set {
        private final Set set;
        private final SetRepository repository;
        
        public static SharedSet make(SetRepository repository, Collection s) {
            return new SharedSet(repository, s);
        }
        public static SharedSet makeUnion(SetRepository repository, Collection sets) {
            Iterator i = sets.iterator();
            Set s = (Set) i.next();
            SharedSet that = new SharedSet(repository, s);
            while (i.hasNext()) {
                s = (Set) i.next();
                that.set.addAll(s);
            }
            return that;
        }
        private SharedSet(SetRepository repository, Collection s) {
            this.repository = repository;
            this.set = repository.setFactory.makeSet(s);
        }
        
        public SharedSet copyAndAddAll(Set s, boolean disjoint) {
            return repository.getUnion(new Pair(this.set, s), disjoint);
        }
        
        public SharedSet copyAndAddAllSets(Collection sets, boolean disjoint) {
            return repository.getUnion(sets, disjoint);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[shared: ");
            for (Iterator i=iterator(); i.hasNext(); ) {
                sb.append(i.next());
                if (i.hasNext()) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        
        public int hashCode() {
            return this.set.hashCode();
        }
        
        public boolean equals(Collection c) {
            return this.containsAll(c) && c.containsAll(this);
        }
        
        public boolean equals(Object o) {
            if (o instanceof Collection) return equals((Collection)o);
            return false;
        }
        
        /**
         * @see java.util.Collection#add(Object)
         */
        public boolean add(Object arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#addAll(Collection)
         */
        public boolean addAll(Collection arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#clear()
         */
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#contains(Object)
         */
        public boolean contains(Object arg0) {
            return set.contains(arg0);
        }

        /**
         * @see java.util.Collection#containsAll(Collection)
         */
        public boolean containsAll(Collection arg0) {
            return set.containsAll(arg0);
        }

        /**
         * @see java.util.Collection#isEmpty()
         */
        public boolean isEmpty() {
            return set.isEmpty();
        }

        /**
         * @see java.util.Collection#iterator()
         */
        public Iterator iterator() {
            return set.iterator();
        }

        /**
         * @see java.util.Collection#remove(Object)
         */
        public boolean remove(Object arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#removeAll(Collection)
         */
        public boolean removeAll(Collection arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#retainAll(Collection)
         */
        public boolean retainAll(Collection arg0) {
            throw new UnsupportedOperationException();
        }

        /**
         * @see java.util.Collection#size()
         */
        public int size() {
            return set.size();
        }

        /**
         * @see java.util.Collection#toArray()
         */
        public Object[] toArray() {
            return set.toArray();
        }

        /**
         * @see java.util.Collection#toArray(Object[])
         */
        public Object[] toArray(Object[] arg0) {
            return set.toArray(arg0);
        }

    }
    
}
