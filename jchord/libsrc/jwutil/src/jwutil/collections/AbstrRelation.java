// AbstrRelation.java, created Thu Jun 29 19:13:12 2000 by salcianu
// Copyright (C) 2000 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import jwutil.strings.Strings;

/**
 * <code>AbstrRelation</code>
 * 
 * @author  Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: AbstrRelation.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public abstract class AbstrRelation implements Relation, Cloneable, 
                                        java.io.Serializable {
    
    /** Optimizes the .hashCode() method by caching the hash code.
        The cached value is invalidated after each update. */
    public static final boolean OPTIMIZE_HASH_CODE = true;

    protected Relation getEmptyRelation() {
        throw new UnsupportedOperationException();
    }


    public boolean add(Object key, Object value) {
        throw new UnsupportedOperationException();
    }


    // functional but inefficient implementation
    public boolean addAll(Object key, Collection values) {
        hashCode = 0;
        boolean retval = false;
        for(Iterator it = values.iterator(); it.hasNext(); )
            if(add(key, it.next()))
                retval = true;
        return retval;
    }


    public void remove(Object key, Object value) {
        Set vals = getValues2(key);
        if(vals == null) return;
        
        hashCode = 0;
        vals.remove(value);
        if(vals.isEmpty())
            removeKey(key);
    }


    protected Set getValues2(Object key) {
        throw new UnsupportedOperationException();
    }


    // functional but inefficient implementation
    public void removeAll(Object key, Collection values) {
        for(Iterator it = values.iterator(); it.hasNext(); )
            remove(key, it.next());     
    }


    public void removeKey(Object key) {
        throw new UnsupportedOperationException();
    }


    public void removeKeys(PredicateWrapper predicate) {
        hashCode = 0;
        // 1. put all the keys that satisfy predicate in keysToRemove 
        Collection keysToRemove = new Vector();
        for(Iterator it = keys().iterator(); it.hasNext(); ) {
            Object key = it.next();
            if(predicate.check(key))
                keysToRemove.add(key);
        }
        // 2. actually do the removal
        for(Iterator it = keysToRemove.iterator(); it.hasNext(); )
            removeKey(it.next());
    }


    public void removeValues(PredicateWrapper predicate) {
        hashCode = 0;

        Collection keysToRemove = new Vector();
        for(Iterator itk = keys().iterator(); itk.hasNext(); ) {
            Object key = itk.next();
            Set vals = getValues(key);
            for(Iterator itv = vals.iterator(); itv.hasNext(); ) {
                Object value = itv.next();
                if(predicate.check(value))
                    itv.remove();
            }
            if(vals.isEmpty())
                keysToRemove.add(key);
        }

        for(Iterator itk = keysToRemove.iterator(); itk.hasNext(); )
            removeKey(itk.next());
    }


    public void removeObjects(PredicateWrapper predicate) {
        removeKeys(predicate);
        removeValues(predicate);
    }


    public boolean contains(Object key, Object value) {
        Set vals = getValues(key);
        return (vals != null) && vals.contains(value);
    }


    public boolean containsKey(Object key) {
        Set vals = getValues(key);
        return (vals != null) && !vals.isEmpty();
    }


    public boolean isEmpty() {
        return keys().isEmpty();
    }


    public Set getValues(Object key) {
        throw new UnsupportedOperationException();
    }


    public Set keys() {
        throw new UnsupportedOperationException();
    }


    public Set values() {
        Set vals = new HashSet();
        for(Iterator it = keys().iterator(); it.hasNext(); )
            vals.addAll(getValues(it.next()));
        return vals;
    }
    

    public void union(Relation rel) {
        for(Iterator itk = rel.keys().iterator(); itk.hasNext(); ) {
            Object key = itk.next();
            addAll(key, rel.getValues(key));
        }
    }


    public boolean equals(Object o) {
        if((o == null) || !(o instanceof Relation))
            return false;
        Relation rel2 = (Relation) o;

        Set ks  = keys();
        Set ks2 = rel2.keys();
        
        if(!equal_sets(ks, ks2))
            return false;

        for(Iterator it = ks.iterator(); it.hasNext(); ){
            Object key = it.next();
            Set vs  = getValues(key);
            Set vs2 = rel2.getValues(key);
            if(!equal_sets(vs, vs2))
                return false;
        }

        return true;
    }


    public int hashCode() {
        if((hashCode == 0) || !OPTIMIZE_HASH_CODE) {
            hashCode = 0;
            for(Iterator itk = keys().iterator(); itk.hasNext(); ) {
                Object key = itk.next();
                hashCode += key.hashCode();
                for(Iterator itv = getValues(key).iterator(); itv.hasNext(); ){
                    Object value = itv.next();
                    hashCode += value.hashCode();
                }
            }
        }
        return hashCode;
    }
    protected int hashCode = 0;
    

    // Test whether c1 and c2 contain the same elements.
    // Precondition: c1 and c2 don't contain duplicates.
    // So, we can say that c1 and c2 are equal if they have the same number
    // of elements and each element of c1 is also in c2.
    private boolean equal_sets(Collection c1, Collection c2) {
        if((c1 == null) || (c2 == null))
            return c1 == c2;

        if(c1.size() != c2.size())
            return false;

        for(Iterator it = c1.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if(!c2.contains(obj))
                return false;
        }

        return true;        
    }


    public Relation select(Collection selected_keys) {
        Relation retval = getEmptyRelation();

        for(Iterator it = selected_keys.iterator(); it.hasNext(); ) {
            Object key = it.next();
            retval.addAll(key, getValues(key));
        }

        return retval;
    }


    public void forAllEntries(EntryVisitor visitor) {
        for(Iterator itk = keys().iterator(); itk.hasNext(); ) {
            Object key = itk.next();
            for(Iterator itv = getValues(key).iterator(); itv.hasNext(); )
                visitor.visit(key, itv.next());
        }
    }


    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("{");
        /*
        
        Object[] ks = Debug.sortedCollection(keys());
        for(int i = 0 ; i < ks.length ; i++ ){
            Object key = ks[i];
            buffer.append(Strings.lineSep+"  ");
            buffer.append(key);
            buffer.append(" -> ");
            buffer.append(Debug.stringImg(getValues(key)));
        }
        
        */
        buffer.append(Strings.lineSep+" }"+Strings.lineSep);
        
        return buffer.toString();
    }


    public Object clone() {
        try{
            return super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

}
